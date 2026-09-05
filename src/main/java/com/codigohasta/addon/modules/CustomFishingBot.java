package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class CustomFishingBot extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final char BAR_POINTER = '뀁';
   private static final char BAR_BAR1 = '뀂';
   private static final char BAR_BAR2 = '뀃';
   private static final char BAR_BAR3 = '뀄';
   private static final char BAR_BAR4 = '뀅';
   private static final char BAR_BAR5 = '뀆';
   private static final char BAR_BAR6 = '뀇';
   private static final char BAR_BAR7 = '뀈';
   private static final char BAR_BAR8 = '뀉';
   private static final char BAR_BAR9 = '뀊';
   private static final char BAR_RAINBOW = '뀋';
   private static final char BAR_BAR10 = '뀌';
   private static final char BAR_FISH = '뀍';
   private static final char BAR_STRUGGLE_0 = '뀎';
   private static final char BAR_STRUGGLE_1 = '뀏';
   private static final char BAR_STRUGGLE_2 = '뀐';
   private static final char BAR_BAR11 = '뀑';
   private static final char BAR_JUDGE_EASY = '뀒';
   private static final char BAR_JUDGE_NORMAL = '뀓';
   private static final char BAR_JUDGE_HARD = '뀔';
   private static final Map<Character, Integer> OFFSET_MAP = buildOffsetMap();
   private static final Set<Character> BAR_CHARS = Set.of('뀂', '뀃', '뀄', '뀅', '뀆', '뀇', '뀈', '뀉', '뀊', '뀌', '뀑', '뀋');
   private static final Set<Character> JUDGE_CHARS = Set.of('뀒', '뀓', '뀔');
   private static final Set<Character> FISH_CHARS = Set.of('뀍', '뀎', '뀏', '뀐');
   private static final Set<Character> ALL_CUSTOM_CHARS = new HashSet<>();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> verboseLog = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("verbose-log")).description("Hitf***tochatSky")).defaultValue(true)).build());
   private final Setting<Integer> actionInterval = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("action-interval"))
                  .description("OperationBetween(tick,1tick=50ms)"))
               .defaultValue(1))
            .min(0)
            .max(10)
            .sliderRange(0, 10)
            .build()
      );
   private final Setting<Integer> maxWaitTicks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("max-wait-ticks"))
                  .description("mostlongWaitHookTime(tick, 20tick=1)"))
               .defaultValue(600))
            .min(100)
            .max(3600)
            .sliderRange(100, 1200)
            .build()
      );
   private final Setting<Integer> cooldownTicks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("cooldown-ticks"))
                  .description("Reelafter'sWaitTime(tick)"))
               .defaultValue(20))
            .min(5)
            .max(100)
            .sliderRange(5, 60)
            .build()
      );
   private final Setting<Integer> acClickDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ac-click-delay"))
                  .description("ACCURATE_CLICK pointStrikeDelay(tick), WaitPointtohighRate"))
               .defaultValue(20))
            .min(5)
            .max(80)
            .sliderRange(5, 60)
            .build()
      );
   private CustomFishingBot.FishingState fishingState = CustomFishingBot.FishingState.IDLE;
   private CustomFishingBot.GameType currentGame = CustomFishingBot.GameType.NONE;
   private String currentTitle = "";
   private String currentSubtitle = "";
   private int stateTimer = 0;
   private int castTimer = 0;
   private int waitTimer = 0;
   private int cooldownTimer = 0;
   private int bobberEntityId = -1;
   private boolean bobberGoingDown = false;
   private double bobberLastY = Double.NaN;
   private int bobberDropTicks = 0;
   private static final double BOBBER_DROP_THRESHOLD = 0.05;
   private static final int BOBBER_DROP_MIN_TICKS = 3;
   private boolean bobberInVoid = false;
   private boolean voidBiteDetectedBySound = false;
   private int particleNearBobberCount = 0;
   private int particleCheckTicks = 0;
   private int tickCounter = 0;
   private boolean sneaking = false;
   private int pointerToJudgeGap = 0;
   private int prevGap = 0;
   private int sameDirectionTicks = 0;
   private int fishOffsetFromStart = 0;
   private int phaseTick = 0;
   private int acv2PointerIdx = -1;
   private int acv2TargetStart = -1;
   private int acv2TargetEnd = -1;
   private int acOffsetSum = 0;
   private int acMinOffset = Integer.MAX_VALUE;
   private int acMaxOffset = Integer.MIN_VALUE;
   private int acTargetSection = -1;
   private boolean acCalibrated = false;
   private int acPrevOffsetSum = 0;
   private boolean acOffsetInitialized = false;

   private static Map<Character, Integer> buildOffsetMap() {
      Map<Character, Integer> map = new HashMap<>();
      map.put('\uf801', 1);
      map.put('\uf802', 2);
      map.put('\uf803', 4);
      map.put('\uf804', 8);
      map.put('\uf805', 16);
      map.put('\uf806', 32);
      map.put('\uf807', 64);
      map.put('\uf808', 128);
      map.put('\uf811', -1);
      map.put('\uf812', -2);
      map.put('\uf813', -4);
      map.put('\uf814', -8);
      map.put('\uf815', -16);
      map.put('\uf816', -32);
      map.put('\uf817', -64);
      map.put('\uf818', -128);
      return Map.copyOf(map);
   }

   public CustomFishingBot() {
      super(
         AddonTemplate.SC_CATEGORY, "CustomFishingBot", "Automatic fishing bot built for a specific server plugin. (Incomplete - read-only on other servers.)"
      );
   }

   public void onActivate() {
      this.resetAll();
      if ((Boolean)this.verboseLog.get()) {
         this.info("§bCustomFishingBot v5 Already - AutoMode", new Object[0]);
      }
   }

   public void onDeactivate() {
      this.resetAll();
      this.releaseKeys();
   }

   private void resetAll() {
      this.fishingState = CustomFishingBot.FishingState.IDLE;
      this.currentGame = CustomFishingBot.GameType.NONE;
      this.currentTitle = "";
      this.currentSubtitle = "";
      this.stateTimer = 0;
      this.castTimer = 0;
      this.waitTimer = 0;
      this.cooldownTimer = 0;
      this.bobberEntityId = -1;
      this.bobberGoingDown = false;
      this.bobberLastY = Double.NaN;
      this.bobberDropTicks = 0;
      this.bobberInVoid = false;
      this.voidBiteDetectedBySound = false;
      this.particleNearBobberCount = 0;
      this.particleCheckTicks = 0;
      this.tickCounter = 0;
      this.sneaking = false;
      this.pointerToJudgeGap = 0;
      this.prevGap = 0;
      this.sameDirectionTicks = 0;
      this.fishOffsetFromStart = 0;
      this.phaseTick = 0;
      this.acv2PointerIdx = -1;
      this.acv2TargetStart = -1;
      this.acv2TargetEnd = -1;
      this.acOffsetSum = 0;
      this.acMinOffset = Integer.MAX_VALUE;
      this.acMaxOffset = Integer.MIN_VALUE;
      this.acTargetSection = -1;
      this.acCalibrated = false;
      this.acPrevOffsetSum = 0;
      this.acOffsetInitialized = false;
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (mc.player != null) {
         Packet<?> packet = event.packet;
         String cn = packet.getClass().getSimpleName();
         boolean hasTitle = false;
         boolean hasSub = false;
         Text titleText = this.tryExtractTitle(packet, cn, true);
         if (titleText != null) {
            this.currentTitle = titleText.getString();
            hasTitle = true;
         }

         Text subText = this.tryExtractTitle(packet, cn, false);
         if (subText != null) {
            this.currentSubtitle = subText.getString();
            hasSub = true;
         }

         if (hasTitle || hasSub) {
            this.detectGame();
         }

         if (this.fishingState == CustomFishingBot.FishingState.WAITING && cn.contains("Particle") && cn.contains("S2C")) {
            this.tryDetectBiteByParticle(packet);
         }

         if (this.fishingState == CustomFishingBot.FishingState.WAITING && this.bobberInVoid && cn.contains("PlaySound") && cn.contains("S2C")) {
            this.tryDetectBiteBySound(packet);
         }

         if (this.fishingState == CustomFishingBot.FishingState.WAITING && this.bobberEntityId > 0 && cn.contains("Velocity") && cn.contains("S2C")) {
            try {
               Method getId = packet.getClass().getMethod("getEntityId");
               int eid = (Integer)getId.invoke(packet);
               if (eid == this.bobberEntityId) {
                  Method getY = packet.getClass().getMethod("getVelocityY");
                  double vy = (Double)getY.invoke(packet);
                  if (vy < -0.05) {
                     this.bobberGoingDown = true;
                     if ((Boolean)this.verboseLog.get()) {
                        this.info("§e! Hook! (vy=" + String.format("%.3f", vy) + ")", new Object[0]);
                     }
                  }
               }
            } catch (Exception var13) {
            }
         }

         if (this.fishingState == CustomFishingBot.FishingState.WAITING && cn.contains("EntityStatus") && cn.contains("S2C")) {
            try {
               Method getId = packet.getClass().getMethod("getEntityId");
               Method getStatus = packet.getClass().getMethod("getStatus");
               int eid = (Integer)getId.invoke(packet);
               byte status = (Byte)getStatus.invoke(packet);
               if (eid == this.bobberEntityId && (status == 16 || status == 23)) {
                  this.bobberGoingDown = true;
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§e! Hook!", new Object[0]);
                  }
               }
            } catch (Exception var14) {
            }
         }
      }
   }

   private void tryDetectBiteByParticle(Packet<?> packet) {
      try {
         Method getX = packet.getClass().getMethod("getX");
         Method getY = packet.getClass().getMethod("getY");
         Method getZ = packet.getClass().getMethod("getZ");
         double px = (Double)getX.invoke(packet);
         double py = (Double)getY.invoke(packet);
         double pz = (Double)getZ.invoke(packet);
         FishingBobberEntity bobber = mc.player.fishHook;
         if (bobber == null) {
            return;
         }

         double bx = bobber.getX();
         double by = bobber.getY();
         double bz = bobber.getZ();
         double dist = Math.sqrt(Math.pow(px - bx, 2.0) + Math.pow(py - by, 2.0) + Math.pow(pz - bz, 2.0));
         if (dist < 2.0) {
            this.particleNearBobberCount++;
            int threshold = this.bobberInVoid ? 8 : 2;
            if (this.particleNearBobberCount >= threshold) {
               this.bobberGoingDown = true;
               if ((Boolean)this.verboseLog.get()) {
                  this.info(
                     "§e! Hook! (ParticleCheckTest, Mode="
                        + (this.bobberInVoid ? "Void" : "Water")
                        + ", Mark="
                        + String.format("%.2f", dist)
                        + ", Number="
                        + this.particleNearBobberCount
                        + ")",
                     new Object[0]
                  );
               }
            }
         }
      } catch (Exception var21) {
      }
   }

   private void tryDetectBiteBySound(Packet<?> packet) {
      try {
         Method getX = packet.getClass().getMethod("getX");
         Method getY = packet.getClass().getMethod("getY");
         Method getZ = packet.getClass().getMethod("getZ");
         Object xRaw = getX.invoke(packet);
         Object yRaw = getY.invoke(packet);
         Object zRaw = getZ.invoke(packet);
         double sx = ((Number)xRaw).doubleValue() / 8.0;
         double sy = ((Number)yRaw).doubleValue() / 8.0;
         double sz = ((Number)zRaw).doubleValue() / 8.0;
         FishingBobberEntity bobber = mc.player.fishHook;
         if (bobber == null) {
            return;
         }

         double bx = bobber.getX();
         double by = bobber.getY();
         double bz = bobber.getZ();
         double dist = Math.sqrt(Math.pow(sx - bx, 2.0) + Math.pow(sy - by, 2.0) + Math.pow(sz - bz, 2.0));
         if (dist < 3.0) {
            this.voidBiteDetectedBySound = true;
            this.bobberGoingDown = true;
            if ((Boolean)this.verboseLog.get()) {
               this.info("§e! Hook! (VoidSoundCheckTest, Mark=" + String.format("%.2f", dist) + ")", new Object[0]);
            }
         }
      } catch (Exception var23) {
      }
   }

   private Text tryExtractTitle(Packet<?> packet, String cn, boolean isTitle) {
      try {
         String target = isTitle ? "SetTitle" : "SetSubtitle";
         if (cn.contains(target)) {
            Method m = packet.getClass().getMethod("getText");
            return (Text)m.invoke(packet);
         }

         if ("TitleS2CPacket".equals(cn)) {
            String mn = isTitle ? "getTitle" : "getSubtitle";

            try {
               Method m = packet.getClass().getMethod(mn);
               return (Text)m.invoke(packet);
            } catch (NoSuchMethodException var7) {
            }
         }

         if (cn.contains("Title")
            && !cn.contains("Animation")
            && !cn.contains("Clear")
            && !cn.contains("Time")
            && (isTitle && !cn.contains("Subtitle") || !isTitle && cn.contains("Subtitle"))) {
            Method m = packet.getClass().getMethod("getText");
            return (Text)m.invoke(packet);
         }
      } catch (Exception var8) {
      }

      return null;
   }

   private void detectGame() {
      if (!this.currentTitle.isEmpty() || !this.currentSubtitle.isEmpty()) {
         CustomFishingBot.GameType g = this.detectFromChars(this.currentTitle, this.currentSubtitle);
         if (g != CustomFishingBot.GameType.NONE && g != this.currentGame) {
            this.currentGame = g;
            this.fishingState = CustomFishingBot.FishingState.GAME;
            this.tickCounter = 0;
            this.phaseTick = 0;
            this.pointerToJudgeGap = 0;
            this.prevGap = 0;
            this.sameDirectionTicks = 0;
            this.acOffsetSum = 0;
            this.acMinOffset = Integer.MAX_VALUE;
            this.acMaxOffset = Integer.MIN_VALUE;
            this.acTargetSection = -1;
            this.acCalibrated = false;
            this.acOffsetInitialized = false;
            this.acTargetSection = this.parseColorTarget(this.currentTitle);
            this.releaseKeys();
            if ((Boolean)this.verboseLog.get()) {
               String extra = this.acTargetSection >= 0 ? "Target=" + this.acTargetSection : "";
               this.info("§b→ little:" + this.gameName(g) + extra, new Object[0]);
            }
         }
      }
   }

   private CustomFishingBot.GameType detectFromChars(String title, String subtitle) {
      if (title.isEmpty() && subtitle.isEmpty()) {
         return CustomFishingBot.GameType.NONE;
      } else {
         boolean hasCustomChars = false;
         boolean hasBar = false;
         boolean hasJudge = false;
         boolean hasPointer = false;
         boolean hasFish = false;

         for (int i = 0; i < subtitle.length(); i++) {
            char c = subtitle.charAt(i);
            if (ALL_CUSTOM_CHARS.contains(c)) {
               hasCustomChars = true;
            }

            if (BAR_CHARS.contains(c)) {
               hasBar = true;
            }

            if (JUDGE_CHARS.contains(c)) {
               hasJudge = true;
            }

            if (FISH_CHARS.contains(c)) {
               hasFish = true;
            }

            if (c == '뀁') {
               hasPointer = true;
            }
         }

         if (!hasCustomChars) {
            return CustomFishingBot.GameType.NONE;
         } else if (hasFish && hasBar && !hasJudge) {
            return CustomFishingBot.GameType.TENSION;
         } else {
            if (title != null) {
               String[] ptrs = new String[]{"█", "▲", "▼", "◆", "●", "■", "▶", "▷", "★", "✦", "►", "⬛"};

               for (String s : ptrs) {
                  if (title.contains(s)) {
                     return CustomFishingBot.GameType.ACCURATE_CLICK_V2;
                  }
               }
            }

            if (hasJudge && hasPointer && hasBar) {
               return CustomFishingBot.GameType.HOLD;
            } else if (hasBar && hasPointer && !hasFish) {
               String t = title.toLowerCase();
               return !t.contains("sneak") && !t.contains("key.") && !t.contains("Sneak") && !t.contains("Hold")
                  ? CustomFishingBot.GameType.ACCURATE_CLICK_V3
                  : CustomFishingBot.GameType.HOLD;
            } else if (hasBar && hasPointer) {
               return CustomFishingBot.GameType.ACCURATE_CLICK;
            } else {
               String sx = subtitle.toLowerCase();
               if (!sx.contains("dance") && !sx.contains("Dance") && !sx.contains("Dance")) {
                  if (!sx.contains("click") && !sx.contains("pointStrike") && !sx.contains("SingleStrike") || !sx.contains("times") && !sx.contains("Times")) {
                     if (!sx.contains("|") || !sx.contains("time left") && !sx.contains("Remaining") && !sx.contains("Time")) {
                        return !hasBar && !hasPointer ? CustomFishingBot.GameType.NONE : CustomFishingBot.GameType.ACCURATE_CLICK;
                     } else {
                        return CustomFishingBot.GameType.CLICK_V2;
                     }
                  } else {
                     return CustomFishingBot.GameType.CLICK;
                  }
               } else {
                  return CustomFishingBot.GameType.DANCE;
               }
            }
         }
      }
   }

   private int parseColorTarget(String title) {
      if (title != null && !title.isEmpty()) {
         if (title.contains("Red") || title.toUpperCase().contains("RED")) {
            return 0;
         } else if (title.contains("Orange") || title.toUpperCase().contains("ORANGE")) {
            return 1;
         } else if (title.contains("Yellow") || title.toUpperCase().contains("YELLOW")) {
            return 2;
         } else if (title.contains("Green") || title.toUpperCase().contains("GREEN")) {
            return 3;
         } else if (title.contains("Cyan") || title.toUpperCase().contains("AQUA") || title.contains("CYAN")) {
            return 4;
         } else if (title.contains("Blue") || title.toUpperCase().contains("BLUE")) {
            return 5;
         } else {
            return !title.contains("Purple") && !title.toUpperCase().contains("PURPLE") ? -1 : 6;
         }
      } else {
         return -1;
      }
   }

   private int calcAcOffsetSum(String sub) {
      if (sub != null && !sub.isEmpty()) {
         int bi = -1;
         int pi = -1;

         for (int i = 0; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if ((BAR_CHARS.contains(c) || JUDGE_CHARS.contains(c)) && bi < 0) {
               bi = i;
            }

            if (c == '뀁') {
               pi = i;
               break;
            }
         }

         if (bi >= 0 && pi >= 0 && pi > bi) {
            int total = 0;

            for (int i = bi + 1; i < pi; i++) {
               Integer v = OFFSET_MAP.get(sub.charAt(i));
               if (v != null) {
                  total += v;
               }
            }

            return total;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (mc.player != null) {
         this.trackBobberY();
         this.particleCheckTicks++;
         if (this.particleCheckTicks >= 20) {
            this.particleNearBobberCount = Math.max(0, this.particleNearBobberCount - 1);
            this.particleCheckTicks = 0;
         }

         if (this.currentGame != CustomFishingBot.GameType.NONE) {
            this.tickCounter++;
            this.handleGameTick();
         } else {
            this.tickAutoFishing();
         }
      }
   }

   private void trackBobberY() {
      if (mc.player == null || mc.player.fishHook == null || mc.world == null) {
         this.bobberLastY = Double.NaN;
         this.bobberDropTicks = 0;
         this.bobberInVoid = false;
      } else if (this.fishingState == CustomFishingBot.FishingState.WAITING) {
         double currentY = mc.player.fishHook.getY();
         int bottomY = mc.world.getBottomY();
         boolean wasInVoid = this.bobberInVoid;
         this.bobberInVoid = currentY <= bottomY + 1;
         if (this.bobberInVoid && !wasInVoid && (Boolean)this.verboseLog.get()) {
            this.info("§5VoidMode", new Object[0]);
         }

         if (!this.bobberInVoid && !Double.isNaN(this.bobberLastY)) {
            double drop = this.bobberLastY - currentY;
            if (drop > 0.05) {
               this.bobberDropTicks++;
               if (this.bobberDropTicks >= 3 && !this.bobberGoingDown) {
                  this.bobberGoingDown = true;
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§e! Hook! (MarkYdown" + String.format("%.2f", drop) + ", =" + this.bobberDropTicks + "tick)", new Object[0]);
                  }
               }
            } else {
               this.bobberDropTicks = 0;
            }
         }

         this.bobberLastY = currentY;
      }
   }

   private void tickAutoFishing() {
      switch (this.fishingState) {
         case IDLE:
            if (this.holdingFishingRod()) {
               if ((Boolean)this.verboseLog.get()) {
                  this.info("§7...", new Object[0]);
               }

               this.rightClick();
               this.fishingState = CustomFishingBot.FishingState.CASTING;
               this.castTimer = 0;
               this.bobberEntityId = -1;
               this.bobberGoingDown = false;
               this.bobberInVoid = false;
               this.voidBiteDetectedBySound = false;
               this.bobberLastY = Double.NaN;
               this.bobberDropTicks = 0;
               this.particleNearBobberCount = 0;
            }
            break;
         case CASTING:
            this.castTimer++;
            FishingBobberEntity bobber = mc.player.fishHook;
            if (bobber != null) {
               this.bobberEntityId = bobber.getId();
               this.fishingState = CustomFishingBot.FishingState.WAITING;
               this.waitTimer = 0;
               this.bobberLastY = bobber.getY();
               if (mc.world != null && bobber.getY() <= mc.world.getBottomY() + 1) {
                  this.bobberInVoid = true;
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§5VoidMode WaitHook...", new Object[0]);
                  }
               } else {
                  this.bobberInVoid = false;
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§7WaitHook...", new Object[0]);
                  }
               }
            } else if (this.castTimer > 40) {
               this.fishingState = CustomFishingBot.FishingState.IDLE;
            }
            break;
         case WAITING:
            this.waitTimer++;
            if (mc.player.fishHook == null) {
               if (this.waitTimer > 20) {
                  this.fishingState = CustomFishingBot.FishingState.COOLDOWN;
               }
            } else {
               this.bobberEntityId = mc.player.fishHook.getId();
               if (this.bobberGoingDown) {
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§6Reel!", new Object[0]);
                  }

                  this.rightClick();
                  this.fishingState = CustomFishingBot.FishingState.BITE;
                  this.stateTimer = 0;
               }

               if (this.waitTimer >= (Integer)this.maxWaitTicks.get()) {
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§7WaitTime, ReelHeavy", new Object[0]);
                  }

                  this.rightClick();
                  this.fishingState = CustomFishingBot.FishingState.BITE;
                  this.stateTimer = 0;
               }
            }
            break;
         case BITE:
            this.stateTimer++;
            if (mc.player.fishHook == null && this.currentGame == CustomFishingBot.GameType.NONE && this.stateTimer > 5) {
               this.fishingState = CustomFishingBot.FishingState.COOLDOWN;
               this.cooldownTimer = 0;
            }

            if (this.stateTimer > 60) {
               this.fishingState = CustomFishingBot.FishingState.IDLE;
            }
            break;
         case GAME:
            this.stateTimer++;
            if (this.stateTimer > 600) {
               this.currentGame = CustomFishingBot.GameType.NONE;
               this.fishingState = CustomFishingBot.FishingState.COOLDOWN;
               this.cooldownTimer = 0;
            }
            break;
         case COOLDOWN:
            this.cooldownTimer++;
            if (this.cooldownTimer >= (Integer)this.cooldownTicks.get()) {
               this.fishingState = CustomFishingBot.FishingState.IDLE;
            }
      }
   }

   private boolean holdingFishingRod() {
      return mc.player == null
         ? false
         : mc.player.getMainHandStack().isOf(Items.FISHING_ROD) || mc.player.getOffHandStack().isOf(Items.FISHING_ROD);
   }

   private void handleGameTick() {
      if (this.tickCounter > 600) {
         if ((Boolean)this.verboseLog.get()) {
            this.warning("Time", new Object[0]);
         }

         this.currentGame = CustomFishingBot.GameType.NONE;
         this.fishingState = CustomFishingBot.FishingState.COOLDOWN;
         this.cooldownTimer = 0;
      } else {
         switch (this.currentGame) {
            case HOLD:
               this.parseHoldSubtitle(this.currentSubtitle);
               this.phaseTick++;
               if (this.pointerToJudgeGap > 5) {
                  this.startSneak();
               } else if (this.pointerToJudgeGap < -10) {
                  this.stopSneak();
               } else if (this.sameDirectionTicks > 5 && this.pointerToJudgeGap > 0) {
                  this.startSneak();
               } else if (this.sameDirectionTicks > 5 && this.pointerToJudgeGap < -5) {
                  this.stopSneak();
               } else {
                  this.startSneak();
               }

               if (this.tickCounter > 600) {
                  this.releaseKeys();
                  this.gameEnded();
               }
               break;
            case HOLD_V2:
               this.parseHoldSubtitle(this.currentSubtitle);
               this.phaseTick++;
               if (this.pointerToJudgeGap > 5) {
                  this.rightClick();
               }

               if (this.tickCounter > 600) {
                  this.gameEnded();
               }
               break;
            case CLICK:
            case CLICK_V2:
               if (this.tickCounter % Math.max(1, (Integer)this.actionInterval.get() + 1) == 0) {
                  this.rightClick();
               }

               if (this.tickCounter > 200) {
                  this.gameEnded();
               }
               break;
            case TENSION:
               this.phaseTick++;
               if (this.phaseTick % 14 < 6) {
                  this.startSneak();
               } else {
                  this.stopSneak();
               }

               if (this.tickCounter > 600) {
                  this.releaseKeys();
                  this.gameEnded();
               }
               break;
            case DANCE:
               switch (this.tickCounter % 4) {
                  case 0:
                     this.rightClick();
                     break;
                  case 1:
                     this.leftClick();
                     break;
                  case 2:
                     if (mc.player != null) {
                        mc.player.jump();
                     }
                     break;
                  case 3:
                     this.sneakTap();
               }

               if (this.tickCounter > 200) {
                  this.gameEnded();
               }
               break;
            case ACCURATE_CLICK:
               this.phaseTick++;
               int curOffset = this.calcAcOffsetSum(this.currentSubtitle);
               if (curOffset != 0) {
                  if (!this.acOffsetInitialized) {
                     this.acPrevOffsetSum = curOffset;
                     this.acMinOffset = curOffset;
                     this.acMaxOffset = curOffset;
                     this.acOffsetInitialized = true;
                     if ((Boolean)this.verboseLog.get()) {
                        this.info("§8AC Start: offset=" + curOffset, new Object[0]);
                     }
                  } else {
                     if (curOffset < this.acMinOffset) {
                        this.acMinOffset = curOffset;
                     }

                     if (curOffset > this.acMaxOffset) {
                        this.acMaxOffset = curOffset;
                     }

                     this.acPrevOffsetSum = curOffset;
                  }
               }

               if (this.acOffsetInitialized && !this.acCalibrated) {
                  int totalRange = this.acMaxOffset - this.acMinOffset;
                  if (totalRange > 20) {
                     this.acCalibrated = true;
                     if ((Boolean)this.verboseLog.get()) {
                        this.info("§8AC CompleteComplete: range=" + totalRange + "Target=" + this.acTargetSection, new Object[0]);
                     }
                  }
               }

               if (this.acCalibrated && this.acTargetSection >= 0) {
                  int totalRange = this.acMaxOffset - this.acMinOffset;
                  if (totalRange > 0) {
                     int relPos = curOffset - this.acMinOffset;
                     int estSection = relPos * 7 / totalRange;
                     if (estSection == this.acTargetSection && this.phaseTick > 10) {
                        if ((Boolean)this.verboseLog.get()) {
                           this.info("§a✓ HitTarget Color! section=" + estSection + " tick=" + this.phaseTick, new Object[0]);
                        }

                        this.rightClick();
                        this.gameEnded();
                        break;
                     }
                  }
               }

               if (this.phaseTick >= (Integer)this.acClickDelay.get() + 30) {
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§eAC TimeReel (tick=" + this.phaseTick + ")", new Object[0]);
                  }

                  this.rightClick();
                  this.gameEnded();
               }
               break;
            case ACCURATE_CLICK_V2:
               this.parseAcv2Bar(this.currentTitle);
               if (this.acv2PointerIdx >= 0
                  && this.acv2TargetStart >= 0
                  && this.acv2TargetEnd >= 0
                  && this.acv2PointerIdx >= this.acv2TargetStart
                  && this.acv2PointerIdx <= this.acv2TargetEnd) {
                  this.rightClick();
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§a✓ ACv2 Hit!", new Object[0]);
                  }

                  this.gameEnded();
               } else if (this.tickCounter > 100) {
                  this.rightClick();
                  this.gameEnded();
               }
               break;
            case ACCURATE_CLICK_V3:
               this.parseHoldSubtitle(this.currentSubtitle);
               this.phaseTick++;
               if (this.pointerToJudgeGap < 0 && this.pointerToJudgeGap > -30) {
                  this.rightClick();
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§a✓ ACv3 Hit! (gap=" + this.pointerToJudgeGap + ")", new Object[0]);
                  }

                  this.gameEnded();
               } else if (this.phaseTick >= (Integer)this.acClickDelay.get() + 10) {
                  this.rightClick();
                  if ((Boolean)this.verboseLog.get()) {
                     this.info("§aACv3 TimepointStrike (phaseTick=" + this.phaseTick + ")", new Object[0]);
                  }

                  this.gameEnded();
               }
         }
      }
   }

   private void gameEnded() {
      this.currentGame = CustomFishingBot.GameType.NONE;
      this.fishingState = CustomFishingBot.FishingState.COOLDOWN;
      this.cooldownTimer = 0;
      this.tickCounter = 0;
      this.releaseKeys();
   }

   private void parseHoldSubtitle(String sub) {
      if (sub != null && !sub.isEmpty()) {
         int ji = -1;
         int pi = -1;

         for (int i = 0; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (JUDGE_CHARS.contains(c)) {
               ji = i;
            }

            if (c == '뀁') {
               pi = i;
            }
         }

         if (ji >= 0 && pi >= 0 && pi > ji) {
            int total = 0;

            for (int i = ji + 1; i < pi; i++) {
               Integer v = OFFSET_MAP.get(sub.charAt(i));
               if (v != null) {
                  total += v;
               }
            }

            this.prevGap = this.pointerToJudgeGap;
            this.pointerToJudgeGap = total;
            this.sameDirectionTicks = Math.signum((float)this.pointerToJudgeGap) == Math.signum((float)this.prevGap) ? this.sameDirectionTicks + 1 : 0;
         }
      }
   }

   private void parseTensionSubtitle(String sub) {
      if (sub != null && !sub.isEmpty()) {
         int bi = -1;
         int fi = -1;

         for (int i = 0; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (BAR_CHARS.contains(c)) {
               bi = i;
            }

            if (FISH_CHARS.contains(c)) {
               fi = i;
            }
         }

         if (bi >= 0 && fi >= 0 && fi > bi) {
            int total = 0;

            for (int i = bi + 1; i < fi; i++) {
               Integer v = OFFSET_MAP.get(sub.charAt(i));
               if (v != null) {
                  total += v;
               }
            }

            this.fishOffsetFromStart = total;
         }
      }
   }

   private void parseAcv2Bar(String bar) {
      if (bar != null && !bar.isEmpty()) {
         String[] ptrs = new String[]{"█", "▲", "▼", "◆", "●", "■", "▶", "▷", "★", "✦", "►", "⬛"};
         String[] tgts = new String[]{"→", "◆", "■", "●", "★", "☆", "▬", "▮", "▓", "▰", "▱", "▭"};
         this.acv2PointerIdx = -1;
         this.acv2TargetStart = -1;
         this.acv2TargetEnd = -1;

         for (String s : ptrs) {
            int idx = bar.indexOf(s);
            if (idx >= 0) {
               this.acv2PointerIdx = idx;
               break;
            }
         }

         for (String sx : tgts) {
            int start = bar.indexOf(sx);
            if (start >= 0 && start != this.acv2PointerIdx) {
               this.acv2TargetStart = start;
               this.acv2TargetEnd = start;
               int i = start + 1;

               while (i < bar.length() && String.valueOf(bar.charAt(i)).equals(sx)) {
                  this.acv2TargetEnd = i++;
               }
               break;
            }
         }
      }
   }

   private void rightClick() {
      if (mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }
   }

   private void leftClick() {
      if (mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.attackEntity(mc.player, mc.player);
      }
   }

   private void startSneak() {
      if (!this.sneaking && mc.options != null) {
         mc.options.sneakKey.setPressed(true);
         this.sneaking = true;
      }
   }

   private void stopSneak() {
      if (this.sneaking && mc.options != null) {
         mc.options.sneakKey.setPressed(false);
         this.sneaking = false;
      }
   }

   private void sneakTap() {
      if (mc.options != null) {
         mc.options.sneakKey.setPressed(true);
      }
   }

   private void releaseKeys() {
      this.stopSneak();
   }

   private String gameName(CustomFishingBot.GameType g) {
      return switch (g) {
         case HOLD -> "HOLD(HoldSneak)";
         case HOLD_V2 -> "HOLD_V2(HoldRightKey)";
         case CLICK -> "CLICK(Strike)";
         case CLICK_V2 -> "CLICK_V2(Strike)";
         case TENSION -> "TENSION(Power)";
         case DANCE -> "DANCE(Dance)";
         case ACCURATE_CLICK -> "ACCURATE_CLICK(RateSet)";
         case ACCURATE_CLICK_V2 -> "ACCURATE_CLICK_V2(Text)";
         case ACCURATE_CLICK_V3 -> "ACCURATE_CLICK_V3(Image)";
         default -> "NONE";
      };
   }

   static {
      ALL_CUSTOM_CHARS.add('뀁');
      ALL_CUSTOM_CHARS.addAll(BAR_CHARS);
      ALL_CUSTOM_CHARS.addAll(JUDGE_CHARS);
      ALL_CUSTOM_CHARS.addAll(FISH_CHARS);
   }

   private static enum FishingState {
      IDLE,
      CASTING,
      WAITING,
      BITE,
      GAME,
      COOLDOWN;
   }

   private static enum GameType {
      NONE,
      HOLD,
      HOLD_V2,
      CLICK,
      CLICK_V2,
      TENSION,
      DANCE,
      ACCURATE_CLICK,
      ACCURATE_CLICK_V2,
      ACCURATE_CLICK_V3;
   }
}
