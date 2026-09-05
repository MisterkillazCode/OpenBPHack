package com.codigohasta.addon.enums;

public final class ImgModes {
   private ImgModes() {
   }

   public static enum ImgMode {
      Off,
      BypassGrim,
      LazyBypassGrim,
      LazyGrimPlus,
      DupFullFakeGround;
   }

   public static enum ImgPacketSneak {
      Off,
      BadPacket,
      Interact,
      GrimFallFlying;
   }

   public static enum ImgSneakBypass {
      Off,
      GrimLazy,
      GrimLazyV3;
   }

   public static enum IntValues {
      One(1),
      Two(2),
      Three(3),
      Four(4),
      Five(5),
      Ten(10),
      Twenty(20);

      private final int v;

      private IntValues(int v) {
         this.v = v;
      }

      public int asInt() {
         return this.v;
      }
   }
}
