package com.codigohasta.addon.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({HandledScreen.class})
public interface HandledScreenSlotAccessor {
   @Invoker("getSlotAt")
   Slot bphack$getSlotAt(double var1, double var3);
}
