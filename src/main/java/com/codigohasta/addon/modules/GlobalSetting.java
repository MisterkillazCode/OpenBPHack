package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;

public class GlobalSetting extends Module {
   public static GlobalSetting INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRotation = this.settings.createGroup("Rotation");
   private final SettingGroup sgElytra = this.settings.createGroup("Elytra");
   public final Setting<Boolean> packetPlace = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("PacketPlace")).description("SendPackPut")).defaultValue(false)).build());
   public final Setting<Boolean> optimizedCalc = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("OptimizedCalc")).description("WaterSimple")).defaultValue(true)).build());
   public final Setting<GlobalSetting.SwingMode> placeSwing = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("PlaceSwing"))
                  .description("PutHandMode"))
               .defaultValue(GlobalSetting.SwingMode.Packet))
            .build()
      );
   public final Setting<GlobalSetting.SwingMode> attackSwing = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("AttackSwing"))
                  .description("AttackHandMode"))
               .defaultValue(GlobalSetting.SwingMode.Packet))
            .build()
      );
   public final Setting<GlobalSetting.HandMode> handMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("HandMode"))
                  .description("HandSelect"))
               .defaultValue(GlobalSetting.HandMode.MainHand))
            .build()
      );
   public final Setting<Boolean> noBadPackets = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("NoBadPackets")).description("AntiMethodPackSend")).defaultValue(false)).build());
   public final Setting<Boolean> clientSwitch = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("ClientSwitch")).description("ClientSwitch")).defaultValue(true)).build());
   public final Setting<Boolean> moveFix = this.sgRotation
      .add(((Builder)((Builder)((Builder)new Builder().name("1.21+")).description("highVersionHead(MoveFixMove)")).defaultValue(true)).build());
   public final Setting<Boolean> grimRotation = this.sgRotation
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("GrimRotation")).description("GrimModeHead")).defaultValue(true))
               .visible(() -> !(Boolean)this.moveFix.get()))
            .build()
      );
   public final Setting<Boolean> snapBack = this.sgRotation
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("SnapBack")).description("AutoHead")).defaultValue(true))
               .visible(() -> !(Boolean)this.moveFix.get()))
            .build()
      );
   public final Setting<Boolean> baritone = this.sgElytra
      .add(((Builder)((Builder)((Builder)new Builder().name("Baritone")).description("ConnectBaritone")).defaultValue(true)).build());
   public final Setting<Integer> elytraMinDamage = this.sgElytra
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ElytraMinDamage"))
                  .description("mostlittleDurabilityCheck"))
               .defaultValue(10))
            .min(0)
            .max(100)
            .build()
      );
   public final Setting<Integer> minFireworks = this.sgElytra
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("MinFireworks"))
                  .description("mostfewAmountCheck"))
               .defaultValue(10))
            .min(0)
            .max(64)
            .build()
      );

   public GlobalSetting() {
      super(AddonTemplate.CATEGORY, "GlobalSetting", "Shared settings used by the leaveshack combat modules: placing, swinging, rotation and packet options.");
      INSTANCE = this;
   }

   public static enum HandMode {
      MainHand,
      OffHand;
   }

   public static enum SwingMode {
      Both,
      Packet,
      Client,
      None;
   }
}
