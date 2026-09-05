package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class HitboxESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgColors = this.settings.createGroup("ColorSetting");
   private final Setting<Boolean> ignoreSelf = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("yourself")).description("notRenderyourself's.")).defaultValue(true)).build());
   private final Setting<Double> lineWidth = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("LineWidth"))
            .defaultValue(2.5)
            .min(0.1)
            .max(5.0)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Render Mode"))
                  .description("SelectLineBox, allDisplay."))
               .defaultValue(ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("Fill Color"))
                  .description("Internal'sFill Color."))
               .defaultValue(new SettingColor(255, 255, 255, 30))
               .visible(() -> this.shapeMode.get() != ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("LineColor"))
                  .description("Line'sColor."))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(() -> this.shapeMode.get() != ShapeMode.Sides))
            .build()
      );

   public HitboxESP() {
      super(AddonTemplate.CATEGORY, "HitboxESP", "Draws players hitboxes as ESP boxes.");
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.world != null) {
         for (PlayerEntity player : this.mc.world.getPlayers()) {
            if ((!(Boolean)this.ignoreSelf.get() || player != this.mc.player) && !player.isSpectator()) {
               this.renderHitbox(event, player);
            }
         }
      }
   }

   private void renderHitbox(Render3DEvent event, PlayerEntity entity) {
      double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX());
      double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY());
      double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ());
      float width = entity.getWidth();
      float height = entity.getHeight();
      double minX = x - width / 2.0;
      double minZ = z - width / 2.0;
      double maxX = x + width / 2.0;
      double maxY = y + height;
      double maxZ = z + width / 2.0;
      event.renderer.box(minX, y, minZ, maxX, maxY, maxZ, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
   }
}
