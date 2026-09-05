package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Random;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public class AutoNod extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoNod.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("mode")).description("MoveDoMode.")).defaultValue(AutoNod.Mode.Sleepy)).build());
   private final Setting<AutoNod.Visibility> visibility = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("visibility"))
                  .description(
                     "DisplayDirection: \nPacket = , Onlyhavedon'tPlayercanlookto\nClient = Visible, FacewillwithMove\nSmart = First Person, Third PersonVisible"
                  ))
               .defaultValue(AutoNod.Visibility.Smart))
            .build()
      );
   private final Setting<Double> speed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("speed"))
               .description("Speed."))
            .defaultValue(3.0)
            .min(0.1)
            .sliderMax(50.0)
            .max(100.0)
            .build()
      );
   private final Setting<Double> angle = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("angle"))
               .description("AngleDegree."))
            .defaultValue(50.0)
            .min(1.0)
            .sliderMax(180.0)
            .max(360.0)
            .build()
      );
   private final Setting<Double> sleepWait = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("sleep-wait"))
                  .description("Timelong: HeadlowdownafterHoldnotMove'sTimethan(0.0-0.8)."))
               .defaultValue(0.3)
               .min(0.0)
               .max(0.8)
               .sliderMax(0.8)
               .visible(() -> this.mode.get() == AutoNod.Mode.Sleepy))
            .build()
      );
   private final Setting<Double> wakeUpSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("wake-up-speed"))
                  .description("Speed: NumberbigHeadfast."))
               .defaultValue(20.0)
               .min(1.0)
               .sliderMax(50.0)
               .visible(() -> this.mode.get() == AutoNod.Mode.Sleepy))
            .build()
      );
   private double timer = 0.0;
   private final Random random = new Random();
   private float lastAddedYaw = 0.0F;
   private float lastAddedPitch = 0.0F;

   public AutoNod() {
      super(AddonTemplate.CATEGORY, "AutoNod", "Nods your head (pitch animation) on demand, e.g. to signal yes/no.");
   }

   public void onActivate() {
      this.timer = 0.0;
      this.lastAddedYaw = 0.0F;
      this.lastAddedPitch = 0.0F;
   }

   public void onDeactivate() {
      if (this.mc.player != null) {
         this.mc.player.setYaw(this.mc.player.getYaw() - this.lastAddedYaw);
         this.mc.player.setPitch(this.mc.player.getPitch() - this.lastAddedPitch);
      }

      this.lastAddedYaw = 0.0F;
      this.lastAddedPitch = 0.0F;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null) {
         this.mc.player.setYaw(this.mc.player.getYaw() - this.lastAddedYaw);
         this.mc.player.setPitch(this.mc.player.getPitch() - this.lastAddedPitch);
         this.lastAddedYaw = 0.0F;
         this.lastAddedPitch = 0.0F;
         this.timer = this.timer + (Double)this.speed.get() * 0.05;
         double range = (Double)this.angle.get();
         float offsetX = 0.0F;
         float offsetY = 0.0F;
         switch ((AutoNod.Mode)this.mode.get()) {
            case Nod:
               offsetY = (float)(Math.sin(this.timer) * range);
               break;
            case Shake:
               offsetX = (float)(Math.sin(this.timer) * range);
               break;
            case Circular:
               offsetX = (float)(Math.cos(this.timer) * range);
               offsetY = (float)(Math.sin(this.timer) * range);
               break;
            case Diagonal:
               offsetX = (float)(Math.sin(this.timer) * range);
               offsetY = (float)(Math.sin(this.timer) * range);
               break;
            case Random:
               if ((int)(this.timer * 10.0) % 2 == 0) {
                  offsetX = (float)((this.random.nextDouble() - 0.5) * range);
                  offsetY = (float)((this.random.nextDouble() - 0.5) * range);
               }
               break;
            case Sleepy:
               double cycle = 10.0;
               double t = this.timer % cycle / cycle;
               double wakeLen = 0.5 / (Double)this.wakeUpSpeed.get();
               double waitLen = (Double)this.sleepWait.get();
               if (waitLen + wakeLen > 0.95) {
                  waitLen = 0.95 - wakeLen;
               }

               if (waitLen < 0.0) {
                  waitLen = 0.0;
               }

               double endDrop = 1.0 - wakeLen - waitLen;
               double startWake = 1.0 - wakeLen;
               if (t < endDrop) {
                  double p = t / endDrop;
                  offsetY = (float)(range * (p * p));
               } else if (t < startWake) {
                  offsetY = (float)range;
               } else {
                  double p = (t - startWake) / wakeLen;
                  offsetY = (float)(range * (1.0 - p));
               }
         }

         boolean usePacket = false;
         switch ((AutoNod.Visibility)this.visibility.get()) {
            case Packet:
               usePacket = true;
               break;
            case Client:
               usePacket = false;
               break;
            case Smart:
               usePacket = this.mc.options.getPerspective() == Perspective.FIRST_PERSON;
         }

         float realYaw = this.mc.player.getYaw();
         float realPitch = this.mc.player.getPitch();
         float targetYaw = realYaw + offsetX;
         float targetPitch = realPitch + offsetY;
         targetPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);
         if (usePacket) {
            Rotations.rotate(targetYaw, targetPitch, 100);
         } else {
            this.mc.player.setYaw(targetYaw);
            this.mc.player.setPitch(targetPitch);
            this.lastAddedYaw = targetYaw - realYaw;
            this.lastAddedPitch = targetPitch - realPitch;
         }
      }
   }

   public static enum Mode {
      Nod,
      Shake,
      Circular,
      Diagonal,
      Random,
      Sleepy;
   }

   public static enum Visibility {
      Packet,
      Client,
      Smart;
   }
}
