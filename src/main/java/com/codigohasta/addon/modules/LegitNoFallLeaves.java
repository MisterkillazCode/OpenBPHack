package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.BlockPosX;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class LegitNoFallLeaves extends Module {
   public static LegitNoFallLeaves INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> checkDown = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("checkDown")).description("CheckDistance")).defaultValue(1)).min(0).sliderMax(3).build());
   private final Setting<Boolean> inventorySwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("inventorySwap"))
                  .description("PackHand"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> offSet = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("offSet"))
               .description("MoveBitMove"))
            .defaultValue(0.3)
            .min(0.0)
            .sliderMax(1.0)
            .build()
      );
   private boolean hasPlacedWater = false;
   private BlockPos lastPos = null;

   public LegitNoFallLeaves() {
      super(AddonTemplate.CATEGORY, "LegitNoFallLeaves", "Leaf variant of legit no fall.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.hasPlacedWater = false;
   }

   @EventHandler
   private void onRender3d(Render3DEvent event) {
      if (this.mc.world.getRegistryKey() != World.NETHER) {
         int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
         int water = this.hasPlacedWater ? this.findItem(Items.BUCKET) : this.findItem(Items.WATER_BUCKET);
         if (water != -1) {
            if (this.hasPlacedWater && this.lastPos != null) {
               Direction clickSide = BlockUtil.getClickSide(this.lastPos);
               if (clickSide != null) {
                  Vec3d directionVec = new Vec3d(
                     this.lastPos.getX() + 0.5 + clickSide.getVector().getX() * 0.5,
                     this.lastPos.getY() + 0.5 + clickSide.getVector().getY() * 0.5,
                     this.lastPos.getZ() + 0.5 + clickSide.getVector().getZ() * 0.5
                  );
                  this.doSwap(water);
                  Color color = new Color(70, 177, 229, 80);
                  event.renderer.box(this.lastPos, color, color, ShapeMode.Both, 0);
                  Rotation.snapAt(directionVec);
                  this.mc.player.swingHand(Hand.MAIN_HAND);
                  this.mc
                     .getNetworkHandler()
                     .sendPacket(
                        new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, Rotation.getRotation(directionVec)[0], Rotation.getRotation(directionVec)[1])
                     );
                  if ((Boolean)this.inventorySwap.get()) {
                     this.doSwap(water);
                  } else {
                     this.doSwap(old);
                  }

                  Rotation.snapBack();
                  this.hasPlacedWater = false;
               }
            } else if (!this.hasPlacedWater) {
               BlockPos pos = this.mc.player.getBlockPos().down((Integer)this.checkDown.get());
               double[] xzOffset = new double[]{(Double)this.offSet.get(), -(Double)this.offSet.get()};

               for (double x : xzOffset) {
                  for (double z : xzOffset) {
                     BlockPos offSetPos = new BlockPosX(pos.getX() + x, pos.getY(), pos.getZ() + z);
                     if (this.checkFalling() && !this.mc.world.isAir(offSetPos) && !this.mc.world.getBlockState(offSetPos).isReplaceable()) {
                        Direction side = BlockUtil.getPlaceSide(pos.up(), null);
                        if (side != null && !this.behindWall(offSetPos.up())) {
                           Color color = new Color(70, 177, 229, 80);
                           event.renderer.box(offSetPos.up(), color, color, ShapeMode.Both, 0);
                           this.doSwap(water);
                           Rotation.snapAt(offSetPos.up().toCenterPos());
                           this.lastPos = offSetPos.up();
                           this.mc.player.swingHand(Hand.MAIN_HAND);
                           this.mc
                              .getNetworkHandler()
                              .sendPacket(
                                 new PlayerInteractItemC2SPacket(
                                    Hand.MAIN_HAND,
                                    1,
                                    Rotation.getRotation(offSetPos.up().toCenterPos())[0],
                                    Rotation.getRotation(offSetPos.up().toCenterPos())[1]
                                 )
                              );
                           if ((Boolean)this.inventorySwap.get()) {
                              this.doSwap(water);
                           } else {
                              this.doSwap(old);
                           }

                           Rotation.snapBack();
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

   public boolean behindWall(BlockPos pos) {
      Vec3d testVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.7, pos.getZ() + 0.5);
      HitResult result = this.mc
         .world
         .raycast(new RaycastContext(this.mc.player.getEyePos(), testVec, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
      return result != null && result.getType() != Type.MISS;
   }

   private boolean checkFalling() {
      return this.mc.player.fallDistance > 3.0 && !this.mc.player.isOnGround() && !this.mc.player.isGliding();
   }

   private int findItem(Item item) {
      return this.inventorySwap.get() ? InventoryUtil.findItemInventorySlot(item) : InventoryUtil.findItem(item);
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
}
