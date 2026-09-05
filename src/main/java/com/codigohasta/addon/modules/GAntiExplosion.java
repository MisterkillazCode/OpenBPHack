package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class GAntiExplosion extends Module {
   private final SettingGroup sgCrystal = this.settings.createGroup("Anti Crystal");
   private final SettingGroup sgAnchor = this.settings.createGroup("Anti Anchor");
   private final SettingGroup sgVClip = this.settings.createGroup("VClip Exploit Options");
   private final Setting<Boolean> antiCrystal = this.sgCrystal
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("enable-anti-crystal")).description("Prevents end crystal explosion damage via VClip."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> crystalScanBorder = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("crystal-scan-border"))
                  .description("The radius to search for a safe spot."))
               .defaultValue(8))
            .min(1)
            .sliderMax(15)
            .build()
      );
   private final Setting<Double> crystalHitRange = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("crystal-hit-range"))
               .description("Maximum distance you can hit the crystal from the safe spot."))
            .defaultValue(6.0)
            .min(0.1)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> crystalMaxDamage = this.sgCrystal
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("crystal-max-damage"))
               .description("If predicted damage is above this, the exploit will trigger."))
            .defaultValue(6.0)
            .min(0.01)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Boolean> antiAnchor = this.sgAnchor
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("enable-anti-anchor")).description("Prevents respawn anchor explosion damage via VClip."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> anchorScanBorder = this.sgAnchor
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("anchor-scan-border"))
                  .description("The radius to search for a safe spot."))
               .defaultValue(8))
            .min(1)
            .sliderMax(15)
            .build()
      );
   private final Setting<Double> anchorClickRange = this.sgAnchor
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("anchor-click-range"))
               .description("Maximum distance to click the anchor from the safe spot."))
            .defaultValue(6.0)
            .min(0.1)
            .sliderMax(6.0)
            .build()
      );
   private final Setting<Double> anchorMaxDamage = this.sgAnchor
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("anchor-max-damage"))
               .description("If predicted damage is above this, the exploit will trigger."))
            .defaultValue(6.0)
            .min(0.01)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Double> vClipStep = this.sgVClip
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("vclip-step-distance"))
               .description("Distance per spoofed movement packet (bypass anti-cheat)."))
            .defaultValue(8.0)
            .min(1.0)
            .sliderMax(10.0)
            .build()
      );
   private final Setting<Integer> packetLimit = this.sgVClip
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("max-packets"))
                  .description("Maximum movement packets allowed per exploit (prevents kicks)."))
               .defaultValue(40))
            .min(10)
            .sliderMax(100)
            .build()
      );
   private final Setting<Boolean> returnBack = this.sgVClip
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("return-to-original-pos"))
                  .description("Teleports you back to your real position after the explosion."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> notifyLimit = this.sgVClip
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("notify-limit-reached")).description("Prints a chat message if packet limit is exceeded."))
               .defaultValue(true))
            .build()
      );

   public GAntiExplosion() {
      super(
         AddonTemplate.CATEGORY, "GAntiExplosion", "Reduces explosion damage from crystals and anchors by pre-emptively moving you. Currently has no effect."
      );
   }

   @EventHandler
   private void onReceivePacket(Receive event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (event.packet instanceof EntitySpawnS2CPacket packet && (Boolean)this.antiCrystal.get()) {
            if (packet.getEntityType() == EntityType.END_CRYSTAL) {
               this.mc.execute(() -> this.handleCrystalExploit(packet));
            }
         } else if (event.packet instanceof BlockUpdateS2CPacket packetx && (Boolean)this.antiAnchor.get()) {
            BlockState state = packetx.getState();
            if (state.getBlock() == Blocks.RESPAWN_ANCHOR) {
               int charges = (Integer)state.get(RespawnAnchorBlock.CHARGES);
               if (charges == 0) {
                  this.mc.execute(() -> this.handleAnchorExploit(packet));
               }
            }
         }
      }
   }

   private void handleCrystalExploit(EntitySpawnS2CPacket packet) {
      Vec3d crystalPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
      Vec3d currentPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      float currentDamage = this.calculateDamageAt(currentPos, crystalPos);
      if (!(currentDamage <= (Double)this.crystalMaxDamage.get())) {
         Vec3d safePos = this.findSafePosForCrystal(crystalPos);
         if (safePos != null) {
            EndCrystalEntity dummyCrystal = new EndCrystalEntity(this.mc.world, crystalPos.x, crystalPos.y, crystalPos.z);
            dummyCrystal.setId(packet.getEntityId());
            List<Vec3d> pathToSafe = this.buildVClipPath(currentPos, safePos, (Double)this.vClipStep.get());
            List<Vec3d> pathBack = (List<Vec3d>)(this.returnBack.get()
               ? this.buildVClipPath(safePos, currentPos, (Double)this.vClipStep.get())
               : new ArrayList<>());
            int totalPackets = pathToSafe.size() + pathBack.size() + 2;
            if (totalPackets > (Integer)this.packetLimit.get()) {
               if ((Boolean)this.notifyLimit.get()) {
                  ChatUtils.warning("§7[GAntiExplosion] TP packet limit exceeded! Aborting.", new Object[0]);
               }
            } else {
               this.sendVClipPath(pathToSafe);
               this.sendPositionPacket(safePos);
               PlayerInteractEntityC2SPacket attackPacket = PlayerInteractEntityC2SPacket.attack(dummyCrystal, this.mc.player.isSneaking());
               this.mc.getNetworkHandler().sendPacket(attackPacket);
               this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               if ((Boolean)this.returnBack.get()) {
                  this.sendVClipPath(pathBack);
                  this.sendPositionPacket(currentPos);
               } else {
                  this.mc.player.setPosition(safePos);
               }

               ChatUtils.info("§a[GAntiExplosion] VClip Crystal Exploit Successful!", new Object[0]);
            }
         }
      }
   }

   private void handleAnchorExploit(BlockUpdateS2CPacket packet) {
      BlockPos anchorPos = packet.getPos();
      Vec3d anchorCenter = anchorPos.toCenterPos();
      FindItemResult glowstoneResult = InvUtils.findInHotbar(itemStack -> itemStack.getItem().toString().contains("glowstone"));
      if (!glowstoneResult.found()) {
         ChatUtils.warning("§c[GAntiExplosion] Aborted: fastno!", new Object[0]);
      } else {
         FindItemResult igniteItemResult = InvUtils.findInHotbar(itemStack -> !itemStack.getItem().toString().contains("glowstone"));
         if (igniteItemResult.found()) {
            Vec3d currentPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
            float currentDamage = this.calculateDamageAt(currentPos, anchorCenter);
            if (!(currentDamage <= (Double)this.anchorMaxDamage.get())) {
               Vec3d safePos = this.findSafePosForAnchor(anchorPos);
               if (safePos != null) {
                  List<Vec3d> pathToSafe = this.buildVClipPath(currentPos, safePos, (Double)this.vClipStep.get());
                  List<Vec3d> pathBack = (List<Vec3d>)(this.returnBack.get()
                     ? this.buildVClipPath(safePos, currentPos, (Double)this.vClipStep.get())
                     : new ArrayList<>());
                  int totalPackets = pathToSafe.size() + pathBack.size() + 4;
                  if (totalPackets > (Integer)this.packetLimit.get()) {
                     if ((Boolean)this.notifyLimit.get()) {
                        ChatUtils.warning("§7[GAntiExplosion] TP packet limit exceeded! Aborting.", new Object[0]);
                     }
                  } else {
                     this.sendVClipPath(pathToSafe);
                     this.sendPositionPacket(safePos);
                     this.swapAndInteract(glowstoneResult.slot(), anchorPos);
                     this.swapAndInteract(igniteItemResult.slot(), anchorPos);
                     if ((Boolean)this.returnBack.get()) {
                        this.sendVClipPath(pathBack);
                        this.sendPositionPacket(currentPos);
                     } else {
                        this.mc.player.setPosition(safePos);
                     }

                     ChatUtils.info("§a[GAntiExplosion] VClip Anchor Exploit Successful!", new Object[0]);
                  }
               }
            }
         }
      }
   }

   private List<Vec3d> buildVClipPath(Vec3d start, Vec3d end, double step) {
      List<Vec3d> path = new ArrayList<>();
      double distance = start.distanceTo(end);
      int segments = (int)Math.ceil(distance / step);

      for (int i = 1; i < segments; i++) {
         double percent = (double)i / segments;
         path.add(start.lerp(end, percent));
      }

      return path;
   }

   private void sendVClipPath(List<Vec3d> path) {
      for (Vec3d pos : path) {
         this.sendPositionPacket(pos);
      }
   }

   private void sendPositionPacket(Vec3d pos) {
      PlayerMoveC2SPacket packet = new PositionAndOnGround(pos.x, pos.y, pos.z, this.mc.player.isOnGround(), false);
      this.mc.getNetworkHandler().sendPacket(packet);
   }

   private void swapAndInteract(int slot, BlockPos pos) {
      if (this.mc.player != null && this.mc.interactionManager != null) {
         InventoryAccessor accessor = (InventoryAccessor)this.mc.player.getInventory();
         int prevSlot = accessor.getSelectedSlot();
         accessor.setSelectedSlot(slot);
         this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         BlockHitResult hitResult = new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false);
         this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hitResult);
         this.mc.player.swingHand(Hand.MAIN_HAND);
         accessor.setSelectedSlot(prevSlot);
         this.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
      }
   }

   private float calculateDamageAt(Vec3d simulatedPlayerPos, Vec3d explosionPos) {
      Box originalBox = this.mc.player.getBoundingBox();
      Vec3d originalPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());

      float var6;
      try {
         this.mc.player.setPosition(simulatedPlayerPos);
         this.mc.player.setBoundingBox(originalBox.offset(simulatedPlayerPos.subtract(originalPos)));
         return DamageUtils.crystalDamage(this.mc.player, explosionPos);
      } catch (Exception var10) {
         var6 = 100.0F;
      } finally {
         this.mc.player.setPosition(originalPos);
         this.mc.player.setBoundingBox(originalBox);
      }

      return var6;
   }

   private Vec3d findSafePosForCrystal(Vec3d crystalPos) {
      int border = (Integer)this.crystalScanBorder.get();
      double maxDist = (Double)this.crystalHitRange.get();
      Vec3d bestPos = null;
      float lowestDamage = Float.MAX_VALUE;

      for (int x = -border; x <= border; x++) {
         for (int y = -border; y <= border; y++) {
            for (int z = -border; z <= border; z++) {
               Vec3d testPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ())
                  .add(x, y, z);
               if (!(testPos.distanceTo(crystalPos) > maxDist)) {
                  BlockPos testBlockPos = BlockPos.ofFloored(testPos);
                  if (this.mc.world.getBlockState(testBlockPos).getCollisionShape(this.mc.world, testBlockPos).isEmpty()) {
                     float dmg = this.calculateDamageAt(testPos, crystalPos);
                     if (dmg < lowestDamage) {
                        lowestDamage = dmg;
                        bestPos = testPos;
                     }
                  }
               }
            }
         }
      }

      if (bestPos != null && lowestDamage > (Double)this.crystalMaxDamage.get()) {
         ChatUtils.warning("§c[GAntiExplosion] PutDefense: Explosionpointto" + String.format("%.1f", lowestDamage) + "Damage.", new Object[0]);
         return null;
      } else {
         return bestPos;
      }
   }

   private Vec3d findSafePosForAnchor(BlockPos anchorPos) {
      Vec3d anchorCenter = anchorPos.toCenterPos();
      int border = (Integer)this.anchorScanBorder.get();
      double maxDist = (Double)this.anchorClickRange.get();
      Vec3d bestPos = null;
      float lowestDamage = Float.MAX_VALUE;

      for (int x = -border; x <= border; x++) {
         for (int y = -border; y <= border; y++) {
            for (int z = -border; z <= border; z++) {
               Vec3d testPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ())
                  .add(x, y, z);
               if (!(testPos.distanceTo(anchorCenter) > maxDist)) {
                  BlockPos testBlockPos = BlockPos.ofFloored(testPos);
                  if (this.mc.world.getBlockState(testBlockPos).getCollisionShape(this.mc.world, testBlockPos).isEmpty()) {
                     float dmg = this.calculateDamageAt(testPos, anchorCenter);
                     if (dmg < lowestDamage) {
                        lowestDamage = dmg;
                        bestPos = testPos;
                     }
                  }
               }
            }
         }
      }

      if (bestPos != null && lowestDamage > (Double)this.anchorMaxDamage.get()) {
         ChatUtils.warning("§c[GAntiExplosion] PutDefense: HeavySpawnExplosionpointto" + String.format("%.1f", lowestDamage) + "Damage.", new Object[0]);
         return null;
      } else {
         return bestPos;
      }
   }
}
