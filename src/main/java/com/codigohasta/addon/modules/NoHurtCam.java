package com.codigohasta.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class NoHurtCam extends Module {
   public static NoHurtCam INSTANCE;
   private final SettingGroup sg = this.settings.getDefaultGroup();
   private final Setting<Boolean> noTilt = this.sg
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("no-hurt-tilt")).description("Removes the camera tilt / shake animation when you take damage."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> seeInsideBlock = this.sg
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("see-inside-block"))
                  .description("Stops rendering the block your camera is stuck in, so you can see out instead of getting a white screen."))
               .defaultValue(true))
            .build()
      );
   public BlockPos hiddenBlock = null;

   public NoHurtCam() {
      super(
         Categories.Render,
         "NoHurtCam",
         "Removes the camera shake when you take damage, and stops the block you are stuck inside from rendering so you can see out instead of getting a white screen."
      );
      INSTANCE = this;
   }

   public boolean shouldRemoveTilt() {
      return (Boolean)this.noTilt.get();
   }

   public void onDeactivate() {
      this.setHiddenBlock(null);
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.world != null && this.mc.worldRenderer != null) {
         Entity cam = this.mc.getCameraEntity();
         if (cam == null) {
            this.setHiddenBlock(null);
         } else {
            BlockPos camPos = BlockPos.ofFloored(cam.getCameraPosVec(1.0F));
            BlockState state = this.mc.world.getBlockState(camPos);
            boolean inside = !state.isAir() && state.isOpaque();
            BlockPos want = this.seeInsideBlock.get() && inside ? camPos : null;
            if (want == null ? this.hiddenBlock != null : !want.equals(this.hiddenBlock)) {
               this.setHiddenBlock(want);
            }
         }
      }
   }

   private void setHiddenBlock(BlockPos pos) {
      if (this.mc.worldRenderer == null) {
         this.hiddenBlock = pos;
      } else {
         BlockPos old = this.hiddenBlock;
         this.hiddenBlock = pos;
         if (old != null || pos != null) {
            this.mc.worldRenderer.reload();
         }
      }
   }
}
