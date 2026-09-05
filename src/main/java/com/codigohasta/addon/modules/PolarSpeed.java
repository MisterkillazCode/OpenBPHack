package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class PolarSpeed extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> speed = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("speed")).description("Strafe acceleration multiplier (0-1)."))
            .defaultValue(1.0)
            .min(0.0)
            .max(1.0)
            .sliderMin(0.0)
            .sliderMax(1.0)
            .build()
      );
   private int jumps = 0;
   private int airTicks = 0;

   public PolarSpeed() {
      super(
         AddonTemplate.CATEGORY,
         "PolarSpeed",
         "Polar-style hop speed: auto-jumps while on the ground with constant strafe acceleration and a small air push-down on odd hops. For self-hosted / LAN PvP only."
      );
   }

   public void onActivate() {
      if (this.mc.player == null) {
         this.toggle();
      } else {
         this.jumps = 0;
         this.airTicks = 0;
      }
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         boolean onGround = this.mc.player.isOnGround();
         this.airTicks = onGround ? 0 : this.airTicks + 1;
         if (this.airTicks == 5 && this.jumps % 2 != 0) {
            this.mc.player.addVelocity(0.0, -0.03, 0.0);
         }

         if (onGround) {
            this.mc.player.jump();
            this.jumps++;
         }

         this.strafe(0.002 * (Double)this.speed.get());
      }
   }

   private void strafe(double accel) {
      if (PlayerUtils.isMoving()) {
         Vec3d v = this.mc.player.getVelocity();
         double bps = Math.hypot(v.x, v.z) * 20.0;
         Vec3d target = PlayerUtils.getHorizontalVelocity(bps + accel * 20.0);
         this.mc.player.setVelocity(target.x, v.y, target.z);
      }
   }
}
