package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class xhPacketMinePlus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgAutoMine = this.settings.createGroup("Auto Mine");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<xhPacketMinePlus.SpeedmineMode> modeConfig = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("Mode")).defaultValue(xhPacketMinePlus.SpeedmineMode.PACKET)).build());
   private final Setting<Boolean> multitaskConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("many"))
                  .defaultValue(false))
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Boolean> doubleBreakConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("(Double Mine)"))
                     .description("AllowmanyMine"))
                  .defaultValue(true))
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Double> rangeConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Range"))
               .defaultValue(5.0)
               .min(0.1)
               .sliderRange(0.1, 6.0)
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Double> speedConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("Speed"))
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 1.0)
            .build()
      );
   private final Setting<Boolean> bypassGround = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Bypass Ground"))
                  .description("atWaterinAirinMine (Grim/NCP)"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> instantConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("BetweenMine"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Keybind> instantToggleKey = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("BetweenMineSwitchKey"))
               .defaultValue(Keybind.none()))
            .build()
      );
   private final Setting<Boolean> persistentConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("HoldMode"))
                  .defaultValue(false))
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<xhPacketMinePlus.Swap> swapConfig = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("AutoSwitch")).defaultValue(xhPacketMinePlus.Swap.SILENT))
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Boolean> rotateConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Rotate"))
                  .defaultValue(false))
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Boolean> grimConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Grimpast"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> grimNewConfig = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Grim-v3"))
                  .defaultValue(true))
               .visible(this.grimConfig::get))
            .build()
      );
   private final Setting<Boolean> miningFix = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Mine"))
                  .defaultValue(false))
               .visible(() -> (Boolean)this.grimConfig.get() && (Boolean)this.grimNewConfig.get()))
            .build()
      );
   private final Setting<Keybind> autoMineKey = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                  .name("Auto MineKey"))
               .defaultValue(Keybind.none()))
            .build()
      );
   private final Setting<Boolean> autoMine = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Auto Mine"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Double> enemyRange = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("enemyRange"))
            .defaultValue(5.0)
            .build()
      );
   private final Setting<Boolean> strictDirection = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Direction"))
                  .defaultValue(false))
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> targetHead = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("TargetHead"))
                  .defaultValue(false))
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> autoRotate = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoRotate"))
                  .defaultValue(true))
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> antiCrawl = this.sgAutoMine
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DefenseClimbRow"))
                  .defaultValue(true))
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Render"))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender.add(((Builder)((Builder)new Builder().name("Mode")).defaultValue(ShapeMode.Both)).build());
   private final Setting<SettingColor> colorConfig = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("MineinColor"))
               .defaultValue(new SettingColor(0, 0, 255))
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<SettingColor> colorDoneConfig = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("CompleteCompleteColor"))
               .defaultValue(new SettingColor(0, 255, 255))
               .visible(() -> this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET))
            .build()
      );
   private final Setting<Integer> fadeTimeConfig = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("OutTime"))
                  .defaultValue(250))
               .visible(() -> false))
            .build()
      );
   private final Map<BlockPos, xhPacketMinePlus.Animation> fadeList = new HashMap<>();
   private final ArrayList<xhPacketMinePlus.MiningData> miningQueue = new ArrayList<>();
   private boolean instantTogglePressed = false;
   private boolean autoMineTogglePressed = false;
   private PlayerEntity currentTarget = null;
   private BlockPos lastAutoMineBlock = null;
   private long lastAutoMineTime = 0L;
   private BlockPos lastAntiCrawlBlock = null;
   private long lastAntiCrawlTime = 0L;
   private int serverSideSlot = -1;

   public xhPacketMinePlus() {
      super(AddonTemplate.CATEGORY, "xhPacketMinePlus", "Packet-based mining with double-mine and bypass options. Currently unusable.");
   }

   public void onActivate() {
      this.miningQueue.clear();
      this.lastAutoMineBlock = null;
      this.lastAutoMineTime = 0L;
      if (this.mc.player != null) {
         this.serverSideSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
      }
   }

   public void onDeactivate() {
      if (!(Boolean)this.persistentConfig.get() || this.mc.getNetworkHandler() == null) {
         this.miningQueue.clear();
         this.fadeList.clear();
         this.syncSlot();
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.miningQueue.clear();
      this.fadeList.clear();
      this.serverSideSlot = -1;
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.serverSideSlot == -1) {
            this.serverSideSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
         }

         this.handleKeybinds();
         if (this.modeConfig.get() != xhPacketMinePlus.SpeedmineMode.DAMAGE) {
            if ((Boolean)this.autoMine.get() && this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET) {
               this.handleAutoMine();
            }

            this.processMiningQueue();
         }
      }
   }

   private void syncSlot() {
      if (this.mc.player != null) {
         int clientSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
         if (this.serverSideSlot != clientSlot) {
            this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(clientSlot));
            this.serverSideSlot = clientSlot;
         }
      }
   }

   private void processMiningQueue() {
      if (!this.mc.options.useKey.isPressed() && !this.mc.player.isUsingItem()) {
         if (this.miningQueue.isEmpty()) {
            this.syncSlot();
         } else {
            Iterator<xhPacketMinePlus.MiningData> it = this.miningQueue.iterator();

            while (it.hasNext()) {
               xhPacketMinePlus.MiningData data = it.next();
               if (data.getState().isAir()) {
                  data.resetBreakTime();
                  if (!(Boolean)this.instantConfig.get()) {
                     it.remove();
                  }
               } else {
                  double distSq = this.mc.player.getEyePos().squaredDistanceTo(data.getPos().toCenterPos());
                  if (!(distSq > (Double)this.rangeConfig.get() * (Double)this.rangeConfig.get())) {
                     if (!data.isStarted()) {
                        this.startMining(data);
                     }

                     float damageDelta = this.getBreakDelta(data.getSlot(), data.getState(), data.getPos());
                     data.damage(damageDelta);
                  }
               }
            }

            Iterator<xhPacketMinePlus.MiningData> breakIt = this.miningQueue.iterator();

            while (breakIt.hasNext()) {
               xhPacketMinePlus.MiningData data = breakIt.next();
               if (!data.getState().isAir()
                  && data.getBlockDamage() >= (Double)this.speedConfig.get()
                  && ((Boolean)this.multitaskConfig.get() || !this.mc.player.isUsingItem())) {
                  int bestSlot = data.getSlot();
                  if (bestSlot != -1) {
                     boolean forceSwap = data.hasAttemptedBreak();
                     if (this.serverSideSlot != bestSlot || forceSwap) {
                        if (this.swapConfig.get() == xhPacketMinePlus.Swap.SILENT) {
                           this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
                           this.serverSideSlot = bestSlot;
                        } else if (this.swapConfig.get() == xhPacketMinePlus.Swap.NORMAL) {
                           ((InventoryAccessor)this.mc.player.getInventory()).setSelectedSlot(bestSlot);
                           this.serverSideSlot = bestSlot;
                        }
                     }

                     if ((Boolean)this.rotateConfig.get()) {
                        Rotations.rotate(Rotations.getYaw(data.getPos()), Rotations.getPitch(data.getPos()));
                     }

                     boolean isFallFlying = this.mc.player.getPose().name().equals("GLIDING");
                     if ((Boolean)this.bypassGround.get() && !isFallFlying && !data.getState().isAir()) {
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

                     this.sendSequenced(id -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection(), id));
                     this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                     data.setAttemptedBreak(true);
                     if (!(Boolean)this.instantConfig.get()) {
                        breakIt.remove();
                     }
                  }
               }
            }
         }
      } else {
         this.syncSlot();
      }
   }

   public void clickMine(xhPacketMinePlus.MiningData miningData) {
      for (xhPacketMinePlus.MiningData d : this.miningQueue) {
         if (d.getPos().equals(miningData.getPos())) {
            return;
         }
      }

      int max = this.doubleBreakConfig.get() ? 2 : 1;
      if (this.miningQueue.size() >= max) {
         this.miningQueue.remove(this.miningQueue.size() - 1);
      }

      this.miningQueue.add(0, miningData);
   }

   @EventHandler
   public void onSendPacket(Send event) {
      if (event.packet instanceof PlayerActionC2SPacket packet
         && packet.getAction() == Action.START_DESTROY_BLOCK
         && this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.DAMAGE
         && (Boolean)this.grimConfig.get()) {
         this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, packet.getPos(), packet.getDirection()));
      }

      if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
         this.serverSideSlot = packet.getSelectedSlot();
      }
   }

   @EventHandler
   public void onReceivePacket(Receive event) {
      if (this.mc.player != null && this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET) {
         if (event.packet instanceof BlockUpdateS2CPacket packet) {
            this.handleBlockUpdate(packet.getPos(), packet.getState());
         } else if (event.packet instanceof BundleS2CPacket bundle) {
            for (Packet<? super ClientPlayPacketListener> p : bundle.getPackets()) {
               if (p instanceof BlockUpdateS2CPacket packet) {
                  this.handleBlockUpdate(packet.getPos(), packet.getState());
               }
            }
         }
      }
   }

   private void handleBlockUpdate(BlockPos pos, BlockState newState) {
      if (newState.isAir()) {
         for (xhPacketMinePlus.MiningData data : this.miningQueue) {
            if (data.getPos().equals(pos)) {
               data.setAttemptedBreak(false);
            }
         }
      }
   }

   private float getBreakDelta(int slot, BlockState state, BlockPos pos) {
      float hardness = state.getHardness(this.mc.world, pos);
      if (hardness == -1.0F) {
         return 0.0F;
      } else {
         float speed = this.getBlockBreakingSpeed(slot, state);
         if (!(Boolean)this.bypassGround.get()) {
            boolean hasAquaAffinity = false;

            try {
               RegistryEntry<Enchantment> aquaAffinityEntry = this.mc
                  .world
                  .getRegistryManager()
                  .getOrThrow(RegistryKeys.ENCHANTMENT)
                  .getOrThrow(Enchantments.AQUA_AFFINITY);
               hasAquaAffinity = EnchantmentHelper.getEquipmentLevel(aquaAffinityEntry, this.mc.player) > 0;
            } catch (Exception var8) {
            }

            if (this.mc.player.isSubmergedIn(FluidTags.WATER) && !hasAquaAffinity) {
               speed /= 5.0F;
            }

            if (!this.mc.player.isOnGround()) {
               speed /= 5.0F;
            }
         }

         boolean canHarvest = true;
         if (state.isToolRequired()) {
            ItemStack stack = this.mc.player.getInventory().getStack(slot);
            canHarvest = stack.isSuitableFor(state);
         }

         return speed / hardness / (canHarvest ? 30.0F : 100.0F);
      }
   }

   private float getBlockBreakingSpeed(int slot, BlockState state) {
      try {
         ItemStack stack = this.mc.player.getInventory().getStack(slot);
         float f = stack.getMiningSpeedMultiplier(state);
         if (f > 1.0F) {
            try {
               RegistryEntry<Enchantment> effEntry = this.mc
                  .world
                  .getRegistryManager()
                  .getOrThrow(RegistryKeys.ENCHANTMENT)
                  .getOrThrow(Enchantments.EFFICIENCY);
               int i = EnchantmentHelper.getLevel(effEntry, stack);
               if (i > 0 && !stack.isEmpty()) {
                  f += i * i + 1;
               }
            } catch (Exception var7) {
            }
         }

         if (this.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            f *= 1.0F + (this.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
         }

         if (this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            f *= switch (this.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
               case 0 -> 0.3F;
               case 1 -> 0.09F;
               case 2 -> 0.0027F;
               default -> 8.1E-4F;
            };
         }

         return f;
      } catch (Exception var8) {
         return 1.0F;
      }
   }

   private PendingUpdateManager getPendingManager() {
      try {
         Method m = ClientWorld.class.getDeclaredMethod("getPendingUpdateManager");
         m.setAccessible(true);
         return (PendingUpdateManager)m.invoke(this.mc.world);
      } catch (Exception var4) {
         try {
            Method m2 = ClientWorld.class.getDeclaredMethod("getPendingUpdateManager");
            m2.setAccessible(true);
            return (PendingUpdateManager)m2.invoke(this.mc.world);
         } catch (Exception var3) {
            return null;
         }
      }
   }

   private void sendSequenced(SequencedPacketCreator packetCreator) {
      if (this.mc.world != null && this.mc.getNetworkHandler() != null) {
         PendingUpdateManager pendingUpdateManager = this.getPendingManager();
         if (pendingUpdateManager != null) {
            PendingUpdateManager manager = pendingUpdateManager.incrementSequence();

            try {
               int i = manager.getSequence();
               this.mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
            } catch (Throwable var7) {
               if (manager != null) {
                  try {
                     manager.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (manager != null) {
               manager.close();
            }
         } else {
            this.mc.getNetworkHandler().sendPacket(packetCreator.predict(0));
         }
      }
   }

   private void sendAction(Action action, xhPacketMinePlus.MiningData data) {
      this.sendSequenced(id -> new PlayerActionC2SPacket(action, data.getPos(), data.getDirection(), id));
   }

   private void handleKeybinds() {
      if (((Keybind)this.autoMineKey.get()).isPressed()) {
         if (!this.autoMineTogglePressed && this.mc.currentScreen == null) {
            this.autoMineTogglePressed = true;
            this.autoMine.set(!(Boolean)this.autoMine.get());
            this.info("Auto-mine " + (this.autoMine.get() ? "§aenabled" : "§cdisabled"), new Object[0]);
         }
      } else {
         this.autoMineTogglePressed = false;
      }

      if (((Keybind)this.instantToggleKey.get()).isPressed()) {
         if (!this.instantTogglePressed && this.mc.currentScreen == null) {
            this.instantTogglePressed = true;
            this.instantConfig.set(!(Boolean)this.instantConfig.get());
            if (!(Boolean)this.instantConfig.get()) {
               this.miningQueue.clear();
            }

            this.info("Instant mining " + (this.instantConfig.get() ? "§aenabled" : "§cdisabled"), new Object[0]);
         }
      } else {
         this.instantTogglePressed = false;
      }
   }

   private void handleAutoMine() {
      int maxQueue = this.doubleBreakConfig.get() ? 2 : 1;
      long now = System.currentTimeMillis();
      boolean isSwimming = this.mc.player.getPose().name().equals("SWIMMING");
      if ((Boolean)this.antiCrawl.get() && isSwimming && this.miningQueue.size() < maxQueue && now - this.lastAntiCrawlTime >= 100L) {
         BlockPos crawlBlock = this.getAntiCrawlBlock();
         if (crawlBlock != null && !this.isMiningBlock(crawlBlock)) {
            this.addAutoMineTask(crawlBlock, Direction.UP);
            this.lastAntiCrawlBlock = crawlBlock;
            this.lastAntiCrawlTime = now;
            if (!this.miningQueue.isEmpty()) {
               return;
            }
         }
      }

      this.currentTarget = this.getClosestEnemy();
      if (this.currentTarget != null) {
         if (this.miningQueue.size() < maxQueue && now - this.lastAutoMineTime >= 250L) {
            BlockPos targetBlock = this.findBestEnemyBlock(this.currentTarget);
            if (targetBlock != null && !this.isMiningBlock(targetBlock)) {
               Direction dir = this.getInteractDirection(targetBlock);
               if (dir != null || !(Boolean)this.strictDirection.get()) {
                  this.addAutoMineTask(targetBlock, dir == null ? Direction.DOWN : dir);
                  this.lastAutoMineBlock = targetBlock;
                  this.lastAutoMineTime = now;
               }
            }
         }
      } else {
         this.lastAutoMineBlock = null;
      }
   }

   private void addAutoMineTask(BlockPos pos, Direction dir) {
      if ((Boolean)this.autoRotate.get() && (Boolean)this.rotateConfig.get()) {
         Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
      }

      this.queueMiningData(new xhPacketMinePlus.MiningData(pos, dir));
   }

   public void queueMiningData(xhPacketMinePlus.MiningData data) {
      if (!data.getState().isAir()) {
         if (this.miningQueue.stream().anyMatch(p1 -> data.getPos().equals(p1.getPos()))) {
            return;
         }

         this.miningQueue.add(0, data);
      }
   }

   private boolean startMining(xhPacketMinePlus.MiningData data) {
      if (data.isStarted()) {
         return false;
      } else {
         data.setStarted();
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
            this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         } else {
            this.sendAction(Action.ABORT_DESTROY_BLOCK, data);
            this.sendAction(Action.STOP_DESTROY_BLOCK, data);
            this.sendAction(Action.START_DESTROY_BLOCK, data);
            this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }

         return true;
      }
   }

   private int getBestTool(BlockState state) {
      int bestSlot = -1;
      float bestSpeed = 0.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         float speed = stack.getMiningSpeedMultiplier(state);
         if (speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = i;
         }
      }

      return bestSlot == -1 ? ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot() : bestSlot;
   }

   private boolean isMiningBlock(BlockPos pos) {
      for (xhPacketMinePlus.MiningData data : this.miningQueue) {
         if (data.getPos().equals(pos)) {
            return true;
         }
      }

      return false;
   }

   private PlayerEntity getClosestEnemy() {
      return this.mc.world == null
         ? null
         : this.mc
            .world
            .getPlayers()
            .stream()
            .filter(p -> p != this.mc.player && !p.isDead() && !Friends.get().isFriend(p))
            .min(Comparator.comparingDouble(p -> this.mc.player.distanceTo(p)))
            .filter(p -> this.mc.player.distanceTo(p) <= (Double)this.enemyRange.get())
            .orElse(null);
   }

   private BlockPos findBestEnemyBlock(PlayerEntity enemy) {
      if (enemy == null) {
         return null;
      } else {
         BlockPos enemyPos = enemy.getBlockPos();
         if (this.isValidBlock(enemyPos)) {
            return enemyPos;
         } else {
            BlockPos[] surround = new BlockPos[]{enemyPos.north(), enemyPos.south(), enemyPos.east(), enemyPos.west()};

            for (BlockPos pos : surround) {
               if (this.isValidBlock(pos)) {
                  return pos;
               }
            }

            if ((Boolean)this.targetHead.get()) {
               BlockPos head = enemyPos.up(2);
               if (this.isValidBlock(head)) {
                  return head;
               }
            }

            return null;
         }
      }
   }

   private boolean isValidBlock(BlockPos pos) {
      if (this.isMiningBlock(pos)) {
         return false;
      } else if (this.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > (Double)this.rangeConfig.get() * (Double)this.rangeConfig.get()) {
         return false;
      } else {
         BlockState state = this.mc.world.getBlockState(pos);
         return !state.isAir() && state.getHardness(this.mc.world, pos) != -1.0F
            ? state.getBlock() == Blocks.OBSIDIAN
               || state.getBlock() == Blocks.ENDER_CHEST
               || state.getBlock() == Blocks.NETHERITE_BLOCK
               || state.getBlock() == Blocks.ANVIL
            : false;
      }
   }

   private BlockPos getAntiCrawlBlock() {
      BlockPos head = this.mc.player.getBlockPos().up();
      return this.isValidBlock(head) ? head : null;
   }

   private Direction getInteractDirection(BlockPos pos) {
      return Direction.DOWN;
   }

   @EventHandler
   public void onStartBreaking(StartBreakingBlockEvent event) {
      if (this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET) {
         event.cancel();
         BlockState state = this.mc.world.getBlockState(event.blockPos);
         if (state.getHardness(this.mc.world, event.blockPos) != -1.0F && !state.isAir()) {
            this.clickMine(new xhPacketMinePlus.MiningData(event.blockPos, event.direction));
            this.mc.player.swingHand(Hand.MAIN_HAND);
         }
      }
   }

   @EventHandler
   public void onRender(Render3DEvent event) {
      if (this.modeConfig.get() == xhPacketMinePlus.SpeedmineMode.PACKET && (Boolean)this.render.get()) {
         for (xhPacketMinePlus.MiningData data : this.miningQueue) {
            if (!this.fadeList.containsKey(data.getPos())) {
               this.fadeList.put(data.getPos(), new xhPacketMinePlus.Animation(true, ((Integer)this.fadeTimeConfig.get()).longValue()));
            }
         }

         this.fadeList.entrySet().removeIf(e -> {
            boolean active = false;

            for (xhPacketMinePlus.MiningData d : this.miningQueue) {
               if (d.getPos().equals(e.getKey()) && !d.getState().isAir()) {
                  active = true;
                  break;
               }
            }

            e.getValue().setState(active);
            return e.getValue().getFactor() == 0.0F;
         });

         for (xhPacketMinePlus.MiningData datax : this.miningQueue) {
            if (!datax.getState().isAir()) {
               xhPacketMinePlus.Animation anim = this.fadeList.get(datax.getPos());
               if (anim != null) {
                  float factor = anim.getFactor();
                  boolean done = datax.getBlockDamage() >= 0.95F;
                  SettingColor c = done ? (SettingColor)this.colorDoneConfig.get() : (SettingColor)this.colorConfig.get();
                  int boxColor = new SettingColor(c.r, c.g, c.b, (int)(40.0F * factor)).getPacked();
                  int lineColor = new SettingColor(c.r, c.g, c.b, (int)(100.0F * factor)).getPacked();
                  BlockPos pos = datax.getPos();
                  VoxelShape shape = datax.getState().getOutlineShape(this.mc.world, pos);
                  if (shape.isEmpty()) {
                     shape = VoxelShapes.fullCube();
                  }

                  Box box = shape.getBoundingBox().offset(pos);
                  float total = 1.0F;
                  float progress = datax.getState().isAir() ? 1.0F : MathHelper.clamp(datax.getBlockDamage() / total, 0.0F, 1.0F);
                  if (progress == 0.0F) {
                     progress = 0.01F;
                  }

                  double centerX = box.minX + (box.maxX - box.minX) / 2.0;
                  double centerY = box.minY + (box.maxY - box.minY) / 2.0;
                  double centerZ = box.minZ + (box.maxZ - box.minZ) / 2.0;
                  double scale = progress;
                  double dx = (box.maxX - box.minX) / 2.0 * scale;
                  double dy = (box.maxY - box.minY) / 2.0 * scale;
                  double dz = (box.maxZ - box.minZ) / 2.0 * scale;
                  event.renderer
                     .box(
                        new Box(centerX - dx, centerY - dy, centerZ - dz, centerX + dx, centerY + dy, centerZ + dz),
                        new SettingColor(boxColor),
                        new SettingColor(lineColor),
                        (ShapeMode)this.shapeMode.get(),
                        0
                     );
               }
            }
         }
      }
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
         if (this.state) {
            return 1.0F;
         } else {
            long elapsed = System.currentTimeMillis() - this.time;
            return Math.max(0.0F, 1.0F - (float)elapsed / (float)this.duration);
         }
      }
   }

   private class MiningData {
      private final BlockPos pos;
      private final Direction direction;
      private boolean attemptedBreak;
      private long breakTime;
      private float blockDamage;
      private boolean started;

      public MiningData(BlockPos pos, Direction direction) {
         this.pos = pos;
         this.direction = direction;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public Direction getDirection() {
         return this.direction;
      }

      public boolean isStarted() {
         return this.started;
      }

      public void setStarted() {
         this.started = true;
      }

      public boolean hasAttemptedBreak() {
         return this.attemptedBreak;
      }

      public void setAttemptedBreak(boolean b) {
         this.attemptedBreak = b;
         if (b) {
            this.resetBreakTime();
         }
      }

      public void resetBreakTime() {
         this.breakTime = System.currentTimeMillis();
      }

      public float damage(float dmg) {
         this.blockDamage += dmg;
         return this.blockDamage;
      }

      public void resetDamage() {
         this.started = false;
         this.blockDamage = 0.0F;
      }

      public float getBlockDamage() {
         return this.blockDamage;
      }

      public BlockState getState() {
         return xhPacketMinePlus.this.mc.world.getBlockState(this.pos);
      }

      public int getSlot() {
         return xhPacketMinePlus.this.getBestTool(this.getState());
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
