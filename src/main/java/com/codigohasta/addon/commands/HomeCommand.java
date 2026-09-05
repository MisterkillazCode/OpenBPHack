package com.codigohasta.addon.commands;

import com.codigohasta.addon.modules.HomeWaypoint;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

public class HomeCommand extends Command {
   public HomeCommand() {
      super("home", "Manage home waypoints.", new String[0]);
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("set").then(argument("name", StringArgumentType.word()).executes(context -> {
         String name = StringArgumentType.getString(context, "name");
         boolean ok = HomeWaypoint.setHome(name);
         this.reply(ok ? "已设置家「" + name + "」。" : "设置失败，请确认已进游戏。");
         return 1;
      })));
      builder.then(literal("del").then(argument("name", StringArgumentType.word()).executes(context -> {
         String name = StringArgumentType.getString(context, "name");
         boolean ok = HomeWaypoint.delHome(name);
         this.reply(ok ? "已删除家「" + name + "」。" : "没有叫「" + name + "」的家。");
         return 1;
      })));
      builder.then(literal("list").executes(context -> {
         List<String> homes = HomeWaypoint.listHomes();
         this.reply(homes.isEmpty() ? "还没有保存任何家。" : String.join(", ", homes));
         return 1;
      }));
      builder.then(literal("go").then(argument("name", StringArgumentType.word()).executes(context -> {
         String name = StringArgumentType.getString(context, "name");
         Waypoint wp = HomeWaypoint.getHome(name);
         if (wp == null) {
            this.reply("没有叫「" + name + "」的家。");
            return 1;
         } else {
            BlockPos pos = (BlockPos)wp.pos.get();
            wp.visible.set(true);
            wp.maxVisible.set(100000);
            Waypoints.get().add(wp);
            this.reply("「" + name + "」在 " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "，已高亮。");
            return 1;
         }
      })));
   }

   private void reply(String message) {
      HomeWaypoint module = HomeWaypoint.INSTANCE;
      if (module != null) {
         module.reply(message);
      } else {
         this.info(message, new Object[0]);
      }
   }
}
