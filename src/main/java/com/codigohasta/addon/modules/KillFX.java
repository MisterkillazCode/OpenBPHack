package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class KillFX extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgLightning = this.settings.createGroup("Setting");
   private final SettingGroup sgParticles = this.settings.createGroup("ParticleEffect");
   private final SettingGroup sgSound = this.settings.createGroup("Sound");
   private final SettingGroup sgExtra = this.settings.createGroup("outsideView");
   private final Setting<Boolean> onlyTargeted = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("LimitAttack Target")).description("OnlyhaveyouAttackpast'sMobDeathTimethenTriggerEffect."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> targetTimeout = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("MemoryTime"))
                  .description("Attack Target'sNumber."))
               .defaultValue(3.5)
               .min(0.5)
               .sliderMax(10.0)
               .visible(this.onlyTargeted::get))
            .build()
      );
   private final Setting<Boolean> useLightning = this.sgLightning
      .add(((Builder)((Builder)((Builder)new Builder().name("Enable")).description("StrikekillTime.")).defaultValue(true)).build());
   private final Setting<Integer> lightningAmount = this.sgLightning
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Amount"))
                     .description("Timedown'sAmount."))
                  .defaultValue(1))
               .min(1)
               .sliderMax(10)
               .visible(this.useLightning::get))
            .build()
      );
   private final Setting<Boolean> useParticles = this.sgParticles.add(((Builder)((Builder)new Builder().name("EnableParticle")).defaultValue(true)).build());
   private final Setting<KillFX.ParticleCategory> particleCategory = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("ParticleCategory"))
                  .defaultValue(KillFX.ParticleCategory.Magic))
               .visible(this.useParticles::get))
            .build()
      );
   private final Setting<KillFX.CombatParticle> pCombat = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Particle"))
                  .defaultValue(KillFX.CombatParticle.Crit))
               .visible(() -> (Boolean)this.useParticles.get() && this.particleCategory.get() == KillFX.ParticleCategory.Combat))
            .build()
      );
   private final Setting<KillFX.MagicParticle> pMagic = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("MethodParticle"))
                  .defaultValue(KillFX.MagicParticle.EndRod))
               .visible(() -> (Boolean)this.useParticles.get() && this.particleCategory.get() == KillFX.ParticleCategory.Magic))
            .build()
      );
   private final Setting<KillFX.FireParticle> pFire = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Particle"))
                  .defaultValue(KillFX.FireParticle.Flame))
               .visible(() -> (Boolean)this.useParticles.get() && this.particleCategory.get() == KillFX.ParticleCategory.Fire))
            .build()
      );
   private final Setting<KillFX.NatureParticle> pNature = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("SelfParticle"))
                  .defaultValue(KillFX.NatureParticle.Heart))
               .visible(() -> (Boolean)this.useParticles.get() && this.particleCategory.get() == KillFX.ParticleCategory.Nature))
            .build()
      );
   private final Setting<KillFX.UpdateParticle> pUpdate = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("1.21Particle"))
                  .defaultValue(KillFX.UpdateParticle.Ominous))
               .visible(() -> (Boolean)this.useParticles.get() && this.particleCategory.get() == KillFX.ParticleCategory.Update121))
            .build()
      );
   private final Setting<KillFX.MiscParticle> pMisc = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("OtherParticle"))
                  .defaultValue(KillFX.MiscParticle.SculkSoul))
               .visible(() -> (Boolean)this.useParticles.get() && this.particleCategory.get() == KillFX.ParticleCategory.Misc))
            .build()
      );
   private final Setting<KillFX.ParticleShape> particleShape = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Particle"))
                  .defaultValue(KillFX.ParticleShape.Burst))
               .visible(this.useParticles::get))
            .build()
      );
   private final Setting<Integer> particleCount = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ParticleAmount"))
                  .defaultValue(40))
               .min(5)
               .sliderMax(200)
               .visible(this.useParticles::get))
            .build()
      );
   private final Setting<Double> particleSpeed = this.sgParticles
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("ParticleSpeed"))
               .defaultValue(0.2)
               .min(0.0)
               .max(2.0)
               .visible(this.useParticles::get))
            .build()
      );
   private final Setting<Boolean> useSound = this.sgSound.add(((Builder)((Builder)new Builder().name("EnableSound")).defaultValue(true)).build());
   private final Setting<KillFX.SoundGroup> soundGroup = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("SoundCategory"))
                  .defaultValue(KillFX.SoundGroup.Combat))
               .visible(this.useSound::get))
            .build()
      );
   private final Setting<KillFX.CombatSound> sCombat = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Combat Sound"))
                  .defaultValue(KillFX.CombatSound.Thunder))
               .visible(() -> (Boolean)this.useSound.get() && this.soundGroup.get() == KillFX.SoundGroup.Combat))
            .build()
      );
   private final Setting<KillFX.MagicSound> sMagic = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("MethodSound"))
                  .defaultValue(KillFX.MagicSound.AnchorCharge))
               .visible(() -> (Boolean)this.useSound.get() && this.soundGroup.get() == KillFX.SoundGroup.Magic))
            .build()
      );
   private final Setting<KillFX.CreatureSound> sCreature = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("MobSound"))
                  .defaultValue(KillFX.CreatureSound.Warden))
               .visible(() -> (Boolean)this.useSound.get() && this.soundGroup.get() == KillFX.SoundGroup.Creature))
            .build()
      );
   private final Setting<KillFX.FunSound> sFun = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Fun Sound"))
                  .defaultValue(KillFX.FunSound.Pling))
               .visible(() -> (Boolean)this.useSound.get() && this.soundGroup.get() == KillFX.SoundGroup.Fun))
            .build()
      );
   private final Setting<Double> volume = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("SoundAmount"))
                  .description("SoundAmountSize, NumberhighSoundDistance."))
               .defaultValue(1.0)
               .min(0.0)
               .max(10.0)
               .sliderMax(5.0)
               .visible(this.useSound::get))
            .build()
      );
   private final Setting<Double> pitch = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Pitch"))
                  .description("low'sPitchStartcomemore."))
               .defaultValue(1.0)
               .min(0.5)
               .max(2.0)
               .visible(this.useSound::get))
            .build()
      );
   private final Setting<Boolean> useFirework = this.sgExtra.add(((Builder)((Builder)new Builder().name("SpawnComplete")).defaultValue(false)).build());
   private final Setting<Boolean> useExplosion = this.sgExtra
      .add(((Builder)((Builder)new Builder().name("SpawnCompleteExplosion")).defaultValue(false)).build());
   private final Set<Integer> processedEntities = new HashSet<>();
   private final Map<Integer, Long> attackedTargets = new HashMap<>();

   public KillFX() {
      super(AddonTemplate.CATEGORY, "KillFX", "Plays custom effects (lightning, particles, sounds) when a player dies.");
   }

   public void onActivate() {
      this.processedEntities.clear();
      this.attackedTargets.clear();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.processedEntities.clear();
      this.attackedTargets.clear();
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (event.entity instanceof LivingEntity) {
         this.attackedTargets.put(event.entity.getId(), System.currentTimeMillis());
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.world != null && this.mc.player != null) {
         if ((Boolean)this.onlyTargeted.get()) {
            long threshold = (long)((Double)this.targetTimeout.get() * 1000.0);
            long now = System.currentTimeMillis();
            this.attackedTargets.entrySet().removeIf(entry -> now - entry.getValue() > threshold);
         }

         List<Entity> entities = new ArrayList<>();
         this.mc.world.getEntities().forEach(entities::add);

         for (Entity entity : entities) {
            if (entity instanceof LivingEntity living
               && entity != this.mc.player
               && (living.getHealth() <= 0.0F || living.isDead())
               && !this.processedEntities.contains(entity.getId())) {
               if ((Boolean)this.onlyTargeted.get() && !this.attackedTargets.containsKey(entity.getId())) {
                  this.processedEntities.add(entity.getId());
               } else {
                  this.renderEffects(living);
                  this.processedEntities.add(entity.getId());
               }
            }
         }
      }
   }

   private void renderEffects(LivingEntity entity) {
      Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
      if ((Boolean)this.useLightning.get()) {
         int amount = (Integer)this.lightningAmount.get();

         for (int i = 0; i < amount; i++) {
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, this.mc.world);
            double offsetX = i == 0 ? 0.0 : (Math.random() - 0.5) * 0.5;
            double offsetZ = i == 0 ? 0.0 : (Math.random() - 0.5) * 0.5;
            lightning.setPosition(pos.x + offsetX, pos.y, pos.z + offsetZ);
            this.mc.world.addEntity(lightning);
         }
      }

      if ((Boolean)this.useParticles.get()) {
         ParticleEffect effect = null;
         switch ((KillFX.ParticleCategory)this.particleCategory.get()) {
            case Combat:
               effect = ((KillFX.CombatParticle)this.pCombat.get()).p;
               break;
            case Magic:
               effect = ((KillFX.MagicParticle)this.pMagic.get()).p;
               break;
            case Fire:
               effect = ((KillFX.FireParticle)this.pFire.get()).p;
               break;
            case Nature:
               effect = ((KillFX.NatureParticle)this.pNature.get()).p;
               break;
            case Update121:
               effect = ((KillFX.UpdateParticle)this.pUpdate.get()).p;
               break;
            case Misc:
               effect = ((KillFX.MiscParticle)this.pMisc.get()).p;
         }

         if (effect != null) {
            this.spawnParticles(entity, effect);
         }
      }

      if ((Boolean)this.useSound.get()) {
         String soundId = "entity.lightning_bolt.thunder";
         switch ((KillFX.SoundGroup)this.soundGroup.get()) {
            case Combat:
               soundId = ((KillFX.CombatSound)this.sCombat.get()).id;
               break;
            case Magic:
               soundId = ((KillFX.MagicSound)this.sMagic.get()).id;
               break;
            case Creature:
               soundId = ((KillFX.CreatureSound)this.sCreature.get()).id;
               break;
            case Fun:
               soundId = ((KillFX.FunSound)this.sFun.get()).id;
         }

         this.mc
            .world
            .playSound(
               this.mc.player,
               BlockPos.ofFloored(pos),
               SoundEvent.of(Identifier.of("minecraft", soundId)),
               SoundCategory.PLAYERS,
               ((Double)this.volume.get()).floatValue(),
               ((Double)this.pitch.get()).floatValue()
            );
      }

      if ((Boolean)this.useFirework.get()) {
         ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET);
         FireworkRocketEntity rocket = new FireworkRocketEntity(this.mc.world, itemStack, entity);
         rocket.setPosition(pos.x, pos.y + 0.5, pos.z);
         this.mc.world.addEntity(rocket);
         this.mc.particleManager.addParticle(ParticleTypes.EXPLOSION, pos.x, pos.y + 1.0, pos.z, 0.0, 0.0, 0.0);
      }

      if ((Boolean)this.useExplosion.get()) {
         this.mc.particleManager.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1.0, pos.z, 0.0, 0.0, 0.0);
      }
   }

   private void spawnParticles(LivingEntity entity, ParticleEffect effect) {
      Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
      int count = (Integer)this.particleCount.get();
      double speed = (Double)this.particleSpeed.get();
      double height = entity.getHeight();
      double width = entity.getWidth();
      switch ((KillFX.ParticleShape)this.particleShape.get()) {
         case Burst:
            for (int i = 0; i < count; i++) {
               this.mc
                  .particleManager
                  .addParticle(
                     effect,
                     pos.x + (Math.random() - 0.5) * width,
                     pos.y + Math.random() * height,
                     pos.z + (Math.random() - 0.5) * width,
                     (Math.random() - 0.5) * speed,
                     (Math.random() - 0.5) * speed,
                     (Math.random() - 0.5) * speed
                  );
            }
            break;
         case Sphere:
            for (int i = 0; i < count; i++) {
               double u = Math.random();
               double v = Math.random();
               double theta = (Math.PI * 2) * u;
               double phi = Math.acos(2.0 * v - 1.0);
               double r = 1.0;
               double dx = r * Math.sin(phi) * Math.cos(theta);
               double dy = r * Math.sin(phi) * Math.sin(theta);
               double dz = r * Math.cos(phi);
               this.mc.particleManager.addParticle(effect, pos.x + dx, pos.y + dy + height / 2.0, pos.z + dz, 0.0, 0.0, 0.0);
            }
            break;
         case Spiral:
            for (int i = 0; i < count; i++) {
               double yOffset = (double)i / count * 2.5;
               double angle = yOffset * 5.0;
               double radius = 0.8;
               double dx = Math.cos(angle) * radius;
               double dz = Math.sin(angle) * radius;
               this.mc.particleManager.addParticle(effect, pos.x + dx, pos.y + yOffset, pos.z + dz, 0.0, 0.05, 0.0);
            }
            break;
         case Column:
            for (int i = 0; i < count; i++) {
               this.mc
                  .particleManager
                  .addParticle(
                     effect,
                     pos.x + (Math.random() - 0.5) * width,
                     pos.y + 0.1,
                     pos.z + (Math.random() - 0.5) * width,
                     0.0,
                     speed,
                     0.0
                  );
            }
            break;
         case Halo:
            for (int i = 0; i < count; i++) {
               double angle = (double)i / count * Math.PI * 2.0;
               double radius = 0.7;
               double dx = Math.cos(angle) * radius;
               double dz = Math.sin(angle) * radius;
               this.mc.particleManager.addParticle(effect, pos.x + dx, pos.y + height + 0.5, pos.z + dz, 0.0, 0.0, 0.0);
            }
            break;
         case Heart:
            for (int i = 0; i < count; i++) {
               double t = (double)i / count * Math.PI * 2.0;
               double hx = 16.0 * Math.pow(Math.sin(t), 3.0);
               double hz = 13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t);
               double scale = 0.06;
               this.mc
                  .particleManager
                  .addParticle(effect, pos.x + hx * scale, pos.y + 1.2 + hz * scale * 0.5, pos.z + hz * scale, 0.0, 0.0, 0.0);
            }
            break;
         case Helix:
            for (int i = 0; i < count; i++) {
               double h = (double)i / count * 3.0;
               double angle = h * 4.0;
               double radius = 0.6;
               this.mc
                  .particleManager
                  .addParticle(effect, pos.x + Math.cos(angle) * radius, pos.y + h, pos.z + Math.sin(angle) * radius, 0.0, 0.0, 0.0);
               this.mc
                  .particleManager
                  .addParticle(
                     effect,
                     pos.x + Math.cos(angle + Math.PI) * radius,
                     pos.y + h,
                     pos.z + Math.sin(angle + Math.PI) * radius,
                     0.0,
                     0.0,
                     0.0
                  );
            }
            break;
         case Star:
            for (int i = 0; i < count; i++) {
               double angle = (double)i / count * Math.PI * 2.0;
               double dx = Math.cos(angle) * 1.0;
               double dz = Math.sin(angle) * 1.0;
               this.mc.particleManager.addParticle(effect, pos.x, pos.y + 0.2, pos.z, dx * speed, 0.0, dz * speed);
            }
            break;
         case Ring:
            for (int i = 0; i < count; i++) {
               double angle = (double)i / count * Math.PI * 2.0;
               double dx = Math.cos(angle);
               double dz = Math.sin(angle);
               this.mc
                  .particleManager
                  .addParticle(effect, pos.x + dx * 0.2, pos.y + 0.1, pos.z + dz * 0.2, dx * speed * 2.0, 0.0, dz * speed * 2.0);
            }
      }
   }

   public static enum CombatParticle {
      Damage(ParticleTypes.DAMAGE_INDICATOR, "DamageCenter"),
      Crit(ParticleTypes.CRIT, "Strike"),
      EnchantedHit(ParticleTypes.ENCHANTED_HIT, "Strike"),
      Sweep(ParticleTypes.SWEEP_ATTACK, "Sweeping Edge"),
      Explosion(ParticleTypes.EXPLOSION, "Explosion Dust"),
      ExplosionHuge(ParticleTypes.EXPLOSION_EMITTER, "bigExplosion"),
      Sonic(ParticleTypes.SONIC_BOOM, "Warden Sonic"),
      Totem(ParticleTypes.TOTEM_OF_UNDYING, "notdieImage"),
      Firework(ParticleTypes.FIREWORK, "Firework Rocket"),
      EggCrack(ParticleTypes.EGG_CRACK, "Egg Crack");

      final ParticleEffect p;
      final String n;

      private CombatParticle(ParticleEffect p, String n) {
         this.p = p;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum CombatSound {
      Thunder("entity.lightning_bolt.thunder", "Lightning"),
      Explode("entity.generic.explode", "Explosion"),
      Anvil("block.anvil.land", "Anvil Squish"),
      Trident("item.trident.thunder", "Trident Thunder"),
      WitherSpawn("entity.wither.spawn", "SpawnComplete"),
      WitherShoot("entity.wither.shoot", "ShootStrike"),
      Anchor("block.respawn_anchor.deplete", "HeavySpawnExplosion"),
      Crystal("entity.end_crystal.explode", "Crystal Explosion"),
      Break("item.shield.break", "Shield Break"),
      Crit("entity.player.attack.crit", "StrikeSound"),
      Smash("item.mace.smash_ground", "HeavyGround(1.21)"),
      MaceSmashHeavy("item.mace.smash_ground_heavy", "HeavyHeavyStrike(1.21)"),
      WindCharge("entity.wind_charge.wind_burst", "Wind Burst"),
      CrossbowHit("item.crossbow.hit", "ArrowHit"),
      TridentHit("item.trident.hit", "TridentHit"),
      FireworkBlast("entity.firework_rocket.blast", "Firework Explosion"),
      AtkStrong("entity.player.attack.strong", "HeavyStrikeHit"),
      AtkSweep("entity.player.attack.sweep", "SweepHit");

      final String id;
      final String n;

      private CombatSound(String id, String n) {
         this.id = id;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum CreatureSound {
      Warden("entity.warden.sonic_boom", "Warden Sonic"),
      WardenHeart("entity.warden.heartbeat", "Center"),
      Dragon("entity.ender_dragon.death", "Ender DragonDeath"),
      DragonGrowl("entity.ender_dragon.growl", "Ender Dragon Roar"),
      Blaze("entity.blaze.death", "Player"),
      Ghast("entity.ghast.scream", "Ghast Scream"),
      Enderman("entity.enderman.stare", "littleBlackView"),
      Phantom("entity.phantom.bite", "Phantom Bite"),
      Wolf("entity.wolf.howl", "Wolf Howl"),
      Cat("entity.cat.hiss", "haAir"),
      AllayItem("entity.allay.item_given", "give"),
      BreezeShoot("entity.breeze.shoot", "PlayerShootStrike"),
      BreezeIdle("entity.breeze.idle_air", "PlayerFly"),
      CreakingAttack("entity.creaking.attack", "CreakingAttack"),
      CreakingDeath("entity.creaking.death", "CreakingDeath"),
      CreakingSpawn("entity.creaking.spawn", "CreakingSpawnComplete"),
      BeeSting("entity.bee.sting", "Bee Sting"),
      RavagerRoar("entity.ravager.roar", "Ravager Roar");

      final String id;
      final String n;

      private CreatureSound(String id, String n) {
         this.id = id;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum FireParticle {
      Flame(ParticleTypes.FLAME, "Normal Flame"),
      SoulFlame(ParticleTypes.SOUL_FIRE_FLAME, "Soul Flame"),
      SmallFlame(ParticleTypes.SMALL_FLAME, "Candle Flame"),
      CopperFlame(ParticleTypes.COPPER_FIRE_FLAME, "Copper Flame"),
      Lava(ParticleTypes.LAVA, "Lava"),
      LargeSmoke(ParticleTypes.LARGE_SMOKE, "Dense Black Smoke"),
      Smoke(ParticleTypes.SMOKE, "Normal Smoke"),
      WhiteSmoke(ParticleTypes.WHITE_SMOKE, "WhiteColor"),
      Campfire(ParticleTypes.CAMPFIRE_COSY_SMOKE, "Campfire Smoke"),
      CampfireSignal(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, "Campfire Signal Smoke"),
      Glow(ParticleTypes.GLOW, "Glow Dust"),
      Wax(ParticleTypes.WAX_ON, "Wax Copper"),
      WaxOff(ParticleTypes.WAX_OFF, "Unwax Copper"),
      Scrape(ParticleTypes.SCRAPE, "Scrape Copper"),
      Spark(ParticleTypes.ELECTRIC_SPARK, "Spark");

      final ParticleEffect p;
      final String n;

      private FireParticle(ParticleEffect p, String n) {
         this.p = p;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum FunSound {
      Burp("entity.player.burp", "HitHiccup"),
      Pling("block.note_block.pling", "NotePling"),
      Goat("entity.goat.screaming.milk", "Goat Scream"),
      No("entity.villager.no", "Villager:no"),
      Yes("entity.villager.yes", "Villager:good"),
      Eat("entity.generic.eat", "thing"),
      Toast("ui.toast.challenge_complete", "CompleteComplete"),
      Glass("block.glass.break", "Center"),
      VillagerCelebrate("entity.villager.celebrate", "Villager"),
      VillagerTrade("entity.villager.trade", "VillagerExchange"),
      BellResonate("block.bell.resonate", "Bell Resonance"),
      NoteBit("block.note_block.bit", "Electronic"),
      NoteBanjo("block.note_block.banjo", "Banjo");

      final String id;
      final String n;

      private FunSound(String id, String n) {
         this.id = id;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum MagicParticle {
      Witch(ParticleTypes.WITCH, "Method"),
      EndRod(ParticleTypes.END_ROD, "Ground"),
      Portal(ParticleTypes.PORTAL, "Send"),
      Enchant(ParticleTypes.ENCHANT, "Text"),
      Nautilus(ParticleTypes.NAUTILUS, "Center"),
      ElderGuardian(ParticleTypes.ELDER_GUARDIAN, "Ancient Guardian"),
      SculkCharge(ParticleTypes.SCULK_CHARGE_POP, "SculkCharge"),
      Soul(ParticleTypes.SOUL, "SoulOut"),
      GlowSquidInk(ParticleTypes.GLOW_SQUID_INK, "Glow Squid Ink");

      final ParticleEffect p;
      final String n;

      private MagicParticle(ParticleEffect p, String n) {
         this.p = p;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum MagicSound {
      AnchorCharge("block.respawn_anchor.charge", "HeavySpawnCharge"),
      AnchorSet("block.respawn_anchor.set_spawn", "HeavySpawnSetting"),
      Totem("item.totem.use", "notdieImage"),
      Beacon("block.beacon.activate", "MarkMove"),
      Conduit("block.conduit.activate", "Center"),
      Portal("block.portal.trigger", "SendSound"),
      LevelUp("entity.player.levelup", "Level Up Ding"),
      Enchant("block.enchantment_table.use", "Enchant Table"),
      Teleport("entity.enderman.teleport", "Move"),
      Bell("block.bell.use", "Bell"),
      Chime("block.amethyst_block.chime", "Amethyst Chime"),
      Resonate("block.amethyst_block.resonate", "Amethyst Resonance"),
      EnderEye("entity.ender_eye.death", "Eye of Ender Break"),
      ExpOrb("entity.experience_orb.pickup", "XP Pickup"),
      EvokerCast("entity.evoker.cast_spell", "Method"),
      ConduitAtk("block.conduit.attack_target", "CenterAttack"),
      DragonFireball("entity.dragon_fireball.explode", "Dragon Ball Explosion");

      final String id;
      final String n;

      private MagicSound(String id, String n) {
         this.id = id;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum MiscParticle {
      Ash(ParticleTypes.ASH, "Volcanic Ash"),
      Mycelium(ParticleTypes.MYCELIUM, "Mycelium"),
      SculkSoul(ParticleTypes.SCULK_SOUL, "Sculk Soul"),
      Happy(ParticleTypes.HAPPY_VILLAGER, "Villagerhigh"),
      Angry(ParticleTypes.ANGRY_VILLAGER, "VillagerSpawnAir"),
      Sneeze(ParticleTypes.SNEEZE, "HitSneeze"),
      Ink(ParticleTypes.SQUID_INK, "Ink Sac");

      final ParticleEffect p;
      final String n;

      private MiscParticle(ParticleEffect p, String n) {
         this.p = p;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum NatureParticle {
      Heart(ParticleTypes.HEART, "RedColorCenter"),
      Cloud(ParticleTypes.CLOUD, "Cloud"),
      Rain(ParticleTypes.RAIN, "Rain"),
      Snow(ParticleTypes.SNOWFLAKE, "Snowflake"),
      Slime(ParticleTypes.ITEM_SLIME, "Slime"),
      Bubble(ParticleTypes.BUBBLE, "Air"),
      BubbleColumnUp(ParticleTypes.BUBBLE_COLUMN_UP, "AirRise"),
      CurrentDown(ParticleTypes.CURRENT_DOWN, "Airdown"),
      BubblePop(ParticleTypes.BUBBLE_POP, "Air"),
      Splash(ParticleTypes.SPLASH, "Splash"),
      Fishing(ParticleTypes.FISHING, "Fishing Ripple"),
      Dolphin(ParticleTypes.DOLPHIN, "Move"),
      Underwater(ParticleTypes.UNDERWATER, "WaterdownAir"),
      Note(ParticleTypes.NOTE, "Note"),
      Cherry(ParticleTypes.CHERRY_LEAVES, "Cherry Petal"),
      PaleOakLeaves(ParticleTypes.PALE_OAK_LEAVES, "Pale Oak Leaf"),
      Firefly(ParticleTypes.FIREFLY, "Firefly"),
      Spore(ParticleTypes.SPORE_BLOSSOM_AIR, "Spore Flower"),
      WhiteAsh(ParticleTypes.WHITE_ASH, "WhiteColorGray"),
      WarpedSpore(ParticleTypes.WARPED_SPORE, "Crimson Spore"),
      CrimsonSpore(ParticleTypes.CRIMSON_SPORE, "Crimson Spore");

      final ParticleEffect p;
      final String n;

      private NatureParticle(ParticleEffect p, String n) {
         this.p = p;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }

   public static enum ParticleCategory {
      Combat("/Strike"),
      Magic("Method/Effect"),
      Fire("/"),
      Nature("Self/Mob"),
      Update121("1.21NewParticle"),
      Misc("Other");

      final String name;

      private ParticleCategory(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   public static enum ParticleShape {
      Burst("Explosion"),
      Sphere("Pack"),
      Spiral("SpiralRise"),
      Column("Light BeamAscend"),
      Halo("Head"),
      Heart("Center"),
      Helix("SpiralDNA"),
      Star("Pentagram"),
      Ring("Impact");

      final String name;

      private ParticleShape(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   public static enum SoundGroup {
      Combat("hard"),
      Magic("Method"),
      Creature("Mob"),
      Fun("Funny Prank");

      final String name;

      private SoundGroup(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   public static enum UpdateParticle {
      Gust(ParticleTypes.GUST, "()"),
      GustSmall(ParticleTypes.SMALL_GUST, "little"),
      GustEmitterLarge(ParticleTypes.GUST_EMITTER_LARGE, "bigSendShoot"),
      GustEmitterSmall(ParticleTypes.GUST_EMITTER_SMALL, "littleSendShoot"),
      Trial(ParticleTypes.TRIAL_SPAWNER_DETECTION, "Trial Spawner"),
      TrialOminous(ParticleTypes.TRIAL_SPAWNER_DETECTION_OMINOUS, "notAuspiciousTrialCheckTest"),
      Ominous(ParticleTypes.OMINOUS_SPAWNING, "Ominous"),
      Vault(ParticleTypes.VAULT_CONNECTION, "Connect"),
      Raid(ParticleTypes.RAID_OMEN, "StrikeOmen"),
      TrialOmen(ParticleTypes.TRIAL_OMEN, "TrialOmen"),
      Infested(ParticleTypes.INFESTED, "Spawn"),
      Cobweb(ParticleTypes.ITEM_COBWEB, "Cobweb"),
      DustPlume(ParticleTypes.DUST_PLUME, "Pot Dust");

      final ParticleEffect p;
      final String n;

      private UpdateParticle(ParticleEffect p, String n) {
         this.p = p;
         this.n = n;
      }

      @Override
      public String toString() {
         return this.n;
      }
   }
}
