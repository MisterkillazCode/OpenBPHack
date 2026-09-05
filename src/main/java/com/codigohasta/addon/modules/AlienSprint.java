package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.alien.AlienMovementUtil;
import com.codigohasta.addon.utils.alien.AlienPlayerUtil;
import com.codigohasta.addon.utils.alien.AlienRotationUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;

public class AlienSprint extends Module {
   public static AlienSprint INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<AlienSprint.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Sprint mode")).defaultValue(AlienSprint.Mode.Legit)).build());
   private final Setting<Boolean> inWaterPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("InWaterPause"))
                  .description("Pause sprinting in water"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> inWebPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("InWebPause"))
                  .description("Pause sprinting in webs"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> sneakingPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("SneakingPause"))
                  .description("Pause sprinting when sneaking"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> blindnessPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("BlindnessPause"))
                  .description("Pause sprinting when blind"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("UsingPause"))
                  .description("Pause sprinting when using items"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> lagPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("LagPause"))
                  .description("Pause sprinting after teleport"))
               .defaultValue(true))
            .build()
      );
   boolean pause = false;

   public AlienSprint() {
      super(AddonTemplate.CATEGORY, "AlienSprint", "Sprint module with conditional pauses: water, cobweb, sneaking, blindness, item use and lag.");
      INSTANCE = this;
   }

   private boolean isSprintPressed() {
      return this.mc.options.forwardKey.isPressed() && !this.mc.options.backKey.isPressed();
   }

   private boolean isBackPressed() {
      return this.mc.options.backKey.isPressed() && !this.mc.options.forwardKey.isPressed();
   }

   private boolean isLeftPressed() {
      return this.mc.options.leftKey.isPressed() && !this.mc.options.rightKey.isPressed();
   }

   private boolean isRightPressed() {
      return this.mc.options.rightKey.isPressed() && !this.mc.options.leftKey.isPressed();
   }

   public float getSprintYaw(float yaw) {
      if (this.isSprintPressed()) {
         if (this.isLeftPressed()) {
            yaw -= 45.0F;
         } else if (this.isRightPressed()) {
            yaw += 45.0F;
         }
      } else if (this.isBackPressed()) {
         yaw += 180.0F;
         if (this.isLeftPressed()) {
            yaw += 45.0F;
         } else if (this.isRightPressed()) {
            yaw -= 45.0F;
         }
      } else if (this.isLeftPressed()) {
         yaw -= 90.0F;
      } else if (this.isRightPressed()) {
         yaw += 90.0F;
      }

      return MathHelper.wrapDegrees(yaw);
   }

   public String getInfoString() {
      return ((AlienSprint.Mode)this.mode.get()).name();
   }

   public void onDeactivate() {
      AlienRotationUtil.shouldRotate = false;
      if (this.mc.player != null) {
         this.mc.player.bodyYaw = this.mc.player.getYaw();
         this.mc.player.headYaw = this.mc.player.getYaw();
      }
   }

   @EventHandler
   public void onPacket(Receive event) {
      if ((Boolean)this.lagPause.get() && event.packet instanceof PlayerPositionLookS2CPacket) {
         this.pause = true;
      }
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.mc.player.getPose().name().equals("GLIDING")) {
            AlienRotationUtil.shouldRotate = false;
            if (this.mode.get() == AlienSprint.Mode.PressKey) {
               if (!this.inWater()) {
                  this.mc.options.sprintKey.setPressed(true);
               }
            } else {
               this.mc.player.setSprinting(this.shouldSprint());
               if (this.mode.get() == AlienSprint.Mode.Rotation && AlienMovementUtil.isMoving()) {
                  AlienRotationUtil.sprintYaw = this.getSprintYaw(this.mc.player.getYaw());
                  AlienRotationUtil.shouldRotate = true;
               }
            }
         }
      }
   }

   @EventHandler
   public void onTickPost(Post event) {
      this.pause = false;
   }

   private boolean inWater() {
      return (Boolean)this.inWaterPause.get() && this.mc.player.isInFluid();
   }

   private boolean shouldSprint() {
      if ((this.mc.player.getHungerManager().getFoodLevel() > 6 || this.mc.player.isCreative())
         && AlienMovementUtil.isMoving()
         && !this.pause
         && (!this.mc.player.isSneaking() || !(Boolean)this.sneakingPause.get())
         && (!AlienPlayerUtil.isInWeb(this.mc.player) || !(Boolean)this.inWebPause.get())
         && (!this.mc.player.isUsingItem() || !(Boolean)this.usingPause.get())
         && !this.mc.player.isRiding()
         && (!this.mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || !(Boolean)this.blindnessPause.get())) {
         return switch ((AlienSprint.Mode)this.mode.get()) {
            case Legit -> this.mc.options.forwardKey.isPressed();
            case Rage -> true;
            case Rotation -> true;
            default -> false;
         };
      } else {
         return false;
      }
   }

   public static enum Mode {
      PressKey,
      Legit,
      Rage,
      Rotation;
   }
}
