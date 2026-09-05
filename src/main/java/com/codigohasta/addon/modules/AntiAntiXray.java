package com.codigohasta.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AntiAntiXray extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBaritone = this.settings.createGroup("Baritone Auto");
   private final SettingGroup sgPerformance = this.settings.createGroup("canand");
   private final SettingGroup sgColors = this.settings.createGroup("ThingColor(high)");
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final Setting<AntiAntiXray.TriggerMode> triggerMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("TriggerMode")).description("SelectAutoScanstillKeyManualScan."))
               .defaultValue(AntiAntiXray.TriggerMode.Automatic))
            .build()
      );
   private final Setting<Keybind> forceScanKey = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                        .name("ScanKey"))
                     .description("downKeystartScan."))
                  .defaultValue(Keybind.fromKey(-1)))
               .visible(() -> this.triggerMode.get() == AntiAntiXray.TriggerMode.Keybind))
            .build()
      );
   private final Setting<Integer> radius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Scan Radius"))
                  .description("ScanRangeRadius."))
               .defaultValue(5))
            .min(1)
            .max(7)
            .build()
      );
   private final Setting<AntiAntiXray.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("ExchangeMode")).description("SendPackDirection.")).defaultValue(AntiAntiXray.Mode.PacketMine))
            .build()
      );
   private final Setting<Boolean> autoMine = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("EnableAuto Mine"))
                  .description("ScanOutreallyTime, letBaritoneAutogoMine."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> mineThreshold = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("inMineThreshold"))
                     .description("OnlyhaveSendfewNreallyTime, thenStopStopWaygo."))
                  .defaultValue(1))
               .min(1)
               .sliderMax(10)
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> tunnelMode = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("AirTimeWay"))
                     .description("ifnoSendreally(Amountnot), letBaritoneAutobeforeWay(Mechanism)."))
                  .defaultValue(true))
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Integer> minDurability = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Tool Protection Threshold"))
                     .description("HandToolDurabilitylowTime, ForceStopStopBaritone."))
                  .defaultValue(15))
               .min(0)
               .sliderMax(100)
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> scanWaitMode = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("WaitScanCompleteComplete"))
                     .description(
                        "Enableafter, WayTimewillSetPauseWaitScanCompleteCompletecontinue, ScanBetweendownDirectionSettingSystem. DisableHoldhaveMoveScan'sTimeRowFor."
                     ))
                  .defaultValue(false))
               .visible(() -> (Boolean)this.autoMine.get() && (Boolean)this.tunnelMode.get()))
            .build()
      );
   private final Setting<Integer> tunnelScanInterval = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("WayScanBetween()"))
                     .description("WayTime, manyfewPauseScanThing. ScanBetweenBaritonewillPauseWait."))
                  .defaultValue(5))
               .min(1)
               .max(60)
               .sliderMax(30)
               .visible(() -> (Boolean)this.autoMine.get() && (Boolean)this.tunnelMode.get() && (Boolean)this.scanWaitMode.get()))
            .build()
      );
   private final Setting<Boolean> scanWhileMining = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("TimealsoBetweenScan"))
                     .description("Enableafter, beforeMineThing'spastinalsowillSettingBetweenPauseScan. DisableTimenotTriggerBetweenScan."))
                  .defaultValue(false))
               .visible(() -> (Boolean)this.autoMine.get() && (Boolean)this.scanWaitMode.get()))
            .build()
      );
   private final Setting<Boolean> scanOnMineComplete = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("CompleteCompleteTimeScan"))
                     .description("Enableafter, MineThingCompleteCompleteTimeTickScanOneTimesWaitScanend."))
                  .defaultValue(false))
               .visible(() -> (Boolean)this.autoMine.get() && (Boolean)this.scanWaitMode.get()))
            .build()
      );
   private final Setting<Integer> threadCount = this.sgPerformance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("LineAmount"))
               .defaultValue(2))
            .min(1)
            .max(8)
            .build()
      );
   private final Setting<Integer> blocksPerTick = this.sgPerformance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("LineRate"))
               .defaultValue(1))
            .min(1)
            .max(10)
            .build()
      );
   private final Setting<Integer> delay = this.sgPerformance
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("SendPackDelay(Tick)"))
               .defaultValue(1))
            .min(1)
            .build()
      );
   private final Setting<Boolean> dynamicReset = this.sgPerformance
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("MoveTimeReset"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> diamondColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(0, 255, 255, 200))
            .build()
      );
   private final Setting<SettingColor> debrisColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(160, 32, 240, 220))
            .build()
      );
   private final Setting<SettingColor> goldColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("GoldColor"))
            .defaultValue(new SettingColor(255, 215, 0, 200))
            .build()
      );
   private final Setting<SettingColor> ironColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(210, 180, 140, 200))
            .build()
      );
   private final Setting<SettingColor> emeraldColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("GreenColor"))
            .defaultValue(new SettingColor(0, 255, 0, 200))
            .build()
      );
   private final Setting<SettingColor> lapisColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("CyanGoldColor"))
            .defaultValue(new SettingColor(0, 0, 170, 220))
            .build()
      );
   private final Setting<SettingColor> redstoneColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("RedColor"))
            .defaultValue(new SettingColor(255, 0, 0, 200))
            .build()
      );
   private final Setting<SettingColor> coalColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(20, 20, 20, 200))
            .build()
      );
   private final Setting<SettingColor> copperColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(230, 115, 0, 200))
            .build()
      );
   private final Setting<SettingColor> quartzColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(230, 230, 230, 180))
            .build()
      );
   private final Setting<SettingColor> otherColor = this.sgColors
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("OtherColor"))
            .defaultValue(new SettingColor(255, 255, 255, 200))
            .build()
      );
   private final Setting<List<Block>> targetBlocks = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
               .name("TargetBlockList"))
            .defaultValue(
               new Block[]{
                  Blocks.DIAMOND_ORE,
                  Blocks.DEEPSLATE_DIAMOND_ORE,
                  Blocks.ANCIENT_DEBRIS,
                  Blocks.GOLD_ORE,
                  Blocks.DEEPSLATE_GOLD_ORE,
                  Blocks.NETHER_GOLD_ORE,
                  Blocks.IRON_ORE,
                  Blocks.DEEPSLATE_IRON_ORE,
                  Blocks.COAL_ORE,
                  Blocks.DEEPSLATE_COAL_ORE,
                  Blocks.EMERALD_ORE,
                  Blocks.DEEPSLATE_EMERALD_ORE,
                  Blocks.LAPIS_ORE,
                  Blocks.DEEPSLATE_LAPIS_ORE,
                  Blocks.REDSTONE_ORE,
                  Blocks.DEEPSLATE_REDSTONE_ORE,
                  Blocks.COPPER_ORE,
                  Blocks.DEEPSLATE_COPPER_ORE,
                  Blocks.NETHER_QUARTZ_ORE
               }
            )
            .build()
      );
   private final Setting<Boolean> renderTracers = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("EnableLine"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> showProgress = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("DisplayEnterDegree"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderScan = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("RenderScanMark"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> scanColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("MarkColor"))
               .defaultValue(new SettingColor(255, 255, 255, 100))
               .visible(this.renderScan::get))
            .build()
      );
   private final Queue<BlockPos> queueA = new ConcurrentLinkedQueue<>();
   private final Queue<BlockPos> queueB = new ConcurrentLinkedQueue<>();
   private final Set<BlockPos> scannedPositions = Collections.synchronizedSet(new HashSet<>());
   private final Map<BlockPos, Block> foundOres = new ConcurrentHashMap<>();
   private final Map<BlockPos, Long> activeScanningRender = new ConcurrentHashMap<>();
   private ExecutorService executor;
   private int timer = 0;
   private int baritoneTimer = 0;
   private int totalBlocksInCurrentScan = 0;
   private boolean isScanning = false;
   private boolean lastKeyPressedState = false;
   private BlockPos lastRefillPos = null;
   private int tunnelScanCooldown = 0;
   private boolean waitingForScan = false;
   private boolean wasMining = false;

   public AntiAntiXray() {
      super(
         AddonTemplate.CATEGORY,
         "AntiAntiXray",
         "Bypasses server-side anti-xray by scanning for real ores and mining only genuine blocks. Only useful on servers with an anti-xray plugin."
      );
   }

   public void onActivate() {
      this.clearAll();
      this.executor = Executors.newFixedThreadPool((Integer)this.threadCount.get());
      this.lastKeyPressedState = false;
      this.lastRefillPos = null;
      this.baritoneTimer = 0;
      this.tunnelScanCooldown = (Integer)this.tunnelScanInterval.get() * 4;
      this.waitingForScan = false;
      if (this.triggerMode.get() == AntiAntiXray.TriggerMode.Automatic && (!(Boolean)this.scanWaitMode.get() || !(Boolean)this.autoMine.get())) {
         this.refillQueue();
      }
   }

   public void onDeactivate() {
      this.clearAll();
      if (this.executor != null) {
         this.executor.shutdownNow();
         this.executor = null;
      }

      if (BaritoneUtils.IS_AVAILABLE && BaritoneAPI.getProvider().getPrimaryBaritone() != null) {
         BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
      }
   }

   private void clearAll() {
      this.queueA.clear();
      this.queueB.clear();
      this.scannedPositions.clear();
      this.foundOres.clear();
      this.activeScanningRender.clear();
      this.totalBlocksInCurrentScan = 0;
      this.isScanning = false;
      this.timer = 0;
      this.baritoneTimer = 0;
      this.lastRefillPos = null;
      this.tunnelScanCooldown = 0;
      this.waitingForScan = false;
      this.wasMining = false;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.world != null && this.mc.player != null) {
         BlockPos playerPos = this.mc.player.getBlockPos();
         if (this.mc.player.age % 5 == 0 && !this.foundOres.isEmpty()) {
            this.foundOres.entrySet().removeIf(entry -> {
               BlockPos pos = entry.getKey();
               Block storedBlock = entry.getValue();
               Block currentBlock = this.mc.world.getBlockState(pos).getBlock();
               return currentBlock != storedBlock;
            });
         }

         if (this.triggerMode.get() == AntiAntiXray.TriggerMode.Automatic) {
            boolean suppressAutoScan = this.waitingForScan || (Boolean)this.scanWaitMode.get() && (Boolean)this.autoMine.get();
            if (!suppressAutoScan) {
               boolean movedSignificantly = this.lastRefillPos == null || this.lastRefillPos.getSquaredDistance(playerPos) > 2.25;
               if (movedSignificantly) {
                  if ((Boolean)this.dynamicReset.get()) {
                     this.queueA.clear();
                     this.queueB.clear();
                  }

                  this.lastRefillPos = playerPos;
                  this.refillQueue();
               } else if (this.isQueuesEmpty() && this.mc.player.age % 10 == 0) {
                  if (this.isScanning) {
                     this.finishScan();
                  }

                  this.refillQueue();
               }
            } else {
               this.lastRefillPos = playerPos;
               if (this.isQueuesEmpty() && this.isScanning) {
                  this.finishScan();
               }
            }
         } else {
            boolean isPressed = ((Keybind)this.forceScanKey.get()).isPressed();
            if (isPressed && !this.lastKeyPressedState) {
               if (!this.isQueuesEmpty()) {
                  this.mc.inGameHud.setOverlayMessage(Text.of("§cScanatProceedin..."), false);
               } else {
                  this.mc.inGameHud.setOverlayMessage(Text.of("§a[Manual] startScan..."), false);
                  this.queueA.clear();
                  this.queueB.clear();
                  int clearRadius = (Integer)this.radius.get() + 5;
                  synchronized (this.scannedPositions) {
                     this.scannedPositions.removeIf(p -> p.isWithinDistance(playerPos, clearRadius));
                  }

                  if (this.executor == null || this.executor.isShutdown()) {
                     this.executor = Executors.newFixedThreadPool((Integer)this.threadCount.get());
                  }

                  this.refillQueue();
               }
            }

            this.lastKeyPressedState = isPressed;
            if (this.isScanning && this.isQueuesEmpty()) {
               this.finishScan();
            }
         }

         if (this.isScanning) {
            this.updateProgress();
         }

         if ((Boolean)this.autoMine.get() && BaritoneUtils.IS_AVAILABLE && this.checkToolHealth()) {
            if (this.baritoneTimer > 0) {
               this.baritoneTimer--;
            } else {
               this.baritoneTimer = 5;
               if (this.waitingForScan) {
                  this.handleScanWait(playerPos);
               } else {
                  this.handleBaritoneLogic(playerPos);
               }
            }
         }

         if (this.mc.player.age % 20 == 0) {
            int cleanupThreshold = (Integer)this.radius.get() + 8;
            synchronized (this.scannedPositions) {
               this.scannedPositions.removeIf(pos -> !pos.isWithinDistance(playerPos, cleanupThreshold));
            }

            this.foundOres.keySet().removeIf(pos -> !pos.isWithinDistance(playerPos, 100.0));
         }

         if (!this.isQueuesEmpty()) {
            if (this.timer < (Integer)this.delay.get()) {
               this.timer++;
            } else {
               this.timer = 0;
               int count = (Integer)this.blocksPerTick.get();
               this.submitTasks(this.queueA, count);
               this.submitTasks(this.queueB, count);
            }
         }
      }
   }

   private boolean checkToolHealth() {
      if (!this.isPathing()) {
         return true;
      } else {
         ItemStack stack = this.mc.player.getMainHandStack();
         if (!stack.isEmpty() && stack.isDamageable()) {
            int remainingDurability = stack.getMaxDamage() - stack.getDamage();
            if (remainingDurability <= (Integer)this.minDurability.get()) {
               BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
               this.mc
                  .inGameHud
                  .setOverlayMessage(Text.of("§c[Warning] ToolDurabilitypastlow (" + remainingDurability + "), AlreadyStopStopAuto Mine!"), true);
               return false;
            } else {
               return true;
            }
         } else {
            return true;
         }
      }
   }

   private void triggerScan(BlockPos playerPos, String message) {
      this.tunnelScanCooldown = -1;
      BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
      int clearRadius = (Integer)this.radius.get();
      synchronized (this.scannedPositions) {
         this.scannedPositions.removeIf(p -> p.isWithinDistance(playerPos, clearRadius));
      }

      this.queueA.clear();
      this.queueB.clear();
      this.refillQueue();
      this.waitingForScan = true;
      this.mc.inGameHud.setOverlayMessage(Text.of(message), false);
   }

   private void handleBaritoneLogic(BlockPos playerPos) {
      int threshold = (Integer)this.mineThreshold.get();
      int oreCount = this.foundOres.size();
      if (this.wasMining && oreCount < threshold && (Boolean)this.scanOnMineComplete.get() && !this.waitingForScan) {
         this.wasMining = false;
         this.triggerScan(playerPos, "§e[CompleteComplete] ThingAlreadyMine, Scan...");
      } else {
         this.wasMining = oreCount >= threshold;
         if (oreCount >= threshold) {
            BlockPos closest = this.foundOres.keySet().stream().min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(playerPos))).orElse(null);
            if ((Boolean)this.scanWaitMode.get() && (Boolean)this.scanWhileMining.get()) {
               if (this.tunnelScanCooldown > 0) {
                  this.tunnelScanCooldown--;
                  if (closest != null && !this.isMiningTarget(closest)) {
                     BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(closest));
                  }
               }

               if (this.tunnelScanCooldown <= 0 && !this.waitingForScan) {
                  this.triggerScan(playerPos, "§e[Scan] beforeThingin, PauseScan...");
               }
            } else if (closest != null && !this.isMiningTarget(closest)) {
               BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(closest));
            }
         } else if ((Boolean)this.tunnelMode.get()) {
            if ((Boolean)this.scanWaitMode.get()) {
               if (this.tunnelScanCooldown > 0) {
                  this.tunnelScanCooldown--;
                  if (!this.isPathing()) {
                     BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("tunnel");
                  }
               }

               if (this.tunnelScanCooldown <= 0 && !this.waitingForScan) {
                  this.triggerScan(playerPos, "§e[WayScan] PauseMine, ScanThingin...");
               }
            } else if (!this.isPathing()) {
               BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("tunnel");
            }
         }
      }
   }

   private void handleScanWait(BlockPos playerPos) {
      if (this.isQueuesEmpty()) {
         if (this.isScanning) {
            this.finishScan();
         }

         int oreCount = this.foundOres.size();
         int threshold = (Integer)this.mineThreshold.get();
         if (oreCount >= threshold) {
            BlockPos closest = this.foundOres.keySet().stream().min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(playerPos))).orElse(null);
            if (closest != null) {
               BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(closest));
               this.mc.inGameHud.setOverlayMessage(Text.of("§a[WayScan] Send" + oreCount + "Thing, beforeMine!"), false);
            }
         } else if ((Boolean)this.tunnelMode.get()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("tunnel");
            this.mc.inGameHud.setOverlayMessage(Text.of("§7[WayScan] SendThing, continueMineWay..."), false);
         }

         this.waitingForScan = false;
         this.tunnelScanCooldown = (Integer)this.tunnelScanInterval.get() * 4;
      }
   }

   private boolean isMiningTarget(BlockPos target) {
      if (!this.isPathing()) {
         return false;
      } else {
         Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
         if (goal == null) {
            return false;
         } else {
            return goal instanceof GoalBlock goalBlock ? goalBlock.getGoalPos().equals(target) : false;
         }
      }
   }

   private boolean isPathing() {
      return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
   }

   private void finishScan() {
      this.isScanning = false;
      if ((Boolean)this.showProgress.get()) {
         this.mc.inGameHud.setOverlayMessage(Text.of("§aScanCompleteComplete!"), false);
      }
   }

   private void submitTasks(Queue<BlockPos> queue, int count) {
      for (int i = 0; i < count; i++) {
         BlockPos pos = queue.poll();
         if (pos == null) {
            break;
         }

         this.scannedPositions.add(pos);
         if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.submit(() -> this.processBlock(pos));
         }
      }
   }

   private void processBlock(BlockPos pos) {
      if (this.mc.getNetworkHandler() != null && this.mc.player != null) {
         this.activeScanningRender.put(pos, System.currentTimeMillis());

         try {
            boolean usePacketMine = this.mode.get() == AntiAntiXray.Mode.PacketMine;
            if (!usePacketMine) {
               Item mainItem = this.mc.player.getMainHandStack().getItem();
               if (mainItem instanceof BlockItem || mainItem instanceof FireworkRocketItem) {
                  usePacketMine = true;
               }
            }

            if (usePacketMine) {
               this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, Direction.UP));
               this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
            } else {
               BlockHitResult hitResult = new BlockHitResult(
                  new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false
               );
               this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
            }
         } catch (Exception var4) {
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (event.packet instanceof BlockUpdateS2CPacket packet) {
         BlockPos pos = packet.getPos();
         Block block = packet.getState().getBlock();
         if (((List)this.targetBlocks.get()).contains(block)) {
            this.foundOres.put(pos, block);
         } else {
            this.foundOres.remove(pos);
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if ((Boolean)this.renderScan.get()) {
         long now = System.currentTimeMillis();
         this.activeScanningRender.forEach((posx, time) -> {
            if (now - time > 150L) {
               this.activeScanningRender.remove(posx);
            } else {
               event.renderer.box(posx, (Color)this.scanColor.get(), (Color)this.scanColor.get(), ShapeMode.Lines, 0);
            }
         });
      }

      for (Entry<BlockPos, Block> entry : this.foundOres.entrySet()) {
         BlockPos pos = entry.getKey();
         Block block = entry.getValue();
         SettingColor color = this.getColorForBlock(block);
         event.renderer.box(pos, color, color, ShapeMode.Lines, 0);
         if ((Boolean)this.renderTracers.get()) {
            event.renderer
               .line(
                  RenderUtils.center.x,
                  RenderUtils.center.y,
                  RenderUtils.center.z,
                  pos.getX() + 0.5,
                  pos.getY() + 0.5,
                  pos.getZ() + 0.5,
                  color
               );
         }
      }
   }

   private void updateProgress() {
      if ((Boolean)this.showProgress.get() && this.totalBlocksInCurrentScan != 0) {
         int remaining = this.queueA.size() + this.queueB.size();
         int completed = this.totalBlocksInCurrentScan - remaining;
         float percent = (float)completed / this.totalBlocksInCurrentScan * 100.0F;
         StringBuilder bar = new StringBuilder("[");
         int barLength = 10;
         int filled = (int)(percent / 100.0F * barLength);

         for (int i = 0; i < barLength; i++) {
            if (i < filled) {
               bar.append("§a█");
            } else {
               bar.append("§7-");
            }
         }

         bar.append("§r]");
         String msg = String.format("ScanEnterDegree: %s §e%.2f%% §7(Remaining: %d)", bar, percent, remaining);
         this.mc.inGameHud.setOverlayMessage(Text.of(msg), false);
      }
   }

   private boolean isQueuesEmpty() {
      return this.queueA.isEmpty() && this.queueB.isEmpty();
   }

   private void refillQueue() {
      if (this.mc.player != null) {
         int r = (Integer)this.radius.get();
         BlockPos pPos = this.mc.player.getBlockPos();
         List<BlockPos> allCandidates = new ArrayList<>();

         for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
               for (int z = -r; z <= r; z++) {
                  BlockPos target = pPos.add(x, y, z);
                  if (target.isWithinDistance(pPos, r) && !this.mc.world.getBlockState(target).isAir()) {
                     synchronized (this.scannedPositions) {
                        if (this.scannedPositions.contains(target)) {
                           continue;
                        }
                     }

                     if (!this.foundOres.containsKey(target) && !this.queueA.contains(target) && !this.queueB.contains(target)) {
                        allCandidates.add(target);
                     }
                  }
               }
            }
         }

         if (!allCandidates.isEmpty()) {
            allCandidates.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(pPos)));

            for (int i = 0; i < allCandidates.size(); i++) {
               if (i % 2 == 0) {
                  this.queueA.add(allCandidates.get(i));
               } else {
                  this.queueB.add(allCandidates.get(i));
               }
            }

            this.totalBlocksInCurrentScan = this.queueA.size() + this.queueB.size();
            if (this.totalBlocksInCurrentScan > 0) {
               this.isScanning = true;
            }
         }
      }
   }

   private SettingColor getColorForBlock(Block block) {
      if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
         return (SettingColor)this.diamondColor.get();
      } else if (block == Blocks.ANCIENT_DEBRIS) {
         return (SettingColor)this.debrisColor.get();
      } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) {
         return (SettingColor)this.goldColor.get();
      } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
         return (SettingColor)this.ironColor.get();
      } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
         return (SettingColor)this.emeraldColor.get();
      } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
         return (SettingColor)this.lapisColor.get();
      } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
         return (SettingColor)this.redstoneColor.get();
      } else if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
         return (SettingColor)this.coalColor.get();
      } else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
         return (SettingColor)this.copperColor.get();
      } else {
         return block == Blocks.NETHER_QUARTZ_ORE ? (SettingColor)this.quartzColor.get() : (SettingColor)this.otherColor.get();
      }
   }

   public String getInfoString() {
      if (this.isScanning) {
         return "Scanin";
      } else {
         return this.triggerMode.get() == AntiAntiXray.TriggerMode.Automatic ? "Auto" : "Key";
      }
   }

   public static enum Mode {
      PacketMine,
      RightClick;
   }

   public static enum TriggerMode {
      Automatic,
      Keybind;
   }
}
