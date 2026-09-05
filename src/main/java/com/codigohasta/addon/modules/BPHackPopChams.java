package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.alien.AlienAnimation;
import com.codigohasta.addon.utils.alien.AlienEasing;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class BPHackPopChams extends Module {
   public static BPHackPopChams INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AlienEasing> ease = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("ease")).description("Easing mode used for the animation.")).defaultValue(AlienEasing.CubicInOut))
            .build()
      );
   private final Setting<Boolean> fillEnabled = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("fill"))
                  .description("Render filled model."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> fillColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("fill-color"))
               .description("Fill color."))
            .defaultValue(new SettingColor(55, 135, 255, 100))
            .build()
      );
   private final Setting<Boolean> lineEnabled = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("line"))
                  .description("Render the outlind of the model.er line/outline."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("line-color"))
               .description("Line color."))
            .defaultValue(new SettingColor(0, 210, 255, 120))
            .build()
      );
   private final Setting<Boolean> alpha = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("alpha"))
                  .description("Outalpha."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> forceSneak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("force-sneak"))
                  .description("Force the ghost to be in sneaking pose."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> noSelf = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("no-self"))
                  .description("Don't render ghost for the player you are controlling.host for yourself."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> noLimb = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("no-limb"))
                  .description("Disable limb movement for the ghost. (May not work in 1.21.11)"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> fadeTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("fade-time"))
                  .description("OutTime, SingleBit."))
               .defaultValue(300))
            .min(0)
            .max(1000)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Double> yOffset = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("y-offset"))
               .description("Image'syMoveAmount."))
            .defaultValue(0.0)
            .min(-10.0)
            .max(10.0)
            .build()
      );
   private final Setting<Double> scale = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("scale"))
               .description("Image'sScalethan."))
            .defaultValue(1.0)
            .min(0.0)
            .max(2.0)
            .build()
      );
   private final Setting<Double> yaw = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("yaw"))
               .description("outside'syawRotate."))
            .defaultValue(0.0)
            .min(0.0)
            .max(720.0)
            .build()
      );
   private final CopyOnWriteArrayList<BPHackPopChams.GhostPlayer> ghostList = new CopyOnWriteArrayList<>();

   public static void onFakePlayerTotemPop(PlayerEntity player) {
      if (INSTANCE != null && INSTANCE.isActive()) {
         INSTANCE.ghostList.add(INSTANCE.new GhostPlayer(player));
      }
   }

   public BPHackPopChams() {
      super(AddonTemplate.CATEGORY, "BPHackPopChams", "LiquidBounce-style pop chams: renders a ghost where a player popped a totem.");
      INSTANCE = this;
   }

   public void onDeactivate() {
      this.ghostList.clear();
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      if (event.packet instanceof EntityStatusS2CPacket p) {
         if (p.getStatus() == 35) {
            Entity entity = p.getEntity(this.mc.world);
            if (entity instanceof PlayerEntity player) {
               if (!(Boolean)this.noSelf.get() || entity != this.mc.player) {
                  this.ghostList.add(new BPHackPopChams.GhostPlayer(player));
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      this.ghostList.removeIf(ghost -> ghost.render(event));
   }

   private class GhostPlayer extends FakePlayerEntity {
      private final AlienAnimation animation = new AlienAnimation();
      private final double origX;
      private final double origY;
      private final double origZ;
      private final float origBodyYaw;
      private final float origHeadYaw;
      private final int sourceId;

      public GhostPlayer(PlayerEntity player) {
         super(player, "ghost", 20.0F, false);
         this.origX = player.getX();
         this.origY = player.getY();
         this.origZ = player.getZ();
         this.origBodyYaw = player.bodyYaw;
         this.origHeadYaw = player.headYaw;
         this.sourceId = player.getId();
         this.copyPositionAndRotation(player);
         this.bodyYaw = player.bodyYaw;
         this.headYaw = player.headYaw;
         if ((Boolean)BPHackPopChams.this.forceSneak.get()) {
            this.setSneaking(true);
         }
      }

      public boolean render(Render3DEvent event) {
         double progress = this.animation.get(1.0, ((Integer)BPHackPopChams.this.fadeTime.get()).intValue(), (AlienEasing)BPHackPopChams.this.ease.get());
         if (progress >= 1.0) {
            return true;
         } else {
            double animScale = 1.0 + ((Double)BPHackPopChams.this.scale.get() - 1.0) * progress;
            double animYaw = (Double)BPHackPopChams.this.yaw.get() * progress;
            double animYOffset = (Double)BPHackPopChams.this.yOffset.get() * progress;
            double animAlpha = BPHackPopChams.this.alpha.get() ? 1.0 - progress : 1.0;
            this.setPosition(this.origX, this.origY + animYOffset, this.origZ);
            this.bodyYaw = this.origBodyYaw + (float)animYaw;
            this.headYaw = this.origHeadYaw + (float)animYaw;
            this.lastRenderX = this.getX();
            this.lastRenderY = this.getY();
            this.lastRenderZ = this.getZ();
            int fillA = (int)(((SettingColor)BPHackPopChams.this.fillColor.get()).a * animAlpha);
            int lineA = (int)(((SettingColor)BPHackPopChams.this.lineColor.get()).a * animAlpha);
            Color sideC = new Color(
               ((SettingColor)BPHackPopChams.this.fillColor.get()).r,
               ((SettingColor)BPHackPopChams.this.fillColor.get()).g,
               ((SettingColor)BPHackPopChams.this.fillColor.get()).b,
               Math.max(0, Math.min(255, fillA))
            );
            Color lineC = new Color(
               ((SettingColor)BPHackPopChams.this.lineColor.get()).r,
               ((SettingColor)BPHackPopChams.this.lineColor.get()).g,
               ((SettingColor)BPHackPopChams.this.lineColor.get()).b,
               Math.max(0, Math.min(255, lineA))
            );
            ShapeMode mode;
            if ((Boolean)BPHackPopChams.this.fillEnabled.get() && (Boolean)BPHackPopChams.this.lineEnabled.get()) {
               mode = ShapeMode.Both;
            } else if ((Boolean)BPHackPopChams.this.fillEnabled.get()) {
               mode = ShapeMode.Sides;
            } else if ((Boolean)BPHackPopChams.this.lineEnabled.get()) {
               mode = ShapeMode.Lines;
            } else {
               mode = ShapeMode.Sides;
            }

            WireframeEntityRenderer.render(event, this, animScale, sideC, lineC, mode);
            return false;
         }
      }
   }
}
