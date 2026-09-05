package com.codigohasta.addon.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class Criticals extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgMace = this.settings.createGroup("Mace");
   private final Setting<Criticals.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("The mode on how Criticals will function.")).defaultValue(Criticals.Mode.Packet))
            .build()
      );
   private final Setting<Boolean> ka = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("only-killaura"))
                     .description("Only performs crits when using killaura."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() != Criticals.Mode.None))
            .build()
      );
   private final Setting<Boolean> mace = this.sgMace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("smash-attack"))
                  .description("Will always perform smash attacks when using a mace."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> extraHeight = this.sgMace
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("additional-height"))
                  .description("The amount of additional height to spoof. More height means more damage."))
               .defaultValue(0.0)
               .min(0.0)
               .sliderRange(0.0, 100.0)
               .visible(this.mace::get))
            .build()
      );
   private final Setting<Double> imgDeltaLow = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("img-delta-low"))
                  .description("First y offset for IMG_Packet / IMG_Test."))
               .defaultValue(5.0E-4)
               .min(0.0)
               .max(0.05)
               .visible(() -> this.mode.get() == Criticals.Mode.IMG_Packet || this.mode.get() == Criticals.Mode.IMG_Test))
            .build()
      );
   private final Setting<Double> imgDeltaHigh = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("img-delta-high"))
                  .description("Second y offset for IMG_Packet / IMG_Test."))
               .defaultValue(1.0E-4)
               .min(0.0)
               .max(0.05)
               .visible(() -> this.mode.get() == Criticals.Mode.IMG_Packet || this.mode.get() == Criticals.Mode.IMG_Test))
            .build()
      );
   private final Setting<Double> imgGrimA = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("img-grim-a"))
                  .description("First lift for IMG_Grim."))
               .defaultValue(0.0625)
               .min(0.0)
               .max(0.5)
               .visible(() -> this.mode.get() == Criticals.Mode.IMG_Grim))
            .build()
      );
   private final Setting<Double> imgGrimB = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("img-grim-b"))
                  .description("Second lift for IMG_Grim."))
               .defaultValue(0.04535)
               .min(0.0)
               .max(0.5)
               .visible(() -> this.mode.get() == Criticals.Mode.IMG_Grim))
            .build()
      );
   private PlayerInteractEntityC2SPacket attackPacket;
   private HandSwingC2SPacket swingPacket;
   private boolean sendPackets;
   private int sendTimer;
   private double lastY;
   private boolean waitingForPeak;
   private boolean lastOnGround;

   public Criticals() {
      super(Categories.Combat, "criticals", "Performs critical attacks when you hit your target.");
   }

   public void onActivate() {
      this.attackPacket = null;
      this.swingPacket = null;
      this.sendPackets = false;
      this.sendTimer = 0;
      this.lastY = 0.0;
      this.waitingForPeak = false;
      if (this.mc.player != null) {
         this.lastOnGround = this.mc.player.isOnGround();
      }
   }

   @EventHandler
   private void onSendPacket(Send event) {
      Criticals.Mode m = (Criticals.Mode)this.mode.get();
      if (m != Criticals.Mode.None) {
         if (event.packet instanceof PlayerInteractEntityC2SPacket pkt && pkt.type == PlayerInteractEntityC2SPacket.ATTACK) {
            if ((Boolean)this.mace.get() && this.mc.player.getMainHandStack().getItem() instanceof MaceItem) {
               if (this.mc.player.isGliding()) {
                  return;
               }

               this.sendPacket(0.0);
               this.sendPacket(1.501 + (Double)this.extraHeight.get());
               this.sendPacket(0.0);
               return;
            }

            if (m == Criticals.Mode.IMG_Packet || m == Criticals.Mode.IMG_Grim || m == Criticals.Mode.IMG_Test) {
               if (this.skipCrit()) {
                  return;
               }

               Entity entity = this.mc.world.getEntityById(pkt.entityId);
               if (!(entity instanceof LivingEntity)) {
                  return;
               }

               if (entity != ((KillAura)Modules.get().get(KillAura.class)).getTarget() && (Boolean)this.ka.get()) {
                  return;
               }

               double x = this.mc.player.getX();
               double y = this.mc.player.getY();
               double z = this.mc.player.getZ();
               switch (m) {
                  case IMG_Packet:
                  case IMG_Test:
                     if (this.mc.player.isOnGround()) {
                        this.sendPosition(x, y + (Double)this.imgDeltaLow.get(), z, false);
                        this.sendPosition(x, y + (Double)this.imgDeltaHigh.get(), z, false);
                     }
                     break;
                  case IMG_Grim:
                     if (this.mc.player.isOnGround()) {
                        this.sendPosition(x, y + (Double)this.imgGrimA.get(), z, false);
                        this.sendPosition(x, y + (Double)this.imgGrimB.get(), z, false);
                     }
                  case IMG_Freeze:
                  case IMG_GrimSim:
               }

               return;
            }

            if (m == Criticals.Mode.IMG_GrimSim || m == Criticals.Mode.IMG_Freeze) {
               return;
            }

            if (this.skipCrit()) {
               return;
            }

            Entity entityx = this.mc.world.getEntityById(pkt.entityId);
            if (!(entityx instanceof LivingEntity)) {
               return;
            }

            if (entityx != ((KillAura)Modules.get().get(KillAura.class)).getTarget() && (Boolean)this.ka.get()) {
               return;
            }

            switch (m) {
               case Packet:
                  this.sendPacket(0.0625);
                  this.sendPacket(0.0);
                  break;
               case UpdatedNCP:
                  this.sendPacket(8.0E-7);
                  this.sendPacket(0.0);
                  break;
               case OldNCP:
                  this.sendPacket(0.11);
                  this.sendPacket(0.1100013579);
                  this.sendPacket(1.3579E-6);
                  break;
               case Jump:
               case MiniJump:
                  if (!this.sendPackets) {
                     this.sendPackets = true;
                     this.attackPacket = (PlayerInteractEntityC2SPacket)event.packet;
                     if (m == Criticals.Mode.Jump) {
                        this.mc.player.jump();
                        this.waitingForPeak = true;
                        this.lastY = this.mc.player.getY();
                     } else {
                        ((IVec3d)this.mc.player.getVelocity()).meteor$setY(0.25);
                        this.sendTimer = 4;
                     }

                     event.cancel();
                  }
            }
         } else if (event.packet instanceof HandSwingC2SPacket
            && m != Criticals.Mode.Packet
            && m != Criticals.Mode.IMG_Packet
            && m != Criticals.Mode.IMG_Grim
            && m != Criticals.Mode.IMG_Test
            && m != Criticals.Mode.IMG_Freeze
            && m != Criticals.Mode.IMG_GrimSim) {
            if (this.skipCrit()) {
               return;
            }

            if (this.sendPackets && this.swingPacket == null) {
               this.swingPacket = (HandSwingC2SPacket)event.packet;
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.sendPackets) {
         Criticals.Mode m = (Criticals.Mode)this.mode.get();
         if (m == Criticals.Mode.Jump && this.waitingForPeak) {
            double currentY = this.mc.player.getY();
            if (currentY <= this.lastY) {
               this.waitingForPeak = false;
               this.sendTimer = 0;
            }

            this.lastY = currentY;
            return;
         }

         if (this.sendTimer <= 0) {
            if (this.attackPacket == null || this.swingPacket == null) {
               this.sendPackets = false;
               return;
            }

            this.mc.getNetworkHandler().sendPacket(this.attackPacket);
            this.mc.getNetworkHandler().sendPacket(this.swingPacket);
            this.attackPacket = null;
            this.swingPacket = null;
            this.sendPackets = false;
         } else {
            this.sendTimer--;
         }
      }

      if (this.mc.player != null) {
         Criticals.Mode mx = (Criticals.Mode)this.mode.get();
         if (mx == Criticals.Mode.IMG_Freeze) {
            boolean now = this.mc.player.isOnGround();
            if (this.lastOnGround && !now && !this.mc.player.isGliding()) {
               this.mc
                  .getNetworkHandler()
                  .sendPacket(new LookAndOnGround(this.mc.player.getYaw(), this.mc.player.getPitch(), false, this.mc.player.horizontalCollision));
            }

            this.lastOnGround = now;
         }
      }
   }

   private void sendPacket(double height) {
      double x = this.mc.player.getX();
      double y = this.mc.player.getY();
      double z = this.mc.player.getZ();
      PlayerMoveC2SPacket packet = new PositionAndOnGround(x, y + height, z, false, false);
      ((IPlayerMoveC2SPacket)packet).meteor$setTag(1337);
      this.mc.player.networkHandler.sendPacket(packet);
   }

   private void sendPosition(double x, double y, double z, boolean onGround) {
      PlayerMoveC2SPacket packet = new PositionAndOnGround(x, y, z, onGround, this.mc.player.horizontalCollision);
      ((IPlayerMoveC2SPacket)packet).meteor$setTag(1337);
      this.mc.player.networkHandler.sendPacket(packet);
   }

   private boolean skipCrit() {
      Criticals.Mode m = (Criticals.Mode)this.mode.get();
      return !EntityUtils.isInCobweb(this.mc.player) || m != Criticals.Mode.Jump && m != Criticals.Mode.MiniJump
         ? !this.mc.player.isOnGround() || this.mc.player.isSubmergedInWater() || this.mc.player.isInLava() || this.mc.player.isClimbing()
         : true;
   }

   public String getInfoString() {
      return ((Criticals.Mode)this.mode.get()).name();
   }

   public static enum Mode {
      None,
      Packet,
      UpdatedNCP,
      OldNCP,
      Jump,
      MiniJump,
      IMG_Packet,
      IMG_Grim,
      IMG_Freeze,
      IMG_GrimSim,
      IMG_Test;
   }
}
