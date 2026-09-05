package com.codigohasta.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.ICustomGoalProcess;
import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.modules.villager.EnchantSortConfig;
import com.codigohasta.addon.modules.villager.LibrarianStep;
import com.codigohasta.addon.modules.villager.LibrarianWarp;
import com.codigohasta.addon.modules.villager.ShulkerManager;
import com.codigohasta.addon.modules.villager.VillagerMode;
import com.codigohasta.addon.utils.heutil.HeBlockUtils;
import com.codigohasta.addon.utils.heutil.HeInvUtils;
import com.codigohasta.addon.utils.heutil.HeRotationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

public class AutoLibrarian extends Module implements AbstractGameEventListener {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTarget = this.settings.createGroup("Target");
   private final SettingGroup sgVillager = this.settings.createGroup("VillagerMechanism");
   private final SettingGroup sgDelay = this.settings.createGroup("DelaySystem");
   private final SettingGroup sgSupply = this.settings.createGroup("giveSetting");
   private final SettingGroup sgMemory = this.settings.createGroup("Memory System");
   private final Setting<Integer> minDistance = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("OperationRange")).description("OperationTolerateDeviceandVillager'sTriggerDistance"))
               .min(2)
               .sliderMax(4)
               .defaultValue(2))
            .build()
      );
   private final Setting<Integer> searchRange = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("TolerateDeviceRange")).description("MoveTimeScanTolerateDevice'sRadius"))
               .min(6)
               .sliderMax(30)
               .defaultValue(15))
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> emeraldStorage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
               .name("GreenTolerateDevice"))
            .defaultValue(new BlockEntityType[]{BlockEntityType.BARREL})
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> bookStorage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
               .name("NormalTolerateDevice"))
            .defaultValue(new BlockEntityType[]{BlockEntityType.CHEST})
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> dumpStorage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
               .name("TolerateDevice"))
            .defaultValue(new BlockEntityType[]{BlockEntityType.TRAPPED_CHEST})
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> emptyBoxStorage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
               .name("AirgiveTolerateDevice"))
            .defaultValue(new BlockEntityType[]{BlockEntityType.DROPPER})
            .build()
      );
   private final Setting<Set<RegistryKey<Enchantment>>> targets = this.sgTarget
      .add(
         ((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)new meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder()
                     .name("TargetSingle"))
                  .description("SelectyouwantExchange'shave"))
               .defaultValue(Collections.emptySet()))
            .build()
      );
   private final Setting<VillagerMode> mode = this.sgTarget
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("ExchangeMode"))
                  .description("SystemtoVillager/'sTolerateDegree"))
               .defaultValue(VillagerMode.OnlyNoPremium))
            .build()
      );
   private final Setting<Integer> maxPrice = this.sgTarget
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mosthighCanConnect")).description("SingleThispastmanyfewGreenjustnotBuy"))
               .min(1)
               .sliderMax(64)
               .defaultValue(30))
            .build()
      );
   private final Setting<Integer> checkCooldown = this.sgVillager
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Out of StockCooldown()")).description("VillagerOut of Stockafter, WaitmanyTimeshe"))
               .min(10)
               .sliderMax(120)
               .defaultValue(30))
            .build()
      );
   private final Setting<Integer> maxExhaustions = this.sgVillager
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("f***ExchangeupLimit")).description("OneVillagerSkymostmanybyBuyAirTimes(For3Times)"))
               .min(1)
               .sliderMax(4)
               .defaultValue(3))
            .build()
      );
   private final Setting<Integer> workEndTick = this.sgVillager
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("downTime(Tick)")).description("VillagerStopStopDo'sTime"))
               .min(8000)
               .sliderMax(12000)
               .defaultValue(9000))
            .build()
      );
   private final Setting<Integer> windowDelay = this.sgDelay
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("FaceDelay(Tick)")).description("OpenDisableTolerateDeviceTime'sWaitTime"))
               .min(1)
               .sliderMax(20)
               .defaultValue(5))
            .build()
      );
   private final Setting<Integer> clickDelay = this.sgDelay
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("pointStrikeDelay(Tick)")).description("atFacein, ExchangeItem'sBetween"))
               .min(1)
               .sliderMax(10)
               .defaultValue(2))
            .build()
      );
   private final Setting<Integer> supplyEmeraldStacks = this.sgSupply
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("GreengiveAmount()")).description("TimesgomanyfewGreen")).min(1).sliderMax(27).defaultValue(9))
            .build()
      );
   private final Setting<Integer> supplyBookStacks = this.sgSupply
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("NormalgiveAmount()")).description("TimesgomanyfewNormal")).min(1).sliderMax(27).defaultValue(4))
            .build()
      );
   private final Setting<Boolean> useMemory = this.sgMemory
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("EnableVillagerMemory"))
                  .description("Enableafter, DisableOpenModulenotwillHeavyNewScanAlreadyVillager, goTime."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> forceReset = this.sgMemory
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ForceHeavyNewScan"))
                  .description("EnableafterEnableModule, MemoryHeavyNew. ScanMoveafterTogglewillAutoDisable."))
               .defaultValue(false))
            .build()
      );
   private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
   private final ICustomGoalProcess customGoalProcess = this.baritone.getCustomGoalProcess();
   private final Settings baritoneSettings = BaritoneAPI.getSettings();
   private LibrarianStep step = LibrarianStep.NONE;
   private LibrarianStep nextStep = null;
   private LibrarianStep closeScreenNextStep = null;
   private final EnchantSortConfig sortConfig = new EnchantSortConfig();
   private final ShulkerManager shulkerManager = new ShulkerManager();
   private List<LibrarianWarp> librarianList = new ArrayList<>();
   private LibrarianWarp currentTarget = null;
   private LibrarianWarp.LibrarianOffer currentOffer = null;
   private BlockPos emeraldPos;
   private BlockPos bookPos;
   private BlockPos dumpPos;
   private BlockPos emptyBoxPos;
   private BlockPos sortAreaCenter;
   private int timer = 0;
   private int requiredEmeralds = 0;
   private int requiredBooks = 0;
   private boolean wait = false;
   private boolean tradedThisSession = false;
   private int currentSortItemSlot = -1;
   private BlockPos currentSortBoxPos = null;
   private String memoryWorld = "";

   public AutoLibrarian() {
      super(
         AddonTemplate.SC_CATEGORY,
         "AutoLibrarian",
         "Auto librarian trade; auto-inserts books. Incomplete: currently only force-starts, mechanics to be finished (based on xiaohe666 lotus update)."
      );
      this.baritone.getGameEventHandler().registerEventListener(this);
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.world != null) {
         String currentWorld = this.mc.world.getRegistryKey().getValue().toString();
         boolean worldChanged = !currentWorld.equals(this.memoryWorld);
         if (!(Boolean)this.forceReset.get() && !worldChanged && (Boolean)this.useMemory.get() && !this.librarianList.isEmpty()) {
            this.info("\ud83e\udde0 TriggerMemory System: AlreadyLoad" + this.librarianList.size() + "VillagerData, pastDirectExchange!", new Object[0]);
            this.librarianList.forEach(warp -> {
               warp.setTradeTimes(0L);
               warp.setLastTradeTime(0L);
               warp.getOffers().forEach(offer -> offer.setOutOfStock(false));
            });
            this.step = LibrarianStep.CHECK_SUPPLY;
         } else {
            this.info("\ud83d\udd04 Mode: atScanVillagerOldMemory...", new Object[0]);
            this.librarianList.clear();
            this.memoryWorld = currentWorld;

            for (Entity entity : this.mc.world.getEntities()) {
               if (entity instanceof VillagerEntity villager && villager.getVillagerData().profession().matchesKey(VillagerProfession.LIBRARIAN)) {
                  BlockPos lecternPos = this.getOperatePos(villager);
                  if (lecternPos != null) {
                     this.librarianList.add(new LibrarianWarp(villager.getUuid(), lecternPos));
                  }
               }
            }

            if ((Boolean)this.forceReset.get()) {
               this.forceReset.set(false);
            }

            if (this.librarianList.isEmpty()) {
               this.warning("Complete. notohavehaveEffectBiliBit'sImageLogic!", new Object[0]);
               this.toggle();
               return;
            }

            this.info("Lock" + this.librarianList.size() + "NameImageLogic, Preparestart!", new Object[0]);
            this.step = LibrarianStep.INIT_SCAN;
         }

         this.scanContainers();
         if (this.emeraldPos != null && this.bookPos != null) {
            this.sortAreaCenter = this.mc.player.getBlockPos();
            this.baritoneSettings.allowBreak.value = false;
            this.baritoneSettings.allowPlace.value = false;
            this.wait = false;
            this.timer = 0;
            this.tradedThisSession = false;
         } else {
            this.warning("fewwant'sgiveTolerateDevice(Green/Normal), ModuleDisable!", new Object[0]);
            this.toggle();
         }
      } else {
         this.toggle();
      }
   }

   public void onDeactivate() {
      this.step = LibrarianStep.NONE;
      this.nextStep = null;
      this.baritone.getCommandManager().execute("cancel");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.checkAndDecrement()) {
         switch (this.step) {
            case INIT_SCAN:
               this.doInitScan();
               break;
            case OPEN_FOR_DISCOVER:
               this.doDiscoverTrade();
               break;
            case CHECK_SUPPLY:
               this.checkSupplyAndTarget();
               break;
            case GOTO_EMERALD:
               this.gotoIfNeed(this.emeraldPos, "beforegiveGreen", LibrarianStep.TAKE_EMERALD);
               break;
            case TAKE_EMERALD:
               this.takeItems(
                  this.emeraldPos, Items.EMERALD, Math.max(this.requiredEmeralds, (Integer)this.supplyEmeraldStacks.get() * 64), LibrarianStep.CHECK_SUPPLY
               );
               break;
            case GOTO_BASE_BOOK:
               this.gotoIfNeed(this.bookPos, "beforegiveNormal", LibrarianStep.TAKE_BASE_BOOK);
               break;
            case TAKE_BASE_BOOK:
               this.takeItems(
                  this.bookPos, Items.BOOK, Math.max(this.requiredBooks, (Integer)this.supplyBookStacks.get() * 64), LibrarianStep.CHECK_SUPPLY
               );
               break;
            case NEXT_LIBRARIAN:
               this.nextLibrarian();
               break;
            case OPEN_TRADE:
               this.openTradeGUI();
               break;
            case EXECUTE_TRADE:
               this.executeTrade();
               break;
            case CLOSE_SCREEN_AND_NEXT:
               this.closeScreenAndNext();
               break;
            case GOTO_SORT_AREA:
               this.gotoIfNeed(this.sortAreaCenter, "beforeCategory", LibrarianStep.SORT_BOOKS);
               break;
            case SORT_BOOKS:
               this.sortBooksLogic();
               break;
            case HANDLE_FULL_BOX:
               this.prepareShulkerReplacement();
               break;
            case BREAK_FULL_BOX:
               if (this.shulkerManager.tickBreakFullBox()) {
                  this.step = LibrarianStep.GOTO_DUMP_CHEST;
               }
               break;
            case GOTO_DUMP_CHEST:
               this.gotoIfNeed(this.dumpPos, "before", LibrarianStep.DUMP_FULL_BOX);
               break;
            case DUMP_FULL_BOX:
               if (this.shulkerManager.tickDumpFullBox(this.dumpPos)) {
                  this.step = LibrarianStep.GOTO_EMPTY_BOX_CHEST;
               }
               break;
            case GOTO_EMPTY_BOX_CHEST:
               this.gotoIfNeed(this.emptyBoxPos, "beforeAirgive", LibrarianStep.TAKE_EMPTY_BOX);
               break;
            case TAKE_EMPTY_BOX:
               if (this.shulkerManager.tickTakeEmptyBox(this.emptyBoxPos)) {
                  this.step = LibrarianStep.GOTO_PLACE_POS;
               }
               break;
            case GOTO_PLACE_POS:
               this.gotoIfNeed(this.currentSortBoxPos, "toby'sPosition", LibrarianStep.PLACE_NEW_BOX);
               break;
            case PLACE_NEW_BOX:
               if (this.shulkerManager.tickPlaceNewBox()) {
                  this.step = LibrarianStep.SORT_BOOKS;
               }
               break;
            case WAIT:
               this.handleWait();
               break;
            case WALK_TO_UNDISCOVERED:
            case WALKING_TO_VILLAGER:
               this.none();
         }
      }
   }

   private boolean checkAndDecrement() {
      if (this.timer > 0) {
         this.timer--;
         return false;
      } else {
         return true;
      }
   }

   private void setDelay(int d) {
      this.timer = d;
   }

   private void none() {
   }

   private void doInitScan() {
      for (LibrarianWarp warp : this.librarianList) {
         if (!warp.isDiscovered()) {
            this.currentTarget = warp;
            this.gotoIfNeed(warp.getOperatePos(), "Villager", LibrarianStep.OPEN_FOR_DISCOVER);
            return;
         }
      }

      this.info("AllVillagerComplete, startExchange!", new Object[0]);
      this.step = LibrarianStep.CHECK_SUPPLY;
   }

   private void doDiscoverTrade() {
      VillagerEntity villager = this.currentTarget.getVillager();
      if (villager != null && villager.isAlive()) {
         if (!(this.mc.currentScreen instanceof MerchantScreen)) {
            this.openEntityGUI(villager, LibrarianStep.OPEN_FOR_DISCOVER);
         } else {
            MerchantScreenHandler handler = (MerchantScreenHandler)this.mc.player.currentScreenHandler;
            TradeOfferList offers = handler.getRecipes();
            this.currentTarget.clearOffers();

            for (int i = 0; i < offers.size(); i++) {
               TradeOffer trade = (TradeOffer)offers.get(i);
               ItemStack sellItem = trade.getSellItem();
               if (sellItem.getItem().toString().contains("enchanted_book")) {
                  RegistryKey<Enchantment> mainEnchant = this.sortConfig.getMainEnchantment(sellItem);
                  if (mainEnchant != null) {
                     int cPrice = trade.getDisplayedFirstBuyItem().getCount();
                     int oPrice = trade.getOriginalFirstBuyItem().getCount();
                     int level = this.sortConfig.getEnchantmentLevel(sellItem, mainEnchant);
                     this.currentTarget.addOffer(new LibrarianWarp.LibrarianOffer(i, mainEnchant, level, cPrice, oPrice, trade.isDisabled()));
                     this.info(
                        "Send:" + mainEnchant.getValue().getPath() + " Lv." + level + "| Original Price:" + oPrice + "Current Price:" + cPrice,
                        new Object[0]
                     );
                  }
               }
            }

            this.currentTarget.setDiscovered(true);
            this.setDelayCloseScreenAndNext(LibrarianStep.INIT_SCAN);
         }
      } else {
         this.currentTarget.setDiscovered(true);
         this.step = LibrarianStep.INIT_SCAN;
      }
   }

   private void checkSupplyAndTarget() {
      if (InvUtils.find(item -> item.toString().contains("enchanted_book")).found()) {
         this.step = LibrarianStep.GOTO_SORT_AREA;
      } else {
         int emptySlots = 0;

         for (int i = 0; i < 36; i++) {
            if (this.mc.player.getInventory().getStack(i).isEmpty()) {
               emptySlots++;
            }
         }

         if (emptySlots < 2) {
            this.warning("PackSpaceHeavynot, CannotEnter, Logic!", new Object[0]);
            this.wait = true;
            this.step = LibrarianStep.WAIT;
         } else {
            this.currentTarget = null;
            this.currentOffer = null;
            long currentTime = System.currentTimeMillis();
            long cooldownMs = ((Integer)this.checkCooldown.get()).intValue() * 1000L;

            label101:
            for (LibrarianWarp warp : this.librarianList) {
               if (warp.getTradeTimes() < ((Integer)this.maxExhaustions.get()).intValue() && currentTime - warp.getLastTradeTime() >= cooldownMs) {
                  Iterator hasBooks = warp.getOffers().iterator();

                  while (true) {
                     if (hasBooks.hasNext()) {
                        LibrarianWarp.LibrarianOffer offer = (LibrarianWarp.LibrarianOffer)hasBooks.next();
                        if (offer.isOutOfStock()) {
                           continue;
                        }

                        int currentPrice = offer.getEmeraldPrice();
                        int originalPrice = offer.getOriginalPrice();
                        if (this.mode.get() == VillagerMode.OnlyOneMoney && currentPrice > 1
                           || this.mode.get() == VillagerMode.OnlyNoPremium && currentPrice > originalPrice
                           || currentPrice > (Integer)this.maxPrice.get()) {
                           continue;
                        }

                        boolean isWanted = false;

                        for (RegistryKey<Enchantment> targetKey : (Set)this.targets.get()) {
                           if (targetKey.equals(offer.getEnchantment())) {
                              isWanted = true;
                              break;
                           }
                        }

                        if (!isWanted) {
                           continue;
                        }

                        this.currentTarget = warp;
                        this.currentOffer = offer;
                     }

                     if (this.currentTarget != null) {
                        break label101;
                     }
                     break;
                  }
               }
            }

            if (this.currentTarget == null) {
               if (!this.wait) {
                  long time = this.mc.world.getTimeOfDay() % 24000L;
                  if (time >= ((Integer)this.workEndTick.get()).intValue()) {
                     this.info("VillagerAlreadydownhaveTargetOut of Stock, EnterMechanismMode...", new Object[0]);
                  } else {
                     this.info("currentno'sExchangeTarget(CancanallatCooldown), GroundDoWait...", new Object[0]);
                  }

                  this.wait = true;
               }

               this.step = LibrarianStep.WAIT;
            } else {
               this.wait = false;
               int maxTradeUses = Math.min(12, emptySlots - 1);
               this.requiredBooks = maxTradeUses;
               this.requiredEmeralds = maxTradeUses * this.currentOffer.getEmeraldPrice();
               int hasEmeralds = InvUtils.find(new Item[]{Items.EMERALD}).count();
               int hasBooks = InvUtils.find(new Item[]{Items.BOOK}).count();
               if (hasEmeralds < this.requiredEmeralds) {
                  this.step = LibrarianStep.GOTO_EMERALD;
               } else if (hasBooks < this.requiredBooks) {
                  this.step = LibrarianStep.GOTO_BASE_BOOK;
               } else {
                  this.step = LibrarianStep.NEXT_LIBRARIAN;
               }
            }
         }
      }
   }

   private void takeItems(BlockPos containerPos, Item targetItem, int amountNeeded, LibrarianStep nextStep) {
      ScreenHandler handler = this.mc.player.currentScreenHandler;
      if (handler instanceof PlayerScreenHandler) {
         HeBlockUtils.open(containerPos);
         this.setDelay((Integer)this.windowDelay.get());
      } else {
         if (handler instanceof GenericContainerScreenHandler chestHandler) {
            int currentCount = InvUtils.find(new Item[]{targetItem}).count();
            if (currentCount >= amountNeeded) {
               this.setDelayCloseScreenAndNext(nextStep);
               return;
            }

            for (int i = 0; i < chestHandler.getInventory().size(); i++) {
               if (chestHandler.getSlot(i).getStack().getItem() == targetItem) {
                  InvUtils.shiftClick().slotId(i);
                  this.setDelay((Integer)this.clickDelay.get());
                  return;
               }
            }

            this.warning("TolerateDeviceinside" + targetItem.getName().getString() + "not! needPlayerPre.", new Object[0]);
            this.toggle();
         } else {
            HeInvUtils.closeCurScreen();
            this.setDelay((Integer)this.windowDelay.get());
         }
      }
   }

   private void nextLibrarian() {
      if (this.currentTarget != null) {
         this.gotoIfNeed(this.currentTarget.getOperatePos(), "beforeVillagerBiliBit", LibrarianStep.OPEN_TRADE);
      } else {
         this.step = LibrarianStep.CHECK_SUPPLY;
      }
   }

   private void openTradeGUI() {
      VillagerEntity villager = this.currentTarget.getVillager();
      if (villager != null && villager.isAlive()) {
         this.openEntityGUI(villager, LibrarianStep.EXECUTE_TRADE);
      } else {
         this.currentTarget.setTradeTimes(((Integer)this.maxExhaustions.get()).intValue());
         this.step = LibrarianStep.CHECK_SUPPLY;
      }
   }

   private void executeTrade() {
      if (!(this.mc.currentScreen instanceof MerchantScreen)) {
         this.setDelay((Integer)this.windowDelay.get());
         this.step = LibrarianStep.OPEN_TRADE;
      } else {
         MerchantScreenHandler handler = (MerchantScreenHandler)this.mc.player.currentScreenHandler;
         TradeOfferList tradeOffers = handler.getRecipes();
         if (this.currentOffer.getTradeIndex() >= tradeOffers.size()) {
            this.setDelayCloseScreenAndNext(LibrarianStep.CHECK_SUPPLY);
         } else {
            TradeOffer actualTrade = (TradeOffer)tradeOffers.get(this.currentOffer.getTradeIndex());
            ItemStack sellItem = actualTrade.getSellItem();
            if (sellItem.getItem().toString().contains("enchanted_book")) {
               RegistryKey<Enchantment> actualEnchant = this.sortConfig.getMainEnchantment(sellItem);
               if (actualEnchant == null || !actualEnchant.equals(this.currentOffer.getEnchantment())) {
                  this.warning("⚠️ Warning: VillagerMemoryandActualOut'snot! (CancanVillagerbyRise). atResetVillagerMemory...", new Object[0]);
                  this.currentTarget.setDiscovered(false);
                  this.currentTarget.clearOffers();
                  this.setDelayCloseScreenAndNext(LibrarianStep.INIT_SCAN);
                  return;
               }
            }

            if (!actualTrade.isDisabled() && actualTrade.getUses() < actualTrade.getMaxUses()) {
               int emptySlots = 0;

               for (int i = 0; i < 36; i++) {
                  if (this.mc.player.getInventory().getStack(i).isEmpty()) {
                     emptySlots++;
                  }
               }

               if (emptySlots == 0) {
                  this.info("PackAlready, StopStopEnter, Preparego.", new Object[0]);
                  this.setDelayCloseScreenAndNext(LibrarianStep.GOTO_SORT_AREA);
               } else {
                  int emeralds = InvUtils.find(new Item[]{Items.EMERALD}).count();
                  if (handler.getSlot(0).getStack().isOf(Items.EMERALD)) {
                     emeralds += handler.getSlot(0).getStack().getCount();
                  }

                  if (handler.getSlot(1).getStack().isOf(Items.EMERALD)) {
                     emeralds += handler.getSlot(1).getStack().getCount();
                  }

                  int books = InvUtils.find(new Item[]{Items.BOOK}).count();
                  if (handler.getSlot(0).getStack().isOf(Items.BOOK)) {
                     books += handler.getSlot(0).getStack().getCount();
                  }

                  if (handler.getSlot(1).getStack().isOf(Items.BOOK)) {
                     books += handler.getSlot(1).getStack().getCount();
                  }

                  if (emeralds >= this.currentOffer.getEmeraldPrice() && books >= 1) {
                     this.tradedThisSession = true;
                     handler.setRecipeIndex(this.currentOffer.getTradeIndex());
                     this.mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(this.currentOffer.getTradeIndex()));
                     InvUtils.shiftClick().slotId(2);
                     this.setDelay((Integer)this.clickDelay.get());
                  } else {
                     this.info("Bodyupnot(Green), DisableFacegogive.", new Object[0]);
                     this.setDelayCloseScreenAndNext(LibrarianStep.CHECK_SUPPLY);
                  }
               }
            } else {
               long currentExhaustions = this.currentTarget.getTradeTimes();
               if (this.tradedThisSession) {
                  this.currentTarget.setTradeTimes(currentExhaustions + 1L);
                  this.info(", f***ExchangeTimesNumber:" + (currentExhaustions + 1L) + "/" + this.maxExhaustions.get(), new Object[0]);
               } else {
                  long time = this.mc.world.getTimeOfDay() % 24000L;
                  if (time >= ((Integer)this.workEndTick.get()).intValue()) {
                     this.currentTarget.setTradeTimes(((Integer)this.maxExhaustions.get()).intValue());
                  }

                  this.info("TimeOut of Stock, EnterCooldown...", new Object[0]);
               }

               this.currentOffer.setOutOfStock(true);
               this.currentTarget.setLastTradeTime(System.currentTimeMillis());
               this.setDelayCloseScreenAndNext(LibrarianStep.CHECK_SUPPLY);
            }
         }
      }
   }

   private void sortBooksLogic() {
      ScreenHandler handler = this.mc.player.currentScreenHandler;
      if (!(handler instanceof PlayerScreenHandler)) {
         ItemStack itemInSlot = this.mc.player.getInventory().getStack(this.currentSortItemSlot);
         if (!itemInSlot.isEmpty() && itemInSlot.getItem().toString().contains("enchanted_book")) {
            InvUtils.shiftClick().slot(this.currentSortItemSlot);
            this.setDelay((Integer)this.clickDelay.get());
            ItemStack afterClick = this.mc.player.getInventory().getStack(this.currentSortItemSlot);
            if (!afterClick.isEmpty() && afterClick.getCount() == itemInSlot.getCount()) {
               this.warning("ColorAlready! TriggerAuto...", new Object[0]);
               this.setDelayCloseScreenAndNext(LibrarianStep.HANDLE_FULL_BOX);
            }
         } else {
            this.setDelayCloseScreenAndNext(LibrarianStep.SORT_BOOKS);
         }
      } else {
         FindItemResult book = InvUtils.find(item -> item.toString().contains("enchanted_book"));
         if (!book.found()) {
            this.info("haveCategoryComplete!", new Object[0]);
            this.step = LibrarianStep.CHECK_SUPPLY;
         } else {
            this.currentSortItemSlot = book.slot();
            Block targetColor = this.sortConfig.getTargetBoxType(this.mc.player.getInventory().getStack(book.slot()));
            BlockPos nearestBoxPos = null;
            double minDistance = Double.MAX_VALUE;
            Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());

            for (BlockEntity be : Utils.blockEntities()) {
               if (this.mc.world.getBlockState(be.getPos()).getBlock() == targetColor) {
                  double dist = be.getPos().toCenterPos().squaredDistanceTo(playerPos);
                  if (dist < minDistance) {
                     minDistance = dist;
                     nearestBoxPos = be.getPos();
                  }
               }
            }

            if (nearestBoxPos != null) {
               this.currentSortBoxPos = nearestBoxPos;
               this.gotoIfNeed(this.currentSortBoxPos, "beforemost'sCategory:" + targetColor.getName().getString(), LibrarianStep.SORT_BOOKS);
               if (this.step == LibrarianStep.SORT_BOOKS) {
                  HeBlockUtils.open(this.currentSortBoxPos);
                  this.setDelay((Integer)this.windowDelay.get());
               }
            } else {
               this.warning("nottoConnectDirection (" + targetColor.getName().getString() + ")! Check.", new Object[0]);
               this.toggle();
            }
         }
      }
   }

   private void prepareShulkerReplacement() {
      if (this.dumpPos != null && this.emptyBoxPos != null) {
         BlockState boxState = this.mc.world.getBlockState(this.currentSortBoxPos);
         Item boxItem = boxState.getBlock().asItem();
         this.shulkerManager.initReplacement(this.currentSortBoxPos, boxItem);
         this.step = LibrarianStep.BREAK_FULL_BOX;
      } else {
         this.warning("SettingAirgive, CannotAuto!", new Object[0]);
         this.toggle();
      }
   }

   private void gotoIfNeed(BlockPos targetPos, String msg, LibrarianStep nextStep) {
      Vec3d currentPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      double dist = currentPos.distanceTo(targetPos.toCenterPos());
      if (dist > ((Integer)this.minDistance.get()).intValue() + 1.5) {
         this.info(msg, new Object[0]);
         this.nextStep = nextStep;
         this.gotoTarget(targetPos);
      } else {
         this.step = nextStep;
      }
   }

   private void gotoTarget(BlockPos targetPos) {
      this.customGoalProcess.setGoalAndPath(new GoalNear(targetPos, (Integer)this.minDistance.get()));
      this.step = LibrarianStep.WALKING_TO_VILLAGER;
   }

   public void onPathEvent(PathEvent event) {
      if ((event == PathEvent.CANCELED || event == PathEvent.AT_GOAL) && this.nextStep != null) {
         this.step = this.nextStep;
         this.nextStep = null;
      }
   }

   private void openEntityGUI(Entity entity, LibrarianStep nextStep) {
      Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
      Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      double distance = entityPos.distanceTo(playerPos);
      if (distance > ((Integer)this.minDistance.get()).intValue() + 1.5) {
         this.gotoIfNeed(entity.getBlockPos(), "Villager", nextStep);
      } else {
         EntityHitResult hit = ProjectileUtil.raycast(
            this.mc.player, playerPos, entityPos, entity.getBoundingBox(), Entity::canHit, distance * distance
         );
         HeRotationUtils.rotate(entity.getEyePos());
         if (hit == null) {
            this.mc.interactionManager.interactEntity(this.mc.player, entity, Hand.MAIN_HAND);
         } else {
            ActionResult res = this.mc.interactionManager.interactEntityAtLocation(this.mc.player, entity, hit, Hand.MAIN_HAND);
            if (!res.isAccepted()) {
               this.mc.interactionManager.interactEntity(this.mc.player, entity, Hand.MAIN_HAND);
            }
         }

         this.tradedThisSession = false;
         this.setDelay((Integer)this.windowDelay.get());
         this.step = nextStep;
      }
   }

   private void setDelayCloseScreenAndNext(LibrarianStep closeScreenNextStep) {
      this.setDelay((Integer)this.windowDelay.get());
      this.step = LibrarianStep.CLOSE_SCREEN_AND_NEXT;
      this.closeScreenNextStep = closeScreenNextStep;
   }

   private void closeScreenAndNext() {
      if (this.mc.currentScreen instanceof HandledScreen) {
         HeInvUtils.closeCurScreen();
      }

      this.step = this.closeScreenNextStep;
   }

   private BlockPos getOperatePos(VillagerEntity villager) {
      Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

      for (Direction dir : dirs) {
         BlockPos blockPos = villager.getBlockPos().offset(dir);
         BlockState blockState = this.mc.world.getBlockState(blockPos);
         if (blockState.isOf(Blocks.LECTERN)) {
            BlockPos pos = blockPos.offset(dir);
            if (this.mc.world.getBlockState(pos).isAir() || this.mc.world.getBlockState(pos).isReplaceable()) {
               return pos;
            }
         }
      }

      return null;
   }

   private void scanContainers() {
      this.emeraldPos = this.bookPos = this.dumpPos = this.emptyBoxPos = null;
      Vec3d pPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());

      for (BlockEntity be : Utils.blockEntities()) {
         if (!(pPos.distanceTo(be.getPos().toCenterPos()) > ((Integer)this.searchRange.get()).intValue())) {
            BlockEntityType<?> type = be.getType();
            if (((List)this.emeraldStorage.get()).contains(type)) {
               this.emeraldPos = be.getPos();
            } else if (((List)this.bookStorage.get()).contains(type)) {
               this.bookPos = be.getPos();
            } else if (((List)this.dumpStorage.get()).contains(type)) {
               this.dumpPos = be.getPos();
            } else if (((List)this.emptyBoxStorage.get()).contains(type)) {
               this.emptyBoxPos = be.getPos();
            }
         }
      }
   }

   private void handleWait() {
      long time = this.mc.world.getTimeOfDay() % 24000L;
      if (time < 100L) {
         if (this.wait) {
            this.info("\ud83c\udf1e New'sOneSky, ResethaveVillager'sExchangeandOut of StockStatus!", new Object[0]);

            for (LibrarianWarp warp : this.librarianList) {
               warp.setTradeTimes(0L);
               warp.setLastTradeTime(0L);
               warp.getOffers().forEach(offerx -> offerx.setOutOfStock(false));
            }

            this.wait = false;
            this.step = LibrarianStep.CHECK_SUPPLY;
         }
      } else {
         if (time < ((Integer)this.workEndTick.get()).intValue()) {
            long currentTime = System.currentTimeMillis();
            long cooldownMs = ((Integer)this.checkCooldown.get()).intValue() * 1000L;

            for (LibrarianWarp warp : this.librarianList) {
               if (warp.getTradeTimes() < ((Integer)this.maxExhaustions.get()).intValue() && currentTime - warp.getLastTradeTime() > cooldownMs) {
                  boolean hasWantedOffer = false;

                  for (LibrarianWarp.LibrarianOffer offer : warp.getOffers()) {
                     int currentPrice = offer.getEmeraldPrice();
                     int originalPrice = offer.getOriginalPrice();
                     boolean priceValid = true;
                     if (this.mode.get() == VillagerMode.OnlyOneMoney && currentPrice > 1) {
                        priceValid = false;
                     }

                     if (this.mode.get() == VillagerMode.OnlyNoPremium && currentPrice > originalPrice) {
                        priceValid = false;
                     }

                     if (currentPrice > (Integer)this.maxPrice.get()) {
                        priceValid = false;
                     }

                     boolean isWanted = false;

                     for (RegistryKey<Enchantment> targetKey : (Set)this.targets.get()) {
                        if (targetKey.equals(offer.getEnchantment())) {
                           isWanted = true;
                           break;
                        }
                     }

                     if (priceValid && isWanted) {
                        hasWantedOffer = true;
                        break;
                     }
                  }

                  if (hasWantedOffer) {
                     warp.getOffers().forEach(offerx -> offerx.setOutOfStock(false));
                     this.wait = false;
                     this.step = LibrarianStep.CHECK_SUPPLY;
                     return;
                  }
               }
            }
         }
      }
   }
}
