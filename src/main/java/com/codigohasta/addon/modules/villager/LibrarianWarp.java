package com.codigohasta.addon.modules.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class LibrarianWarp {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final UUID uuid;
   private final BlockPos operatePos;
   private final Vec3d operatePosCenter;
   private boolean discovered = false;
   private long lastTradeTime = 0L;
   private long tradeTimes = 0L;
   private final List<LibrarianWarp.LibrarianOffer> offers = new ArrayList<>();

   public LibrarianWarp(UUID uuid, BlockPos operatePos) {
      this.uuid = uuid;
      this.operatePos = operatePos;
      this.operatePosCenter = operatePos.toCenterPos();
   }

   public VillagerEntity getVillager() {
      if (mc.world == null) {
         return null;
      } else {
         try {
            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof VillagerEntity && entity.getUuid().equals(this.uuid)) {
                  return (VillagerEntity)entity;
               }
            }
         } catch (Exception var3) {
            ChatUtils.error("GetImageLogicStatus :" + var3.getMessage(), new Object[0]);
         }

         return null;
      }
   }

   public void addOffer(LibrarianWarp.LibrarianOffer offer) {
      this.offers.add(offer);
   }

   public void clearOffers() {
      this.offers.clear();
   }

   public List<LibrarianWarp.LibrarianOffer> getOffers() {
      return this.offers;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public BlockPos getOperatePos() {
      return this.operatePos;
   }

   public Vec3d getOperatePosCenter() {
      return this.operatePosCenter;
   }

   public boolean isDiscovered() {
      return this.discovered;
   }

   public void setDiscovered(boolean discovered) {
      this.discovered = discovered;
   }

   public long getLastTradeTime() {
      return this.lastTradeTime;
   }

   public void setLastTradeTime(long lastTradeTime) {
      this.lastTradeTime = lastTradeTime;
   }

   public long getTradeTimes() {
      return this.tradeTimes;
   }

   public void setTradeTimes(long tradeTimes) {
      this.tradeTimes = tradeTimes;
   }

   public static class LibrarianOffer {
      private final int tradeIndex;
      private final RegistryKey<Enchantment> enchantment;
      private final int level;
      private final int emeraldPrice;
      private final int originalPrice;
      private boolean outOfStock;

      public LibrarianOffer(int tradeIndex, RegistryKey<Enchantment> enchantment, int level, int emeraldPrice, int originalPrice, boolean outOfStock) {
         this.tradeIndex = tradeIndex;
         this.enchantment = enchantment;
         this.level = level;
         this.emeraldPrice = emeraldPrice;
         this.originalPrice = originalPrice;
         this.outOfStock = outOfStock;
      }

      public int getOriginalPrice() {
         return this.originalPrice;
      }

      public int getTradeIndex() {
         return this.tradeIndex;
      }

      public RegistryKey<Enchantment> getEnchantment() {
         return this.enchantment;
      }

      public int getLevel() {
         return this.level;
      }

      public int getEmeraldPrice() {
         return this.emeraldPrice;
      }

      public boolean isOutOfStock() {
         return this.outOfStock;
      }

      public void setOutOfStock(boolean outOfStock) {
         this.outOfStock = outOfStock;
      }
   }
}
