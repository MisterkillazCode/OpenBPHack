package com.codigohasta.addon.modules.villager;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.VillagerProfession;

public enum VillagerType {
   Armorer(Items.BLAST_FURNACE, VillagerProfession.ARMORER),
   Butcher(Items.SMOKER, VillagerProfession.BUTCHER),
   Cartographer(Items.CARTOGRAPHY_TABLE, VillagerProfession.CARTOGRAPHER),
   Cleric(Items.BREWING_STAND, VillagerProfession.CLERIC),
   X(Items.COMPOSTER, VillagerProfession.FARMER),
   X_36(Items.BARREL, VillagerProfession.FISHERMAN),
   SystemArrow(Items.FLETCHING_TABLE, VillagerProfession.FLETCHER),
   SkinSmith(Items.CAULDRON, VillagerProfession.LEATHERWORKER),
   ImageLogic(Items.LECTERN, VillagerProfession.LIBRARIAN),
   Smith(Items.STONECUTTER, VillagerProfession.MASON),
   Player(Items.LOOM, VillagerProfession.SHEPHERD),
   Toolsmith(Items.SMITHING_TABLE, VillagerProfession.TOOLSMITH),
   WeaponSmith(Items.GRINDSTONE, VillagerProfession.WEAPONSMITH);

   private final Item item;
   private final RegistryKey<VillagerProfession> profession;

   private VillagerType(Item item, RegistryKey<VillagerProfession> profession) {
      this.item = item;
      this.profession = profession;
   }

   public Item getItem() {
      return this.item;
   }

   public RegistryKey<VillagerProfession> getProfession() {
      return this.profession;
   }

   public static VillagerType valueOf(RegistryEntry<VillagerProfession> profession) {
      for (VillagerType villagerType : values()) {
         if (profession.matchesKey(villagerType.getProfession())) {
            return villagerType;
         }
      }

      return null;
   }
}
