package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;

public class ChatHider extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> openOpacity = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("DisplayTimeOpacity")).description("youOpenchatSkyBoxTime, chatSkyTextText'sOpacity (0-1)."))
            .defaultValue(1.0)
            .min(0.1)
            .max(1.0)
            .sliderMax(1.0)
            .build()
      );
   private double originalOpacity;

   public ChatHider() {
      super(AddonTemplate.CATEGORY, "ChatHider", "Hides chat for a set time; only shows messages containing a hit keyword, then displays them.");
   }

   public void onActivate() {
      this.originalOpacity = (Double)this.mc.options.getChatOpacity().getValue();
   }

   public void onDeactivate() {
      this.mc.options.getChatOpacity().setValue(this.originalOpacity > 0.0 ? this.originalOpacity : (Double)this.openOpacity.get());
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null) {
         if (this.isActive()) {
            if (this.mc.currentScreen instanceof ChatScreen) {
               this.mc.options.getChatOpacity().setValue((Double)this.openOpacity.get());
            } else {
               this.mc.options.getChatOpacity().setValue(0.0);
            }
         }
      }
   }
}
