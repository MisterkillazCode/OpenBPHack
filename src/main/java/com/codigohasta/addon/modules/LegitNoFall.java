package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class LegitNoFall extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> checkDown = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("checkDown")).defaultValue(1)).min(0).sliderMax(3).build());
   private final Setting<Boolean> inventorySwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("inventorySwap"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> offSet = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("offSet"))
            .defaultValue(0.3)
            .min(0.0)
            .sliderMax(1.0)
            .build()
      );
   private boolean hasPlacedWater = false;
   private BlockPos lastPos = null;
   private float rotationYaw = 0.0F;
   private float rotationPitch = 0.0F;
   private int lastSlot = -1;
   private int lastSelect = -1;

   private int getSelectedSlot() {
      return this.mc.player == null ? 0 : this.mc.player.getInventory().getSelectedSlot();
   }

   private void setSelectedSlot(int slot) {
      if (this.mc.player != null) {
         this.mc.player.getInventory().setSelectedSlot(slot);
      }
   }

   public LegitNoFall() {
      super(AddonTemplate.CATEGORY, "LegitNoFall", "Legitimate no-fall for self-hosted use; effectiveness uncertain.");
   }

   public void onActivate() {
      this.hasPlacedWater = false;
      this.lastPos = null;
      this.lastSlot = -1;
      this.lastSelect = -1;
   }

   @EventHandler
   private void onRender3d(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.world.getRegistryKey() != World.NETHER) {
            this.rotationYaw = this.mc.player.getYaw();
            this.rotationPitch = this.mc.player.getPitch();
            int old = this.getSelectedSlot();
            int water = this.hasPlacedWater ? this.findItem(Items.BUCKET) : this.findItem(Items.WATER_BUCKET);
            if (water != -1) {
               if (this.hasPlacedWater && this.lastPos != null) {
                  this.doSwap(water);
                  Color color = new Color(70, 177, 229, 80);
                  event.renderer.box(this.lastPos, color, color, ShapeMode.Both, 0);
                  Vec3d targetAim = new Vec3d(this.lastPos.getX() + 0.5, this.lastPos.getY(), this.lastPos.getZ() + 0.5);
                  this.snapAt(targetAim);
                  this.mc.player.swingHand(Hand.MAIN_HAND);
                  float[] rot = this.getRotation(targetAim);
                  this.mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, rot[0], rot[1]));
                  if ((Boolean)this.inventorySwap.get()) {
                     this.doSwap(water);
                  } else {
                     this.doSwap(old);
                  }

                  this.snapBack();
                  this.hasPlacedWater = false;
               } else if (!this.hasPlacedWater) {
                  BlockPos pos = this.mc.player.getBlockPos().down((Integer)this.checkDown.get());
                  double[] xzOffset = new double[]{(Double)this.offSet.get(), -(Double)this.offSet.get()};

                  for (double x : xzOffset) {
                     for (double z : xzOffset) {
                        BlockPos offSetPos = new BlockPos(
                           MathHelper.floor(this.mc.player.getX() + x),
                           pos.getY(),
                           MathHelper.floor(this.mc.player.getZ() + z)
                        );
                        if (this.checkFalling() && !this.mc.world.isAir(offSetPos) && !this.mc.world.getBlockState(offSetPos).isReplaceable()) {
                           Direction side = this.getPlaceSide(pos.up(), null);
                           if (side != null && !this.behindWall(offSetPos.up())) {
                              Color color = new Color(70, 177, 229, 80);
                              event.renderer.box(offSetPos.up(), color, color, ShapeMode.Both, 0);
                              this.doSwap(water);
                              Vec3d targetAim = new Vec3d(offSetPos.getX() + 0.5, offSetPos.getY() + 1.0, offSetPos.getZ() + 0.5);
                              this.snapAt(targetAim);
                              this.lastPos = offSetPos.up();
                              this.mc.player.swingHand(Hand.MAIN_HAND);
                              float[] rot = this.getRotation(targetAim);
                              this.mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, rot[0], rot[1]));
                              if ((Boolean)this.inventorySwap.get()) {
                                 this.doSwap(water);
                              } else {
                                 this.doSwap(old);
                              }

                              this.snapBack();
                              this.hasPlacedWater = true;
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean behindWall(BlockPos pos) {
      Vec3d testVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
      HitResult result = this.mc
         .world
         .raycast(new RaycastContext(this.mc.player.getEyePos(), testVec, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
      return result != null && result.getType() != Type.MISS ? false : false;
   }

   private Direction getPlaceSide(BlockPos pos, Predicate<Direction> directionPredicate) {
      if (pos == null) {
         return null;
      } else {
         for (Direction i : Direction.values()) {
            if (directionPredicate == null || directionPredicate.test(i)) {
               BlockPos neighbor = pos.offset(i);
               if (!this.mc.world.getBlockState(neighbor).isAir() && !this.mc.world.getBlockState(neighbor).isReplaceable()) {
                  return i;
               }
            }
         }

         return null;
      }
   }

   private boolean checkFalling() {
      return this.mc.player.fallDistance > this.mc.player.getSafeFallDistance() && !this.mc.player.isOnGround() && !this.mc.player.isGliding();
   }

   private int findItem(Item item) {
      if ((Boolean)this.inventorySwap.get()) {
         for (int i = 0; i < 45; i++) {
            ItemStack stack = this.mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
               return i < 9 ? i + 36 : i;
            }
         }

         return -1;
      } else {
         for (int ix = 0; ix < 9; ix++) {
            ItemStack stack = this.mc.player.getInventory().getStack(ix);
            if (stack.getItem() == item) {
               return ix;
            }
         }

         return -1;
      }
   }

   private void doSwap(int slot) {
      if (!(Boolean)this.inventorySwap.get()) {
         this.switchToSlot(slot);
      } else {
         this.inventorySwap(slot, this.getSelectedSlot());
      }
   }

   private void switchToSlot(int slot) {
      this.setSelectedSlot(slot);
      this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
   }

   private void inventorySwap(int slot, int selectedSlot) {
      if (slot == this.lastSlot) {
         this.switchToSlot(this.lastSelect);
         this.lastSlot = -1;
         this.lastSelect = -1;
      } else if (slot - 36 != selectedSlot) {
         if (slot - 36 >= 0) {
            this.lastSlot = slot;
            this.lastSelect = selectedSlot;
            this.switchToSlot(slot - 36);
         } else {
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, this.mc.player);
         }
      }
   }

   private void snapAt(float yaw, float pitch) {
      this.mc
         .getNetworkHandler()
         .sendPacket(
            new Full(
               this.mc.player.getX(),
               this.mc.player.getY(),
               this.mc.player.getZ(),
               yaw,
               pitch,
               this.mc.player.isOnGround(),
               this.mc.player.horizontalCollision
            )
         );
   }

   private void snapBack() {
      this.mc
         .getNetworkHandler()
         .sendPacket(
            new Full(
               this.mc.player.getX(),
               this.mc.player.getY(),
               this.mc.player.getZ(),
               this.rotationYaw,
               this.rotationPitch,
               this.mc.player.isOnGround(),
               this.mc.player.horizontalCollision
            )
         );
   }

   private void snapAt(Vec3d directionVec) {
      float[] angle = this.getRotation(directionVec);
      this.snapAt(angle[0], angle[1]);
   }

   private float[] getRotation(Vec3d vec) {
      Vec3d eyesPos = this.mc.player.getEyePos();
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
   }
}
