package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ShieldESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Renderoutside");
   private final SettingGroup sgColors = this.settings.createGroup("ColorSetting");
   private final Setting<Boolean> renderSelf = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Displayyourself")).description("whetherRenderyourself'sDefenseRange.")).defaultValue(false)).build()
      );
   private final Setting<Double> radius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Radius"))
               .description("Defense'sRadiusSize."))
            .defaultValue(1.5)
            .min(0.5)
            .sliderMax(3.0)
            .build()
      );
   private final Setting<Double> height = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("HeightMove"))
               .description("Render'sHeightPosition(to)."))
            .defaultValue(0.1)
            .min(0.0)
            .sliderMax(2.0)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Render Mode"))
                  .description("Display."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<Integer> segments = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Degree"))
                  .description("'sDegree(Number)."))
               .defaultValue(30))
            .min(10)
            .max(60)
            .build()
      );
   private final Setting<SettingColor> otherSideColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("hePlayer-Fill Color"))
                  .description("OtherPlayerInternal'sColor."))
               .defaultValue(new SettingColor(255, 0, 0, 60))
               .visible(() -> this.shapeMode.get() != ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> otherLineColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("hePlayer-LineColor"))
                  .description("OtherPlayer'sColor."))
               .defaultValue(new SettingColor(255, 0, 0, 255))
               .visible(() -> this.shapeMode.get() != ShapeMode.Sides))
            .build()
      );
   private final Setting<SettingColor> selfSideColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("yourself-Fill Color"))
                  .description("yourselfInternal'sColor."))
               .defaultValue(new SettingColor(0, 100, 255, 60))
               .visible(() -> (Boolean)this.renderSelf.get() && this.shapeMode.get() != ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> selfLineColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("yourself-LineColor"))
                  .description("yourself'sColor."))
               .defaultValue(new SettingColor(0, 100, 255, 255))
               .visible(() -> (Boolean)this.renderSelf.get() && this.shapeMode.get() != ShapeMode.Sides))
            .build()
      );

   public ShieldESP() {
      super(AddonTemplate.CATEGORY, "ShieldESP", "Draws the 180-degree shield blocking arc around players.");
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         for (PlayerEntity player : this.mc.world.getPlayers()) {
            if ((player != this.mc.player || (Boolean)this.renderSelf.get()) && player.isBlocking()) {
               this.renderShieldArc(event, player);
            }
         }
      }
   }

   private void renderShieldArc(Render3DEvent event, PlayerEntity player) {
      boolean isMe = player == this.mc.player;
      SettingColor currentSideColor = isMe ? (SettingColor)this.selfSideColor.get() : (SettingColor)this.otherSideColor.get();
      SettingColor currentLineColor = isMe ? (SettingColor)this.selfLineColor.get() : (SettingColor)this.otherLineColor.get();
      double x = MathHelper.lerp(event.tickDelta, player.lastRenderX, player.getX());
      double y = MathHelper.lerp(event.tickDelta, player.lastRenderY, player.getY()) + (Double)this.height.get();
      double z = MathHelper.lerp(event.tickDelta, player.lastRenderZ, player.getZ());
      float yaw = MathHelper.lerp(event.tickDelta, player.lastBodyYaw, player.bodyYaw);
      List<Vec3d> points = new ArrayList<>();
      Vec3d center = new Vec3d(x, y, z);
      points.add(center);
      int segs = (Integer)this.segments.get();
      double r = (Double)this.radius.get();

      for (int i = 0; i <= segs; i++) {
         double offsetDeg = -90.0 + 180.0 * i / segs;
         double radians = Math.toRadians(yaw + offsetDeg + 180.0);
         double px = x + Math.sin(radians) * r;
         double pz = z - Math.cos(radians) * r;
         points.add(new Vec3d(px, y, pz));
      }

      if (this.shapeMode.get() != ShapeMode.Lines) {
         for (int i = 1; i < points.size() - 1; i++) {
            Vec3d p1 = points.get(i);
            Vec3d p2 = points.get(i + 1);
            event.renderer
               .quad(
                  center.x,
                  center.y,
                  center.z,
                  p1.x,
                  p1.y,
                  p1.z,
                  p2.x,
                  p2.y,
                  p2.z,
                  center.x,
                  center.y,
                  center.z,
                  currentSideColor
               );
            event.renderer
               .quad(
                  center.x,
                  center.y,
                  center.z,
                  p2.x,
                  p2.y,
                  p2.z,
                  p1.x,
                  p1.y,
                  p1.z,
                  center.x,
                  center.y,
                  center.z,
                  currentSideColor
               );
         }
      }

      if (this.shapeMode.get() != ShapeMode.Sides) {
         Vec3d start = points.get(1);
         Vec3d end = points.get(points.size() - 1);
         event.renderer.line(center.x, center.y, center.z, start.x, start.y, start.z, currentLineColor);
         event.renderer.line(center.x, center.y, center.z, end.x, end.y, end.z, currentLineColor);

         for (int i = 1; i < points.size() - 1; i++) {
            Vec3d p1 = points.get(i);
            Vec3d p2 = points.get(i + 1);
            event.renderer.line(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, currentLineColor);
         }
      }
   }
}
