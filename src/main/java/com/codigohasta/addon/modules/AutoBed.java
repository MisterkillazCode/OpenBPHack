package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.World;

public class AutoBed extends Module {
   public static AutoBed INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoBed.BedMode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Sleep = sleep automatically; Place = place a bed; Both = place then sleep."))
               .defaultValue(AutoBed.BedMode.Sleep))
            .build()
      );
   private final Setting<Integer> radius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Radius"))
                  .description("Search radius for finding / placing a bed, in blocks."))
               .defaultValue(4))
            .min(1)
            .sliderRange(1, 8)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("How often to run, in ticks."))
               .defaultValue(20))
            .min(0)
            .sliderRange(0, 100)
            .build()
      );
   private final Setting<Boolean> onlyAtNight = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("OnlyAtNight"))
                  .description("Only sleep at night or during a thunderstorm."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> avoidNether = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AvoidNether"))
                  .description("Do not place beds in the Nether or End (they explode)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("SwapBack"))
                  .description("Swap back to your previous item after placing."))
               .defaultValue(true))
            .build()
      );
   private int timer = 0;

   public AutoBed() {
      super(AddonTemplate.SC_CATEGORY, "AutoBed", "Automates beds: sleeps at night, or places a bed at your feet. Disabled in the Nether and End by default.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.mc.player.isSleeping()) {
            if (this.timer > 0) {
               this.timer--;
            } else {
               boolean bedExplodes = this.mc.world.getRegistryKey() == World.NETHER || this.mc.world.getRegistryKey() == World.END;
               boolean canPlace = !(Boolean)this.avoidNether.get() || !bedExplodes;
               if (this.mode.get() != AutoBed.BedMode.Sleep && canPlace && this.tryPlace()) {
                  this.timer = (Integer)this.delay.get();
               } else {
                  if (this.mode.get() != AutoBed.BedMode.Place && this.trySleep()) {
                     this.timer = (Integer)this.delay.get();
                  }
               }
            }
         }
      }
   }

   private boolean tryPlace() {
      FindItemResult bed = InvUtils.find(AutoBed::isBed);
      if (!bed.found()) {
         return false;
      } else {
         BlockPos origin = this.mc.player.getBlockPos();
         int r = (Integer)this.radius.get();

         for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
               for (int y = -1; y <= 1; y++) {
                  BlockPos pos = origin.add(x, y, z);
                  if (this.canPlaceBedAt(pos) && BlockUtils.place(pos, bed, 100)) {
                     if ((Boolean)this.swapBack.get()) {
                        InvUtils.swapBack();
                     }

                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean canPlaceBedAt(BlockPos pos) {
      BlockState state = this.mc.world.getBlockState(pos);
      if (!state.isAir()) {
         return false;
      } else if (this.mc.world.getBlockState(pos.down()).isAir()) {
         return false;
      } else {
         for (Direction dir : Type.HORIZONTAL) {
            BlockPos head = pos.offset(dir);
            if (this.mc.world.getBlockState(head).isAir() && !this.mc.world.getBlockState(head.down()).isAir()) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean trySleep() {
      if ((Boolean)this.onlyAtNight.get() && !this.isNight()) {
         return false;
      } else {
         BlockPos origin = this.mc.player.getBlockPos();
         int r = (Integer)this.radius.get();

         for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 1; y++) {
               for (int z = -r; z <= r; z++) {
                  BlockPos pos = origin.add(x, y, z);
                  Block block = this.mc.world.getBlockState(pos).getBlock();
                  if (block instanceof BedBlock && !(this.mc.player.squaredDistanceTo(pos.toCenterPos()) > r * r)) {
                     this.mc.player.trySleep(pos);
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean isNight() {
      long time = this.mc.world.getTimeOfDay() % 24000L;
      return time > 12500L && time < 23500L;
   }

   private static boolean isBed(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BedBlock;
   }

   public static enum BedMode {
      Sleep,
      Place,
      Both;
   }
}
