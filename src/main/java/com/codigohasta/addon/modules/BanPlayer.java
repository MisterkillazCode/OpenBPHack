package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.mojang.authlib.GameProfile;
import java.util.Collection;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;

public class BanPlayer extends Module {
   private static final String REASON = "BPHack提醒：你已被服务器封禁";
   private static final String REASON_TELLRAW = "{\"text\":\"BPHack\\u63d0\\u9192\\uff1a\\u4f60\\u5df2\\u88ab\\u670d\\u52a1\\u5668\\u5c01\\u7981\"}";
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> includeSelf = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("include-self")).description("Danger: bans yourself too. Off by default.")).defaultValue(false))
            .build()
      );

   public BanPlayer() {
      super(AddonTemplate.OP_CATEGORY, "BanPlayer", "OP only: ban-ip everyone except yourself on enable, and ban on join. Reason: BPHack提醒：你已被服务器封禁");
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         if (!this.mc.player.isCreativeLevelTwoOp()) {
            ChatUtils.warning("BanPlayer：需要管理员(OP)权限才能使用，已自动关闭。", new Object[0]);
            this.toggle();
         } else {
            ChatUtils.info("BanPlayer：正在封禁除你之外的所有玩家...", new Object[0]);
            this.banAllOnline();
         }
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
   }

   @EventHandler
   private void onPacket(Receive event) {
      if (event.packet instanceof PlayerListS2CPacket packet) {
         if (this.mc.player != null) {
            if (packet.getActions().contains(Action.ADD_PLAYER)) {
               String selfName = this.mc.player.getGameProfile().name();

               for (Entry entry : packet.getEntries()) {
                  GameProfile profile = entry.profile();
                  if (profile != null) {
                     String name = profile.name();
                     if (name != null && !name.isEmpty() && ((Boolean)this.includeSelf.get() || !name.equals(selfName))) {
                        this.ban(name);
                     }
                  }
               }
            }
         }
      }
   }

   private void banAllOnline() {
      if (this.mc.getNetworkHandler() != null) {
         String selfName = this.mc.player.getGameProfile().name();
         Collection<PlayerListEntry> list = this.mc.getNetworkHandler().getPlayerList();
         if (list != null) {
            for (PlayerListEntry entry : list) {
               String name = entry.getProfile().name();
               if (name != null && !name.isEmpty() && ((Boolean)this.includeSelf.get() || !name.equals(selfName))) {
                  this.ban(name);
               }
            }
         }
      }
   }

   private void ban(String name) {
      if (this.mc.getNetworkHandler() != null) {
         ClientPlayNetworkHandler nh = this.mc.getNetworkHandler();
         nh.sendPacket(new CommandExecutionC2SPacket("ban-ip " + name));
         nh.sendPacket(
            new CommandExecutionC2SPacket(
               "tellraw " + name + " {\"text\":\"BPHack\\u63d0\\u9192\\uff1a\\u4f60\\u5df2\\u88ab\\u670d\\u52a1\\u5668\\u5c01\\u7981\"}"
            )
         );
         ChatUtils.info("BanPlayer：已封禁 " + name + " 并发送提示", new Object[0]);
      }
   }
}
