package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FrostedIceBlock;
import net.minecraft.block.IceBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class GrimAc extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> notify = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("notify")).description("Show VL violations in chat.")).defaultValue(true)).build());
   private final Map<UUID, GrimAc.PlayerData> data = new HashMap<>();
   private static final double ICE_SPEED_MPS = 25.0;

   public GrimAc() {
      super(
         AddonTemplate.CATEGORY,
         "GrimAc",
         "Client-side GrimAC-style cheat detector. Detects suspicious movement (Speed / Fly / HighJump) of other players and reports violation levels (VL). Context-aware to avoid false-flagging legitimate players. For use on self-hosted / LAN PvP only."
      );
   }

   public static void toggleFromCommand() {
      GrimAc m = (GrimAc)Modules.get().get(GrimAc.class);
      if (m != null) {
         m.toggle();
         if (m.isActive()) {
            ChatUtils.info("(highlight)GrimAc开启(default)", new Object[0]);
         } else {
            ChatUtils.warning("(highlight)GrimAc关闭(default)", new Object[0]);
         }
      }
   }

   public void onDeactivate() {
      this.data.clear();
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         long now = System.currentTimeMillis();
         Set<UUID> present = new HashSet<>();

         for (PlayerEntity p : this.mc.world.getPlayers()) {
            UUID id = p.getUuid();
            present.add(id);
            if (p != this.mc.player && p.isAlive() && !p.isSpectator() && !p.isSleeping() && p.getVehicle() == null) {
               GrimAc.PlayerData d = this.data.computeIfAbsent(id, k -> new GrimAc.PlayerData());
               Vec3d pos = p.getSyncedPos();
               boolean normal = !p.isGliding() && !p.getAbilities().flying && !p.isTouchingWater() && !p.isInLava() && !p.isClimbing();
               boolean airborne = normal && !p.isOnGround();
               boolean ice = this.isIceUnderFeet(p);
               boolean levitation = p.hasStatusEffect(StatusEffects.LEVITATION);
               boolean slowFall = p.hasStatusEffect(StatusEffects.SLOW_FALLING);
               if (airborne) {
                  if (!d.arcActive) {
                     d.arcActive = true;
                     d.arcTakeoffY = pos.y;
                     d.arcPeakY = pos.y;
                     d.flyAirTicks = 0;
                  } else {
                     d.arcPeakY = Math.max(d.arcPeakY, pos.y);
                     d.flyAirTicks++;
                     if (d.flyAirTicks >= 40 && !levitation && !slowFall) {
                        double descended = d.arcTakeoffY - pos.y;
                        double rose = pos.y - d.arcTakeoffY;
                        if (descended < 2.0 && rose < 2.0) {
                           d.vlFly++;
                           if ((Boolean)this.notify.get() && now - d.lastFlyPrint > 1000L) {
                              d.lastFlyPrint = now;
                              this.printVl(p, d.vlFly, "Fly");
                           }

                           d.flyAirTicks = 0;
                           d.arcTakeoffY = pos.y;
                           d.arcPeakY = pos.y;
                        }
                     }
                  }
               } else {
                  if (d.arcActive) {
                     if (normal) {
                        double gain = d.arcPeakY - d.arcTakeoffY;
                        if (gain > this.expectedJumpHeight(p) + 2.0) {
                           d.vlJump++;
                           if ((Boolean)this.notify.get() && now - d.lastJumpPrint > 1000L) {
                              d.lastJumpPrint = now;
                              this.printVl(p, d.vlJump, "HighJump");
                           }
                        }
                     }

                     d.arcActive = false;
                  }

                  d.flyAirTicks = 0;
               }

               if (d.lastPos != null && normal && d.lastTime > 0L) {
                  double dt = (now - d.lastTime) / 1000.0;
                  double dx = pos.x - d.lastPos.x;
                  double dz = pos.z - d.lastPos.z;
                  double dist = Math.sqrt(dx * dx + dz * dz);
                  if (dist > 8.0) {
                     d.speedStreak = 0;
                  } else if (dt > 0.0 && dt <= 1.0) {
                     double speedMps = dist / dt;
                     double limit = ice ? 25.0 : this.maxSpeedMps(p);
                     if (speedMps > limit) {
                        d.speedStreak++;
                        if (d.speedStreak >= 3) {
                           d.vlSpeed++;
                           if ((Boolean)this.notify.get() && now - d.lastSpeedPrint > 1000L) {
                              d.lastSpeedPrint = now;
                              this.printVl(p, d.vlSpeed, "Speed");
                           }
                        }
                     } else {
                        d.speedStreak = 0;
                     }
                  }
               }

               d.lastPos = pos;
               d.lastTime = now;
            } else {
               this.data.remove(id);
            }
         }

         if (!this.data.isEmpty()) {
            this.data.keySet().removeIf(uuid -> !present.contains(uuid));
         }
      }
   }

   private void printVl(PlayerEntity p, double vl, String check) {
      this.info(
         Text.empty()
            .append(Text.literal(p.getName().getString()).formatted(Formatting.WHITE))
            .append(Text.literal(" VL: ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf((int)vl)).formatted(Formatting.RED))
            .append(Text.literal(" (" + check + ")").formatted(Formatting.YELLOW))
      );
   }

   private double maxSpeedMps(PlayerEntity p) {
      double base = p.isSprinting() ? 5.8 : 4.5;
      if (p.hasStatusEffect(StatusEffects.SPEED)) {
         int amp = p.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
         base *= 1.0 + 0.2 * amp;
      }

      return base * 1.4;
   }

   private double expectedJumpHeight(PlayerEntity p) {
      double jb = p.hasStatusEffect(StatusEffects.JUMP_BOOST) ? p.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1 : 0.0;
      double vy = Math.sqrt(0.17639999999999997 + 0.1 * jb);
      return vy * vy / 0.16;
   }

   private boolean isIceUnderFeet(PlayerEntity p) {
      BlockPos bp = p.getBlockPos().down();
      BlockState bs = this.mc.world.getBlockState(bp);
      Block b = bs.getBlock();
      return b instanceof IceBlock || b instanceof FrostedIceBlock;
   }

   private static class PlayerData {
      Vec3d lastPos;
      long lastTime;
      double vlSpeed = 0.0;
      double vlFly = 0.0;
      double vlJump = 0.0;
      int speedStreak = 0;
      boolean arcActive = false;
      double arcTakeoffY;
      double arcPeakY;
      int flyAirTicks = 0;
      long lastSpeedPrint = 0L;
      long lastFlyPrint = 0L;
      long lastJumpPrint = 0L;
   }
}
