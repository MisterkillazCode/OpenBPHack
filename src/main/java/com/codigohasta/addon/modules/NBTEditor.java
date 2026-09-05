package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Builder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

public class NBTEditor extends Module {
   public NBTEditor() {
      super(
         AddonTemplate.OP_CATEGORY,
         "NBTEditor",
         "OP / creative only. Open a GUI to edit the held item's name, lore, enchantments, attribute modifiers, unbreakable, damage, count and raw NBT, then click 'Done' to apply and get a god item."
      );
   }

   public void onActivate() {
      if (this.mc.player == null) {
         this.toggle();
      } else if (!this.mc.player.isCreative() && !this.mc.player.isCreativeLevelTwoOp()) {
         ChatUtils.warning("NBTEditor: requires creative mode or OP, disabled.", new Object[0]);
         this.toggle();
      } else if (this.mc.player.getMainHandStack().isEmpty()) {
         ChatUtils.warning("NBTEditor: hold an item first, disabled.", new Object[0]);
         this.toggle();
      } else {
         this.mc.setScreen(new NBTEditor.NBTEditorScreen(this.mc));
      }
   }

   private static String textToLegacy(Text text) {
      StringBuilder sb = new StringBuilder();
      appendStyle(sb, text.getStyle());
      sb.append(text.getString());

      for (Text sibling : text.getSiblings()) {
         appendStyle(sb, sibling.getStyle());
         sb.append(sibling.getString());
      }

      return sb.toString();
   }

   private static void appendStyle(StringBuilder sb, Style style) {
      if (style != null && !style.isEmpty()) {
         TextColor color = style.getColor();
         if (color != null) {
            String name = color.getName();
            if (name != null) {
               sb.append('§').append(legacyCode(name));
            } else {
               String hex = color.getHexCode().substring(1);
               sb.append("§x");

               for (char c : hex.toCharArray()) {
                  sb.append('§').append(c);
               }
            }
         }

         if (style.isBold()) {
            sb.append("§l");
         }

         if (style.isItalic()) {
            sb.append("§o");
         }

         if (style.isUnderlined()) {
            sb.append("§n");
         }

         if (style.isStrikethrough()) {
            sb.append("§m");
         }

         if (style.isObfuscated()) {
            sb.append("§k");
         }
      }
   }

   private static String legacyCode(String name) {
      return switch (name) {
         case "black" -> "0";
         case "dark_blue" -> "1";
         case "dark_green" -> "2";
         case "dark_aqua" -> "3";
         case "dark_red" -> "4";
         case "dark_purple" -> "5";
         case "gold" -> "6";
         case "gray" -> "7";
         case "dark_gray" -> "8";
         case "blue" -> "9";
         case "green" -> "a";
         case "aqua" -> "b";
         case "red" -> "c";
         case "light_purple" -> "d";
         case "yellow" -> "e";
         case "white" -> "f";
         default -> "f";
      };
   }

   public static class NBTEditorScreen extends Screen {
      private final MinecraftClient mc;
      private TextFieldWidget nameField;
      private TextFieldWidget loreField;
      private TextFieldWidget enchField;
      private TextFieldWidget attrField;
      private TextFieldWidget rawField;
      private TextFieldWidget damageField;
      private TextFieldWidget countField;
      private ButtonWidget unbreakableBtn;
      private boolean unbreakable = false;
      private final List<NBTEditor.NBTEditorScreen.Label> labels = new ArrayList<>();
      private int actionY;

      public NBTEditorScreen(MinecraftClient mc) {
         super(Text.literal("BPHack NBT Editor"));
         this.mc = mc;
      }

      protected void init() {
         int w = this.width;
         int left = 12;
         int fieldW = Math.min(360, w - 200);
         int y = 40;
         ItemStack hand = this.mc.player.getMainHandStack();
         this.addLabel(left, y, "Name  (&a colour codes, &l&o&n&m&k styles)");
         this.nameField = new TextFieldWidget(this.mc.textRenderer, left, y + 14, fieldW, 18, Text.literal(""));
         this.nameField.setMaxLength(200);
         Text curName = (Text)hand.get(DataComponentTypes.CUSTOM_NAME);
         if (curName != null) {
            this.nameField.setText(NBTEditor.textToLegacy(curName).replace('§', '&'));
         }

         this.addDrawableChild(this.nameField);
         y += 40;
         this.addLabel(left, y, "Lore  (separate lines with ; )");
         this.loreField = new TextFieldWidget(this.mc.textRenderer, left, y + 14, fieldW, 18, Text.literal(""));
         this.loreField.setMaxLength(2000);
         LoreComponent lore = (LoreComponent)hand.get(DataComponentTypes.LORE);
         if (lore != null) {
            StringBuilder sb = new StringBuilder();

            for (Text t : lore.lines()) {
               if (sb.length() > 0) {
                  sb.append(";");
               }

               sb.append(NBTEditor.textToLegacy(t).replace('§', '&'));
            }

            this.loreField.setText(sb.toString());
         }

         this.addDrawableChild(this.loreField);
         y += 40;
         this.addLabel(left, y, "Damage  (durability damage, 0 = full)");
         this.damageField = new TextFieldWidget(this.mc.textRenderer, left, y + 14, fieldW, 18, Text.literal(""));
         this.damageField.setMaxLength(10);
         int damage = (Integer)hand.getOrDefault(DataComponentTypes.DAMAGE, 0);
         if (damage > 0) {
            this.damageField.setText(String.valueOf(damage));
         }

         this.addDrawableChild(this.damageField);
         y += 40;
         this.addLabel(left, y, "Count  (1-99)");
         this.countField = new TextFieldWidget(this.mc.textRenderer, left, y + 14, fieldW, 18, Text.literal(""));
         this.countField.setMaxLength(3);
         this.countField.setText(String.valueOf(hand.getCount()));
         this.addDrawableChild(this.countField);
         y += 40;
         this.addLabel(left, y, "Enchantments  (id:level, comma separated, e.g. sharpness:10,unbreaking:3)");
         this.enchField = new TextFieldWidget(this.mc.textRenderer, left, y + 14, fieldW, 18, Text.literal(""));
         this.enchField.setMaxLength(2000);
         this.addDrawableChild(this.enchField);
         y += 40;
         this.addLabel(left, y, "Attributes  (id;amount;slot;op, e.g. attack_damage;20;mainhand;add_value)");
         this.attrField = new TextFieldWidget(this.mc.textRenderer, left, y + 14, fieldW, 18, Text.literal(""));
         this.attrField.setMaxLength(2000);
         this.addDrawableChild(this.attrField);
         y += 40;
         this.addLabel(left, y, "Raw NBT  (written to custom_data, e.g. {HideFlags:32})");
         this.rawField = new TextFieldWidget(this.mc.textRenderer, left, y + 14, fieldW, 18, Text.literal(""));
         this.rawField.setMaxLength(20000);
         this.addDrawableChild(this.rawField);
         y += 40;
         this.unbreakable = hand.contains(DataComponentTypes.UNBREAKABLE);
         this.unbreakableBtn = ButtonWidget.builder(Text.literal(this.unbreakable ? "Unbreakable: ON" : "Unbreakable: OFF"), b -> {
            this.unbreakable = !this.unbreakable;
            b.setMessage(Text.literal(this.unbreakable ? "Unbreakable: ON" : "Unbreakable: OFF"));
         }).dimensions(left, y, 160, 20).build();
         this.addDrawableChild(this.unbreakableBtn);
         y += 32;
         this.actionY = y + 6;
         int btnW = 150;
         ButtonWidget done = ButtonWidget.builder(Text.literal("Done"), b -> this.applyAndClose()).dimensions(left, y, btnW, 20).build();
         this.addDrawableChild(done);
         ButtonWidget cancel = ButtonWidget.builder(Text.literal("Cancel"), b -> this.client.setScreen(null))
            .dimensions(left + btnW + 10, y, btnW, 20)
            .build();
         this.addDrawableChild(cancel);
      }

      private void addLabel(int x, int y, String text) {
         this.labels.add(new NBTEditor.NBTEditorScreen.Label(x, y, text));
      }

      public void render(DrawContext context, int mouseX, int mouseY, float delta) {
         super.render(context, mouseX, mouseY, delta);

         for (NBTEditor.NBTEditorScreen.Label l : this.labels) {
            context.drawText(this.mc.textRenderer, Text.literal(l.text), l.x, l.y, 16755200, false);
         }

         context.drawText(
            this.mc.textRenderer, Text.literal("Editing: " + this.mc.player.getMainHandStack().getName().getString()), 12, 14, 5614335, false
         );
         int px = this.width - 64;
         int py = 12;
         ItemStack hand = this.mc.player.getMainHandStack();
         RenderUtils.drawItem(context, hand, px, py, 3.0F, true, null, false);
         context.drawText(this.mc.textRenderer, Text.literal(hand.getCount() + "x"), px - 4, py + 52, 11184810, false);
         List<String> errs = this.validateInputs();
         int ey = this.actionY + 28;
         if (errs.isEmpty()) {
            context.drawText(this.mc.textRenderer, Text.literal("§aAll inputs valid."), 12, ey, 5635925, false);
         } else {
            context.drawText(this.mc.textRenderer, Text.literal("§cInvalid: " + String.join("  |  ", errs)), 12, ey, 16733525, false);
         }

         context.drawText(
            this.mc.textRenderer,
            Text.literal("§7& -> §, ; separates lore/attr. Custom NBT merges into custom_data."),
            12,
            this.height - 14,
            8947848,
            false
         );
      }

      private List<String> validateInputs() {
         List<String> errs = new ArrayList<>();
         if (this.mc.world == null) {
            return errs;
         } else {
            Registry<Enchantment> er = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

            for (String part : this.enchField.getText().split(",")) {
               String t = part.trim();
               if (!t.isEmpty()) {
                  String[] kv = t.split(":", 2);
                  if (kv.length < 2) {
                     errs.add("ench fmt:" + t);
                  } else {
                     Identifier id = Identifier.of(kv[0].contains(":") ? kv[0].trim() : "minecraft:" + kv[0].trim());
                     if (er.getEntry(id).isEmpty()) {
                        errs.add("unknown ench:" + kv[0].trim());
                     }

                     try {
                        Integer.parseInt(kv[1].trim());
                     } catch (NumberFormatException var16) {
                        errs.add("ench lvl:" + kv[1].trim());
                     }
                  }
               }
            }

            Registry<EntityAttribute> ar = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ATTRIBUTE);

            for (String partx : this.attrField.getText().split(",")) {
               String t = partx.trim();
               if (!t.isEmpty()) {
                  String[] f = t.split(";");
                  if (f.length < 4) {
                     errs.add("attr fmt:" + t);
                  } else {
                     Identifier aid = Identifier.of(f[0].contains(":") ? f[0].trim() : "minecraft:" + f[0].trim());
                     if (ar.getEntry(aid).isEmpty()) {
                        errs.add("unknown attr:" + f[0].trim());
                     }

                     try {
                        Double.parseDouble(f[1].trim());
                     } catch (NumberFormatException var15) {
                        errs.add("attr amt:" + f[1].trim());
                     }
                  }
               }
            }

            String dmg = this.damageField.getText().trim();
            if (!dmg.isEmpty()) {
               try {
                  Integer.parseInt(dmg);
               } catch (NumberFormatException var14) {
                  errs.add("damage NaN");
               }
            }

            String cnt = this.countField.getText().trim();
            if (!cnt.isEmpty()) {
               try {
                  Integer.parseInt(cnt);
               } catch (NumberFormatException var13) {
                  errs.add("count NaN");
               }
            }

            String raw = this.rawField.getText().trim();
            if (!raw.isEmpty()) {
               try {
                  StringNbtReader.readCompound(raw);
               } catch (Exception var12) {
                  errs.add("raw NBT: " + var12.getMessage());
               }
            }

            return errs;
         }
      }

      private void applyAndClose() {
         ItemStack hand = this.mc.player.getMainHandStack();
         if (hand.isEmpty()) {
            this.client.setScreen(null);
         } else {
            String name = this.nameField.getText().trim();
            if (!name.isEmpty()) {
               hand.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name.replace('&', '§')));
            } else {
               hand.remove(DataComponentTypes.CUSTOM_NAME);
            }

            String lore = this.loreField.getText().trim();
            if (!lore.isEmpty()) {
               List<Text> lines = new ArrayList<>();

               for (String line : lore.split(";")) {
                  String trimmed = line.trim();
                  if (!trimmed.isEmpty()) {
                     lines.add(Text.literal(trimmed.replace('&', '§')));
                  }
               }

               hand.set(DataComponentTypes.LORE, new LoreComponent(lines));
            } else {
               hand.remove(DataComponentTypes.LORE);
            }

            String damageStr = this.damageField.getText().trim();
            if (!damageStr.isEmpty()) {
               try {
                  hand.set(DataComponentTypes.DAMAGE, Integer.parseInt(damageStr));
               } catch (NumberFormatException var25) {
                  ChatUtils.error("NBTEditor: Damage invalid, skipped.", new Object[0]);
               }
            } else {
               hand.remove(DataComponentTypes.DAMAGE);
            }

            String countStr = this.countField.getText().trim();
            if (!countStr.isEmpty()) {
               try {
                  int cnt = Math.max(1, Math.min(99, Integer.parseInt(countStr)));
                  hand.setCount(cnt);
               } catch (NumberFormatException var24) {
                  ChatUtils.error("NBTEditor: Count invalid, skipped.", new Object[0]);
               }
            }

            if (this.unbreakable) {
               hand.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
            } else {
               hand.remove(DataComponentTypes.UNBREAKABLE);
            }

            hand.remove(DataComponentTypes.ENCHANTMENTS);
            String ench = this.enchField.getText().trim();
            if (!ench.isEmpty() && this.mc.world != null) {
               Registry<Enchantment> reg = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

               for (String part : ench.split(",")) {
                  String[] kv = part.split(":", 2);
                  if (kv.length >= 2) {
                     Identifier id = Identifier.of(kv[0].contains(":") ? kv[0].trim() : "minecraft:" + kv[0].trim());

                     try {
                        int lvl = Integer.parseInt(kv[1].trim());
                        reg.getEntry(id).ifPresent(r -> hand.addEnchantment(r, lvl));
                     } catch (NumberFormatException var23) {
                     }
                  }
               }
            }

            hand.remove(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            String attrs = this.attrField.getText().trim();
            if (!attrs.isEmpty() && this.mc.world != null) {
               Registry<EntityAttribute> areg = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ATTRIBUTE);
               Builder builder = AttributeModifiersComponent.builder();

               for (String partx : attrs.split(",")) {
                  String[] f = partx.split(";");
                  if (f.length >= 4) {
                     Identifier aid = Identifier.of(f[0].contains(":") ? f[0].trim() : "minecraft:" + f[0].trim());

                     try {
                        double amt = Double.parseDouble(f[1].trim());
                        AttributeModifierSlot slot = this.parseSlot(f[2].trim());
                        Operation op = this.parseOp(f[3].trim());
                        areg.getEntry(aid)
                           .ifPresent(
                              r -> builder.add(r, new EntityAttributeModifier(Identifier.of("bphack:" + f[0].trim()), amt, op), slot)
                           );
                     } catch (NumberFormatException var22) {
                     }
                  }
               }

               hand.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
            }

            String raw = this.rawField.getText().trim();
            if (!raw.isEmpty()) {
               try {
                  NbtCompound nbt = StringNbtReader.readCompound(raw);
                  hand.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
               } catch (Exception var21) {
                  ChatUtils.error("NBTEditor: raw NBT parse failed: " + var21.getMessage(), new Object[0]);
               }
            } else {
               hand.remove(DataComponentTypes.CUSTOM_DATA);
            }

            if (this.mc.player.isCreative()) {
               try {
                  this.mc.interactionManager.clickCreativeStack(hand, 36 + this.mc.player.getInventory().getSelectedSlot());
               } catch (Exception var20) {
               }
            }

            ChatUtils.info("NBTEditor: applied changes to held item, god item acquired.", new Object[0]);
            this.client.setScreen(null);
         }
      }

      private AttributeModifierSlot parseSlot(String s) {
         String var2 = s.toLowerCase();

         return switch (var2) {
            case "offhand", "off" -> AttributeModifierSlot.OFFHAND;
            case "feet", "boots" -> AttributeModifierSlot.FEET;
            case "legs", "pants", "leggings" -> AttributeModifierSlot.LEGS;
            case "chest", "body_armor", "armor_chest" -> AttributeModifierSlot.CHEST;
            case "head", "helmet" -> AttributeModifierSlot.HEAD;
            case "armor" -> AttributeModifierSlot.ARMOR;
            case "body" -> AttributeModifierSlot.BODY;
            case "hand", "mainhand", "main" -> AttributeModifierSlot.MAINHAND;
            default -> AttributeModifierSlot.ANY;
         };
      }

      private Operation parseOp(String s) {
         String var2 = s.toLowerCase();

         return switch (var2) {
            case "multiply_base", "add_multiplied_base", "1" -> Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total", "add_multiplied_total", "2" -> Operation.ADD_MULTIPLIED_TOTAL;
            default -> Operation.ADD_VALUE;
         };
      }

      public boolean shouldPause() {
         return false;
      }

      private record Label(int x, int y, String text) {
      }
   }
}
