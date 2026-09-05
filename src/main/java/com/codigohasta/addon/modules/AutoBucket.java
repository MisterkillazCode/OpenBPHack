package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoBucket extends Module {
   public static AutoBucket INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> triggerHeight = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("TriggerHeight"))
               .description("How many blocks above the landing spot to start placing. Too high and you still take fall damage, too low and it is too late."))
            .defaultValue(3.0)
            .min(0.5)
            .sliderRange(0.5, 20.0)
            .build()
      );
   private final Setting<Double> minFall = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("MinFallDistance"))
               .description("Minimum fall distance before triggering, so it does not fire while just walking."))
            .defaultValue(3.0)
            .min(0.0)
            .sliderRange(0.0, 30.0)
            .build()
      );
   private final Setting<Integer> rotatePriority = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("RotatePriority"))
                  .description("Rotation priority; higher numbers take precedence."))
               .defaultValue(100))
            .min(0)
            .sliderRange(0, 200)
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("SwapBack"))
                  .description("Swap back to your previous item after placing."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreElytra = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("IgnoreElytra"))
                  .description("Do not trigger while gliding with an elytra."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreCreative = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("IgnoreCreative"))
                  .description("Do not trigger in creative mode."))
               .defaultValue(true))
            .build()
      );

   public AutoBucket() {
      super(AddonTemplate.SC_CATEGORY, "AutoBucket", "MLG water bucket: places water below you during a free fall just before impact. Self-hosted / LAN only.");
      INSTANCE = this;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.mc.player.isOnGround()) {
            if (!this.mc.player.isGliding() || !(Boolean)this.ignoreElytra.get()) {
               if (!this.mc.player.getAbilities().creativeMode || !(Boolean)this.ignoreCreative.get()) {
                  if (!(this.mc.player.fallDistance < (Double)this.minFall.get())) {
                     if (!(this.mc.player.getVelocity().y >= 0.0)) {
                        if (!this.mc.player.isTouchingWater() && !this.mc.player.isInLava()) {
                           Vec3d start = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
                           Vec3d end = start.add(0.0, -(Double)this.triggerHeight.get() - 1.0, 0.0);
                           HitResult hit = this.mc
                              .world
                              .raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.ANY, this.mc.player));
                           if (hit.getType() == Type.BLOCK) {
                              double distance = start.y - hit.getPos().y;
                              if (!(distance > (Double)this.triggerHeight.get())) {
                                 BlockPos below = BlockPos.ofFloored(hit.getPos()).up();
                                 if (this.mc.world.getFluidState(below).isEmpty()) {
                                    FindItemResult bucket = InvUtils.findInHotbar(new Item[]{Items.WATER_BUCKET});
                                    if (bucket.found()) {
                                       InvUtils.swap(bucket.slot(), (Boolean)this.swapBack.get());
                                       float yaw = this.mc.player.getYaw();
                                       Rotations.rotate(yaw, 90.0, (Integer)this.rotatePriority.get(), () -> {
                                          this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
                                          this.mc.player.swingHand(Hand.MAIN_HAND);
                                          if ((Boolean)this.swapBack.get()) {
                                             InvUtils.swapBack();
                                          }
                                       });
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
