package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class AutoRefreshTrade extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> range = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Range")).description("Reach Distance")).defaultValue(5)).min(0).sliderMax(12).build());
   private final Setting<Integer> wallRange = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("WallRange")).description("Reach Distance")).defaultValue(5)).min(0).sliderMax(12).build());
   private final Setting<Integer> waitMine = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("WaitMineDelay")).description("WaitMineDelay(MS)")).defaultValue(5000))
            .min(0)
            .sliderMax(10000)
            .build()
      );
   private final Setting<Set<RegistryKey<Enchantment>>> enchantmentList = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)new meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder()
                  .name("EnchantmentsList"))
               .description("TargetList"))
            .build()
      );
   private final Setting<Integer> enchantmentLevel = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Level")).description("Target")).defaultValue(3)).min(0).sliderMax(5).build());
   public BlockPos pos = null;
   public Timer timer = new Timer();

   public AutoRefreshTrade() {
      super(AddonTemplate.SC_CATEGORY, "AutoRefreshTrade", "Automatically refreshes villager trades.");
   }

   public void onActivate() {
      this.pos = null;
      this.timer.setMs(999999L);
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.timer.passedMs((long)((Integer)this.waitMine.get()).intValue())) {
         if (this.mc.options.backKey.isPressed()) {
            this.toggle();
         } else if (this.pos != null && this.mc.world.isAir(this.pos)) {
            int slot = this.findItem(Items.LECTERN);
            int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            if (slot != -1) {
               InventoryUtil.switchToSlot(slot);
               Direction side = BlockUtil.getPlaceSide(this.pos, null);
               if (side != null) {
                  BlockUtil.placeBlock(this.pos, side, true);
                  InventoryUtil.switchToSlot(old);
                  Rotation.snapBack();
               }
            }
         } else {
            VillagerEntity target = this.getTarget();
            if (target != null) {
               Rotation.snapAt(target.getEyePos());
               Vec3d playerPos = this.mc.player.getEyePos();
               Vec3d villagerPos = target.getEyePos();
               EntityHitResult hitResult = ProjectileUtil.raycast(
                  this.mc.player, playerPos, villagerPos, target.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(villagerPos)
               );
               if (hitResult == null) {
                  this.mc.interactionManager.interactEntity(this.mc.player, target, Hand.MAIN_HAND);
               } else {
                  ActionResult result = this.mc.interactionManager.interactEntityAtLocation(this.mc.player, target, hitResult, Hand.MAIN_HAND);
                  if (!result.isAccepted()) {
                     this.mc.interactionManager.interactEntity(this.mc.player, target, Hand.MAIN_HAND);
                  }
               }

               if (this.mc.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
                  TradeOfferList list = handler.getRecipes();
                  AtomicBoolean find = new AtomicBoolean(false);
                  boolean findBook = false;

                  for (int size = 0; size < list.size(); size++) {
                     TradeOffer tradeOffer = (TradeOffer)list.get(size);
                     ItemStack sellStack = tradeOffer.getSellItem();
                     Item item = sellStack.getItem();
                     if (item == Items.ENCHANTED_BOOK) {
                        findBook = true;
                        ItemEnchantmentsComponent enchantments = (ItemEnchantmentsComponent)sellStack.getOrDefault(
                           DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT
                        );
                        enchantments.getEnchantments().forEach(entry -> {
                           int level = enchantments.getLevel(entry);
                           int maxLevel = ((Enchantment)entry.value()).getMaxLevel();
                           String name = Enchantment.getName(entry, level).getString();
                           this.mc.player.sendMessage(Text.of("[LeavesHack]ThisTimes" + name), false);

                           for (RegistryKey<Enchantment> enchantmentKey : (Set)this.enchantmentList.get()) {
                              if (hasEnchantments(sellStack, enchantmentKey) && (level >= (Integer)this.enchantmentLevel.get() || level == maxLevel)) {
                                 find.set(true);
                                 this.mc.player.sendMessage(Text.of("[LeavesHack]:Alreadyto"), false);
                                 return;
                              }
                           }
                        });
                     }
                  }

                  if (!findBook) {
                     this.mc.player.sendMessage(Text.of("[LeavesHack]:ThisTimesto"), false);
                  }

                  this.mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(this.mc.player.currentScreenHandler.syncId));
                  this.mc.currentScreen.close();
                  if (find.get()) {
                     this.toggle();
                     return;
                  }

                  Direction facing1 = this.mc.player.getHorizontalFacing();
                  switch (facing1) {
                     case NORTH:
                        this.pos = this.mc.player.getBlockPos().north();
                        break;
                     case SOUTH:
                        this.pos = this.mc.player.getBlockPos().south();
                        break;
                     case EAST:
                        this.pos = this.mc.player.getBlockPos().east();
                        break;
                     case WEST:
                        this.pos = this.mc.player.getBlockPos().west();
                        break;
                     default:
                        this.pos = this.mc.player.getBlockPos();
                  }

                  Rotation.snapAt(this.pos.toCenterPos());
                  this.mc.interactionManager.attackBlock(this.pos, BlockUtils.getClosestPlaceSide(this.pos));
                  this.timer.reset();
               }
            }
         }
      }
   }

   public int findItem(Item input) {
      for (int i = 0; i < 9; i++) {
         Item item = this.getStackInSlot(i).getItem();
         if (Item.getRawId(item) == Item.getRawId(input)) {
            return i;
         }
      }

      return -1;
   }

   public ItemStack getStackInSlot(int i) {
      return this.mc.player.getInventory().getStack(i);
   }

   @EventHandler
   private void onRender3d(Render3DEvent event) {
      if (this.pos != null) {
         Color color = new Color(50, 232, 252, 80);
         event.renderer.box(this.pos, color, color, ShapeMode.Both, 0);
      }
   }

   private VillagerEntity getTarget() {
      Entity target = null;
      double distance = ((Integer)this.range.get()).intValue();

      for (Entity entity : this.mc.world.getEntities()) {
         if (entity instanceof VillagerEntity
            && (this.mc.player.canSee(entity) || !(this.mc.player.distanceTo(entity) > ((Integer)this.wallRange.get()).intValue()))) {
            if (target == null) {
               target = entity;
               distance = this.mc.player.distanceTo(entity);
            } else if (this.mc.player.distanceTo(entity) < distance) {
               target = entity;
               distance = this.mc.player.distanceTo(entity);
            }
         }
      }

      return (VillagerEntity)target;
   }

   public static boolean hasEnchantments(ItemStack itemStack, RegistryKey<Enchantment>... enchantments) {
      if (itemStack.isEmpty()) {
         return false;
      } else {
         Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap();
         Utils.getEnchantments(itemStack, itemEnchantments);

         for (RegistryKey<Enchantment> enchantment : enchantments) {
            if (!hasEnchantment(itemEnchantments, enchantment)) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean hasEnchantment(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantmentKey) {
      ObjectIterator var2 = itemEnchantments.keySet().iterator();

      while (var2.hasNext()) {
         RegistryEntry<Enchantment> enchantment = (RegistryEntry<Enchantment>)var2.next();
         if (enchantment.matchesKey(enchantmentKey)) {
            return true;
         }
      }

      return false;
   }
}
