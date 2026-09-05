package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class VelocityAlien extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<VelocityAlien.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("The mode to use for velocity.")).defaultValue(VelocityAlien.Mode.Custom))
            .build()
      );
   private final Setting<Double> horizontal = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("horizontal"))
                  .description("Horizontal velocity factor."))
               .defaultValue(0.0)
               .min(0.0)
               .max(100.0)
               .sliderMax(100.0)
               .visible(() -> this.mode.get() == VelocityAlien.Mode.Custom))
            .build()
      );
   private final Setting<Double> vertical = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("vertical"))
                  .description("Vertical velocity factor."))
               .defaultValue(0.0)
               .min(0.0)
               .max(100.0)
               .sliderMax(100.0)
               .visible(() -> this.mode.get() == VelocityAlien.Mode.Custom))
            .build()
      );
   private final Setting<Boolean> flagInWall = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("flag-in-wall"))
                     .description("Whether to flag when inside a wall (Grim/Wall mode)."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() == VelocityAlien.Mode.Grim || this.mode.get() == VelocityAlien.Mode.Wall))
            .build()
      );
   private final Setting<Boolean> noExplosions = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("no-explosions"))
                  .description("Prevents knockback from explosions."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> pauseInLiquid = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("pause-in-liquid"))
                  .description("Pauses the module when in liquid."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> fishBob = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("no-fish-bob"))
                  .description("Prevents being pulled by fishing rods."))
               .defaultValue(true))
            .build()
      );
   private long lastTeleportTime = 0L;
   private boolean flag;
   private Field explosionVecField;
   private Field velXField;
   private Field velYField;
   private Field velZField;
   private Field entityIdField;
   private boolean reflectionInitialized = false;

   public VelocityAlien() {
      super(AddonTemplate.CATEGORY, "VelocityAlien", "Customises knockback taken: horizontal and vertical modifiers with conditional pauses. (Unreliable.)");
   }

   private void initReflection() {
      if (!this.reflectionInitialized) {
         try {
            for (Field f : ExplosionS2CPacket.class.getDeclaredFields()) {
               f.setAccessible(true);
               if (f.getType() == Vec3d.class) {
                  this.explosionVecField = f;
                  break;
               }
            }

            List<Field> intFields = new ArrayList<>();

            for (Field fx : EntityVelocityUpdateS2CPacket.class.getDeclaredFields()) {
               fx.setAccessible(true);
               if (fx.getType() == int.class || fx.getType() == int.class) {
                  intFields.add(fx);
               }
            }

            if (intFields.size() >= 4) {
               this.entityIdField = intFields.get(0);
               this.velXField = intFields.get(1);
               this.velYField = intFields.get(2);
               this.velZField = intFields.get(3);
            }

            this.reflectionInitialized = true;
         } catch (Exception var6) {
            var6.printStackTrace();
         }
      }
   }

   public String getInfoString() {
      return this.mode.get() == VelocityAlien.Mode.Custom
         ? String.format("%.0f%% %.0f%%", this.horizontal.get(), this.vertical.get())
         : ((VelocityAlien.Mode)this.mode.get()).name();
   }

   @EventHandler
   public void onPacketReceive(Receive event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.reflectionInitialized) {
            this.initReflection();
         }

         if (event.packet instanceof PlayerPositionLookS2CPacket) {
            this.lastTeleportTime = System.currentTimeMillis();
         }

         if (!(Boolean)this.pauseInLiquid.get() || !this.mc.player.isTouchingWater() && !this.mc.player.isInLava()) {
            if ((Boolean)this.fishBob.get()
               && event.packet instanceof EntityStatusS2CPacket packet
               && packet.getStatus() == 31
               && packet.getEntity(this.mc.world) instanceof FishingBobberEntity fishHook
               && fishHook.getHookedEntity() == this.mc.player) {
               event.cancel();
            }

            if (this.mode.get() == VelocityAlien.Mode.Custom) {
               float h = ((Double)this.horizontal.get()).floatValue() / 100.0F;
               float v = ((Double)this.vertical.get()).floatValue() / 100.0F;
               if (event.packet instanceof ExplosionS2CPacket packet) {
                  if ((Boolean)this.noExplosions.get()) {
                     event.cancel();
                     return;
                  }

                  if (this.explosionVecField != null) {
                     try {
                        Vec3d original = (Vec3d)this.explosionVecField.get(packet);
                        if (original != null) {
                           Vec3d modified = new Vec3d(original.x * h, original.y * v, original.z * h);
                           this.explosionVecField.set(packet, modified);
                        }
                     } catch (Exception var8) {
                        var8.printStackTrace();
                     }
                  }

                  return;
               }

               if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
                  try {
                     if (this.entityIdField != null && this.entityIdField.getInt(packet) == this.mc.player.getId()) {
                        if ((Double)this.horizontal.get() == 0.0 && (Double)this.vertical.get() == 0.0) {
                           event.cancel();
                        } else if (this.velXField != null) {
                           int x = this.velXField.getInt(packet);
                           int y = this.velYField.getInt(packet);
                           int z = this.velZField.getInt(packet);
                           this.velXField.setInt(packet, (int)(x * h));
                           this.velYField.setInt(packet, (int)(y * v));
                           this.velZField.setInt(packet, (int)(z * h));
                        }
                     }
                  } catch (Exception var11) {
                     var11.printStackTrace();
                  }
               }
            } else {
               if (System.currentTimeMillis() - this.lastTeleportTime < 100L) {
                  return;
               }

               boolean insideBlock = this.isInsideBlock();
               if (this.mode.get() == VelocityAlien.Mode.Wall && !insideBlock) {
                  return;
               }

               if (event.packet instanceof ExplosionS2CPacket) {
                  if (this.explosionVecField != null) {
                     try {
                        this.explosionVecField.set(event.packet, Vec3d.ZERO);
                     } catch (Exception var9) {
                     }
                  }

                  this.flag = true;
                  return;
               }

               if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
                  try {
                     if (this.entityIdField != null && this.entityIdField.getInt(packet) == this.mc.player.getId()) {
                        event.cancel();
                        this.flag = true;
                     }
                  } catch (Exception var10) {
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(Post event) {
      if (this.mc.player != null) {
         if (!(Boolean)this.pauseInLiquid.get() || !this.mc.player.isTouchingWater() && !this.mc.player.isInLava()) {
            if (this.flag) {
               if (System.currentTimeMillis() - this.lastTeleportTime >= 100L) {
                  boolean insideBlock = this.isInsideBlock();
                  if ((Boolean)this.flagInWall.get() || !insideBlock) {
                     BlockPos pos = this.mc.player.getBlockPos();
                     this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
                  }
               }

               this.flag = false;
            }
         }
      }
   }

   private boolean isInsideBlock() {
      return this.mc.world.getBlockState(this.mc.player.getBlockPos()).isSolid()
         || this.mc.world.getBlockState(this.mc.player.getBlockPos().up()).isSolid();
   }

   public static enum Mode {
      Custom,
      Grim,
      Wall;
   }
}
