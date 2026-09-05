package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MaceDMGPlus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgExploit = this.settings.createGroup("instakillMechanism (Dragon Palm)");
   private final SettingGroup sgTargeting = this.settings.createGroup("TargetSetting");
   private final SettingGroup sgWhitelist = this.settings.createGroup("WhiteNameSingle/BlackNameSingle");
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("CheckTestRange")).description("AutoAttackenemy'smostbigDistance."))
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(7.0)
            .build()
      );
   private final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoSwitch"))
                  .description("AttackTimeAutoSwitchtoHeavy (Mace)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> silentSwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Switch"))
                     .description("UseDataPackSwitchWeapon(noMove, noSound), OtherPlayermoreHard."))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.autoSwitch.get()))
            .build()
      );
   private final Setting<Integer> attackDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Attack Delay"))
                  .description("AutoAttack'sBetween (Tick). Suggest15Right, toofastCancanby."))
               .defaultValue(15))
            .min(0)
            .sliderRange(0, 40)
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoAim"))
                  .description("AttackTimeForceViewTarget."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> preventDeath = this.sgExploit
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DefenseFalldieProtection"))
                  .description("DefenseStopBuildHeightFalldie (RecommendEnable)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> maxPower = this.sgExploit
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("mostbigPower-come (Paper/Spigot)"))
                  .description("Simfrom170highAirDrop. atServicenotneedEnable."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> fallHeight = this.sgExploit
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("BuildHeight"))
                     .description("Build'sDropHeight. Default22atGrounddownalsocanUsenotby."))
                  .defaultValue(22))
               .sliderRange(1, 170)
               .min(1)
               .max(170)
               .visible(() -> !(Boolean)this.maxPower.get()))
            .build()
      );
   private final Setting<Boolean> airCheck = this.sgExploit
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AirAirCheck (DefenseRebound)"))
                  .description("Enable: CheckHeadwhetherhaveAirAir(, DefenseAntiDo). Disable: noViewGroundForceSendPack(Power, Canat2highUse)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
                  .name("MobList"))
               .description("SelectyouwantAttack'sMob."))
            .onlyAttackable()
            .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.ZOMBIE, EntityType.SKELETON})
            .build()
      );
   private final Setting<Boolean> players = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AttackPlayer"))
                  .description("whetherAttackPlayer."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> throughWalls = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Attack"))
                  .description("noViewDirectAttack."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreNamed = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("LifeNameMob"))
                  .description("notAttackhaveNameMark'sMob."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreTamed = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Thing"))
                  .description("notAttackAlreadybyService'sMob."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("friend"))
                  .description("EnableafternotfriendForAttack Target"))
               .defaultValue(false))
            .build()
      );
   private final Setting<MaceDMGPlus.ListMode> listMode = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Mode"))
                  .description(
                     "NameSinglepastMode. WhiteNameSingle=OnlyHitNameSingleinside'sPlayer,notatNameSinglenotHit; BlackNameSingle=notHitNameSingleinside'sPlayer, notatNameSingle'sHit."
                  ))
               .defaultValue(MaceDMGPlus.ListMode.Off))
            .build()
      );
   private final Setting<String> playerList = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("PlayerNameSingle"))
                     .description("PlayerNameList, UseText(,)."))
                  .defaultValue("Player1,Player2"))
               .visible(() -> this.listMode.get() != MaceDMGPlus.ListMode.Off))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RenderTarget"))
                  .description("SystemTargetBox."))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("BoxMode"))
                  .defaultValue(ShapeMode.Lines))
               .visible(this.render::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Fill Color"))
               .defaultValue(new SettingColor(255, 0, 0, 40))
               .visible(this.render::get))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("LineColor"))
               .defaultValue(new SettingColor(255, 0, 0, 255))
               .visible(this.render::get))
            .build()
      );
   private final SettingGroup sgTotem = this.settings.createGroup("Imagepast");
   private final Setting<Boolean> totemBypass = this.sgTotem
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Imagepast"))
                  .description("manyTimesAttackImageInvincible"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> totemAttacks = this.sgTotem
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("AttackTimesNumber"))
                     .description("AttackTimesNumber(1-3)"))
                  .defaultValue(2))
               .min(1)
               .max(3)
               .sliderRange(1, 3)
               .visible(() -> (Boolean)this.totemBypass.get()))
            .build()
      );
   private final Setting<Integer> totemHeightIncrease = this.sgTotem
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("BuildHeight"))
                     .description("TimesoutsideAttack'sBuildDropHeight()"))
                  .defaultValue(10))
               .min(1)
               .sliderRange(1, 50)
               .visible(() -> (Boolean)this.totemBypass.get()))
            .build()
      );
   private int timer;
   private int originalSlot = -1;
   private int silentSwapSlot = -1;
   private int silentSwapPrevSlot = -1;
   private final List<Entity> targets = new ArrayList<>();
   private Entity currentTarget;
   private Vec3d previouspos;
   private boolean isSendingTotem = false;

   public MaceDMGPlus() {
      super(AddonTemplate.CATEGORY, "MaceDMGPlus", "Maximises mace smash damage by controlling fall height and swapping automatically. Currently no effect.");
   }

   public void onActivate() {
      this.timer = 0;
      this.originalSlot = -1;
      this.silentSwapSlot = -1;
      this.silentSwapPrevSlot = -1;
      this.targets.clear();
      this.currentTarget = null;
   }

   public void onDeactivate() {
      if (this.silentSwapSlot != -1 && this.mc.player != null) {
         this.swapBackWeapon();
      }

      if (this.originalSlot != -1 && (Boolean)this.autoSwitch.get() && !(Boolean)this.silentSwap.get() && this.mc.player != null) {
         InvUtils.swap(this.originalSlot, false);
         this.originalSlot = -1;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.timer > 0) {
            this.timer--;
         } else {
            boolean holdingMace = this.mc.player.getMainHandStack().getItem().toString().contains("mace");
            if ((Boolean)this.autoSwitch.get()) {
               if (!holdingMace && !this.checkAndSwapWeapon()) {
                  return;
               }
            } else if (!holdingMace) {
               return;
            }

            this.targets.clear();
            TargetUtils.getList(this.targets, this::entityCheck, SortPriority.ClosestAngle, 1);
            if (this.targets.isEmpty()) {
               this.currentTarget = null;
               this.swapBackWeapon();
            } else {
               this.currentTarget = this.targets.get(0);
               if ((Boolean)this.rotate.get()) {
                  Rotations.rotate(Rotations.getYaw(this.currentTarget), Rotations.getPitch(this.currentTarget));
               }

               if ((Boolean)this.totemBypass.get()) {
                  this.isSendingTotem = true;

                  try {
                     this.performTotemBypass(this.currentTarget);
                  } finally {
                     this.isSendingTotem = false;
                  }
               } else {
                  this.performMaceExploit(this.currentTarget);
                  this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.currentTarget, this.mc.player.isSneaking()));
                  this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               }

               this.swapBackWeapon();
               this.timer = (Integer)this.attackDelay.get();
            }
         }
      }
   }

   @EventHandler
   private void onSendPacket(Send event) {
      if (this.mc.player != null) {
         if (this.mc.player.getMainHandStack().getItem() == Items.MACE) {
            if (event.packet instanceof PlayerInteractEntityC2SPacket) {
               if (String.valueOf(((IPlayerInteractEntityC2SPacket)event.packet).meteor$getType()).equals("ATTACK")) {
                  Entity target = ((IPlayerInteractEntityC2SPacket)event.packet).meteor$getEntity();
                  if (target != null && this.entityCheck(target)) {
                     if (!this.isSendingTotem) {
                        if ((Boolean)this.totemBypass.get()) {
                           event.cancel();
                           this.isSendingTotem = true;

                           try {
                              this.performTotemBypass(target);
                           } finally {
                              this.isSendingTotem = false;
                           }
                        } else {
                           this.performMaceExploit(target);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void performTotemBypass(Entity target) {
      int baseHeight = this.maxPower.get() ? 170 : (Integer)this.fallHeight.get();
      int count = (Integer)this.totemAttacks.get();

      for (int i = 0; i < count; i++) {
         int height = baseHeight + (i > 0 ? (Integer)this.totemHeightIncrease.get() * i : 0);
         if ((Boolean)this.airCheck.get()) {
            int maxAir = this.getMaxHeightAbovePlayer();
            if (maxAir <= 0) {
               break;
            }

            height = Math.min(height, maxAir);
         }

         this.sendExploitPackets(height);
         this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
         this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }
   }

   private void performMaceExploit(Entity target) {
      int blocks;
      if ((Boolean)this.airCheck.get()) {
         blocks = this.getMaxHeightAbovePlayer();
         BlockPos isopenair1 = this.mc.player.getBlockPos().add(0, blocks, 0);
         BlockPos isopenair2 = this.mc.player.getBlockPos().add(0, blocks + 1, 0);
         if (!this.isSafeBlock(isopenair1) || !this.isSafeBlock(isopenair2)) {
            return;
         }
      } else {
         blocks = this.maxPower.get() ? 170 : (Integer)this.fallHeight.get();
      }

      if (blocks > 0) {
         this.sendExploitPackets(blocks);
      }
   }

   private void sendExploitPackets(int blocks) {
      this.previouspos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      int packetsRequired = (int)Math.ceil(Math.abs(blocks / 10.0));
      if (packetsRequired > 20) {
         packetsRequired = 1;
      }

      if (blocks <= 22) {
         if (this.mc.player.hasVehicle()) {
            for (int i = 0; i < 4; i++) {
               this.mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(this.mc.player.getVehicle()));
            }

            double maxHeight = Math.min(this.mc.player.getVehicle().getY() + 22.0, this.mc.player.getVehicle().getY() + blocks);
            this.doVehicleTeleports(maxHeight, blocks);
         } else {
            for (int i = 0; i < 4; i++) {
               this.mc.player.networkHandler.sendPacket(new OnGroundOnly(false, this.mc.player.horizontalCollision));
            }

            double heightY = Math.min(this.mc.player.getY() + 22.0, this.mc.player.getY() + blocks);
            this.doPlayerTeleports(heightY);
         }
      } else if (this.mc.player.hasVehicle()) {
         for (int packetNumber = 0; packetNumber < packetsRequired - 1; packetNumber++) {
            this.mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(this.mc.player.getVehicle()));
         }

         double maxHeight = this.mc.player.getVehicle().getY() + blocks;
         this.doVehicleTeleports(maxHeight, blocks);
      } else {
         for (int i = 0; i < packetsRequired - 1; i++) {
            this.mc.player.networkHandler.sendPacket(new OnGroundOnly(false, this.mc.player.horizontalCollision));
         }

         double heightY = this.mc.player.getY() + blocks;
         this.doPlayerTeleports(heightY);
      }
   }

   private void doPlayerTeleports(double height) {
      PlayerMoveC2SPacket movepacket = new PositionAndOnGround(
         this.mc.player.getX(), height, this.mc.player.getZ(), false, this.mc.player.horizontalCollision
      );
      PlayerMoveC2SPacket homepacket = new PositionAndOnGround(
         this.previouspos.getX(), this.previouspos.getY(), this.previouspos.getZ(), false, this.mc.player.horizontalCollision
      );
      if ((Boolean)this.preventDeath.get()) {
         homepacket = new PositionAndOnGround(
            this.previouspos.getX(), this.previouspos.getY() + 0.25, this.previouspos.getZ(), false, this.mc.player.horizontalCollision
         );
      }

      ((IPlayerMoveC2SPacket)homepacket).meteor$setTag(1337);
      ((IPlayerMoveC2SPacket)movepacket).meteor$setTag(1337);
      this.mc.player.networkHandler.sendPacket(movepacket);
      this.mc.player.networkHandler.sendPacket(homepacket);
      if ((Boolean)this.preventDeath.get()) {
         this.mc.player.setVelocity(this.mc.player.getVelocity().x, 0.1, this.mc.player.getVelocity().z);
         this.mc.player.fallDistance = 0.0;
      }
   }

   private void doVehicleTeleports(double height, int blocks) {
      if (this.mc.player.getVehicle() != null) {
         this.mc
            .player
            .getVehicle()
            .setPosition(this.mc.player.getVehicle().getX(), height + blocks, this.mc.player.getVehicle().getZ());
         this.mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(this.mc.player.getVehicle()));
         this.mc.player.getVehicle().setPosition(this.previouspos);
         this.mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(this.mc.player.getVehicle()));
      }
   }

   private boolean checkAndSwapWeapon() {
      if (this.mc.player.getMainHandStack().getItem().toString().contains("mace")) {
         return true;
      } else {
         if ((Boolean)this.silentSwap.get()) {
            int slot = InventoryUtil.findItemInventorySlot(Items.MACE);
            if (slot != -1) {
               this.silentSwapSlot = slot;
               this.silentSwapPrevSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               if (slot >= 36) {
                  InventoryUtil.switchToSlot(slot - 36);
               } else {
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.SWAP, this.mc.player);
                  InventoryUtil.switchToSlot(0);
               }

               return true;
            }
         } else {
            FindItemResult mace = InvUtils.find(itemStack -> itemStack.getItem().toString().contains("mace"), 0, 8);
            if (mace.found()) {
               if (this.originalSlot == -1) {
                  this.originalSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               }

               InvUtils.swap(mace.slot(), false);
               return true;
            }
         }

         return false;
      }
   }

   private void swapBackWeapon() {
      if (this.silentSwapSlot != -1) {
         if (this.silentSwapSlot >= 36) {
            InventoryUtil.switchToSlot(this.silentSwapPrevSlot);
         } else {
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, this.silentSwapSlot, 0, SlotActionType.SWAP, this.mc.player);
            InventoryUtil.switchToSlot(this.silentSwapPrevSlot);
            this.mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.mc.player.currentScreenHandler.syncId));
         }

         this.silentSwapSlot = -1;
         this.silentSwapPrevSlot = -1;
      }
   }

   private int getMaxHeightAbovePlayer() {
      BlockPos playerPos = this.mc.player.getBlockPos();
      int maxHeight = playerPos.getY() + (this.maxPower.get() ? 170 : (Integer)this.fallHeight.get());

      for (int i = maxHeight; i > playerPos.getY(); i--) {
         BlockPos up1 = new BlockPos(playerPos.getX(), i, playerPos.getZ());
         BlockPos up2 = up1.up(1);
         if (this.isSafeBlock(up1) && this.isSafeBlock(up2)) {
            return i - playerPos.getY();
         }
      }

      return 0;
   }

   private boolean isSafeBlock(BlockPos pos) {
      return this.mc.world.getBlockState(pos).isReplaceable()
         && this.mc.world.getFluidState(pos).isEmpty()
         && !this.mc.world.getBlockState(pos).isOf(Blocks.POWDER_SNOW);
   }

   private boolean entityCheck(Entity entity) {
      if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
         return false;
      } else if (entity == this.mc.player) {
         return false;
      } else if (this.mc.player.distanceTo(entity) > (Double)this.range.get()) {
         return false;
      } else if (!(Boolean)this.throughWalls.get() && !this.mc.player.canSee(entity)) {
         return false;
      } else {
         if (entity instanceof PlayerEntity p) {
            if (!(Boolean)this.players.get()) {
               return false;
            }

            if (p.isCreative()) {
               return false;
            }

            if ((Boolean)this.ignoreFriends.get() && Friends.get().isFriend(p)) {
               return false;
            }

            if (!Friends.get().shouldAttack(p)) {
               return false;
            }

            String name = p.getName().getString();
            List<String> list = Arrays.stream(((String)this.playerList.get()).split(","))
               .map(String::trim)
               .filter(s -> !s.isEmpty())
               .collect(Collectors.toList());
            if (this.listMode.get() == MaceDMGPlus.ListMode.Whitelist && !list.contains(name)) {
               return false;
            }

            if (this.listMode.get() == MaceDMGPlus.ListMode.Blacklist && list.contains(name)) {
               return false;
            }
         }

         if ((Boolean)this.ignoreNamed.get() && entity.hasCustomName()) {
            return false;
         } else {
            return this.ignoreTamed.get() && entity instanceof TameableEntity t && t.isTamed()
               ? false
               : ((Set)this.entities.get()).contains(entity.getType());
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if ((Boolean)this.render.get() && this.currentTarget != null) {
         event.renderer.box(this.currentTarget.getBoundingBox(), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
      }
   }

   public static enum ListMode {
      Whitelist,
      Blacklist,
      Off;
   }
}
