package com.codigohasta.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class CommandExample extends Command {
   public CommandExample() {
      super("example", "Sends a message.", new String[0]);
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(context -> {
         this.info("hi", new Object[0]);
         return 1;
      });
      builder.then(literal("name").then(argument("nameArgument", StringArgumentType.word()).executes(context -> {
         String argument = StringArgumentType.getString(context, "nameArgument");
         this.info("hi, " + argument, new Object[0]);
         return 1;
      })));
   }
}
