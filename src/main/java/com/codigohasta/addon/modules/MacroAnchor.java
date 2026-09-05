package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class MacroAnchor extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delaySetting = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("action-delay")).description("MoveDoofBetween'sTickDelay (pastGrim'sKey).")).defaultValue(2))
            .min(1)
            .sliderMax(10)
            .build()
      );
   private final Setting<Boolean> blockMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("block-anchor"))
                  .description("EnableMode(AutoPutShootLine)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> detonateSlot = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("detonate-slot")).description("mostafterTimestrongRowSwitch'sfastSlot (1-9)")).defaultValue(7))
            .min(1)
            .max(9)
            .sliderMin(1)
            .sliderMax(9)
            .build()
      );
   private MacroAnchor.State currentState = MacroAnchor.State.IDLE;
   private int timer = 0;
   private boolean isRotating = false;
   private BlockPos anchorPos = null;
   private BlockPos blockerPos = null;
   private int originalSlot = -1;

   public MacroAnchor() {
      super(AddonTemplate.CATEGORY, "MacroAnchor", "Automates anchor charging and detonation. Easily flagged; cannot bypass Grim.");
   }

   public void onDeactivate() {
      this.currentState = MacroAnchor.State.IDLE;
      this.isRotating = false;
   }

   private void setSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         if (((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot() != slot) {
            ((InventoryAccessor)this.mc.player.getInventory()).setSelectedSlot(slot);
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   }

   private int getSlot() {
      return ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (event.packet instanceof PlayerInteractBlockC2SPacket packet && this.currentState == MacroAnchor.State.IDLE) {
         if (this.mc.player.getStackInHand(packet.getHand()).getItem() != Items.RESPAWN_ANCHOR) {
            return;
         }

         BlockHitResult hitResult = packet.getBlockHitResult();
         BlockPos placedPos = hitResult.getBlockPos().offset(hitResult.getSide());
         BlockState clickedState = this.mc.world.getBlockState(hitResult.getBlockPos());
         if (clickedState.isReplaceable()) {
            placedPos = hitResult.getBlockPos();
         }

         this.startMacro(placedPos);
      }
   }

   private void startMacro(BlockPos pos) {
      this.anchorPos = pos;
      this.originalSlot = this.getSlot();
      this.timer = (Integer)this.delaySetting.get();
      this.isRotating = false;
      if ((Boolean)this.blockMode.get()) {
         this.blockerPos = this.calculateBlockerPos(this.anchorPos, this.mc.player.getEyePos());
         if (this.blockerPos != null && this.canPlaceBlocker(this.blockerPos)) {
            this.currentState = MacroAnchor.State.PLACE_BLOCKER;
         } else {
            this.currentState = MacroAnchor.State.CHARGE;
         }
      } else {
         this.currentState = MacroAnchor.State.CHARGE;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.currentState != MacroAnchor.State.IDLE) {
         if (!this.isRotating) {
            if (this.timer > 0) {
               this.timer--;
            } else {
               switch (this.currentState) {
                  case PLACE_BLOCKER:
                     FindItemResult glowstone = InvUtils.findInHotbar(new Item[]{Items.GLOWSTONE});
                     if (!glowstone.found()) {
                        this.abort();
                        return;
                     }

                     this.setSlot(glowstone.slot());
                     this.isRotating = true;
                     boolean canPlace = this.placeBlockLegit(this.blockerPos, this.anchorPos, () -> {
                        this.isRotating = false;
                        this.currentState = MacroAnchor.State.CHARGE;
                        this.timer = (Integer)this.delaySetting.get();
                     });
                     if (!canPlace) {
                        this.isRotating = false;
                        this.currentState = MacroAnchor.State.CHARGE;
                        this.timer = (Integer)this.delaySetting.get();
                     }
                     break;
                  case CHARGE:
                     FindItemResult glowstone = InvUtils.findInHotbar(new Item[]{Items.GLOWSTONE});
                     if (!glowstone.found()) {
                        this.abort();
                        return;
                     }

                     this.setSlot(glowstone.slot());
                     this.isRotating = true;
                     this.interactBlockLegit(this.anchorPos, () -> {
                        this.isRotating = false;
                        this.currentState = MacroAnchor.State.DETONATE;
                        this.timer = (Integer)this.delaySetting.get();
                     });
                     break;
                  case DETONATE:
                     int slot = (Integer)this.detonateSlot.get() - 1;
                     this.setSlot(slot);
                     this.isRotating = true;
                     this.interactBlockLegit(this.anchorPos, () -> {
                        this.isRotating = false;
                        this.currentState = MacroAnchor.State.SWITCH_BACK;
                        this.timer = (Integer)this.delaySetting.get();
                     });
                     break;
                  case SWITCH_BACK:
                     this.setSlot(this.originalSlot);
                     this.currentState = MacroAnchor.State.IDLE;
               }
            }
         }
      }
   }

   private BlockPos calculateBlockerPos(BlockPos anchor, Vec3d playerEye) {
      double centerX = anchor.getX() + 0.5;
      double centerZ = anchor.getZ() + 0.5;
      double dx = playerEye.x - centerX;
      double dz = playerEye.z - centerZ;
      if (Math.abs(dx) < 0.1 && Math.abs(dz) < 0.1) {
         return null;
      } else {
         double angle = Math.atan2(dz, dx);
         long sector = Math.round(angle / (Math.PI / 4));
         int offsetX = 0;
         int offsetZ = 0;
         if (sector == 0L) {
            offsetX = 1;
            offsetZ = 0;
         } else if (sector == 1L) {
            offsetX = 1;
            offsetZ = 1;
         } else if (sector == 2L) {
            offsetX = 0;
            offsetZ = 1;
         } else if (sector == 3L) {
            offsetX = -1;
            offsetZ = 1;
         } else if (sector == 4L || sector == -4L) {
            offsetX = -1;
            offsetZ = 0;
         } else if (sector == -3L) {
            offsetX = -1;
            offsetZ = -1;
         } else if (sector == -2L) {
            offsetX = 0;
            offsetZ = -1;
         } else if (sector == -1L) {
            offsetX = 1;
            offsetZ = -1;
         }

         return new BlockPos(anchor.getX() + offsetX, anchor.getY(), anchor.getZ() + offsetZ);
      }
   }

   private boolean canPlaceBlocker(BlockPos pos) {
      BlockState state = this.mc.world.getBlockState(pos);
      return !state.isReplaceable()
         ? false
         : this.mc.world.canPlace(Blocks.GLOWSTONE.getDefaultState(), pos, ShapeContext.of(this.mc.player));
   }

   private boolean placeBlockLegit(BlockPos targetPos, BlockPos excludePos, Runnable onDone) {
      Direction[] priorities = new Direction[]{
         Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
      };

      for (Direction side : priorities) {
         BlockPos neighborPos = targetPos.offset(side);
         if (!neighborPos.equals(excludePos)) {
            BlockState state = this.mc.world.getBlockState(neighborPos);
            boolean isSolid = state.isFullCube(this.mc.world, neighborPos);
            if (isSolid) {
               Direction hitSide = side.getOpposite();
               Vec3d hitVec = new Vec3d(
                  neighborPos.getX() + 0.5 + hitSide.getOffsetX() * 0.49,
                  neighborPos.getY() + 0.5 + hitSide.getOffsetY() * 0.49,
                  neighborPos.getZ() + 0.5 + hitSide.getOffsetZ() * 0.49
               );
               Rotations.rotate(Rotations.getYaw(hitVec), Rotations.getPitch(hitVec), 50, () -> {
                  BlockHitResult result = new BlockHitResult(hitVec, hitSide, neighborPos, false);
                  this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, result);
                  this.mc.player.swingHand(Hand.MAIN_HAND);
                  if (onDone != null) {
                     onDone.run();
                  }
               });
               return true;
            }
         }
      }

      return false;
   }

   private void interactBlockLegit(BlockPos pos, Runnable onDone) {
      Direction bestSide = null;
      double shortestDist = Double.MAX_VALUE;
      Vec3d eyePos = this.mc.player.getEyePos();

      for (Direction dir : Direction.values()) {
         Vec3d faceCenter = new Vec3d(
            pos.getX() + 0.5 + dir.getOffsetX() * 0.5,
            pos.getY() + 0.5 + dir.getOffsetY() * 0.5,
            pos.getZ() + 0.5 + dir.getOffsetZ() * 0.5
         );
         HitResult hit = this.mc
            .world
            .raycast(new RaycastContext(eyePos, faceCenter, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
         if (!(hit instanceof BlockHitResult blockHit && hit.getType() != Type.MISS && !blockHit.getBlockPos().equals(pos))) {
            double dist = eyePos.squaredDistanceTo(faceCenter);
            if (dist < shortestDist) {
               shortestDist = dist;
               bestSide = dir;
            }
         }
      }

      if (bestSide == null) {
         bestSide = Direction.UP;
      }

      Vec3d hitVec = new Vec3d(
         pos.getX() + 0.5 + bestSide.getOffsetX() * 0.49,
         pos.getY() + 0.5 + bestSide.getOffsetY() * 0.49,
         pos.getZ() + 0.5 + bestSide.getOffsetZ() * 0.49
      );
      Direction finalBestSide = bestSide;
      Rotations.rotate(Rotations.getYaw(hitVec), Rotations.getPitch(hitVec), 50, () -> {
         BlockHitResult result = new BlockHitResult(hitVec, finalBestSide, pos, false);
         this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, result);
         this.mc.player.swingHand(Hand.MAIN_HAND);
         if (onDone != null) {
            onDone.run();
         }
      });
   }

   private void abort() {
      this.currentState = MacroAnchor.State.IDLE;
      this.isRotating = false;
      if (this.originalSlot != -1) {
         this.setSlot(this.originalSlot);
      }
   }

   private static enum State {
      IDLE,
      PLACE_BLOCKER,
      CHARGE,
      DETONATE,
      SWITCH_BACK;
   }
}
