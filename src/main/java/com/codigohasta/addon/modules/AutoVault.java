package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.VaultBlock;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AutoVault extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoVault.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Manual: RightKeyTrigger / Auto: lookAutoOpen"))
               .defaultValue(AutoVault.Mode.Manual))
            .build()
      );
   private final Setting<AutoVault.SwapMode> swapMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("SwapMode")).description("Inventory: Switch / Hotbar: Switchfast"))
               .defaultValue(AutoVault.SwapMode.Inventory))
            .build()
      );
   private final Setting<Boolean> inventorySwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("InventorySwap"))
                  .description("AllowfromPack"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Range"))
               .description("mostbigReach Distance"))
            .defaultValue(5.0)
            .min(1.0)
            .max(10.0)
            .sliderRange(1.0, 10.0)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("AutoModeOperationBetween(ms)"))
               .defaultValue(200))
            .min(0)
            .max(1000)
            .sliderRange(0, 1000)
            .build()
      );
   private final Timer timer = new Timer();
   private boolean manualUsed = false;

   public AutoVault() {
      super(AddonTemplate.CATEGORY, "AutoVault", "Auto-switches to the vault item on right-click.");
   }

   public void onActivate() {
      this.timer.setMs(99999L);
      this.manualUsed = false;
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.currentScreen == null) {
            if (this.mc.crosshairTarget instanceof BlockHitResult hitResult) {
               BlockPos var12 = hitResult.getBlockPos();
               Block block = this.mc.world.getBlockState(var12).getBlock();
               if (block == Blocks.VAULT) {
                  if (!(this.mc.player.getEyePos().squaredDistanceTo(var12.toCenterPos()) > (Double)this.range.get() * (Double)this.range.get())) {
                     if (this.mode.get() == AutoVault.Mode.Manual) {
                        if (!this.mc.options.useKey.isPressed()) {
                           this.manualUsed = false;
                           return;
                        }

                        if (this.manualUsed) {
                           return;
                        }

                        this.manualUsed = true;
                     } else if (!this.timer.passedMs((long)((Integer)this.delay.get()).intValue())) {
                        return;
                     }

                     boolean ominous = (Boolean)this.mc.world.getBlockState(var12).get(VaultBlock.OMINOUS);
                     Item targetKey = ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
                     boolean keyInHand = this.mc.player.getMainHandStack().getItem() == targetKey;
                     int hotbarSlot = -1;
                     int invSlot = -1;
                     if (!keyInHand) {
                        hotbarSlot = InventoryUtil.findItem(targetKey);
                        if (hotbarSlot == -1 && (Boolean)this.inventorySwap.get()) {
                           if (this.mc.player.getOffHandStack().getItem() == targetKey) {
                              keyInHand = true;
                           } else {
                              invSlot = InventoryUtil.findItemInventorySlot(targetKey);
                           }
                        }
                     }

                     if (keyInHand || hotbarSlot != -1 || invSlot != -1) {
                        int oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                        if (!keyInHand) {
                           if (this.swapMode.get() == AutoVault.SwapMode.Hotbar || hotbarSlot != -1) {
                              int slot = hotbarSlot != -1 ? hotbarSlot : invSlot;
                              if (slot < 0 || slot > 8) {
                                 return;
                              }

                              InventoryUtil.switchToSlot(slot);
                           } else if (invSlot != -1) {
                              InventoryUtil.inventorySwap(invSlot, oldSlot);
                           }
                        }

                        this.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, id));
                        if (!keyInHand) {
                           if (this.swapMode.get() == AutoVault.SwapMode.Hotbar) {
                              if (hotbarSlot != -1) {
                                 InventoryUtil.switchToSlot(oldSlot);
                              }
                           } else if (invSlot != -1) {
                              InventoryUtil.inventorySwap(invSlot, oldSlot);
                              this.mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(this.mc.player.currentScreenHandler.syncId));
                           }
                        }

                        this.timer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   private void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (this.mc.getNetworkHandler() != null && this.mc.world != null) {
         PendingUpdateManager pendingUpdateManager = this.mc.world.getPendingUpdateManager().incrementSequence();

         try {
            int i = pendingUpdateManager.getSequence();
            this.mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
         } catch (Throwable var6) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      }
   }

   public static enum Mode {
      Manual,
      Auto;
   }

   public static enum SwapMode {
      Inventory,
      Hotbar;
   }
}
