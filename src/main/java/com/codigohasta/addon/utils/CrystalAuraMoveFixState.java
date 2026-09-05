package com.codigohasta.addon.utils;

public final class CrystalAuraMoveFixState {
   public static boolean active = false;
   public static boolean attacking = false;
   public static CrystalAuraMoveFixState.Mode mode = CrystalAuraMoveFixState.Mode.WalkOnly;

   private CrystalAuraMoveFixState() {
   }

   public static enum Mode {
      None,
      WalkOnly,
      StopSprint;
   }
}
