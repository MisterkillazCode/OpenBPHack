package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.IVec3d;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import com.codigohasta.addon.utils.leaveshack.events.ElytraUpdateEvent;
import com.codigohasta.addon.utils.leaveshack.events.TravelEvent;
import java.util.TimerTask;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

public class FireworkElytraFly extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<FireworkElytraFly.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("RowMode(LegitMethod, GrimDurability)"))
               .defaultValue(FireworkElytraFly.Mode.Legit))
            .build()
      );
   public final Setting<FireworkElytraFly.FireWorkMode> fireWorkMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("FireWorkMode")).description("UseMode(DelayDelayPut, AutoAutoPut)"))
               .defaultValue(FireworkElytraFly.FireWorkMode.Delay))
            .build()
      );
   private final Setting<Double> packetDealy = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("PacketDelay"))
               .description("SendPackDelaytickNumber"))
            .defaultValue(3.0)
            .sliderMax(100.0)
            .build()
      );
   public final Setting<Boolean> unbreaking = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Unbreaking"))
                     .description("InfiniteDurability"))
                  .description(""))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> fakeDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("FakeDelay"))
               .description("InfiniteDurabilityOperationDelay"))
            .defaultValue(800.0)
            .sliderMax(1000.0)
            .build()
      );
   public final Setting<Boolean> stand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Stand"))
                     .description("Stand Fly"))
                  .description(""))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> releaseSneak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("ReleaseSneak"))
                     .description("Autoshift"))
                  .description(""))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> pressSneak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("PressSneak"))
                     .description("Autoshift"))
                  .description(""))
               .defaultValue(true))
            .build()
      );
   public final Setting<Integer> releaseDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ReleaseDelay"))
                  .description("shiftDelay"))
               .defaultValue(100))
            .sliderMax(1000)
            .build()
      );
   private final Setting<Double> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("FireWorkDelay"))
                  .description("OperationDelay"))
               .defaultValue(1000.0)
               .visible(() -> this.fireWorkMode.get() == FireworkElytraFly.FireWorkMode.Delay))
            .sliderMax(3000.0)
            .build()
      );
   private final Setting<Boolean> checkFirework = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("CheckFireWork"))
                  .description("AutoCheck"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> inventorySwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("InventorySwap"))
                  .description("PackHand"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> control = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Control"))
                  .description("System"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> fallSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("FallSpeed"))
               .description("downSpeed"))
            .defaultValue(0.02)
            .sliderRange(0.0, 3.0)
            .build()
      );
   private final Setting<Boolean> deBug = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DeBug"))
                  .description("devbug's, notiqnotwantOpen"))
               .defaultValue(false))
            .build()
      );
   private final SettingGroup sgTakeoff = this.settings.createGroup("Takeoff");
   private final Setting<Boolean> safeTakeoff = this.sgTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Safe-Takeoff"))
                  .description(
                     "Sends the elytra deploy packet at the end of the tick and only when the server already knows you are airborne. Fixes the 'cannot take off' issue."
                  ))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> takeoffDelay = this.sgTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Takeoff-Delay"))
                     .description("Ticks to wait after leaving the ground before deploying the elytra."))
                  .defaultValue(2))
               .min(0)
               .max(20)
               .sliderRange(0, 20)
               .visible(this.safeTakeoff::get))
            .build()
      );
   private final Setting<Boolean> requireFalling = this.sgTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Require-Falling"))
                     .description("Only deploys once you are actually falling (y velocity below 0), exactly like a vanilla player would."))
                  .defaultValue(false))
               .visible(this.safeTakeoff::get))
            .build()
      );
   private final Setting<Boolean> sendPositionBefore = this.sgTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Position-Before"))
                     .description("Sends an airborne movement packet right before the deploy packet so the server never rejects it."))
                  .defaultValue(true))
               .visible(this.safeTakeoff::get))
            .build()
      );
   private final Setting<Integer> takeoffCooldown = this.sgTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Takeoff-Cooldown"))
                     .description("Ticks between deploy attempts. Prevents packet spam, which makes Grim set you back."))
                  .defaultValue(3))
               .min(0)
               .max(40)
               .sliderRange(0, 40)
               .visible(this.safeTakeoff::get))
            .build()
      );
   private final Setting<Boolean> spaceCheck = this.sgTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Space-Check"))
                  .description("Never tries to deploy while stuck inside a block or with a ceiling directly above you."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoJump = this.sgTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Auto-Jump"))
                  .description("Jumps automatically while you are on the ground and moving, so the module can start flying."))
               .defaultValue(false))
            .build()
      );
   private int airTicks = 0;
   private int takeoffCooldownTicks = 0;
   private int deployCount = 0;
   private boolean airborneSynced = false;
   public static FireworkElytraFly INSTANCE;
   public float yaw = Rotation.rotationYaw;
   public float pitch = Rotation.rotationPitch;
   public boolean isUsingFirework = false;
   private final Timer fireworkTimer = new Timer();
   private final Timer swapTimer = new Timer();
   public boolean isFallFlying = false;
   public int packetDelayInt = 0;
   public int clearInputTicks = 0;
   public boolean forceJumpInput = false;

   public FireworkElytraFly() {
      super(AddonTemplate.CATEGORY, "FireworkElytraFly", "Elytra flight with automatic firework boosting and durability-safe swapping.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.clearInputTicks = 0;
      this.forceJumpInput = false;
      this.fireworkTimer.setMs(99999L);
      this.packetDelayInt = 0;
      this.swapTimer.setMs(99999L);
      this.airTicks = 0;
      this.takeoffCooldownTicks = 0;
      this.deployCount = 0;
   }

   public void onDeactivate() {
      this.mc.options.jumpKey.setPressed(false);
      this.clearInputTicks = 0;
      this.forceJumpInput = false;
      if ((Boolean)this.pressSneak.get()) {
         this.mc.options.sneakKey.setPressed(true);
      }

      if ((Boolean)this.releaseSneak.get()) {
         long delay = ((Integer)this.releaseDelay.get()).intValue();
         java.util.Timer timer = new java.util.Timer();
         timer.schedule(new TimerTask() {
            @Override
            public void run() {
               FireworkElytraFly.this.mc.execute(() -> FireworkElytraFly.this.mc.options.sneakKey.setPressed(false));
            }
         }, delay);
      }
   }

   @EventHandler
   public void onTravel(TravelEvent event) {
      if (this.isFallFlying) {
         if (this.mode.get() != FireworkElytraFly.Mode.Legit) {
            if ((Boolean)this.control.get()) {
               if (this.mc.currentScreen instanceof ChatScreen) {
                  this.setY((Double)this.fallSpeed.get());
               } else {
                  if (!this.wantToMove()) {
                     this.setX(0.0);
                     this.setZ(0.0);
                     this.setY((Double)this.fallSpeed.get());
                  }
               }
            }
         }
      }
   }

   private void setY(double f) {
      ((IVec3d)this.mc.player.getVelocity()).setY(f);
   }

   private void setX(double f) {
      ((IVec3d)this.mc.player.getVelocity()).setX(f);
   }

   private void setZ(double f) {
      ((IVec3d)this.mc.player.getVelocity()).setZ(f);
   }

   public String getInfoString() {
      if (this.mc.player != null && this.mc.world != null) {
         int fireworks = 0;
         if ((Boolean)this.inventorySwap.get()) {
            for (int i = 0; i < 45; i++) {
               ItemStack stack = this.mc.player.getInventory().getStack(i);
               if (stack.getItem() == Items.FIREWORK_ROCKET) {
                  fireworks += stack.getCount();
               }
            }
         } else {
            for (int ix = 0; ix < 9; ix++) {
               ItemStack stack = this.mc.player.getInventory().getStack(ix);
               if (stack.getItem() == Items.FIREWORK_ROCKET) {
                  fireworks += stack.getCount();
               }
            }
         }

         String base = "f[F:" + fireworks + "]";
         return this.mc.player != null && !this.mc.player.isGliding() ? base + " takeoff" : base;
      } else {
         return null;
      }
   }

   @EventHandler
   public void onElytraUpdate(ElytraUpdateEvent event) {
      if ((Boolean)this.stand.get()) {
         event.cancel();
      }
   }

   @EventHandler
   public void onTick(Pre event) {
      this.forceJumpInput = false;
      this.airborneSynced = false;
      if (this.mc.currentScreen != null && (Boolean)this.deBug.get()) {
         this.info(
            "screen"
               + this.mc.currentScreen.getTitle()
               + " "
               + this.mc.currentScreen.getClass().getSimpleName()
               + " "
               + this.mc.currentScreen.getClass().getSuperclass().getSimpleName()
               + " "
               + this.mc.currentScreen.getTitle(),
            new Object[0]
         );
      }

      if (this.mc.currentScreen == null
         || !(this.mc.currentScreen instanceof HandledScreen)
         || this.mc.currentScreen instanceof InventoryScreen
         || this.mc.currentScreen instanceof CreativeInventoryScreen) {
         int elytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
         this.packetDelayInt++;
         if (!this.mc.player.isOnGround() && !this.mc.player.hasVehicle() && !this.mc.player.isTouchingWater()) {
            this.airTicks++;
         } else {
            this.airTicks = 0;
         }

         if (this.takeoffCooldownTicks > 0) {
            this.takeoffCooldownTicks--;
         }

         if ((Boolean)this.autoJump.get() && this.mc.player.isOnGround() && !this.mc.player.isGliding() && this.wantToMove()) {
            this.mc.player.jump();
         }

         if (this.mode.get() == FireworkElytraFly.Mode.GrimDurability && elytra != -1 && this.packetDelayInt == ((Double)this.packetDealy.get()).intValue()) {
            this.clearInputTicks = 2;
         }

         this.yaw = this.getSprintYaw(this.mc.player.getYaw());
         this.pitch = this.getPitch(this.mc.player.getPitch());
         if ((Boolean)this.deBug.get()) {
            this.info("Yaw: " + this.yaw + " Pitch: " + this.pitch, new Object[0]);
         }

         if (this.mode.get() == FireworkElytraFly.Mode.GrimDurability) {
            if ((Boolean)GlobalSetting.INSTANCE.moveFix.get()) {
               Rotation.snapAt(this.yaw, this.pitch);
            } else {
               this.mc
                  .getNetworkHandler()
                  .sendPacket(
                     new Full(
                        this.mc.player.getX(),
                        this.mc.player.getY(),
                        this.mc.player.getZ(),
                        this.yaw,
                        this.pitch,
                        this.mc.player.isOnGround(),
                        this.mc.player.horizontalCollision
                     )
                  );
               this.airborneSynced = !this.mc.player.isOnGround();
            }
         }

         boolean hasFirework = false;
         if ((Boolean)this.checkFirework.get()) {
            for (Entity entity : this.mc.world.getEntities()) {
               if (entity instanceof FireworkRocketEntity firework && firework.getOwner() == this.mc.player) {
                  hasFirework = true;
               }
            }
         }

         this.isUsingFirework = hasFirework;
         ItemStack chestStack = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
         boolean wearingElytra = chestStack.getItem() == Items.ELYTRA && chestStack.getDamage() < chestStack.getMaxDamage() - 1;
         if (this.mode.get() == FireworkElytraFly.Mode.GrimDurability) {
            if (elytra != -1 && this.packetDelayInt > (Double)this.packetDealy.get()) {
               this.clickSlot(elytra, 0, SlotActionType.PICKUP);
               this.clickSlot(6, 0, SlotActionType.PICKUP);
               this.clickSlot(elytra, 0, SlotActionType.PICKUP);
               if (this.canDeployNow()) {
                  this.deployElytra(true);
               }

               if (!hasFirework && this.fireWorkMode.get() == FireworkElytraFly.FireWorkMode.Auto) {
                  this.offFirework();
               } else if (this.fireWorkMode.get() == FireworkElytraFly.FireWorkMode.Delay
                  && this.wantToMove()
                  && (!(Boolean)this.checkFirework.get() || !this.isUsingFirework)) {
                  this.offFirework();
               }

               this.clickSlot(elytra, 0, SlotActionType.PICKUP);
               this.clickSlot(6, 0, SlotActionType.PICKUP);
               this.clickSlot(elytra, 0, SlotActionType.PICKUP);
               this.forceJumpInput = true;
               this.packetDelayInt = 0;
            }
         } else {
            if (this.mode.get() == FireworkElytraFly.Mode.Legit
               && wearingElytra
               && this.mc.player.isGliding()
               && !this.mc.player.isOnGround()
               && (Boolean)this.unbreaking.get()
               && this.swapTimer.passedMs((Double)this.fakeDelay.get())) {
               this.clickSlot(6, 0, SlotActionType.PICKUP);
               this.clickSlot(6, 0, SlotActionType.PICKUP);
               Rotation.sendPacket(
                  new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
               );
               this.mc.player.startGliding();
               this.swapTimer.reset();
            }

            if (wearingElytra && this.mc.player.isGliding()) {
               if (!hasFirework && this.fireWorkMode.get() == FireworkElytraFly.FireWorkMode.Auto) {
                  this.offFirework();
               } else if (this.fireWorkMode.get() == FireworkElytraFly.FireWorkMode.Delay
                  && this.wantToMove()
                  && (!(Boolean)this.checkFirework.get() || !this.isUsingFirework)) {
                  this.offFirework();
               }
            }
         }
      }
   }

   @EventHandler
   public void onTickPost(Post event) {
      if (this.clearInputTicks > 0) {
         this.clearInputTicks--;
      }

      this.handleTakeoff();
   }

   private void handleTakeoff() {
      if (this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null) {
         if (!(this.mc.currentScreen instanceof HandledScreen)
            || this.mc.currentScreen instanceof InventoryScreen
            || this.mc.currentScreen instanceof CreativeInventoryScreen) {
            if (!this.mc.player.isGliding()) {
               if (this.canDeployNow()) {
                  ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
                  boolean wearing = chest.getItem() == Items.ELYTRA && chest.getDamage() < chest.getMaxDamage() - 1;
                  if (this.mode.get() != FireworkElytraFly.Mode.GrimDurability || wearing) {
                     this.deployElytra(false);
                  }
               }
            }
         }
      }
   }

   private boolean canDeployNow() {
      if (this.mc.player == null || this.mc.world == null) {
         return false;
      } else if (this.mc.player.isOnGround()) {
         return false;
      } else if (this.mc.player.hasVehicle()) {
         return false;
      } else if (this.mc.player.isTouchingWater() || this.mc.player.isInLava()) {
         return false;
      } else if (this.mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
         return false;
      } else if ((Boolean)this.spaceCheck.get() && !this.hasRoomToFly()) {
         return false;
      } else {
         if ((Boolean)this.safeTakeoff.get()) {
            if (this.airTicks < (Integer)this.takeoffDelay.get()) {
               return false;
            }

            if ((Boolean)this.requireFalling.get() && this.mc.player.getVelocity().y >= 0.0) {
               return false;
            }

            if (this.takeoffCooldownTicks > 0) {
               return false;
            }
         }

         return true;
      }
   }

   private void deployElytra(boolean forceSync) {
      if (!this.airborneSynced && (Boolean)this.safeTakeoff.get() && ((Boolean)this.sendPositionBefore.get() || forceSync)) {
         this.mc
            .getNetworkHandler()
            .sendPacket(
               new Full(
                  this.mc.player.getX(),
                  this.mc.player.getY(),
                  this.mc.player.getZ(),
                  this.yaw,
                  this.pitch,
                  false,
                  this.mc.player.horizontalCollision
               )
            );
      }

      this.mc
         .getNetworkHandler()
         .sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
      this.mc.player.startGliding();
      this.takeoffCooldownTicks = (Integer)this.takeoffCooldown.get();
      this.deployCount++;
      if ((Boolean)this.deBug.get()) {
         this.info("deploy elytra #" + this.deployCount + " airTicks=" + this.airTicks, new Object[0]);
      }
   }

   private boolean hasRoomToFly() {
      Box box = this.mc.player.getBoundingBox().contract(0.001);
      return !this.mc.world.isSpaceEmpty(box) ? false : this.mc.world.isSpaceEmpty(box.offset(0.0, 0.35, 0.0));
   }

   public void offFirework() {
      if (this.fireworkTimer.passedMs((Double)this.delay.get()) || this.fireWorkMode.get() != FireworkElytraFly.FireWorkMode.Delay) {
         if (this.mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, this.yaw, this.pitch));
            this.fireworkTimer.reset();
         } else if (this.mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, this.yaw, this.pitch));
            this.fireworkTimer.reset();
         } else {
            int firework;
            if ((Boolean)this.inventorySwap.get() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               InventoryUtil.inventorySwap(firework, ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot());
               this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, this.yaw, this.pitch));
               InventoryUtil.inventorySwap(firework, ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot());
               Rotation.sendPacket(new CloseHandledScreenC2SPacket(this.mc.player.currentScreenHandler.syncId));
               this.fireworkTimer.reset();
            } else if ((firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET)) != -1) {
               int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               InventoryUtil.switchToSlot(firework);
               this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, this.yaw, this.pitch));
               InventoryUtil.switchToSlot(old);
               this.fireworkTimer.reset();
            }
         }
      }
   }

   public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (this.mc.getNetworkHandler() != null && this.mc.world != null) {
         PendingUpdateManager pendingUpdateManager = this.mc.world.getPendingUpdateManager().incrementSequence();

         try {
            int i = pendingUpdateManager.getSequence();
            this.mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
         } catch (Throwable var6) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      }
   }

   private boolean wantToMove() {
      return this.mc.options.forwardKey.isPressed()
         || this.mc.options.backKey.isPressed()
         || this.mc.options.leftKey.isPressed()
         || this.mc.options.rightKey.isPressed()
         || this.mc.options.jumpKey.isPressed()
         || this.mc.options.sneakKey.isPressed();
   }

   public boolean isMoving() {
      return this.mc.player == null
         ? false
         : this.mc.player.input.playerInput.forward()
            || this.mc.player.input.playerInput.backward()
            || this.mc.player.input.playerInput.left()
            || this.mc.player.input.playerInput.right();
   }

   public float getSprintYaw(float yaw) {
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

      return yaw;
   }

   private float getPitch(float pitch) {
      if (!(this.mc.currentScreen instanceof ChatScreen)) {
         boolean pressingWASD = this.mc.options.forwardKey.isPressed()
            || this.mc.options.backKey.isPressed()
            || this.mc.options.leftKey.isPressed()
            || this.mc.options.rightKey.isPressed();
         if (this.mc.options.sneakKey.isPressed() && this.mc.options.jumpKey.isPressed()) {
            pitch = -3.0F;
         } else if (this.mc.options.jumpKey.isPressed()) {
            if (pressingWASD) {
               pitch = -45.0F;
            } else {
               pitch = -90.0F;
            }
         } else if (this.mc.options.sneakKey.isPressed()) {
            if (pressingWASD) {
               pitch = 45.0F;
            } else {
               pitch = 90.0F;
            }
         }

         if (pressingWASD && !this.mc.options.sneakKey.isPressed() && !this.mc.options.jumpKey.isPressed()) {
            pitch = -1.9F;
         }
      }

      return pitch;
   }

   public boolean isPhased() {
      return this.mc.world.canCollide(this.mc.player, this.mc.player.getBoundingBox());
   }

   private void clickSlot(int screenSlot, int button, SlotActionType type) {
      if (this.mc.player != null) {
         if (this.mc.player.currentScreenHandler == this.mc.player.playerScreenHandler) {
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, screenSlot, button, type, this.mc.player);
         }
      }
   }

   public static enum FireWorkMode {
      Auto,
      Delay,
      None;
   }

   public static enum Mode {
      Legit,
      GrimDurability;
   }
}
