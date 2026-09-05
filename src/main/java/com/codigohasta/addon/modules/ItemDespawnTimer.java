package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.joml.Vector3d;

public class ItemDespawnTimer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final SettingGroup sgColor = this.settings.createGroup("ColorSetting");
   private final SettingGroup sgSound = this.settings.createGroup("SoundSetting");
   private final Setting<Double> scale = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("ScaleSize")).description("TextText'sDisplaySize.")).defaultValue(1.0).min(0.5).sliderMax(3.0).build());
   private final Setting<Double> heightOffset = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("HeightMove")).description("TextTextDisplayatItemupDirection'sDistance."))
            .defaultValue(0.75)
            .min(0.0)
            .sliderMax(3.0)
            .build()
      );
   private final Setting<String> prefix = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("Custombefore"))
                  .description("DisplayatTimebeforeFace'sTextText."))
               .defaultValue(""))
            .build()
      );
   private final Setting<Boolean> blink = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("mostafter10"))
                  .description("RemainingTimefew10Time, TextTextRed."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> playSound = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Play Sound"))
                  .description("ItemDisappearTimePutSound."))
               .defaultValue(true))
            .build()
      );
   private final Setting<ItemDespawnTimer.SoundType> soundType = this.sgSound
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("SoundType"))
                     .description("SelectPut'sHintSound."))
                  .defaultValue(ItemDespawnTimer.SoundType.PLING))
               .visible(this.playSound::get))
            .build()
      );
   private final Setting<Double> soundVolume = this.sgSound
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("SoundAmount")).description("HintSound'sSoundAmountSize."))
               .defaultValue(1.0)
               .min(0.1)
               .max(2.0)
               .visible(this.playSound::get))
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("BackgroundColor"))
               .description("TextTextBackground'sColor."))
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build()
      );
   private final Setting<ItemDespawnTimer.ColorMode> colorMode = this.sgColor
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Color Mode"))
                  .description("TextTextColor'sDisplayDirection."))
               .defaultValue(ItemDespawnTimer.ColorMode.Gradient))
            .build()
      );
   private final Setting<SettingColor> staticColor = this.sgColor
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("Color"))
                  .description("ModeForTime'sColor."))
               .defaultValue(new SettingColor(255, 255, 255))
               .visible(() -> this.colorMode.get() == ItemDespawnTimer.ColorMode.Static))
            .build()
      );
   private final Setting<SettingColor> startColor = this.sgColor
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("startColor"))
                  .description("TimeTime'sColor(: GreenColor)."))
               .defaultValue(new SettingColor(25, 252, 25))
               .visible(() -> this.colorMode.get() == ItemDespawnTimer.ColorMode.Gradient))
            .build()
      );
   private final Setting<SettingColor> middleColor = this.sgColor
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("inBetweenColor"))
                  .description("Remaining1Time'sColor(: YellowColor)."))
               .defaultValue(new SettingColor(255, 255, 25))
               .visible(() -> this.colorMode.get() == ItemDespawnTimer.ColorMode.Gradient))
            .build()
      );
   private final Setting<SettingColor> endColor = this.sgColor
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("endColor"))
                  .description("DisappearTime'sColor(: RedColor)."))
               .defaultValue(new SettingColor(255, 25, 25))
               .visible(() -> this.colorMode.get() == ItemDespawnTimer.ColorMode.Gradient))
            .build()
      );
   private final Vector3d pos = new Vector3d();
   private final Color BLINK_COLOR = new Color(255, 0, 0);
   private final Map<UUID, Long> despawnCache = new ConcurrentHashMap<>();

   public ItemDespawnTimer() {
      super(AddonTemplate.SC_CATEGORY, "ItemDespawnTimer", "Shows the remaining time before dropped items despawn. (Unreliable.)");
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.despawnCache.clear();
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.world != null && this.mc.player != null) {
         boolean shouldPlay = false;
         boolean fastPlay = false;

         for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
               int ticksLeft = this.getRealTicksLeft(itemEntity);
               if (ticksLeft > 0 && ticksLeft <= 6000 && (Boolean)this.playSound.get() && ticksLeft > 0 && ticksLeft <= 200) {
                  if (ticksLeft <= 60) {
                     if (ticksLeft % 5 == 0) {
                        fastPlay = true;
                     }
                  } else if (ticksLeft % 20 == 0) {
                     shouldPlay = true;
                  }
               }
            }
         }

         if (fastPlay || shouldPlay) {
            float pitch = fastPlay ? 2.0F : 1.0F;
            this.mc
               .world
               .playSound(
                  this.mc.player,
                  this.mc.player.getBlockPos(),
                  ((ItemDespawnTimer.SoundType)this.soundType.get()).getSound(),
                  SoundCategory.PLAYERS,
                  ((Double)this.soundVolume.get()).floatValue(),
                  pitch
               );
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
               this.renderTimer(itemEntity, event);
            }
         }
      }
   }

   private int getRealTicksLeft(ItemEntity item) {
      int itemAge = item.getItemAge();
      if (itemAge == -32768) {
         return 999999;
      } else {
         UUID uuid = item.getUuid();
         long now = System.currentTimeMillis();
         int maxAge = 6000;
         long estimatedDespawnTime = now + (maxAge - itemAge) * 50L;
         if (!this.despawnCache.containsKey(uuid)) {
            this.despawnCache.put(uuid, estimatedDespawnTime);
            return maxAge - itemAge;
         } else {
            long cachedDespawnTime = this.despawnCache.get(uuid);
            long millisLeft = cachedDespawnTime - now;
            int cachedTicksLeft = (int)(millisLeft / 50L);
            if (maxAge - itemAge > cachedTicksLeft + 20) {
               return cachedTicksLeft;
            } else {
               if (estimatedDespawnTime < cachedDespawnTime - 1000L) {
                  this.despawnCache.put(uuid, estimatedDespawnTime);
               }

               return maxAge - itemAge;
            }
         }
      }
   }

   private void renderTimer(ItemEntity item, Render2DEvent event) {
      int ticksLeft = this.getRealTicksLeft(item);
      if (ticksLeft <= 6000) {
         if (ticksLeft > 0) {
            double secondsLeft = ticksLeft / 20.0;
            String timeStr = String.format("%.1fs", secondsLeft);
            if (secondsLeft > 60.0) {
               int mins = (int)secondsLeft / 60;
               double secs = secondsLeft % 60.0;
               timeStr = String.format("%d:%04.1f", mins, secs);
            }

            String finalContent = (String)this.prefix.get() + timeStr;
            Color finalColor;
            if ((Boolean)this.blink.get() && ticksLeft < 200 && ticksLeft % 10 < 5) {
               finalColor = this.BLINK_COLOR;
            } else if (this.colorMode.get() == ItemDespawnTimer.ColorMode.Static) {
               finalColor = (Color)this.staticColor.get();
            } else {
               int midPoint = 1200;
               int maxAge = 6000;
               if (ticksLeft >= midPoint) {
                  double progress = (double)(ticksLeft - midPoint) / (maxAge - midPoint);
                  progress = Math.min(1.0, Math.max(0.0, progress));
                  finalColor = this.interpolate((Color)this.middleColor.get(), (Color)this.startColor.get(), progress);
               } else {
                  double progress = (double)ticksLeft / midPoint;
                  progress = Math.min(1.0, Math.max(0.0, progress));
                  finalColor = this.interpolate((Color)this.endColor.get(), (Color)this.middleColor.get(), progress);
               }
            }

            Utils.set(this.pos, item, event.tickDelta);
            this.pos.add(0.0, item.getHeight() + (Double)this.heightOffset.get(), 0.0);
            if (NametagUtils.to2D(this.pos, (Double)this.scale.get())) {
               this.renderNametag(finalContent, finalColor, event);
            }
         }
      }
   }

   private Color interpolate(Color start, Color end, double progress) {
      int r = (int)(start.r + (end.r - start.r) * progress);
      int g = (int)(start.g + (end.g - start.g) * progress);
      int b = (int)(start.b + (end.b - start.b) * progress);
      int a = (int)(start.a + (end.a - start.a) * progress);
      return new Color(r, g, b, a);
   }

   private void renderNametag(String textStr, Color color, Render2DEvent event) {
      TextRenderer text = TextRenderer.get();
      NametagUtils.begin(this.pos, event.drawContext);
      double width = text.getWidth(textStr, true);
      double height = text.getHeight(true);
      double widthHalf = width / 2.0;
      this.drawBg(-widthHalf, -height, width, height);
      text.beginBig();
      text.render(textStr, -widthHalf, -height, color, true);
      text.end();
      NametagUtils.end(event.drawContext);
   }

   private void drawBg(double x, double y, double width, double height) {
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad(x - 1.0, y - 1.0, width + 2.0, height + 2.0, (Color)this.backgroundColor.get());
      Renderer2D.COLOR.render();
   }

   public static enum ColorMode {
      Static,
      Gradient;
   }

   public static enum SoundType {
      PLING((SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()),
      ORB(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
      ANVIL(SoundEvents.BLOCK_ANVIL_LAND),
      CLICK((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value()),
      POP(SoundEvents.ENTITY_ITEM_PICKUP);

      private final SoundEvent sound;

      private SoundType(SoundEvent sound) {
         this.sound = sound;
      }

      public SoundEvent getSound() {
         return this.sound;
      }
   }
}
