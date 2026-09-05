package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class PlaceRender extends Module {
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("ShapeMode")).description("Render Mode")).defaultValue(ShapeMode.Both)).build());
   private final Setting<Integer> speed = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Speed"))
                  .description("RenderSpeed"))
               .defaultValue(10))
            .sliderRange(1, 100)
            .build()
      );
   private final Setting<Double> animationExp = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Animation Exponent"))
               .description("MovePointNumber"))
            .defaultValue(3.0)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .build()
      );
   private final Setting<SettingColor> sideStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("SideStart"))
               .description("BlockstartColor"))
            .defaultValue(new SettingColor(55, 135, 255, 0))
            .build()
      );
   private final Setting<SettingColor> sideEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("SideEnd"))
               .description("BlockendColor"))
            .defaultValue(new SettingColor(55, 135, 255, 50))
            .build()
      );
   private final Setting<SettingColor> lineStartColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("LineStart"))
               .description("BlockBoxstartColor"))
            .defaultValue(new SettingColor(55, 135, 255, 0))
            .build()
      );
   private final Setting<SettingColor> lineEndColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("LineEnd"))
               .description("BlockBoxendColor"))
            .defaultValue(new SettingColor(55, 135, 255, 255))
            .build()
      );
   private final Map<BlockPos, PlaceRender.PosEntry> posEntries = new HashMap<>();

   public PlaceRender() {
      super(AddonTemplate.CATEGORY, "PlaceRender", "Renders place previews.");
   }

   public void onActivate() {
      BlockUtil.placeList.clear();
   }

   @EventHandler
   public void onRender(Render3DEvent event) {
      if (!BlockUtil.placeList.isEmpty()) {
         for (BlockPos pos : BlockUtil.placeList) {
            if (!this.posEntries.containsKey(pos)) {
               this.posEntries.put(pos, new PlaceRender.PosEntry(pos));
            }
         }

         for (BlockPos posx : BlockUtil.placeList) {
            PlaceRender.PosEntry entry = this.posEntries.get(posx);
            if (entry != null && !(entry.progress <= 0.0)) {
               double p = 1.0 - MathHelper.clamp(entry.progress, 0.0, 1.0);
               p = Math.pow(p, (Double)this.animationExp.get());
               p = 1.0 - p;
               double size = p / 2.0;
               Box box = new Box(
                  posx.getX() + 0.5 - size,
                  posx.getY() + 0.5 - size,
                  posx.getZ() + 0.5 - size,
                  posx.getX() + 0.5 + size,
                  posx.getY() + 0.5 + size,
                  posx.getZ() + 0.5 + size
               );
               Color side = this.getColor((Color)this.sideStartColor.get(), (Color)this.sideEndColor.get(), p);
               Color line = this.getColor((Color)this.lineStartColor.get(), (Color)this.lineEndColor.get(), p);
               event.renderer.box(box, side, line, (ShapeMode)this.shapeMode.get(), 0);
               entry.progress = entry.progress - ((Integer)this.speed.get()).intValue() * 0.01;
            } else {
               this.posEntries.remove(posx);
               BlockUtil.placeList.remove(posx);
            }
         }
      }
   }

   private Color getColor(Color start, Color end, double progress) {
      return new Color(
         this.lerp(start.r, end.r, progress), this.lerp(start.g, end.g, progress), this.lerp(start.b, end.b, progress), this.lerp(start.a, end.a, progress)
      );
   }

   private int lerp(double start, double end, double d) {
      return (int)Math.round(start + (end - start) * d);
   }

   private static class PosEntry {
      final BlockPos pos;
      double progress = 1.0;

      PosEntry(BlockPos pos) {
         this.pos = pos;
      }
   }
}
