package com.codigohasta.addon.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ArmorStandItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AirPlace extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRange = this.settings.createGroup("Range");
   private final Setting<AirPlace.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("AirPlace mode. Meteor = original behavior; Grim variants = anti-cheat bypass."))
               .defaultValue(AirPlace.Mode.Meteor))
            .build()
      );
   private final Setting<Boolean> render = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render"))
                  .description("Renders a block overlay where the block will be placed."))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("shape-mode")).description("How the shapes are rendered.")).defaultValue(ShapeMode.Both)).build());
   private final Setting<SettingColor> sideColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("side-color"))
               .description("The color of the sides of the blocks being rendered."))
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("line-color"))
               .description("The color of the lines of the blocks being rendered."))
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
      );
   private final Setting<Boolean> customRange = this.sgRange
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("custom-range"))
                  .description("Use custom range for air place."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Double> range = this.sgRange
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("range"))
                  .description("Custom range to place at."))
               .visible(this.customRange::get))
            .defaultValue(5.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Integer> grimDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("grim-delay"))
                     .description("Ticks to cancel inbound S2C before flushing (Grim modes)."))
                  .defaultValue(2))
               .min(0)
               .sliderMax(10)
               .visible(() -> this.mode.get() != AirPlace.Mode.Meteor))
            .build()
      );
   private HitResult hitResult;
   private int tickCounter;
   private boolean grimCanceling;
   private int grimRemaining;

   public AirPlace() {
      super(Categories.Player, "air-place", "Places a block where your crosshair is pointing at.");
   }

   public void onActivate() {
      this.tickCounter = 0;
      this.grimCanceling = false;
      this.grimRemaining = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (InvUtils.testInHands(this::placeable)) {
         if (this.mc.crosshairTarget == null || this.mc.crosshairTarget.getType() == Type.MISS) {
            double r = this.customRange.get() ? (Double)this.range.get() : this.mc.player.getBlockInteractionRange();
            this.hitResult = this.mc.getCameraEntity().raycast(r, 0.0F, false);
         }
      }
   }

   @EventHandler
   private void onInteractItem(InteractItemEvent event) {
      if (this.hitResult instanceof BlockHitResult bhr && this.placeable(this.mc.player.getStackInHand(event.hand))) {
         Block toPlace = Blocks.OBSIDIAN;
         Item i = this.mc.player.getStackInHand(event.hand).getItem();
         if (i instanceof BlockItem blockItem) {
            toPlace = blockItem.getBlock();
         }

         if (BlockUtils.canPlaceBlock(bhr.getBlockPos(), i instanceof ArmorStandItem || i instanceof BlockItem, toPlace)) {
            if (this.mode.get() == AirPlace.Mode.Meteor) {
               Vec3d hitPos = Vec3d.ofCenter(bhr.getBlockPos());
               BlockHitResult b = new BlockHitResult(hitPos, this.mc.player.getMovementDirection().getOpposite(), bhr.getBlockPos(), false);
               BlockUtils.interact(b, event.hand, true);
               event.toReturn = ActionResult.SUCCESS;
            } else {
               double r = this.customRange.get() ? (Double)this.range.get() : this.mc.player.getBlockInteractionRange();
               BlockHitResult miss = (BlockHitResult)this.mc.getCameraEntity().raycast(r, 0.0F, false);
               if (miss != null && miss.getType() == Type.MISS) {
                  BlockPos exit = miss.getBlockPos();
                  Direction side = miss.getSide();
                  BlockPos placePos = exit.offset(side);
                  if (this.mc.world.getBlockState(placePos).isReplaceable()) {
                     BlockHitResult placeHit = new BlockHitResult(exit.toCenterPos().offset(side, 0.5), side, exit, false);
                     if (this.mode.get() == AirPlace.Mode.GrimFastGhostBlockWall) {
                        this.grimCanceling = true;
                        this.grimRemaining = (Integer)this.grimDelay.get() + 1;
                     } else {
                        this.grimCanceling = true;
                        this.grimRemaining = (Integer)this.grimDelay.get();
                     }

                     this.mc.interactionManager.sendSequencedPacket(this.mc.world, seq -> new PlayerInteractBlockC2SPacket(event.hand, placeHit, seq));
                     this.mc.player.swingHand(event.hand);
                     event.toReturn = ActionResult.SUCCESS;
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (this.grimCanceling) {
         if (!(event.packet instanceof ScreenHandlerSlotUpdateS2CPacket)) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.hitResult instanceof BlockHitResult bhr
         && (this.mc.crosshairTarget == null || this.mc.crosshairTarget.getType() == Type.MISS)
         && this.mc.world.getBlockState(bhr.getBlockPos()).isReplaceable()
         && InvUtils.testInHands(this::placeable)
         && (Boolean)this.render.get()) {
         event.renderer.box(bhr.getBlockPos(), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
      }
   }

   private boolean placeable(ItemStack stack) {
      Item i = stack.getItem();
      return i instanceof BlockItem || i instanceof SpawnEggItem || i instanceof FireworkRocketItem || i instanceof ArmorStandItem;
   }

   public static enum Mode {
      Meteor,
      GrimGhostBlockWall,
      GrimFastGhostBlockWall;
   }
}
