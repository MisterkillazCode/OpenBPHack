package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BPHackWorldStats extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgStats = this.settings.createGroup("Statistics");
   private final SettingGroup sgVisual = this.settings.createGroup("ViewSystem");
   private final Setting<Boolean> showTicks = this.sgGeneral.add(((Builder)((Builder)new Builder().name("Tick(f***)")).defaultValue(true)).build());
   private final Setting<Boolean> showDays = this.sgGeneral.add(((Builder)((Builder)new Builder().name("SpawnSkyNumber")).defaultValue(true)).build());
   private final Setting<Boolean> showBiome = this.sgGeneral.add(((Builder)((Builder)new Builder().name("currentMobGroup")).defaultValue(true)).build());
   private final Setting<Boolean> showWeather = this.sgGeneral.add(((Builder)((Builder)new Builder().name("SkyAirStatus")).defaultValue(true)).build());
   private final Setting<Boolean> showCoords = this.sgGeneral.add(((Builder)((Builder)new Builder().name("DegreeMark")).defaultValue(true)).build());
   private final Setting<Boolean> showPlaytime = this.sgStats.add(((Builder)((Builder)new Builder().name("Timelong")).defaultValue(true)).build());
   private final Setting<Boolean> showDistance = this.sgStats.add(((Builder)((Builder)new Builder().name("MoveDistance")).defaultValue(true)).build());
   private final Setting<Boolean> showKills = this.sgStats.add(((Builder)((Builder)new Builder().name("StrikekillNumber")).defaultValue(true)).build());
   private final Setting<Boolean> showDeaths = this.sgStats.add(((Builder)((Builder)new Builder().name("DeathTimesNumber")).defaultValue(true)).build());
   private final Setting<Boolean> showBreaks = this.sgStats.add(((Builder)((Builder)new Builder().name("MineNumber")).defaultValue(true)).build());
   private final Setting<Double> textX = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("TextText X Mark"))
            .defaultValue(15.0)
            .min(0.0)
            .sliderMax(2000.0)
            .build()
      );
   private final Setting<Double> textY = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("TextText Y Mark"))
            .defaultValue(15.0)
            .min(0.0)
            .sliderMax(2000.0)
            .build()
      );
   private final Setting<Double> textScale = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("TextTextScale"))
            .defaultValue(1.0)
            .min(0.1)
            .sliderMax(3.0)
            .build()
      );
   private final Setting<SettingColor> textColor = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("TextTextColor"))
            .defaultValue(new SettingColor(55, 135, 255))
            .build()
      );
   private final Setting<Boolean> textShadow = this.sgVisual.add(((Builder)((Builder)new Builder().name("TextTextShadow")).defaultValue(true)).build());
   private final Setting<Double> bgX = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("Background X Mark"))
            .defaultValue(10.0)
            .min(0.0)
            .sliderMax(2000.0)
            .build()
      );
   private final Setting<Double> bgY = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("Background Y Mark"))
            .defaultValue(10.0)
            .min(0.0)
            .sliderMax(2000.0)
            .build()
      );
   private final Setting<Double> bgWidth = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("BackgroundWidth"))
            .defaultValue(150.0)
            .min(10.0)
            .sliderMax(1000.0)
            .build()
      );
   private final Setting<Double> bgHeight = this.sgVisual
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("BackgroundHeight"))
            .defaultValue(180.0)
            .min(10.0)
            .sliderMax(1000.0)
            .build()
      );
   private int syncTimer = 0;
   private int cachedBlocksMined = 0;
   private final List<BPHackWorldStats.Particle> particles = new ArrayList<>();
   private final Random random = new Random();

   public BPHackWorldStats() {
      super(
         AddonTemplate.SC_CATEGORY,
         "BPHackWorldStats",
         "Displays current world data and stats (players, entities, etc.). Some stats need manual background setup."
      );
   }

   public void onActivate() {
      this.requestStats();
      this.particles.clear();

      for (int i = 0; i < 40; i++) {
         this.particles.add(new BPHackWorldStats.Particle(this.random.nextDouble() * 200.0, this.random.nextDouble() * 100.0));
      }
   }

   private void requestStats() {
      if (this.mc.getNetworkHandler() != null && this.mc.player != null) {
         this.mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(Mode.REQUEST_STATS));
         this.updateCachedBlocksMined();
      }
   }

   private void updateCachedBlocksMined() {
      if (this.mc.player != null) {
         StatHandler stats = this.mc.player.getStatHandler();
         int total = 0;

         for (Block block : Registries.BLOCK) {
            total += stats.getStat(Stats.MINED.getOrCreateStat(block));
         }

         this.cachedBlocksMined = total;
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null) {
         if (this.syncTimer++ >= 200) {
            this.requestStats();
            this.syncTimer = 0;
         }

         for (BPHackWorldStats.Particle p : this.particles) {
            p.y += 0.9;
            if (p.y > 1000.0) {
               p.y = 0.0;
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         List<String> lines = this.buildDisplayLines();
         if (!lines.isEmpty()) {
            double bX = (Double)this.bgX.get();
            double bY = (Double)this.bgY.get();
            double bW = (Double)this.bgWidth.get();
            double bH = (Double)this.bgHeight.get();
            Renderer2D.COLOR.begin();
            this.renderBackground(bX, bY, bW, bH);
            Renderer2D.COLOR.render();
            TextRenderer text = TextRenderer.get();
            double tX = (Double)this.textX.get();
            double tY = (Double)this.textY.get();
            double tS = (Double)this.textScale.get();
            Color c = this.getThemeTextColor();
            text.begin(tS);

            for (String line : lines) {
               text.render(line, tX / tS, tY / tS, c, (Boolean)this.textShadow.get());
               tY += (text.getHeight() + 2.0) * tS;
            }

            text.end();
         }
      }
   }

   private List<String> buildDisplayLines() {
      List<String> lines = new ArrayList<>();
      StatHandler stats = this.mc.player.getStatHandler();
      if ((Boolean)this.showTicks.get()) {
         long dayTime = this.mc.world.getTimeOfDay() % 24000L;
         lines.add(String.format("f***Tick: %d", dayTime));
      }

      if ((Boolean)this.showDays.get()) {
         long worldDays = this.mc.world.getTimeOfDay() / 24000L;
         lines.add("SpawnSkyNumber:" + worldDays + "Sky");
      }

      if ((Boolean)this.showBiome.get()) {
         Identifier biomeId = ((RegistryKey)this.mc.world.getBiome(this.mc.player.getBlockPos()).getKey().get()).getValue();
         String transKey = "biome." + biomeId.toString().replace(":", ".");
         String localBiomeName = Text.translatable(transKey).getString();
         lines.add("MobGroup:" + localBiomeName);
      }

      if ((Boolean)this.showWeather.get()) {
         String w = this.mc.world.isThundering() ? "⛈" : (this.mc.world.isRaining() ? "down \ud83c\udf27" : "☀");
         lines.add("SkyAir:" + w);
      }

      if ((Boolean)this.showCoords.get()) {
         double px = this.mc.player.getX();
         double py = this.mc.player.getY();
         double pz = this.mc.player.getZ();
         boolean inNether = this.mc.world.getRegistryKey().getValue().getPath().contains("nether");
         lines.add(String.format(": %.1f, %.1f, %.1f", inNether ? px * 8.0 : px, py, inNether ? pz * 8.0 : pz));
         lines.add(String.format("down: %.1f, %.1f, %.1f", inNether ? px : px / 8.0, py, inNether ? pz : pz / 8.0));
      }

      if ((Boolean)this.showPlaytime.get()) {
         int pt = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
         int hrs = pt / 72000;
         int mins = pt % 72000 / 1200;
         lines.add(String.format("Timelong: %dh %dm", hrs, mins));
      }

      if ((Boolean)this.showDistance.get()) {
         long cm = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM))
            + stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM))
            + stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM));
         lines.add(String.format("Distance: %.1f km", cm / 100000.0));
      }

      if ((Boolean)this.showKills.get()) {
         int pk = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAYER_KILLS));
         int mk = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS));
         lines.add("Strikekill: Player[" + pk + "] / Thing[" + mk + "]");
      }

      if ((Boolean)this.showDeaths.get()) {
         lines.add("DeathTimesNumber:" + stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS)));
      }

      if ((Boolean)this.showBreaks.get()) {
         lines.add("MineNumber:" + this.cachedBlocksMined);
      }

      return lines;
   }

   private void renderBackground(double x, double y, double w, double h) {
      Renderer2D.COLOR.quad(x, y, w, h, new Color(0, 220, 255, 200), new Color(0, 220, 255, 200), new Color(150, 80, 255, 200), new Color(150, 80, 255, 200));
      Renderer2D.COLOR.quad(x + 2.0, y + 2.0, w - 4.0, h - 4.0, new Color(10, 14, 26, 200));

      for (BPHackWorldStats.Particle p : this.particles) {
         double px = p.x % w;
         double py = p.y % h;
         Renderer2D.COLOR.quad(x + 4.0 + px, y + 4.0 + py, 1.5, 5.0, new Color(0, 220, 255, 120));
      }
   }

   private Color getThemeTextColor() {
      return (Color)this.textColor.get();
   }

   private static class Particle {
      double x;
      double y;

      Particle(double x, double y) {
         this.x = x;
         this.y = y;
      }
   }
}
