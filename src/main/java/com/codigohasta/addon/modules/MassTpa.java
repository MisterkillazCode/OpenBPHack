package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Vec3d;

public class MassTpa extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgStop = this.settings.createGroup("StopStop");
   private final Setting<Double> delay = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("SendBetween")).description("notPlayerSendTPA'sBetween().")).defaultValue(5.0).min(1.0).sliderMax(20.0).build()
      );
   private final Setting<String> command = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("PointCommand"))
                  .description("Send'sPointCommand, notneedPlayerName."))
               .defaultValue("/tpa"))
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("friend"))
                  .description("notMeteorfriendListinside'sPlayerSend."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> randomOrder = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Random"))
                  .description("RandomHitPlayer List, notText."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> stopOnTeleport = this.sgStop
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("BitMoveStopStop"))
                  .description("CheckTesttoMarkSendSpawnbigDegree(SendSuccess)TimeStopStopModule."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> stopOnChat = this.sgStop
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DisappearStopStop"))
                  .description("CheckTesttochatSkyOutSetKeywordTimeStopStopModule."))
               .defaultValue(true))
            .build()
      );
   private final Setting<List<String>> successKeywords = this.sgStop
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                     .name("SuccessKeyword"))
                  .description("ifchatSkyDisappearPackListin'sMeaningOne, ViewFortoDirectionConnectTPA."))
               .defaultValue(new String[]{"Connect"})
               .visible(this.stopOnChat::get))
            .build()
      );
   private final Setting<Boolean> debugChat = this.sgStop
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Debug Message"))
                  .description("atThisGroundDisplayServerSend'shaveDisappear, DirectionCasualKeyword."))
               .defaultValue(false))
            .build()
      );
   private final List<String> targetPlayers = new ArrayList<>();
   private int playerIndex = 0;
   private int timer = 0;
   private Vec3d lastPos = null;

   public MassTpa() {
      super(AddonTemplate.CATEGORY, "MassTpa", "Sends /tpa to all online players automatically; stops when a player connects.");
   }

   public void onActivate() {
      this.loadPlayers();
      this.timer = 0;
      this.lastPos = this.mc.player != null
         ? new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ())
         : null;
   }

   public void onDeactivate() {
      this.targetPlayers.clear();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.toggle();
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.stopOnTeleport.get() && this.lastPos != null) {
            double distance = this.lastPos
               .distanceTo(new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ()));
            if (distance > 8.0) {
               this.info("CheckTesttoPosition (BitMove %.1f), SetSendSuccess, StopStopModule.", new Object[]{distance});
               this.toggle();
               return;
            }
         }

         this.lastPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         if (this.timer > 0) {
            this.timer--;
         } else {
            if (this.targetPlayers.isEmpty()) {
               this.info("Player ListAlreadyComplete, HeavyNewLoad...", new Object[0]);
               this.loadPlayers();
               if (this.targetPlayers.isEmpty()) {
                  this.warning("currentServernoOtherCanSend'sPlayer, StopStopModule.", new Object[0]);
                  this.toggle();
                  return;
               }
            }

            if (this.playerIndex >= this.targetPlayers.size()) {
               this.playerIndex = 0;
            }

            String target = this.targetPlayers.get(this.playerIndex);
            if (!target.equals(this.mc.player.getName().getString())) {
               ChatUtils.sendPlayerMsg((String)this.command.get() + " " + target);
            }

            this.playerIndex++;
            this.timer = (int)((Double)this.delay.get() * 20.0);
         }
      }
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      if ((Boolean)this.stopOnChat.get()) {
         if (event.packet instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();
            if ((Boolean)this.debugChat.get()) {
               this.info("[Debug] toDisappear:" + message, new Object[0]);
            }

            for (String keyword : (List)this.successKeywords.get()) {
               if (message.toLowerCase().contains(keyword.toLowerCase())) {
                  this.info("CheckTesttoKeyword [" + keyword + "], SettoDirectionAlreadyConnect, StopStopModule.", new Object[0]);
                  this.toggle();
                  break;
               }
            }
         }
      }
   }

   private void loadPlayers() {
      this.targetPlayers.clear();
      if (this.mc.getNetworkHandler() != null) {
         for (PlayerListEntry entry : this.mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (!name.equals(this.mc.player.getName().getString()) && (!(Boolean)this.ignoreFriends.get() || !Friends.get().isFriend(entry))) {
               this.targetPlayers.add(name);
            }
         }

         if ((Boolean)this.randomOrder.get()) {
            Collections.shuffle(this.targetPlayers);
         }

         this.info("AlreadyLoad" + this.targetPlayers.size() + "NamePlayerEnter TPA .", new Object[0]);
         this.playerIndex = 0;
      }
   }
}
