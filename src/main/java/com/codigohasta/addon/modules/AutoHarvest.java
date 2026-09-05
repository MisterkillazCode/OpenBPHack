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
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class AutoHarvest extends Module {
   public static AutoHarvest INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Integer> radius = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Radius")).description("Scan radius, in blocks.")).defaultValue(4)).min(1).sliderRange(1, 8).build()
      );
   private final Setting<Boolean> replant = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Replant"))
                  .description("Automatically replant the same crop after harvesting."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Delay")).description("How often to process one block, in ticks.")).defaultValue(2))
            .min(0)
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Boolean> requireSeeds = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RequireSeeds"))
                  .description("Only harvest if you actually have seeds left, so you never leave a hole."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Render"))
                  .description("Highlight crops that are about to be harvested."))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("ShapeMode"))
                  .description("Render mode for the highlight box."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("SideColor"))
               .description("Fill colour."))
            .defaultValue(new SettingColor(120, 220, 80, 60))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("LineColor"))
               .description("Outline colour."))
            .defaultValue(new SettingColor(120, 220, 80, 220))
            .build()
      );
   private final List<BlockPos> targets = new ArrayList<>();
   private int timer = 0;

   public AutoHarvest() {
      super(
         AddonTemplate.SC_CATEGORY, "AutoHarvest", "Harvests mature crops and replants in place. Supports wheat, carrots, potatoes, beetroots and nether wart."
      );
      INSTANCE = this;
   }

   public void onActivate() {
      this.timer = 0;
      this.targets.clear();
   }

   public void onDeactivate() {
      this.targets.clear();
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.targets.clear();
         BlockPos origin = this.mc.player.getBlockPos();
         int r = (Integer)this.radius.get();

         for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
               for (int z = -r; z <= r; z++) {
                  BlockPos pos = origin.add(x, y, z);
                  if (this.isMature(pos)) {
                     this.targets.add(pos);
                  }
               }
            }
         }

         if (!this.targets.isEmpty()) {
            this.targets.sort(Comparator.comparingDouble(p -> this.mc.player.squaredDistanceTo(p.toCenterPos())));
            if (this.timer > 0) {
               this.timer--;
            } else {
               BlockPos pos = this.targets.get(0);
               BlockState state = this.mc.world.getBlockState(pos);
               Item seedItem = this.getSeedFor(state.getBlock());
               FindItemResult seed = seedItem == null ? null : InvUtils.find(new Item[]{seedItem});
               if (!(Boolean)this.replant.get() || !(Boolean)this.requireSeeds.get() || seed != null && seed.found()) {
                  BlockUtils.breakBlock(pos, false);
                  if ((Boolean)this.replant.get() && seed != null && seed.found()) {
                     BlockUtils.place(pos, seed, 0);
                  }

                  this.timer = (Integer)this.delay.get();
               }
            }
         }
      }
   }

   private boolean isMature(BlockPos pos) {
      BlockState state = this.mc.world.getBlockState(pos);
      Block block = state.getBlock();
      if (block instanceof CropBlock crop) {
         return crop.isMature(state);
      } else {
         return block == Blocks.NETHER_WART ? (Integer)state.get(NetherWartBlock.AGE) >= 3 : false;
      }
   }

   private Item getSeedFor(Block block) {
      if (block == Blocks.WHEAT) {
         return Items.WHEAT_SEEDS;
      } else if (block == Blocks.CARROTS) {
         return Items.CARROT;
      } else if (block == Blocks.POTATOES) {
         return Items.POTATO;
      } else if (block == Blocks.BEETROOTS) {
         return Items.BEETROOT_SEEDS;
      } else {
         return block == Blocks.NETHER_WART ? Items.NETHER_WART : null;
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.get() && !this.targets.isEmpty()) {
         for (BlockPos pos : this.targets) {
            event.renderer.box(pos, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
         }
      }
   }
}
