package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class HomeWaypoint extends Module {
   public static HomeWaypoint INSTANCE;
   public static final String PREFIX = "HOME_";
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> chatMsg = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("ChatMessage")).description("Echo command results in chat.")).defaultValue(true)).build());

   public HomeWaypoint() {
      super(
         AddonTemplate.SC_CATEGORY, "HomeWaypoint", "Home waypoint manager. Use with .home set / list / del / go; data is stored in Meteor's waypoint system."
      );
      INSTANCE = this;
   }

   public static List<String> listHomes() {
      List<String> out = new ArrayList<>();

      for (Waypoint wp : Waypoints.get()) {
         if (wp != null && wp.name != null) {
            String name = (String)wp.name.get();
            if (name.startsWith("HOME_")) {
               out.add(name.substring("HOME_".length()));
            }
         }
      }

      return out;
   }

   public static Waypoint getHome(String name) {
      return Waypoints.get().get("HOME_" + name);
   }

   public static boolean setHome(String name) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         BlockPos pos = client.player.getBlockPos();
         Dimension dim = PlayerUtils.getDimension();
         Waypoint existing = getHome(name);
         if (existing != null) {
            existing.pos.set(pos);
            existing.dimension.set(dim);
            Waypoints.get().add(existing);
         } else {
            Waypoints.get()
               .add(new meteordevelopment.meteorclient.systems.waypoints.Waypoint.Builder().name("HOME_" + name).pos(pos).dimension(dim).icon("square").build());
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean delHome(String name) {
      Waypoint wp = getHome(name);
      return wp != null && Waypoints.get().remove(wp);
   }

   public void reply(String message) {
      if ((Boolean)this.chatMsg.get()) {
         ChatUtils.sendMsg(Text.literal("[Home] " + message));
      }
   }
}
