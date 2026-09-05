package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.alien.AlienBreakManager;
import com.codigohasta.addon.utils.alien.AlienColorUtil;
import com.codigohasta.addon.utils.alien.AlienEasing;
import com.codigohasta.addon.utils.alien.AlienRender3DUtil;
import java.awt.Color;
import java.text.DecimalFormat;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

public class BreakESP extends Module {
   public static BreakESP INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> progress = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Progress")).description("Show break progress percentage.")).defaultValue(true)).build());
   private final Setting<Double> damage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Damage"))
               .description("Damage multiplier for break time calculation."))
            .defaultValue(1.0)
            .range(0.0, 2.0)
            .sliderRange(0.0, 2.0)
            .build()
      );
   private final Setting<AlienEasing> ease = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Ease"))
                  .description("Easing mode for the box animation."))
               .defaultValue(AlienEasing.CubicInOut))
            .build()
      );
   private final Setting<Boolean> second = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Second")).description("Show second/double break indicators.")).defaultValue(true)).build());
   private final Setting<Boolean> boxToggle = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("Box")).description("Show box outline.")).defaultValue(true)).build());
   private final Setting<SettingColor> boxColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Box Color"))
               .description("Color of the box outline."))
            .defaultValue(new SettingColor(198, 176, 12, 255))
            .build()
      );
   private final Setting<Boolean> fillToggle = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("Fill")).description("Show filled box.")).defaultValue(true)).build());
   private final Setting<SettingColor> fillColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Fill Color"))
               .description("Color of the fill."))
            .defaultValue(new SettingColor(198, 176, 12, 78))
            .build()
      );
   private final Setting<Boolean> friendBoxToggle = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("Friend Box")).description("Show box outline for friends.")).defaultValue(true)).build());
   private final Setting<SettingColor> friendBoxColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Friend Box Color"))
               .description("Color of the box outline for friends."))
            .defaultValue(new SettingColor(30, 45, 169, 255))
            .build()
      );
   private final Setting<Boolean> friendFillToggle = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("Friend Fill")).description("Show filled box for friends.")).defaultValue(true)).build());
   private final Setting<SettingColor> friendFillColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Friend Fill Color"))
               .description("Color of the fill for friends."))
            .defaultValue(new SettingColor(30, 45, 169, 78))
            .build()
      );
   private final Setting<Boolean> secondBoxToggle = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("Second Box")).description("Show box outline for second break.")).defaultValue(true)).build());
   private final Setting<SettingColor> secondBoxColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Second Box Color"))
               .description("Color of the second box outline."))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<Boolean> secondFillToggle = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("Second Fill")).description("Show filled box for second break.")).defaultValue(true)).build());
   private final Setting<SettingColor> secondFillColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Second Fill Color"))
               .description("Color of the second fill."))
            .defaultValue(new SettingColor(255, 255, 255, 100))
            .build()
      );
   private final Setting<Double> maxTextScale = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("TextMaxScale"))
               .description("mostbigTextTextScaleupLimit."))
            .defaultValue(15.0)
            .min(1.0)
            .max(100.0)
            .sliderRange(1.0, 50.0)
            .build()
      );
   private final Setting<Double> textScaleBase = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("TextScaleBase"))
               .description("TextTextBasicScale(DistanceFor0Time'sSize). Default5.0"))
            .defaultValue(5.0)
            .min(0.5)
            .max(30.0)
            .sliderRange(0.5, 20.0)
            .build()
      );
   private final Setting<Double> textScaleFactor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("TextScaleFactor"))
               .description("Distance'sScale. Default0.1"))
            .defaultValue(0.1)
            .min(0.0)
            .max(2.0)
            .sliderRange(0.0, 1.0)
            .build()
      );
   final DecimalFormat df = new DecimalFormat("0.0");
   final Color startColor = new Color(255, 6, 6);
   final Color endColor = new Color(0, 255, 12);
   final Color doubleColor = new Color(255, 179, 96);

   public BreakESP() {
      super(AddonTemplate.SC_CATEGORY, "BreakESP", "Renders the break progress of blocks being mined, with configurable box and fill.");
      INSTANCE = this;
   }

   public void onActivate() {
      if (AlienBreakManager.INSTANCE != null) {
         AlienBreakManager.INSTANCE.damageMultiplier = (Double)this.damage.get();
      }
   }

   private Color getFillColor(PlayerEntity player) {
      return Friends.get().isFriend(player) ? this.toAwt((SettingColor)this.friendFillColor.get()) : this.toAwt((SettingColor)this.fillColor.get());
   }

   private Color getBoxColor(PlayerEntity player) {
      return Friends.get().isFriend(player) ? this.toAwt((SettingColor)this.friendBoxColor.get()) : this.toAwt((SettingColor)this.boxColor.get());
   }

   private boolean isBoxVisible(PlayerEntity player) {
      return Friends.get().isFriend(player) ? (Boolean)this.friendBoxToggle.get() : (Boolean)this.boxToggle.get();
   }

   private boolean isFillVisible(PlayerEntity player) {
      return Friends.get().isFriend(player) ? (Boolean)this.friendFillToggle.get() : (Boolean)this.fillToggle.get();
   }

   private Color toAwt(SettingColor c) {
      return new Color(c.r, c.g, c.b, c.a);
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      AlienBreakManager manager = AlienBreakManager.INSTANCE;
      if (manager != null) {
         manager.damageMultiplier = (Double)this.damage.get();

         for (AlienBreakManager.BreakData breakData : manager.breakMap.values()) {
            if (breakData != null && breakData.getEntity() != null) {
               PlayerEntity player = (PlayerEntity)breakData.getEntity();
               double easeVal = breakData.fade.ease((AlienEasing)this.ease.get());
               double size = 0.5 * (1.0 - easeVal);
               Box cbox = new Box(breakData.pos).shrink(size, size, size).shrink(-size, -size, -size);
               if (this.isFillVisible(player)) {
                  AlienRender3DUtil.drawFill(event, cbox, this.getFillColor(player));
               }

               if (this.isBoxVisible(player)) {
                  AlienRender3DUtil.drawBox(event, cbox, this.getBoxColor(player));
               }

               AlienRender3DUtil.drawText3D(
                  player.getName().getString(),
                  breakData.pos.toCenterPos().add(0.0, this.progress.get() ? 0.15 : 0.0, 0.0),
                  (Double)this.textScaleBase.get(),
                  (Double)this.textScaleFactor.get(),
                  (Double)this.maxTextScale.get(),
                  -1
               );
               if ((Boolean)this.progress.get()) {
                  String progressText = breakData.failed
                     ? "§4Failed"
                     : (breakData.complete ? "Broke" : this.df.format(Math.min(1.0, breakData.timer.getMs() / breakData.breakTime) * 100.0));
                  Color progressColor = breakData.complete
                     ? (this.mc.world.isAir(breakData.pos) ? this.endColor : this.startColor)
                     : AlienColorUtil.fadeColor(this.startColor, this.endColor, breakData.timer.getMs() / breakData.breakTime);
                  AlienRender3DUtil.drawText3D(
                     Text.of(progressText),
                     breakData.pos.toCenterPos().add(0.0, -0.15, 0.0),
                     0.0,
                     0.0,
                     1.0,
                     progressColor.getRGB(),
                     (Double)this.textScaleBase.get(),
                     (Double)this.textScaleFactor.get(),
                     (Double)this.maxTextScale.get()
                  );
               }
            }
         }

         if ((Boolean)this.second.get()) {
            for (int i : manager.doubleMap.keySet()) {
               AlienBreakManager.BreakData breakDatax = manager.doubleMap.get(i);
               if (breakDatax != null && breakDatax.getEntity() != null && !this.mc.world.isAir(breakDatax.pos)) {
                  AlienBreakManager.BreakData singleBreakData = manager.breakMap.get(i);
                  if (singleBreakData == null || !singleBreakData.pos.equals(breakDatax.pos)) {
                     double easeValx = breakDatax.fade.ease((AlienEasing)this.ease.get());
                     double sizex = 0.5 * (1.0 - easeValx);
                     Box cboxx = new Box(breakDatax.pos).shrink(sizex, sizex, sizex).shrink(-sizex, -sizex, -sizex);
                     if ((Boolean)this.secondFillToggle.get()) {
                        AlienRender3DUtil.drawFill(event, cboxx, this.toAwt((SettingColor)this.secondFillColor.get()));
                     }

                     if ((Boolean)this.secondBoxToggle.get()) {
                        AlienRender3DUtil.drawBox(event, cboxx, this.toAwt((SettingColor)this.secondBoxColor.get()));
                     }

                     AlienRender3DUtil.drawText3D(
                        breakDatax.getEntity().getName().getString(),
                        breakDatax.pos.toCenterPos().add(0.0, 0.15, 0.0),
                        (Double)this.textScaleBase.get(),
                        (Double)this.textScaleFactor.get(),
                        (Double)this.maxTextScale.get(),
                        -1
                     );
                     AlienRender3DUtil.drawText3D(
                        "Double",
                        breakDatax.pos.toCenterPos().add(0.0, -0.15, 0.0),
                        (Double)this.textScaleBase.get(),
                        (Double)this.textScaleFactor.get(),
                        (Double)this.maxTextScale.get(),
                        this.doubleColor.getRGB()
                     );
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      AlienRender3DUtil.renderDeferred();
   }
}
