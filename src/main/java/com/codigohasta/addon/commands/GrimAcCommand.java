package com.codigohasta.addon.commands;

import com.codigohasta.addon.modules.GrimAc;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class GrimAcCommand extends Command {
   public GrimAcCommand() {
      super("grimac", "Toggles the client-side GrimAc cheat detector. Usage: .grimac", new String[0]);
   }

   public void build(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes(context -> {
         GrimAc.toggleFromCommand();
         return 1;
      });
   }
}
