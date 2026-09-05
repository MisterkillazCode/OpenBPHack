package com.codigohasta.addon.utils.leaveshack.events;

public class KeyboardInputEvent {
   private float forward;
   private float strafe;
   public boolean jump;
   public boolean sneak;

   public KeyboardInputEvent(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak) {
      this.jump = jump;
      this.sneak = sneak;
      this.forward = (forward ? 1.0F : 0.0F) - (backward ? 1.0F : 0.0F);
      this.strafe = (right ? 1.0F : 0.0F) - (left ? 1.0F : 0.0F);
   }

   public float getForward() {
      return this.forward;
   }

   public void setForward(float forward) {
      this.forward = forward;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   public float getMovementForward() {
      return this.forward;
   }

   public float getMovementSideways() {
      return this.strafe;
   }

   public boolean isForward() {
      return this.forward > 0.0F;
   }

   public boolean isBackward() {
      return this.forward < 0.0F;
   }

   public boolean isLeft() {
      return this.strafe < 0.0F;
   }

   public boolean isRight() {
      return this.strafe > 0.0F;
   }
}
