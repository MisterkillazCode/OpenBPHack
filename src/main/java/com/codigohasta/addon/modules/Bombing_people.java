package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

public class Bombing_people extends Module {
   private final Setting<Integer> interval = this.settings
      .getDefaultGroup()
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("interval")).description("Ticks between each round of TNT spawns (20 ticks = 1 second)."))
               .defaultValue(20))
            .min(1)
            .max(400)
            .sliderRange(1, 400)
            .build()
      );
   private int timer = 0;

   public Bombing_people() {
      super(
         AddonTemplate.OP_CATEGORY,
         "Bombing_people",
         "OP only. Continuously spawns a TNT under every online player's feet (execute at @a run summon minecraft:tnt) while enabled. Toggle off to stop."
      );
   }

   public void onActivate() {
      if (this.mc.player == null || this.mc.getNetworkHandler() == null) {
         this.toggle();
      } else if (!this.mc.player.isCreativeLevelTwoOp()) {
         this.toggle();
      } else {
         this.timer = 0;
      }
   }

   public void onDeactivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         if (!this.mc.player.isCreativeLevelTwoOp()) {
            this.toggle();
         } else if (++this.timer >= (Integer)this.interval.get()) {
            this.timer = 0;
            this.mc.getNetworkHandler().sendPacket(new CommandExecutionC2SPacket("execute at @a run summon minecraft:tnt"));
         }
      }
   }
}
