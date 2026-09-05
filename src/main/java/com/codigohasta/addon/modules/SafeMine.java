package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SafeMine extends Module {
   public static SafeMine INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> protectLava = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("ProtectLava")).description("Plug the block when lava is detected next to it.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> protectWater = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("ProtectWater")).description("Plug the block when water is detected next to it.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> cancelBreak = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("CancelBreak")).description("Cancel the current break first, plug it, then carry on."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> chatWarning = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("ChatWarning")).description("Warn in chat when a hazard is detected.")).defaultValue(true)).build());
   private final Setting<Integer> scanRadius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ScanRadius"))
                  .description("How many blocks around the target to check."))
               .defaultValue(1))
            .min(1)
            .sliderRange(1, 3)
            .build()
      );
   private long lastWarn = 0L;

   public SafeMine() {
      super(AddonTemplate.SC_CATEGORY, "SafeMine", "Mining safety: if a block you break is adjacent to lava or water, plugs it with a solid block first.");
      INSTANCE = this;
   }

   @EventHandler
   private void onStartBreaking(StartBreakingBlockEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.isActive()) {
            BlockPos target = event.blockPos;

            for (Direction dir : Direction.values()) {
               for (int i = 1; i <= this.scanRadius.get(); i++) {
                  BlockPos side = target.offset(dir, i);
                  BlockState state = this.mc.world.getBlockState(side);
                  boolean danger = (Boolean)this.protectLava.get() && state.getBlock() == Blocks.LAVA
                     || (Boolean)this.protectWater.get() && state.getBlock() == Blocks.WATER;
                  if (danger) {
                     if ((Boolean)this.chatWarning.get() && System.currentTimeMillis() - this.lastWarn > 2000L) {
                        ChatUtils.warning(
                           "SafeMine：%s 方向检测到%s，正在封堵。", new Object[]{dir.asString(), state.getBlock() == Blocks.LAVA ? "岩浆" : "水"}
                        );
                        this.lastWarn = System.currentTimeMillis();
                     }

                     if ((Boolean)this.cancelBreak.get()) {
                        event.cancel();
                     }

                     BlockPos fill = target.offset(dir);
                     if (this.mc.world.getBlockState(fill).isAir() || this.mc.world.getBlockState(fill).isLiquid()) {
                        FindItemResult block = InvUtils.findInHotbar(SafeMine::isSolidBlockItem);
                        if (block.found()) {
                           BlockUtils.place(fill, block, 100);
                        }
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   private static boolean isSolidBlockItem(ItemStack stack) {
      if (!(stack.getItem() instanceof BlockItem)) {
         return false;
      } else {
         Block block = ((BlockItem)stack.getItem()).getBlock();
         return block != Blocks.LAVA
            && block != Blocks.WATER
            && block != Blocks.AIR
            && block != Blocks.SAND
            && block != Blocks.RED_SAND
            && block != Blocks.GRAVEL
            && !(block instanceof FallingBlock);
      }
   }
}
