package com.codigohasta.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.ICustomGoalProcess;
import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.modules.villager.VillagerEntityWarp;
import com.codigohasta.addon.modules.villager.VillagerMode;
import com.codigohasta.addon.modules.villager.VillagerStep;
import com.codigohasta.addon.modules.villager.VillagerType;
import com.codigohasta.addon.utils.heutil.HeBlockUtils;
import com.codigohasta.addon.utils.heutil.HeInvUtils;
import com.codigohasta.addon.utils.heutil.HeRotationUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerTrader extends Module implements AbstractGameEventListener {
   private static final Logger log = LoggerFactory.getLogger(VillagerTrader.class);
   private static final int INIT_TICK = 100;
   private static final Item[] EMPTY_ITEM_ARR = new Item[0];
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> debug = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Mode")).defaultValue(false)).build());
   private final Setting<List<BlockEntityType<?>>> supplyStorage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
                     .name("giveTolerateDevice"))
                  .description("InstallhaveExchangeThing'sTolerateDevice'sType"))
               .defaultValue(new BlockEntityType[]{BlockEntityType.BARREL})
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> supplyQty = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("giveAmount()"))
                  .description("Timesgive'sAmount"))
               .min(1)
               .sliderMax(18)
               .defaultValue(5))
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> putStorage = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
                     .name("TolerateDevice"))
                  .description("UseTolerateDevice'sType"))
               .defaultValue(new BlockEntityType[]{BlockEntityType.CHEST})
               .visible(() -> false))
            .build()
      );
   public final Setting<Integer> supplyRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("TolerateDeviceRange"))
                  .description("EnableFeatureTime, TolerateDevice'sRange"))
               .min(6)
               .sliderMax(20)
               .defaultValue(8))
            .build()
      );
   public final Setting<VillagerMode> mode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Mode"))
                  .description("ExchangeMode"))
               .defaultValue(VillagerMode.OnlyOneMoney))
            .build()
      );
   public final Setting<Integer> checkCooldown = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Out of StockCooldown()"))
                  .description("VillagerOut of Stockafter, WaitmanyTimesCheckhewhether"))
               .min(10)
               .sliderMax(120)
               .defaultValue(30))
            .build()
      );
   public final Setting<Integer> maxExhaustions = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("f***ExchangeupLimit"))
                  .description("OneVillagerSkymostmanybyBuyAirTimes(WikiMechanismFor1Times+2Times=3Times)"))
               .min(1)
               .sliderMax(4)
               .defaultValue(3))
            .build()
      );
   public final Setting<Integer> workEndTick = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("downTime(tick)"))
                     .description("VillagerStopStopDo'sTime(are9000)"))
                  .min(8000)
                  .sliderMax(12000)
                  .defaultValue(9000))
               .visible(() -> false))
            .build()
      );
   public final Setting<Boolean> lateTrade = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("downafterExchange")).description("atVillagerdownafterEnableModuleTime, ForceExchangehavePlayerOne"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Integer> minDistance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("OperationRange"))
                  .description("give, 'sDistance"))
               .min(2)
               .sliderMax(3)
               .defaultValue(2))
            .build()
      );
   public final Setting<Boolean> one = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Exchange1")).description("Exchange1")).defaultValue(true)).build());
   public final Setting<VillagerType> type1 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("Villager Type"))
                     .description("Villager Type"))
                  .defaultValue(VillagerType.Cleric))
               .visible(this.one::get))
            .build()
      );
   public final Setting<List<Item>> buy1 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("Buy"))
                  .description("youneedBuy'sItem"))
               .defaultValue(new Item[]{Items.ROTTEN_FLESH})
               .visible(this.one::get))
            .build()
      );
   public final Setting<List<Item>> sell1 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("Sell"))
                  .description("youneedSell'sItem"))
               .visible(this.one::get))
            .build()
      );
   public final Setting<Boolean> two = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Exchange2")).description("EnableExchange")).defaultValue(false)).build());
   public final Setting<VillagerType> type2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("Villager Type2"))
                     .description("Villager Type, notSupportSelectmany"))
                  .defaultValue(VillagerType.Toolsmith))
               .visible(this.two::get))
            .build()
      );
   public final Setting<List<Item>> buy2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("Buy2"))
                  .description("youneedBuy'sItem"))
               .visible(this.two::get))
            .build()
      );
   public final Setting<List<Item>> sell2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                     .name("Sell2"))
                  .description("youneedBuy'sItem"))
               .defaultValue(new Item[]{Items.DIAMOND_PICKAXE})
               .visible(this.two::get))
            .build()
      );
   public final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("OperationDelaySystem"))
               .defaultValue(10))
            .build()
      );
   public final Setting<Integer> clickDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("pointStrikeDelay(tick)"))
                  .description("atFacein, PutItem'sBetween"))
               .min(0)
               .sliderMax(10)
               .defaultValue(1))
            .build()
      );
   public final Setting<Integer> windowDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("FaceDelay(tick)"))
                  .description("OpenDisableTolerateDeviceTime'sWaitTime"))
               .min(1)
               .sliderMax(20)
               .defaultValue(5))
            .build()
      );
   private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
   private final ICustomGoalProcess customGoalProcess = this.baritone.getCustomGoalProcess();
   private final Settings baritoneSettings = BaritoneAPI.getSettings();
   private BlockPos moneyPos;
   private BlockPos putPos;
   private BlockPos goodsPos;
   private Item goodsItem;
   private boolean needBuy;
   private List<VillagerEntityWarp> villagerList = Collections.emptyList();
   private VillagerEntityWarp currentVillager;
   private VillagerStep step = VillagerStep.None;
   private VillagerStep nextStep;
   private VillagerStep closeScreenNextStep;
   private boolean wait;
   private int tradeIndex = 0;
   private volatile int todayTimes = 0;
   private int timer = 0;
   private boolean tradedThisSession = false;
   private boolean doingLateTrade = false;

   public VillagerTrader() {
      super(
         AddonTemplate.SC_CATEGORY,
         "VillagerTrader",
         "Automates villager trading: opens the target container and repeats trades. (Sell mode only supports one item type.)"
      );
      this.baritone.getGameEventHandler().registerEventListener(this);
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.world != null) {
         List<VillagerEntityWarp> villagerList = this.getVillagerEntity();
         if (villagerList.isEmpty()) {
            this.warning("noVillager", new Object[0]);
            this.toggle();
         } else {
            BlockPos moneyPos = null;
            BlockPos putPos = null;
            BlockPos goodsPos = null;
            double min1 = Double.MAX_VALUE;
            double min2 = Double.MAX_VALUE;
            double min3 = Double.MAX_VALUE;
            Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());

            for (BlockEntity blockEntity : Utils.blockEntities()) {
               BlockEntityType<?> type = blockEntity.getType();
               if (((List)this.supplyStorage.get()).contains(type)) {
                  double distanceTo = playerPos.distanceTo(blockEntity.getPos().toCenterPos());
                  if (distanceTo < ((Integer)this.supplyRange.get()).intValue() && distanceTo < min1) {
                     moneyPos = blockEntity.getPos();
                     min1 = distanceTo;
                  }
               } else if (((List)this.putStorage.get()).contains(type)) {
                  double distanceTo = playerPos.distanceTo(blockEntity.getPos().toCenterPos());
                  if (distanceTo < ((Integer)this.supplyRange.get()).intValue() && distanceTo < min2) {
                     putPos = blockEntity.getPos();
                     min2 = distanceTo;
                  }
               } else if (type == BlockEntityType.SHULKER_BOX) {
                  double distanceTo = playerPos.distanceTo(blockEntity.getPos().toCenterPos());
                  if (distanceTo < ((Integer)this.supplyRange.get()).intValue() && distanceTo < min3) {
                     goodsPos = blockEntity.getPos();
                     min3 = distanceTo;
                  }
               }
            }

            if (moneyPos == null && goodsPos == null) {
               this.warning("notto<giveTolerateDevice-><TolerateDevice->", new Object[0]);
            } else if (goodsPos == null) {
               this.warning("notto<TolerateDevice-big>", new Object[0]);
            } else {
               long time = this.timeOfDay();
               if (time >= ((Integer)this.workEndTick.get()).intValue() && (Boolean)this.lateTrade.get()) {
                  this.info("\ud83c\udf19 ModeMove: VillagerAlreadydown, ForceProceedOneExchange!", new Object[0]);
                  this.doingLateTrade = true;
                  villagerList.forEach(item -> {
                     item.setTradeTimes((Integer)this.maxExhaustions.get() - 1);
                     item.setLastTradeTime(0L);
                  });
               } else {
                  this.info("\ud83c\udf1e AutoExchangeAlreadyMove, atScanVillager...", new Object[0]);
                  this.doingLateTrade = false;
                  villagerList.forEach(item -> {
                     item.setTradeTimes(0L);
                     item.setLastTradeTime(0L);
                  });
               }

               this.todayTimes = 0;
               this.baritoneSettings.allowBreak.value = false;
               this.baritoneSettings.allowPlace.value = false;
               this.goodsItem = null;
               this.needBuy = false;
               if ((Boolean)this.one.get()) {
                  if (!((List)this.sell1.get()).isEmpty()) {
                     this.goodsItem = (Item)((List)this.sell1.get()).get(0);
                  }

                  if (!((List)this.buy1.get()).isEmpty() && !((List)this.buy1.get()).contains(Items.EMERALD)) {
                     this.needBuy = true;
                  }
               }

               if ((Boolean)this.two.get()) {
                  if (this.goodsItem == null && !((List)this.sell2.get()).isEmpty()) {
                     this.goodsItem = (Item)((List)this.sell2.get()).get(0);
                  }

                  if (!((List)this.buy2.get()).isEmpty() && !((List)this.buy2.get()).contains(Items.EMERALD)) {
                     this.needBuy = true;
                  }
               }

               this.tradeIndex = 0;
               this.wait = false;
               this.moneyPos = moneyPos;
               this.putPos = putPos;
               this.goodsPos = goodsPos;
               this.villagerList = villagerList;
               this.step = VillagerStep.Wait;
            }
         }
      } else {
         this.toggle();
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.checkAndDecrement()) {
         switch (this.step) {
            case GoToPut:
               this.gotoPut();
               break;
            case Put:
               this.put();
               break;
            case GotoGoods:
               this.gotoGoodsIfNotFull();
               break;
            case TakeGoods:
               this.takeGoods();
               break;
            case GotoMoney:
               this.gotoMoneyIfNotFull();
               break;
            case TakeMoney:
               this.takeMoney();
               break;
            case NextVillager:
               this.nextVillager();
               break;
            case Walking:
               this.none();
               break;
            case OpenTrade:
               this.openTrade();
               break;
            case ExecuteTrade:
               this.executeTrade();
               break;
            case CloseScreenAndNext:
               this.closeScreenAndNext();
               break;
            case Wait:
               this.waitTrade();
               break;
            default:
               this.disableAuto();
               this.toggle();
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

   private void setDelay() {
      this.timer = (Integer)this.delay.get();
   }

   private void waitTrade() {
      long time = this.timeOfDay();
      if (time < 100L) {
         if (this.wait || this.doingLateTrade) {
            this.info("\ud83c\udf1e New'sOneSkystart, ResethaveVillager'sExchangeStatus!", new Object[0]);
            this.doingLateTrade = false;
            this.villagerList.forEach(item -> {
               item.setTradeTimes(0L);
               item.setLastTradeTime(0L);
            });
            this.wait = false;
            this.step = VillagerStep.GoToPut;
         }
      } else {
         long currentTimeMillis = System.currentTimeMillis();
         long cooldownMs = ((Integer)this.checkCooldown.get()).intValue() * 1000L;
         boolean hasReadyVillager = false;

         for (VillagerEntityWarp warp : this.villagerList) {
            if (warp.getTradeTimes() < ((Integer)this.maxExhaustions.get()).intValue() && currentTimeMillis - warp.getLastTradeTime() > cooldownMs) {
               hasReadyVillager = true;
               break;
            }
         }

         if (time >= ((Integer)this.workEndTick.get()).intValue() && !this.doingLateTrade) {
            if (!this.wait) {
               this.info("\ud83c\udf19 VillagerAlreadydown (past" + this.workEndTick.get() + "Tick), Skynot, MechanismWaitSky...", new Object[0]);
               this.wait = true;
            }
         } else if (hasReadyVillager) {
            this.wait = false;
            this.step = VillagerStep.GoToPut;
         } else if (time >= ((Integer)this.workEndTick.get()).intValue() && this.doingLateTrade) {
            this.info("\ud83c\udf19 BetweenExchangeend, PrepareMechanismWaitSky...", new Object[0]);
            this.doingLateTrade = false;
            this.wait = true;
         }
      }
   }

   private void closeScreenAndNext() {
      if (this.mc.currentScreen instanceof HandledScreen) {
         HeInvUtils.closeCurScreen();
      }

      this.step = this.closeScreenNextStep;
   }

   private boolean needClean() {
      if (this.goodsItem != null) {
         FindItemResult findItemResult = InvUtils.find(new Item[]{this.goodsItem});
         if (!findItemResult.found() || findItemResult.count() < 64) {
            return true;
         }
      }

      if (this.needBuy) {
         FindItemResult findItemResult = InvUtils.find(new Item[]{Items.EMERALD});
         if (!findItemResult.found()) {
            return true;
         }

         int qty = this.mode.get() == VillagerMode.OnlyOneMoney ? 12 : 64;
         if (findItemResult.count() < qty) {
            return true;
         }
      }

      PlayerInventory playerInventory = this.mc.player.getInventory();
      int emptyQty = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = playerInventory.getStack(i);
         if (itemStack.isEmpty()) {
            emptyQty++;
         }
      }

      return emptyQty <= 0;
   }

   private void executeTrade() {
      if (!(this.mc.currentScreen instanceof MerchantScreen)) {
         this.setDelay();
         this.step = VillagerStep.OpenTrade;
      } else {
         MerchantScreenHandler handler = (MerchantScreenHandler)this.mc.player.currentScreenHandler;
         TradeOfferList tradeOfferList = handler.getRecipes();
         boolean foundValidTrade = false;

         for (int i = 0; i < tradeOfferList.size(); i++) {
            TradeOffer trade = (TradeOffer)tradeOfferList.get(i);
            if (!trade.isDisabled() && trade.getUses() < trade.getMaxUses()) {
               Item gives = trade.getSellItem().getItem();
               Item wants = trade.getDisplayedFirstBuyItem().getItem();
               boolean isSellingGoods = wants == this.goodsItem && gives == Items.EMERALD;
               boolean isBuyingGoods1 = (Boolean)this.one.get() && ((List)this.buy1.get()).contains(gives) && wants == Items.EMERALD;
               boolean isBuyingGoods2 = (Boolean)this.two.get() && ((List)this.buy2.get()).contains(gives) && wants == Items.EMERALD;
               if (isSellingGoods || isBuyingGoods1 || isBuyingGoods2) {
                  int originCount = trade.getOriginalFirstBuyItem().getCount();
                  int currentPrice = trade.getDisplayedFirstBuyItem().getCount();
                  if ((this.mode.get() != VillagerMode.OnlyOneMoney || currentPrice <= 1)
                     && (this.mode.get() != VillagerMode.OnlyNoPremium || currentPrice <= originCount)) {
                     this.tradeIndex = i;
                     foundValidTrade = true;
                     break;
                  }
               }
            }
         }

         if (!foundValidTrade) {
            long currentExhaustions = this.currentVillager.getTradeTimes();
            if (this.tradedThisSession) {
               this.currentVillager.setTradeTimes(currentExhaustions + 1L);
               this.info("VillagerAlreadybyBuyAir. f***Exchange:" + (currentExhaustions + 1L) + "/" + this.maxExhaustions.get() + "Times", new Object[0]);
            } else if (this.timeOfDay() >= ((Integer)this.workEndTick.get()).intValue()) {
               this.info("VillagerAlreadydownno, not, pasthe...", new Object[0]);
               this.currentVillager.setTradeTimes(((Integer)this.maxExhaustions.get()).intValue());
            } else {
               this.info("Villagerstill, notExchangeTimesNumber, EnterOut of StockCooldown...", new Object[0]);
            }

            this.currentVillager.setLastTradeTime(System.currentTimeMillis());
            this.setDelayCloseScreenAndNext(VillagerStep.NextVillager);
         } else {
            this.tradedThisSession = true;
            handler.setRecipeIndex(this.tradeIndex);
            this.mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(this.tradeIndex));
            InvUtils.shiftClick().slotId(2);
            this.setDelay((Integer)this.clickDelay.get());
         }
      }
   }

   private void openTrade() {
      VillagerEntity villager = this.currentVillager.getVillager();
      if (villager != null && villager.isAlive()) {
         Vec3d villagerPos = new Vec3d(villager.getX(), villager.getY(), villager.getZ());
         Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         double distance = villagerPos.distanceTo(playerPos);
         if (distance > ((Integer)this.minDistance.get()).intValue() + 0.5) {
            this.printLog("distance false");
            this.currentVillager.setLastTradeTime(System.currentTimeMillis() - ((Integer)this.checkCooldown.get()).intValue() * 1000L + 1000L);
            this.nextVillager();
         } else {
            this.printLog("openTrade");
            EntityHitResult entityHitResult = ProjectileUtil.raycast(
               this.mc.player, playerPos, villagerPos, villager.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(villagerPos)
            );
            if (entityHitResult == null) {
               if ((Boolean)this.debug.get()) {
                  this.info("111", new Object[0]);
               }

               HeRotationUtils.rotate(villager.getEyePos());
               this.mc.interactionManager.interactEntity(this.mc.player, villager, Hand.MAIN_HAND);
               this.tradedThisSession = false;
               this.step = VillagerStep.ExecuteTrade;
            } else {
               if ((Boolean)this.debug.get()) {
                  this.info("222", new Object[0]);
               }

               HeRotationUtils.rotate(entityHitResult.getEntity().getEyePos());
               ActionResult actionResult = this.mc.interactionManager.interactEntityAtLocation(this.mc.player, villager, entityHitResult, Hand.MAIN_HAND);
               if (!actionResult.isAccepted()) {
                  this.mc.interactionManager.interactEntity(this.mc.player, villager, Hand.MAIN_HAND);
                  this.tradedThisSession = false;
                  this.step = VillagerStep.ExecuteTrade;
               } else {
                  ChatUtils.error("CannotOpenExchangeFace, Heavyin...", new Object[0]);
               }
            }

            this.setDelay((Integer)this.windowDelay.get());
         }
      } else {
         this.info("VillagerHiccup, RemoveOutf***ExchangeNameSingle", new Object[0]);
         this.currentVillager.setTradeTimes(((Integer)this.maxExhaustions.get()).intValue());
         this.nextVillager();
      }
   }

   public void onPathEvent(PathEvent event) {
      if (event == PathEvent.CANCELED && this.nextStep != null) {
         this.step = this.nextStep;
         this.nextStep = null;
      }
   }

   private void nextVillager() {
      this.printLog("find next villager");
      if (this.needClean()) {
         this.gotoIfNeed(this.putPos, "before", VillagerStep.Put);
      } else {
         VillagerEntityWarp best = null;
         Vec3d pos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         double minDistance = Double.MAX_VALUE;
         long currentTimeMillis = System.currentTimeMillis();
         long cooldownMs = ((Integer)this.checkCooldown.get()).intValue() * 1000L;

         for (VillagerEntityWarp warp : this.villagerList) {
            if (warp.getTradeTimes() < ((Integer)this.maxExhaustions.get()).intValue() && currentTimeMillis - warp.getLastTradeTime() > cooldownMs) {
               double distance = warp.getOperatePosCenter().distanceTo(pos);
               if (distance < minDistance) {
                  best = warp;
                  minDistance = distance;
               }
            }
         }

         if (best == null) {
            long time = this.timeOfDay();
            if (time >= ((Integer)this.workEndTick.get()).intValue()) {
               this.info("haveVillagerAlreadyExchangeCompleteAlreadydown, MechanismSky...", new Object[0]);
            } else {
               this.info("haveVillagerallatOut of StockCooldownin, GroundDoWait...", new Object[0]);
            }

            this.wait = true;
            this.step = VillagerStep.GoToPut;
         } else {
            this.printLog("LockdownOneTarget : {}", best.getOperatePosCenter());
            this.currentVillager = best;
            this.gotoVillager();
         }
      }
   }

   private void takeMoney() {
      ScreenHandler handler = this.mc.player.currentScreenHandler;
      if (handler instanceof PlayerScreenHandler) {
         this.info("OpenGreen...", new Object[0]);
         HeBlockUtils.open(this.moneyPos);
         this.setDelay((Integer)this.windowDelay.get());
      } else {
         if (handler instanceof GenericContainerScreenHandler barrelHandler) {
            FindItemResult emeralds = InvUtils.find(new Item[]{Items.EMERALD});
            if (emeralds.found() && emeralds.count() >= 64 * (Integer)this.supplyQty.get()) {
               this.info("Green, startVillager", new Object[0]);
               HeInvUtils.closeCurScreen();
               this.step = VillagerStep.NextVillager;
               this.setDelay((Integer)this.windowDelay.get());
               return;
            }

            for (int i = 0; i < 27; i++) {
               ItemStack stack = barrelHandler.getSlot(i).getStack();
               if (stack.getItem() == Items.EMERALD && !stack.isEmpty()) {
                  InvUtils.shiftClick().slotId(i);
                  this.setDelay((Integer)this.clickDelay.get());
                  return;
               }
            }

            this.warning("insidenot!", new Object[0]);
            this.toggle();
         } else {
            HeInvUtils.closeCurScreen();
            this.setDelay((Integer)this.windowDelay.get());
         }
      }
   }

   private void gotoMoneyIfNotFull() {
      if (this.needBuy) {
         FindItemResult findItemResult = InvUtils.find(new Item[]{Items.EMERALD});
         if (!findItemResult.found() || findItemResult.count() < 64 * (Integer)this.supplyQty.get()) {
            this.gotoIfNeed(this.moneyPos, "beforeGreen", VillagerStep.TakeMoney);
            return;
         }
      }

      if (this.wait) {
         this.info("giveComplete, GroundMechanismWaitdownOneExchangeTime", new Object[0]);
         this.step = VillagerStep.Wait;
      } else {
         this.step = VillagerStep.NextVillager;
      }
   }

   private void takeGoods() {
      ScreenHandler handler = this.mc.player.currentScreenHandler;
      if (handler instanceof PlayerScreenHandler) {
         this.info("Open...", new Object[0]);
         HeBlockUtils.open(this.goodsPos);
         this.setDelay((Integer)this.windowDelay.get());
      } else {
         if (handler instanceof ShulkerBoxScreenHandler shulkerHandler) {
            FindItemResult currentInv = InvUtils.find(new Item[]{this.goodsItem});
            if (currentInv.found() && currentInv.count() >= 64 * (Integer)this.supplyQty.get()) {
               this.info("give, Disable", new Object[0]);
               HeInvUtils.closeCurScreen();
               this.step = VillagerStep.GotoMoney;
               this.setDelay((Integer)this.windowDelay.get());
               return;
            }

            for (int i = 0; i < 27; i++) {
               ItemStack stack = shulkerHandler.getSlot(i).getStack();
               if (stack.getItem() == this.goodsItem && !stack.isEmpty()) {
                  InvUtils.shiftClick().slotId(i);
                  this.setDelay((Integer)this.clickDelay.get());
                  return;
               }
            }

            this.warning("AlreadyAir!", new Object[0]);
            this.toggle();
         } else {
            HeInvUtils.closeCurScreen();
            this.setDelay((Integer)this.windowDelay.get());
         }
      }
   }

   private void gotoGoodsIfNotFull() {
      if (this.goodsItem != null) {
         FindItemResult findItemResult = InvUtils.find(new Item[]{this.goodsItem});
         if (!findItemResult.found() || findItemResult.count() < 64 * (Integer)this.supplyQty.get()) {
            this.gotoIfNeed(this.goodsPos, "before", VillagerStep.TakeGoods);
            return;
         }
      }

      this.gotoMoneyIfNotFull();
   }

   private void gotoIfNeed(BlockPos targetPos, String msg, VillagerStep nextStep) {
      Vec3d currentPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      double dist = currentPos.distanceTo(targetPos.toCenterPos());
      if ((Boolean)this.debug.get()) {
         this.info("Target Distance:" + dist, new Object[0]);
      }

      if (dist > ((Integer)this.minDistance.get()).intValue() + 1.5) {
         this.info(msg, new Object[0]);
         this.nextStep = nextStep;
         this.gotoTarget(targetPos);
      } else {
         this.step = nextStep;
      }
   }

   private void put() {
      ScreenHandler handler = this.mc.player.currentScreenHandler;
      if (handler instanceof PlayerScreenHandler) {
         this.info("Openbig...", new Object[0]);
         HeBlockUtils.open(this.putPos);
         this.setDelay((Integer)this.windowDelay.get());
      } else {
         if (!(handler instanceof GenericContainerScreenHandler) && !(handler instanceof ShulkerBoxScreenHandler)) {
            HeInvUtils.closeCurScreen();
            this.setDelay((Integer)this.windowDelay.get());
         } else {
            List<Item> toDump = new ArrayList<>();
            toDump.addAll((Collection<? extends Item>)this.buy1.get());
            toDump.addAll((Collection<? extends Item>)this.buy2.get());
            if (!this.needBuy) {
               toDump.add(Items.EMERALD);
            }

            FindItemResult result = InvUtils.find(itemStack -> toDump.contains(itemStack.getItem()));
            if (result.found()) {
               InvUtils.shiftClick().slot(result.slot());
               this.setDelay((Integer)this.clickDelay.get());
            } else {
               this.info("CompleteComplete, PreparegoCheckgive", new Object[0]);
               HeInvUtils.closeCurScreen();
               this.step = VillagerStep.GotoGoods;
               this.setDelay((Integer)this.windowDelay.get());
            }
         }
      }
   }

   private void gotoPut() {
      FindItemResult findItemResult = InvUtils.find(
         item -> ((List)this.buy1.get()).contains(item.getItem()) || ((List)this.buy2.get()).contains(item.getItem())
      );
      if ((!findItemResult.found() || findItemResult.count() <= 0) && !this.wait) {
         this.gotoGoodsIfNotFull();
      } else {
         this.gotoIfNeed(this.putPos, "before", VillagerStep.Put);
      }
   }

   private void gotoVillager() {
      BlockPos targetPos = this.currentVillager.getOperatePos();
      this.printLog("gotoVillager : {}", targetPos);
      this.nextStep = VillagerStep.OpenTrade;
      this.gotoTarget(targetPos, 0);
   }

   private void gotoTarget(BlockPos targetPos) {
      this.gotoTarget(targetPos, (Integer)this.minDistance.get());
   }

   private void gotoTarget(BlockPos targetPos, int distance) {
      this.customGoalProcess.setGoalAndPath(new GoalNear(targetPos, distance));
      this.step = VillagerStep.Walking;
   }

   private void none() {
   }

   private long timeOfDay() {
      return this.mc.world.getTimeOfDay() % 24000L;
   }

   private List<VillagerEntityWarp> getVillagerEntity() {
      List<VillagerEntityWarp> villagerList = new ArrayList<>();

      for (Entity entity : this.mc.world.getEntities()) {
         if (entity instanceof VillagerEntity villager) {
            double y = entity.getY() - this.mc.player.getY();
            if (y >= -2.0 && y <= 2.0) {
               RegistryEntry<VillagerProfession> profession = villager.getVillagerData().profession();
               VillagerType currentType = VillagerType.valueOf(profession);
               if (currentType != null
                  && ((Boolean)this.one.get() && currentType == this.type1.get() || (Boolean)this.two.get() && currentType == this.type2.get())) {
                  BlockPos pos = this.getOperatePos(villager, currentType, Direction.NORTH);
                  if (pos == null) {
                     pos = this.getOperatePos(villager, currentType, Direction.SOUTH);
                  }

                  if (pos == null) {
                     pos = this.getOperatePos(villager, currentType, Direction.WEST);
                  }

                  if (pos == null) {
                     pos = this.getOperatePos(villager, currentType, Direction.EAST);
                  }

                  if (pos != null) {
                     villagerList.add(new VillagerEntityWarp(villager.getUuid(), pos));
                  }
               }
            }
         }
      }

      return villagerList;
   }

   private BlockPos getOperatePos(VillagerEntity villager, VillagerType currentType, Direction direction) {
      BlockPos blockPos = villager.getBlockPos().offset(direction);
      BlockState blockState = this.mc.world.getBlockState(blockPos);
      if (blockState.getBlock().asItem() == currentType.getItem()) {
         BlockPos pos = blockPos.offset(direction);
         if (this.mc.world.getBlockState(pos).isAir()) {
            return pos;
         }
      }

      return null;
   }

   private void setDelayCloseScreenAndNext(VillagerStep closeScreenNextStep) {
      this.setDelay((Integer)this.windowDelay.get());
      this.step = VillagerStep.CloseScreenAndNext;
      this.closeScreenNextStep = closeScreenNextStep;
   }

   private void disableAuto() {
      this.step = VillagerStep.None;
      this.nextStep = null;
      this.baritone.getCommandManager().execute("cancel");
      this.moneyPos = null;
      this.putPos = null;
      this.villagerList = Collections.emptyList();
   }

   public void onDeactivate() {
      this.disableAuto();
   }

   private void printLog(String str, Object arg) {
      if ((Boolean)this.debug.get()) {
         log.info(str, arg);
      }
   }

   private void printLog(String str, Object... args) {
      if ((Boolean)this.debug.get()) {
         log.info(str, args);
      }
   }
}
