package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.ChestType;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoChestAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> range = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("EnableDistance")).description("Enable'smostbigDistance.")).defaultValue(5.0).min(0.0).sliderMax(6.0).build());
   private final Setting<List<BlockEntityType<?>>> blocks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
                  .name("TolerateDeviceType"))
               .description("SelectwantEnable'sTolerateDevice."))
            .defaultValue(new BlockEntityType[]{BlockEntityType.CHEST, BlockEntityType.BARREL, BlockEntityType.SHULKER_BOX})
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("EnableBetween"))
                  .description("EnabledownOnebefore'sCooldown (Tick)."))
               .defaultValue(3))
            .min(1)
            .sliderMax(20)
            .build()
      );
   private final Setting<Integer> waitTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Record Wait"))
                  .description("toDataPackafterWaitmanyDisable (Tick). giveChestTrackerAntiTime."))
               .defaultValue(2))
            .min(1)
            .sliderMax(20)
            .build()
      );
   private final Setting<Integer> timeout = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ForceTime"))
                  .description("ifnotDisable, manyfew Tick afterForceDisable. die."))
               .defaultValue(15))
            .min(5)
            .sliderMax(60)
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoFace"))
                  .description("EnableTimeAutoFace."))
               .defaultValue(true))
            .build()
      );
   private final Map<BlockPos, Long> openedBlocks = new HashMap<>();
   private final AutoChestAura.PacketListener packetListener = new AutoChestAura.PacketListener();
   private int timer = 0;
   private int packetTimer = 0;
   private int stuckTimer = 0;
   private boolean isPending = false;

   public AutoChestAura() {
      super(AddonTemplate.SC_CATEGORY, "AutoChestAura", "Auto-opens chests in range and tracks them in a chest list. Anti-cheat safety unknown.");
   }

   public void onActivate() {
      this.timer = 0;
      this.packetTimer = 0;
      this.stuckTimer = 0;
      this.isPending = false;
      this.openedBlocks.clear();
      MeteorClient.EVENT_BUS.subscribe(this.packetListener);
   }

   public void onDeactivate() {
      MeteorClient.EVENT_BUS.unsubscribe(this.packetListener);
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.world != null && this.mc.player != null) {
         if (this.isPending) {
            this.stuckTimer++;
            if (this.mc.currentScreen == null && this.stuckTimer > 5) {
               this.resetState();
            } else {
               if (this.packetTimer > 0) {
                  this.packetTimer--;
                  if (this.packetTimer <= 0) {
                     this.forceClose();
                     return;
                  }
               }

               if (this.stuckTimer >= (Integer)this.timeout.get()) {
                  this.forceClose();
               }
            }
         } else if (this.timer > 0) {
            this.timer--;
         } else if (this.mc.currentScreen == null) {
            for (BlockEntity block : Utils.blockEntities()) {
               if (((List)this.blocks.get()).contains(block.getType())
                  && !(this.mc.player.getEyePos().distanceTo(Vec3d.ofCenter(block.getPos())) >= (Double)this.range.get())) {
                  BlockPos pos = block.getPos();
                  if (!this.openedBlocks.containsKey(pos)) {
                     Runnable click = () -> this.mc
                        .interactionManager
                        .interactBlock(
                           this.mc.player,
                           Hand.MAIN_HAND,
                           new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false)
                        );
                     if ((Boolean)this.rotate.get()) {
                        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), click);
                     } else {
                        click.run();
                     }

                     this.markOpened(block, pos);
                     this.isPending = true;
                     this.stuckTimer = 0;
                     this.packetTimer = 0;
                     this.timer = (Integer)this.delay.get();
                     break;
                  }
               }
            }
         }
      }
   }

   private void markOpened(BlockEntity block, BlockPos pos) {
      this.openedBlocks.put(pos, System.currentTimeMillis());
      BlockState state = block.getCachedState();
      if (state.contains(ChestBlock.CHEST_TYPE)) {
         Direction direction = (Direction)state.get(ChestBlock.FACING);
         switch ((ChestType)state.get(ChestBlock.CHEST_TYPE)) {
            case LEFT:
               this.openedBlocks.put(pos.offset(direction.rotateYClockwise()), System.currentTimeMillis());
               break;
            case RIGHT:
               this.openedBlocks.put(pos.offset(direction.rotateYCounterclockwise()), System.currentTimeMillis());
         }
      }
   }

   private void forceClose() {
      if (this.mc.player != null) {
         this.mc.player.closeHandledScreen();
         if (this.mc.player.currentScreenHandler != null) {
            this.mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(this.mc.player.currentScreenHandler.syncId));
         }
      }

      this.resetState();
   }

   private void resetState() {
      this.isPending = false;
      this.stuckTimer = 0;
      this.packetTimer = 0;
   }

   private class PacketListener {
      @EventHandler(
         priority = 100
      )
      private void onPacket(Receive event) {
         if (AutoChestAura.this.isPending) {
            if (event.packet instanceof InventoryS2CPacket packet) {
               ScreenHandler handler = AutoChestAura.this.mc.player.currentScreenHandler;
               if (handler != null && packet.syncId() == handler.syncId) {
                  AutoChestAura.this.packetTimer = (Integer)AutoChestAura.this.waitTime.get();
               }
            } else if (event.packet instanceof OpenScreenS2CPacket packetx && AutoChestAura.this.packetTimer == 0) {
               AutoChestAura.this.packetTimer = (Integer)AutoChestAura.this.timeout.get() - 5;
            }
         }
      }
   }
}
