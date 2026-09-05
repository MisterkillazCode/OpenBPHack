package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class SonarBypass extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<String> brandName = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("brand-name")).description("Install'sClientName.")).defaultValue("vanilla")).build());
   public final Setting<Boolean> blockFabric = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("block-fabric-payloads"))
                  .description("AutoBlockhave fabric meteor Mark's CustomPayload Pack."))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> strictPhysics = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("strict-physics-on-join"))
                  .description("EnterServiceTimeForceDisableandMethodMove, DefensebyThingLogicOut."))
               .defaultValue(true))
            .build()
      );
   private int joinTicks = 0;

   public SonarBypass() {
      super(AddonTemplate.CATEGORY, "SonarBypass", "Bypasses Sonar/whitelist single-client checks (1.21.11).");
   }

   public void onActivate() {
      this.joinTicks = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.strictPhysics.get() && this.mc.player.age < 60) {
            if (this.mc.player.isGliding()) {
               this.mc.player.stopGliding();
            }

            ItemStack chestStack = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (chestStack != null && chestStack.getItem().toString().contains("elytra") && this.mc.options.sneakKey.isPressed()) {
               this.mc.options.sneakKey.setPressed(false);
            }

            if (this.mc.player.input.playerInput.forward()) {
            }
         }
      }
   }
}
