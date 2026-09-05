package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.text.SimpleDateFormat;
import java.util.Date;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.math.BlockPos;

public class DeathWaypoint extends Module {
   public static DeathWaypoint INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> chatMsg = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("ChatMessage")).description("Print the waypoint coordinates in chat on death.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> includeTime = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("IncludeTime")).description("Include the time of death in the waypoint name.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> iconSkull = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("SkullIcon"))
                  .description("Mark the death point with a skull icon; falls back to the default block icon."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> maxWaypoints = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("MaxWaypoints"))
                  .description("Maximum number of death waypoints to keep; oldest gets deleted. 0 = unlimited."))
               .defaultValue(20))
            .min(0)
            .sliderRange(0, 100)
            .build()
      );
   private BlockPos lastPos = null;
   private Dimension lastDim = null;

   public DeathWaypoint() {
      super(AddonTemplate.SC_CATEGORY, "DeathWaypoint", "Drops a waypoint where you die so you can find your way back to your items.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.lastPos = null;
      this.lastDim = null;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.mc.player.isDead() && !(this.mc.player.getHealth() <= 0.0F)) {
            this.lastPos = this.mc.player.getBlockPos();
            this.lastDim = PlayerUtils.getDimension();
         }
      }
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if (event.screen instanceof DeathScreen) {
         if (this.lastPos != null) {
            String name = this.includeTime.get() ? "死亡 " + new SimpleDateFormat("MM-dd HH:mm").format(new Date()) : "死亡点";
            meteordevelopment.meteorclient.systems.waypoints.Waypoint.Builder builder = new meteordevelopment.meteorclient.systems.waypoints.Waypoint.Builder()
               .name(name)
               .pos(this.lastPos)
               .dimension(this.lastDim == null ? Dimension.Overworld : this.lastDim);
            if ((Boolean)this.iconSkull.get()) {
               builder.icon("skull");
            }

            Waypoints.get().add(builder.build());
            this.trimWaypoints();
            if ((Boolean)this.chatMsg.get()) {
               ChatUtils.info("已记录死亡点：%d, %d, %d", new Object[]{this.lastPos.getX(), this.lastPos.getY(), this.lastPos.getZ()});
            }

            this.lastPos = null;
         }
      }
   }

   private void trimWaypoints() {
      int max = (Integer)this.maxWaypoints.get();
      if (max > 0) {
         int count = 0;
         Waypoint oldest = null;

         for (Waypoint wp : Waypoints.get()) {
            if (wp != null && wp.name != null && ((String)wp.name.get()).startsWith("死亡")) {
               count++;
               if (oldest == null || wp.createdAt < oldest.createdAt) {
                  oldest = wp;
               }
            }
         }

         if (count > max && oldest != null) {
            Waypoints.get().remove(oldest);
         }
      }
   }
}
