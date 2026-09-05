package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.alien.AlienBlockUtil;
import com.codigohasta.addon.utils.alien.AlienCombatUtil;
import com.codigohasta.addon.utils.alien.AlienEasing;
import com.codigohasta.addon.utils.alien.AlienEntityUtil;
import com.codigohasta.addon.utils.alien.AlienFadeUtils;
import com.codigohasta.addon.utils.alien.AlienInventoryUtil;
import com.codigohasta.addon.utils.alien.AlienRender3DUtil;
import com.codigohasta.addon.utils.alien.AlienTimer;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class AlienV4PacketMine extends Module {
   public static AlienV4PacketMine INSTANCE;
   public static BlockPos secondPos;
   public static double progress = 0.0;
   public static boolean ghost = false;
   public static boolean complete = false;
   private final AlienFadeUtils animationTime = new AlienFadeUtils(1000L);
   private final AlienFadeUtils secondAnim = new AlienFadeUtils(1000L);
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final AlienTimer mineTimer = new AlienTimer();
   private final AlienTimer sync = new AlienTimer();
   private final AlienTimer secondTimer = new AlienTimer();
   private final AlienTimer delayTimer = new AlienTimer();
   private final AlienTimer placeTimer = new AlienTimer();
   private final AlienTimer startTime = new AlienTimer();
   int lastSlot = -1;
   Vec3d directionVec = null;
   Runnable switchBack;
   BlockPos breakPos;
   boolean startPacket = false;
   int breakNumber = 0;
   double breakFinalTime;
   double secondFinalTime;
   boolean sendGroundPacket = false;
   boolean swapped = false;
   int mainSlot = 0;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCheck = this.settings.createGroup("Check");
   private final SettingGroup sgRotation = this.settings.createGroup("Rotation");
   private final SettingGroup sgPlace = this.settings.createGroup("Place");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<AlienV4PacketMine.Page> page = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Page")).description("Settings page")).defaultValue(AlienV4PacketMine.Page.General)).build());
   private final Setting<Double> stopDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("StopDelay"))
                  .description("Delay before stopping"))
               .defaultValue(50.0)
               .min(0.0)
               .max(500.0)
               .sliderRange(0.0, 500.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Double> startDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("StartDelay"))
                  .description("Delay before starting"))
               .defaultValue(200.0)
               .min(0.0)
               .max(500.0)
               .sliderRange(0.0, 500.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Double> damage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Damage"))
                  .description("Mine speed multiplier"))
               .defaultValue(0.7)
               .min(0.0)
               .max(2.0)
               .sliderRange(0.0, 2.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Integer> maxBreak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("MaxBreak"))
                     .description("Max break count before stopping"))
                  .defaultValue(3))
               .min(0)
               .max(20)
               .sliderRange(0, 20)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   public final Setting<Boolean> noGhostHand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("1.21"))
                     .description("1.21 mode (no ghost hand)"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   public final Setting<Boolean> noCollide = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("NoCollide"))
                     .description("No collision when ghost"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<AlienV4PacketMine.TimingMode> timing = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("Timing")).description("Tick timing")).defaultValue(AlienV4PacketMine.TimingMode.All))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> grimDisabler = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("GrimDisabler"))
                     .description("Grim anticheat bypass"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> instant = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Instant"))
                     .description("Instant break mode"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> wait = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Wait"))
                     .description("Wait for block break confirmation"))
                  .defaultValue(true))
               .visible(() -> !(Boolean)this.instant.get() && this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> mineAir = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("MineAir"))
                     .description("Allow mining air blocks"))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.wait.get() && !(Boolean)this.instant.get() && this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> hotBar = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("HotbarSwap"))
                     .description("Only swap in hotbar"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> doubleBreak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("DoubleBreak"))
                     .description("Break two blocks at once"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   public final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("AutoSwitch"))
                     .description("Auto switch tools for double break"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General && (Boolean)this.doubleBreak.get()))
            .build()
      );
   private final Setting<Double> start = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Start"))
                  .description("Double break start threshold"))
               .defaultValue(0.9)
               .min(0.0)
               .max(2.0)
               .sliderRange(0.0, 2.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General && (Boolean)this.doubleBreak.get()))
            .build()
      );
   private final Setting<Double> timeOut = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("TimeOut"))
                  .description("Double break timeout multiplier"))
               .defaultValue(1.2)
               .min(0.0)
               .max(2.0)
               .sliderRange(0.0, 2.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General && (Boolean)this.doubleBreak.get()))
            .build()
      );
   private final Setting<Boolean> setAir = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("SetAir"))
                     .description("Set air client-side after break"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> swing = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Swing"))
                     .description("Swing when starting mining"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> endSwing = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("EndSwing"))
                     .description("Swing when finishing mining"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   public final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Range"))
                  .description("Mine reach distance"))
               .defaultValue(6.0)
               .min(3.0)
               .max(10.0)
               .sliderRange(3.0, 10.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<AlienV4PacketMine.SwingHandMode> swingMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("SwingMode")).description("Swing hand mode"))
                  .defaultValue(AlienV4PacketMine.SwingHandMode.All))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.General))
            .build()
      );
   private final Setting<Boolean> unbreakableCancel = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("UnbreakableCancel"))
                     .description("Cancel mining unbreakable blocks"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> switchReset = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("SwitchReset"))
                     .description("Reset on tool switch"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> preferWeb = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("PreferWeb"))
                     .description("Prefer mining webs"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> preferHead = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("PreferHead"))
                     .description("Prefer head-level blocks"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> farCancel = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("FarCancel"))
                     .description("Cancel if too far"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> onlyGround = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("OnlyGround"))
                     .description("Only mine when on ground"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> checkWeb = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("CheckWeb"))
                     .description("Check web slowdown"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> checkGround = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("CheckGround"))
                     .description("Check ground slowdown"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> smart = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Smart"))
                     .description("Smart ground check"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check && (Boolean)this.checkGround.get()))
            .build()
      );
   private final Setting<Boolean> usingPause = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("UsingPause"))
                     .description("Pause when using items"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> allowOffhand = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("AllowOffhand"))
                     .description("Allow offhand use"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check && (Boolean)this.usingPause.get()))
            .build()
      );
   private final Setting<Boolean> bypassGround = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("BypassGround"))
                     .description("Bypass ground slowdown"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Integer> bypassTime = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("BypassTime"))
                     .description("Bypass time in ms"))
                  .defaultValue(400))
               .min(0)
               .max(2000)
               .sliderRange(0, 2000)
               .visible(() -> (Boolean)this.bypassGround.get() && this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> pauseBind = this.sgCheck
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Pause"))
                     .description("Pause mining"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Check))
            .build()
      );
   private final Setting<Boolean> rotate = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("StartRotate"))
                     .description("Rotate when starting"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation))
            .build()
      );
   private final Setting<Boolean> endRotate = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("EndRotate"))
                     .description("Rotate when ending"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation))
            .build()
      );
   private final Setting<Integer> syncTime = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Sync"))
                     .description("Rotation sync time"))
                  .defaultValue(300))
               .min(0)
               .max(1000)
               .sliderRange(0, 1000)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation))
            .build()
      );
   private final Setting<Boolean> yawStep = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("YawStep"))
                     .description("Step rotation gradually"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation))
            .build()
      );
   private final Setting<Boolean> whenElytra = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("FallFlying"))
                     .description("Yaw step while flying"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation && (Boolean)this.yawStep.get()))
            .build()
      );
   private final Setting<Double> steps = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Steps"))
                  .description("Yaw step size"))
               .defaultValue(0.05)
               .min(0.0)
               .max(1.0)
               .sliderRange(0.0, 1.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation && (Boolean)this.yawStep.get()))
            .build()
      );
   private final Setting<Boolean> checkFov = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("OnlyLooking"))
                     .description("Only rotate if not looking"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation && (Boolean)this.yawStep.get()))
            .build()
      );
   private final Setting<Double> fov = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Fov"))
                  .description("Field of view check"))
               .defaultValue(20.0)
               .min(0.0)
               .max(360.0)
               .sliderRange(0.0, 360.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation && (Boolean)this.yawStep.get()))
            .build()
      );
   private final Setting<Integer> priority = this.sgRotation
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Priority"))
                     .description("Rotation priority"))
                  .defaultValue(10))
               .min(0)
               .max(100)
               .sliderRange(0, 100)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Rotation && (Boolean)this.yawStep.get()))
            .build()
      );
   private final Setting<Boolean> crystal = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Crystal"))
                     .description("Place crystal after break"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place))
            .build()
      );
   private final Setting<Boolean> onlyHeadBomber = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("OnlyCev"))
                     .description("Only place for cev breaker"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place && (Boolean)this.crystal.get()))
            .build()
      );
   private final Setting<Boolean> waitPlace = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("WaitPlace"))
                     .description("Wait for place confirmation"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place && (Boolean)this.crystal.get()))
            .build()
      );
   private final Setting<Boolean> spamPlace = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("SpamPlace"))
                     .description("Spam place crystals"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place && (Boolean)this.crystal.get()))
            .build()
      );
   private final Setting<Boolean> afterBreak = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("AfterBreak"))
                     .description("Place crystal after break"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place && (Boolean)this.crystal.get()))
            .build()
      );
   private final Setting<Boolean> checkDamage = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("DetectProgress"))
                     .description("Check progress before placing"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place && (Boolean)this.crystal.get()))
            .build()
      );
   private final Setting<Double> crystalDamage = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Progress"))
                  .description("Progress threshold for crystal"))
               .defaultValue(0.9)
               .min(0.0)
               .max(1.0)
               .sliderRange(0.0, 1.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place && (Boolean)this.crystal.get() && (Boolean)this.checkDamage.get()))
            .build()
      );
   private final Setting<Boolean> obsidian = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Obsidian"))
                     .description("Place obsidian after break"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place))
            .build()
      );
   private final Setting<Boolean> enderChest = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("EnderChest"))
                     .description("Place ender chest after break"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place))
            .build()
      );
   private final Setting<Boolean> placeRotate = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("PlaceRotate"))
                     .description("Rotate when placing"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place))
            .build()
      );
   private final Setting<Boolean> inventory = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("InventorySwap"))
                     .description("Use inventory swap"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place))
            .build()
      );
   private final Setting<Integer> placeDelay = this.sgPlace
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("PlaceDelay"))
                     .description("Delay between placements"))
                  .defaultValue(100))
               .min(0)
               .max(1000)
               .sliderRange(0, 1000)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Place))
            .build()
      );
   private final Setting<Boolean> checkDouble = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("CheckDouble"))
                     .description("Check double break render"))
                  .defaultValue(false))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<AlienV4PacketMine.AnimMode> animation = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("Animation")).description("Animation type")).defaultValue(AlienV4PacketMine.AnimMode.Up))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<AlienEasing> ease = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("Ease")).description("Easing function")).defaultValue(AlienEasing.CubicInOut))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<AlienEasing> fadeEase = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("FadeEase")).description("Fade easing function")).defaultValue(AlienEasing.CubicInOut))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<Double> expandLine = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("ExpandLine"))
                  .description("Expand outline"))
               .defaultValue(0.0)
               .min(0.0)
               .max(1.0)
               .sliderRange(0.0, 1.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<SettingColor> startColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("StartFill"))
                  .description("Start fill color"))
               .defaultValue(new SettingColor(255, 255, 255, 100))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<SettingColor> startOutlineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("StartOutline"))
                  .description("Start outline color"))
               .defaultValue(new SettingColor(255, 255, 255, 100))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<SettingColor> endColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("EndFill"))
                  .description("End fill color"))
               .defaultValue(new SettingColor(255, 255, 255, 100))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<SettingColor> endOutlineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("EndOutline"))
                  .description("End outline color"))
               .defaultValue(new SettingColor(255, 255, 255, 100))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<SettingColor> doubleColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("DoubleFill"))
                  .description("Double break fill color"))
               .defaultValue(new SettingColor(88, 94, 255, 100))
               .visible(() -> (Boolean)this.doubleBreak.get() && this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<SettingColor> doubleOutlineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("DoubleOutline"))
                  .description("Double break outline color"))
               .defaultValue(new SettingColor(88, 94, 255, 100))
               .visible(() -> (Boolean)this.doubleBreak.get() && this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<Boolean> text = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Text"))
                     .description("Show progress text"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<Boolean> box = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Box"))
                     .description("Show fill box"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<Boolean> outline = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Outline"))
                     .description("Show outline"))
                  .defaultValue(true))
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render))
            .build()
      );
   private final Setting<Double> maxTextScale = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("TextMaxScale"))
                  .description("mostbigTextTextScaleupLimit."))
               .defaultValue(15.0)
               .min(1.0)
               .max(100.0)
               .sliderRange(1.0, 50.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render && (Boolean)this.text.get()))
            .build()
      );
   private final Setting<Double> textScaleBase = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("TextScaleBase"))
                  .description("TextTextBasicScale(DistanceFor0Time'sSize). Default5.0"))
               .defaultValue(5.0)
               .min(0.5)
               .max(30.0)
               .sliderRange(0.5, 20.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render && (Boolean)this.text.get()))
            .build()
      );
   private final Setting<Double> textScaleFactor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("TextScaleFactor"))
                  .description("Distance'sScale. Default0.1"))
               .defaultValue(0.1)
               .min(0.0)
               .max(2.0)
               .sliderRange(0.0, 1.0)
               .visible(() -> this.page.get() == AlienV4PacketMine.Page.Render && (Boolean)this.text.get()))
            .build()
      );

   public AlienV4PacketMine() {
      super(AddonTemplate.CATEGORY, "AlienV4PacketMine", "AlienV4 style packet mine. Can pass through some blocks; functionality incomplete.");
      INSTANCE = this;
   }

   public static BlockPos getBreakPos() {
      return INSTANCE.isActive() ? INSTANCE.breakPos : null;
   }

   public String getInfoString() {
      return progress >= 1.0 ? "Done" : this.df.format(progress * 100.0) + "%";
   }

   public void onActivate() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
      secondPos = null;
   }

   public void onDeactivate() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
      secondPos = null;
      this.directionVec = null;
      this.switchBack = null;
   }

   private float[] getRotationTo(Vec3d vec) {
      double diffX = vec.x - this.mc.player.getX();
      double diffY = vec.y - (this.mc.player.getY() + this.mc.player.getEyeHeight(this.mc.player.getPose()));
      double diffZ = vec.z - this.mc.player.getZ();
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new float[]{yaw, pitch};
   }

   private void lookAt(Vec3d vec) {
      float[] rot = this.getRotationTo(vec);
      this.mc.player.setYaw(rot[0]);
      this.mc.player.setPitch(rot[1]);
   }

   private boolean inFov(Vec3d vec, float fovDeg) {
      float[] rot = this.getRotationTo(vec);
      float yawDiff = MathHelper.wrapDegrees(this.mc.player.getYaw() - rot[0]);
      float pitchDiff = MathHelper.wrapDegrees(this.mc.player.getPitch() - rot[1]);
      return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff) <= fovDeg;
   }

   private boolean shouldYawStep() {
      if (!(Boolean)this.yawStep.get()) {
         return false;
      } else {
         return this.whenElytra.get() ? true : !this.mc.player.getPose().name().equals("GLIDING");
      }
   }

   boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         this.lookAt(directionVec);
         return true;
      } else {
         this.sync.reset();
         this.directionVec = directionVec;
         return this.inFov(directionVec, ((Double)this.fov.get()).floatValue()) || !(Boolean)this.checkFov.get();
      }
   }

   private boolean isInWeb(PlayerEntity player) {
      BlockPos pos = player.getBlockPos();
      if (this.mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
         return true;
      } else {
         return this.mc.world.getBlockState(pos.up()).getBlock() == Blocks.COBWEB
            ? true
            : this.mc.world.getBlockState(pos.down()).getBlock() == Blocks.COBWEB;
      }
   }

   private void autoSwitch() {
      if ((Boolean)this.autoSwitch.get() && (Boolean)this.doubleBreak.get()) {
         int index = -1;
         if (secondPos != null) {
            float currentFastest = 1.0F;

            for (int i = 0; i < 9; i++) {
               ItemStack stack = this.mc.player.getInventory().getStack(i);
               if (stack != ItemStack.EMPTY) {
                  int eff = this.getEfficiencyLevel(stack);
                  float digSpeed = eff;
                  float destroySpeed = stack.getMiningSpeedMultiplier(this.mc.world.getBlockState(secondPos));
                  if (digSpeed + destroySpeed > currentFastest) {
                     currentFastest = digSpeed + destroySpeed;
                     index = i;
                  }
               }
            }
         }

         if (index != -1
            && !this.mc.options.useKey.isPressed()
            && !this.mc.options.attackKey.isPressed()
            && !this.mc.player.isUsingItem()
            && this.secondTimer.passedMs(this.getBreakTime(secondPos, index, (Double)this.start.get()))) {
            if (index != ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot()) {
               this.mainSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               AlienInventoryUtil.switchToSlot(index);
               this.swapped = true;
            }
         } else if (this.swapped) {
            AlienInventoryUtil.switchToSlot(this.mainSlot);
            this.swapped = false;
         }
      }
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.rotate.get() && this.shouldYawStep() && this.directionVec != null && !this.sync.passedMs(((Integer)this.syncTime.get()).intValue())) {
            this.lookAt(this.directionVec);
         }

         if (this.breakPos != null && this.mc.world.isAir(this.breakPos)) {
            complete = true;
         }

         if (secondPos != null) {
            int secondSlot = this.getTool(secondPos);
            if (secondSlot == -1) {
               secondSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            }

            this.secondFinalTime = this.getBreakTime(secondPos, secondSlot, 1.0);
            if (!this.isAir(secondPos) && !unbreakable(secondPos)) {
               if (this.secondTimer
                  .passedMs(
                     this.getBreakTime(secondPos, ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot(), 1.0) * (Double)this.timeOut.get()
                  )) {
                  secondPos = null;
               }
            } else {
               secondPos = null;
            }
         }

         if (this.switchBack != null) {
            if (!AlienEntityUtil.inInventory() && !(Boolean)this.hotBar.get()) {
               if (this.breakPos != null) {
                  this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, AlienBlockUtil.getClickSide(this.breakPos)));
               }

               this.breakNumber++;
               this.delayTimer.reset();
               this.startTime.reset();
            } else {
               this.switchBack.run();
            }

            this.switchBack = null;
         }

         if (this.mc.player.isDead()) {
            secondPos = null;
         }

         this.autoSwitch();
         if (this.mc.player.isCreative()) {
            this.startPacket = false;
            ghost = false;
            complete = false;
            this.breakNumber = 0;
            this.breakPos = null;
            progress = 0.0;
         } else if (this.breakPos == null) {
            this.breakNumber = 0;
            this.startPacket = false;
            ghost = false;
            complete = false;
            progress = 0.0;
         } else {
            int slot = this.getTool(this.breakPos);
            if (slot == -1) {
               slot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            }

            this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
            progress = this.mineTimer.getMs() / this.breakFinalTime;
            if (this.isAir(this.breakPos)) {
               this.breakNumber = 0;
            }

            boolean maxBreakReached = this.breakNumber > (Integer)this.maxBreak.get() - 1 && (Integer)this.maxBreak.get() > 0 && !complete;
            if (!maxBreakReached && ((Boolean)this.wait.get() || !this.isAir(this.breakPos) || (Boolean)this.instant.get())) {
               if (unbreakable(this.breakPos)) {
                  if ((Boolean)this.unbreakableCancel.get()) {
                     this.breakPos = null;
                     this.startPacket = false;
                     ghost = false;
                     complete = false;
                  }

                  this.breakNumber = 0;
               } else if (MathHelper.sqrt((float)this.mc.player.getEyePos().squaredDistanceTo(this.breakPos.toCenterPos()))
                  > (Double)this.range.get()) {
                  if ((Boolean)this.farCancel.get()) {
                     this.startPacket = false;
                     ghost = false;
                     complete = false;
                     this.breakNumber = 0;
                     this.breakPos = null;
                  }
               } else if ((
                     !(Boolean)this.usingPause.get()
                        || !this.mc.player.isUsingItem()
                        || (Boolean)this.allowOffhand.get() && this.mc.player.getActiveHand() != Hand.MAIN_HAND
                  )
                  && !(Boolean)this.pauseBind.get()
                  && ((Boolean)this.hotBar.get() || AlienEntityUtil.inInventory())) {
                  if (!this.isAir(this.breakPos)) {
                     if (this.canPlaceCrystal(this.breakPos.up()) && this.shouldCrystal()) {
                        if (this.placeTimer.passedMs(((Integer)this.placeDelay.get()).intValue())) {
                           if ((Boolean)this.checkDamage.get()) {
                              if (this.mineTimer.getMs() / this.breakFinalTime >= (Double)this.crystalDamage.get() && !this.placeCrystal()) {
                                 return;
                              }
                           } else if (!this.placeCrystal()) {
                              return;
                           }
                        } else if (this.startPacket) {
                           return;
                        }
                     }
                  } else {
                     if (this.shouldCrystal()) {
                        for (Direction facing : Direction.values()) {
                           AlienCombatUtil.attackCrystal(this.breakPos.offset(facing), (Boolean)this.placeRotate.get(), true);
                        }
                     }

                     if (this.placeTimer.passedMs(((Integer)this.placeDelay.get()).intValue())
                        && AlienBlockUtil.canPlace(this.breakPos)
                        && this.mc.currentScreen == null) {
                        if ((Boolean)this.enderChest.get()) {
                           int eChest = AlienInventoryUtil.findBlock(Blocks.ENDER_CHEST);
                           if (eChest != -1) {
                              int oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                              this.doSwap(eChest, eChest);
                              AlienBlockUtil.placeBlock(this.breakPos, (Boolean)this.placeRotate.get(), true);
                              this.doSwap(oldSlot, eChest);
                              this.placeTimer.reset();
                           }
                        } else if ((Boolean)this.obsidian.get()) {
                           int obby = AlienInventoryUtil.findBlock(Blocks.OBSIDIAN);
                           if (obby != -1) {
                              boolean hasCrystal = false;
                              if (this.shouldCrystal()) {
                                 for (Entity entity : AlienBlockUtil.getEntities(new Box(this.breakPos.up()))) {
                                    if (entity instanceof EndCrystalEntity) {
                                       hasCrystal = true;
                                       break;
                                    }
                                 }
                              }

                              if (!hasCrystal || (Boolean)this.spamPlace.get()) {
                                 int oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                                 this.doSwap(obby, obby);
                                 AlienBlockUtil.placeBlock(this.breakPos, (Boolean)this.placeRotate.get(), true);
                                 this.doSwap(oldSlot, obby);
                                 this.placeTimer.reset();
                              }
                           }
                        }
                     }

                     this.breakNumber = 0;
                  }

                  if (this.delayTimer.passed(((Double)this.stopDelay.get()).longValue())) {
                     if (this.startPacket) {
                        if (this.isAir(this.breakPos)) {
                           return;
                        }

                        if ((Boolean)this.onlyGround.get() && !this.mc.player.isOnGround()) {
                           return;
                        }

                        if (this.mineTimer.passed((long)this.breakFinalTime)) {
                           if ((Boolean)this.endRotate.get()
                              && this.shouldYawStep()
                              && !this.faceVector(this.breakPos.toCenterPos().offset(AlienBlockUtil.getClickSide(this.breakPos), 0.5))) {
                              return;
                           }

                           int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                           boolean shouldSwitch;
                           if ((Boolean)this.hotBar.get()) {
                              shouldSwitch = slot != old;
                           } else {
                              int invSlot = slot < 9 ? slot + 36 : slot;
                              shouldSwitch = old + 36 != invSlot;
                           }

                           if (shouldSwitch) {
                              if ((Boolean)this.hotBar.get()) {
                                 AlienInventoryUtil.switchToSlot(slot);
                              } else {
                                 int invSlot = slot < 9 ? slot + 36 : slot;
                                 this.mc
                                    .interactionManager
                                    .clickSlot(this.mc.player.currentScreenHandler.syncId, invSlot, old, SlotActionType.SWAP, this.mc.player);
                              }
                           }

                           int finalSlot = slot;
                           this.switchBack = () -> {
                              if (!(Boolean)this.endRotate.get()
                                 || this.faceVector(this.breakPos.toCenterPos().offset(AlienBlockUtil.getClickSide(this.breakPos), 0.5))) {
                                 this.mc
                                    .getNetworkHandler()
                                    .sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, AlienBlockUtil.getClickSide(this.breakPos)));
                                 if ((Boolean)this.endSwing.get()) {
                                    this.swingHand(Hand.MAIN_HAND, (AlienV4PacketMine.SwingHandMode)this.swingMode.get());
                                 }

                                 if (shouldSwitch) {
                                    if ((Boolean)this.hotBar.get()) {
                                       AlienInventoryUtil.switchToSlot(old);
                                    } else if (AlienEntityUtil.inInventory()) {
                                       int fs = finalSlot < 9 ? finalSlot + 36 : finalSlot;
                                       this.mc
                                          .interactionManager
                                          .clickSlot(this.mc.player.currentScreenHandler.syncId, fs, old, SlotActionType.SWAP, this.mc.player);
                                       AlienEntityUtil.syncInventory();
                                    } else if (old >= 0 && old <= 8) {
                                       AlienInventoryUtil.switchToSlot(old);
                                    }
                                 }

                                 this.breakNumber++;
                                 this.delayTimer.reset();
                                 this.startTime.reset();
                                 if ((Boolean)this.afterBreak.get() && this.shouldCrystal()) {
                                    for (Direction facing : Direction.values()) {
                                       AlienCombatUtil.attackCrystal(this.breakPos.offset(facing), (Boolean)this.placeRotate.get(), true);
                                    }
                                 }

                                 if ((Boolean)this.setAir.get()) {
                                    this.mc.world.setBlockState(this.breakPos, Blocks.AIR.getDefaultState());
                                 }

                                 ghost = true;
                              } else if (shouldSwitch) {
                                 if ((Boolean)this.hotBar.get()) {
                                    AlienInventoryUtil.switchToSlot(old);
                                 } else if (AlienEntityUtil.inInventory()) {
                                    int fs = finalSlot < 9 ? finalSlot + 36 : finalSlot;
                                    this.mc
                                       .interactionManager
                                       .clickSlot(this.mc.player.currentScreenHandler.syncId, fs, old, SlotActionType.SWAP, this.mc.player);
                                    AlienEntityUtil.syncInventory();
                                 } else if (old >= 0 && old <= 8) {
                                    AlienInventoryUtil.switchToSlot(old);
                                 }
                              }
                           };
                           if (!(Boolean)this.noGhostHand.get()) {
                              this.switchBack.run();
                              this.switchBack = null;
                           }
                        }
                     } else {
                        if (!this.startTime.passed(((Double)this.startDelay.get()).intValue())) {
                           return;
                        }

                        if (!(Boolean)this.mineAir.get() && this.isAir(this.breakPos)) {
                           return;
                        }

                        Direction side = AlienBlockUtil.getClickSide(this.breakPos);
                        if ((Boolean)this.rotate.get()) {
                           Vec3i vec3i = side.getVector();
                           if (!this.faceVector(
                              this.breakPos
                                 .toCenterPos()
                                 .add(new Vec3d(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5))
                           )) {
                              return;
                           }
                        }

                        this.mineTimer.reset();
                        this.animationTime.reset();
                        if ((Boolean)this.swing.get()) {
                           this.swingHand(Hand.MAIN_HAND, (AlienV4PacketMine.SwingHandMode)this.swingMode.get());
                        }

                        if ((Boolean)this.doubleBreak.get()) {
                           if (secondPos == null || this.isAir(secondPos)) {
                              double breakTime = this.getBreakTime(this.breakPos, slot, 1.0);
                              this.secondAnim.reset();
                              this.secondAnim.setLength((long)breakTime);
                              this.secondTimer.reset();
                              secondPos = this.breakPos;
                           }

                           this.doDoubleBreak(side);
                        }

                        this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side));
                        this.startTime.reset();
                     }
                  }
               }
            } else {
               if (this.breakPos.equals(secondPos)) {
                  secondPos = null;
               }

               this.startPacket = false;
               ghost = false;
               complete = false;
               this.breakNumber = 0;
               this.breakPos = null;
            }
         }
      }
   }

   @EventHandler
   public void onStartBreakingBlock(StartBreakingBlockEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.mc.player.isCreative()) {
            event.cancel();
            BlockPos pos = event.blockPos;
            if (!pos.equals(this.breakPos)) {
               if (!unbreakable(pos)) {
                  if ((this.breakPos == null || !(Boolean)this.preferWeb.get() || AlienBlockUtil.getBlock(this.breakPos) != Blocks.COBWEB)
                     && (
                        this.breakPos == null
                           || !(Boolean)this.preferHead.get()
                           || !this.mc.player.isCrawling()
                           || !AlienEntityUtil.getPlayerPos(true).up().equals(this.breakPos)
                     )) {
                     if (AlienBlockUtil.getClickSideStrict(pos) == null) {
                        return;
                     }

                     if (MathHelper.sqrt((float)this.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos())) > (Double)this.range.get()) {
                        return;
                     }

                     this.breakPos = pos;
                     this.breakNumber = 0;
                     this.startPacket = false;
                     ghost = false;
                     complete = false;
                     this.mineTimer.reset();
                     this.animationTime.reset();
                     Direction side = AlienBlockUtil.getClickSide(this.breakPos);
                     if ((Boolean)this.rotate.get()) {
                        Vec3i vec3i = side.getVector();
                        if (!this.faceVector(
                           this.breakPos
                              .toCenterPos()
                              .add(new Vec3d(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5))
                        )) {
                           return;
                        }
                     }

                     if (this.startTime.passed(((Double)this.startDelay.get()).intValue())) {
                        if ((Boolean)this.swing.get()) {
                           this.swingHand(Hand.MAIN_HAND, (AlienV4PacketMine.SwingHandMode)this.swingMode.get());
                        }

                        if ((Boolean)this.doubleBreak.get()) {
                           if (secondPos == null || this.isAir(secondPos)) {
                              int s = this.getTool(this.breakPos);
                              if (s == -1) {
                                 s = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                              }

                              this.secondFinalTime = this.getBreakTime(this.breakPos, s, 1.0);
                              this.secondAnim.reset();
                              this.secondAnim.setLength((long)this.secondFinalTime);
                              this.secondTimer.reset();
                              secondPos = this.breakPos;
                           }

                           this.doDoubleBreak(side);
                        }

                        int s = this.getTool(this.breakPos);
                        if (s == -1) {
                           s = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                        }

                        this.breakFinalTime = this.getBreakTime(this.breakPos, s);
                        this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side));
                        this.startTime.reset();
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacketSend(Send event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.mc.player.isCreative()) {
            if (event.packet instanceof PlayerMoveC2SPacket) {
               if ((Boolean)this.bypassGround.get()
                  && !this.mc.player.getPose().name().equals("GLIDING")
                  && this.breakPos != null
                  && !this.isAir(this.breakPos)
                  && (Integer)this.bypassTime.get() > 0
                  && MathHelper.sqrt((float)this.breakPos.toCenterPos().squaredDistanceTo(this.mc.player.getEyePos()))
                     <= ((Double)this.range.get()).floatValue() + 2.0F) {
                  double breakTime = this.breakFinalTime - ((Integer)this.bypassTime.get()).intValue();
                  if (breakTime <= 0.0 || this.mineTimer.passed((long)breakTime)) {
                     this.sendGroundPacket = true;
                  }
               } else {
                  this.sendGroundPacket = false;
               }
            } else if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
               if (packet.getSelectedSlot() != this.lastSlot) {
                  this.lastSlot = packet.getSelectedSlot();
                  if ((Boolean)this.switchReset.get()) {
                     this.startPacket = false;
                     ghost = false;
                     complete = false;
                     this.mineTimer.reset();
                     this.animationTime.reset();
                  }
               }
            } else if (event.packet instanceof PlayerActionC2SPacket packetx) {
               if (packetx.getAction() == Action.START_DESTROY_BLOCK) {
                  if (this.breakPos == null || !packetx.getPos().equals(this.breakPos)) {
                     return;
                  }

                  if ((Boolean)this.grimDisabler.get()) {
                     this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, packetx.getPos(), packetx.getDirection()));
                  }

                  this.startPacket = true;
               } else if (packetx.getAction() == Action.STOP_DESTROY_BLOCK) {
                  if (this.breakPos == null || !packetx.getPos().equals(this.breakPos)) {
                     return;
                  }

                  if (!(Boolean)this.instant.get()) {
                     this.startPacket = false;
                     ghost = false;
                     complete = false;
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.breakPos != null && this.mc.world.isAir(this.breakPos)) {
            complete = true;
         }

         if (this.mc.player.isCreative()) {
            progress = 0.0;
         } else {
            if (secondPos != null) {
               if (this.isAir(secondPos)) {
                  secondPos = null;
                  return;
               }

               if (!(Boolean)this.checkDouble.get() || !secondPos.equals(this.breakPos)) {
                  this.secondAnim.setLength((long)this.secondFinalTime);
                  double easeVal = this.secondAnim.ease((AlienEasing)this.ease.get());
                  if ((Boolean)this.box.get()) {
                     AlienRender3DUtil.drawFill(event, this.getFillBox(secondPos, easeVal), this.toAwt((SettingColor)this.doubleColor.get()));
                  }

                  if ((Boolean)this.outline.get()) {
                     AlienRender3DUtil.drawBox(event, this.getOutlineBox(secondPos, easeVal), this.toAwt((SettingColor)this.doubleOutlineColor.get()));
                  }
               }
            }

            if (this.breakPos != null) {
               progress = this.mineTimer.getMs() / this.breakFinalTime;
               this.animationTime.setLength((long)this.breakFinalTime);
               double easeValx = this.animationTime.ease((AlienEasing)this.ease.get());
               if (unbreakable(this.breakPos)) {
                  if ((Boolean)this.box.get()) {
                     AlienRender3DUtil.drawFill(event, new Box(this.breakPos), this.toAwt((SettingColor)this.startColor.get()));
                  }

                  if ((Boolean)this.outline.get()) {
                     AlienRender3DUtil.drawBox(event, new Box(this.breakPos), this.toAwt((SettingColor)this.startOutlineColor.get()));
                  }

                  return;
               }

               double fadeVal = this.animationTime.ease((AlienEasing)this.fadeEase.get());
               if ((Boolean)this.box.get()) {
                  AlienRender3DUtil.drawFill(event, this.getFillBox(this.breakPos, easeValx), this.toAwt(this.getColor(fadeVal)));
               }

               if ((Boolean)this.outline.get()) {
                  AlienRender3DUtil.drawBox(event, this.getOutlineBox(this.breakPos, easeValx), this.toAwt(this.getOutlineColor(fadeVal)));
               }

               if ((Boolean)this.text.get()) {
                  Vec3d textPos = this.breakPos.toCenterPos();
                  String progressText;
                  if (this.isAir(this.breakPos)) {
                     progressText = "Waiting";
                  } else if (this.mineTimer.getMs() < this.breakFinalTime) {
                     progressText = this.df.format(progress * 100.0) + "%";
                  } else {
                     progressText = "100.0%";
                  }

                  AlienRender3DUtil.drawText3D(
                     progressText, textPos, (Double)this.textScaleBase.get(), (Double)this.textScaleFactor.get(), (Double)this.maxTextScale.get(), -1
                  );
               }
            } else {
               progress = 0.0;
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      AlienRender3DUtil.renderDeferred();
   }

   private void swingHand(Hand hand, AlienV4PacketMine.SwingHandMode mode) {
      switch (mode) {
         case All:
            this.mc.player.swingHand(hand);
            break;
         case Client:
            this.mc.player.swingHand(hand, false);
            break;
         case Server:
            this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
      }
   }

   private Box getFillBox(BlockPos pos, double easeVal) {
      return switch ((AlienV4PacketMine.AnimMode)this.animation.get()) {
         case Center -> {
            easeVal = (1.0 - easeVal) / 2.0;
            yield new Box(pos).shrink(easeVal, easeVal, easeVal).shrink(-easeVal, -easeVal, -easeVal);
         }
         case Grow -> {
            easeVal = (1.0 - easeVal) / 2.0;
            yield new Box(pos).shrink(easeVal, 0.0, easeVal).shrink(-easeVal, 0.0, -easeVal);
         }
         case Up -> new Box(
            pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + easeVal, pos.getZ() + 1
         );
         case Down -> new Box(
            pos.getX(), pos.getY() + 1 - easeVal, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
         );
         case Oscillation -> new Box(pos).shrink(easeVal, easeVal, easeVal).shrink(-easeVal, -easeVal, -easeVal);
         case None -> new Box(pos);
      };
   }

   private Box getOutlineBox(BlockPos pos, double easeVal) {
      easeVal = Math.min(easeVal + (Double)this.expandLine.get(), 1.0);

      return switch ((AlienV4PacketMine.AnimMode)this.animation.get()) {
         case Center -> {
            easeVal = (1.0 - easeVal) / 2.0;
            yield new Box(pos).shrink(easeVal, easeVal, easeVal).shrink(-easeVal, -easeVal, -easeVal);
         }
         case Grow -> {
            easeVal = (1.0 - easeVal) / 2.0;
            yield new Box(pos).shrink(easeVal, 0.0, easeVal).shrink(-easeVal, 0.0, -easeVal);
         }
         case Up -> new Box(
            pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + easeVal, pos.getZ() + 1
         );
         case Down -> new Box(
            pos.getX(), pos.getY() + 1 - easeVal, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
         );
         case Oscillation -> new Box(pos).shrink(easeVal, easeVal, easeVal).shrink(-easeVal, -easeVal, -easeVal);
         case None -> new Box(pos);
      };
   }

   boolean canPlaceCrystal(BlockPos pos) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (AlienBlockUtil.getBlock(obsPos) == Blocks.BEDROCK || AlienBlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN)
         && AlienBlockUtil.getClickSideStrict(obsPos) != null
         && this.noEntity(boost)
         && this.noEntity(boost.up());
   }

   boolean noEntity(BlockPos pos) {
      for (Entity entity : AlienBlockUtil.getEntities(new Box(pos))) {
         if (!(entity instanceof ItemEntity) && !(entity instanceof ArmorStandEntity)) {
            return false;
         }
      }

      return true;
   }

   boolean shouldCrystal() {
      return (Boolean)this.crystal.get() && (!(Boolean)this.onlyHeadBomber.get() || (Boolean)this.obsidian.get());
   }

   boolean placeCrystal() {
      int crystalSlot = this.inventory.get() ? AlienInventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : AlienInventoryUtil.findItem(Items.END_CRYSTAL);
      if (crystalSlot != -1) {
         int oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
         this.doSwap(crystalSlot, crystalSlot);
         AlienBlockUtil.placeCrystal(this.breakPos.up(), (Boolean)this.placeRotate.get());
         this.doSwap(oldSlot, crystalSlot);
         this.placeTimer.reset();
         return !(Boolean)this.waitPlace.get();
      } else {
         return true;
      }
   }

   void doSwap(int slot, int inv) {
      if (!(Boolean)this.inventory.get()) {
         if (slot < 0 || slot > 8) {
            return;
         }

         AlienInventoryUtil.switchToSlot(slot);
      } else {
         if (inv == -1) {
            return;
         }

         AlienInventoryUtil.inventorySwap(inv, ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot());
      }
   }

   void doDoubleBreak(Direction side) {
      this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, this.breakPos, side));
      this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, side));
   }

   public static double getBreakTime(BlockPos pos) {
      int slot = INSTANCE.getTool(pos);
      if (slot == -1) {
         slot = ((InventoryAccessor)INSTANCE.mc.player.getInventory()).getSelectedSlot();
      }

      return INSTANCE.getBreakTime(pos, slot);
   }

   double getBreakTime(BlockPos pos, int slot) {
      return this.getBreakTime(pos, slot, (Double)this.damage.get());
   }

   double getBreakTime(BlockPos pos, int slot, double damageMul) {
      return 1.0F / this.getBlockStrength(pos, this.mc.player.getInventory().getStack(slot)) / 20.0F * 1000.0F * damageMul;
   }

   float getBlockStrength(BlockPos position, ItemStack itemStack) {
      BlockState state = this.mc.world.getBlockState(position);
      float hardness = state.getHardness(this.mc.world, position);
      if (hardness < 0.0F) {
         return 0.0F;
      } else {
         float i = state.isToolRequired() && !itemStack.isSuitableFor(state) ? 100.0F : 30.0F;
         return this.getDigSpeed(state, itemStack) / hardness / i;
      }
   }

   float getDigSpeed(BlockState state, ItemStack itemStack) {
      float digSpeed = this.getDestroySpeed(state, itemStack);
      if (digSpeed > 1.0F) {
         int efficiencyModifier = this.getEfficiencyLevel(itemStack);
         if (efficiencyModifier > 0 && !itemStack.isEmpty()) {
            digSpeed += (float)(StrictMath.pow(efficiencyModifier, 2.0) + 1.0);
         }
      }

      if (this.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
         digSpeed *= 1.0F + (this.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
      }

      if (this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         digSpeed *= switch (this.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
      }

      if (this.mc.player.isSubmergedInWater()) {
         digSpeed *= (float)this.mc.player.getAttributeValue(EntityAttributes.SUBMERGED_MINING_SPEED);
      }

      boolean inWeb = (Boolean)this.checkWeb.get()
         && this.isInWeb(this.mc.player)
         && this.mc.world.getBlockState(this.breakPos).getBlock() == Blocks.COBWEB;
      if ((!this.mc.player.isOnGround() || inWeb)
         && (Boolean)this.checkGround.get()
         && (!(Boolean)this.smart.get() || this.mc.player.getPose().name().equals("GLIDING") || inWeb)) {
         digSpeed /= 5.0F;
      }

      return digSpeed < 0.0F ? 0.0F : digSpeed;
   }

   float getDestroySpeed(BlockState state, ItemStack itemStack) {
      float destroySpeed = 1.0F;
      if (itemStack != null && !itemStack.isEmpty()) {
         destroySpeed *= itemStack.getMiningSpeedMultiplier(state);
      }

      return destroySpeed;
   }

   int getEfficiencyLevel(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         try {
            ItemEnchantmentsComponent enchantments = stack.getEnchantments();
            if (enchantments == null) {
               return 0;
            }

            for (Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
               if (entry != null && entry.getKey() != null) {
                  String idStr = ((RegistryEntry)entry.getKey())
                     .getKey()
                     .map(k -> k.getValue().toString())
                     .orElse(((RegistryEntry)entry.getKey()).toString().toLowerCase());
                  if (idStr.contains("efficiency")) {
                     return entry.getIntValue();
                  }
               }
            }
         } catch (Exception var6) {
         }

         return 0;
      } else {
         return 0;
      }
   }

   int getTool(BlockPos pos) {
      if ((Boolean)this.hotBar.get()) {
         int index = -1;
         float currentFastest = 1.0F;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (stack != ItemStack.EMPTY) {
               int eff = this.getEfficiencyLevel(stack);
               float destroySpeed = stack.getMiningSpeedMultiplier(this.mc.world.getBlockState(pos));
               if (eff + destroySpeed > currentFastest) {
                  currentFastest = eff + destroySpeed;
                  index = i;
               }
            }
         }

         return index;
      } else {
         AtomicInteger slot = new AtomicInteger(-1);
         float currentFastest = 1.0F;

         for (java.util.Map.Entry<Integer, ItemStack> entry : AlienInventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
            if (!(entry.getValue().getItem() instanceof AirBlockItem)) {
               int eff = this.getEfficiencyLevel(entry.getValue());
               float destroySpeed = entry.getValue().getMiningSpeedMultiplier(this.mc.world.getBlockState(pos));
               if (eff + destroySpeed > currentFastest) {
                  currentFastest = eff + destroySpeed;
                  slot.set(entry.getKey());
               }
            }
         }

         return slot.get();
      }
   }

   boolean isAir(BlockPos breakPos) {
      return this.mc.world.isAir(breakPos) || AlienBlockUtil.getBlock(breakPos) == Blocks.FIRE && AlienBlockUtil.hasCrystal(breakPos);
   }

   public static boolean unbreakable(BlockPos blockPos) {
      if (INSTANCE != null && INSTANCE.mc.world != null) {
         Block block = INSTANCE.mc.world.getBlockState(blockPos).getBlock();
         return !(block instanceof AirBlock) && (block.getHardness() == -1.0F || block.getHardness() == 100.0F);
      } else {
         return true;
      }
   }

   SettingColor getColor(double quad) {
      SettingColor sc = (SettingColor)this.startColor.get();
      SettingColor ec = (SettingColor)this.endColor.get();
      return new SettingColor(
         (int)(sc.r + (ec.r - sc.r) * quad), (int)(sc.g + (ec.g - sc.g) * quad), (int)(sc.b + (ec.b - sc.b) * quad), (int)(sc.a + (ec.a - sc.a) * quad)
      );
   }

   SettingColor getOutlineColor(double quad) {
      SettingColor sc = (SettingColor)this.startOutlineColor.get();
      SettingColor ec = (SettingColor)this.endOutlineColor.get();
      return new SettingColor(
         (int)(sc.r + (ec.r - sc.r) * quad), (int)(sc.g + (ec.g - sc.g) * quad), (int)(sc.b + (ec.b - sc.b) * quad), (int)(sc.a + (ec.a - sc.a) * quad)
      );
   }

   private Color toAwt(SettingColor c) {
      return new Color(c.r, c.g, c.b, c.a);
   }

   public static enum AnimMode {
      Center,
      Grow,
      Up,
      Down,
      Oscillation,
      None;
   }

   public static enum Page {
      General,
      Check,
      Rotation,
      Place,
      Render;
   }

   public static enum SwingHandMode {
      All,
      Client,
      Server;
   }

   public static enum TimingMode {
      Pre,
      Post,
      All;
   }
}
