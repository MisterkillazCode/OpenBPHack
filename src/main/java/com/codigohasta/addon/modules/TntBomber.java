package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TntBomber extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("delay")).description("PutTNT'sBetween (ticks).")).defaultValue(4)).min(0).sliderMax(20).build());
   private final Setting<Boolean> onlyWhenFlying = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("only-when-flying"))
                  .description("atUseFlyTime."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> requireRightClick = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("require-right-click"))
                  .description("needHoldRightKeythencan."))
               .defaultValue(true))
            .build()
      );
   private int timer;

   public TntBomber() {
      super(AddonTemplate.CATEGORY, "TntBomber", "Drops TNT below you while flying, on a delay. Will kill you.");
   }

   public void onActivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.timer > 0) {
            this.timer--;
         } else {
            boolean shouldActivate = !(Boolean)this.requireRightClick.get() || this.mc.options.useKey.isPressed();
            boolean isFlying = this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && !this.mc.player.isOnGround();
            if (shouldActivate && (!(Boolean)this.onlyWhenFlying.get() || isFlying)) {
               FindItemResult tnt = InvUtils.findInHotbar(new Item[]{Items.TNT});
               FindItemResult flint = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof FlintAndSteelItem);
               FindItemResult fireCharge = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof FireChargeItem);
               if (tnt.found() && (flint.found() || fireCharge.found())) {
                  Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
                  Vec3d playerVelocity = this.mc.player.getVelocity();
                  Vec3d behindPos = playerPos.subtract(playerVelocity.multiply(2.5));
                  BlockPos placePos = BlockPos.ofFloored(behindPos.x, playerPos.y - 1.0, behindPos.z);
                  if (BlockUtils.canPlace(placePos)) {
                     int prevSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                     Rotations.rotate(Rotations.getYaw(placePos), Rotations.getPitch(placePos), () -> {
                        if (BlockUtils.place(placePos, tnt, false, 0)) {
                           FindItemResult igniter = flint.found() ? flint : fireCharge;
                           BlockHitResult hitResult = new BlockHitResult(placePos.toCenterPos(), Direction.UP, placePos, false);
                           InvUtils.swap(igniter.slot(), false);
                           this.mc.interactionManager.interactBlock(this.mc.player, igniter.getHand(), hitResult);
                           InvUtils.swap(prevSlot, false);
                           this.timer = (Integer)this.delay.get();
                        }
                     });
                  }
               }
            }
         }
      }
   }
}
