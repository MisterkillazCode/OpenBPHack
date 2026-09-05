package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class AdaPacketMine extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgAutoMine = this.settings.createGroup("Auto Mine");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<AdaPacketMine.SpeedmineMode> modeConfig = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Mode")).description("MineMode")).defaultValue(AdaPacketMine.SpeedmineMode.PACKET)).build());
   private final Setting<Boolean> multitaskConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("many"))
                     .description("AllowatUseItem(Gold)TimeMine"))
                  .defaultValue(false))
               .visible(() -> this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Boolean> doubleBreakConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Heavy"))
                     .description("AllowTimeMineBlock"))
                  .defaultValue(true))
               .visible(() -> this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Double> rangeConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("MineDistance"))
               .defaultValue(4.5)
               .min(0.1)
               .sliderRange(0.1, 6.0)
               .visible(() -> this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Double> speedConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("SpeedRate"))
               .description("MineEnterDegree'sSpeed"))
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 1.0)
            .build()
      );
   private final Setting<Boolean> bypassGround = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("GroundFacepast (Bypass)"))
                     .description("atWaterinAirinMine (Grim/NCP)"))
                  .defaultValue(true))
               .visible(() -> this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Boolean> instantConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("BetweenMine(Instant)"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Keybind> instantToggleKey = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("SwitchKey"))
               .defaultValue(Keybind.none()))
            .build()
      );
   private final Setting<AdaPacketMine.Swap> swapConfig = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("AutoSwitchTool")).description("MineCompleteCompleteafterSwitchTool'sDirection"))
                  .defaultValue(AdaPacketMine.Swap.SILENT))
               .visible(() -> this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Boolean> rotateConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("AutoRotate"))
                     .description("MineTimeRotateViewFacingBlock"))
                  .defaultValue(false))
               .visible(() -> this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Boolean> grimConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Grimpast"))
                  .description("toGrimAntiDoSendPack"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> grimNewConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Grim-V3"))
                     .description("toNewGrim (V3/V4)"))
                  .defaultValue(true))
               .visible(this.grimConfig::get))
            .build()
      );
   private final Setting<Boolean> miningFix = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("die"))
                     .description("DefenseStopMineEnterDegreedie"))
                  .defaultValue(false))
               .visible(() -> (Boolean)this.grimConfig.get() && (Boolean)this.grimNewConfig.get()))
            .build()
      );
   private final Setting<Boolean> autoMine = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("EnableAuto Mine"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Double> enemyRange = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("enemyRange"))
               .defaultValue(5.0)
               .min(1.0)
               .sliderRange(1.0, 10.0)
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> strictDirection = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DirectionCheck"))
                  .defaultValue(false))
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("EnableRender"))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender.add(((Builder)((Builder)new Builder().name("Render Mode")).defaultValue(ShapeMode.Both)).build());
   private final Setting<SettingColor> colorConfig = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("MineinColor"))
            .defaultValue(new SettingColor(255, 0, 0, 80))
            .build()
      );
   private final Setting<SettingColor> colorDoneConfig = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("justColor"))
            .defaultValue(new SettingColor(0, 255, 255, 80))
            .build()
      );
   private final Map<BlockPos, AdaPacketMine.Animation> fadeList = new ConcurrentHashMap<>();
   private AdaPacketMine.FirstOutQueue<AdaPacketMine.MiningData> miningQueue;
   private long lastBreak;
   private boolean instantTogglePressed = false;
   private BlockPos lastAutoMineBlock = null;
   private long lastAutoMineTime = 0L;
   private int internalSwappedSlot = -1;
   private int internalOriginalSlot = -1;
   private int swapBackTicks = 0;
   private Field cachedSlotField = null;

   public AdaPacketMine() {
      super(AddonTemplate.CATEGORY, "AdaPacketMine", "Packet mine with multi-block support, auto tool switching and configurable break speed.");
   }

   public void onActivate() {
      int queueSize = this.doubleBreakConfig.get() ? 2 : 1;
      this.miningQueue = new AdaPacketMine.FirstOutQueue<>(queueSize);
      this.resetInternalState();
   }

   public void onDeactivate() {
      if (this.miningQueue != null) {
         this.miningQueue.clear();
      }

      this.fadeList.clear();
      this.resetInternalState();
      if (this.mc.player != null && this.internalOriginalSlot != -1) {
         this.setInvSlot(this.internalOriginalSlot);
         this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.internalOriginalSlot));
      }
   }

   private void resetInternalState() {
      this.internalSwappedSlot = -1;
      this.internalOriginalSlot = -1;
      this.swapBackTicks = 0;
      this.lastAutoMineBlock = null;
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.onDeactivate();
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.swapBackTicks > 0) {
            this.swapBackTicks--;
            if (this.swapBackTicks <= 0 && this.internalSwappedSlot != -1 && this.internalOriginalSlot != -1) {
               this.setInvSlot(this.internalOriginalSlot);
               this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.internalOriginalSlot));
               this.internalSwappedSlot = -1;
               this.internalOriginalSlot = -1;
            }
         }

         this.handleKeyToggles();
         if (this.modeConfig.get() != AdaPacketMine.SpeedmineMode.DAMAGE) {
            this.handleAutoMine();
            if (!this.miningQueue.isEmpty()) {
               List<AdaPacketMine.MiningData> toRemove = new ArrayList<>();

               for (AdaPacketMine.MiningData data : this.miningQueue) {
                  if (data.isAir()) {
                     data.resetBreakTime();
                     if (!(Boolean)this.instantConfig.get()) {
                        toRemove.add(data);
                     }
                  } else {
                     double distSq = this.mc.player.squaredDistanceTo(data.getCenterPos());
                     if (distSq > (Double)this.rangeConfig.get() * (Double)this.rangeConfig.get()) {
                        toRemove.add(data);
                     } else {
                        if (!data.isStarted()) {
                           this.startMining(data);
                        }

                        float damageDelta = this.calcBlockBreakingDelta(data.getState(), data.getPos());
                        data.damage(damageDelta);
                        if (data.getBlockDamage() >= (Double)this.speedConfig.get()) {
                           if (this.mc.player.isUsingItem() && !(Boolean)this.multitaskConfig.get()) {
                              return;
                           }

                           long now = System.currentTimeMillis();
                           boolean needsResend = data.hasAttemptedBreak() && now - data.lastStopPacket > 500L;
                           if (!data.hasAttemptedBreak() || needsResend) {
                              this.stopMining(data);
                              data.setAttemptedBreak(true);
                              data.lastStopPacket = now;
                           }

                           if (!(Boolean)this.instantConfig.get() && data.passedAttemptedBreakTime(2000L)) {
                              toRemove.add(data);
                           }
                        }

                        if (data.hasAttemptedBreak() && data.passedAttemptedBreakTime(5000L)) {
                           this.abortMining(data);
                           toRemove.add(data);
                        }
                     }
                  }
               }

               this.miningQueue.removeAll(toRemove);
            }
         }
      }
   }

   private void handleKeyToggles() {
      if (((Keybind)this.instantToggleKey.get()).isPressed() && !this.instantTogglePressed) {
         this.instantTogglePressed = true;
         this.instantConfig.set(!(Boolean)this.instantConfig.get());
         this.info("BetweenMine:" + (this.instantConfig.get() ? "Enable" : "Disable"), new Object[0]);
         if (!(Boolean)this.instantConfig.get()) {
            this.miningQueue.clear();
         }
      } else if (!((Keybind)this.instantToggleKey.get()).isPressed()) {
         this.instantTogglePressed = false;
      }
   }

   private void handleAutoMine() {
      if ((Boolean)this.autoMine.get()) {
         long currentTime = System.currentTimeMillis();
         int maxQueue = this.doubleBreakConfig.get() ? 2 : 1;
         if (this.miningQueue.size() < maxQueue && currentTime - this.lastAutoMineTime >= 250L) {
            PlayerEntity target = this.getClosestEnemy();
            if (target != null) {
               BlockPos targetBlock = this.findBestEnemyBlock(target);
               if (targetBlock != null && !this.isMiningBlock(targetBlock)) {
                  Direction dir = this.getInteractDirection(targetBlock);
                  if (dir == null && (Boolean)this.strictDirection.get()) {
                     return;
                  }

                  if (dir == null) {
                     dir = Direction.UP;
                  }

                  if ((Boolean)this.rotateConfig.get()) {
                     this.performRotation(targetBlock.toCenterPos());
                  }

                  AdaPacketMine.MiningData data = new AdaPacketMine.MiningData(targetBlock, dir);
                  this.queueMiningData(data);
                  this.lastAutoMineBlock = targetBlock;
                  this.lastAutoMineTime = currentTime;
               }
            }
         }
      }
   }

   @EventHandler
   public void onStartBreakingBlock(StartBreakingBlockEvent event) {
      if (!this.mc.player.isCreative() && this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET) {
         event.cancel();
         BlockState state = this.mc.world.getBlockState(event.blockPos);
         if (!state.isAir() && state.getHardness(this.mc.world, event.blockPos) != -1.0F) {
            this.clickMine(new AdaPacketMine.MiningData(event.blockPos, event.direction));
            this.mc.player.swingHand(Hand.MAIN_HAND);
         }
      }
   }

   @EventHandler
   public void onPacketSend(Send event) {
      if (event.packet instanceof UpdateSelectedSlotC2SPacket && this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET) {
      }
   }

   @EventHandler
   public void onPacketReceive(Receive event) {
      if (this.mc.player != null) {
         if (event.packet instanceof BlockUpdateS2CPacket packet) {
            this.handleBlockUpdate(packet.getPos(), packet.getState());
         } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket packet) {
            packet.visitUpdates(this::handleBlockUpdate);
         }
      }
   }

   private void handleBlockUpdate(BlockPos pos, BlockState state) {
      if (state.isAir()) {
         for (AdaPacketMine.MiningData data : this.miningQueue) {
            if (data.getPos().equals(pos)) {
               data.setAttemptedBreak(false);
               if ((Boolean)this.instantConfig.get()) {
                  data.blockDamage = 0.0F;
                  data.started = false;
               }
            }
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.modeConfig.get() == AdaPacketMine.SpeedmineMode.PACKET && (Boolean)this.render.get()) {
         for (AdaPacketMine.MiningData data : this.miningQueue) {
            if (!this.fadeList.containsKey(data.getPos())) {
               this.fadeList.put(data.getPos(), new AdaPacketMine.Animation(true, 250L));
            }
         }

         this.fadeList.entrySet().removeIf(e -> {
            boolean active = false;

            for (AdaPacketMine.MiningData d : this.miningQueue) {
               if (d.getPos().equals(e.getKey()) && !d.getState().isAir()) {
                  active = true;
                  break;
               }
            }

            e.getValue().setState(active);
            return e.getValue().getFactor() == 0.0F;
         });

         for (AdaPacketMine.MiningData datax : this.miningQueue) {
            if (!datax.getState().isAir()) {
               AdaPacketMine.Animation anim = this.fadeList.get(datax.getPos());
               if (anim != null) {
                  float factor = anim.getFactor();
                  boolean done = datax.getBlockDamage() >= 0.95F;
                  SettingColor c = done ? (SettingColor)this.colorDoneConfig.get() : (SettingColor)this.colorConfig.get();
                  int boxAlpha = (int)(c.a * 0.5 * factor);
                  int lineAlpha = (int)(c.a * factor);
                  Color boxColor = new Color(c.r, c.g, c.b, boxAlpha);
                  Color lineColor = new Color(c.r, c.g, c.b, lineAlpha);
                  BlockPos pos = datax.getPos();
                  VoxelShape shape = datax.getState().getOutlineShape(this.mc.world, pos);
                  if (shape.isEmpty()) {
                     shape = VoxelShapes.fullCube();
                  }

                  Box box = shape.getBoundingBox().offset(pos);
                  float total = 1.0F;
                  float progress = MathHelper.clamp(datax.getBlockDamage() / total, 0.01F, 1.0F);
                  double centerX = box.minX + (box.maxX - box.minX) / 2.0;
                  double centerY = box.minY + (box.maxY - box.minY) / 2.0;
                  double centerZ = box.minZ + (box.maxZ - box.minZ) / 2.0;
                  double scale = progress;
                  double dx = (box.maxX - box.minX) / 2.0 * scale;
                  double dy = (box.maxY - box.minY) / 2.0 * scale;
                  double dz = (box.maxZ - box.minZ) / 2.0 * scale;
                  Box renderBox = new Box(centerX - dx, centerY - dy, centerZ - dz, centerX + dx, centerY + dy, centerZ + dz);
                  event.renderer.box(renderBox, boxColor, lineColor, (ShapeMode)this.shapeMode.get(), 0);
               }
            }
         }
      }
   }

   public void clickMine(AdaPacketMine.MiningData data) {
      if (this.miningQueue.size() <= (this.doubleBreakConfig.get() ? 2 : 1)) {
         this.queueMiningData(data);
      }
   }

   private void queueMiningData(AdaPacketMine.MiningData data) {
      if (!data.isAir()) {
         boolean exists = this.miningQueue.stream().anyMatch(d -> d.getPos().equals(data.getPos()));
         if (!exists) {
            this.miningQueue.addFirst(data);
         }
      }
   }

   private boolean startMining(AdaPacketMine.MiningData data) {
      if (data.isStarted()) {
         return false;
      } else {
         data.setStarted();
         if ((Boolean)this.grimConfig.get()) {
            if ((Boolean)this.grimNewConfig.get()) {
               if (!(Boolean)this.miningFix.get()) {
                  this.sendAction(Action.START_DESTROY_BLOCK, data);
                  this.sendAction(Action.ABORT_DESTROY_BLOCK, data);
                  this.sendAction(Action.STOP_DESTROY_BLOCK, data);
               } else {
                  this.sendAction(Action.ABORT_DESTROY_BLOCK, data);
               }

               this.sendAction(Action.START_DESTROY_BLOCK, data);
               this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            } else {
               this.sendAction(Action.ABORT_DESTROY_BLOCK, data);
               this.sendAction(Action.STOP_DESTROY_BLOCK, data);
               this.sendAction(Action.START_DESTROY_BLOCK, data);
               this.sendAction(Action.STOP_DESTROY_BLOCK, data);
            }
         } else {
            this.sendAction(Action.START_DESTROY_BLOCK, data);
            this.sendAction(Action.STOP_DESTROY_BLOCK, data);
         }

         return true;
      }
   }

   private void stopMining(AdaPacketMine.MiningData data) {
      if (data.isStarted() && !data.isAir()) {
         if ((Boolean)this.rotateConfig.get()) {
            this.performRotation(data.getCenterPos());
         }

         int bestSlot = this.getBestTool(data.getState());
         int currentSlot = this.getInvSlot();
         boolean needsSwap = bestSlot != -1 && bestSlot != currentSlot;
         if (needsSwap) {
            if (this.internalOriginalSlot == -1) {
               this.internalOriginalSlot = currentSlot;
            }

            if (this.swapConfig.get() == AdaPacketMine.Swap.SILENT) {
               this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
               this.internalSwappedSlot = bestSlot;
               this.swapBackTicks = 3;
            } else if (this.swapConfig.get() == AdaPacketMine.Swap.NORMAL) {
               this.setInvSlot(bestSlot);
            }
         } else if (this.internalSwappedSlot != -1) {
            this.swapBackTicks = 3;
         }

         boolean isFallFlying = this.mc.player.getPose().toString().equals("GLIDING");
         if ((Boolean)this.bypassGround.get() && !isFallFlying && !data.getState().isAir()) {
            boolean inWater = this.mc.player.isSubmergedIn(FluidTags.WATER);
            boolean inAir = !this.mc.player.isOnGround();
            if (inWater || inAir) {
               this.mc
                  .getNetworkHandler()
                  .sendPacket(
                     new Full(
                        this.mc.player.getX(),
                        this.mc.player.getY() + 1.0E-9,
                        this.mc.player.getZ(),
                        this.mc.player.getYaw(),
                        this.mc.player.getPitch(),
                        true,
                        false
                     )
                  );
               this.mc.player.onLanding();
            }
         }

         this.sendAction(Action.STOP_DESTROY_BLOCK, data);
         this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         this.lastBreak = System.currentTimeMillis();
      }
   }

   private void abortMining(AdaPacketMine.MiningData data) {
      if (data.isStarted() && !data.isAir()) {
         this.sendAction(Action.ABORT_DESTROY_BLOCK, data);
      }
   }

   private void sendAction(Action action, AdaPacketMine.MiningData data) {
      this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(action, data.getPos(), data.getDirection()));
   }

   private void setInvSlot(int slot) {
      try {
         Field f = this.getSlotField();
         if (f != null) {
            f.setInt(this.mc.player.getInventory(), slot);
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   private int getInvSlot() {
      try {
         Field f = this.getSlotField();
         if (f != null) {
            return f.getInt(this.mc.player.getInventory());
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }

      return 0;
   }

   private Field getSlotField() {
      if (this.cachedSlotField != null) {
         return this.cachedSlotField;
      } else {
         Class<?> clazz = PlayerInventory.class;
         String[] possibleNames = new String[]{"selectedSlot", "selectedSlot", "c"};

         for (String name : possibleNames) {
            try {
               Field f = clazz.getDeclaredField(name);
               f.setAccessible(true);
               this.cachedSlotField = f;
               return f;
            } catch (NoSuchFieldException var8) {
            }
         }

         return null;
      }
   }

   private float calcBlockBreakingDelta(BlockState state, BlockPos pos) {
      if (this.swapConfig.get() == AdaPacketMine.Swap.OFF) {
         return state.calcBlockBreakingDelta(this.mc.player, this.mc.world, pos);
      } else {
         float hardness = state.getHardness(this.mc.world, pos);
         if (hardness == -1.0F) {
            return 0.0F;
         } else {
            int bestSlot = this.getBestTool(state);
            ItemStack stack = this.mc.player.getInventory().getStack(bestSlot);
            float speed = stack.getMiningSpeedMultiplier(state);
            int efficiencyLevel = this.getEfficiencyLevel(stack);
            if (efficiencyLevel > 0 && !stack.isEmpty()) {
               speed += efficiencyLevel * efficiencyLevel + 1;
            }

            if (this.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
               int amplifier = this.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier();
               speed *= 1.0F + (amplifier + 1) * 0.2F;
            }

            if (this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
               speed *= 0.3F;
            }

            if (!(Boolean)this.bypassGround.get()) {
               if (this.mc.player.isSubmergedIn(FluidTags.WATER) && !this.hasAquaAffinity()) {
                  speed /= 5.0F;
               }

               if (!this.mc.player.isOnGround()) {
                  speed /= 5.0F;
               }
            }

            float damage = speed / hardness;
            boolean canHarvest = !state.isToolRequired() || stack.isSuitableFor(state);
            return damage / (canHarvest ? 30.0F : 100.0F);
         }
      }
   }

   private int getEfficiencyLevel(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         try {
            ItemEnchantmentsComponent enchantments = stack.getEnchantments();
            if (enchantments == null) {
               return 0;
            } else {
               for (Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
                  if (entry != null && entry.getKey() != null) {
                     String idStr = ((RegistryEntry)entry.getKey()).toString().toLowerCase();
                     if (((RegistryEntry)entry.getKey()).getKey().isPresent()) {
                        idStr = ((RegistryKey)((RegistryEntry)entry.getKey()).getKey().get()).getValue().toString();
                     }

                     if (idStr.contains("efficiency")) {
                        return entry.getIntValue();
                     }
                  }
               }

               return 0;
            }
         } catch (Exception var6) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private boolean hasAquaAffinity() {
      ItemStack helmet = this.mc.player.getEquippedStack(EquipmentSlot.HEAD);
      if (helmet.isEmpty()) {
         return false;
      } else {
         try {
            for (Entry<RegistryEntry<Enchantment>> entry : helmet.getEnchantments().getEnchantmentEntries()) {
               String id = ((RegistryEntry)entry.getKey()).getKey().map(k -> k.getValue().toString()).orElse("");
               if (id.contains("aqua_affinity")) {
                  return true;
               }
            }
         } catch (Exception var5) {
         }

         return false;
      }
   }

   private int getBestTool(BlockState state) {
      int bestSlot = -1;
      float bestSpeed = 0.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         float speed = stack.getMiningSpeedMultiplier(state);
         if (speed > 1.0F) {
            int effLevel = this.getEfficiencyLevel(stack);
            if (effLevel > 0) {
               speed += effLevel * effLevel + 1;
            }
         }

         if (speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = i;
         }
      }

      return bestSlot == -1 ? this.getInvSlot() : bestSlot;
   }

   private void performRotation(Vec3d targetPos) {
      if (this.mc.player != null) {
         double diffX = targetPos.x - this.mc.player.getX();
         double diffY = targetPos.y - this.mc.player.getEyeY();
         double diffZ = targetPos.z - this.mc.player.getZ();
         double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
         float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
         if ((Boolean)this.grimConfig.get()) {
            Rotations.rotate(yaw, pitch, 100, null);
         } else {
            this.mc.player.setYaw(yaw);
            this.mc.player.setPitch(pitch);
         }
      }
   }

   private PlayerEntity getClosestEnemy() {
      PlayerEntity closest = null;
      double closestDist = (Double)this.enemyRange.get() * (Double)this.enemyRange.get();

      for (PlayerEntity player : this.mc.world.getPlayers()) {
         if (player != this.mc.player && !Friends.get().isFriend(player)) {
            double dist = this.mc.player.squaredDistanceTo(player);
            if (dist < closestDist) {
               closestDist = dist;
               closest = player;
            }
         }
      }

      return closest;
   }

   private BlockPos findBestEnemyBlock(PlayerEntity enemy) {
      BlockPos feet = enemy.getBlockPos();
      if (!this.mc.world.getBlockState(feet).isAir()) {
         return feet;
      } else {
         BlockPos[] offsets = new BlockPos[]{feet.north(), feet.south(), feet.east(), feet.west()};

         for (BlockPos p : offsets) {
            if (!this.mc.world.getBlockState(p).isAir() && this.mc.world.getBlockState(p).getHardness(this.mc.world, p) != -1.0F) {
               return p;
            }
         }

         return null;
      }
   }

   private Direction getInteractDirection(BlockPos pos) {
      return Direction.UP;
   }

   private boolean isMiningBlock(BlockPos pos) {
      return this.miningQueue.stream().anyMatch(d -> d.getPos().equals(pos));
   }

   private static class Animation {
      private boolean state;
      private long time;
      private final long duration;

      public Animation(boolean state, long duration) {
         this.state = state;
         this.duration = duration;
         this.time = System.currentTimeMillis();
      }

      public void setState(boolean state) {
         if (this.state != state) {
            this.state = state;
            this.time = System.currentTimeMillis();
         }
      }

      public float getFactor() {
         long elapsed = System.currentTimeMillis() - this.time;
         float progress = Math.min(1.0F, (float)elapsed / (float)this.duration);
         return this.state ? 1.0F : 1.0F - progress;
      }
   }

   private static class FirstOutQueue<T> extends ArrayList<T> {
      private final int maxSize;

      public FirstOutQueue(int maxSize) {
         this.maxSize = maxSize;
      }

      @Override
      public void addFirst(T t) {
         super.add(0, t);

         while (this.size() > this.maxSize) {
            this.remove(this.size() - 1);
         }
      }

      @Override
      public T getFirst() {
         return this.isEmpty() ? null : this.get(0);
      }
   }

   private static class MiningData {
      private final BlockPos pos;
      private final Direction direction;
      private float blockDamage = 0.0F;
      private long breakTime;
      private boolean attemptedBreak;
      private boolean started;
      private long lastStopPacket = 0L;

      public MiningData(BlockPos pos, Direction direction) {
         this.pos = pos;
         this.direction = direction;
         this.breakTime = System.currentTimeMillis();
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public Direction getDirection() {
         return this.direction;
      }

      public Vec3d getCenterPos() {
         return this.pos.toCenterPos();
      }

      public boolean isAir() {
         return MeteorClient.mc.world == null ? true : MeteorClient.mc.world.getBlockState(this.pos).isAir();
      }

      public BlockState getState() {
         return MeteorClient.mc.world.getBlockState(this.pos);
      }

      public void damage(float amount) {
         this.blockDamage += amount;
      }

      public float getBlockDamage() {
         return this.blockDamage;
      }

      public void setAttemptedBreak(boolean b) {
         this.attemptedBreak = b;
         if (b) {
            this.breakTime = System.currentTimeMillis();
         }
      }

      public boolean hasAttemptedBreak() {
         return this.attemptedBreak;
      }

      public void resetBreakTime() {
         this.breakTime = System.currentTimeMillis();
      }

      public boolean passedAttemptedBreakTime(long ms) {
         return System.currentTimeMillis() - this.breakTime >= ms;
      }

      public boolean isStarted() {
         return this.started;
      }

      public void setStarted() {
         this.started = true;
      }

      public boolean isPacketMine() {
         return true;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            AdaPacketMine.MiningData that = (AdaPacketMine.MiningData)o;
            return Objects.equals(this.pos, that.pos);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.pos);
      }
   }

   public static enum SpeedmineMode {
      PACKET,
      DAMAGE;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      OFF;
   }
}
