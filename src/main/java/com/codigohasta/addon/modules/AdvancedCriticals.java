package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import java.lang.reflect.Field;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class AdvancedCriticals extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AdvancedCriticals.Mode> mode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("mode")).description("Alien 'sSendPackMode.")).defaultValue(AdvancedCriticals.Mode.Vanilla)).build()
      );
   private final Setting<Boolean> noCrystal = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("no-crystal"))
                     .description("SwapModedownnotAttackWater."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == AdvancedCriticals.Mode.Swap))
            .build()
      );
   private final Setting<Boolean> inventorySwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("inventory-swap"))
                     .description("fromPackinHeavy(notarefast)."))
                  .defaultValue(false))
               .visible(() -> this.mode.get() == AdvancedCriticals.Mode.Swap))
            .build()
      );
   private final Setting<Boolean> onlyGround = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("only-ground"))
                     .description("atGroundFaceFlyTimeTrigger."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == AdvancedCriticals.Mode.Vanilla))
            .build()
      );
   private final Setting<Double> height = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("height"))
                  .description("BuildHeight. Alien Default 25, mosthigh 2000."))
               .defaultValue(25.0)
               .min(0.0)
               .max(2000.0)
               .visible(() -> this.mode.get() == AdvancedCriticals.Mode.Vanilla))
            .build()
      );
   private boolean ignore = false;
   private PlayerInteractEntityC2SPacket lastPacket = null;
   private static Field onGroundField;

   public AdvancedCriticals() {
      super(AddonTemplate.CATEGORY, "AdvancedCriticals", "Manual heavy-effect criticals; you must land the hit yourself.");

      try {
         for (Field field : PlayerMoveC2SPacket.class.getDeclaredFields()) {
            if (field.getType() == boolean.class && (field.getName().equals("onGround") || field.getName().equals("sidesToUpgrade"))) {
               field.setAccessible(true);
               onGroundField = field;
               break;
            }
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   @EventHandler(
      priority = 200
   )
   public void onPacketSend(Send event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mode.get() == AdvancedCriticals.Mode.Vanilla) {
            if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
               if (!this.mc.player.getMainHandStack().getItem().toString().contains("mace")) {
                  return;
               }

               if (!this.isAttack(packet)) {
                  return;
               }

               Entity entity = this.getEntity(packet);
               if (entity instanceof EndCrystalEntity) {
                  return;
               }

               if ((Boolean)this.onlyGround.get() && !this.mc.player.isOnGround() && !this.mc.player.getAbilities().flying) {
                  return;
               }

               if (this.mc.player.isInLava() || this.mc.player.isSubmergedInWater()) {
                  return;
               }

               if (entity == null) {
                  return;
               }

               for (int i = 0; i < 4; i++) {
                  this.sendFakeY(0.0);
               }

               this.sendFakeY((Double)this.height.get());
               this.sendFakeY(0.0);
            }
         } else if (this.mode.get() == AdvancedCriticals.Mode.NCP) {
            if (event.packet instanceof PlayerMoveC2SPacket && onGroundField != null) {
               try {
                  onGroundField.setBoolean(event.packet, false);
               } catch (IllegalAccessException var5) {
                  var5.printStackTrace();
               }
            }
         } else if (this.mode.get() == AdvancedCriticals.Mode.Swap) {
            if (event.isCancelled()) {
               return;
            }

            int slot = this.getMaceSlot();
            if (slot == -1) {
               return;
            }

            if (this.ignore) {
               return;
            }

            if (event.packet instanceof PlayerInteractEntityC2SPacket packet && this.isAttack(packet)) {
               if ((Boolean)this.noCrystal.get() && this.getEntity(packet) instanceof EndCrystalEntity) {
                  return;
               }

               this.lastPacket = packet;
               this.ignore = true;
               this.doSpoof();
               this.ignore = false;
               event.cancel();
            }
         }
      }
   }

   private void doSpoof() {
      if (this.lastPacket != null) {
         int slot = this.getMaceSlot();
         if (slot != -1) {
            int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            this.doSwap(slot);
            this.mc.getNetworkHandler().sendPacket(this.lastPacket);
            if ((Boolean)this.inventorySwap.get()) {
               this.doSwap(slot);
            } else {
               this.doSwap(old);
            }
         }
      }
   }

   private void doSwap(int slot) {
      if ((Boolean)this.inventorySwap.get()) {
         InvUtils.swap(slot, true);
      } else {
         InvUtils.swap(slot, false);
      }
   }

   private int getMaceSlot() {
      FindItemResult result;
      if ((Boolean)this.inventorySwap.get()) {
         result = InvUtils.find(itemStack -> itemStack.getItem() == Items.MACE);
      } else {
         result = InvUtils.findInHotbar(new Item[]{Items.MACE});
      }

      return result.found() ? result.slot() : -1;
   }

   private void sendFakeY(double offset) {
      PlayerMoveC2SPacket packet = new PositionAndOnGround(
         this.mc.player.getX(), this.mc.player.getY() + offset, this.mc.player.getZ(), false, this.mc.player.horizontalCollision
      );
      ((IPlayerMoveC2SPacket)packet).meteor$setTag(1337);
      this.mc.getNetworkHandler().sendPacket(packet);
   }

   private Entity getEntity(PlayerInteractEntityC2SPacket packet) {
      return ((IPlayerInteractEntityC2SPacket)packet).meteor$getEntity();
   }

   private boolean isAttack(PlayerInteractEntityC2SPacket packet) {
      IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket)packet;
      return String.valueOf(accessor.meteor$getType()).equals("ATTACK");
   }

   public static enum Mode {
      Vanilla,
      NCP,
      Swap;
   }
}
