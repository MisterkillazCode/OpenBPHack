package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class xhEntityList extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup itemGroup = this.settings.createGroup("ItemSetting");
   private final SettingGroup ui = this.settings.createGroup("FaceSetting");
   private final Setting<Set<EntityType<?>>> overworldEntities = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("Entity")).description("atDisplay'sEntity."))
            .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.CREEPER, EntityType.EXPERIENCE_ORB, EntityType.ZOMBIFIED_PIGLIN})
            .build()
      );
   private final Setting<Set<EntityType<?>>> netherEntities = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("downEntity")).description("atdownDisplay'sEntity."))
            .defaultValue(
               new EntityType[]{
                  EntityType.GHAST,
                  EntityType.BLAZE,
                  EntityType.WITHER_SKELETON,
                  EntityType.PIGLIN,
                  EntityType.COW,
                  EntityType.HORSE,
                  EntityType.PIG,
                  EntityType.SHEEP,
                  EntityType.BOGGED,
                  EntityType.CAVE_SPIDER,
                  EntityType.DROWNED,
                  EntityType.CREEPER,
                  EntityType.HUSK,
                  EntityType.SLIME,
                  EntityType.SPIDER,
                  EntityType.ZOMBIE,
                  EntityType.ZOMBIE_VILLAGER,
                  EntityType.EXPERIENCE_ORB,
                  EntityType.VILLAGER
               }
            )
            .build()
      );
   private final Setting<Set<EntityType<?>>> endEntities = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("GroundEntity")).description("atGroundDisplay'sEntity."))
            .defaultValue(new EntityType[]{EntityType.ENDERMAN, EntityType.SHULKER})
            .build()
      );
   private final Setting<SettingColor> entityColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("EntityColor"))
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .build()
      );
   private final Setting<SettingColor> playerColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("PlayerColor"))
            .defaultValue(new SettingColor(255, 100, 100, 255))
            .build()
      );
   private final Setting<Boolean> entityLog = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Entityf***"))
                  .description("SendEntityTimeatchatSkyHint."))
               .defaultValue(false))
            .build()
      );
   private final Setting<List<Item>> items1 = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("ItemList 1"))
               .description("HeavypointFollow'sItem."))
            .defaultValue(
               new Item[]{
                  Items.ELYTRA,
                  Items.SHULKER_BOX,
                  Items.WHITE_SHULKER_BOX,
                  Items.ORANGE_SHULKER_BOX,
                  Items.MAGENTA_SHULKER_BOX,
                  Items.LIGHT_BLUE_SHULKER_BOX,
                  Items.YELLOW_SHULKER_BOX,
                  Items.LIME_SHULKER_BOX,
                  Items.PINK_SHULKER_BOX,
                  Items.GRAY_SHULKER_BOX,
                  Items.LIGHT_GRAY_SHULKER_BOX,
                  Items.CYAN_SHULKER_BOX,
                  Items.PURPLE_SHULKER_BOX,
                  Items.BLUE_SHULKER_BOX,
                  Items.BROWN_SHULKER_BOX,
                  Items.GREEN_SHULKER_BOX,
                  Items.RED_SHULKER_BOX,
                  Items.BLACK_SHULKER_BOX,
                  Items.BUNDLE,
                  Items.WHITE_BUNDLE,
                  Items.ORANGE_BUNDLE,
                  Items.MAGENTA_BUNDLE,
                  Items.LIGHT_BLUE_BUNDLE,
                  Items.YELLOW_BUNDLE,
                  Items.LIME_BUNDLE,
                  Items.PINK_BUNDLE,
                  Items.GRAY_BUNDLE,
                  Items.LIGHT_GRAY_BUNDLE,
                  Items.CYAN_BUNDLE,
                  Items.PURPLE_BUNDLE,
                  Items.BLUE_BUNDLE,
                  Items.BROWN_BUNDLE,
                  Items.GREEN_BUNDLE,
                  Items.RED_BUNDLE,
                  Items.BLACK_BUNDLE,
                  Items.ANCIENT_DEBRIS,
                  Items.NETHERITE_SCRAP,
                  Items.NETHERITE_INGOT,
                  Items.NETHERITE_BLOCK,
                  Items.NETHERITE_SWORD,
                  Items.NETHERITE_AXE,
                  Items.NETHERITE_HOE,
                  Items.NETHERITE_PICKAXE,
                  Items.NETHERITE_SHOVEL,
                  Items.NETHERITE_HELMET,
                  Items.NETHERITE_CHESTPLATE,
                  Items.NETHERITE_LEGGINGS,
                  Items.NETHERITE_BOOTS,
                  Items.ENCHANTED_GOLDEN_APPLE
               }
            )
            .build()
      );
   private final Setting<SettingColor> items1Color = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Item1 Color"))
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
      );
   private final Setting<Boolean> item1Log = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Item1 f***"))
                  .description("SendList1'sItemTimeatchatSkyHint."))
               .defaultValue(false))
            .build()
      );
   private final Setting<List<Item>> items2 = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("ItemList 2"))
               .description("TimeswantFollow'sItem."))
            .defaultValue(new Item[]{Items.GOLD_INGOT, Items.IRON_INGOT, Items.DIAMOND})
            .build()
      );
   private final Setting<SettingColor> items2Color = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Item2 Color"))
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .build()
      );
   private final Setting<SettingColor> defaultItemColor = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("DefaultItemColor"))
               .description("atListin'sOtherDropThingColor."))
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .build()
      );
   private final Setting<List<Item>> blackList = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("ItemBlackNameSingle"))
               .description("notDisplay'strashItem."))
            .defaultValue(new Item[]{Items.COBBLESTONE, Items.DIRT, Items.NETHERRACK})
            .build()
      );
   private final Setting<Integer> xOffset = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("X Move"))
               .defaultValue(10))
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<Integer> yOffset = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("Y Move"))
               .defaultValue(528))
            .min(0)
            .sliderMax(1000)
            .build()
      );
   private final Setting<xhEntityList.DisplaySide> displaySide = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("toDirection"))
               .defaultValue(xhEntityList.DisplaySide.Left))
            .build()
      );
   private final Setting<Double> scale = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("ScaleSize"))
            .defaultValue(1.0)
            .min(0.5)
            .sliderMax(4.0)
            .build()
      );
   private final Setting<Integer> lineHeight = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("Rowhigh"))
               .defaultValue(18))
            .min(5)
            .sliderMax(50)
            .build()
      );
   private final Setting<Boolean> showDistance = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Show Distance"))
               .defaultValue(true))
            .build()
      );
   private final Set<Integer> loggedEntities = new HashSet<>();

   public xhEntityList() {
      super(AddonTemplate.CATEGORY, "xhEntityList", "Displays an on-screen list of nearby entities, players and dropped items.");
   }

   public void onActivate() {
      this.loggedEntities.clear();
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         Map<Item, xhEntityList.ItemStat> mapItems1 = new HashMap<>();
         Map<Item, xhEntityList.ItemStat> mapItems2 = new HashMap<>();
         Map<Item, xhEntityList.ItemStat> mapItemsOther = new HashMap<>();
         Map<EntityType<?>, xhEntityList.EntityStat> mapEntities = new HashMap<>();
         List<xhEntityList.PlayerStat> listPlayers = new ArrayList<>();
         Set<Item> set1 = new HashSet<>((Collection<? extends Item>)this.items1.get());
         Set<Item> set2 = new HashSet<>((Collection<? extends Item>)this.items2.get());
         Set<Item> setBlack = new HashSet<>((Collection<? extends Item>)this.blackList.get());
         RegistryKey<World> dimension = this.mc.world.getRegistryKey();
         Set<EntityType<?>> allowedTypes;
         if (dimension == World.OVERWORLD) {
            allowedTypes = (Set<EntityType<?>>)this.overworldEntities.get();
         } else if (dimension == World.NETHER) {
            allowedTypes = (Set<EntityType<?>>)this.netherEntities.get();
         } else {
            allowedTypes = (Set<EntityType<?>>)this.endEntities.get();
         }

         for (Entity entity : this.mc.world.getEntities()) {
            if (entity.isAlive() && entity != this.mc.player) {
               if (entity instanceof ItemEntity itemEntity) {
                  ItemStack stack = itemEntity.getStack();
                  Item item = stack.getItem();
                  if (!setBlack.contains(item)) {
                     Map<Item, xhEntityList.ItemStat> targetMap;
                     if (set1.contains(item)) {
                        targetMap = mapItems1;
                        if ((Boolean)this.item1Log.get() && !this.loggedEntities.contains(entity.getId())) {
                           this.info("SendItem:" + Names.get(item) + "Mark:" + entity.getBlockPos().toShortString(), new Object[0]);
                           this.loggedEntities.add(entity.getId());
                        }
                     } else if (set2.contains(item)) {
                        targetMap = mapItems2;
                     } else {
                        targetMap = mapItemsOther;
                     }

                     xhEntityList.ItemStat stat = targetMap.computeIfAbsent(item, k -> new xhEntityList.ItemStat(item));
                     stat.count = stat.count + stack.getCount();
                     double dist = this.mc.player.distanceTo(entity);
                     if (dist < stat.minDistance) {
                        stat.minDistance = dist;
                     }
                  }
               } else {
                  EntityType<?> type = entity.getType();
                  if (allowedTypes.contains(type)) {
                     if (entity instanceof PlayerEntity player) {
                        double dist = this.mc.player.distanceTo(player);
                        listPlayers.add(new xhEntityList.PlayerStat(player.getName().getString(), dist));
                        if ((Boolean)this.entityLog.get() && !this.loggedEntities.contains(entity.getId())) {
                           this.info("SendPlayer:" + player.getName().getString() + "Mark:" + entity.getBlockPos().toShortString(), new Object[0]);
                           this.loggedEntities.add(entity.getId());
                        }
                     } else {
                        xhEntityList.EntityStat stat = mapEntities.computeIfAbsent(type, k -> new xhEntityList.EntityStat(type));
                        stat.count++;
                        double dist = this.mc.player.distanceTo(entity);
                        if (dist < stat.minDistance) {
                           stat.minDistance = dist;
                        }

                        if ((Boolean)this.entityLog.get() && !this.loggedEntities.contains(entity.getId())) {
                           this.info("SendEntity:" + Names.get(type) + "Mark:" + entity.getBlockPos().toShortString(), new Object[0]);
                           this.loggedEntities.add(entity.getId());
                        }
                     }
                  }
               }
            }
         }

         listPlayers.sort(Comparator.comparingDouble(p -> p.distance));
         int screenWidth = this.mc.getWindow().getScaledWidth();
         double currentY = ((Integer)this.yOffset.get()).intValue();
         double scaleVal = (Double)this.scale.get();
         double scaledLineHeight = ((Integer)this.lineHeight.get()).intValue() * scaleVal;
         currentY = this.drawItems(mapItems1, currentY, (Color)this.items1Color.get(), screenWidth, scaleVal, scaledLineHeight);
         currentY = this.drawItems(mapItems2, currentY, (Color)this.items2Color.get(), screenWidth, scaleVal, scaledLineHeight);
         currentY = this.drawItems(mapItemsOther, currentY, (Color)this.defaultItemColor.get(), screenWidth, scaleVal, scaledLineHeight);
         currentY = this.drawPlayers(listPlayers, currentY, (Color)this.playerColor.get(), screenWidth, scaleVal, scaledLineHeight);
         this.drawEntities(mapEntities, currentY, (Color)this.entityColor.get(), screenWidth, scaleVal, scaledLineHeight);
      }
   }

   private double drawItems(Map<Item, xhEntityList.ItemStat> map, double y, Color color, int width, double scale, double scaledLineHeight) {
      if (map.isEmpty()) {
         return y;
      } else {
         TextRenderer renderer = TextRenderer.get();
         renderer.begin(scale);

         for (xhEntityList.ItemStat stat : map.values()) {
            String name = Names.get(stat.item);
            String text;
            if ((Boolean)this.showDistance.get()) {
               text = String.format("%s x%d (%.1fm)", name, stat.count, stat.minDistance);
            } else {
               text = String.format("%s x%d", name, stat.count);
            }

            this.drawTextInternal(renderer, text, y, color, width, scale);
            y += scaledLineHeight;
         }

         renderer.end();
         return y;
      }
   }

   private double drawPlayers(List<xhEntityList.PlayerStat> players, double y, Color color, int width, double scale, double scaledLineHeight) {
      if (players.isEmpty()) {
         return y;
      } else {
         TextRenderer renderer = TextRenderer.get();
         renderer.begin(scale);

         for (xhEntityList.PlayerStat p : players) {
            String text;
            if ((Boolean)this.showDistance.get()) {
               text = String.format("%s (%.1fm)", p.name, p.distance);
            } else {
               text = p.name;
            }

            this.drawTextInternal(renderer, text, y, color, width, scale);
            y += scaledLineHeight;
         }

         renderer.end();
         return y;
      }
   }

   private void drawEntities(Map<EntityType<?>, xhEntityList.EntityStat> map, double y, Color color, int width, double scale, double scaledLineHeight) {
      if (!map.isEmpty()) {
         TextRenderer renderer = TextRenderer.get();
         renderer.begin(scale);

         for (xhEntityList.EntityStat stat : map.values()) {
            String name = Names.get(stat.type);
            String text;
            if ((Boolean)this.showDistance.get()) {
               text = String.format("%s x%d (%.1fm)", name, stat.count, stat.minDistance);
            } else {
               text = String.format("%s x%d", name, stat.count);
            }

            this.drawTextInternal(renderer, text, y, color, width, scale);
            y += scaledLineHeight;
         }

         renderer.end();
      }
   }

   private void drawTextInternal(TextRenderer renderer, String text, double y, Color color, int screenWidth, double scale) {
      double textWidth = renderer.getWidth(text) * scale;
      double x;
      if (this.displaySide.get() == xhEntityList.DisplaySide.Right) {
         x = screenWidth - textWidth - ((Integer)this.xOffset.get()).intValue();
      } else {
         x = ((Integer)this.xOffset.get()).intValue();
      }

      renderer.render(text, x, y, color, true);
   }

   public static enum DisplaySide {
      Left,
      Right;
   }

   private static class EntityStat {
      EntityType<?> type;
      int count = 0;
      double minDistance = Double.MAX_VALUE;

      public EntityStat(EntityType<?> type) {
         this.type = type;
      }
   }

   private static class ItemStat {
      Item item;
      int count = 0;
      double minDistance = Double.MAX_VALUE;

      public ItemStat(Item item) {
         this.item = item;
      }
   }

   private static class PlayerStat {
      String name;
      double distance;

      public PlayerStat(String name, double distance) {
         this.name = name;
         this.distance = distance;
      }
   }
}
