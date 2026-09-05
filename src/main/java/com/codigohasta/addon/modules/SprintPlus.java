package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class SprintPlus extends Module {
   public static SprintPlus INSTANCE;
   private final SettingGroup sg = this.settings.getDefaultGroup();
   public final Setting<SprintPlus.Mode> mode = this.sg
      .add(((Builder)((Builder)((Builder)new Builder().name("Mode")).description("When to keep sprinting")).defaultValue(SprintPlus.Mode.Always)).build());
   private final Setting<Boolean> pauseWater = this.sg
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PauseInWater"))
                  .description("Stop sprinting while in water"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> pauseWeb = this.sg
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PauseInWeb"))
                  .description("Stop sprinting while in cobwebs"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> pauseSneak = this.sg
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PauseSneaking"))
                  .description("Stop sprinting while sneaking"))
               .defaultValue(true))
            .build()
      );

   public SprintPlus() {
      super(
         AddonTemplate.CATEGORY,
         "SprintPlus",
         "Automatic sprint for legitimate movement. Keeps you sprinting whenever you move, without needing double-tap. Still respects hunger, so it stays fully legit."
      );
      INSTANCE = this;
   }

   public void onDeactivate() {
      if (this.mc.player != null) {
         this.mc.player.setSprinting(false);
      }
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.mc.player.setSprinting(this.shouldSprint());
      }
   }

   private boolean shouldSprint() {
      boolean moving;
      if (this.mode.get() == SprintPlus.Mode.Forward) {
         moving = this.mc.options.forwardKey.isPressed() && !this.mc.options.backKey.isPressed();
      } else {
         moving = this.mc.options.forwardKey.isPressed()
            || this.mc.options.backKey.isPressed()
            || this.mc.options.leftKey.isPressed()
            || this.mc.options.rightKey.isPressed();
      }

      if (!moving) {
         return false;
      } else if (!this.mc.player.isCreative() && this.mc.player.getHungerManager().getFoodLevel() <= 6) {
         return false;
      } else if ((Boolean)this.pauseSneak.get() && this.mc.player.isSneaking()) {
         return false;
      } else {
         return this.pauseWater.get() && this.mc.player.isInFluid() ? false : !(Boolean)this.pauseWeb.get() || !this.inWeb();
      }
   }

   private boolean inWeb() {
      BlockPos p = this.mc.player.getBlockPos();
      return this.mc.world.getBlockState(p).getBlock() == Blocks.COBWEB
         || this.mc.world.getBlockState(p.up()).getBlock() == Blocks.COBWEB
         || this.mc.world.getBlockState(p.down()).getBlock() == Blocks.COBWEB;
   }

   public static enum Mode {
      Forward,
      Always;
   }
}
