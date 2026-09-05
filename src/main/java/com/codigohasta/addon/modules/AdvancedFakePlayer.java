package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AdvancedFakePlayer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCombat = this.settings.createGroup("Combat");
   private final Setting<String> name = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Name")).description("Dummy'sName")).defaultValue("CodigoHasta")).build());
   private final Setting<Integer> health = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Health"))
                  .description("Dummy'sHealth"))
               .defaultValue(20))
            .min(1)
            .sliderMax(36)
            .build()
      );
   private final Setting<Boolean> copyInv = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("SystemPack"))
                  .description("SystemyourPackItemgiveDummy"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> simulateDamage = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Damage"))
                  .description("whetherSimDamage(Attack/Explosion)"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> invulnerableTicks = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Invincible Time"))
                     .description("Take Damageafter'sInvincible Time(Ticks). Defaultare20. For0canTestLimitDPS."))
                  .defaultValue(20))
               .min(0)
               .max(20)
               .visible(this.simulateDamage::get))
            .build()
      );
   private final Setting<Boolean> autoTotem = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Held Totem"))
                     .description("whetherAutoatHandImageatDeathTimeTrigger"))
                  .defaultValue(true))
               .visible(this.simulateDamage::get))
            .build()
      );
   private final Setting<Boolean> showDamage = this.sgCombat
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Damage"))
                     .description("atchatSkyDisplayto'sDamageNumber"))
                  .defaultValue(true))
               .visible(this.simulateDamage::get))
            .build()
      );
   private final List<AdvancedFakePlayer.CustomFakePlayer> fakePlayers = new ArrayList<>();

   public AdvancedFakePlayer() {
      super(AddonTemplate.CATEGORY, "AdvancedFakePlayer", "Spawns an advanced fake player with custom health, damage handling and totem pops. (Incomplete.)");
   }

   public void onActivate() {
      if (this.mc.player != null) {
         this.spawnFakePlayer();
      }
   }

   public void onDeactivate() {
      this.removeAll();
   }

   public String getInfoString() {
      return String.valueOf(this.fakePlayers.size());
   }

   private void spawnFakePlayer() {
      AdvancedFakePlayer.CustomFakePlayer fp = new AdvancedFakePlayer.CustomFakePlayer(
         this.mc.player, (String)this.name.get(), ((Integer)this.health.get()).intValue(), (Boolean)this.copyInv.get()
      );
      fp.copyPositionAndRotation(this.mc.player);
      this.mc.world.addEntity(fp);
      this.fakePlayers.add(fp);
      this.info("AlreadySpawnCompleteDummy:" + (String)this.name.get(), new Object[0]);
   }

   private void removeAll() {
      for (AdvancedFakePlayer.CustomFakePlayer fp : this.fakePlayers) {
         fp.discard();
      }

      this.fakePlayers.clear();
   }

   @EventHandler
   private void onTick(Pre event) {
      if ((Boolean)this.simulateDamage.get()) {
         for (AdvancedFakePlayer.CustomFakePlayer fp : this.fakePlayers) {
            fp.tickCombat();
            if ((Boolean)this.autoTotem.get() && fp.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               fp.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
         }
      }
   }

   @EventHandler
   private void onAttack(Send event) {
      if ((Boolean)this.simulateDamage.get() && event.packet instanceof PlayerInteractEntityC2SPacket packet) {
         int targetId = this.getPacketEntityId(packet);
         if (targetId != -1) {
            if (this.mc.world.getEntityById(targetId) instanceof AdvancedFakePlayer.CustomFakePlayer fp && this.fakePlayers.contains(fp)) {
               if (this.mc.player.isUsingItem()) {
                  return;
               }

               float damage = DamageUtils.getAttackDamage(this.mc.player, fp);
               boolean isCrit = this.mc.player.fallDistance > 0.0
                  && !this.mc.player.isOnGround()
                  && !this.mc.player.isClimbing()
                  && !this.mc.player.isTouchingWater();
               if (isCrit) {
                  damage *= 1.5F;
               }

               fp.applyDamage(damage);
               this.mc
                  .world
                  .playSound(
                     this.mc.player,
                     fp.getX(),
                     fp.getY(),
                     fp.getZ(),
                     SoundEvents.ENTITY_PLAYER_HURT,
                     SoundCategory.PLAYERS,
                     1.0F,
                     1.0F
                  );
               if (isCrit) {
                  this.mc.player.addCritParticles(fp);
               }
            }
         }
      }
   }

   @EventHandler
   private void onExplosion(Receive event) {
      if ((Boolean)this.simulateDamage.get() && event.packet instanceof ExplosionS2CPacket packet) {
         Vec3d explosionPos = this.getExplosionPos(packet);
         if (explosionPos != null) {
            for (AdvancedFakePlayer.CustomFakePlayer fp : this.fakePlayers) {
               float damage = this.calculateReflectedDamage(fp, explosionPos);
               if (damage > 0.0F) {
                  fp.applyDamage(damage);
               }
            }
         }
      }
   }

   private Vec3d getExplosionPos(ExplosionS2CPacket packet) {
      try {
         List<Double> doubles = new ArrayList<>();

         for (Field f : ExplosionS2CPacket.class.getDeclaredFields()) {
            if (f.getType() == double.class) {
               f.setAccessible(true);
               doubles.add((Double)f.get(packet));
            }
         }

         if (doubles.size() >= 3) {
            return new Vec3d(doubles.get(0), doubles.get(1), doubles.get(2));
         }
      } catch (Exception var7) {
      }

      return null;
   }

   private float calculateReflectedDamage(LivingEntity entity, Vec3d pos) {
      try {
         for (Method method : DamageUtils.class.getMethods()) {
            if (method.getName().equals("crystalDamage")) {
               Class<?>[] params = method.getParameterTypes();
               if (params.length == 4 && params[2] == Box.class) {
                  return (Float)method.invoke(null, entity, pos, entity.getBoundingBox(), false);
               }

               if (params.length == 5 && params[2] == boolean.class) {
                  return (Float)method.invoke(null, entity, pos, false, entity.getBoundingBox(), false);
               }
            }
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }

      return 0.0F;
   }

   private int getPacketEntityId(PlayerInteractEntityC2SPacket packet) {
      try {
         for (Field field : PlayerInteractEntityC2SPacket.class.getDeclaredFields()) {
            if (field.getType() == int.class) {
               field.setAccessible(true);
               return field.getInt(packet);
            }
         }
      } catch (Exception var6) {
      }

      return -1;
   }

   private class CustomFakePlayer extends OtherClientPlayerEntity {
      private int combatCooldown = 0;

      public CustomFakePlayer(PlayerEntity player, String name, float health, boolean copyInv) {
         super(AdvancedFakePlayer.this.mc.world, new GameProfile(UUID.randomUUID(), name));
         this.copyPositionAndRotation(player);
         this.setBodyYaw(player.bodyYaw);
         this.setHeadYaw(player.headYaw);
         this.setHealth(health);
         if (copyInv) {
            this.getInventory().clone(player.getInventory());
         }
      }

      public void tickCombat() {
         if (this.combatCooldown > 0) {
            this.combatCooldown--;
         }

         if (this.hurtTime > 0) {
            this.hurtTime--;
         }
      }

      public void applyDamage(float damage) {
         if (this.combatCooldown <= 0) {
            float oldHealth = this.getHealth();
            float newHealth = oldHealth - damage;
            if ((Boolean)AdvancedFakePlayer.this.showDamage.get()) {
               AdvancedFakePlayer.this.info(String.format("DummyTake Damage: %.1f (Remaining: %.1f)", damage, Math.max(0.0F, newHealth)), new Object[0]);
            }

            this.combatCooldown = (Integer)AdvancedFakePlayer.this.invulnerableTicks.get();
            this.hurtTime = 10;
            this.maxHurtTime = 10;
            this.animateDamage(0.0F);
            if (newHealth <= 0.0F) {
               if ((Boolean)AdvancedFakePlayer.this.autoTotem.get()) {
                  this.popTotem();
               } else {
                  this.die();
               }
            } else {
               this.setHealth(newHealth);
            }
         }
      }

      private void popTotem() {
         this.setHealth(1.0F);
         this.setAbsorptionAmount(4.0F);
         this.clearStatusEffects();
         this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
         this.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
         AdvancedFakePlayer.this.mc
            .world
            .playSound(
               AdvancedFakePlayer.this.mc.player,
               this.getX(),
               this.getY(),
               this.getZ(),
               SoundEvents.ITEM_TOTEM_USE,
               SoundCategory.PLAYERS,
               1.0F,
               1.0F
            );
         this.handleStatus((byte)35);
         this.combatCooldown = (Integer)AdvancedFakePlayer.this.invulnerableTicks.get();
         this.hurtTime = 10;
         if ((Boolean)AdvancedFakePlayer.this.showDamage.get()) {
            AdvancedFakePlayer.this.info(Text.of("§6DummyTriggernotdieImage!"));
         }
      }

      private void die() {
         this.setHealth(0.0F);
         this.setRemoved(RemovalReason.KILLED);
         this.discard();
         AdvancedFakePlayer.this.fakePlayers.remove(this);
         AdvancedFakePlayer.this.mc
            .world
            .playSound(
               AdvancedFakePlayer.this.mc.player,
               this.getX(),
               this.getY(),
               this.getZ(),
               SoundEvents.ENTITY_PLAYER_DEATH,
               SoundCategory.PLAYERS,
               1.0F,
               1.0F
            );
         AdvancedFakePlayer.this.info("DummyAlreadyDeath.", new Object[0]);
      }
   }
}
