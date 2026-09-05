package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.lang.reflect.Field;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class InfiniteChat extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> infiniteChatBox = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("InfinitechatSkyBox")).description("RemovechatSky256TextLimitSystem (PowerMode)."))
               .defaultValue(true))
            .build()
      );
   private boolean isHooked = false;

   public InfiniteChat() {
      super(AddonTemplate.CATEGORY, "InfiniteChat", "Removes the chat character limit so long commands can be sent.");
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if ((Boolean)this.infiniteChatBox.get()) {
         if (event.screen instanceof ChatScreen) {
            this.unlockChatLength((ChatScreen)event.screen);
         }
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if ((Boolean)this.infiniteChatBox.get()) {
         if (this.mc.currentScreen instanceof ChatScreen) {
            this.unlockChatLength((ChatScreen)this.mc.currentScreen);
         } else {
            this.isHooked = false;
         }
      }
   }

   private void unlockChatLength(ChatScreen screen) {
      if (screen != null) {
         try {
            Field[] fields = ChatScreen.class.getDeclaredFields();

            for (Field field : fields) {
               try {
                  field.setAccessible(true);
                  if (field.get(screen) instanceof TextFieldWidget inputField) {
                     inputField.setMaxLength(32767);
                     this.isHooked = true;
                  }
               } catch (Exception var9) {
               }
            }
         } catch (Exception var10) {
            var10.printStackTrace();
         }
      }
   }
}
