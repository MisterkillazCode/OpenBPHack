package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Oxidizable;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AutoDeoxidizer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> stripLogs = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("LogStrip")).description("whetherAutoLogForStripLog.")).defaultValue(true)).build());
   private final Setting<Boolean> removeWax = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Remove")).description("whetherTimeRemove's (Wax).")).defaultValue(true)).build());
   private final Setting<Boolean> checkHand = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("CheckHandHoldHead")).description("ifyouHandinsideAlreadyHead, notProceedAutoSwitch."))
               .defaultValue(true))
            .build()
      );
   private final Setting<List<Item>> handBlacklist = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("HandHoldBlackNameSingle"))
               .description("HandHoldthisItemTimenotTriggerFeature."))
            .defaultValue(
               new Item[]{Items.HONEYCOMB, Items.FLINT_AND_STEEL, Items.SHEARS, Items.GLOW_INK_SAC, Items.INK_SAC, Items.BRUSH, Items.BONE_MEAL}
            )
            .build()
      );
   private final Setting<Boolean> checkInventory = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("CheckPack")).description("iffastnoHead, whetherfromPackinTimeMove.")).defaultValue(true)).build());
   private final Map<Block, Block> stripMap = new HashMap<>();
   private final Map<Block, Block> waxMap = new HashMap<>();

   public AutoDeoxidizer() {
      super(AddonTemplate.CATEGORY, "AutoDeoxidizer", "Auto-selects the right tool on hold-right-click and runs remove/strip logic for oxidized copper.");
   }

   public void onActivate() {
      this.initStripMap();
      this.initWaxMap();
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         if (event.hand == Hand.MAIN_HAND) {
            BlockHitResult hitResult = event.result;
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = this.mc.world.getBlockState(pos);
            Block block = state.getBlock();
            if (!(Boolean)this.checkHand.get() || !this.mc.player.getMainHandStack().getItem().toString().contains("_axe")) {
               if (!((List)this.handBlacklist.get()).contains(this.mc.player.getMainHandStack().getItem())) {
                  boolean shouldScrape = false;
                  if (Oxidizable.getDecreasedOxidationState(state).isPresent()) {
                     shouldScrape = true;
                  } else if ((Boolean)this.removeWax.get() && this.waxMap.containsKey(block)) {
                     shouldScrape = true;
                  } else if ((Boolean)this.stripLogs.get() && this.stripMap.containsKey(block)) {
                     shouldScrape = true;
                  }

                  if (shouldScrape) {
                     FindItemResult axe = InvUtils.find(item -> item.getItem().toString().contains("_axe"));
                     if (axe.found()) {
                        event.cancel();
                        int currentSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                        int axeSlot = axe.slot();
                        if (axe.isHotbar()) {
                           InvUtils.swap(axeSlot, true);
                           this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hitResult);
                           this.mc.player.swingHand(Hand.MAIN_HAND);
                           InvUtils.swapBack();
                        } else if ((Boolean)this.checkInventory.get()) {
                           InvUtils.move().from(axeSlot).to(currentSlot);
                           this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hitResult);
                           this.mc.player.swingHand(Hand.MAIN_HAND);
                           InvUtils.move().from(currentSlot).to(axeSlot);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void initStripMap() {
      this.stripMap.clear();
      this.stripMap.put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
      this.stripMap.put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD);
      this.stripMap.put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG);
      this.stripMap.put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD);
      this.stripMap.put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
      this.stripMap.put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD);
      this.stripMap.put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG);
      this.stripMap.put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD);
      this.stripMap.put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG);
      this.stripMap.put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD);
      this.stripMap.put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG);
      this.stripMap.put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD);
      this.stripMap.put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG);
      this.stripMap.put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD);
      this.stripMap.put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG);
      this.stripMap.put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD);
      this.stripMap.put(Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG);
      this.stripMap.put(Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD);
      this.stripMap.put(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK);
      this.stripMap.put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM);
      this.stripMap.put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE);
      this.stripMap.put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM);
      this.stripMap.put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE);
   }

   private void initWaxMap() {
      this.waxMap.clear();
      this.addWaxSet(Blocks.WAXED_COPPER_BLOCK, Blocks.COPPER_BLOCK);
      this.addWaxSet(Blocks.WAXED_EXPOSED_COPPER, Blocks.EXPOSED_COPPER);
      this.addWaxSet(Blocks.WAXED_WEATHERED_COPPER, Blocks.WEATHERED_COPPER);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_COPPER, Blocks.OXIDIZED_COPPER);
      this.addWaxSet(Blocks.WAXED_CUT_COPPER, Blocks.CUT_COPPER);
      this.addWaxSet(Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER);
      this.addWaxSet(Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER);
      this.addWaxSet(Blocks.WAXED_CUT_COPPER_SLAB, Blocks.CUT_COPPER_SLAB);
      this.addWaxSet(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_SLAB);
      this.addWaxSet(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_SLAB);
      this.addWaxSet(Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.CUT_COPPER_STAIRS);
      this.addWaxSet(Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS);
      this.addWaxSet(Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER_STAIRS);
      this.addWaxSet(Blocks.WAXED_COPPER_DOOR, Blocks.COPPER_DOOR);
      this.addWaxSet(Blocks.WAXED_EXPOSED_COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR);
      this.addWaxSet(Blocks.WAXED_WEATHERED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR);
      this.addWaxSet(Blocks.WAXED_COPPER_TRAPDOOR, Blocks.COPPER_TRAPDOOR);
      this.addWaxSet(Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR);
      this.addWaxSet(Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR, Blocks.OXIDIZED_COPPER_TRAPDOOR);
      this.addWaxSet(Blocks.WAXED_COPPER_GRATE, Blocks.COPPER_GRATE);
      this.addWaxSet(Blocks.WAXED_EXPOSED_COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE);
      this.addWaxSet(Blocks.WAXED_WEATHERED_COPPER_GRATE, Blocks.WEATHERED_COPPER_GRATE);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_COPPER_GRATE, Blocks.OXIDIZED_COPPER_GRATE);
      this.addWaxSet(Blocks.WAXED_COPPER_BULB, Blocks.COPPER_BULB);
      this.addWaxSet(Blocks.WAXED_EXPOSED_COPPER_BULB, Blocks.EXPOSED_COPPER_BULB);
      this.addWaxSet(Blocks.WAXED_WEATHERED_COPPER_BULB, Blocks.WEATHERED_COPPER_BULB);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_COPPER_BULB, Blocks.OXIDIZED_COPPER_BULB);
      this.addWaxSet(Blocks.WAXED_CHISELED_COPPER, Blocks.CHISELED_COPPER);
      this.addWaxSet(Blocks.WAXED_EXPOSED_CHISELED_COPPER, Blocks.EXPOSED_CHISELED_COPPER);
      this.addWaxSet(Blocks.WAXED_WEATHERED_CHISELED_COPPER, Blocks.WEATHERED_CHISELED_COPPER);
      this.addWaxSet(Blocks.WAXED_OXIDIZED_CHISELED_COPPER, Blocks.OXIDIZED_CHISELED_COPPER);
   }

   private void addWaxSet(Block waxed, Block unwaxed) {
      if (waxed != null && unwaxed != null) {
         this.waxMap.put(waxed, unwaxed);
      }
   }
}
