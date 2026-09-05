package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public class RTsearch extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCoordinates = this.settings.createGroup("Coordinates");
   private final SettingGroup sgBiome = this.settings.createGroup("Biome");
   private final SettingGroup sgWebhook = this.settings.createGroup("Webhook");
   private final Setting<RTsearch.RTPMode> rtpMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("rtp-mode")).description("RTP mode: Coordinates or Biome."))
               .defaultValue(RTsearch.RTPMode.COORDINATES))
            .build()
      );
   private final Setting<Integer> targetX = this.sgCoordinates
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("target-x"))
                     .description("Target X coordinate."))
                  .defaultValue(0))
               .visible(() -> this.rtpMode.get() == RTsearch.RTPMode.COORDINATES))
            .build()
      );
   private final Setting<Integer> targetZ = this.sgCoordinates
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("target-z"))
                     .description("Target Z coordinate."))
                  .defaultValue(0))
               .visible(() -> this.rtpMode.get() == RTsearch.RTPMode.COORDINATES))
            .build()
      );
   private final Setting<String> distance = this.sgCoordinates
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("distance"))
                     .description("Distance to get within (supports k/m, e.g., 10k = 10000, 1.5m = 1500000)."))
                  .defaultValue("1000"))
               .visible(() -> this.rtpMode.get() == RTsearch.RTPMode.COORDINATES))
            .build()
      );
   private final Setting<String> rtpCommand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("rtp-command"))
                  .description("The command to send for teleporting (without slash). : rt, rtp, wild"))
               .defaultValue("rt"))
            .build()
      );
   private final Setting<RTsearch.MinecraftBiome> targetBiome = this.sgBiome
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("target-biome")).description("Target biome to find."))
                  .defaultValue(RTsearch.MinecraftBiome.PLAINS))
               .visible(() -> false))
            .build()
      );
   private final Setting<Boolean> disconnectOnReach = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("disconnect-on-reach"))
                  .description("Disconnect when reaching the target coordinates or finding the target biome."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> rtpDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("rtp-delay"))
                  .description("Delay between RTP attempts in seconds."))
               .defaultValue(3))
            .min(1)
            .max(30)
            .sliderMin(1)
            .sliderMax(10)
            .build()
      );
   private final Setting<Boolean> webhookEnabled = this.sgWebhook
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("webhook-enabled"))
                  .description("Enable webhook notifications."))
               .defaultValue(false))
            .build()
      );
   private final Setting<String> webhookUrl = this.sgWebhook
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("webhook-url"))
                     .description("Discord webhook URL."))
                  .defaultValue(""))
               .visible(this.webhookEnabled::get))
            .build()
      );
   private final Setting<Boolean> selfPing = this.sgWebhook
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("self-ping"))
                     .description("Ping yourself in the webhook message."))
                  .defaultValue(false))
               .visible(this.webhookEnabled::get))
            .build()
      );
   private final Setting<String> discordId = this.sgWebhook
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("discord-id"))
                     .description("Your Discord user ID for pinging."))
                  .defaultValue(""))
               .visible(() -> (Boolean)this.webhookEnabled.get() && (Boolean)this.selfPing.get()))
            .build()
      );
   private int tickTimer = 0;
   private boolean isRtping = false;
   private int rtpAttempts = 0;
   private BlockPos lastRtpPos = null;
   private double lastReportedDistance = -1.0;
   private int targetDistanceBlocks = 1000;
   private boolean biomeFound = false;
   private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

   public RTsearch() {
      super(
         AddonTemplate.CATEGORY,
         "RTsearch",
         "Automates random teleport searching: repeats /rtp until a target distance or biome is found, then optionally disconnects."
      );
   }

   public void onActivate() {
      this.tickTimer = (Integer)this.rtpDelay.get() * 20;
      this.isRtping = false;
      this.rtpAttempts = 0;
      this.lastRtpPos = null;
      this.lastReportedDistance = -1.0;
      this.biomeFound = false;
      if (this.rtpMode.get() == RTsearch.RTPMode.COORDINATES) {
         this.targetDistanceBlocks = this.parseDistance();
      }

      if (this.mc.player != null) {
         if (this.rtpMode.get() == RTsearch.RTPMode.COORDINATES) {
            double currentDist = this.getCurrentDistance();
            this.info("RTsearch started - target: (%d, %d)", new Object[]{this.targetX.get(), this.targetZ.get()});
            this.info("Distance: %s -> %d blocks", new Object[]{this.distance.get(), this.targetDistanceBlocks});
            this.info("Current: %.1f blocks away", new Object[]{currentDist});
            if (currentDist <= this.targetDistanceBlocks) {
               this.info("Already close enough!", new Object[0]);
               this.toggle();
            }
         } else {
            this.info("RTsearch started - Biome Finder mode", new Object[0]);
            this.info("Target biome: %s", new Object[]{((RTsearch.MinecraftBiome)this.targetBiome.get()).getDisplayName()});
         }
      }
   }

   public void onDeactivate() {
      if (this.rtpMode.get() == RTsearch.RTPMode.COORDINATES) {
         this.info("Stopped after %d attempts", new Object[]{this.rtpAttempts});
      } else {
         this.info("Biome finder stopped after %d attempts", new Object[]{this.rtpAttempts});
      }

      this.isRtping = false;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.rtpMode.get() == RTsearch.RTPMode.COORDINATES) {
            this.handleCoordinatesMode();
         } else {
            this.handleBiomeMode();
         }

         this.tickTimer++;
         if (this.isRtping && this.tickTimer >= 100) {
            this.isRtping = false;
         }

         if (this.tickTimer >= (Integer)this.rtpDelay.get() * 20 && !this.isRtping) {
            this.performRTP();
            this.tickTimer = 0;
         }
      }
   }

   private void handleCoordinatesMode() {
      double currentDistance = this.getCurrentDistance();
      if (this.isNearTarget(currentDistance)) {
         this.info("Done! %.1f blocks away (target: %d)", new Object[]{currentDistance, this.targetDistanceBlocks});
         if ((Boolean)this.webhookEnabled.get()) {
            this.sendWebhook(
               "Target Reached!",
               String.format(
                  "Got to %d, %d using /\" + rtpCommand.get() + \"\\\\nDistance: %.1f/%d blocks\\nAttempts: %d",
                  this.targetX.get(),
                  this.targetZ.get(),
                  currentDistance,
                  this.targetDistanceBlocks,
                  this.rtpAttempts
               ),
               65280
            );
         }

         if ((Boolean)this.disconnectOnReach.get()) {
            this.info("Disconnecting...", new Object[0]);
            if (this.mc.world != null) {
               this.mc.world.disconnect(Text.of("Disconnected"));
            }
         }

         this.toggle();
      } else {
         if (this.tickTimer % 20 == 0 && Math.abs(currentDistance - this.lastReportedDistance) > 100.0) {
         }
      }
   }

   private void handleBiomeMode() {
      if (this.biomeFound) {
         this.info("Target biome found: %s", new Object[]{((RTsearch.MinecraftBiome)this.targetBiome.get()).getDisplayName()});
         if ((Boolean)this.webhookEnabled.get()) {
            this.sendWebhook(
               "Biome Found!",
               String.format(
                  "Found %s biome using /rt!\\nAttempts: %d\\nPosition: %d, %d, %d",
                  ((RTsearch.MinecraftBiome)this.targetBiome.get()).getDisplayName(),
                  this.rtpAttempts,
                  this.mc.player.getBlockPos().getX(),
                  this.mc.player.getBlockPos().getY(),
                  this.mc.player.getBlockPos().getZ()
               ),
               65280
            );
         }

         if ((Boolean)this.disconnectOnReach.get()) {
            this.info("Disconnecting...", new Object[0]);
            this.disconnectWithMessage("RTsearch: found requested biome");
         }

         this.toggle();
      } else if (this.isInTargetBiome()) {
         this.biomeFound = true;
      }
   }

   private boolean isInTargetBiome() {
      if (this.mc.world != null && this.mc.player != null) {
         BlockPos pos = this.mc.player.getBlockPos();
         String biomeId = this.getBiomeIdAt(pos);
         return biomeId == null ? false : biomeId.equals(((RTsearch.MinecraftBiome)this.targetBiome.get()).getId());
      } else {
         return false;
      }
   }

   private String getCurrentBiome() {
      if (this.mc.world != null && this.mc.player != null) {
         BlockPos pos = this.mc.player.getBlockPos();
         String biomeId = this.getBiomeIdAt(pos);
         if (biomeId == null) {
            return "Unknown";
         } else {
            for (RTsearch.MinecraftBiome minecraftBiome : RTsearch.MinecraftBiome.values()) {
               if (minecraftBiome.getId().equals(biomeId)) {
                  return minecraftBiome.getDisplayName();
               }
            }

            return this.toDisplayName(biomeId);
         }
      } else {
         return "Unknown";
      }
   }

   private String getBiomeIdAt(BlockPos pos) {
      if (this.mc.world == null) {
         return null;
      } else {
         Biome biome = (Biome)this.mc.world.getBiome(pos).value();
         if (biome == null) {
            return null;
         } else {
            Identifier id = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getId(biome);
            return id != null ? id.toString() : null;
         }
      }
   }

   private String toDisplayName(String id) {
      if (id != null && !id.isEmpty()) {
         String raw = id.contains(":") ? id.substring(id.indexOf(":") + 1) : id;
         String[] parts = raw.split("_");
         StringBuilder b = new StringBuilder();

         for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
               b.append(Character.toUpperCase(p.charAt(0)));
               if (p.length() > 1) {
                  b.append(p.substring(1));
               }

               if (i < parts.length - 1) {
                  b.append(' ');
               }
            }
         }

         return b.toString();
      } else {
         return "Unknown";
      }
   }

   private void disconnectWithMessage(String message) {
      try {
         if (this.mc != null) {
            if (this.mc.getNetworkHandler() != null && this.mc.getNetworkHandler().getConnection() != null) {
               this.mc.getNetworkHandler().getConnection().disconnect(Text.literal(message));
               return;
            }

            if (this.mc.player != null && this.mc.player.networkHandler != null && this.mc.player.networkHandler.getConnection() != null) {
               this.mc.player.networkHandler.getConnection().disconnect(Text.literal(message));
               return;
            }

            if (this.mc.world != null) {
               this.mc.world.disconnect(Text.of("Disconnected"));
            }
         }
      } catch (Exception var3) {
         if (this.mc != null && this.mc.world != null) {
            this.mc.world.disconnect(Text.of("Disconnected"));
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (event.packet instanceof PlayerPositionLookS2CPacket && this.mc.player != null) {
         this.isRtping = false;
         BlockPos currentPos = this.mc.player.getBlockPos();
         if (this.lastRtpPos == null || !currentPos.equals(this.lastRtpPos)) {
            this.rtpAttempts++;
            this.lastRtpPos = currentPos;
            if (this.rtpMode.get() == RTsearch.RTPMode.COORDINATES) {
               double distance = this.getCurrentDistance();
               this.info("RTP %d done - dist: %.1f", new Object[]{this.rtpAttempts, distance});
               this.lastReportedDistance = distance;
            } else {
               String biome = this.getCurrentBiome();
               this.info("RTP %d done - biome: %s", new Object[]{this.rtpAttempts, biome});
               if (this.isInTargetBiome()) {
                  this.biomeFound = true;
               }
            }
         }
      }
   }

   private void performRTP() {
      if (this.mc.player != null) {
         this.isRtping = true;
         String cmd = ((String)this.rtpCommand.get()).trim();
         if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
         }

         if (this.mc.getNetworkHandler() != null) {
            this.mc.getNetworkHandler().sendChatCommand(cmd);
         } else {
            ChatUtils.sendPlayerMsg("/" + cmd);
         }

         if (this.rtpMode.get() == RTsearch.RTPMode.COORDINATES) {
         }
      }
   }

   private boolean isNearTarget() {
      return this.isNearTarget(this.getCurrentDistance());
   }

   private boolean isNearTarget(double currentDistance) {
      return currentDistance <= this.targetDistanceBlocks;
   }

   private double getCurrentDistance() {
      if (this.mc.player == null) {
         return Double.MAX_VALUE;
      } else {
         BlockPos pos = this.mc.player.getBlockPos();
         double dx = pos.getX() - (Integer)this.targetX.get();
         double dz = pos.getZ() - (Integer)this.targetZ.get();
         return Math.sqrt(dx * dx + dz * dz);
      }
   }

   private int parseDistance() {
      String dist = ((String)this.distance.get()).toLowerCase().trim();
      if (dist.isEmpty()) {
         return 1000;
      } else {
         try {
            if (dist.endsWith("k")) {
               String num = dist.substring(0, dist.length() - 1).trim();
               double val = Double.parseDouble(num);
               return (int)(val * 1000.0);
            } else if (dist.endsWith("m")) {
               String num = dist.substring(0, dist.length() - 1).trim();
               double val = Double.parseDouble(num);
               return (int)(val * 1000000.0);
            } else {
               return Integer.parseInt(dist);
            }
         } catch (NumberFormatException var5) {
            return 1000;
         }
      }
   }

   private void sendWebhook(String title, String description, int color) {
      if ((Boolean)this.webhookEnabled.get() && !((String)this.webhookUrl.get()).isEmpty()) {
         CompletableFuture.runAsync(
            () -> {
               try {
                  String serverInfo = this.mc.getCurrentServerEntry() != null ? this.mc.getCurrentServerEntry().address : "Unknown Server";
                  String messageContent = "";
                  if ((Boolean)this.selfPing.get() && !((String)this.discordId.get()).trim().isEmpty()) {
                     messageContent = String.format("<@%s>", ((String)this.discordId.get()).trim());
                  }

                  String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                  String jsonPayload = String.format(
                     "{\n    \"content\": \"%s\",\n    \"username\": \"RTsearch Webhook\",\n    \"avatar_url\": \"https://i.imgur.com/OL2y1cr.png\",\n    \"embeds\": [{\n        \"title\": \"\ud83c\udfaf RTsearch Alert\",\n        \"description\": \"%s\",\n        \"color\": %d,\n        \"fields\": [\n            {\n                \"name\": \"Status\",\n                \"value\": \"RTP /rt\",\n                \"inline\": true\n            },\n            {\n                \"name\": \"Server\",\n                \"value\": \"%s\",\n                \"inline\": true\n            },\n            {\n                \"name\": \"Time\",\n                \"value\": \"<t:%d:R>\",\n                \"inline\": true\n            }\n        ],\n        \"footer\": {\n            \"text\": \"RTsearch Module\"\n        },\n        \"timestamp\": \"%sZ\"\n    }]\n}",
                     messageContent.replace("\"", "\\\""),
                     description.replace("\"", "\\\"").replace("\\n", "\\n"),
                     color,
                     serverInfo.replace("\"", "\\\""),
                     System.currentTimeMillis() / 1000L,
                     timestamp
                  );
                  HttpRequest request = HttpRequest.newBuilder()
                     .uri(URI.create((String)this.webhookUrl.get()))
                     .header("Content-Type", "application/json")
                     .POST(BodyPublishers.ofString(jsonPayload))
                     .timeout(Duration.ofSeconds(30L))
                     .build();
                  this.httpClient.send(request, BodyHandlers.discarding());
               } catch (Exception var8) {
               }
            }
         );
      }
   }

   public WWidget getWidget(GuiTheme theme) {
      WTable table = theme.table();
      table.add(theme.label("Biome Picker:"));
      WLabel current = (WLabel)table.add(theme.label(((RTsearch.MinecraftBiome)this.targetBiome.get()).getDisplayName())).expandX().widget();
      WButton open = (WButton)table.add(theme.button("Select")).widget();
      open.action = () -> {
         if (this.rtpMode.get() == RTsearch.RTPMode.BIOME) {
            this.mc.setScreen(new RTsearch.BiomePickerScreen(theme, current));
         }
      };
      table.row();
      return table;
   }

   private class BiomePickerScreen extends WindowScreen {
      private WTable listTable;
      private WTextBox searchBox;
      private final WLabel currentLabel;

      public BiomePickerScreen(GuiTheme theme, WLabel currentLabel) {
         super(theme, "Select Biome");
         this.currentLabel = currentLabel;
      }

      public void initWidgets() {
         this.searchBox = (WTextBox)this.add(this.theme.textBox("")).expandX().widget();
         this.searchBox.setFocused(true);
         this.searchBox.action = this::reloadList;
         this.add(this.theme.horizontalSeparator()).expandX();
         this.listTable = (WTable)this.add(this.theme.table()).expandX().widget();
         this.reloadList();
      }

      private void reloadList() {
         this.listTable.clear();
         String query = this.searchBox.get().trim().toLowerCase();

         for (RTsearch.MinecraftBiome biome : RTsearch.MinecraftBiome.values()) {
            String name = biome.getDisplayName();
            if (query.isEmpty() || name.toLowerCase().contains(query)) {
               this.listTable.add(this.theme.label(name)).expandX();
               WButton select = (WButton)this.listTable.add(this.theme.button("Use")).widget();
               select.action = () -> {
                  RTsearch.this.targetBiome.set(biome);
                  if (this.currentLabel != null) {
                     this.currentLabel.set(biome.getDisplayName());
                  }

                  RTsearch.this.mc.setScreen(null);
               };
               this.listTable.row();
            }
         }
      }
   }

   public static enum MinecraftBiome {
      PLAINS("Plains", "minecraft:plains"),
      SUNFLOWER_PLAINS("Sunflower Plains", "minecraft:sunflower_plains"),
      SNOWY_PLAINS("Snowy Plains", "minecraft:snowy_plains"),
      FOREST("Forest", "minecraft:forest"),
      FLOWER_FOREST("Flower Forest", "minecraft:flower_forest"),
      BIRCH_FOREST("Birch Forest", "minecraft:birch_forest"),
      OLD_GROWTH_BIRCH_FOREST("Old Growth Birch Forest", "minecraft:old_growth_birch_forest"),
      DARK_FOREST("Dark Forest", "minecraft:dark_forest"),
      OAK_AND_BIRCH_FOREST("Oak and Birch Forest", "minecraft:oak_and_birch_forest"),
      TAIGA("Taiga", "minecraft:taiga"),
      SNOWY_TAIGA("Snowy Taiga", "minecraft:snowy_taiga"),
      OLD_GROWTH_SPRUCE_TAIGA("Old Growth Spruce Taiga", "minecraft:old_growth_spruce_taiga"),
      OLD_GROWTH_PINE_TAIGA("Old Growth Pine Taiga", "minecraft:old_growth_pine_taiga"),
      JUNGLE("Jungle", "minecraft:jungle"),
      SPARSE_JUNGLE("Sparse Jungle", "minecraft:sparse_jungle"),
      BAMBOO_JUNGLE("Bamboo Jungle", "minecraft:bamboo_jungle"),
      DESERT("Desert", "minecraft:desert"),
      SAVANNA("Savanna", "minecraft:savanna"),
      SAVANNA_PLATEAU("Savanna Plateau", "minecraft:savanna_plateau"),
      WINDSWEPT_SAVANNA("Windswept Savanna", "minecraft:windswept_savanna"),
      BADLANDS("Badlands", "minecraft:badlands"),
      ERODED_BADLANDS("Eroded Badlands", "minecraft:eroded_badlands"),
      WOODED_BADLANDS("Wooded Badlands", "minecraft:wooded_badlands"),
      SWAMP("Swamp", "minecraft:swamp"),
      MANGROVE_SWAMP("Mangrove Swamp", "minecraft:mangrove_swamp"),
      BEACH("Beach", "minecraft:beach"),
      SNOWY_SLOPES("Snowy Slopes", "minecraft:snowy_slopes"),
      JAGGED_PEAKS("Jagged Peaks", "minecraft:jagged_peaks"),
      FROZEN_PEAKS("Frozen Peaks", "minecraft:frozen_peaks"),
      STONY_PEAKS("Stony Peaks", "minecraft:stony_peaks"),
      OCEAN("Ocean", "minecraft:ocean"),
      WARM_OCEAN("Warm Ocean", "minecraft:warm_ocean"),
      LUKEWARM_OCEAN("Lukewarm Ocean", "minecraft:lukewarm_ocean"),
      COLD_OCEAN("Cold Ocean", "minecraft:cold_ocean"),
      FROZEN_OCEAN("Frozen Ocean", "minecraft:frozen_ocean"),
      DEEP_OCEAN("Deep Ocean", "minecraft:deep_ocean"),
      DEEP_WARM_OCEAN("Deep Warm Ocean", "minecraft:deep_warm_ocean"),
      DEEP_LUKEWARM_OCEAN("Deep Lukewarm Ocean", "minecraft:deep_lukewarm_ocean"),
      DEEP_COLD_OCEAN("Deep Cold Ocean", "minecraft:deep_cold_ocean"),
      DEEP_FROZEN_OCEAN("Deep Frozen Ocean", "minecraft:deep_frozen_ocean"),
      RIVER("River", "minecraft:river"),
      FROZEN_RIVER("Frozen River", "minecraft:frozen_river"),
      MUSHROOM_FIELDS("Mushroom Fields", "minecraft:mushroom_fields"),
      DRIPSTONE_CAVES("Dripstone Caves", "minecraft:dripstone_caves"),
      LUSH_CAVES("Lush Caves", "minecraft:lush_caves"),
      DEEP_DARK("Deep Dark", "minecraft:deep_dark"),
      NETHER_WASTES("Nether Wastes", "minecraft:nether_wastes"),
      SOUL_SAND_VALLEY("Soul Sand Valley", "minecraft:soul_sand_valley"),
      CRIMSON_FOREST("Crimson Forest", "minecraft:crimson_forest"),
      WARPED_FOREST("Warped Forest", "minecraft:warped_forest"),
      BASALT_DELTAS("Basalt Deltas", "minecraft:basalt_deltas"),
      THE_END("The End", "minecraft:the_end"),
      END_HIGHLANDS("End Highlands", "minecraft:end_highlands"),
      END_MIDLANDS("End Midlands", "minecraft:end_midlands"),
      SMALL_END_ISLANDS("Small End Islands", "minecraft:small_end_islands"),
      END_BARRENS("End Barrens", "minecraft:end_barrens"),
      CAVES("Caves", "minecraft:caves"),
      GROVE("Grove", "minecraft:grove"),
      MEADOW("Meadow", "minecraft:meadow"),
      CHERRY_GROVE("Cherry Grove", "minecraft:cherry_grove");

      private final String displayName;
      private final String id;

      private MinecraftBiome(String displayName, String id) {
         this.displayName = displayName;
         this.id = id;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getId() {
         return this.id;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }

   public static enum RTPMode {
      COORDINATES("Coordinates"),
      BIOME("Biome");

      private final String displayName;

      private RTPMode(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }
}
