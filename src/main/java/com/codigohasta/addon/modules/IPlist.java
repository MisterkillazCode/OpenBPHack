package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.text.Text;

public class IPlist extends Module {
   private final SettingGroup sg = this.settings.getDefaultGroup();
   private final Setting<String> command = this.sg
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("command"))
                  .description(
                     "IP lookup command template. %s is replaced with the player name. The server must support an IP query command (e.g. EssentialsX /ip)."
                  ))
               .defaultValue("ip %s"))
            .build()
      );
   private final Setting<Boolean> autoQuery = this.sg
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-query"))
                  .description("Automatically run the lookup command for every online player."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> interval = this.sg
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("query-interval"))
                  .description("Delay in seconds between automatic query rounds, to avoid spamming command cooldowns."))
               .defaultValue(5))
            .min(1)
            .max(60)
            .sliderRange(1, 60)
            .build()
      );
   private final Setting<Boolean> showUnknown = this.sg
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("show-unknown"))
                  .description("Show player names even when their IP has not been resolved yet (marked as ?)."))
               .defaultValue(true))
            .build()
      );
   private final Map<String, String> ips = new HashMap<>();
   private int tickCounter = 0;
   private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

   public IPlist() {
      super(
         AddonTemplate.OP_CATEGORY,
         "IPlist",
         "Shows every online player's real IP in the bottom-left corner. Requires OP permissions and a server-side IP lookup command (e.g. EssentialsX /ip). The client cannot see other players' IPs directly, so this module queries the server and parses the chat response."
      );
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         if (!this.mc.player.isCreativeLevelTwoOp()) {
            ChatUtils.warning("IPlist：需要 OP 权限，已关闭。", new Object[0]);
            this.toggle();
         } else {
            ChatUtils.info("IPlist：已开始查询全体玩家 IP。", new Object[0]);
            this.ips.clear();
            if ((Boolean)this.autoQuery.get()) {
               this.queryAll();
            }
         }
      }
   }

   public void onDeactivate() {
      this.ips.clear();
   }

   @EventHandler
   private void onTick(Post event) {
      if ((Boolean)this.autoQuery.get()) {
         if (++this.tickCounter >= (Integer)this.interval.get() * 20) {
            this.tickCounter = 0;
            this.queryAll();
         }
      }
   }

   private void queryAll() {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         Collection<PlayerListEntry> list = this.mc.getNetworkHandler().getPlayerList();
         if (list != null) {
            for (PlayerListEntry entry : list) {
               String name = entry.getProfile().name();
               if (name != null && !name.isEmpty()) {
                  String cmd = ((String)this.command.get()).trim();
                  if (cmd.startsWith("/")) {
                     cmd = cmd.substring(1);
                  }

                  cmd = cmd.replace("%s", name);
                  this.mc.getNetworkHandler().sendPacket(new CommandExecutionC2SPacket(cmd));
               }
            }
         }
      }
   }

   @EventHandler
   private void onMessage(ReceiveMessageEvent event) {
      Text msgText = event.getMessage();
      if (msgText != null) {
         String msg = msgText.getString();
         Matcher ipm = IPV4.matcher(msg);
         if (ipm.find()) {
            String ip = ipm.group();
            Collection<PlayerListEntry> list = this.mc.getNetworkHandler() != null ? this.mc.getNetworkHandler().getPlayerList() : null;
            if (list != null) {
               for (PlayerListEntry entry : list) {
                  String name = entry.getProfile().name();
                  if (name != null && !name.isEmpty() && msg.contains(name)) {
                     this.ips.put(name, ip);
                     break;
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.getNetworkHandler() != null) {
         Collection<PlayerListEntry> list = this.mc.getNetworkHandler().getPlayerList();
         if (list != null) {
            List<String> lines = new ArrayList<>();
            lines.add("[BPHack] IPlist");

            for (PlayerListEntry entry : list) {
               String name = entry.getProfile().name();
               if (name != null && !name.isEmpty()) {
                  String ip = this.ips.get(name);
                  if (ip != null) {
                     lines.add(name + ": " + ip);
                  } else if ((Boolean)this.showUnknown.get()) {
                     lines.add(name + ": ?");
                  }
               }
            }

            if (!lines.isEmpty()) {
               int lineHeight = 9 + 2;
               int totalH = lines.size() * lineHeight;
               int x = 4;
               int y = this.mc.getWindow().getScaledHeight() - totalH - 4;

               for (int i = 0; i < lines.size(); i++) {
                  String line = lines.get(i);
                  int color = i == 0 ? 5614335 : 16777215;
                  event.drawContext.drawText(this.mc.textRenderer, line, x, y, color, true);
                  y += lineHeight;
               }
            }
         }
      }
   }
}
