package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;

public class BPHackChams extends Module {
   public static BPHackChams INSTANCE;
   private final SettingGroup sgCrystal = this.settings.createGroup("Crystal");
   public final Setting<Boolean> crystalEnabled = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("crystal-enabled")).description("Enable custom end crystal rendering.")).defaultValue(true)).build()
      );
   public final Setting<Boolean> custom = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("custom")).description("Use custom colored overlay on crystal model.")).defaultValue(false))
               .visible(() -> (Boolean)this.crystalEnabled.get()))
            .build()
      );
   public final Setting<SettingColor> crystalColor = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("crystal-color"))
                  .description("Color tint for the crystal."))
               .defaultValue(new SettingColor(55, 135, 255, 255))
               .visible(() -> (Boolean)this.crystalEnabled.get() && (Boolean)this.custom.get()))
            .build()
      );
   public final Setting<Boolean> depth = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("depth")).description("Enable depth test for custom crystal.")).defaultValue(false))
               .visible(() -> (Boolean)this.crystalEnabled.get() && (Boolean)this.custom.get()))
            .build()
      );
   public final Setting<Boolean> chamsTexture = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("chams-texture")).description("Use texture on crystal model parts.")).defaultValue(true))
               .visible(() -> (Boolean)this.crystalEnabled.get() && (Boolean)this.custom.get()))
            .build()
      );
   public final Setting<Boolean> glint = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("glint")).description("Enable glint effect on crystal.")).defaultValue(true))
               .visible(this.crystalEnabled::get))
            .build()
      );
   public final Setting<Boolean> textureEnabled = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("texture")).description("Enable crystal texture.")).defaultValue(true))
               .visible(this.crystalEnabled::get))
            .build()
      );
   public final Setting<Boolean> spinSync = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("spin-sync")).description("Sync spin with module age.")).defaultValue(false))
               .visible(this.crystalEnabled::get))
            .build()
      );
   public final Setting<Double> scale = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("scale"))
                  .description("Crystal scale multiplier (relative to default 2x)."))
               .defaultValue(1.0)
               .min(0.0)
               .max(3.0)
               .visible(this.crystalEnabled::get))
            .build()
      );
   public final Setting<Double> spinSpeed = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("spin-speed"))
                  .description("Crystal spin speed."))
               .defaultValue(1.0)
               .min(0.0)
               .max(10.0)
               .visible(this.crystalEnabled::get))
            .build()
      );
   public final Setting<Double> bounceHeight = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("bounce-height"))
                  .description("Crystal bounce height."))
               .defaultValue(1.0)
               .min(0.0)
               .max(3.0)
               .visible(this.crystalEnabled::get))
            .build()
      );
   public final Setting<Double> bounceSpeed = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("bounce-speed"))
                  .description("Crystal bounce speed."))
               .defaultValue(1.0)
               .min(0.0)
               .max(3.0)
               .visible(this.crystalEnabled::get))
            .build()
      );
   public final Setting<Double> yOffset = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("y-offset"))
                  .description("Crystal Y offset."))
               .defaultValue(0.0)
               .min(-1.0)
               .max(1.0)
               .visible(this.crystalEnabled::get))
            .build()
      );
   private final SettingGroup sgThroughWall = this.settings.createGroup("ThroughWall");
   public final Setting<Boolean> throughWall = this.sgThroughWall
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("through-wall")).description("Show entities through walls (wireframe overlay).")).defaultValue(false))
            .build()
      );
   private final Setting<Boolean> twCrystals = this.sgThroughWall
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("crystals")).description("Show end crystals through walls.")).defaultValue(true))
               .visible(() -> (Boolean)this.throughWall.get()))
            .build()
      );
   private final Setting<Boolean> twPlayers = this.sgThroughWall
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("players")).description("Show players through walls.")).defaultValue(true))
               .visible(() -> (Boolean)this.throughWall.get()))
            .build()
      );
   private final Setting<Boolean> twMobs = this.sgThroughWall
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("mobs")).description("Show mobs through walls.")).defaultValue(true))
               .visible(() -> (Boolean)this.throughWall.get()))
            .build()
      );
   private final Setting<Boolean> twAnimals = this.sgThroughWall
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("animals")).description("Show animals through walls.")).defaultValue(true))
               .visible(() -> (Boolean)this.throughWall.get()))
            .build()
      );
   private final Setting<Boolean> twVillagers = this.sgThroughWall
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("villagers")).description("Show villagers through walls.")).defaultValue(true))
               .visible(() -> (Boolean)this.throughWall.get()))
            .build()
      );
   private final Setting<Boolean> twSlimes = this.sgThroughWall
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("slimes")).description("Show slimes through walls.")).defaultValue(true))
               .visible(() -> (Boolean)this.throughWall.get()))
            .build()
      );
   private final SettingGroup sgHand = this.settings.createGroup("Hand");
   public final Setting<Boolean> handEnabled = this.sgHand
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("hand-enabled")).description("Tint the player's hand and held items.")).defaultValue(false)).build()
      );
   public final Setting<Boolean> handTexture = this.sgHand
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("hand-texture")).description("Show hand texture.")).defaultValue(true))
               .visible(this.handEnabled::get))
            .build()
      );
   public final Setting<SettingColor> handColor = this.sgHand
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("hand-color"))
                  .description("Color for hand tint."))
               .defaultValue(new SettingColor(55, 135, 255, 150))
               .visible(this.handEnabled::get))
            .build()
      );
   public int age;

   public BPHackChams() {
      super(AddonTemplate.CATEGORY, "BPHackChams", "LiquidBounce-style chams and custom end crystal rendering.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.age = 0;
   }

   @EventHandler
   private void onTick(Post event) {
      this.age++;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.throughWall.get() && this.mc.world != null) {
         for (Entity entity : this.mc.world.getEntities()) {
            if (this.shouldRenderThroughWall(entity) && entity != this.mc.player) {
               WireframeEntityRenderer.render(event, entity, 1.0, new SettingColor(55, 135, 255, 30), new SettingColor(55, 135, 255, 127), ShapeMode.Both);
            }
         }
      }
   }

   public boolean customCrystal() {
      return this.isActive() && (Boolean)this.crystalEnabled.get();
   }

   private boolean shouldRenderThroughWall(Entity entity) {
      if (!this.isActive() || !(Boolean)this.throughWall.get()) {
         return false;
      } else if (entity instanceof EndCrystalEntity) {
         return (Boolean)this.twCrystals.get();
      } else if (entity instanceof SlimeEntity) {
         return (Boolean)this.twSlimes.get();
      } else if (entity instanceof PlayerEntity) {
         return (Boolean)this.twPlayers.get();
      } else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
         return (Boolean)this.twVillagers.get();
      } else if (entity instanceof AnimalEntity) {
         return (Boolean)this.twAnimals.get();
      } else {
         return entity instanceof MobEntity ? (Boolean)this.twMobs.get() : false;
      }
   }
}
