package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FillESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgColors = this.settings.createGroup("Colors");
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("shape-mode")).description("How the shapes are rendered.")).defaultValue(ShapeMode.Both)).build());
   private final Setting<SettingColor> unopenedColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("unopened-color"))
               .description("Color for containers you haven't interacted with."))
            .defaultValue(new SettingColor(55, 135, 255, 100))
            .build()
      );
   private final Setting<SettingColor> openedColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("opened-color"))
               .description("Color for containers you have clicked (Filled)."))
            .defaultValue(new SettingColor(45, 215, 160, 100))
            .build()
      );
   private final Set<BlockPos> openedBlocks = new HashSet<>();

   public FillESP() {
      super(AddonTemplate.SC_CATEGORY, "FillESP", "Highlights containers and marks them green when clicked.");
   }

   public void onDeactivate() {
      this.openedBlocks.clear();
   }

   public WWidget getWidget(GuiTheme theme) {
      WButton clear = theme.button("Reset Opened Cache");
      clear.action = this.openedBlocks::clear;
      return clear;
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      BlockPos pos = event.result.getBlockPos();
      if (this.mc.world != null) {
         BlockEntity be = this.mc.world.getBlockEntity(pos);
         if (be != null) {
            if (this.isContainer(be)) {
               this.openedBlocks.add(pos);
               if (be instanceof ChestBlockEntity) {
                  BlockState state = this.mc.world.getBlockState(pos);
                  if (state.contains(ChestBlock.CHEST_TYPE)) {
                     ChestType type = (ChestType)state.get(ChestBlock.CHEST_TYPE);
                     if (type != ChestType.SINGLE) {
                        Direction facing = (Direction)state.get(ChestBlock.FACING);
                        BlockPos neighborPos = pos.offset(type == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
                        this.openedBlocks.add(neighborPos);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.world != null) {
         for (BlockEntity be : Utils.blockEntities()) {
            if (this.isContainer(be)) {
               BlockPos pos = be.getPos();
               Color renderSideColor;
               Color renderLineColor;
               if (this.openedBlocks.contains(pos)) {
                  renderSideColor = new Color((Color)this.openedColor.get()).a(((SettingColor)this.openedColor.get()).a);
                  renderLineColor = new Color((Color)this.openedColor.get()).a(255);
               } else {
                  renderSideColor = new Color((Color)this.unopenedColor.get()).a(((SettingColor)this.unopenedColor.get()).a);
                  renderLineColor = new Color((Color)this.unopenedColor.get()).a(255);
               }

               this.drawBox(event, be, renderSideColor, renderLineColor);
            }
         }
      }
   }

   private void drawBox(Render3DEvent event, BlockEntity be, Color side, Color line) {
      double x = be.getPos().getX();
      double y = be.getPos().getY();
      double z = be.getPos().getZ();
      double shrink = 0.0625;
      if (!(be instanceof ChestBlockEntity) && !(be instanceof EnderChestBlockEntity)) {
         event.renderer.box(x, y, z, x + 1.0, y + 1.0, z + 1.0, side, line, (ShapeMode)this.shapeMode.get(), 0);
      } else {
         event.renderer
            .box(x + shrink, y, z + shrink, x + 1.0 - shrink, y + 1.0 - shrink * 2.0, z + 1.0 - shrink, side, line, (ShapeMode)this.shapeMode.get(), 0);
      }
   }

   private boolean isContainer(BlockEntity be) {
      String type = be.getType().toString().toLowerCase();
      return be instanceof ChestBlockEntity
         || be instanceof BarrelBlockEntity
         || be instanceof ShulkerBoxBlockEntity
         || be instanceof EnderChestBlockEntity
         || be instanceof HopperBlockEntity
         || be instanceof DispenserBlockEntity
         || type.contains("furnace")
         || type.contains("chest");
   }
}
