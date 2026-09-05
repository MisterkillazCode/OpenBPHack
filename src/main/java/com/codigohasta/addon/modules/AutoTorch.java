package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.TorchBlock;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoTorch extends Module {
   public static AutoTorch INSTANCE;
   private Timer placeTimer = new Timer();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Delay")).description("Place Delay (ms)")).defaultValue(50)).min(0).sliderMax(10000).build());
   private final Setting<Integer> renderRange = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("RenderRange")).description("RenderDistance")).defaultValue(10)).min(0).sliderMax(10).build());
   private final Setting<Integer> range = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Range")).description("Reach Distance")).defaultValue(5)).min(0).sliderMax(6).build());
   private final Setting<Boolean> onlyRender = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("OnlyRender"))
                  .description("RendernotOperation"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> throughWall = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ThroughWall"))
                  .description("Place Through Wall"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> checkLightLevel = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("CheckLightLevel")).description("Light Level")).defaultValue(7)).min(0).sliderMax(15).build());

   public AutoTorch() {
      super(AddonTemplate.SC_CATEGORY, "AutoTorch", "Automatically places torches.");
      INSTANCE = this;
   }

   @EventHandler
   private void onRender3d(Render3DEvent event) {
      for (BlockPos pos : BlockUtil.getSphere(((Integer)this.renderRange.get()).intValue())) {
         if (this.mc.world.getLightLevel(LightType.BLOCK, pos) <= (Integer)this.checkLightLevel.get() && BlockUtil.canPlace(pos)) {
            Color color = new Color(255, 0, 0, 255);
            event.renderer
               .line(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY(), pos.getZ() + 1, color);
            event.renderer
               .line(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ() + 1, color);
         }
      }

      if (!(Boolean)this.onlyRender.get()) {
         if (this.placeTimer.passedMs((long)((Integer)this.delay.get()).intValue())) {
            int oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            int slot = InventoryUtil.findClass(TorchBlock.class);
            if (slot != -1) {
               int counts = 0;

               for (BlockPos posx : BlockUtil.getSphere(((Integer)this.range.get()).intValue())) {
                  if (counts >= 1) {
                     break;
                  }

                  if ((!(Boolean)this.throughWall.get() || !this.behindWall(posx))
                     && !(BlockUtil.getBlock(posx) instanceof TorchBlock)
                     && !(BlockUtil.getBlock(posx.down()) instanceof TorchBlock)
                     && this.mc.world.isAir(posx)
                     && !this.mc.world.isAir(posx.down())
                     && !this.mc.world.getBlockState(posx.down()).isReplaceable()
                     && !BlockUtil.hasPlayerEntity(posx)
                     && !BlockUtil.hasEntity(posx, false)
                     && this.mc.world.getLightLevel(LightType.BLOCK, posx) <= (Integer)this.checkLightLevel.get()) {
                     Direction side = BlockUtil.getPlaceSide(posx, null);
                     if (side != null && side != Direction.UP) {
                        InventoryUtil.switchToSlot(slot);
                        BlockUtil.placeBlock(posx, side, true);
                        Color color = new Color(255, 255, 255, 80);
                        event.renderer.box(posx, color, color, ShapeMode.Both, 0);
                        counts++;
                        InventoryUtil.switchToSlot(oldSlot);
                     }
                  }
               }

               this.placeTimer.reset();
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
}
