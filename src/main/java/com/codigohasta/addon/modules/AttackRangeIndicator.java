package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class AttackRangeIndicator extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final SettingGroup sgColors = this.settings.createGroup("ColorSetting");
   private final Setting<Double> attackRange = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("Attack Range")).description("Actual'sAttackDistance(atRangeinsideColor)."))
            .defaultValue(3.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> warningRange = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("PreDistance")).description("startDisplayPre'sDistance.")).defaultValue(10.0).min(0.0).sliderMax(20.0).build()
      );
   private final Setting<Integer> maxTargets = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("mostbigRenderAmount"))
                  .description("LimitSystemTimeRenderBox'sTargetAmount, DefenseStopatGround (0FornotLimitSystem)."))
               .defaultValue(10))
            .min(0)
            .sliderMax(50)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Mode"))
                  .description("RenderBoxand'sDisplayDirection."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<Boolean> showWarning = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayPreBox"))
                  .description("TargetatPreRangeinsideOutAttack RangeTime, whetherDisplay."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderSelf = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplaySelfBody"))
                  .description("atyourselfdownRenderOne, Pointcurrent'sAttackRange."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderTargetCircle = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayTarget"))
                  .description("atTarget'soutsideRenderOne."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> inRangeSideColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("AttackRange-DirectionBox"))
               .description("TargetatAttackRangeinsideTime'sDirectionBoxFill Color."))
            .defaultValue(new SettingColor(0, 255, 0, 40))
            .build()
      );
   private final Setting<SettingColor> inRangeLineColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("AttackRange-DirectionBoxBox"))
               .description("TargetatAttackRangeinsideTime'sDirectionBoxLineColor."))
            .defaultValue(new SettingColor(0, 255, 0, 200))
            .build()
      );
   private final Setting<SettingColor> warningSideColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("PreRange-DirectionBox"))
               .description("TargetatPreRangeinsideTime'sDirectionBoxFill Color."))
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .build()
      );
   private final Setting<SettingColor> warningLineColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("PreRange-DirectionBoxBox"))
               .description("TargetatPreRangeinsideTime'sDirectionBoxLineColor."))
            .defaultValue(new SettingColor(255, 0, 0, 200))
            .build()
      );
   private final Setting<SettingColor> targetCircleInRangeColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Target-CanAttackColor"))
               .description("TargetatAttackRangeinsideTime, 'sColor."))
            .defaultValue(new SettingColor(0, 255, 0, 150))
            .build()
      );
   private final Setting<SettingColor> targetCircleWarningColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Target-PreColor"))
               .description("TargetatPreRangeinsideTime, 'sColor."))
            .defaultValue(new SettingColor(255, 0, 0, 150))
            .build()
      );
   private final Setting<SettingColor> selfSideColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("SelfBody-Fill Color"))
            .defaultValue(new SettingColor(100, 100, 255, 25))
            .build()
      );
   private final Setting<SettingColor> selfLineColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("SelfBody-BoxColor"))
            .defaultValue(new SettingColor(100, 100, 255, 150))
            .build()
      );

   public AttackRangeIndicator() {
      super(AddonTemplate.CATEGORY, "AttackRangeIndicator", "Draws a visual indicator of your attack reach around your hitbox.");
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.renderSelf.get()) {
            this.drawSelfCircle(event);
         }

         List<Entity> targets = new ArrayList<>();

         for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof LivingEntity && entity != this.mc.player) {
               targets.add(entity);
            }
         }

         targets.sort(Comparator.comparingDouble(e -> this.mc.player.distanceTo(e)));
         int renderedCount = 0;
         int limit = (Integer)this.maxTargets.get();

         for (Entity entityx : targets) {
            if (limit > 0 && renderedCount >= limit) {
               break;
            }

            double distance = this.mc.player.distanceTo(entityx);
            if (distance <= (Double)this.attackRange.get()) {
               this.drawEntityBox(event, entityx, (SettingColor)this.inRangeSideColor.get(), (SettingColor)this.inRangeLineColor.get());
               if ((Boolean)this.renderTargetCircle.get()) {
                  this.drawTargetFeetCircle(event, entityx, (SettingColor)this.targetCircleInRangeColor.get());
               }

               renderedCount++;
            } else if ((Boolean)this.showWarning.get() && distance <= (Double)this.warningRange.get()) {
               this.drawEntityBox(event, entityx, (SettingColor)this.warningSideColor.get(), (SettingColor)this.warningLineColor.get());
               if ((Boolean)this.renderTargetCircle.get()) {
                  this.drawTargetFeetCircle(event, entityx, (SettingColor)this.targetCircleWarningColor.get());
               }

               renderedCount++;
            }
         }
      }
   }

   private void drawEntityBox(Render3DEvent event, Entity entity, SettingColor sideColor, SettingColor lineColor) {
      double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX());
      double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY());
      double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ());
      float w = entity.getWidth() / 2.0F;
      float h = entity.getHeight();
      Box interpolatedBox = new Box(x - w, y, z - w, x + w, y + h, z + w);
      event.renderer.box(interpolatedBox, sideColor, lineColor, (ShapeMode)this.shapeMode.get(), 0);
   }

   private void drawTargetFeetCircle(Render3DEvent event, Entity entity, SettingColor color) {
      double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX());
      double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY());
      double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ());
      double radius = entity.getWidth() * 0.8;
      this.renderCircleManual(event, x, y, z, radius, color, color, (ShapeMode)this.shapeMode.get());
   }

   private void drawSelfCircle(Render3DEvent event) {
      double x = MathHelper.lerp(event.tickDelta, this.mc.player.lastRenderX, this.mc.player.getX());
      double y = MathHelper.lerp(event.tickDelta, this.mc.player.lastRenderY, this.mc.player.getY());
      double z = MathHelper.lerp(event.tickDelta, this.mc.player.lastRenderZ, this.mc.player.getZ());
      this.renderCircleManual(
         event,
         x,
         y,
         z,
         (Double)this.attackRange.get(),
         (SettingColor)this.selfSideColor.get(),
         (SettingColor)this.selfLineColor.get(),
         (ShapeMode)this.shapeMode.get()
      );
   }

   private void renderCircleManual(
      Render3DEvent event, double x, double y, double z, double radius, SettingColor sideColor, SettingColor lineColor, ShapeMode mode
   ) {
      int segments = 40;
      double step = (Math.PI * 2) / segments;
      double prevX = x + Math.cos(0.0) * radius;
      double prevZ = z + Math.sin(0.0) * radius;

      for (int i = 1; i <= segments; i++) {
         double angle = i * step;
         double currX = x + Math.cos(angle) * radius;
         double currZ = z + Math.sin(angle) * radius;
         if (mode == ShapeMode.Sides || mode == ShapeMode.Both) {
            event.renderer.quad(x, y, z, prevX, y, prevZ, currX, y, currZ, currX, y, currZ, sideColor);
         }

         if (mode == ShapeMode.Lines || mode == ShapeMode.Both) {
            event.renderer.line(prevX, y, prevZ, currX, y, currZ, lineColor);
         }

         prevX = currX;
         prevZ = currZ;
      }
   }
}
