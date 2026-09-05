package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.alien.AlienRender3DUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class LavaESP extends Module {
   public static LavaESP INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSphere = this.settings.createGroup("Sphere");
   private final SettingGroup sgArrow = this.settings.createGroup("Arrow");
   private final SettingGroup sgText = this.settings.createGroup("Text");
   private final Setting<Integer> range = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("range")).description("CheckTestRange()")).defaultValue(8)).min(1).max(32).sliderRange(1, 32).build()
      );
   private final Setting<Boolean> dangerColors = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("danger-colors"))
                  .description("AutoColor(UsedownDirectionOneColor)"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> sphere = this.sgSphere
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("sphere"))
                  .description("Display3DBlockBox"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> sphereColor = this.sgSphere
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("sphere-color"))
               .description("BlockBoxColor(ColorDisableTimeUse)"))
            .defaultValue(new SettingColor(255, 50, 50, 200))
            .build()
      );
   private final Setting<Boolean> arrow = this.sgArrow
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("arrow"))
                  .description("DisplayPointLavacomeDirection'sArrowHead"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> arrowColor = this.sgArrow
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("arrow-color"))
               .description("ArrowHeadLineColor"))
            .defaultValue(new SettingColor(255, 100, 0, 255))
            .build()
      );
   private final Setting<Boolean> text = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("text"))
                  .description("atBlockupDirectionDisplayHintTextText"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> textColor = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("text-color"))
               .description("HintTextTextColor(ColorDisableTimeUse)"))
            .defaultValue(new SettingColor(255, 50, 50, 255))
            .build()
      );
   private final Map<BlockPos, LavaESP.LavaDanger> dangerCache = new HashMap<>();
   private int tickCounter = 0;
   private static final Color COLOR_DANGER = new Color(255, 0, 0, 200);
   private static final Color COLOR_WARNING = new Color(255, 100, 0, 200);
   private static final Color COLOR_CAUTION = new Color(255, 180, 0, 180);
   private static final Color COLOR_SAFE = new Color(255, 220, 50, 160);
   private static final String LABEL_DANGER = "§c§lLava!";
   private static final String LABEL_WARNING = "§6Lava";
   private static final String LABEL_SOURCE = "§eLava";
   private static final String LABEL_FLOW = "§7Lava";

   public LavaESP() {
      super(AddonTemplate.SC_CATEGORY, "LavaESP", "Highlights lava sources that could flood the area, with sphere, arrow and text indicators.");
      INSTANCE = this;
   }

   public void onDeactivate() {
      this.dangerCache.clear();
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.world != null && this.mc.player != null) {
         this.tickCounter++;
         if (this.tickCounter % 2 == 0) {
            int r = (Integer)this.range.get();
            BlockPos playerPos = this.mc.player.getBlockPos();
            this.dangerCache.clear();

            for (int dx = -r; dx <= r; dx++) {
               for (int dy = -r; dy <= r; dy++) {
                  for (int dz = -r; dz <= r; dz++) {
                     double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                     if (!(dist > r)) {
                        BlockPos checkPos = playerPos.add(dx, dy, dz);
                        if (!this.mc.world.isAir(checkPos) && !this.mc.world.getFluidState(checkPos).isIn(FluidTags.LAVA)) {
                           Set<Direction> lavaDirs = new HashSet<>();
                           int sourceCount = 0;

                           for (Direction dir : Direction.values()) {
                              FluidState fluid = this.mc.world.getFluidState(checkPos.offset(dir));
                              if (!fluid.isEmpty() && fluid.isIn(FluidTags.LAVA)) {
                                 lavaDirs.add(dir);
                                 if (fluid.isStill()) {
                                    sourceCount++;
                                 }
                              }
                           }

                           if (!lavaDirs.isEmpty()) {
                              this.dangerCache.put(checkPos, new LavaESP.LavaDanger(lavaDirs, sourceCount, dist));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (!this.dangerCache.isEmpty()) {
         for (Entry<BlockPos, LavaESP.LavaDanger> entry : this.dangerCache.entrySet()) {
            BlockPos pos = entry.getKey();
            LavaESP.LavaDanger danger = entry.getValue();
            Color sphereCol = this.getSphereColor(danger);
            Color arrowCol = toMeteor((SettingColor)this.arrowColor.get());
            if ((Boolean)this.sphere.get()) {
               this.drawBlockOutline(event, pos, sphereCol);
            }

            if ((Boolean)this.arrow.get()) {
               for (Direction dir : danger.lavaDirections) {
                  this.drawArrow(event, pos, dir, arrowCol);
               }
            }

            if ((Boolean)this.text.get()) {
               String label = this.getDangerLabel(danger);
               int labelColor = this.dangerColors.get() ? this.getDangerTextColor(danger) : ((SettingColor)this.textColor.get()).getPacked();
               AlienRender3DUtil.drawText3D(label, Vec3d.ofCenter(pos).add(0.0, 0.85, 0.0), 2.0, 0.5, Double.MAX_VALUE, labelColor);
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      AlienRender3DUtil.renderDeferred();
   }

   private void drawBlockOutline(Render3DEvent event, BlockPos pos, Color color) {
      Vec3d c = Vec3d.ofCenter(pos);
      Vec3d top = c.add(0.0, 0.5, 0.0);
      Vec3d bottom = c.add(0.0, -0.5, 0.0);
      Vec3d north = c.add(0.0, 0.0, -0.5);
      Vec3d south = c.add(0.0, 0.0, 0.5);
      Vec3d east = c.add(0.5, 0.0, 0.0);
      Vec3d west = c.add(-0.5, 0.0, 0.0);
      event.renderer.line(top.x, top.y, top.z, north.x, north.y, north.z, color);
      event.renderer.line(top.x, top.y, top.z, south.x, south.y, south.z, color);
      event.renderer.line(top.x, top.y, top.z, east.x, east.y, east.z, color);
      event.renderer.line(top.x, top.y, top.z, west.x, west.y, west.z, color);
      event.renderer.line(bottom.x, bottom.y, bottom.z, north.x, north.y, north.z, color);
      event.renderer.line(bottom.x, bottom.y, bottom.z, south.x, south.y, south.z, color);
      event.renderer.line(bottom.x, bottom.y, bottom.z, east.x, east.y, east.z, color);
      event.renderer.line(bottom.x, bottom.y, bottom.z, west.x, west.y, west.z, color);
   }

   private void drawArrow(Render3DEvent event, BlockPos pos, Direction dir, Color color) {
      Vec3d center = Vec3d.ofCenter(pos);
      double dx = dir.getOffsetX();
      double dy = dir.getOffsetY();
      double dz = dir.getOffsetZ();
      double shaftLen = 0.5;
      double headLen = 0.2;
      double headW = 0.12;
      double mx = center.x + dx * shaftLen;
      double my = center.y + dy * shaftLen;
      double mz = center.z + dz * shaftLen;
      double tx = center.x + dx * (shaftLen + headLen);
      double ty = center.y + dy * (shaftLen + headLen);
      double tz = center.z + dz * (shaftLen + headLen);
      event.renderer.line(center.x, center.y, center.z, mx, my, mz, color);
      Vec3d dirVec = new Vec3d(dx, dy, dz);
      Vec3d up;
      if (Math.abs(dy) < 0.9) {
         up = new Vec3d(0.0, 1.0, 0.0).crossProduct(dirVec).normalize();
      } else {
         up = new Vec3d(1.0, 0.0, 0.0).crossProduct(dirVec).normalize();
      }

      Vec3d right = dirVec.crossProduct(up).normalize();
      double hx = tx - dx * headLen;
      double hy = ty - dy * headLen;
      double hz = tz - dz * headLen;
      Vec3d[] base = new Vec3d[]{
         new Vec3d(
            hx + right.x * headW + up.x * headW,
            hy + right.y * headW + up.y * headW,
            hz + right.z * headW + up.z * headW
         ),
         new Vec3d(
            hx - right.x * headW + up.x * headW,
            hy - right.y * headW + up.y * headW,
            hz - right.z * headW + up.z * headW
         ),
         new Vec3d(
            hx - right.x * headW - up.x * headW,
            hy - right.y * headW - up.y * headW,
            hz - right.z * headW - up.z * headW
         ),
         new Vec3d(
            hx + right.x * headW - up.x * headW,
            hy + right.y * headW - up.y * headW,
            hz + right.z * headW - up.z * headW
         )
      };

      for (Vec3d b : base) {
         event.renderer.line(tx, ty, tz, b.x, b.y, b.z, color);
      }

      for (int i = 0; i < 4; i++) {
         Vec3d a = base[i];
         Vec3d b = base[(i + 1) % 4];
         event.renderer.line(a.x, a.y, a.z, b.x, b.y, b.z, color);
      }
   }

   private Color getSphereColor(LavaESP.LavaDanger danger) {
      return !this.dangerColors.get() ? toMeteor((SettingColor)this.sphereColor.get()) : this.getLevelColor(danger);
   }

   private Color getLevelColor(LavaESP.LavaDanger danger) {
      double d = danger.distance;
      boolean hasSource = danger.lavaSourceCount > 0;
      if (hasSource && d <= 4.0) {
         return COLOR_DANGER;
      } else if (d <= 4.0) {
         return COLOR_WARNING;
      } else {
         return hasSource ? COLOR_CAUTION : COLOR_SAFE;
      }
   }

   private int getDangerTextColor(LavaESP.LavaDanger danger) {
      Color c = this.getLevelColor(danger);
      return c.getPacked();
   }

   private String getDangerLabel(LavaESP.LavaDanger danger) {
      if (!(Boolean)this.dangerColors.get()) {
         return "Lava!";
      } else {
         double d = danger.distance;
         boolean hasSource = danger.lavaSourceCount > 0;
         if (hasSource && d <= 4.0) {
            return "§c§lLava!";
         } else if (d <= 4.0) {
            return "§6Lava";
         } else {
            return hasSource ? "§eLava" : "§7Lava";
         }
      }
   }

   private static Color toMeteor(SettingColor c) {
      return new Color(c.r, c.g, c.b, c.a);
   }

   private static class LavaDanger {
      final Set<Direction> lavaDirections;
      final int lavaSourceCount;
      final double distance;

      LavaDanger(Set<Direction> lavaDirections, int lavaSourceCount, double distance) {
         this.lavaDirections = lavaDirections;
         this.lavaSourceCount = lavaSourceCount;
         this.distance = distance;
      }
   }
}
