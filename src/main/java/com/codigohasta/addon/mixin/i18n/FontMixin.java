package com.codigohasta.addon.mixin.i18n;

import meteordevelopment.meteorclient.renderer.text.Font;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTTPackedchar.Buffer;
import org.lwjgl.system.CustomBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Font.class})
public abstract class FontMixin {
   @Unique
   private static final int CJK_COUNT = 20976;
   @Unique
   private static final int CJK_START = 19968;
   @Unique
   private static Buffer cjkCharData;

   @ModifyVariable(
      method = {"<init>"},
      at = @At(
         value = "INVOKE",
         target = "Lorg/lwjgl/stb/STBTruetype;stbtt_PackBegin(Lorg/lwjgl/stb/STBTTPackContext;Ljava/nio/ByteBuffer;IIII)Z"
      ),
      name = {"cdata"}
   )
   private Buffer[] addCjkCdata(Buffer[] cdata) {
      cjkCharData = STBTTPackedchar.create(20976);
      Buffer[] templist = new Buffer[cdata.length + 1];
      System.arraycopy(cdata, 0, templist, 0, cdata.length);
      templist[cdata.length] = cjkCharData;
      return templist;
   }

   @Redirect(
      method = {"<init>"},
      at = @At(
         value = "INVOKE",
         target = "Lorg/lwjgl/stb/STBTTPackRange$Buffer;flip()Lorg/lwjgl/system/CustomBuffer;"
      )
   )
   private CustomBuffer flipWithCjk(org.lwjgl.stb.STBTTPackRange.Buffer packRange) {
      packRange.put(STBTTPackRange.create().set(((STBTTPackRange)packRange.get(0)).font_size(), 19968, null, 20976, cjkCharData, (byte)2, (byte)2));
      return packRange.flip();
   }

   @ModifyConstant(
      method = {"<init>"},
      constant = {@Constant(
         intValue = 2048
      )}
   )
   private int changeSize(int value) {
      return 8192;
   }

   @ModifyConstant(
      method = {"<init>"},
      constant = {@Constant(
         intValue = 4194304
      )}
   )
   private int changeBitmapSize(int value) {
      return 67108864;
   }

   @ModifyConstant(
      method = {"<init>"},
      constant = {@Constant(
         floatValue = 4.8828125E-4F
      )}
   )
   private float changeUvScale(float value) {
      return 1.2207031E-4F;
   }
}
