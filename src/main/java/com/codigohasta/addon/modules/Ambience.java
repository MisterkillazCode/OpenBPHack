package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class Ambience extends Module {
   public static Ambience INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> filterEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("filter")).description("atupColor")).defaultValue(false)).build());
   public final Setting<SettingColor> filterColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("filter-color"))
                  .description("Color"))
               .defaultValue(new SettingColor(255, 255, 255, 20))
               .visible(this.filterEnabled::get))
            .build()
      );
   public final Setting<Boolean> worldColorEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("world-color")).description("ImageColor")).defaultValue(true)).build());
   public final Setting<SettingColor> worldColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("world-color-value"))
                  .description("Color"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.worldColorEnabled::get))
            .build()
      );
   public final Setting<Boolean> customTime = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("custom-time")).description("EnableCustomTime")).defaultValue(false)).build());
   public final Setting<Integer> time = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("time"))
                     .description("CustomTime (0-24000)"))
                  .defaultValue(0))
               .min(0)
               .max(24000)
               .sliderMax(24000)
               .visible(this.customTime::get))
            .build()
      );
   public final Setting<Boolean> fogEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("fog")).description("Color")).defaultValue(false)).build());
   public final Setting<SettingColor> fogColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("fog-color"))
                  .description("Color"))
               .defaultValue(new SettingColor(204, 136, 85))
               .visible(this.fogEnabled::get))
            .build()
      );
   public final Setting<Boolean> skyEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("sky")).description("SkyAirColor")).defaultValue(false)).build());
   public final Setting<SettingColor> skyColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("sky-color"))
                  .description("SkyAirColor"))
               .defaultValue(new SettingColor(0, 0, 0))
               .visible(this.skyEnabled::get))
            .build()
      );
   public final Setting<Boolean> cloudEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("cloud")).description("Color")).defaultValue(false)).build());
   public final Setting<SettingColor> cloudColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("cloud-color"))
                  .description("Color"))
               .defaultValue(new SettingColor(0, 0, 0))
               .visible(this.cloudEnabled::get))
            .build()
      );
   public final Setting<Boolean> dimensionColorEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("dimension-color")).description("DegreeBackgroundColor")).defaultValue(false)).build());
   public final Setting<SettingColor> dimensionColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("dimension-color-value"))
                  .description("DegreeColor"))
               .defaultValue(new SettingColor(0, 0, 0))
               .visible(this.dimensionColorEnabled::get))
            .build()
      );
   public final Setting<Boolean> fogDistance = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("fog-distance")).description("EnableCustomDistance")).defaultValue(false)).build());
   public final Setting<Double> fogStart = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("fog-start"))
                  .description("StartDistance"))
               .defaultValue(50.0)
               .min(0.0)
               .max(1000.0)
               .sliderMax(1000.0)
               .visible(this.fogDistance::get))
            .build()
      );
   public final Setting<Double> fogEnd = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("fog-end"))
                  .description("endDistance"))
               .defaultValue(100.0)
               .min(0.0)
               .max(1000.0)
               .sliderMax(1000.0)
               .visible(this.fogDistance::get))
            .build()
      );
   public final Setting<Boolean> fullBright = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("full-bright")).description("EnableViewEffect")).defaultValue(false)).build());
   public final Setting<Boolean> forceOverworld = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("force-overworld")).description("ForceUseSkyAirEffect")).defaultValue(false)).build());
   public final Setting<Boolean> customLuminance = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("custom-luminance")).description("EnableCustomBlockDegree")).defaultValue(false)).build());
   public final Setting<Integer> luminance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("luminance"))
                     .description("CustomDegree (0-15)"))
                  .defaultValue(15))
               .min(0)
               .max(15)
               .sliderMax(15)
               .visible(this.customLuminance::get))
            .build()
      );

   public Ambience() {
      super(AddonTemplate.CATEGORY, "Ambience", "Customises world ambience: fog, sky colour, world tint and time override.");
      INSTANCE = this;
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if ((Boolean)this.filterEnabled.get()) {
         event.drawContext
            .fill(0, 0, this.mc.getWindow().getScaledWidth(), this.mc.getWindow().getScaledHeight(), ((SettingColor)this.filterColor.get()).getPacked());
      }
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      if (event.packet instanceof WorldTimeUpdateS2CPacket && (Boolean)this.customTime.get()) {
         event.cancel();
      }
   }
}
