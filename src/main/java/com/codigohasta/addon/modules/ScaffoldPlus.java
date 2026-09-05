package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class ScaffoldPlus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> rotate = this.sgGeneral.add(((Builder)((Builder)new Builder().name("Rotate")).defaultValue(true)).build());
   private final Setting<Boolean> usingPause = this.sgGeneral.add(((Builder)((Builder)new Builder().name("UsingPause")).defaultValue(true)).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Shape Mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Line"))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Side"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );

   public ScaffoldPlus() {
      super(AddonTemplate.CATEGORY, "ScaffoldPlus", "Automatically places blocks to bridge.");
   }

   @EventHandler
   private void onRender3d(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.mc.player.isUsingItem() || !(Boolean)this.usingPause.get()) {
            ItemStack stack = this.mc.player.getInventory().getStack(((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot());
            BlockPos pos = this.mc.player.getBlockPos();
            boolean slabMode = BlockUtil.getBlock(pos) instanceof SlabBlock;

            for (Direction i : Direction.values()) {
               if (i != Direction.UP && i != Direction.DOWN && BlockUtil.getBlock(pos.offset(i)) instanceof SlabBlock) {
                  slabMode = true;
                  break;
               }
            }

            int block = slabMode ? InventoryUtil.findSlabBlock() : InventoryUtil.findBlock();
            if (slabMode) {
               if (stack.getItem() instanceof BlockItem blockItem
                  && blockItem.getBlock() instanceof SlabBlock
                  && !BlockUtil.shiftBlocks.contains(Block.getBlockFromItem(stack.getItem()))
                  && ((BlockItem)stack.getItem()).getBlock() != Blocks.COBWEB) {
                  block = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               }
            } else if (stack.getItem() instanceof BlockItem
               && !BlockUtil.shiftBlocks.contains(Block.getBlockFromItem(stack.getItem()))
               && ((BlockItem)stack.getItem()).getBlock() != Blocks.COBWEB) {
               block = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            }

            if (block != -1) {
               BlockPos placePos = this.mc.player.getBlockPos().down();
               if (!slabMode) {
                  if (BlockUtil.clientCanPlace(placePos, false)) {
                     int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                     if (BlockUtil.getPlaceSide(placePos, null) == null) {
                        double distance = 1000.0;
                        BlockPos bestPos = null;

                        for (Direction ix : Direction.values()) {
                           if (ix != Direction.UP
                              && BlockUtil.canPlace(placePos.offset(ix))
                              && (bestPos == null || this.mc.player.squaredDistanceTo(placePos.offset(ix).toCenterPos()) < distance)) {
                              bestPos = placePos.offset(ix);
                              distance = this.mc.player.squaredDistanceTo(placePos.offset(ix).toCenterPos());
                           }
                        }

                        if (bestPos == null) {
                           return;
                        }

                        placePos = bestPos;
                     }

                     Direction side = BlockUtil.getPlaceSide(placePos, null);
                     Direction slabSide = null;
                     if (this.mc.player.getInventory().getStack(block).getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() instanceof SlabBlock) {
                        slabSide = Direction.UP;
                     }

                     if (side != null) {
                        event.renderer.box(new Box(placePos), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
                        InventoryUtil.switchToSlot(block);
                        BlockUtil.placeSlabBlock(placePos, side, slabSide, (Boolean)this.rotate.get());
                        InventoryUtil.switchToSlot(old);
                     }
                  }
               } else {
                  placePos = pos;
                  if (BlockUtil.getBlock(pos) instanceof SlabBlock) {
                     Direction face = this.mc.player.getHorizontalFacing();
                     placePos = pos.offset(face);
                  } else if (BlockUtil.clientCanPlace(pos, false) && BlockUtil.getPlaceSide(pos, null) == null) {
                     double distance = 1000.0;
                     BlockPos bestPos = null;

                     for (Direction ixx : Direction.values()) {
                        if (ixx != Direction.UP
                           && BlockUtil.canPlace(placePos.offset(ixx))
                           && (bestPos == null || this.mc.player.squaredDistanceTo(placePos.offset(ixx).toCenterPos()) < distance)) {
                           bestPos = placePos.offset(ixx);
                           distance = this.mc.player.squaredDistanceTo(placePos.offset(ixx).toCenterPos());
                        }
                     }

                     if (bestPos == null) {
                        return;
                     }

                     placePos = bestPos;
                  }

                  int oldx = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                  Direction sidex = BlockUtil.getPlaceSide(placePos, null);
                  if (sidex != null && !(BlockUtil.getBlock(placePos) instanceof SlabBlock)) {
                     event.renderer.box(new Box(placePos), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
                     InventoryUtil.switchToSlot(block);
                     BlockUtil.placeSlabBlock(placePos, sidex, Direction.DOWN, (Boolean)this.rotate.get());
                     InventoryUtil.switchToSlot(oldx);
                  }
               }
            }
         }
      }
   }
}
