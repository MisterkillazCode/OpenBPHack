package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.bmw.BMWDirectionalInput;
import com.codigohasta.addon.utils.bmw.BMWPlayerUtil;
import com.codigohasta.addon.utils.bmw.BMWRotation;
import com.codigohasta.addon.utils.bmw.BMWRotationManager;
import com.codigohasta.addon.utils.bmw.BMWRotationUtil;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BMWSprint extends Module {
   public static BMWSprint INSTANCE;
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<BMWSprint.Mode> sprintMode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("mode")).description("runMode")).defaultValue(BMWSprint.Mode.OMNIROTATIONAL)).build());
   private final Setting<Boolean> ignoreBlindness = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ignore-blindness"))
                  .description("atStatusdownHoldrun"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> ignoreHunger = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ignore-hunger"))
                  .description("atStatusdownHoldrun"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> ignoreCollision = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ignore-collision"))
                  .description("TimeHoldrun"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> stopOnGround = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("stop-on-ground"))
                  .description("atGroundFaceTimenoMoveStopStoprun( Legit Mode)"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> stopOnAir = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("stop-on-air"))
                  .description("atAirinTimenoMoveStopStoprun( Legit Mode)"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> elytraRotation = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("elytra-rotation"))
                  .description("FlyTimealsoEnable Omnirotational Rotate( WASD Direction)"))
               .defaultValue(true))
            .build()
      );
   private float preElytraYaw;
   private float preElytraPitch;
   private boolean elytraChangedLookThisTick;

   public BMWSprint() {
      super(
         AddonTemplate.CATEGORY,
         "BMWSprint",
         "BMWClient nextgen sprint. Omnirotational: directional run plus auto-rotate; fly-time WASD direction does not trigger Grim."
      );
      INSTANCE = this;
   }

   public void onActivate() {
      BMWRotationUtil.shouldRotate = false;
      BMWRotationManager.clearTarget();
      this.elytraChangedLookThisTick = false;
   }

   public void onDeactivate() {
      BMWRotationUtil.shouldRotate = false;
      BMWRotationManager.clearTarget();
      if (mc.player != null) {
         mc.player.bodyYaw = mc.player.getYaw();
         mc.player.headYaw = mc.player.getYaw();
      }

      this.elytraChangedLookThisTick = false;
   }

   public String getInfoString() {
      return ((BMWSprint.Mode)this.sprintMode.get()).name();
   }

   @EventHandler(
      priority = 150
   )
   private void onTickPre(Pre event) {
      if (mc.player != null && mc.world != null) {
         BMWRotationUtil.shouldRotate = false;
         BMWRotationManager.clearTarget();
         this.elytraChangedLookThisTick = false;
         boolean isFlying = mc.player.getPose().name().equals("GLIDING");
         if (!isFlying || this.sprintMode.get() == BMWSprint.Mode.OMNIROTATIONAL) {
            switch ((BMWSprint.Mode)this.sprintMode.get()) {
               case LEGIT:
                  this.handleLegit();
                  break;
               case OMNIDIRECTIONAL:
                  this.handleOmnidirectional();
                  break;
               case OMNIROTATIONAL:
                  if (isFlying) {
                     this.handleOmnirotationalElytra();
                  } else {
                     this.handleOmnirotational();
                  }
            }

            BMWRotationManager.update();
         }
      }
   }

   @EventHandler
   private void onTickPost(Post event) {
      if (this.elytraChangedLookThisTick && mc.player != null) {
         mc.player.setYaw(this.preElytraYaw);
         mc.player.setPitch(this.preElytraPitch);
         this.elytraChangedLookThisTick = false;
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.sprintMode.get() == BMWSprint.Mode.OMNIROTATIONAL) {
         if (BMWRotationUtil.shouldRotate) {
            if (BMWPlayerUtil.isMoving()) {
               double horizSpeed = Math.sqrt(event.movement.x * event.movement.x + event.movement.z * event.movement.z);
               if (!(horizSpeed < 1.0E-7)) {
                  float yawRad = BMWRotationUtil.sprintYaw * (float) (Math.PI / 180.0);
                  double newX = -MathHelper.sin(yawRad) * horizSpeed;
                  double newZ = MathHelper.cos(yawRad) * horizSpeed;
                  event.movement = new Vec3d(newX, event.movement.y, newZ);
               }
            }
         }
      }
   }

   private void handleLegit() {
      if (this.canSprint()) {
         if (mc.options.forwardKey.isPressed()) {
            mc.player.setSprinting(true);
         } else {
            if (!mc.player.isSprinting()) {
               return;
            }

            if ((Boolean)this.stopOnGround.get() && mc.player.isOnGround()) {
               mc.player.setSprinting(false);
            } else if ((Boolean)this.stopOnAir.get() && !mc.player.isOnGround()) {
               mc.player.setSprinting(false);
            }
         }
      }
   }

   private void handleOmnidirectional() {
      if (this.canSprint()) {
         mc.player.setSprinting(true);
      }
   }

   private void handleOmnirotational() {
      if (this.canSprint()) {
         mc.player.setSprinting(true);
         float moveYaw = BMWPlayerUtil.getMovementDirectionOfInput(mc.player.getYaw(), BMWDirectionalInput.fromPlayer());
         moveYaw += (float)((Math.random() - 0.5) * 0.001);
         BMWRotationManager.setRotationTarget(new BMWRotation(moveYaw, mc.player.getPitch()), false);
         BMWRotationUtil.sprintYaw = moveYaw;
         BMWRotationUtil.shouldRotate = true;
      }
   }

   private void handleOmnirotationalElytra() {
      if ((Boolean)this.elytraRotation.get()) {
         if (BMWPlayerUtil.isMoving()) {
            float moveYaw = BMWPlayerUtil.getMovementDirectionOfInput(mc.player.getYaw(), BMWDirectionalInput.fromPlayer());
            this.preElytraYaw = mc.player.getYaw();
            this.preElytraPitch = mc.player.getPitch();
            BMWRotationManager.setRotationTarget(new BMWRotation(moveYaw, mc.player.getPitch()), true);
            this.elytraChangedLookThisTick = true;
         }
      }
   }

   private boolean canSprint() {
      if (mc.player == null) {
         return false;
      } else {
         boolean isHungry = mc.player.getHungerManager().getFoodLevel() <= 6 && !mc.player.isCreative();
         if (isHungry && !(Boolean)this.ignoreHunger.get()) {
            return false;
         } else if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !(Boolean)this.ignoreBlindness.get()) {
            return false;
         } else if (mc.player.horizontalCollision && !(Boolean)this.ignoreCollision.get()) {
            return false;
         } else if (!BMWPlayerUtil.isMoving()) {
            return false;
         } else if (mc.player.isSneaking()) {
            return false;
         } else {
            return mc.player.isRiding() ? false : !mc.player.isInFluid();
         }
      }
   }

   public static enum Mode {
      LEGIT("Legit"),
      OMNIDIRECTIONAL("Omnidirectional"),
      OMNIROTATIONAL("Omnirotational");

      private final String title;

      private Mode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
