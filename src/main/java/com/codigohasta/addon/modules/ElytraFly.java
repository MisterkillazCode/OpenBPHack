package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.mixin.LivingEntityAccessor;
import com.codigohasta.addon.utils.alien.AlienEntityUtil;
import com.codigohasta.addon.utils.alien.AlienInventoryUtil;
import com.codigohasta.addon.utils.alien.AlienMathUtil;
import com.codigohasta.addon.utils.alien.AlienMovementUtil;
import com.codigohasta.addon.utils.alien.AlienTimer;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent.Pre;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraFly extends Module {
   public static ElytraFly INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgControl = this.settings.createGroup("Control");
   private final SettingGroup sgPitch = this.settings.createGroup("Pitch");
   private final SettingGroup sgBounce = this.settings.createGroup("Bounce");
   private final SettingGroup sgFirework = this.settings.createGroup("Firework");
   public final Setting<ElytraFly.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("mode")).description("Flight mode.")).defaultValue(ElytraFly.Mode.Control)).build());
   public final Setting<Boolean> infiniteDura = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("infinite-dura"))
                  .description("Prevents elytra from taking damage."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Boolean> packet = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("packet-mode"))
                  .description("Uses packets to fly without equipping elytra (Chestplate fly)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> packetDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("packet-delay"))
                     .description("Delay for packet mode ticks."))
                  .defaultValue(0))
               .min(0)
               .max(20)
               .visible(this.packet::get))
            .build()
      );
   private final Setting<Boolean> setFlag = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("set-flag"))
                     .description("Forces client-side flight flag."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() != ElytraFly.Mode.Bounce))
            .build()
      );
   private final Setting<Boolean> autoStop = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-stop"))
                  .description("Stops flying on unloaded chunks."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> instantFly = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("instant-fly"))
                     .description("Starts flying automatically when falling."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() != ElytraFly.Mode.Bounce))
            .build()
      );
   private final Setting<Double> timeout = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("timeout"))
                  .description("Instant fly timeout."))
               .defaultValue(0.0)
               .min(0.1)
               .max(1.0)
               .visible(() -> this.mode.get() != ElytraFly.Mode.Bounce))
            .build()
      );
   public final Setting<Boolean> releaseSneak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("release-sneak"))
                  .description("Releases shift when module disables."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Double> upPitch = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("up-pitch"))
                  .description("Pitch angle when going up (rotation-based)."))
               .defaultValue(0.0)
               .min(0.0)
               .max(90.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   public final Setting<Double> upFactor = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("up-factor"))
                  .description("Upward velocity factor."))
               .defaultValue(1.0)
               .min(0.0)
               .max(10.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   public final Setting<Double> fallSpeed = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("fall-speed"))
                  .description("Downward velocity factor."))
               .defaultValue(1.0)
               .min(0.0)
               .max(10.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   public final Setting<Double> speed = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("speed"))
                  .description("Horizontal flight speed."))
               .defaultValue(1.0)
               .min(0.1)
               .max(10.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   public final Setting<Boolean> speedLimit = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("speed-limit"))
                     .description("Limits maximum speed."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   public final Setting<Double> maxSpeed = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("max-speed"))
                  .description("Maximum speed cap."))
               .defaultValue(2.5)
               .min(0.1)
               .max(10.0)
               .visible(() -> (Boolean)this.speedLimit.get() && this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   public final Setting<Boolean> noDrag = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("no-drag"))
                     .description("Disables velocity drag."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   private final Setting<Double> sneakDownSpeed = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("down-speed"))
                  .description("Sneak downward speed."))
               .defaultValue(1.0)
               .min(0.1)
               .max(10.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Control))
            .build()
      );
   private final Setting<Double> boost = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("boost"))
                  .description("Boost mode strength."))
               .defaultValue(1.0)
               .min(0.1)
               .max(4.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Boost))
            .build()
      );
   private final Setting<Boolean> freeze = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("freeze"))
                     .description("Freeze in place when not moving (Rotation mode)."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() == ElytraFly.Mode.Rotation))
            .build()
      );
   private final Setting<Boolean> motionStop = this.sgControl
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("motion-stop"))
                     .description("Stop all motion when not pressing keys (Rotation mode)."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() == ElytraFly.Mode.Rotation))
            .build()
      );
   private final Setting<Double> infiniteMaxSpeed = this.sgPitch
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("infinite-max-speed"))
                  .description("Max speed for pitch oscillation."))
               .defaultValue(150.0)
               .min(50.0)
               .max(170.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Pitch))
            .build()
      );
   private final Setting<Double> infiniteMinSpeed = this.sgPitch
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("infinite-min-speed"))
                  .description("Min speed for pitch oscillation."))
               .defaultValue(25.0)
               .min(10.0)
               .max(70.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Pitch))
            .build()
      );
   private final Setting<Double> infiniteMaxHeight = this.sgPitch
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("infinite-max-height"))
                  .description("Max Y level for pitch oscillation."))
               .defaultValue(200.0)
               .min(-50.0)
               .max(360.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Pitch))
            .build()
      );
   public final Setting<Boolean> autoJump = this.sgBounce
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("auto-jump"))
                     .description("Automatically holds jump for Bounce mode."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == ElytraFly.Mode.Bounce))
            .build()
      );
   private final Setting<Boolean> sprint = this.sgBounce
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("sprint"))
                     .description("Sprint in Bounce mode."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == ElytraFly.Mode.Bounce))
            .build()
      );
   private final Setting<Double> bouncePitch = this.sgBounce
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("pitch"))
                  .description("Pitch for Bounce mode."))
               .defaultValue(88.0)
               .min(-90.0)
               .max(90.0)
               .sliderMax(90.0)
               .visible(() -> this.mode.get() == ElytraFly.Mode.Bounce))
            .build()
      );
   public final Setting<Boolean> firework = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("firework"))
                  .description("Auto uses fireworks."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Keybind> fireWorkBind = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("firework-bind"))
                  .description("Manual firework keybind."))
               .defaultValue(Keybind.none()))
            .action(this::manualFirework)
            .build()
      );
   public final Setting<Boolean> packetInteract = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("packet-interact"))
                     .description("Use packets for firework interaction."))
                  .defaultValue(true))
               .visible(this.firework::get))
            .build()
      );
   public final Setting<Boolean> inventorySwap = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("inventory-swap"))
                     .description("Pulls firework from inventory silently."))
                  .defaultValue(true))
               .visible(this.firework::get))
            .build()
      );
   public final Setting<Boolean> onlyOne = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("only-one"))
                     .description("Limits to one rocket entity at a time."))
                  .defaultValue(true))
               .visible(this.firework::get))
            .build()
      );
   private final Setting<Boolean> usingPause = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("using-pause"))
                     .description("Pauses firework while using items."))
                  .defaultValue(true))
               .visible(this.firework::get))
            .build()
      );
   private final Setting<Boolean> checkSpeed = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("check-speed"))
                     .description("Only use firework when speed is below min-speed."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() != ElytraFly.Mode.Bounce))
            .build()
      );
   public final Setting<Double> minSpeed = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("min-speed"))
                  .description("Minimum speed threshold for firework use."))
               .defaultValue(70.0)
               .min(0.1)
               .max(200.0)
               .sliderMax(200.0)
               .visible(() -> this.mode.get() != ElytraFly.Mode.Bounce))
            .build()
      );
   private final Setting<Integer> delay = this.sgFirework
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("delay"))
                     .description("Delay between firework uses (ms)."))
                  .defaultValue(1000))
               .min(0)
               .max(20000)
               .visible(() -> this.mode.get() != ElytraFly.Mode.Bounce))
            .build()
      );
   private final AlienTimer fireworkTimer = new AlienTimer();
   private final AlienTimer instantFlyTimer = new AlienTimer();
   private boolean hasElytra = false;
   private float yaw = 0.0F;
   private float rotationPitch = 0.0F;
   private boolean flying = false;
   private int packetDelayInt = 0;
   private boolean down;
   private float lastInfinitePitch;
   private float infinitePitch;
   private boolean prev;
   private float prePitch;

   public ElytraFly() {
      super(AddonTemplate.CATEGORY, "ElytraFly", "Complete elytra fly (AlienV4 style) with 7 modes. Sends some packets Grim may flag; effect is not great.");
      INSTANCE = this;
   }

   public void onActivate() {
      if (this.mc.player != null) {
         this.hasElytra = false;
         this.yaw = this.mc.player.getYaw();
         this.rotationPitch = this.mc.player.getPitch();
      }
   }

   public void onDeactivate() {
      if (this.mc.player != null && (Boolean)this.releaseSneak.get()) {
         this.mc.options.sneakKey.setPressed(false);
      }
   }

   public String getInfoString() {
      return ((ElytraFly.Mode)this.mode.get()).name();
   }

   private void manualFirework() {
      if (this.mc.player != null) {
         if ((!this.mc.player.isUsingItem() || !(Boolean)this.usingPause.get())
            && this.isFallFlying()
            && this.fireworkTimer.passed(((Integer)this.delay.get()).intValue())) {
            this.off();
            this.fireworkTimer.reset();
         }
      }
   }

   public void off() {
      if (this.mc.player != null) {
         if (!(Boolean)this.inventorySwap.get() || AlienEntityUtil.inInventory()) {
            if ((Boolean)this.onlyOne.get()) {
               for (Entity entity : this.mc.world.getEntities()) {
                  if (entity instanceof FireworkRocketEntity fw && fw.getOwner() == this.mc.player) {
                     return;
                  }
               }
            }

            this.fireworkTimer.reset();
            this.useFireworkItem();
         }
      }
   }

   private void useFireworkItem() {
      if (this.mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
         this.interactFirework();
      } else {
         int invSlot = AlienInventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET);
         int hotbarSlot = AlienInventoryUtil.findItem(Items.FIREWORK_ROCKET);
         if ((Boolean)this.inventorySwap.get() && invSlot != -1) {
            int selectedSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            AlienInventoryUtil.inventorySwap(invSlot, selectedSlot);
            this.interactFirework();
            AlienInventoryUtil.inventorySwap(invSlot, selectedSlot);
            AlienEntityUtil.syncInventory();
         } else if (hotbarSlot != -1) {
            int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            AlienInventoryUtil.switchToSlot(hotbarSlot);
            this.interactFirework();
            AlienInventoryUtil.switchToSlot(old);
         }
      }
   }

   private void interactFirework() {
      if ((Boolean)this.packetInteract.get()) {
         this.mc
            .getNetworkHandler()
            .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, this.mc.player.getYaw(), this.mc.player.getPitch()));
      } else {
         this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
      }
   }

   public static boolean recastElytra(ClientPlayerEntity player) {
      if (checkConditions(player) && ignoreGround(player)) {
         player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         if (INSTANCE != null && (Boolean)INSTANCE.setFlag.get()) {
            INSTANCE.mc.player.startGliding();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(ClientPlayerEntity player) {
      ItemStack stack = player.getEquippedStack(EquipmentSlot.CHEST);
      return !player.getAbilities().flying
         && !player.hasVehicle()
         && !player.isClimbing()
         && stack.isOf(Items.ELYTRA)
         && stack.getMaxDamage() - stack.getDamage() > 1;
   }

   private static boolean ignoreGround(ClientPlayerEntity player) {
      if (!player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.LEVITATION)) {
         ItemStack stack = player.getEquippedStack(EquipmentSlot.CHEST);
         if (stack.isOf(Items.ELYTRA) && stack.getMaxDamage() - stack.getDamage() > 1) {
            player.startGliding();
            return true;
         }
      }

      return false;
   }

   private void boostFunc() {
      if (this.hasElytra && this.isFallFlying()) {
         float yaw = (float)Math.toRadians(this.mc.player.getYaw());
         if (this.mc.options.forwardKey.isPressed()) {
            this.mc
               .player
               .addVelocity(
                  -MathHelper.sin(yaw) * ((Double)this.boost.get()).floatValue() / 10.0F,
                  0.0,
                  MathHelper.cos(yaw) * ((Double)this.boost.get()).floatValue() / 10.0F
               );
         }
      }
   }

   @EventHandler(
      priority = -9999
   )
   private void onUpdateRotation(Pre event) {
      if (this.mc.player != null && this.isFallFlying()) {
         if (this.mode.get() == ElytraFly.Mode.Rotation) {
            if (AlienMovementUtil.isMoving()) {
               this.rotationPitch = this.mc.options.jumpKey.isPressed() ? -45.0F : (this.mc.options.sneakKey.isPressed() ? 45.0F : -1.9F);
            } else {
               this.rotationPitch = this.mc.options.jumpKey.isPressed()
                  ? -89.0F
                  : (this.mc.options.sneakKey.isPressed() ? 89.0F : this.rotationPitch);
               if ((Boolean)this.motionStop.get()) {
                  this.setY(0.0);
               }
            }

            if (AlienMovementUtil.isMoving()) {
               this.yaw = this.getSprintYaw(this.mc.player.getYaw());
            } else if ((Boolean)this.motionStop.get()) {
               this.setX(0.0);
               this.setZ(0.0);
            }

            this.mc.player.setYaw(this.yaw);
            this.mc.player.setPitch(this.rotationPitch);
         } else if (this.mode.get() == ElytraFly.Mode.Pitch && this.isFallFlying()) {
            this.mc.player.setPitch(this.infinitePitch);
         } else if (this.mode.get() == ElytraFly.Mode.Bounce && this.isFallFlying()) {
            this.mc.player.setPitch(((Double)this.bouncePitch.get()).floatValue());
         }
      }
   }

   @EventHandler
   private void onTick(meteordevelopment.meteorclient.events.world.TickEvent.Pre event) {
      if (this.mc.player != null) {
         this.getInfinitePitch();
         this.flying = false;
         if ((Boolean)this.packet.get()) {
            this.hasElytra = AlienInventoryUtil.findItem(Items.ELYTRA) != -1 || AlienInventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
         } else {
            this.hasElytra = false;
            ItemStack chestStack = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
            this.hasElytra = chestStack.isOf(Items.ELYTRA);
            if ((Boolean)this.infiniteDura.get() && !this.mc.player.isOnGround() && this.hasElytra) {
               this.flying = true;
               this.clickDurabilitySlot();
               this.mc
                  .getNetworkHandler()
                  .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               if ((Boolean)this.setFlag.get()) {
                  this.mc.player.startGliding();
               }
            }

            if (this.mode.get() == ElytraFly.Mode.Bounce) {
               ((LivingEntityAccessor)this.mc.player).setJumpingCooldown(0);
               return;
            }
         }

         double speedVal = this.calcSpeed();
         if (this.mode.get() == ElytraFly.Mode.Boost) {
            this.boostFunc();
         }

         if ((Boolean)this.packet.get()) {
            this.handlePacketFly(speedVal);
         } else {
            this.handleNormalFly(speedVal);
         }
      }
   }

   private double calcSpeed() {
      double dx = this.mc.player.getX() - this.mc.player.lastRenderX;
      double dy = this.mc.player.getY() - this.mc.player.lastRenderY;
      double dz = this.mc.player.getZ() - this.mc.player.lastRenderZ;
      double dist = Math.sqrt(dx * dx + dz * dz + dy * dy) / 1000.0;
      return dist / 1.388888888888889E-5;
   }

   private void clickDurabilitySlot() {
      int syncId = this.mc.player.currentScreenHandler.syncId;
      this.mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, this.mc.player);
      this.mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, this.mc.player);
   }

   private void handlePacketFly(double speedVal) {
      if (!this.mc.player.isOnGround()) {
         this.packetDelayInt++;
         if (this.packetDelayInt > (Integer)this.packetDelay.get()) {
            int syncId = this.mc.player.currentScreenHandler.syncId;
            int elytra = AlienInventoryUtil.findItem(Items.ELYTRA);
            if (elytra != -1) {
               this.mc.interactionManager.clickSlot(syncId, 6, elytra, SlotActionType.SWAP, this.mc.player);
               this.mc
                  .getNetworkHandler()
                  .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               this.mc.player.startGliding();
               this.mc.interactionManager.clickSlot(syncId, 6, elytra, SlotActionType.SWAP, this.mc.player);
               this.packetDelayInt = 0;
            } else {
               int invElytra = AlienInventoryUtil.findItemInventorySlot(Items.ELYTRA);
               if (invElytra != -1) {
                  this.mc.interactionManager.clickSlot(syncId, invElytra, 0, SlotActionType.PICKUP, this.mc.player);
                  this.mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, this.mc.player);
                  this.mc
                     .getNetworkHandler()
                     .sendPacket(
                        new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                     );
                  this.mc.player.startGliding();
                  this.mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, this.mc.player);
                  this.mc.interactionManager.clickSlot(syncId, invElytra, 0, SlotActionType.PICKUP, this.mc.player);
                  this.packetDelayInt = 0;
               }
            }
         }

         if (!this.mc.player.isOnGround() && this.isFallFlying()) {
            boolean moving = AlienMovementUtil.isMoving() || this.mode.get() == ElytraFly.Mode.Rotation && this.mc.options.jumpKey.isPressed();
            boolean speedOk = !(Boolean)this.checkSpeed.get() || speedVal <= (Double)this.minSpeed.get();
            boolean notUsing = !this.mc.player.isUsingItem() || !(Boolean)this.usingPause.get();
            if (speedOk && (Boolean)this.firework.get() && this.fireworkTimer.passed(((Integer)this.delay.get()).intValue()) && moving && notUsing) {
               this.off();
               this.fireworkTimer.reset();
            }
         }
      }
   }

   private void handleNormalFly(double speedVal) {
      this.tryAutoFirework(speedVal);
      if (!this.isFallFlying() && this.hasElytra) {
         this.fireworkTimer.setMs(99999999L);
         if (!this.mc.player.isOnGround()
            && (Boolean)this.instantFly.get()
            && this.mc.player.getVelocity().y < 0.0
            && !(Boolean)this.infiniteDura.get()) {
            if (!this.instantFlyTimer.passed((long)(1000.0 * (Double)this.timeout.get()))) {
               return;
            }

            this.instantFlyTimer.reset();
            this.mc
               .getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            if ((Boolean)this.setFlag.get()) {
               this.mc.player.startGliding();
            }
         }
      }
   }

   private void tryAutoFirework(double speedVal) {
      boolean speedOk = !(Boolean)this.checkSpeed.get() || speedVal <= (Double)this.minSpeed.get();
      boolean moving = AlienMovementUtil.isMoving() || this.mode.get() == ElytraFly.Mode.Rotation && this.mc.options.jumpKey.isPressed();
      boolean notUsing = !this.mc.player.isUsingItem() || !(Boolean)this.usingPause.get();
      if (speedOk
         && (Boolean)this.firework.get()
         && this.fireworkTimer.passed(((Integer)this.delay.get()).intValue())
         && moving
         && notUsing
         && this.isFallFlying()) {
         this.off();
         this.fireworkTimer.reset();
      }
   }

   @EventHandler
   private void onTickPost(Post event) {
      if (this.mc.player != null) {
         if (this.mode.get() == ElytraFly.Mode.Bounce && this.hasElytra && !(Boolean)this.packet.get()) {
            if ((Boolean)this.autoJump.get()) {
               this.mc.options.jumpKey.setPressed(true);
            }

            if (checkConditions(this.mc.player)) {
               if (!this.isFallFlying()) {
                  this.mc
                     .getNetworkHandler()
                     .sendPacket(
                        new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                     );
               }

               if (!(Boolean)this.sprint.get()) {
                  this.mc.player.setSprinting(this.isFallFlying() && this.mc.player.isOnGround());
               }
            } else if ((Boolean)this.sprint.get()) {
               this.mc.player.setSprinting(true);
            }
         }
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.mc.player != null) {
         if ((Boolean)this.autoStop.get() && this.isFallFlying()) {
            int chunkX = (int)(this.mc.player.getX() / 16.0);
            int chunkZ = (int)(this.mc.player.getZ() / 16.0);
            if (!this.mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
               ((IVec3d)event.movement).meteor$set(0.0, 0.0, 0.0);
               return;
            }
         }

         if (this.hasElytra && this.isFallFlying()) {
            boolean moving = AlienMovementUtil.isMoving();
            boolean jumpPressed = this.mc.options.jumpKey.isPressed();
            boolean sneakPressed = this.mc.options.sneakKey.isPressed();
            if ((this.mode.get() == ElytraFly.Mode.Freeze || this.mode.get() == ElytraFly.Mode.Rotation && (Boolean)this.freeze.get())
               && !moving
               && !jumpPressed
               && !sneakPressed) {
               ((IVec3d)event.movement).meteor$set(0.0, 0.0, 0.0);
               return;
            }

            if (this.mode.get() == ElytraFly.Mode.Control) {
               this.handleControlMove(event);
            }
         }
      }
   }

   private void handleControlMove(PlayerMoveEvent event) {
      if ((Boolean)this.firework.get()) {
         if (this.mc.options.sneakKey.isPressed() && this.mc.options.jumpKey.isPressed()) {
            this.setY(0.0);
         } else if (this.mc.options.sneakKey.isPressed()) {
            this.setY(-(Double)this.sneakDownSpeed.get());
         } else if (this.mc.options.jumpKey.isPressed()) {
            this.setY((Double)this.upFactor.get());
         } else {
            this.setY(-3.0E-11 * (Double)this.fallSpeed.get());
         }

         double[] dir = AlienMovementUtil.directionSpeed((Double)this.speed.get());
         this.setX(dir[0]);
         this.setZ(dir[1]);
      } else {
         Vec3d lookVec = this.getRotationVec(1.0F);
         double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
         double motionDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
         if (this.mc.options.sneakKey.isPressed()) {
            this.setY(-(Double)this.sneakDownSpeed.get());
         } else if (!this.mc.options.jumpKey.isPressed()) {
            this.setY(-3.0E-11 * (Double)this.fallSpeed.get());
         }

         if (this.mc.options.jumpKey.isPressed()) {
            if (motionDist > (Double)this.upFactor.get() / 10.0) {
               double rawUpSpeed = motionDist * 0.01325;
               this.setY(this.getY() + rawUpSpeed * 3.2);
               this.setX(this.getX() - lookVec.x * rawUpSpeed / lookDist);
               this.setZ(this.getZ() - lookVec.z * rawUpSpeed / lookDist);
            } else {
               double[] dir = AlienMovementUtil.directionSpeed((Double)this.speed.get());
               this.setX(dir[0]);
               this.setZ(dir[1]);
            }
         }

         if (lookDist > 0.0) {
            this.setX(this.getX() + (lookVec.x / lookDist * motionDist - this.getX()) * 0.1);
            this.setZ(this.getZ() + (lookVec.z / lookDist * motionDist - this.getZ()) * 0.1);
         }

         if (!this.mc.options.jumpKey.isPressed()) {
            double[] dir = AlienMovementUtil.directionSpeed((Double)this.speed.get());
            this.setX(dir[0]);
            this.setZ(dir[1]);
         }

         if (!(Boolean)this.noDrag.get()) {
            this.setY(this.getY() * 0.99);
            this.setX(this.getX() * 0.98);
            this.setZ(this.getZ() * 0.99);
         }

         double finalDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
         if ((Boolean)this.speedLimit.get() && finalDist > (Double)this.maxSpeed.get()) {
            this.setX(this.getX() * (Double)this.maxSpeed.get() / finalDist);
            this.setZ(this.getZ() * (Double)this.maxSpeed.get() / finalDist);
         }

         ((IVec3d)event.movement).meteor$set(this.getX(), this.getY(), this.getZ());
      }
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (this.mc.player != null) {
         if (this.mode.get() == ElytraFly.Mode.Bounce
            && this.hasElytra
            && !(Boolean)this.packet.get()
            && event.packet instanceof ClientCommandC2SPacket pkt
            && pkt.getMode() == net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING
            && !(Boolean)this.sprint.get()) {
            this.mc.player.setSprinting(true);
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (this.mc.player != null) {
         if (this.mode.get() == ElytraFly.Mode.Bounce && this.hasElytra && !(Boolean)this.packet.get() && event.packet instanceof PlayerPositionLookS2CPacket) {
            this.mc.player.stopGliding();
         }
      }
   }

   private double getX() {
      return AlienMovementUtil.getMotionX();
   }

   private void setX(double v) {
      AlienMovementUtil.setMotionX(v);
   }

   private double getY() {
      return AlienMovementUtil.getMotionY();
   }

   private void setY(double v) {
      AlienMovementUtil.setMotionY(v);
   }

   private double getZ() {
      return AlienMovementUtil.getMotionZ();
   }

   private void setZ(double v) {
      AlienMovementUtil.setMotionZ(v);
   }

   private void getInfinitePitch() {
      this.lastInfinitePitch = this.infinitePitch;
      double speedVal = Math.hypot(
         this.mc.player.getX() - this.mc.player.lastRenderX, this.mc.player.getZ() - this.mc.player.lastRenderZ
      );
      if (this.mc.player.getY() < (Double)this.infiniteMaxHeight.get()) {
         if (speedVal * 72.0 < (Double)this.infiniteMinSpeed.get() && !this.down) {
            this.down = true;
         }

         if (speedVal * 72.0 > (Double)this.infiniteMaxSpeed.get() && this.down) {
            this.down = false;
         }
      } else {
         this.down = true;
      }

      this.infinitePitch = this.infinitePitch + (this.down ? 3.0F : -3.0F);
      this.infinitePitch = AlienMathUtil.clamp(this.infinitePitch, -40.0F, 40.0F);
   }

   public boolean isFallFlying() {
      return this.mc.player.isGliding() || (Boolean)this.packet.get() && this.hasElytra && !this.mc.player.isOnGround() || this.flying;
   }

   private Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   public Vec3d getRotationVec(float tickDelta) {
      return this.getRotationVector(-((Double)this.upPitch.get()).floatValue(), this.mc.player.getYaw());
   }

   private float getSprintYaw(float yaw) {
      if (this.mc.options.forwardKey.isPressed() && !this.mc.options.backKey.isPressed()) {
         if (this.mc.options.leftKey.isPressed() && !this.mc.options.rightKey.isPressed()) {
            yaw -= 45.0F;
         } else if (this.mc.options.rightKey.isPressed() && !this.mc.options.leftKey.isPressed()) {
            yaw += 45.0F;
         }
      } else if (this.mc.options.backKey.isPressed() && !this.mc.options.forwardKey.isPressed()) {
         yaw += 180.0F;
         if (this.mc.options.leftKey.isPressed() && !this.mc.options.rightKey.isPressed()) {
            yaw += 45.0F;
         } else if (this.mc.options.rightKey.isPressed() && !this.mc.options.leftKey.isPressed()) {
            yaw -= 45.0F;
         }
      } else if (this.mc.options.leftKey.isPressed() && !this.mc.options.rightKey.isPressed()) {
         yaw -= 90.0F;
      } else if (this.mc.options.rightKey.isPressed() && !this.mc.options.leftKey.isPressed()) {
         yaw += 90.0F;
      }

      return MathHelper.wrapDegrees(yaw);
   }

   public static enum Mode {
      Control,
      Boost,
      Bounce,
      Freeze,
      None,
      Rotation,
      Pitch;
   }
}
