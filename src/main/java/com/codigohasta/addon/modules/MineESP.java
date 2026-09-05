package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

public class MineESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("Range")).description("OnlyRenderRangeinside'sMineMove."))
            .defaultValue(15.0)
            .min(0.0)
            .sliderRange(0.0, 50.0)
            .build()
      );
   private final Setting<Double> maxTime = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("Time")).description("StopStopMineafter, RenderBoxOut'sTime()."))
            .defaultValue(3.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Mode"))
                  .description("RenderDirectionBox'swhichPart."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   public final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("LineColor"))
               .description("DirectionBoxLine'sColor."))
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
      );
   public final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("FaceColor"))
               .description("DirectionBoxFace'sColor."))
            .defaultValue(new SettingColor(255, 0, 0, 50))
            .build()
      );
   private final Setting<Boolean> renderName = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayName"))
                  .description("atDirectionBoxinsideDisplayMine'sName."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderProgress = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayEnterDegree"))
                  .description("DisplayMineEnterDegree'sthan."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> smoothProgress = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Number"))
                     .description("Sim 1-100 'sEnterDegreeNumber(ViewEffect)."))
                  .defaultValue(true))
               .visible(this.renderProgress::get))
            .build()
      );
   private final Setting<Boolean> renderBlock = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisplayBlockName"))
                  .description("DisplayatbyMine'sBlockName."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> textScale = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("TextSize")).description("NameandEnterDegree'sScalethan."))
               .defaultValue(1.0)
               .min(0.5)
               .sliderMax(3.0)
               .visible(() -> (Boolean)this.renderName.get() || (Boolean)this.renderProgress.get() || (Boolean)this.renderBlock.get()))
            .build()
      );
   private final Setting<SettingColor> textColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("TextColor"))
                  .description("TextDisplay'sColor."))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(() -> (Boolean)this.renderName.get() || (Boolean)this.renderProgress.get() || (Boolean)this.renderBlock.get()))
            .build()
      );
   private final List<MineESP.RenderInfo> renders = new ArrayList<>();
   private final ConcurrentLinkedQueue<MineESP.RenderInfo> pendingUpdates = new ConcurrentLinkedQueue<>();

   public MineESP() {
      super(AddonTemplate.SC_CATEGORY, "MineESP", "Shows blocks other players have recently mined, with time and player name labels. (Poor usability.)");
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         while (!this.pendingUpdates.isEmpty()) {
            MineESP.RenderInfo update = this.pendingUpdates.poll();
            MineESP.RenderInfo existing = null;

            for (MineESP.RenderInfo r : this.renders) {
               if (r.id == update.id && r.pos.equals(update.pos)) {
                  existing = r;
                  break;
               }
            }

            if (update.serverStage >= 0 && update.serverStage <= 9) {
               if (existing != null) {
                  existing.time = System.currentTimeMillis();
                  existing.serverStage = update.serverStage;
                  if (!update.cachedBlockName.equals("Air")) {
                     existing.cachedBlockName = update.cachedBlockName;
                  }
               } else {
                  this.renders.add(update);
               }
            }
         }

         this.renders.removeIf(rx -> System.currentTimeMillis() > rx.time + Math.round((Double)this.maxTime.get() * 1000.0));
         this.renders
            .forEach(
               rx -> {
                  double lifeDelta = Math.min((System.currentTimeMillis() - rx.time) / ((Double)this.maxTime.get() * 1000.0), 1.0);
                  float targetProgress = (rx.serverStage + 1) * 10.0F;
                  if ((Boolean)this.smoothProgress.get()) {
                     rx.animatedProgress = MathHelper.lerp(0.2F, rx.animatedProgress, targetProgress);
                  } else {
                     rx.animatedProgress = targetProgress;
                  }

                  event.renderer
                     .box(
                        this.getBox(rx.pos, this.getBoxProgress(Math.min(lifeDelta * 4.0, 1.0))),
                        this.getColor((Color)this.sideColor.get(), 1.0 - lifeDelta),
                        this.getColor((Color)this.lineColor.get(), 1.0 - lifeDelta),
                        (ShapeMode)this.shapeMode.get(),
                        0
                     );
               }
            );
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if ((Boolean)this.renderName.get() || (Boolean)this.renderProgress.get() || (Boolean)this.renderBlock.get()) {
         this.renders.forEach(r -> {
            StringBuilder textToRender = new StringBuilder();
            if ((Boolean)this.renderName.get()) {
               Entity entity = this.mc.world.getEntityById(r.id);
               if (entity != null) {
                  textToRender.append(entity.getName().getString());
               } else {
                  textToRender.append("Unknown");
               }
            }

            if ((Boolean)this.renderProgress.get()) {
               if (!textToRender.isEmpty()) {
                  textToRender.append(" ");
               }

               int displayVal = (int)Math.min(100.0, Math.ceil(r.animatedProgress));
               textToRender.append(displayVal).append("%");
            }

            if ((Boolean)this.renderBlock.get()) {
               if (!textToRender.isEmpty()) {
                  textToRender.append(" ");
               }

               textToRender.append(r.cachedBlockName);
            }

            if (!textToRender.isEmpty()) {
               Vector3d pos = new Vector3d(r.pos.getX() + 0.5, r.pos.getY() + 0.5, r.pos.getZ() + 0.5);
               if (NametagUtils.to2D(pos, (Double)this.textScale.get())) {
                  NametagUtils.begin(pos);
                  String text = textToRender.toString();
                  double delta = Math.min((System.currentTimeMillis() - r.time) / ((Double)this.maxTime.get() * 1000.0), 1.0);
                  Color finalColor = this.getColor((Color)this.textColor.get(), 1.0 - delta);
                  TextRenderer.get().begin(1.0, false, true);
                  double w = TextRenderer.get().getWidth(text);
                  double h = TextRenderer.get().getHeight();
                  TextRenderer.get().render(text, -w / 2.0, -h / 2.0, finalColor, true);
                  TextRenderer.get().end();
                  NametagUtils.end();
               }
            }
         });
      }
   }

   @EventHandler
   private void onReceive(Receive event) {
      if (event.packet instanceof BlockBreakingProgressS2CPacket packet) {
         BlockPos pos = packet.getPos();
         String blockName = "Unknown";
         if (this.mc.world != null) {
            BlockState state = this.mc.world.getBlockState(pos);
            if (!state.isAir()) {
               blockName = state.getBlock().getName().getString();
            } else {
               blockName = "Air";
            }
         }

         this.pendingUpdates.add(new MineESP.RenderInfo(pos, packet.getEntityId(), System.currentTimeMillis(), packet.getProgress(), blockName));
      }
   }

   private Color getColor(Color color, double delta) {
      return new Color(color.r, color.g, color.b, (int)Math.floor(color.a * delta));
   }

   private double getBoxProgress(double delta) {
      return 1.0 - Math.pow(1.0 - delta, 5.0);
   }

   private Box getBox(BlockPos pos, double progress) {
      return new Box(
         pos.getX() + 0.5 - progress / 2.0,
         pos.getY() + 0.5 - progress / 2.0,
         pos.getZ() + 0.5 - progress / 2.0,
         pos.getX() + 0.5 + progress / 2.0,
         pos.getY() + 0.5 + progress / 2.0,
         pos.getZ() + 0.5 + progress / 2.0
      );
   }

   private static class RenderInfo {
      final BlockPos pos;
      final int id;
      long time;
      int serverStage;
      float animatedProgress;
      String cachedBlockName;

      public RenderInfo(BlockPos pos, int id, long time, int serverStage, String blockName) {
         this.pos = pos;
         this.id = id;
         this.time = time;
         this.serverStage = serverStage;
         this.animatedProgress = (serverStage + 1) * 10;
         this.cachedBlockName = blockName;
      }
   }
}
