package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoBonemeal extends Module {
   public static AutoBonemeal INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> radius = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Radius")).description("Scan radius, in blocks.")).defaultValue(4)).min(1).sliderRange(1, 8).build()
      );
   private final Setting<Boolean> skipMature = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("SkipMature"))
                  .description("Skip crops that are already fully grown."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> includeSaplings = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("IncludeSaplings"))
                  .description("Also bonemeal saplings (grows full trees)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Delay")).description("How often to apply bonemeal, in ticks.")).defaultValue(2))
            .min(0)
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("SwapBack"))
                  .description("Swap back to your previous item afterwards."))
               .defaultValue(true))
            .build()
      );
   private int timer = 0;

   public AutoBonemeal() {
      super(
         AddonTemplate.SC_CATEGORY, "AutoBonemeal", "Automatically bonemeals crops and saplings in range. Pairs with AutoHarvest for a fully automatic farm."
      );
      INSTANCE = this;
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
            FindItemResult boneMeal = InvUtils.find(AutoBonemeal::isBoneMeal);
            if (boneMeal.found()) {
               List<BlockPos> targets = this.findTargets();
               if (!targets.isEmpty()) {
                  BlockPos pos = targets.get(0);
                  Hand hand = boneMeal.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
                  if (!boneMeal.isOffhand()) {
                     InvUtils.swap(boneMeal.slot(), (Boolean)this.swapBack.get());
                  }

                  Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                  BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
                  this.mc.interactionManager.interactBlock(this.mc.player, hand, hit);
                  this.mc.player.swingHand(hand);
                  if ((Boolean)this.swapBack.get()) {
                     InvUtils.swapBack();
                  }

                  this.timer = (Integer)this.delay.get();
               }
            }
         }
      }
   }

   private List<BlockPos> findTargets() {
      List<BlockPos> out = new ArrayList<>();
      BlockPos origin = this.mc.player.getBlockPos();
      int r = (Integer)this.radius.get();

      for (int x = -r; x <= r; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = origin.add(x, y, z);
               if (this.isTarget(pos)) {
                  out.add(pos);
               }
            }
         }
      }

      out.sort(Comparator.comparingDouble(p -> this.mc.player.squaredDistanceTo(p.toCenterPos())));
      return out;
   }

   private boolean isTarget(BlockPos pos) {
      BlockState state = this.mc.world.getBlockState(pos);
      Block block = state.getBlock();
      if (!(block instanceof Fertilizable)) {
         return false;
      } else if (block instanceof CropBlock crop) {
         return !(Boolean)this.skipMature.get() || !crop.isMature(state);
      } else if (block instanceof SaplingBlock) {
         return (Boolean)this.includeSaplings.get();
      } else {
         return block == Blocks.SWEET_BERRY_BUSH ? true : (Boolean)this.includeSaplings.get();
      }
   }

   private static boolean isBoneMeal(ItemStack stack) {
      return stack.getItem() == Items.BONE_MEAL;
   }
}
