package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.math.Vec3d;

public class FireworklessFlight extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> autoStart = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("AutoStart")).description("装备鞘翅且离地时自动开始滑翔。")).defaultValue(true)).build());
   private final Setting<Double> divePitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("DivePitch"))
               .description("俯冲俯仰角（度，正值=向下），用于积累速度。"))
            .defaultValue(55.0)
            .min(10.0)
            .max(80.0)
            .sliderRange(10.0, 80.0)
            .build()
      );
   private final Setting<Double> climbPitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("ClimbPitch"))
               .description("抬头俯仰角（度，负值=向上），用于把速度转为升力/爬升。"))
            .defaultValue(-32.0)
            .min(-80.0)
            .max(-10.0)
            .sliderRange(-80.0, -10.0)
            .build()
      );
   private final Setting<Double> minSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("MinSpeed"))
               .description("水平速度低于此值时俯冲加速，达到后抬头滑翔。单位 m/s。"))
            .defaultValue(30.0)
            .min(5.0)
            .max(60.0)
            .sliderRange(5.0, 60.0)
            .build()
      );
   private boolean diving = false;
   private int airTicks = 0;
   private int takeoffCooldown = 0;

   public FireworklessFlight() {
      super(
         AddonTemplate.SC_CATEGORY,
         "FireworklessFlight",
         "Achieves firework-less elytra flight using the dive-then-pull-up game mechanic (no fireworks needed)."
      );
   }

   public void onActivate() {
      this.diving = false;
      this.airTicks = 0;
      this.takeoffCooldown = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.currentScreen == null) {
            if (!this.mc.player.isOnGround() && !this.mc.player.hasVehicle() && !this.mc.player.isTouchingWater()) {
               this.airTicks++;
            } else {
               this.airTicks = 0;
            }

            if (this.takeoffCooldown > 0) {
               this.takeoffCooldown--;
            }

            ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
            boolean wearingElytra = chest.getItem() == Items.ELYTRA && chest.getDamage() < chest.getMaxDamage() - 1;
            if (wearingElytra) {
               if (this.mc.player.isGliding()) {
                  Vec3d v = this.mc.player.getVelocity();
                  double hSpeed = Math.sqrt(v.x * v.x + v.z * v.z);
                  if (hSpeed < (Double)this.minSpeed.get()) {
                     this.diving = true;
                  } else {
                     this.diving = false;
                  }

                  float pitch = this.diving ? ((Double)this.divePitch.get()).floatValue() : ((Double)this.climbPitch.get()).floatValue();
                  this.mc.player.setPitch(pitch);
               }
            }
         }
      }
   }

   @EventHandler
   private void onTickPost(Post event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null) {
         if (this.mc.currentScreen == null) {
            if ((Boolean)this.autoStart.get()) {
               if (!this.mc.player.isGliding()) {
                  if (this.takeoffCooldown <= 0) {
                     if (!this.mc.player.isOnGround() && !this.mc.player.hasVehicle() && !this.mc.player.isTouchingWater()) {
                        if (this.airTicks >= 2) {
                           ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
                           if (chest.getItem() == Items.ELYTRA && chest.getDamage() < chest.getMaxDamage() - 1) {
                              this.mc
                                 .getNetworkHandler()
                                 .sendPacket(
                                    new Full(
                                       this.mc.player.getX(),
                                       this.mc.player.getY(),
                                       this.mc.player.getZ(),
                                       this.mc.player.getYaw(),
                                       this.mc.player.getPitch(),
                                       false,
                                       this.mc.player.horizontalCollision
                                    )
                                 );
                              this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_FALL_FLYING));
                              this.mc.player.startGliding();
                              this.takeoffCooldown = 3;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public String getInfoString() {
      if (this.mc.player != null && this.mc.player.isGliding()) {
         return this.diving ? "Dive" : "Climb";
      } else {
         return null;
      }
   }
}
