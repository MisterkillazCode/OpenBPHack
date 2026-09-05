package com.codigohasta.addon.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Accessor("leaningPitch")
   float getLeaningPitch();

   @Accessor("leaningPitch")
   void setLeaningPitch(float var1);

   @Accessor("lastLeaningPitch")
   void setLastLeaningPitch(float var1);

   @Accessor("jumpingCooldown")
   void setJumpingCooldown(int var1);
}
