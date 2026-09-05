package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.BlockPosX;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.EntityUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Render3DUtil;
import com.codigohasta.addon.utils.leaveshack.events.RenderLeaves3DEvent;
import java.util.TimerTask;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class PacketMinePlus extends Module {
   public static PacketMinePlus INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("UsingPause")).description("UsePause")).defaultValue(true)).build());
   private final Setting<Boolean> onlyMain = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("OnlyMain")).description("CheckHand")).defaultValue(true)).visible(this.usingPause::get))
            .build()
      );
   public final Setting<InventoryUtil.MineSwitchMode> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("AutoSwitch"))
                  .description("AutoSwitch"))
               .defaultValue(InventoryUtil.MineSwitchMode.Silent))
            .build()
      );
   public final Setting<Integer> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Range"))
                  .description("Reach Distance"))
               .defaultValue(6))
            .min(0)
            .sliderMax(12)
            .build()
      );
   public final Setting<Integer> maxBreaks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("TryBreakTime"))
                  .description("mostbigMineTimesNumber"))
               .defaultValue(6))
            .min(0)
            .sliderMax(10)
            .build()
      );
   private final Setting<Boolean> farCancel = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("FarCancel")).description("pastDisappear")).defaultValue(true)).build());
   private final Setting<Boolean> swing = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("SwingHand")).description("Hand")).defaultValue(true)).build());
   private final Setting<Boolean> instantMine = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("InstantMine")).description("Instant Mine")).defaultValue(false)).build());
   private final Setting<Integer> instantDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("InstantDelay"))
                  .description("Instant MineDelay"))
               .defaultValue(10))
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Boolean> fastBypass = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("FastBypass")).description("fastMinepast")).defaultValue(true)).build());
   private final Setting<Boolean> doubleBreak = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("DoubleBreak")).description("Double Mine")).defaultValue(false)).build());
   private final Setting<Boolean> checkGround = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("CheckGround")).description("CheckwhetheratGroundFaceup")).defaultValue(true)).build());
   private final Setting<Boolean> bypassGround = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("BypassGround")).description("AirMinepast")).defaultValue(false)).build());
   private final Setting<Integer> switchDamage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("SwitchDamage"))
                  .description("AutoSwitchMineEnterDegreeThreshold"))
               .defaultValue(95))
            .min(0)
            .sliderMax(100)
            .build()
      );
   private final Setting<Integer> switchTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("SwitchTime"))
                  .description("HoldTime"))
               .defaultValue(100))
            .min(0)
            .sliderMax(1000)
            .build()
      );
   public final Setting<Integer> mineDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("MineDelay"))
                  .description("MineSelectDelay"))
               .defaultValue(300))
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Integer> packetDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("PacketDelay"))
                  .description("pastPackSendDelay"))
               .defaultValue(0))
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Double> mineDamage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Damage"))
               .description("MineEnterDegreeSetting"))
            .defaultValue(0.8)
            .sliderMax(2.0)
            .build()
      );
   private final Setting<Double> animationExp = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("Animation Exponent"))
            .defaultValue(3.0)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .build()
      );
   private final Setting<Boolean> renderProgress = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("RenderProgress")).description("RenderEnterDegree")).defaultValue(true)).build());
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("TargetColor"))
               .description("TextColor"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );
   private final Setting<SettingColor> secondColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("SecondColor"))
               .description("TextColor"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("ShapeMode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("SideStart"))
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
      );
   private final Setting<SettingColor> sideEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("SideEnd"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );
   private final Setting<SettingColor> lineStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("LineStart"))
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
      );
   private final Setting<SettingColor> lineEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("LineEnd"))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<SettingColor> secondSideStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("SecondSideStart"))
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
      );
   private final Setting<SettingColor> secondSideEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("SecondSideEnd"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );
   private final Setting<SettingColor> secondLineStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("SecondLineStart"))
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
      );
   private final Setting<SettingColor> secondLineEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("SecondLineEnd"))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   public static BlockPos selfClickPos = null;
   public static int maxBreaksCount;
   public static int publicProgress = 0;
   public static int secondPublicProgress = 0;
   public static boolean completed = false;
   public static BlockPos targetPos;
   public static BlockPos secondPos;
   private static float progress;
   private static float secondProgress;
   private long lastTime;
   private long secondLastTime;
   private static boolean started;
   private static boolean secondStarted;
   private double render = 1.0;
   private double secondRender = 1.0;
   private int oldSlot = -1;
   private final Timer bypassTimer = new Timer();
   private final Timer timer = new Timer();
   private final Timer secondTimer = new Timer();
   public final Timer mineTimer = new Timer();
   private final Timer instantTimer = new Timer();
   private boolean hasSwitch = false;
   private boolean secondHasSwitch = false;

   public PacketMinePlus() {
      super(AddonTemplate.CATEGORY, "PacketMinePlus", "Packet-based mining: breaks blocks server-side with auto tool switch and instant-mine support.");
      INSTANCE = this;
   }

   public void onActivate() {
      maxBreaksCount = 0;
      this.hasSwitch = false;
      this.secondHasSwitch = false;
      this.bypassTimer.setMs(999999L);
      this.mineTimer.setMs(999999L);
      this.instantTimer.setMs(999999L);
      this.timer.setMs(999999L);
      this.secondTimer.setMs(999999L);
      targetPos = null;
      secondPos = null;
      started = false;
      secondStarted = false;
      publicProgress = 0;
      secondPublicProgress = 0;
      progress = 0.0F;
      secondProgress = 0.0F;
      this.lastTime = System.currentTimeMillis();
      this.secondLastTime = System.currentTimeMillis();
      this.render = 1.0;
   }

   public void onDeactivate() {
      if (this.hasSwitch) {
         InventoryUtil.switchToSlot(this.oldSlot);
         this.hasSwitch = false;
      }

      if (this.secondHasSwitch) {
         InventoryUtil.switchToSlot(this.oldSlot);
         this.secondHasSwitch = false;
      }
   }

   @EventHandler
   private void onStartBreakingBlock(StartBreakingBlockEvent event) {
      if (BlockUtils.canBreak(event.blockPos)) {
         event.cancel();
         if (this.mineTimer.passedMs((long)((Integer)this.mineDelay.get()).intValue())) {
            selfClickPos = event.blockPos;
            this.mine(event.blockPos);
         }
      }
   }

   public void mine(BlockPos pos) {
      if (!AutoCity.INSTANCE.isActive() || !(Boolean)AutoCity.INSTANCE.delay.get() || this.mineTimer.passedMs((long)((Integer)this.mineDelay.get()).intValue())
         )
       {
         this.mineTimer.reset();
         maxBreaksCount = 0;
         if ((Boolean)this.doubleBreak.get()) {
            if (targetPos != null && secondPos == null && !targetPos.equals(pos)) {
               if (completed) {
                  if ((Integer)this.mineDelay.get() > 0) {
                     this.mineTimer.reset();
                     targetPos = null;
                     publicProgress = 0;
                     started = false;
                     progress = 0.0F;
                     completed = false;
                     return;
                  }

                  targetPos = pos;
                  secondStarted = false;
                  secondProgress = 0.0F;
                  secondPublicProgress = 0;
                  publicProgress = 0;
                  started = false;
                  progress = 0.0F;
                  completed = false;
               } else {
                  secondPos = targetPos;
                  targetPos = pos;
                  secondStarted = false;
                  secondProgress = 0.0F;
                  secondPublicProgress = 0;
                  started = false;
               }
            } else if (targetPos == null || !targetPos.equals(pos)) {
               publicProgress = 0;
               targetPos = pos;
               started = false;
               progress = 0.0F;
               completed = false;
            }
         } else if (!pos.equals(targetPos)) {
            publicProgress = 0;
            targetPos = pos;
            started = false;
            progress = 0.0F;
            completed = false;
         }
      }
   }

   public String getInfoString() {
      if (targetPos == null) {
         return null;
      } else {
         double max = this.getMineTicks(this.getTool(targetPos));
         return progress >= max * this.mineDamage.get() ? "§f[100%]" : "§f[" + publicProgress + "%]";
      }
   }

   @EventHandler
   private void onMyRender(RenderLeaves3DEvent event) {
      if ((Boolean)this.renderProgress.get()) {
         if (targetPos != null) {
            Render3DUtil.renderText3D(completed ? "Done" : publicProgress + "%", targetPos.toCenterPos(), ((SettingColor)this.targetColor.get()).getPacked());
         }

         if (secondPos != null) {
            Render3DUtil.renderText3D(secondPublicProgress + "%", secondPos.toCenterPos(), ((SettingColor)this.secondColor.get()).getPacked());
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         if (targetPos == null && secondPos == null) {
            selfClickPos = null;
         }

         if (publicProgress >= 100 && !(Boolean)this.instantMine.get()) {
            targetPos = null;
         }

         if (secondPublicProgress >= 100) {
            secondPos = null;
         }

         if (this.timer.passedMs((long)((Integer)this.switchTime.get()).intValue())
            && this.hasSwitch
            && this.autoSwitch.get() != InventoryUtil.MineSwitchMode.None) {
            if (this.autoSwitch.get() == InventoryUtil.MineSwitchMode.Delay) {
               InventoryUtil.switchToSlot(this.oldSlot);
            }

            if (this.autoSwitch.get() == InventoryUtil.MineSwitchMode.Silent) {
               InventoryUtil.sendPacket(new UpdateSelectedSlotC2SPacket(this.oldSlot));
            }

            this.hasSwitch = false;
         }

         if (maxBreaksCount >= (Integer)this.maxBreaks.get() * 10) {
            maxBreaksCount = 0;
            targetPos = null;
         }

         if (secondPos != null && (Boolean)this.doubleBreak.get()) {
            if ((Boolean)this.farCancel.get()
               && Math.sqrt(this.mc.player.getEyePos().squaredDistanceTo(secondPos.toCenterPos())) > ((Integer)this.range.get()).intValue()) {
               secondPos = null;
               return;
            }

            double secondMax = this.getMineTicks2(this.getTool(secondPos));
            double secondDelta = (System.currentTimeMillis() - this.secondLastTime) / 1000.0;
            secondPublicProgress = (int)(secondProgress / (secondMax * (Double)this.mineDamage.get()) * 100.0);
            this.secondLastTime = System.currentTimeMillis();
            if (!secondStarted) {
               this.sendStart(secondPos);
               secondStarted = true;
               secondProgress = 0.0F;
               return;
            }

            Double secondDamage = (Double)this.mineDamage.get();
            if (!(Boolean)this.checkGround.get() || this.mc.player.isOnGround()) {
               secondProgress = (float)(secondProgress + secondDelta * 20.0);
            } else if ((Boolean)this.checkGround.get() && !this.mc.player.isOnGround()) {
               secondProgress = (float)(secondProgress + secondDelta * 4.0);
            }

            this.renderSecondAnimation(event, secondDelta, secondDamage);
            if (secondProgress >= secondMax * secondDamage) {
               this.sendStopSecond();
            }
         }

         if ((Boolean)this.doubleBreak.get()
            && (!(Boolean)this.usingPause.get() || !this.checkPause((Boolean)this.onlyMain.get()))
            && (secondPublicProgress >= (Integer)this.switchDamage.get() || publicProgress >= (Integer)this.switchDamage.get())
            && !this.hasSwitch
            && secondPos != null) {
            int bestSlot = this.getTool(secondPos);
            if (!this.hasSwitch) {
               this.oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            }

            if (this.autoSwitch.get() != InventoryUtil.MineSwitchMode.None && bestSlot != -1) {
               if (this.autoSwitch.get() == InventoryUtil.MineSwitchMode.Delay) {
                  InventoryUtil.switchToSlot(bestSlot);
               }

               if (this.autoSwitch.get() == InventoryUtil.MineSwitchMode.Silent) {
                  InventoryUtil.sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
               }

               this.timer.reset();
               this.hasSwitch = true;
            }
         }

         if (targetPos != null) {
            if ((Boolean)this.farCancel.get()
               && Math.sqrt(this.mc.player.getEyePos().squaredDistanceTo(targetPos.toCenterPos())) > ((Integer)this.range.get()).intValue()) {
               targetPos = null;
               return;
            }

            double max = this.getMineTicks(this.getTool(targetPos));
            publicProgress = (int)(progress / (max * (Double)this.mineDamage.get()) * 100.0);
            if (progress >= max * (Double)this.mineDamage.get() && completed) {
               if (this.isAir(targetPos) || this.mc.world.getBlockState(targetPos).isReplaceable()) {
                  maxBreaksCount = 0;
               }

               if (!this.isAir(targetPos)
                  && !this.mc.world.getBlockState(targetPos).isReplaceable()
                  && (!(Boolean)this.usingPause.get() || !this.checkPause((Boolean)this.onlyMain.get()))) {
                  maxBreaksCount++;
               }
            }

            if ((Boolean)this.instantMine.get() && completed) {
               Color side = this.getColor((Color)this.sideStartColor.get(), (Color)this.sideEndColor.get(), 1.0);
               Color line = this.getColor((Color)this.lineStartColor.get(), (Color)this.lineEndColor.get(), 1.0);
               event.renderer.box(new Box(targetPos), side, line, (ShapeMode)this.shapeMode.get(), 0);
               if (!this.mc.world.isAir(targetPos)
                  && !this.mc.world.getBlockState(targetPos).isReplaceable()
                  && this.instantTimer.passedMs((long)((Integer)this.instantDelay.get()).intValue())) {
                  this.sendStop();
                  this.instantTimer.reset();
               }

               return;
            }

            double delta = (System.currentTimeMillis() - this.lastTime) / 1000.0;
            this.lastTime = System.currentTimeMillis();
            if (!started) {
               this.sendStart(targetPos);
               return;
            }

            Double damage = (Double)this.mineDamage.get();
            if (!(Boolean)this.checkGround.get() || this.mc.player.isOnGround()) {
               progress = (float)(progress + delta * 20.0);
            } else if ((Boolean)this.checkGround.get() && !this.mc.player.isOnGround()) {
               progress = (float)(progress + delta * 4.0);
            }

            this.renderAnimation(event, delta, damage);
            if (progress >= max * damage) {
               this.sendStop();
               completed = true;
               if (!(Boolean)this.instantMine.get() && secondPos == null) {
                  targetPos = null;
               }
            }
         }
      }
   }

   private void sendStart(BlockPos pos) {
      InventoryUtil.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, BlockUtil.getClickSide(pos)));
      if ((Boolean)this.fastBypass.get()) {
         BlockPos bypassPos = new BlockPosX(this.mc.player.getX(), 321.0, this.mc.player.getZ());
         this.sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, bypassPos, Direction.DOWN, id));
      }

      if ((Boolean)this.doubleBreak.get()) {
         long delay = ((Integer)this.packetDelay.get()).intValue();
         final java.util.Timer timer = new java.util.Timer();
         timer.schedule(new TimerTask() {
            @Override
            public void run() {
               PacketMinePlus.this.mc.execute(() -> InventoryUtil.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, BlockUtil.getClickSide(pos))));
               timer.cancel();
            }
         }, delay);
      }

      this.mc.player.swingHand(Hand.MAIN_HAND);
      if (pos.equals(targetPos)) {
         started = true;
         progress = 0.0F;
      } else {
         secondStarted = true;
         secondProgress = 0.0F;
      }
   }

   private void sendStop() {
      if (!(Boolean)this.usingPause.get() || !this.checkPause((Boolean)this.onlyMain.get())) {
         if (!(Boolean)this.doubleBreak.get() || secondPos == null) {
            int bestSlot = this.getTool(targetPos);
            if (!this.hasSwitch) {
               this.oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            }

            if (this.autoSwitch.get() != InventoryUtil.MineSwitchMode.None && bestSlot != -1) {
               if (this.autoSwitch.get() == InventoryUtil.MineSwitchMode.Delay) {
                  InventoryUtil.switchToSlot(bestSlot);
               }

               if (this.autoSwitch.get() == InventoryUtil.MineSwitchMode.Silent) {
                  InventoryUtil.sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
               }

               this.timer.reset();
               this.hasSwitch = true;
            }
         }

         if ((Boolean)this.bypassGround.get()
            && !this.mc.player.isGliding()
            && targetPos != null
            && !this.isAir(targetPos)
            && !this.mc.player.isOnGround()) {
            this.mc
               .getNetworkHandler()
               .sendPacket(
                  new Full(
                     this.mc.player.getX(),
                     this.mc.player.getY() + 1.0E-9,
                     this.mc.player.getZ(),
                     this.mc.player.getYaw(),
                     this.mc.player.getPitch(),
                     true,
                     this.mc.player.horizontalCollision
                  )
               );
            this.mc.player.onLanding();
         }

         if ((Boolean)this.swing.get()) {
            EntityUtil.attackSwingHand();
         }

         this.sendSequencedPacket(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, targetPos, BlockUtil.getClickSide(targetPos), id));
      }
   }

   private void sendStopSecond() {
      if ((Boolean)this.bypassGround.get()
         && !this.mc.player.isGliding()
         && secondPos != null
         && !this.isAir(secondPos)
         && !this.mc.player.isOnGround()) {
         this.mc
            .getNetworkHandler()
            .sendPacket(
               new Full(
                  this.mc.player.getX(),
                  this.mc.player.getY() + 1.0E-9,
                  this.mc.player.getZ(),
                  this.mc.player.getYaw(),
                  this.mc.player.getPitch(),
                  true,
                  this.mc.player.horizontalCollision
               )
            );
         this.mc.player.onLanding();
      }

      if (secondPos != null && !this.mc.world.isAir(secondPos)) {
         this.mc.world.setBlockState(secondPos, Blocks.AIR.getDefaultState());
      }
   }

   private boolean isAir(BlockPos breakPos) {
      return this.mc.world.isAir(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
   }

   private float getMineTicks(int slot) {
      if (targetPos != null && this.mc.world != null && this.mc.player != null) {
         BlockState state = this.mc.world.getBlockState(targetPos);
         float hardness = state.getHardness(this.mc.world, targetPos);
         if (hardness < 0.0F) {
            return Float.MAX_VALUE;
         } else if (hardness == 0.0F) {
            return 1.0F;
         } else {
            ItemStack stack = slot == -1 ? ItemStack.EMPTY : this.mc.player.getInventory().getStack(slot);
            boolean canHarvest = stack.isSuitableFor(state);
            float speed = stack.getMiningSpeedMultiplier(state);
            int efficiency = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0 && speed > 1.0F) {
               speed += efficiency * efficiency + 1;
            }

            if (this.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
               int amp = this.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier();
               speed *= 1.0F + (amp + 1) * 0.2F;
            }

            if (this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
               int amp = this.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier();

               speed *= switch (amp) {
                  case 0 -> 0.3F;
                  case 1 -> 0.09F;
                  case 2 -> 0.0027F;
                  default -> 8.1E-4F;
               };
            }

            float damage = speed / hardness / (canHarvest ? 30.0F : 100.0F);
            return damage <= 0.0F ? Float.MAX_VALUE : 1.0F / damage;
         }
      } else {
         return 20.0F;
      }
   }

   private float getMineTicks2(int slot) {
      if (secondPos != null && this.mc.world != null && this.mc.player != null) {
         BlockState state = this.mc.world.getBlockState(secondPos);
         float hardness = state.getHardness(this.mc.world, secondPos);
         if (hardness < 0.0F) {
            return Float.MAX_VALUE;
         } else if (hardness == 0.0F) {
            return 1.0F;
         } else {
            ItemStack stack = slot == -1 ? ItemStack.EMPTY : this.mc.player.getInventory().getStack(slot);
            boolean canHarvest = stack.isSuitableFor(state);
            float speed = stack.getMiningSpeedMultiplier(state);
            int efficiency = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0 && speed > 1.0F) {
               speed += efficiency * efficiency + 1;
            }

            if (this.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
               int amp = this.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier();
               speed *= 1.0F + (amp + 1) * 0.2F;
            }

            if (this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
               int amp = this.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier();

               speed *= switch (amp) {
                  case 0 -> 0.3F;
                  case 1 -> 0.09F;
                  case 2 -> 0.0027F;
                  default -> 8.1E-4F;
               };
            }

            float damage = speed / hardness / (canHarvest ? 30.0F : 100.0F);
            return damage <= 0.0F ? Float.MAX_VALUE : 1.0F / damage;
         }
      } else {
         return 20.0F;
      }
   }

   private void renderAnimation(Render3DEvent event, double delta, double damage) {
      this.render = MathHelper.clamp(this.render + delta * 2.0, -2.0, 2.0);
      double max = this.getMineTicks(this.getTool(targetPos));
      double p = 1.0 - MathHelper.clamp(progress / (max * damage), 0.0, 1.0);
      p = Math.pow(p, (Double)this.animationExp.get());
      p = 1.0 - p;
      double size = p / 2.0;
      Box box = new Box(
         targetPos.getX() + 0.5 - size,
         targetPos.getY() + 0.5 - size,
         targetPos.getZ() + 0.5 - size,
         targetPos.getX() + 0.5 + size,
         targetPos.getY() + 0.5 + size,
         targetPos.getZ() + 0.5 + size
      );
      Color side = this.getColor((Color)this.sideStartColor.get(), (Color)this.sideEndColor.get(), p);
      Color line = this.getColor((Color)this.lineStartColor.get(), (Color)this.lineEndColor.get(), p);
      event.renderer.box(box, side, line, (ShapeMode)this.shapeMode.get(), 0);
   }

   private void renderSecondAnimation(Render3DEvent event, double delta, double damage) {
      this.secondRender = MathHelper.clamp(this.secondRender + delta * 2.0, -2.0, 2.0);
      double max = this.getMineTicks2(this.getTool(secondPos));
      double p = 1.0 - MathHelper.clamp(secondProgress / (max * damage), 0.0, 1.0);
      p = Math.pow(p, (Double)this.animationExp.get());
      p = 1.0 - p;
      double size = p / 2.0;
      Box box = new Box(
         secondPos.getX() + 0.5 - size,
         secondPos.getY() + 0.5 - size,
         secondPos.getZ() + 0.5 - size,
         secondPos.getX() + 0.5 + size,
         secondPos.getY() + 0.5 + size,
         secondPos.getZ() + 0.5 + size
      );
      Color side = this.getColor((Color)this.secondSideStartColor.get(), (Color)this.secondSideEndColor.get(), p);
      Color line = this.getColor((Color)this.secondLineStartColor.get(), (Color)this.secondLineEndColor.get(), p);
      event.renderer.box(box, side, line, (ShapeMode)this.shapeMode.get(), 0);
   }

   private Color getColor(Color start, Color end, double progress) {
      return new Color(
         this.lerp(start.r, end.r, progress), this.lerp(start.g, end.g, progress), this.lerp(start.b, end.b, progress), this.lerp(start.a, end.a, progress)
      );
   }

   private int lerp(double start, double end, double d) {
      return (int)Math.round(start + (end - start) * d);
   }

   private int getTool(BlockPos pos) {
      int index = -1;
      float CurrentFastest = 1.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY) {
            float digSpeed = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            float destroySpeed = stack.getMiningSpeedMultiplier(this.mc.world.getBlockState(pos));
            if (digSpeed + destroySpeed > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }
      }

      return index;
   }

   public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (this.mc.getNetworkHandler() != null && this.mc.world != null) {
         this.mc.getNetworkHandler().sendPacket(packetCreator.predict(0));
      }
   }

   public boolean checkPause(boolean onlyMain) {
      return this.mc.options.useKey.isPressed() && (!onlyMain || this.mc.player.getActiveHand() == Hand.MAIN_HAND);
   }
}
