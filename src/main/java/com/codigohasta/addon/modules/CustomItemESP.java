package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ItemListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

public class CustomItemESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBox = this.settings.createGroup("3D RenderBox");
   private final SettingGroup sgText = this.settings.createGroup("2D NameMark");
   private final Setting<List<Item>> items = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("ItemList")).description("SelectneedhighDisplay'sSetDropThing.")).build());
   private final Setting<Boolean> renderBox = this.sgBox
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("EnableRenderBox"))
                  .description("whetheratDropThingSystemDirectionBox."))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgBox
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("Mode"))
                     .description("DirectionBox'sRenderLike."))
                  .defaultValue(ShapeMode.Both))
               .visible(this.renderBox::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgBox
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("Fill Color"))
                  .description("DirectionBoxInternal'sColor."))
               .defaultValue(new SettingColor(0, 210, 255, 40))
               .visible(this.renderBox::get))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgBox
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("BoxColor"))
                  .description("DirectionBoxLine'sColor."))
               .defaultValue(new SettingColor(0, 210, 255, 200))
               .visible(this.renderBox::get))
            .build()
      );
   private final Setting<Boolean> renderName = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("EnableNameMark"))
                  .description("whetheratDropThingupDirectionDisplayName."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> scale = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("MarkScale"))
                  .description("NameMark'sSizeScale."))
               .defaultValue(1.0)
               .min(0.1)
               .sliderMax(2.5)
               .visible(this.renderName::get))
            .build()
      );
   private final Setting<Boolean> showCount = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("DisplayAmount"))
                     .description("atNameDisplayItemAmount ( x64)."))
                  .defaultValue(true))
               .visible(this.renderName::get))
            .build()
      );
   private final Setting<SettingColor> nameColor = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("TextTextColor"))
                  .description("DropThingName'sColor."))
               .defaultValue(new SettingColor(230, 241, 251))
               .visible(this.renderName::get))
            .build()
      );
   private final Setting<Boolean> renderBackground = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Background"))
                     .description("whetherRenderBlackColorBackground."))
                  .defaultValue(true))
               .visible(this.renderName::get))
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgText
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("BackgroundColor"))
                  .description("TextTextBackground'sColor."))
               .defaultValue(new SettingColor(10, 14, 22, 75))
               .visible(() -> (Boolean)this.renderName.get() && (Boolean)this.renderBackground.get()))
            .build()
      );
   private final Vector3d pos = new Vector3d();

   public CustomItemESP() {
      super(AddonTemplate.SC_CATEGORY, "CustomItemESP", "Item ESP: draws boxes and name/amount labels for selected dropped items.");
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.renderBox.get()) {
         for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
               ItemStack stack = itemEntity.getStack();
               if (((List)this.items.get()).contains(stack.getItem())) {
                  this.renderItemBox(event, itemEntity);
               }
            }
         }
      }
   }

   private void renderItemBox(Render3DEvent event, ItemEntity entity) {
      Box box = entity.getBoundingBox();
      event.renderer.box(box, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if ((Boolean)this.renderName.get()) {
         for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
               ItemStack stack = itemEntity.getStack();
               if (((List)this.items.get()).contains(stack.getItem())) {
                  this.renderItemNametag(itemEntity, stack, event.tickDelta, event.drawContext);
               }
            }
         }
      }
   }

   private void renderItemNametag(ItemEntity entity, ItemStack stack, double tickDelta, DrawContext drawContext) {
      this.setVector(this.pos, entity, tickDelta);
      this.pos.add(0.0, 0.75, 0.0);
      if (NametagUtils.to2D(this.pos, (Double)this.scale.get())) {
         NametagUtils.begin(this.pos);
         TextRenderer textRenderer = TextRenderer.get();
         String name = Names.get(stack);
         String displayString = name;
         if ((Boolean)this.showCount.get() && stack.getCount() > 1) {
            displayString = name + " x" + stack.getCount();
         }

         double width = textRenderer.getWidth(displayString);
         double height = textRenderer.getHeight();
         double widthHalf = width / 2.0;
         if ((Boolean)this.renderBackground.get()) {
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-widthHalf - 2.0, -height / 2.0 - 2.0, width + 4.0, height + 4.0, (Color)this.backgroundColor.get());
            Renderer2D.COLOR.render();
         }

         textRenderer.beginBig();
         textRenderer.render(displayString, -widthHalf, -height / 2.0, (Color)this.nameColor.get(), true);
         textRenderer.end();
         NametagUtils.end();
      }
   }

   private void setVector(Vector3d pos, Entity entity, double tickDelta) {
      double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
      double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
      double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
      pos.set(x, y, z);
   }
}
