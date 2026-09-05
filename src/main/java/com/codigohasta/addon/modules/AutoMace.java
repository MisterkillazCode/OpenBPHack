package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoMace extends Module {
   private static final int ARMOR_CHEST = EquipmentSlot.CHEST.getEntitySlotId();
   private static final Item[] CHESTPLATES = new Item[]{
      Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE
   };
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCombat = this.settings.createGroup("Combat");
   private final Setting<Boolean> idleFly = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("idle-fly")).description("Keep flying even when there is no target.")).defaultValue(true)).build());
   private final Setting<Double> flightSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("flight-speed"))
               .description("Glide velocity magnitude while chasing (blocks/tick). 0.8 is stable on Grim; higher values risk divergence / setback / 'bounce'."))
            .defaultValue(0.8)
            .min(0.3)
            .sliderMax(2.0)
            .build()
      );
   private final Setting<Double> climbPitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("climb-pitch"))
               .description("Look-up angle (degrees) while climbing. Higher = steeper climb."))
            .defaultValue(55.0)
            .min(20.0)
            .sliderMax(80.0)
            .build()
      );
   private final Setting<Double> maxHeight = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("max-height"))
               .description("Stop climbing at this many blocks above the takeoff point."))
            .defaultValue(50.0)
            .min(4.0)
            .sliderMax(120.0)
            .max(256.0)
            .build()
      );
   private final Setting<Integer> fireworkDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("firework-delay"))
                  .description("Milliseconds between off-hand firework uses while climbing."))
               .defaultValue(1400))
            .min(100)
            .sliderMax(3000)
            .build()
      );
   private final Setting<Boolean> useFireworks = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("use-fireworks")).description("Use off-hand fireworks to boost the climb.")).defaultValue(true))
            .build()
      );
   private final Setting<Double> turnSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("turn-speed"))
               .description(
                  "How fast the look turns per tick while gliding (degrees). Higher = tighter tracking so the flight path does not curve/overshoot a moving target."
               ))
            .defaultValue(30.0)
            .min(4.0)
            .sliderMax(60.0)
            .build()
      );
   private final Setting<Boolean> hover = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("hover")).description("After attacking, take off again and keep chasing instead of landing."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> diveChestplate = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("dive-chestplate"))
                  .description("During the dive swap the elytra for a chestplate (if available) so you free-fall with armor."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> debug = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("debug")).description("Print phase changes and a one-line status every second.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoSwitch = this.sgCombat
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("auto-switch")).description("Grab the mace from the hotbar or backpack before attacking."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> players = this.sgCombat
      .add(((Builder)((Builder)((Builder)new Builder().name("players")).description("Attack players.")).defaultValue(true)).build());
   private final Setting<Set<EntityType<?>>> entities = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
                  .name("entities"))
               .description("Extra entities to attack."))
            .onlyAttackable()
            .defaultValue(new EntityType[]{EntityType.PLAYER})
            .build()
      );
   private final Setting<Double> searchRange = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("target-search-range"))
               .description("How far away a target may be to be chased."))
            .defaultValue(100.0)
            .min(8.0)
            .sliderMax(200.0)
            .build()
      );
   private final Setting<Double> diveRadius = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("dive-radius"))
               .description("Horizontal distance to the target at which the chase switches to the glide-dive."))
            .defaultValue(16.0)
            .min(2.0)
            .sliderMax(40.0)
            .build()
      );
   private final Setting<Double> diveHeight = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("dive-height"))
               .description("Height above the target at which the glide-dive may start."))
            .defaultValue(8.0)
            .min(3.0)
            .sliderMax(30.0)
            .build()
      );
   private final Setting<Double> attackRange = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("attack-range"))
               .description("3D distance at which the mace attack fires."))
            .defaultValue(3.5)
            .min(0.5)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> minFallDistance = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("min-fall-distance"))
               .description("Required accumulated fall distance before attacking (Mace Smash bonus). Set to 0 to hit as soon as in range."))
            .defaultValue(1.5)
            .min(0.0)
            .sliderMax(25.0)
            .build()
      );
   private final Setting<Integer> attackDelay = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("attack-delay"))
                  .description("Cooldown between attacks in ticks."))
               .defaultValue(12))
            .min(0)
            .sliderMax(60)
            .build()
      );
   private final Setting<Boolean> swing = this.sgCombat
      .add(((Builder)((Builder)((Builder)new Builder().name("swing")).description("Send a swing animation with the attack.")).defaultValue(true)).build());
   private final Setting<AutoMace.TargetMode> targetMode = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("target-mode"))
                  .description("Which target to prioritise: Nearest (closest) or Farthest (furthest within range)."))
               .defaultValue(AutoMace.TargetMode.Nearest))
            .build()
      );
   private AutoMace.Phase phase = AutoMace.Phase.IDLE;
   private Entity target = null;
   private double startY = 0.0;
   private int takeoffTicks = 0;
   private int attackCooldown = 0;
   private long lastEquipMs = 0L;
   private long lastStatusMs = 0L;
   private boolean divePrepared = false;
   private float lastLookYaw = 0.0F;
   private float lastLookPitch = 0.0F;
   private boolean hasLastLook = false;
   private boolean pendingFirework = false;
   private long lastFireworkMs = 0L;
   private boolean hasWarnedNoFirework = false;
   private boolean warnedElytra = false;
   private boolean warnedFirework = false;
   private boolean warnedMace = false;
   private long diveStartMs = 0L;

   private static boolean isChestArmor(Item item) {
      for (Item c : CHESTPLATES) {
         if (item == c) {
            return true;
         }
      }

      return false;
   }

   public AutoMace() {
      super(AddonTemplate.CATEGORY, "AutoMace", "Fully automatic elytra mace chaser.");
   }

   public void onActivate() {
      this.phase = AutoMace.Phase.IDLE;
      this.target = null;
      this.takeoffTicks = 0;
      this.attackCooldown = 0;
      this.divePrepared = false;
      this.pendingFirework = false;
      this.lastEquipMs = 0L;
      this.lastStatusMs = 0L;
      this.hasLastLook = false;
      this.hasWarnedNoFirework = false;
      this.warnedElytra = false;
      this.warnedFirework = false;
      this.warnedMace = false;
      this.diveStartMs = 0L;
      if (this.mc.player != null) {
         this.ensureElytra();
         this.ensureOffhandFirework();
      }

      super.onActivate();
   }

   public void onDeactivate() {
      if (this.divePrepared) {
         this.ensureElytra();
         this.divePrepared = false;
      }

      if (this.mc.options != null) {
         this.mc.options.forwardKey.setPressed(false);
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.attackCooldown > 0) {
            this.attackCooldown--;
         }

         if (this.pendingFirework) {
            this.pendingFirework = false;
            if ((Boolean)this.useFireworks.get() && this.mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET && this.isGliding()) {
               this.mc.interactionManager.interactItem(this.mc.player, Hand.OFF_HAND);
               this.mc.player.swingHand(Hand.OFF_HAND);
               this.lastFireworkMs = this.now();
            }
         }

         if (this.phase != AutoMace.Phase.DIVE && this.now() - this.lastEquipMs > 500L) {
            this.ensureElytra();
            this.ensureOffhandFirework();
            this.checkRequirements();
            this.lastEquipMs = this.now();
         }

         this.target = this.findTarget();
         if ((Boolean)this.debug.get() && this.now() - this.lastStatusMs > 1000L) {
            Vec3d v = this.mc.player.getVelocity();
            this.info(
               "(highlight)AutoMace "
                  + this.phase
                  + " | elytra="
                  + this.hasElytra()
                  + " gliding="
                  + this.isGliding()
                  + " fw="
                  + (this.mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET)
                  + " tgt="
                  + (this.target == null ? "none" : this.target.getName().getString())
                  + " fall="
                  + String.format("%.1f", this.mc.player.fallDistance)
                  + " vel="
                  + String.format("%.2f", v.horizontalLength())
                  + "/"
                  + String.format("%.2f", v.y),
               new Object[0]
            );
            this.lastStatusMs = this.now();
         }

         switch (this.phase) {
            case IDLE:
               if ((this.target != null || (Boolean)this.idleFly.get()) && this.hasElytra()) {
                  this.startY = this.mc.player.getY();
                  this.takeoffTicks = 0;
                  this.setPhase(AutoMace.Phase.TAKE_OFF);
               }
               break;
            case TAKE_OFF:
               if (this.isGliding()) {
                  this.startY = this.mc.player.getY();
                  this.takeoffTicks = 0;
                  this.hasLastLook = false;
                  this.setPhase(AutoMace.Phase.ASCEND);
                  return;
               }

               if (!this.hasElytra()) {
                  this.takeoffTicks++;
                  if (this.takeoffTicks > 80) {
                     this.takeoffTicks = 0;
                     this.setPhase(AutoMace.Phase.IDLE);
                  }

                  return;
               }

               this.takeoffTicks++;
               if (this.takeoffTicks > 80) {
                  this.takeoffTicks = 0;
                  if (this.mc.options.forwardKey.isPressed()) {
                     this.mc.options.forwardKey.setPressed(false);
                  }

                  this.setPhase(AutoMace.Phase.IDLE);
                  return;
               }

               if (this.takeoffTicks % 4 != 0) {
                  return;
               }

               if (this.mc.player.isOnGround()) {
                  this.mc.player.jump();
               } else if (!this.mc.player.isSubmergedInWater() && this.canStartGliding()) {
                  this.sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_FALL_FLYING));
               }
               break;
            case ASCEND:
               if (!this.isGliding()) {
                  this.takeoffTicks = 0;
                  this.setPhase(AutoMace.Phase.TAKE_OFF);
                  return;
               }

               this.mc.options.forwardKey.setPressed(false);
               Vec3d toTarget;
               double distXZ;
               double above;
               if (this.target != null) {
                  Vec3d targetCenter = this.target.getBoundingBox().getCenter();
                  toTarget = targetCenter.subtract(this.mc.player.getEyePos());
                  distXZ = Math.hypot(toTarget.x, toTarget.z);
                  above = this.mc.player.getY() - this.target.getY();
               } else {
                  toTarget = this.direction(this.mc.player.getYaw(), 0.0);
                  distXZ = 1.0;
                  above = this.mc.player.getY() - this.startY < this.maxHeight.get() ? -1.0 : (Double)this.diveHeight.get();
               }

               if (this.target != null && distXZ <= (Double)this.diveRadius.get() && above >= (Double)this.diveHeight.get()) {
                  this.divePrepared = false;
                  this.setPhase(AutoMace.Phase.DIVE);
                  return;
               }

               float yaw;
               float pitch;
               if (this.target != null) {
                  yaw = (float)Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0F;
                  if (above < (Double)this.diveHeight.get() && this.mc.player.getY() - this.startY < (Double)this.maxHeight.get()) {
                     pitch = (float)(-(Double)this.climbPitch.get());
                  } else {
                     pitch = (float)(-Math.toDegrees(Math.atan2(toTarget.y, distXZ)));
                  }
               } else {
                  yaw = this.mc.player.getYaw();
                  pitch = (float)(-(Double)this.climbPitch.get());
               }

               this.turnSmooth(yaw, pitch);
               this.sendPacket(
                  new Full(
                     this.mc.player.getX(),
                     this.mc.player.getY(),
                     this.mc.player.getZ(),
                     this.mc.player.getYaw(),
                     this.mc.player.getPitch(),
                     this.mc.player.isOnGround(),
                     this.mc.player.horizontalCollision
                  )
               );
               Vec3d glideDir = this.direction(this.mc.player.getYaw(), this.mc.player.getPitch());
               this.mc.player.setVelocity(this.clampElytraSpeed(glideDir.multiply((Double)this.flightSpeed.get())));
               if ((Boolean)this.useFireworks.get()
                  && pitch <= -(Double)this.climbPitch.get() + 5.0
                  && this.mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET
                  && this.now() - this.lastFireworkMs > ((Integer)this.fireworkDelay.get()).intValue()
                  && !this.pendingFirework) {
                  this.pendingFirework = true;
               }
               break;
            case DIVE:
               if (this.target == null || !this.target.isAlive()) {
                  this.ensureElytra();
                  this.divePrepared = false;
                  this.setPhase(this.hover.get() ? AutoMace.Phase.TAKE_OFF : AutoMace.Phase.IDLE);
                  return;
               }

               if (this.diveStartMs > 0L && this.now() - this.diveStartMs > 15000L) {
                  if ((Boolean)this.debug.get()) {
                     this.info("(highlight)AutoMace 俯冲超时，重新盘旋", new Object[0]);
                  }

                  this.ensureElytra();
                  this.ensureOffhandFirework();
                  this.divePrepared = false;
                  this.startY = this.mc.player.getY();
                  this.takeoffTicks = 0;
                  this.setPhase(AutoMace.Phase.ASCEND);
                  return;
               }

               if ((Boolean)this.autoSwitch.get() && this.mc.player.getMainHandStack().getItem() != Items.MACE) {
                  this.holdMace();
               }

               Vec3d toTargetx = this.target.getBoundingBox().getCenter().subtract(this.mc.player.getEyePos());
               double distXZx = Math.hypot(toTargetx.x, toTargetx.z);
               double abovex = this.mc.player.getY() - this.target.getY();
               float yawx = (float)Math.toDegrees(Math.atan2(toTargetx.z, toTargetx.x)) - 90.0F;
               float pitchx = (float)(-Math.toDegrees(Math.atan2(toTargetx.y, distXZx)));
               if (!this.divePrepared) {
                  boolean closeEnough = distXZx <= 1.5;
                  boolean highEnough = abovex >= (Double)this.diveHeight.get() - 1.0;
                  if (closeEnough && highEnough) {
                     if (this.mc.player.getVelocity().horizontalLength() < 1.0) {
                        if (!this.removeElytraForDive()) {
                           if ((Boolean)this.debug.get() && this.now() - this.lastStatusMs > 1000L) {
                              this.info("(highlight)AutoMace 背包已满且无胸甲，无法脱鞘翅，放弃俯冲", new Object[0]);
                              this.lastStatusMs = this.now();
                           }

                           this.setPhase(AutoMace.Phase.ASCEND);
                           return;
                        }

                        this.mc.options.forwardKey.setPressed(false);
                        this.mc.player.setVelocity(0.0, -0.25, 0.0);
                        this.sendPacket(
                           new PositionAndOnGround(
                              this.mc.player.getX(),
                              this.mc.player.getY(),
                              this.mc.player.getZ(),
                              this.mc.player.isOnGround(),
                              this.mc.player.horizontalCollision
                           )
                        );
                        this.divePrepared = true;
                        if ((Boolean)this.debug.get()) {
                           this.info("(highlight)AutoMace elytra stripped, falling", new Object[0]);
                        }
                     } else {
                        this.turnSmooth(yawx, 25.0F);
                        Vec3d glideDir = this.direction(this.mc.player.getYaw(), this.mc.player.getPitch());
                        this.mc.player.setVelocity(glideDir.multiply(0.6));
                     }

                     return;
                  }

                  float trackPitch;
                  if (!highEnough) {
                     trackPitch = (float)(-(Double)this.climbPitch.get());
                  } else if (distXZx > 6.0) {
                     double desiredDrop = abovex - (Double)this.diveHeight.get();
                     trackPitch = (float)(-Math.toDegrees(Math.atan2(desiredDrop, distXZx)));
                     trackPitch = Math.max(trackPitch, -30.0F);
                  } else {
                     trackPitch = -10.0F;
                  }

                  this.turnSmooth(yawx, trackPitch);
                  Vec3d glideDir = this.direction(this.mc.player.getYaw(), this.mc.player.getPitch());
                  double speed = distXZx > 3.0 ? (Double)this.flightSpeed.get() : 0.8;
                  this.mc.player.setVelocity(this.clampElytraSpeed(glideDir.multiply(speed)));
                  return;
               }

               this.mc.player.setYaw(yawx);
               this.mc.player.setPitch(pitchx);
               this.sendPacket(new LookAndOnGround(yawx, pitchx, this.mc.player.isOnGround(), this.mc.player.horizontalCollision));
               if (this.mc.options.forwardKey.isPressed()) {
                  this.mc.options.forwardKey.setPressed(false);
               }

               double dist3D = this.mc.player.distanceTo(this.target);
               boolean falling = this.mc.player.getVelocity().y < 0.0;
               if ((Boolean)this.debug.get() && this.now() - this.lastStatusMs > 500L) {
                  this.info(
                     "(highlight)AutoMace DIVE fall="
                        + String.format("%.1f", this.mc.player.fallDistance)
                        + " dist="
                        + String.format("%.1f", dist3D)
                        + " mace="
                        + (this.mc.player.getMainHandStack().getItem() == Items.MACE),
                     new Object[0]
                  );
                  this.lastStatusMs = this.now();
               }

               if (falling
                  && dist3D <= Math.min((Double)this.attackRange.get(), 2.9)
                  && this.mc.player.fallDistance >= (Double)this.minFallDistance.get()
                  && this.attackCooldown == 0
                  && this.doAttack(this.target)) {
                  this.attackCooldown = (Integer)this.attackDelay.get();
                  if ((Boolean)this.debug.get()) {
                     this.info("(highlight)AutoMace HIT fall=" + String.format("%.1f", this.mc.player.fallDistance), new Object[0]);
                  }

                  this.ensureElytra();
                  this.ensureOffhandFirework();
                  this.divePrepared = false;
                  this.startY = this.mc.player.getY();
                  this.takeoffTicks = 0;
                  this.setPhase(this.hover.get() ? AutoMace.Phase.TAKE_OFF : AutoMace.Phase.IDLE);
                  return;
               }

               if (this.mc.player.getY() < this.target.getY() - 2.0) {
                  this.ensureElytra();
                  this.divePrepared = false;
                  this.startY = this.mc.player.getY();
                  this.takeoffTicks = 0;
                  this.setPhase(this.hover.get() ? AutoMace.Phase.TAKE_OFF : AutoMace.Phase.IDLE);
               }
         }
      }
   }

   private boolean doAttack(Entity target) {
      if ((Boolean)this.autoSwitch.get() && !this.holdMace()) {
         if ((Boolean)this.debug.get()) {
            this.info("(highlight)AutoMace no mace found", new Object[0]);
         }

         return false;
      } else {
         this.sendPacket(
            new LookAndOnGround(
               this.mc.player.getYaw(), this.mc.player.getPitch(), this.mc.player.isOnGround(), this.mc.player.horizontalCollision
            )
         );
         this.mc.interactionManager.attackEntity(this.mc.player, target);
         if ((Boolean)this.swing.get()) {
            this.mc.player.swingHand(Hand.MAIN_HAND);
         }

         return true;
      }
   }

   private boolean holdMace() {
      if (this.mc.player.getMainHandStack().getItem() == Items.MACE) {
         return true;
      } else {
         FindItemResult mace = InvUtils.find(new Item[]{Items.MACE});
         if (!mace.found()) {
            return false;
         } else {
            int selected = this.mc.player.getInventory().getSelectedSlot();
            if (mace.isOffhand()) {
               InvUtils.move().fromOffhand().toHotbar(selected);
               return true;
            } else if (mace.slot() >= 0 && mace.slot() <= 8) {
               InvUtils.swap(mace.slot(), false);
               return true;
            } else {
               InvUtils.move().from(mace.slot()).toHotbar(selected);
               return true;
            }
         }
      }
   }

   private boolean hasElytra() {
      ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
      return chest.getItem() == Items.ELYTRA && chest.getDamage() < chest.getMaxDamage() - 1;
   }

   private void ensureElytra() {
      if (this.mc.player != null && !this.hasElytra()) {
         if (InvUtils.testInOffHand(new Item[]{Items.ELYTRA})) {
            InvUtils.move().fromOffhand().toArmor(ARMOR_CHEST);
         } else {
            FindItemResult elytra = InvUtils.find(stack -> stack.getItem() == Items.ELYTRA, 0, 35);
            if (elytra.found()) {
               InvUtils.move().from(elytra.slot()).toArmor(ARMOR_CHEST);
            }
         }
      }
   }

   private boolean removeElytraForDive() {
      if (!this.hasElytra()) {
         return true;
      } else {
         if ((Boolean)this.diveChestplate.get()) {
            FindItemResult cp = InvUtils.find(stack -> isChestArmor(stack.getItem()), 0, 35);
            if (cp.found()) {
               InvUtils.move().from(cp.slot()).toArmor(ARMOR_CHEST);
               return true;
            }
         }

         FindItemResult empty = InvUtils.find(ItemStack::isEmpty, 0, 35);
         if (!empty.found()) {
            return false;
         } else {
            InvUtils.move().fromArmor(ARMOR_CHEST).to(empty.slot());
            return true;
         }
      }
   }

   private void ensureOffhandFirework() {
      if (this.mc.player != null) {
         if (this.mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            this.hasWarnedNoFirework = false;
         } else {
            FindItemResult fw = InvUtils.find(stack -> stack.getItem() == Items.FIREWORK_ROCKET, 0, 35);
            if (!fw.found()) {
               if (!this.hasWarnedNoFirework) {
                  this.warning("AutoMace: no Firework Rocket in inventory — cannot boost.", new Object[0]);
                  this.hasWarnedNoFirework = true;
               }
            } else if (!fw.isOffhand()) {
               InvUtils.move().from(fw.slot()).toOffhand();
            }
         }
      }
   }

   private void checkRequirements() {
      if (this.mc.player != null) {
         boolean noElytra = !this.hasElytra()
            && !InvUtils.testInOffHand(new Item[]{Items.ELYTRA})
            && !InvUtils.find(stack -> stack.getItem() == Items.ELYTRA, 0, 35).found();
         if (noElytra) {
            if (!this.warnedElytra) {
               this.warning("AutoMace: no Elytra in inventory — cannot fly.", new Object[0]);
               this.warnedElytra = true;
            }
         } else {
            this.warnedElytra = false;
         }

         boolean noFirework = this.mc.player.getOffHandStack().getItem() != Items.FIREWORK_ROCKET
            && !InvUtils.find(stack -> stack.getItem() == Items.FIREWORK_ROCKET, 0, 35).found();
         if (noFirework) {
            if (!this.warnedFirework) {
               this.warning("AutoMace: no Firework Rocket — climbing will be slow.", new Object[0]);
               this.warnedFirework = true;
            }
         } else {
            this.warnedFirework = false;
         }

         boolean noMace = (Boolean)this.autoSwitch.get()
            && this.mc.player.getMainHandStack().getItem() != Items.MACE
            && !InvUtils.testInOffHand(new Item[]{Items.MACE})
            && !InvUtils.find(stack -> stack.getItem() == Items.MACE, 0, 35).found();
         if (noMace) {
            if (!this.warnedMace) {
               this.warning("AutoMace: no Mace in inventory — no smash damage.", new Object[0]);
               this.warnedMace = true;
            }
         } else {
            this.warnedMace = false;
         }
      }
   }

   private boolean isGliding() {
      return this.mc.player.getPose() == EntityPose.GLIDING || this.mc.player.isGliding();
   }

   private boolean canStartGliding() {
      return this.mc.player.getVelocity().y <= 0.0 && this.mc.player.fallDistance > 0.2;
   }

   private Vec3d direction(double yaw, double pitch) {
      double yawRad = Math.toRadians(yaw);
      double pitchRad = Math.toRadians(pitch);
      return new Vec3d(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
   }

   private Vec3d clampElytraSpeed(Vec3d v) {
      double h = Math.hypot(v.x, v.z);
      if (h > 1.0) {
         double s = 1.0 / h;
         return new Vec3d(v.x * s, v.y, v.z * s);
      } else {
         return v;
      }
   }

   private void turnSmooth(float targetYaw, float targetPitch) {
      if (!this.hasLastLook) {
         this.lastLookYaw = this.mc.player.getYaw();
         this.lastLookPitch = this.mc.player.getPitch();
         this.hasLastLook = true;
      }

      float maxTurn = ((Double)this.turnSpeed.get()).floatValue();
      float dYaw = MathHelper.wrapDegrees(targetYaw - this.lastLookYaw);
      this.lastLookYaw = this.lastLookYaw + MathHelper.clamp(dYaw, -maxTurn, maxTurn);
      this.lastLookPitch = this.lastLookPitch + MathHelper.clamp(targetPitch - this.lastLookPitch, -maxTurn * 0.6F, maxTurn * 0.6F);
      this.mc.player.setYaw(this.lastLookYaw);
      this.mc.player.setPitch(this.lastLookPitch);
   }

   private void sendPacket(Packet<?> packet) {
      if (this.mc.getNetworkHandler() != null) {
         this.mc.getNetworkHandler().sendPacket(packet);
      }
   }

   private Entity findTarget() {
      if (!(Boolean)this.players.get() && ((Set)this.entities.get()).isEmpty()) {
         return null;
      } else {
         Entity best = null;
         boolean nearest = this.targetMode.get() == AutoMace.TargetMode.Nearest;
         double bestDist = nearest ? Double.MAX_VALUE : -1.0;

         for (Entity e : this.mc.world.getEntities()) {
            if (e instanceof LivingEntity
               && e.isAlive()
               && e != this.mc.player
               && !(
                  e instanceof PlayerEntity player
                     ? !(Boolean)this.players.get() || player.isCreative() || !Friends.get().shouldAttack(player)
                     : !((Set)this.entities.get()).contains(e.getType())
               )) {
               double dist = this.mc.player.distanceTo(e);
               if (!(dist > (Double)this.searchRange.get()) && (nearest ? dist < bestDist : dist > bestDist)) {
                  bestDist = dist;
                  best = e;
               }
            }
         }

         return best;
      }
   }

   private long now() {
      return System.currentTimeMillis();
   }

   private void setPhase(AutoMace.Phase newPhase) {
      if ((Boolean)this.debug.get() && newPhase != this.phase) {
         this.info("(highlight)AutoMace phase: " + this.phase + " -> " + newPhase, new Object[0]);
      }

      if (newPhase == AutoMace.Phase.DIVE) {
         this.diveStartMs = this.now();
      }

      this.phase = newPhase;
   }

   private static enum Phase {
      IDLE,
      TAKE_OFF,
      ASCEND,
      DIVE;
   }

   private static enum TargetMode {
      Nearest,
      Farthest;
   }
}
