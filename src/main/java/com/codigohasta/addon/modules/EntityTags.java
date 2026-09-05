package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public class EntityTags extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final SettingGroup sgCluster = this.settings.createGroup("Aggregate");
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("Target Entity")).description("SelectwantDisplayMark'sEntityType."))
            .defaultValue(new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER})
            .build()
      );
   private final Setting<Boolean> displayHealth = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Show Health"))
                  .description("DisplayEntity'sHealth."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> displayDistance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Show Distance"))
                  .description("DisplaytoEntity'sDistance."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> displayItems = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayEquipment"))
                  .description("atMarkupDirectionDisplayEquipment."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> scale = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("MarkScale"))
               .description("Mark'sSizeScale."))
            .defaultValue(1.0)
            .min(0.1)
            .build()
      );
   private final Setting<Double> yOffset = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("YMove"))
               .description("MarkatEntityupDirection'sMoveAmount."))
            .defaultValue(0.5)
            .sliderRange(0.0, 2.0)
            .build()
      );
   private final Setting<SettingColor> bgColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("BackgroundColor"))
               .description("Mark'sBackgroundColor."))
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build()
      );
   private final Setting<Boolean> enableClustering = this.sgCluster
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Enable"))
                  .description("DistanceTimeEntity'sMark."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> clusterPlayerDistance = this.sgCluster
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("TriggerAggregateDistance"))
                  .description("DistancePlayermanyTimestartMark."))
               .defaultValue(15.0)
               .min(0.0)
               .sliderMax(50.0)
               .visible(this.enableClustering::get))
            .build()
      );
   private final Setting<Double> clusterEntityDistance = this.sgCluster
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("EntityBetweenThreshold"))
                  .description("EntityBetweenDistancemanythencan."))
               .defaultValue(3.0)
               .min(0.1)
               .sliderMax(10.0)
               .visible(this.enableClustering::get))
            .build()
      );
   private final Setting<Integer> clusterMinCount = this.sgCluster
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("mostlittleAggregateAmount"))
                     .description("tomanyfewAmountthenProceed."))
                  .defaultValue(2))
               .min(2)
               .sliderMax(10)
               .visible(this.enableClustering::get))
            .build()
      );
   private final Vector3d pos = new Vector3d();
   private final Color WHITE = new Color(255, 255, 255);
   private final Color GREEN = new Color(25, 252, 25);
   private final Color AMBER = new Color(255, 105, 25);
   private final Color RED = new Color(255, 25, 25);

   public EntityTags() {
      super(AddonTemplate.CATEGORY, "EntityTags", "Shows tags above entities: health, distance and entity name.");
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         List<EntityTags.EntityCluster> clusters = new ArrayList<>();

         for (Entity entity : this.mc.world.getEntities()) {
            if (entity.getType() != EntityType.PLAYER && ((Set)this.entities.get()).contains(entity.getType())) {
               double distToPlayer = PlayerUtils.distanceToCamera(entity);
               boolean shouldCluster = (Boolean)this.enableClustering.get() && distToPlayer > (Double)this.clusterPlayerDistance.get();
               boolean added = false;
               if (shouldCluster) {
                  for (EntityTags.EntityCluster cluster : clusters) {
                     Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                     if (cluster.type == entity.getType() && cluster.center.distanceTo(entityPos) <= (Double)this.clusterEntityDistance.get()) {
                        cluster.add(entity);
                        added = true;
                        break;
                     }
                  }
               }

               if (!added) {
                  clusters.add(new EntityTags.EntityCluster(entity));
               }
            }
         }

         for (EntityTags.EntityCluster clusterx : clusters) {
            if ((Boolean)this.enableClustering.get() && clusterx.entities.size() >= (Integer)this.clusterMinCount.get()) {
               this.renderMergedTag(event, clusterx);
            } else {
               for (Entity entityx : clusterx.entities) {
                  this.renderIndividualTag(event, entityx);
               }
            }
         }
      }
   }

   private void renderMergedTag(Render2DEvent event, EntityTags.EntityCluster cluster) {
      Entity first = cluster.entities.get(0);
      this.pos
         .set(
            cluster.center.getX(),
            cluster.center.getY() + first.getEyeHeight(first.getPose()) + (Double)this.yOffset.get(),
            cluster.center.getZ()
         );
      if (NametagUtils.to2D(this.pos, (Double)this.scale.get())) {
         TextRenderer text = TextRenderer.get();
         NametagUtils.begin(this.pos, event.drawContext);
         String nameText = first.getType().getName().getString() + " x" + cluster.entities.size();
         if ((Boolean)this.displayDistance.get()) {
            double dist = Math.round(PlayerUtils.distanceToCamera(cluster.center.x, cluster.center.y, cluster.center.z) * 10.0)
               / 10.0;
            nameText = nameText + " (" + dist + "m)";
         }

         double width = text.getWidth(nameText, true);
         double height = text.getHeight(true);
         double widthHalf = width / 2.0;
         this.drawBg(-widthHalf, -height, width, height);
         text.beginBig();
         text.render(nameText, -widthHalf, -height, this.WHITE, true);
         text.end();
         NametagUtils.end(event.drawContext);
      }
   }

   private void renderIndividualTag(Render2DEvent event, Entity entity) {
      Utils.set(this.pos, entity, event.tickDelta);
      this.pos.add(0.0, entity.getEyeHeight(entity.getPose()) + (Double)this.yOffset.get(), 0.0);
      if (NametagUtils.to2D(this.pos, (Double)this.scale.get())) {
         TextRenderer text = TextRenderer.get();
         NametagUtils.begin(this.pos, event.drawContext);
         String nameText = entity.getType().getName().getString();
         String healthText = "";
         String distText = "";
         Color healthColor = this.WHITE;
         double totalWidth = text.getWidth(nameText, true);
         if ((Boolean)this.displayHealth.get() && entity instanceof LivingEntity living) {
            float health = living.getHealth() + living.getAbsorptionAmount();
            float maxHealth = living.getMaxHealth() + living.getAbsorptionAmount();
            healthText = " " + Math.round(health);
            double hpPercent = health / maxHealth;
            if (hpPercent <= 0.333) {
               healthColor = this.RED;
            } else if (hpPercent <= 0.666) {
               healthColor = this.AMBER;
            } else {
               healthColor = this.GREEN;
            }

            totalWidth += text.getWidth(healthText, true);
         }

         if ((Boolean)this.displayDistance.get()) {
            double dist = Math.round(PlayerUtils.distanceToCamera(entity) * 10.0) / 10.0;
            distText = " " + dist + "m";
            totalWidth += text.getWidth(distText, true);
         }

         double height = text.getHeight(true);
         double widthHalf = totalWidth / 2.0;
         this.drawBg(-widthHalf, -height, totalWidth, height);
         text.beginBig();
         double hX = -widthHalf;
         hX = text.render(nameText, hX, -height, this.WHITE, true);
         if ((Boolean)this.displayHealth.get() && !healthText.isEmpty()) {
            hX = text.render(healthText, hX, -height, healthColor, true);
         }

         if ((Boolean)this.displayDistance.get() && !distText.isEmpty()) {
            text.render(distText, hX, -height, this.WHITE, true);
         }

         text.end();
         if ((Boolean)this.displayItems.get() && entity instanceof LivingEntity living) {
            this.drawItems(event, living, -height, totalWidth);
         }

         NametagUtils.end(event.drawContext);
      }
   }

   private void drawItems(Render2DEvent event, LivingEntity entity, double currentYOffset, double tagWidth) {
      List<ItemStack> equipment = new ArrayList<>();

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         ItemStack stack = entity.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            equipment.add(stack);
         }
      }

      if (!equipment.isEmpty()) {
         double itemSpacing = 2.0;
         double itemSize = 32.0;
         double totalItemWidth = equipment.size() * itemSize + (equipment.size() - 1) * itemSpacing;
         double startX = -(totalItemWidth / 2.0);
         double startY = currentYOffset - itemSize - 5.0;

         for (int i = 0; i < equipment.size(); i++) {
            ItemStack stack = equipment.get(i);
            double x = startX + i * (itemSize + itemSpacing);
            RenderUtils.drawItem(event.drawContext, stack, (int)x, (int)startY, 2.0F, true, null, false);
         }
      }
   }

   private void drawBg(double x, double y, double width, double height) {
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad(x - 1.0, y - 1.0, width + 2.0, height + 2.0, (Color)this.bgColor.get());
      Renderer2D.COLOR.render();
   }

   private static class EntityCluster {
      EntityType<?> type;
      List<Entity> entities = new ArrayList<>();
      Vec3d center;

      EntityCluster(Entity first) {
         this.type = first.getType();
         this.entities.add(first);
         this.center = new Vec3d(first.getX(), first.getY(), first.getZ());
      }

      void add(Entity e) {
         this.entities.add(e);
         double sumX = 0.0;
         double sumY = 0.0;
         double sumZ = 0.0;

         for (Entity ent : this.entities) {
            sumX += ent.getX();
            sumY += ent.getY();
            sumZ += ent.getZ();
         }

         this.center = new Vec3d(sumX / this.entities.size(), sumY / this.entities.size(), sumZ / this.entities.size());
      }
   }
}
