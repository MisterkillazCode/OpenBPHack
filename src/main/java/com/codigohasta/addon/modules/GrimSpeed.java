package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GrimSpeed extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> multiplier = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("multiplier"))
               .description("Horizontal velocity multiplier applied while a LivingEntity is nearby and you have been airborne long enough."))
            .defaultValue(1.3)
            .min(1.0)
            .max(2.0)
            .sliderMin(1.0)
            .sliderMax(2.0)
            .build()
      );
   private final Setting<Double> airTicks = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("air-ticks")).description("Ticks spent off-ground before the algorithm activates."))
            .defaultValue(3.5)
            .min(0.0)
            .max(40.0)
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("range")).description("Half-extent of the AABB used to look for nearby LivingEntities."))
            .defaultValue(1.3)
            .min(0.5)
            .max(4.0)
            .sliderMin(0.5)
            .sliderMax(4.0)
            .build()
      );
   private int offGroundTicks = 0;

   public GrimSpeed() {
      super(
         AddonTemplate.CATEGORY, "GrimSpeed", "Grim-collide style horizontal speed boost when airborne near entities. For use on self-hosted / LAN PvP only."
      );
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.player.isAlive() && !this.mc.player.isSpectator()) {
            boolean inAir = !this.mc.player.isOnGround()
               && !this.mc.player.isClimbing()
               && !this.mc.player.isTouchingWater()
               && !this.mc.player.isInLava()
               && !this.mc.player.getAbilities().flying;
            if (inAir) {
               this.offGroundTicks++;
               if (!(this.offGroundTicks < (Double)this.airTicks.get())) {
                  if (PlayerUtils.isMoving()) {
                     double r = (Double)this.range.get();
                     Box box = this.mc.player.getBoundingBox().expand(r);
                     World world = this.mc.world;
                     List<Entity> nearby = world.getEntitiesByType(
                        TypeFilter.instanceOf(Entity.class),
                        box,
                        e -> e != this.mc.player && e instanceof LivingEntity && !(e instanceof ArmorStandEntity) && e.isAlive()
                     );
                     if (!nearby.isEmpty()) {
                        Vec3d v = this.mc.player.getVelocity();
                        double m = (Double)this.multiplier.get();
                        if (!(m <= 1.0)) {
                           this.mc.player.setVelocity(v.x * m, v.y, v.z * m);
                        }
                     }
                  }
               }
            } else {
               this.offGroundTicks = 0;
            }
         }
      }
   }
}
