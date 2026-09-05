package com.codigohasta.addon.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({PlayerInteractEntityC2SPacket.class})
public interface PlayerInteractEntityC2SPacketAccessor {
   @Mutable
   @Accessor("entityId")
   void setEntityId(int var1);

   @Accessor("entityId")
   int getEntityId();

   @Mutable
   @Accessor("playerSneaking")
   void setPlayerSneaking(boolean var1);
}
