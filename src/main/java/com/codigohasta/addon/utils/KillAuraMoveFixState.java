package com.codigohasta.addon.utils;

public class KillAuraMoveFixState {
   public static boolean active = false;
   public static KillAuraMoveFixState.Mode mode = KillAuraMoveFixState.Mode.None;
   public static boolean attacking = false;

   public static enum Mode {
      None,
      StopSprint,
      KeepSprint;
   }
}
