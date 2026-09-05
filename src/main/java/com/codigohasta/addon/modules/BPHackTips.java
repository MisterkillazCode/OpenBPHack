package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.alien.AlienFriendManager;
import com.codigohasta.addon.utils.alien.AlienInventoryUtil;
import com.codigohasta.addon.utils.alien.AlienPopManager;
import com.codigohasta.addon.utils.alien.AlienTimer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;

public class BPHackTips extends Module {
   public static BPHackTips INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgAppearance = this.settings.createGroup("outside");
   private final Setting<Boolean> visualRange = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("visual-range")).description("Notify when players enter/leave your visual range."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> friends = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("friends")).description("Also notify when friends enter/leave your visual range."))
                  .defaultValue(false))
               .visible(this.visualRange::get))
            .build()
      );
   private final Setting<Boolean> popCounter = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("pop-counter")).description("Reports how many totems a player popped when they die."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> deathCoords = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("death-coords")).description("Records your death coordinates in chat.")).defaultValue(true)).build()
      );
   private final Setting<Boolean> serverLag = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("server-lag")).description("Shows server not responding warning on screen.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> lagBack = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("lag-back")).description("Shows lagback countdown on screen.")).defaultValue(true)).build());
   private final Setting<Boolean> potion = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("potion")).description("Shows potion effect durations on screen.")).defaultValue(true)).build());
   private final Setting<Boolean> resistanceLevelCheck = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("resistance-level-check")).description("Only show resistance when amplifier > 0."))
                  .defaultValue(true))
               .visible(this.potion::get))
            .build()
      );
   private final Setting<Boolean> chinese = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("chinese")).description("Use Chinese language for all tip messages.")).defaultValue(true)).build());
   private final Setting<Double> textScale = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("text-scale"))
               .description("Scale of HUD warning and potion text."))
            .defaultValue(1.5)
            .min(0.5)
            .max(5.0)
            .sliderMax(5.0)
            .build()
      );
   private final Setting<SettingColor> warningColor = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("warning-color"))
               .description("Color of the server lag / lagback warning text."))
            .defaultValue(new SettingColor(55, 135, 255))
            .build()
      );
   private final Setting<SettingColor> potionBaseColor = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("potion-base-color"))
               .description("Base color for potion display (individual colors still apply)."))
            .defaultValue(new SettingColor(230, 241, 251))
            .build()
      );
   private final Setting<Boolean> potionShadow = this.sgAppearance
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("potion-shadow")).description("Draw shadow behind potion display text."))
                  .defaultValue(true))
               .visible(this.potion::get))
            .build()
      );
   private final Setting<Integer> warningX = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("warning-x"))
                  .description("X position for warning text (-1 = auto center)."))
               .defaultValue(-1))
            .min(-1)
            .sliderMax(1920)
            .build()
      );
   private final Setting<Integer> warningY = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("warning-y"))
                  .description("Y position for warning text."))
               .defaultValue(19))
            .min(0)
            .sliderMax(1080)
            .build()
      );
   private final Setting<Integer> potionX = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("potion-x"))
                  .description("X position for potion display (-1 = auto center)."))
               .defaultValue(-1))
            .min(-1)
            .sliderMax(1920)
            .build()
      );
   private final Setting<Integer> potionY = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("potion-y"))
                  .description("Y position for potion text (-1 = default center+9)."))
               .defaultValue(-1))
            .min(-1)
            .sliderMax(1080)
            .build()
      );
   private final Setting<Integer> yOffset = this.sgAppearance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("y-offset"))
                     .description("Fine-tune potion Y offset (applied on top of potion-y)."))
                  .defaultValue(0))
               .min(-200)
               .max(200)
               .sliderMax(200)
               .visible(this.potion::get))
            .build()
      );
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final AlienTimer lagTimer = new AlienTimer();
   private final AlienTimer lagBackTimer = new AlienTimer();
   private final AlienPopManager popManager = new AlienPopManager();
   private final AlienFriendManager friendManager = new AlienFriendManager();
   private final List<PlayerEntity> deadPlayers = new ArrayList<>();
   int turtles = 0;

   public BPHackTips() {
      super(
         AddonTemplate.CATEGORY,
         "BPHackTips",
         "On-screen notifications: visual range alerts, totem pop counter, death coordinates, server lag and potion timers."
      );
      INSTANCE = this;
   }

   public void onActivate() {
      this.lagTimer.reset();
      this.lagBackTimer.reset();
      this.deadPlayers.clear();
   }

   @EventHandler
   private void onEntityAdded(EntityAddedEvent event) {
      if ((Boolean)this.visualRange.get()) {
         if (event.entity instanceof PlayerEntity) {
            if (event.entity.getDisplayName() != null && event.entity != this.mc.player) {
               String playerName = event.entity.getDisplayName().getString();
               boolean isFriend = this.friendManager.isFriend(playerName);
               if (!isFriend || (Boolean)this.friends.get()) {
                  String msg = this.chinese.get() ? playerName + "EnteryourViewRange" : playerName + " entered your visual range.";
                  ChatUtils.sendMsg(event.entity.getId() + 777, Formatting.GRAY, msg, new Object[0]);
                  if (this.mc.world != null) {
                     this.mc
                        .world
                        .playSound(this.mc.player, this.mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onEntityRemoved(EntityRemovedEvent event) {
      if ((Boolean)this.visualRange.get()) {
         if (event.entity instanceof PlayerEntity) {
            if (event.entity.getDisplayName() != null && event.entity != this.mc.player) {
               String playerName = event.entity.getDisplayName().getString();
               boolean isFriend = this.friendManager.isFriend(playerName);
               if (!isFriend || (Boolean)this.friends.get()) {
                  String msg = this.chinese.get() ? playerName + "OpenyourViewRange" : playerName + " left your visual range.";
                  ChatUtils.sendMsg(event.entity.getId() + 777, Formatting.GRAY, msg, new Object[0]);
                  if (this.mc.world != null) {
                     this.mc
                        .world
                        .playSound(this.mc.player, this.mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null && this.mc.world != null) {
         if ((Boolean)this.potion.get()) {
            this.turtles = AlienInventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value());
         }

         if ((Boolean)this.popCounter.get()) {
            for (PlayerEntity player : this.mc.world.getPlayers()) {
               if (player != null) {
                  if (!player.isDead() && !(player.getHealth() <= 0.0F)) {
                     this.deadPlayers.remove(player);
                  } else if (!this.deadPlayers.contains(player)) {
                     this.deadPlayers.add(player);
                     this.onPlayerDeath(player);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacket(Receive event) {
      this.lagTimer.reset();
      if (event.packet instanceof PlayerPositionLookS2CPacket) {
         this.lagBackTimer.reset();
      }

      if ((Boolean)this.popCounter.get()
         && event.packet instanceof EntityStatusS2CPacket packet
         && packet.getStatus() == 35
         && packet.getEntity(this.mc.world) instanceof PlayerEntity player) {
         this.popManager.onTotemPop(player.getName().getString());
         this.onTotemPop(player);
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.currentScreen == null) {
         this.renderText(event.drawContext, event.screenWidth, event.screenHeight);
      }
   }

   public static void renderOnScreen(DrawContext context) {
      BPHackTips module = (BPHackTips)Modules.get().get(BPHackTips.class);
      if (module != null && module.isActive()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.currentScreen != null) {
            module.renderText(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
         }
      }
   }

   private void renderText(DrawContext context, int screenWidth, int screenHeight) {
      double scale = (Double)this.textScale.get();

      try {
         if ((Boolean)this.serverLag.get() && this.lagTimer.passedS(1.4)) {
            String line = (this.chinese.get() ? "Serverno" : "Server not responding") + " (" + this.df.format(this.lagTimer.getMs() / 1000.0) + "s)";
            double y = ((Integer)this.warningY.get()).intValue();
            this.drawString(
               context, line, ((Integer)this.warningX.get()).intValue(), y, (SettingColor)this.warningColor.get(), scale, screenWidth, true, true, true
            );
            if ((Boolean)this.lagBack.get() && !this.lagBackTimer.passedS(1.5)) {
               y += this.getTextHeight(scale) + 2.0;
               this.drawLagback(context, y, scale, screenWidth);
            }
         } else if ((Boolean)this.lagBack.get() && !this.lagBackTimer.passedS(1.5)) {
            this.drawLagback(context, ((Integer)this.warningY.get()).intValue(), scale, screenWidth);
         }

         if ((Boolean)this.potion.get() && this.mc.player != null) {
            StringBuilder sb = this.buildPotionString();
            if (!sb.isEmpty()) {
               String str = sb.toString();
               double x;
               if ((Integer)this.potionX.get() >= 0) {
                  x = ((Integer)this.potionX.get()).intValue();
               } else {
                  x = screenWidth / 2.0 - this.mc.textRenderer.getWidth(str) * scale / 2.0;
               }

               double py = ((Integer)this.potionY.get()).intValue();
               if (py < 0.0) {
                  py = screenHeight / 2.0 + 9.0;
               }

               py -= ((Integer)this.yOffset.get()).intValue();
               this.drawString(context, str, x, py, (SettingColor)this.potionBaseColor.get(), scale, screenWidth, (Boolean)this.potionShadow.get(), false);
            }
         }
      } catch (Exception var12) {
      }
   }

   private void drawLagback(DrawContext context, double y, double scale, int screenWidth) {
      String label = this.chinese.get() ? "Rebound" : "Lagback";
      String line = label + " (" + this.df.format((1500L - this.lagBackTimer.getMs()) / 1000.0) + "s)";
      this.drawString(context, line, ((Integer)this.warningX.get()).intValue(), y, (SettingColor)this.warningColor.get(), scale, screenWidth, true, true, true);
   }

   private void drawString(DrawContext context, String text, double settingX, double settingY, SettingColor color, double scale, int screenWidth) {
      this.drawString(context, text, settingX, settingY, color, scale, screenWidth, true, true, false);
   }

   private void drawString(
      DrawContext context,
      String text,
      double settingX,
      double settingY,
      SettingColor color,
      double scale,
      int screenWidth,
      boolean shadow,
      boolean centerWhenAuto
   ) {
      this.drawString(context, text, settingX, settingY, color, scale, screenWidth, shadow, centerWhenAuto, false);
   }

   private void drawString(
      DrawContext context,
      String text,
      double settingX,
      double settingY,
      SettingColor color,
      double scale,
      int screenWidth,
      boolean shadow,
      boolean centerWhenAuto,
      boolean glow
   ) {
      double x;
      if (settingX >= 0.0) {
         x = settingX;
      } else if (centerWhenAuto) {
         float textWidth = this.mc.textRenderer.getWidth(text);
         x = screenWidth / 2.0 - textWidth * scale / 2.0;
      } else {
         x = 0.0;
      }

      Matrix3x2fStack matrices = context.getMatrices();
      matrices.pushMatrix();
      matrices.translate((float)x, (float)settingY);
      matrices.scale((float)scale, (float)scale);
      if (glow) {
         int glowPacked = new SettingColor(color.r, color.g, color.b, 80).getPacked();
         context.drawText(this.mc.textRenderer, text, -1, 0, glowPacked, false);
         context.drawText(this.mc.textRenderer, text, 1, 0, glowPacked, false);
         context.drawText(this.mc.textRenderer, text, 0, -1, glowPacked, false);
         context.drawText(this.mc.textRenderer, text, 0, 1, glowPacked, false);
      }

      context.drawText(this.mc.textRenderer, text, 0, 0, color.getPacked(), shadow);
      matrices.popMatrix();
   }

   private double getTextHeight(double scale) {
      return (9 + 1) * scale;
   }

   private StringBuilder buildPotionString() {
      StringBuilder sb = new StringBuilder();
      if (this.turtles > 0) {
         sb.append("§e").append(this.turtles);
      }

      if (this.mc.player.hasStatusEffect(StatusEffects.RESISTANCE)
         && (!(Boolean)this.resistanceLevelCheck.get() || this.mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0)) {
         if (!sb.isEmpty()) {
            sb.append(" ");
         }

         sb.append("§9").append(this.mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
      }

      if (this.mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
         if (!sb.isEmpty()) {
            sb.append(" ");
         }

         sb.append("§4").append(this.mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
      }

      if (this.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
         if (!sb.isEmpty()) {
            sb.append(" ");
         }

         sb.append("§b").append(this.mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
      }

      return sb;
   }

   private void onPlayerDeath(PlayerEntity player) {
      String name = player.getName().getString();
      int popCount = this.popManager.getPop(name);
      boolean cn = (Boolean)this.chinese.get();
      MutableText msg;
      if (player.equals(this.mc.player)) {
         if (popCount > 0) {
            msg = Text.literal(cn ? "youatOut" : "You died after popping ").formatted(Formatting.GREEN);
            msg.append(Text.literal(String.valueOf(popCount)).formatted(Formatting.WHITE));
            msg.append(Text.literal(cn ? "ImageafterDeath." : (popCount == 1 ? " totem." : " totems.")).formatted(Formatting.GREEN));
         } else {
            msg = Text.literal(cn ? "youdead." : "You died.").formatted(Formatting.RESET);
         }
      } else if (popCount > 0) {
         msg = Text.literal(name).formatted(Formatting.WHITE);
         msg.append(Text.literal(cn ? "atOut" : " died after popping ").formatted(Formatting.GREEN));
         msg.append(Text.literal(String.valueOf(popCount)).formatted(Formatting.WHITE));
         msg.append(Text.literal(cn ? "ImageafterDeath." : (popCount == 1 ? " totem." : " totems.")).formatted(Formatting.GREEN));
      } else {
         msg = Text.literal(name).formatted(Formatting.WHITE);
         msg.append(Text.literal(cn ? "dead." : " died.").formatted(Formatting.RESET));
      }

      this.info(msg);
      if ((Boolean)this.deathCoords.get() && player == this.mc.player) {
         this.info(
            Text.literal(
                  cn
                     ? "youdie" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ()
                     : "You died at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ()
               )
               .formatted(Formatting.DARK_RED)
         );
      }

      this.popManager.onDeath(name);
   }

   public static void onFakePlayerTotemPop(String playerName, PlayerEntity player) {
      BPHackTips module = (BPHackTips)Modules.get().get(BPHackTips.class);
      if (module != null && module.isActive() && (Boolean)module.popCounter.get()) {
         module.popManager.onTotemPop(playerName);
         module.onTotemPop(player);
      }
   }

   private void onTotemPop(PlayerEntity player) {
      String name = player.getName().getString();
      int popCount = this.popManager.getPop(name);
      boolean cn = (Boolean)this.chinese.get();
      MutableText msg;
      if (player.equals(this.mc.player)) {
         msg = Text.literal(cn ? "youOut" : "You popped ").formatted(Formatting.LIGHT_PURPLE);
         msg.append(Text.literal(String.valueOf(popCount)).formatted(Formatting.WHITE));
         msg.append(Text.literal(cn ? "Image." : (popCount == 1 ? " totem." : " totems.")).formatted(Formatting.LIGHT_PURPLE));
      } else {
         msg = Text.literal(name).formatted(Formatting.WHITE);
         msg.append(Text.literal(cn ? "Out" : " has popped ").formatted(Formatting.RED));
         msg.append(Text.literal(String.valueOf(popCount)).formatted(Formatting.WHITE));
         msg.append(Text.literal(cn ? "Image." : (popCount == 1 ? " totems." : " totems.")).formatted(Formatting.RED));
      }

      this.info(msg);
   }
}
