package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Arrays;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;

public class AutoRespawn extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> showMessage = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("show-message")).description("atTimeSendThisGroundAnti. thisDisappearOnlyhaveyoucanlookto."))
               .defaultValue(true))
            .build()
      );
   private final Setting<AutoRespawn.Mode> actionMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("action-mode"))
                  .description("afterRow'sOperationMode."))
               .defaultValue(AutoRespawn.Mode.Send_Messages))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("delay"))
                     .description("SendDisappear/PointCommandbefore'sDelay(Tick, 20 Tick = 1)."))
                  .defaultValue(20))
               .min(0)
               .sliderRange(0, 100)
               .visible(() -> this.actionMode.get() == AutoRespawn.Mode.Send_Messages))
            .build()
      );
   private final Setting<List<String>> messages = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)((meteordevelopment.meteorclient.settings.StringListSetting.Builder)new meteordevelopment.meteorclient.settings.StringListSetting.Builder()
                        .name("messages"))
                     .description("wantSend'sDisappearPointCommand(ifarePointCommand / OpenHead). Supportmany."))
                  .defaultValue(Arrays.asList("I will be back.", "/back")))
               .visible(() -> this.actionMode.get() == AutoRespawn.Mode.Send_Messages))
            .build()
      );
   private boolean wasDead = false;
   private boolean waitingToSend = false;
   private int delayTimer = 0;

   public AutoRespawn() {
      super(AddonTemplate.SC_CATEGORY, "AutoRespawn", "Auto-respawns on death and can run a custom respawn command or go to a set position.");
   }

   public void onActivate() {
      this.wasDead = false;
      this.waitingToSend = false;
      this.delayTimer = 0;
   }

   public void onDeactivate() {
      this.waitingToSend = false;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null) {
         if (this.mc.player.isDead()) {
            if (!this.wasDead) {
               this.mc
                  .player
                  .networkHandler
                  .sendPacket(new ClientStatusC2SPacket(net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
               if ((Boolean)this.showMessage.get()) {
                  this.info("AlreadyAuto.", new Object[0]);
               }

               if (this.actionMode.get() == AutoRespawn.Mode.Send_Messages && !((List)this.messages.get()).isEmpty()) {
                  this.waitingToSend = true;
                  this.delayTimer = (Integer)this.delay.get();
               }
            }

            this.wasDead = true;
         } else {
            this.wasDead = false;
         }

         if (this.waitingToSend) {
            if (this.delayTimer > 0) {
               this.delayTimer--;
            } else {
               for (String msg : (List)this.messages.get()) {
                  if (msg != null && !msg.trim().isEmpty()) {
                     ChatUtils.sendPlayerMsg(msg);
                  }
               }

               this.waitingToSend = false;
            }
         }
      }
   }

   public static enum Mode {
      None,
      Send_Messages;
   }
}
