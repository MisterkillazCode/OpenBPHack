package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import com.codigohasta.addon.utils.leaveshack.events.MoveEvent;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TargetBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;

public class PrinterLeaves extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgShift = this.settings.createGroup("IgnoreSneak");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final SettingGroup sgWhitelist = this.settings.createGroup("Whitelist");
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Rotate")).description("Head")).defaultValue(true)).build());
   private final Setting<Integer> printingRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("PrintingRange"))
                  .description("HitDistance"))
               .defaultValue(4))
            .min(1)
            .sliderMax(6)
            .build()
      );
   private final Setting<Boolean> inventorySwap = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("InventorySwap")).description("PackHand")).defaultValue(true)).build());
   private final Setting<Boolean> safeWalk = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("SafeWalk")).description("Walk")).defaultValue(true)).build());
   private final Setting<Boolean> ignoreSneak = this.sgShift
      .add(((Builder)((Builder)((Builder)new Builder().name("IgnoreSneak")).description("Sneak")).defaultValue(true)).build());
   private final Setting<Integer> shiftTime = this.sgShift
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ShiftTime"))
                  .description("SneakTime"))
               .defaultValue(100))
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Integer> sneakSpeed = this.sgShift
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("SneakSpeed"))
                  .description("SneakSpeed(currentlycomelookBilinotMovearemostgood'sSelect)"))
               .defaultValue(0))
            .min(0)
            .sliderMax(20)
            .build()
      );
   private final Setting<PrinterLeaves.ListMode> listMode = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("ListMode"))
                  .description("SelectMode"))
               .defaultValue(PrinterLeaves.ListMode.Blacklist))
            .build()
      );
   private final Setting<List<Block>> blacklist = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
                     .name("BlackList"))
                  .description("BlackNameSingle"))
               .visible(() -> this.listMode.get() == PrinterLeaves.ListMode.Blacklist))
            .build()
      );
   private final Setting<List<Block>> whitelist = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
                     .name("WhiteList"))
                  .description("WhiteNameSingle"))
               .visible(() -> this.listMode.get() == PrinterLeaves.ListMode.Whitelist))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("ShapeMode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("LineColor"))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("SideColor"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );
   private final Setting<Boolean> debug = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("DeBug")).description("DevUsecomeTest's, iqlow'snotwantOpen")).defaultValue(false)).build());
   boolean hasSneak = false;
   private Timer shiftTimer = new Timer();

   public PrinterLeaves() {
      super(AddonTemplate.CATEGORY, "PrinterLeaves", "Automatically places blocks (printer).");
   }

   public void onActivate() {
      this.hasSneak = false;
      this.shiftTimer.setMs(99999L);
   }

   public void onDeactivate() {
      if (this.hasSneak) {
         this.sendSneakPacket(false);
         this.hasSneak = false;
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
         if (schematic != null) {
            if (this.shiftTimer.passedMs((long)((Integer)this.shiftTime.get()).intValue()) || !this.hasSneak || !(Boolean)this.ignoreSneak.get()) {
               List<BlockPos> sphere = BlockUtil.getSphere(((Integer)this.printingRange.get()).intValue());
               int placed = 0;

               for (BlockPos pos : sphere) {
                  BlockState required = schematic.getBlockState(pos);
                  if ((this.listMode.get() != PrinterLeaves.ListMode.Blacklist || !((List)this.blacklist.get()).contains(required.getBlock()))
                     && (this.listMode.get() != PrinterLeaves.ListMode.Whitelist || ((List)this.whitelist.get()).contains(required.getBlock()))
                     && !required.isAir()
                     && !required.isLiquid()
                     && (this.mc.world.isAir(pos) || BlockUtil.canReplace(pos))
                     && !BlockUtil.hasEntity(pos, false)) {
                     if (placed >= 1) {
                        if ((Boolean)this.debug.get()) {
                           this.mc.player.sendMessage(Text.of("AlreadypastmostbigAmount, currentplaced:" + placed), false);
                        }

                        return;
                     }

                     int slot = this.inventorySwap.get()
                        ? InventoryUtil.findBlockInventory(required.getBlock())
                        : InventoryUtil.findBlock(required.getBlock());
                     if (slot != -1) {
                        int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                        ArrayList<Direction> sides = BlockUtil.getPlaceSides(pos, null, (Boolean)this.ignoreSneak.get());
                        if (!sides.isEmpty()) {
                           event.renderer.box(new Box(pos), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
                           Direction target = sides.getFirst();
                           Direction facing = getBlockFacing(required);
                           if (facing != null && !isRedstoneComponent(required)) {
                              if ((Boolean)this.debug.get()) {
                                 this.mc.player.sendMessage(Text.of("BlockPackDirection"), false);
                              }

                              boolean find = false;

                              for (Direction i : sides) {
                                 if ((Boolean)this.debug.get()) {
                                    this.mc.player.sendMessage(Text.of("sideList:" + i), false);
                                 }

                                 if (this.checkState(pos.offset(i), required, i.getOpposite())) {
                                    find = true;
                                    target = i;
                                 }
                              }

                              if (!find) {
                                 if ((Boolean)this.debug.get()) {
                                    this.mc.player.sendMessage(Text.of("toTargetDirection"), false);
                                 }
                                 continue;
                              }
                           }

                           if (!(required.getBlock() instanceof RedstoneWireBlock)
                              || !this.mc.world.isAir(pos.down()) && !this.mc.world.getBlockState(pos.down()).isReplaceable()) {
                              if (BlockUtil.needSneak(BlockUtil.getBlock(pos.offset(target))) && !this.hasSneak) {
                                 this.sendSneakPacket(true);
                                 this.hasSneak = true;
                                 this.mc.player.setSneaking(true);
                                 this.shiftTimer.reset();
                                 return;
                              }

                              placed++;
                              this.doSwap(slot);
                              if ((Boolean)this.rotate.get()) {
                                 Vec3d directionVec = new Vec3d(
                                    pos.getX() + 0.5 + target.getVector().getX() * 0.5,
                                    pos.getY() + 0.5 + target.getVector().getY() * 0.5,
                                    pos.getZ() + 0.5 + target.getVector().getZ() * 0.5
                                 );
                                 Rotation.snapAt(directionVec);
                              }

                              if (facing != null && isRedstoneComponent(required)) {
                                 if (required.getBlock() instanceof ObserverBlock) {
                                    this.blockFacing(facing);
                                 } else {
                                    this.blockFacing(facing.getOpposite());
                                 }
                              }

                              SlabType type = getSlabType(required);
                              if (type != null) {
                                 switch (type) {
                                    case TOP:
                                       if (!(BlockUtil.getBlock(pos) instanceof SlabBlock)) {
                                          BlockUtil.placeSlabBlock(pos, target, Direction.UP, false);
                                       }
                                       break;
                                    case BOTTOM:
                                       if (!(BlockUtil.getBlock(pos) instanceof SlabBlock)) {
                                          BlockUtil.placeSlabBlock(pos, target, Direction.DOWN, false);
                                       }
                                 }
                              } else {
                                 BlockUtil.placeBlock(pos, target, false);
                              }

                              if (this.hasSneak && (Boolean)this.ignoreSneak.get()) {
                                 this.sendSneakPacket(false);
                                 this.mc.player.setSneaking(false);
                                 this.hasSneak = false;
                              }

                              Rotation.snapBack();
                              event.renderer.box(new Box(pos), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
                              if ((Boolean)this.inventorySwap.get()) {
                                 this.doSwap(slot);
                              } else {
                                 this.doSwap(old);
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

   @EventHandler(
      priority = -100
   )
   public void onMove1(MoveEvent event) {
      if ((Boolean)this.safeWalk.get()) {
         double x = event.getX();
         double y = event.getY();
         double z = event.getZ();
         if (this.mc.player.isOnGround()) {
            double increment = 0.05;

            while (x != 0.0 && this.isOffsetBBEmpty(x, -1.0, 0.0)) {
               if (x < increment && x >= -increment) {
                  x = 0.0;
               } else if (x > 0.0) {
                  x -= increment;
               } else {
                  x += increment;
               }
            }

            while (z != 0.0 && this.isOffsetBBEmpty(0.0, -1.0, z)) {
               if (z < increment && z >= -increment) {
                  z = 0.0;
               } else if (z > 0.0) {
                  z -= increment;
               } else {
                  z += increment;
               }
            }

            while (x != 0.0 && z != 0.0 && this.isOffsetBBEmpty(x, -1.0, z)) {
               x = x < increment && x >= -increment ? 0.0 : (x > 0.0 ? x - increment : x + increment);
               if (z < increment && z >= -increment) {
                  z = 0.0;
               } else if (z > 0.0) {
                  z -= increment;
               } else {
                  z += increment;
               }
            }
         }

         event.setX(x);
         event.setY(y);
         event.setZ(z);
      }
   }

   public boolean isOffsetBBEmpty(double offsetX, double offsetY, double offsetZ) {
      return !this.mc.world.canCollide(this.mc.player, this.mc.player.getBoundingBox().offset(offsetX, offsetY, offsetZ));
   }

   @EventHandler
   public void onMove2(MoveEvent event) {
      if (this.shiftTimer.passedMs((long)((Integer)this.shiftTime.get() * 2)) && (Boolean)this.ignoreSneak.get() && this.hasSneak) {
         this.sendSneakPacket(false);
         this.hasSneak = false;
      } else if (this.hasSneak) {
         double speed = ((Integer)this.sneakSpeed.get()).intValue();
         double moveSpeed = 0.002873 * speed;
         double n = (this.mc.player.input.playerInput.forward() ? 1.0F : 0.0F)
            - (this.mc.player.input.playerInput.backward() ? 1.0F : 0.0F);
         double n2 = (this.mc.player.input.playerInput.left() ? 1.0F : 0.0F)
            - (this.mc.player.input.playerInput.right() ? 1.0F : 0.0F);
         double n3 = this.mc.player.getYaw();
         if (n == 0.0 && n2 == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
         } else {
            if (n != 0.0 && n2 != 0.0) {
               n *= Math.sin(Math.PI / 4);
               n2 *= Math.cos(Math.PI / 4);
            }

            event.setX(n * moveSpeed * -Math.sin(Math.toRadians(n3)) + n2 * moveSpeed * Math.cos(Math.toRadians(n3)));
            event.setZ(n * moveSpeed * Math.cos(Math.toRadians(n3)) - n2 * moveSpeed * -Math.sin(Math.toRadians(n3)));
         }
      }
   }

   public static SlabType getSlabType(BlockState state) {
      return state.getBlock() instanceof SlabBlock ? (SlabType)state.get(SlabBlock.TYPE) : null;
   }

   public void blockFacing(Direction i) {
      if (i == Direction.EAST) {
         Rotation.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Rotation.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Rotation.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Rotation.snapAt(0.0F, 5.0F);
      } else if (i == Direction.UP) {
         Rotation.snapAt(5.0F, -90.0F);
      } else if (i == Direction.DOWN) {
         Rotation.snapAt(5.0F, 90.0F);
      }
   }

   public static boolean isRedstoneComponent(BlockState state) {
      Block block = state.getBlock();
      return block instanceof RedstoneWireBlock
         || block instanceof AbstractRedstoneGateBlock
         || block instanceof PressurePlateBlock
         || block instanceof ObserverBlock
         || block instanceof TargetBlock
         || block instanceof TripwireHookBlock
         || block instanceof DaylightDetectorBlock
         || block instanceof PistonBlock
         || block instanceof RedstoneLampBlock
         || block instanceof FurnaceBlock;
   }

   public boolean checkState(BlockPos pos, BlockState targetState, Direction i) {
      Vec3d directionVec = new Vec3d(
         pos.getX() + 0.5 + i.getVector().getX() * 0.5,
         pos.getY() + 0.5 + i.getVector().getY() * 0.5,
         pos.getZ() + 0.5 + i.getVector().getZ() * 0.5
      );
      BlockHitResult hit = new BlockHitResult(directionVec, i, pos, false);
      ItemPlacementContext ctx = new ItemPlacementContext(this.mc.player, Hand.MAIN_HAND, this.mc.player.getMainHandStack(), hit);
      BlockState result = targetState.getBlock().getPlacementState(ctx);
      if (result != null && this.isSameFacing(result, targetState)) {
         return true;
      } else {
         if (result == null && (Boolean)this.debug.get()) {
            this.mc.player.sendMessage(Text.of("result: null"), false);
         }

         return false;
      }
   }

   public static Direction getBlockFacing(BlockState state) {
      if (state.getBlock() instanceof HopperBlock) {
         return (Direction)state.get(HopperBlock.FACING);
      } else if (state.contains(Properties.HORIZONTAL_FACING)) {
         return (Direction)state.get(Properties.HORIZONTAL_FACING);
      } else if (state.contains(Properties.FACING)) {
         return (Direction)state.get(Properties.FACING);
      } else {
         if (state.contains(Properties.AXIS)) {
            switch ((Axis)state.get(Properties.AXIS)) {
               case X:
                  return Direction.EAST;
               case Y:
                  return Direction.UP;
               case Z:
                  return Direction.SOUTH;
            }
         }

         return null;
      }
   }

   private boolean isSameFacing(BlockState a, BlockState b) {
      if (a.getBlock() != b.getBlock()) {
         return false;
      } else {
         Direction fa = getBlockFacing(a);
         Direction fb = getBlockFacing(b);
         if ((Boolean)this.debug.get()) {
            this.mc.player.sendMessage(Text.of("fa: " + fa + " fb: " + fb), false);
         }

         return fa != null && fb != null ? fa == fb : true;
      }
   }

   private void sendSneakPacket(boolean sneaking) {
      PlayerInput current = this.mc.player.input.playerInput;
      this.mc
         .getNetworkHandler()
         .sendPacket(
            new PlayerInputC2SPacket(
               new PlayerInput(
                  current.forward(), current.backward(), current.left(), current.right(), current.jump(), sneaking, current.sprint()
               )
            )
         );
   }

   private void doSwap(int slot) {
      if (slot != -1) {
         if (!(Boolean)this.inventorySwap.get()) {
            InventoryUtil.switchToSlot(slot);
         } else {
            InventoryUtil.inventorySwap(slot, ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot());
         }
      }
   }

   public static enum ListMode {
      Whitelist,
      Blacklist;
   }
}
