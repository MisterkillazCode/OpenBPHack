package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.LivingEntityAccessor;
import com.codigohasta.addon.utils.alien.AlienBlockUtil;
import com.codigohasta.addon.utils.alien.AlienDamageUtils;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BPHackFakePlayer extends Module {
   public static BPHackFakePlayer INSTANCE;
   public BPHackFakePlayer.FakePlayerEntity fakePlayer;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<String> name = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("name")).description("The name of the fake player.")).defaultValue("FakePlayer")).build());
   private final Setting<Boolean> damage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("damage"))
                  .description("Simulate damage from attacks and explosions."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoTotem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-totem"))
                  .description("Automatically give totems to the fake player."))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> record = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("record"))
                  .description("Record the real player's movement."))
               .defaultValue(false))
            .build()
      );
   public final Setting<Boolean> play = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("play"))
                  .description("Play back recorded movement on the fake player."))
               .defaultValue(false))
            .build()
      );
   private final List<BPHackFakePlayer.PlayerState> positions = new ArrayList<>();
   private int movementTick;
   private boolean lastRecordValue;
   private boolean pendingHurtSound;
   private boolean pendingCritSound;
   private boolean pendingTotemPopVisuals;

   public BPHackFakePlayer() {
      super(AddonTemplate.CATEGORY, "BPHackFakePlayer", "Spawns a fake player that simulates damage and totem pops. Can record and replay your movement.");
      INSTANCE = this;
   }

   public String getInfoString() {
      return (String)this.name.get();
   }

   public void onActivate() {
      if (this.mc.player == null) {
         this.toggle();
      } else {
         this.fakePlayer = new BPHackFakePlayer.FakePlayerEntity(this.mc.player, (String)this.name.get());
         this.mc.world.addEntity(this.fakePlayer);
      }
   }

   public void onDeactivate() {
      if (this.fakePlayer != null) {
         this.fakePlayer.discard();
         this.fakePlayer = null;
      }

      this.positions.clear();
      this.movementTick = 0;
      this.lastRecordValue = false;
      this.pendingHurtSound = false;
      this.pendingCritSound = false;
      this.pendingTotemPopVisuals = false;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.fakePlayer != null && !this.fakePlayer.isRemoved()) {
         if ((Boolean)this.autoTotem.get()) {
            if (this.fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               this.fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            if (this.fakePlayer.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               this.fakePlayer.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
         }

         if ((Boolean)this.record.get() != this.lastRecordValue && (Boolean)this.record.get()) {
            this.positions.clear();
         }

         this.lastRecordValue = (Boolean)this.record.get();
         if ((Boolean)this.record.get()) {
            this.positions
               .add(
                  new BPHackFakePlayer.PlayerState(
                     this.mc.player.getX(),
                     this.mc.player.getY(),
                     this.mc.player.getZ(),
                     this.mc.player.getYaw(),
                     this.mc.player.getPitch()
                  )
               );
         }

         if ((Boolean)this.play.get() && !this.positions.isEmpty()) {
            this.movementTick++;
            if (this.movementTick >= this.positions.size()) {
               this.movementTick = 0;
            }

            BPHackFakePlayer.PlayerState p = this.positions.get(this.movementTick);
            this.fakePlayer.setYaw(p.yaw);
            this.fakePlayer.setPitch(p.pitch);
            this.fakePlayer.setHeadYaw(p.yaw);
            this.fakePlayer.updateTrackedPosition(p.x, p.y, p.z);
            this.fakePlayer.updateTrackedPositionAndAngles(new Vec3d(p.x, p.y, p.z), p.yaw, p.pitch);
         }

         if (this.pendingHurtSound) {
            this.mc
               .world
               .playSound(
                  null,
                  this.fakePlayer.getX(),
                  this.fakePlayer.getY(),
                  this.fakePlayer.getZ(),
                  SoundEvents.ENTITY_PLAYER_HURT,
                  SoundCategory.PLAYERS,
                  1.0F,
                  1.0F
               );
            this.pendingHurtSound = false;
         }

         if (this.pendingCritSound) {
            this.mc
               .world
               .playSound(
                  null,
                  this.fakePlayer.getX(),
                  this.fakePlayer.getY(),
                  this.fakePlayer.getZ(),
                  SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                  SoundCategory.PLAYERS,
                  1.0F,
                  1.0F
               );
            this.mc.player.addCritParticles(this.fakePlayer);
            this.pendingCritSound = false;
         }

         if (this.pendingTotemPopVisuals) {
            this.spawnTotemPopVisuals(this.fakePlayer);
            this.pendingTotemPopVisuals = false;
         }
      } else {
         this.toggle();
      }
   }

   @EventHandler
   private void onSendPacket(Send event) {
      if ((Boolean)this.damage.get() && this.fakePlayer != null) {
         if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket)packet;
            if (String.valueOf(accessor.meteor$getType()).equals("ATTACK")) {
               if (accessor.meteor$getEntity() == this.fakePlayer) {
                  float dmg = AlienDamageUtils.getAttackDamage(this.mc.player, this.fakePlayer);
                  boolean isCrit = this.mc.player.fallDistance > 0.0
                     && !this.mc.player.isOnGround()
                     && !this.mc.player.isClimbing()
                     && !this.mc.player.isTouchingWater()
                     && !this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                     && !this.mc.player.hasVehicle();
                  if (this.fakePlayer.hurtTime <= 0) {
                     this.fakePlayer.onDamaged(this.mc.world.getDamageSources().generic());
                     if (this.fakePlayer.getAbsorptionAmount() >= dmg) {
                        this.fakePlayer.setAbsorptionAmount(this.fakePlayer.getAbsorptionAmount() - dmg);
                     } else {
                        float remaining = dmg - this.fakePlayer.getAbsorptionAmount();
                        this.fakePlayer.setAbsorptionAmount(0.0F);
                        this.fakePlayer.setHealth(this.fakePlayer.getHealth() - remaining);
                     }

                     if (this.fakePlayer.isDead()) {
                        this.tryTotemPop(this.fakePlayer);
                     }

                     this.fakePlayer.hurtTime = 10;
                     this.fakePlayer.maxHurtTime = 10;
                     this.fakePlayer.animateDamage(0.0F);
                  }

                  if (isCrit) {
                     this.pendingCritSound = true;
                  } else {
                     this.pendingHurtSound = true;
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      if ((Boolean)this.damage.get() && this.fakePlayer != null && this.fakePlayer.hurtTime <= 0) {
         if (event.packet instanceof ExplosionS2CPacket explosion) {
            Vec3d explosionPos = explosion.center();
            if (!(explosionPos.squaredDistanceTo(new Vec3d(this.fakePlayer.getX(), this.fakePlayer.getY(), this.fakePlayer.getZ())) > 100.0)) {
               float dmg;
               if (AlienBlockUtil.getBlock(BlockPos.ofFloored(explosionPos)) == Blocks.RESPAWN_ANCHOR) {
                  dmg = AlienDamageUtils.explosionDamage(this.fakePlayer, explosionPos, 10.0F);
               } else {
                  dmg = AlienDamageUtils.explosionDamage(this.fakePlayer, explosionPos, 12.0F);
               }

               this.fakePlayer.onDamaged(this.mc.world.getDamageSources().generic());
               if (this.fakePlayer.getAbsorptionAmount() >= dmg) {
                  this.fakePlayer.setAbsorptionAmount(this.fakePlayer.getAbsorptionAmount() - dmg);
               } else {
                  float remaining = dmg - this.fakePlayer.getAbsorptionAmount();
                  this.fakePlayer.setAbsorptionAmount(0.0F);
                  this.fakePlayer.setHealth(this.fakePlayer.getHealth() - remaining);
               }

               if (this.fakePlayer.isDead()) {
                  this.tryTotemPop(this.fakePlayer);
               }
            }
         }
      }
   }

   private void tryTotemPop(BPHackFakePlayer.FakePlayerEntity fp) {
      boolean hasTotem = fp.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING || fp.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING;
      if (hasTotem) {
         fp.setHealth(10.0F);
         fp.clearStatusEffects();
         fp.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
         fp.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
         fp.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
         if (fp.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            fp.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
         } else {
            fp.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
         }

         this.pendingTotemPopVisuals = true;
      }
   }

   private void spawnTotemPopVisuals(BPHackFakePlayer.FakePlayerEntity fp) {
      if (this.mc.world != null) {
         for (int i = 0; i < 30; i++) {
            double vx = (this.mc.world.random.nextDouble() - 0.5) * 0.5;
            double vy = this.mc.world.random.nextDouble() * 0.5;
            double vz = (this.mc.world.random.nextDouble() - 0.5) * 0.5;
            this.mc
               .particleManager
               .addParticle(
                  ParticleTypes.TOTEM_OF_UNDYING, fp.getX() + vx * 2.0, fp.getY() + 1.0 + vy * 2.0, fp.getZ() + vz * 2.0, vx, vy + 0.5, vz
               );
         }

         this.mc
            .world
            .playSound(null, fp.getX(), fp.getY(), fp.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
         BPHackTips.onFakePlayerTotemPop(fp.getName().getString(), fp);
         BPHackPopChams.onFakePlayerTotemPop(fp);
      }
   }

   public class FakePlayerEntity extends OtherClientPlayerEntity {
      private final boolean ground;

      public FakePlayerEntity(PlayerEntity player, String name) {
         super(BPHackFakePlayer.this.mc.world, new GameProfile(UUID.fromString("66666666-6666-6666-6666-666666666666"), name));
         this.copyPositionAndRotation(player);
         this.lastRenderX = player.lastRenderX;
         this.lastRenderZ = player.lastRenderZ;
         this.lastRenderY = player.lastRenderY;
         this.bodyYaw = player.bodyYaw;
         this.headYaw = player.headYaw;
         this.handSwingProgress = player.handSwingProgress;
         this.handSwingTicks = player.handSwingTicks;
         this.limbAnimator.setSpeed(player.limbAnimator.getSpeed());
         ((LivingEntityAccessor)this).setLeaningPitch(((LivingEntityAccessor)player).getLeaningPitch());
         ((LivingEntityAccessor)this).setLastLeaningPitch(((LivingEntityAccessor)player).getLeaningPitch());
         this.touchingWater = player.isTouchingWater();
         this.setSneaking(player.isSneaking());
         this.setPose(player.getPose());
         this.ground = player.isOnGround();
         this.setOnGround(this.ground);
         this.getInventory().clone(player.getInventory());
         this.setAbsorptionAmount(player.getAbsorptionAmount());
         this.setHealth(player.getHealth());
         this.setBoundingBox(player.getBoundingBox());
      }

      public boolean isOnGround() {
         return this.ground;
      }

      public boolean isSpectator() {
         return false;
      }

      public boolean isCreative() {
         return false;
      }
   }

   private record PlayerState(double x, double y, double z, float yaw, float pitch) {
   }
}
