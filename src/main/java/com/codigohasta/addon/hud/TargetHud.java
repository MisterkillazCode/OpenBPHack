package com.codigohasta.addon.hud;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.AbstractClientPlayerEntityAccessor;
import com.codigohasta.addon.utils.render.GuiRenderUtil;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;

public class TargetHud extends HudElement {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final HudElementInfo<TargetHud> INFO = new HudElementInfo(
      AddonTemplate.HUD_GROUP, "img-target-hud", "Advanced Target Panel (Neon Glass)", TargetHud::new
   );
   private static final Color NEON_CYAN = new Color(0, 220, 255);
   private static final Color NEON_PURPLE = new Color(150, 80, 255);
   private static final Color NEON_PINK = new Color(255, 70, 130);
   private static final Color NEON_GREEN = new Color(0, 255, 170);
   private static final Color GLASS_BG = new Color(8, 12, 22);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgElements = this.settings.createGroup("Display Elements");
   private final SettingGroup sgColors = this.settings.createGroup("Color");
   private final Setting<Double> scale = this.sgGeneral.add(((Builder)new Builder().name("Scale")).defaultValue(1.0).min(0.5).sliderMax(3.0).build());
   private final Setting<TargetHud.HpDisplay> hpDisplay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Health Display"))
               .defaultValue(TargetHud.HpDisplay.Health))
            .build()
      );
   private final Setting<Double> bgAlpha = this.sgGeneral
      .add(((Builder)new Builder().name("Background Opacity")).defaultValue(0.85).min(0.0).max(1.0).sliderRange(0.0, 1.0).build());
   private final Setting<Boolean> showSkin = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Avatar"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showName = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Name"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showHealth = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Health"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showHpText = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("HealthNumber"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showArmor = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Equipment"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showPotions = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Potion Effect"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showParticles = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("ParticleEffect"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> showShadow = this.sgElements
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Shadow"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> accentColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Accent Color"))
            .defaultValue(new SettingColor(150, 80, 255))
            .build()
      );
   private LivingEntity target;
   private double animHealth;

   public TargetHud() {
      super(INFO);
   }

   public void render(HudRenderer renderer) {
      this.updateTarget();
      if (this.target == null) {
         this.animHealth = 0.0;
      } else {
         float s = ((Double)this.scale.get()).floatValue();
         double bw = 188.0F * s;
         double bh = 58.0F * s;
         this.box.setSize(bw, bh);
         double px = this.x;
         double py = this.y;
         this.renderNeonGlass(renderer, px, py, bw, bh, s);
      }
   }

   private void renderNeonGlass(HudRenderer r, double px, double py, double bw, double bh, float s) {
      double rad = 8.0F * s;
      double b = 2.0F * s;
      Color ac = (Color)this.accentColor.get();
      if ((Boolean)this.showShadow.get()) {
         GuiRenderUtil.drawShadow(r, px, py, bw, bh, rad, new Color(0, 0, 0, 90));
      }

      GuiRenderUtil.drawRoundGradient(r, px, py, bw, bh, rad, NEON_CYAN, NEON_CYAN, ac, ac);
      Color bg = new Color(GLASS_BG.r, GLASS_BG.g, GLASS_BG.b, (int)(GLASS_BG.a + (255 - GLASS_BG.a) * (Double)this.bgAlpha.get()));
      GuiRenderUtil.drawRound(r, px + b, py + b, bw - 2.0 * b, bh - 2.0 * b, Math.max(1.0, rad - b), bg);
      double avSize = 40.0F * s;
      double avX = px + 8.0F * s;
      double avY = py + (bh - avSize) / 2.0;
      double textX = avX + avSize + 8.0F * s;
      double infoW = bw - (avX - px) - avSize - 16.0F * s;
      if ((Boolean)this.showSkin.get()) {
         GuiRenderUtil.drawRound(r, avX - 2.0F * s, avY - 2.0F * s, avSize + 4.0F * s, avSize + 4.0F * s, 9.0F * s, NEON_CYAN);
         this.drawSkin(r, avX, avY, avSize, avSize, 7.0F * s, bg);
      }

      if ((Boolean)this.showName.get()) {
         r.text(this.truncName(this.target.getName().getString()), textX, py + 9.0F * s, new Color(210, 245, 255), true);
      }

      if ((Boolean)this.showArmor.get() && this.target instanceof PlayerEntity pe) {
         this.drawArmorSmall(r, textX, py + 26.0F * s, s, pe);
      }

      if ((Boolean)this.showPotions.get() && this.target instanceof PlayerEntity pe) {
         this.drawPotionText(r, textX, py + 40.0F * s, pe);
      }

      double barY = py + bh - 12.0F * s;
      double barH = 7.0F * s;
      if ((Boolean)this.showHealth.get()) {
         this.animHealth = MathHelper.lerp(0.15, this.animHealth, this.target.getHealth());
         double pct = MathHelper.clamp(this.animHealth / this.target.getMaxHealth(), 0.0, 1.0);
         GuiRenderUtil.drawRound(r, textX, barY, infoW, barH, 3.0F * s, new Color(255, 255, 255, 22));
         if (pct > 0.01) {
            Color hpC = pct > 0.5 ? NEON_GREEN : (pct > 0.2 ? new Color(255, 200, 0) : NEON_PINK);
            GuiRenderUtil.drawRound(r, textX - 1.0F * s, barY - 1.0F * s, infoW * pct + 2.0F * s, barH + 2.0F * s, 4.0F * s, new Color(hpC.r, hpC.g, hpC.b, 60));
            GuiRenderUtil.drawRound(r, textX, barY, infoW * pct, barH, 3.0F * s, hpC);
         }
      }

      if ((Boolean)this.showHpText.get()) {
         String hp = this.hpStr();
         r.text(hp, textX + infoW - r.textWidth(hp), barY - 11.0F * s, new Color(180, 240, 255), true);
      }
   }

   private void drawSkin(HudRenderer r, double x, double y, double size, double size2, double rad, Color bgColor) {
      if (this.target instanceof AbstractClientPlayerEntity ace) {
         Identifier skinID = this.getSkinTexture(ace);
         if (skinID != null) {
            double u1 = 0.125;
            double v1 = 0.125;
            double u2 = 0.25;
            double v2 = 0.25;
            double hu1 = 0.625;
            double hu2 = 0.75;
            AbstractTexture tex = mc.getTextureManager().getTexture(skinID);
            Renderer2D.TEXTURE.begin();
            Renderer2D.TEXTURE.texQuad(x, y, size, size2, 0.0, u1, v1, u2, v2, Color.WHITE);
            Renderer2D.TEXTURE.texQuad(x, y, size, size2, 0.0, hu1, v1, hu2, v2, Color.WHITE);
            Renderer2D.TEXTURE.render(tex.getGlTextureView(), tex.getSampler());
         }
      } else {
         GuiRenderUtil.drawRound(r, x, y, size, size2, rad, new Color(60, 60, 60, 255));
      }
   }

   private Identifier getSkinTexture(AbstractClientPlayerEntity player) {
      try {
         PlayerListEntry entry = ((AbstractClientPlayerEntityAccessor)player).invokeGetPlayerListEntry();
         if (entry != null) {
            return entry.getSkinTextures().body().texturePath();
         }
      } catch (Exception var3) {
      }

      return DefaultSkinHelper.getSkinTextures(player.getUuid()).body().texturePath();
   }

   private void drawArmorSmall(HudRenderer r, double x, double y, float s, PlayerEntity pe) {
      ItemStack[] items = new ItemStack[]{
         pe.getMainHandStack(),
         pe.getEquippedStack(EquipmentSlot.HEAD),
         pe.getEquippedStack(EquipmentSlot.CHEST),
         pe.getEquippedStack(EquipmentSlot.LEGS),
         pe.getEquippedStack(EquipmentSlot.FEET),
         pe.getOffHandStack()
      };
      int ix = (int)x;

      for (ItemStack stack : items) {
         if (stack.isEmpty()) {
            ix += (int)(10.0F * s);
         } else {
            r.item(stack, ix, (int)y, 0.75F, true);
            ix += (int)(13.0F * s);
         }
      }
   }

   private void drawPotionText(HudRenderer r, double x, double y, PlayerEntity pe) {
      StringBuilder sb = new StringBuilder();

      for (StatusEffectInstance effect : pe.getStatusEffects()) {
         StatusEffect type = (StatusEffect)effect.getEffectType().value();
         if (type == StatusEffects.STRENGTH) {
            sb.append("Str").append(effect.getAmplifier() + 1).append(" ");
         } else if (type == StatusEffects.SPEED) {
            sb.append("Spd").append(effect.getAmplifier() + 1).append(" ");
         } else if (type == StatusEffects.REGENERATION) {
            sb.append("Reg ");
         } else if (type == StatusEffects.RESISTANCE) {
            sb.append("Res").append(effect.getAmplifier() + 1).append(" ");
         } else if (type == StatusEffects.WEAKNESS) {
            sb.append("Weak ");
         } else if (type == StatusEffects.SLOWNESS) {
            sb.append("Slow").append(effect.getAmplifier() + 1).append(" ");
         }
      }

      if (!sb.isEmpty()) {
         r.text(sb.toString().trim(), x, y, new Color(140, 230, 255), true);
      }
   }

   private String hpStr() {
      return this.hpDisplay.get() == TargetHud.HpDisplay.Health
         ? String.format("%.1f", this.animHealth)
         : (int)(this.animHealth / this.target.getMaxHealth() * 100.0) + "%";
   }

   private String truncName(String name) {
      return name.length() > 14 ? name.substring(0, 14) + "…" : name;
   }

   private void updateTarget() {
      KillAura aura = (KillAura)Modules.get().get(KillAura.class);
      if (aura != null && aura.isActive() && aura.getTarget() instanceof LivingEntity le) {
         this.target = le;
      } else if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity le) {
         this.target = le;
      } else {
         this.target = null;
      }
   }

   public static enum HpDisplay {
      Health,
      Percentage;
   }
}
