package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoChorus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final Setting<Double> range = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("AttackRange")).description("mostbigAttack Range.")).defaultValue(50.0).min(10.0).max(100.0).build());
   private final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoSwitchGun"))
                  .description("AutoSwitchtoPackinside'sWeapon."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoAim"))
                  .description("AutoRotateViewAim."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ShootStrikeDelay"))
                  .description("ShootStrikeafter'sCooldownTime (Ticks)."))
               .defaultValue(2))
            .min(0)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Render Mode"))
               .defaultValue(ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Fill Color"))
            .defaultValue(new SettingColor(255, 0, 255, 40))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("LineColor"))
            .defaultValue(new SettingColor(255, 0, 255, 200))
            .build()
      );
   private BlockPos currentTarget;
   private int timer;

   public AutoChorus() {
      super(AddonTemplate.SC_CATEGORY, "AutoChorus", "Automatically uses chorus fruit.");
   }

   public void onActivate() {
      this.currentTarget = null;
      this.timer = 0;
   }

   public void onDeactivate() {
      this.mc.options.useKey.setPressed(false);
      if (this.mc.player != null) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.currentTarget = this.findTarget();
         if (this.currentTarget == null) {
            if (this.mc.player.isUsingItem() && this.mc.player.getActiveItem().getItem() instanceof BowItem) {
               this.mc.options.useKey.setPressed(false);
               this.mc.interactionManager.stopUsingItem(this.mc.player);
            }
         } else {
            FindItemResult weaponResult = this.findWeapon();
            if (weaponResult.found() || this.isValidWeapon(this.mc.player.getMainHandStack().getItem())) {
               if ((Boolean)this.autoSwitch.get() && !this.isValidWeapon(this.mc.player.getMainHandStack().getItem())) {
                  InvUtils.swap(weaponResult.slot(), true);
               }

               Item handItem = this.mc.player.getMainHandStack().getItem();
               if (this.isValidWeapon(handItem)) {
                  Vec3d targetPos = new Vec3d(
                     this.currentTarget.getX() + 0.5, this.currentTarget.getY() + 0.5, this.currentTarget.getZ() + 0.5
                  );
                  double distToTarget = Math.sqrt(this.mc.player.squaredDistanceTo(targetPos));
                  float[] rotations = this.solveBallistic(targetPos, handItem, distToTarget);
                  if (rotations != null) {
                     if ((Boolean)this.rotate.get()) {
                        int priority = handItem instanceof BowItem ? 100 : 50;
                        Rotations.rotate(rotations[0], rotations[1], priority, () -> this.shoot(handItem, distToTarget));
                     } else {
                        this.shoot(handItem, distToTarget);
                     }
                  }
               }
            }
         }
      }
   }

   private void shoot(Item item, double distance) {
      if (this.timer > 0) {
         this.timer--;
      } else {
         if (item instanceof BowItem) {
            if (!this.mc.player.isUsingItem()) {
               this.mc.options.useKey.setPressed(true);
               this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
               return;
            }

            int useTicks = this.mc.player.getItemUseTime();
            int targetChargeTicks = this.getOptimalBowCharge(distance);
            if (useTicks >= targetChargeTicks) {
               this.mc.interactionManager.stopUsingItem(this.mc.player);
               this.mc.options.useKey.setPressed(false);
               this.timer = (Integer)this.delay.get();
            }
         } else if (item instanceof CrossbowItem) {
            boolean isCharged = CrossbowItem.isCharged(this.mc.player.getMainHandStack());
            if (isCharged) {
               this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
               this.mc.player.swingHand(Hand.MAIN_HAND);
               this.timer = (Integer)this.delay.get();
            } else {
               this.mc.options.useKey.setPressed(true);
               if (!this.mc.player.isUsingItem()) {
                  this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
               }
            }
         } else {
            this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            this.mc.player.swingHand(Hand.MAIN_HAND);
            this.timer = (Integer)this.delay.get();
         }
      }
   }

   private int getOptimalBowCharge(double distance) {
      return distance < 10.0 ? 12 : 20;
   }

   private float getBowVelocity(int chargeTicks) {
      float f = chargeTicks / 20.0F;
      f = (f * f + f * 2.0F) / 3.0F;
      if (f > 1.0F) {
         f = 1.0F;
      }

      return f * 3.0F;
   }

   private float[] solveBallistic(Vec3d target, Item weapon, double distance) {
      Vec3d playerPos = this.mc.player.getEyePos();
      double dx = target.x - playerPos.x;
      double dy = target.y - playerPos.y;
      double dz = target.z - playerPos.z;
      double distH = Math.sqrt(dx * dx + dz * dz);
      double v = 1.0;
      double g = 0.05;
      if (weapon instanceof BowItem) {
         int intendedTicks = this.getOptimalBowCharge(distance);
         v = this.getBowVelocity(intendedTicks);
         g = 0.05;
         dy += 0.25;
      } else if (weapon instanceof CrossbowItem) {
         v = 3.15;
         g = 0.05;
         dy += 0.25;
      } else if (weapon == Items.SNOWBALL || weapon == Items.EGG) {
         v = 1.5;
         g = 0.03;
      } else if (weapon == Items.TRIDENT) {
         v = 2.5;
         g = 0.05;
      } else if (weapon == Items.WIND_CHARGE) {
         v = 1.6;
         g = 0.01;
      }

      double v2 = v * v;
      double v4 = v2 * v2;
      double x2 = distH * distH;
      double root = v4 - g * (g * x2 + 2.0 * dy * v2);
      if (root < 0.0) {
         return null;
      } else {
         double angleRad = Math.atan2(v2 - Math.sqrt(root), g * distH);
         float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(angleRad));
         return new float[]{yaw, pitch};
      }
   }

   private BlockPos findTarget() {
      BlockPos pPos = this.mc.player.getBlockPos();
      int r = (int)Math.ceil((Double)this.range.get());
      List<BlockPos> candidates = new ArrayList<>();

      for (int x = -r; x <= r; x++) {
         for (int z = -r; z <= r; z++) {
            for (int y = -10; y <= r; y++) {
               BlockPos pos = pPos.add(x, y, z);
               if (!(pos.getSquaredDistance(pPos) > r * r) && this.mc.world.getBlockState(pos).getBlock() == Blocks.CHORUS_FLOWER && this.canSee(pos)) {
                  candidates.add(pos);
               }
            }
         }
      }

      if (candidates.isEmpty()) {
         return null;
      } else {
         candidates.sort(Comparator.comparingDouble(p -> this.mc.player.squaredDistanceTo(p.toCenterPos())));
         return candidates.get(0);
      }
   }

   private boolean canSee(BlockPos pos) {
      Vec3d start = this.mc.player.getEyePos();
      Vec3d end = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      RaycastContext context = new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player);
      return this.mc.world.raycast(context).getType() == Type.MISS || this.mc.world.raycast(context).getBlockPos().equals(pos);
   }

   private FindItemResult findWeapon() {
      return InvUtils.find(item -> this.isValidWeapon(item.getItem()));
   }

   private boolean isValidWeapon(Item item) {
      return item == Items.SNOWBALL
         || item == Items.EGG
         || item == Items.WIND_CHARGE
         || item == Items.TRIDENT
         || item instanceof BowItem
         || item instanceof CrossbowItem;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.currentTarget != null) {
         event.renderer.box(this.currentTarget, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
      }
   }
}
