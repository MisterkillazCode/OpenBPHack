package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TpAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTiming = this.settings.createGroup("AttackMechanism");
   private final SettingGroup sgTP = this.settings.createGroup("Strike");
   private final SettingGroup sgTargeting = this.settings.createGroup("Target");
   private final SettingGroup sgWhitelist = this.settings.createGroup("WhiteNameSingle");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<TpAura.AttackMode> attackMode = this.sgTiming
      .add(((Builder)((Builder)new Builder().name("AttackMode")).defaultValue(TpAura.AttackMode.Smart)).build());
   private final Setting<Double> cooldownThreshold = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("PowerThreshold"))
                  .description("1.0ForDamage"))
               .defaultValue(1.0)
               .min(0.1)
               .sliderMax(1.0)
               .visible(() -> this.attackMode.get() == TpAura.AttackMode.Smart))
            .build()
      );
   private final Setting<Integer> attackDelay = this.sgTiming
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("outsideDelay(Tick)"))
               .defaultValue(0))
            .min(0)
            .build()
      );
   private final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AutoSwitchWeapon"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> requireMace = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("HandHoldHeavy"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> swingHand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Hand"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> silentSwap = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Switch"))
                     .description("UseDataPackSwitchWeapon(noMove, noSound), OtherPlayermoreHard. SwitchTimewillatClientDisplayWeaponImageMark."))
                  .defaultValue(true))
               .visible(() -> (Boolean)this.autoSwitch.get()))
            .build()
      );
   private final Setting<TpAura.Mode> mode = this.sgTP.add(((Builder)((Builder)new Builder().name("TolerateMode")).defaultValue(TpAura.Mode.Paper)).build());
   private final Setting<Double> maxRange = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("mostbigRange"))
            .defaultValue(49.0)
            .min(1.0)
            .sliderMax(99.0)
            .build()
      );
   private final Setting<Boolean> goUp = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("V-Clip"))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == TpAura.Mode.Paper))
            .build()
      );
   private final Setting<Integer> paperPackets = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("PackAmount"))
               .defaultValue(8))
            .min(1)
            .sliderMax(20)
            .build()
      );
   private final Setting<Boolean> returnPos = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Attackafter"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> offsetFix = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Move"))
                  .description("SendlittleMovePackDefenseStopPull, Cancan"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
                  .name("Target Entity"))
               .defaultValue(Collections.singleton(EntityType.PLAYER)))
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("friend"))
                  .defaultValue(false))
               .description("EnableafternotfriendForAttack Target"))
            .build()
      );
   private final Setting<Boolean> ignoreNamed = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("LifeName"))
                  .defaultValue(true))
               .description("EnableafternotLifeNameEntityForAttack Target"))
            .build()
      );
   private final Setting<Boolean> ignoreTamed = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Service"))
                  .defaultValue(false))
               .description("EnableafternotService'sMobForAttack Target"))
            .build()
      );
   private final Setting<TpAura.ListMode> listMode = this.sgWhitelist
      .add(((Builder)((Builder)new Builder().name("NameSingleMode")).defaultValue(TpAura.ListMode.Off)).build());
   private final Setting<String> playerList = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                  .name("Player List"))
               .defaultValue(""))
            .build()
      );
   private final Setting<Boolean> renderPath = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("DisplayPath"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> pathColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(255, 0, 0, 100))
            .build()
      );
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Target Color"))
            .defaultValue(new SettingColor(255, 0, 0, 200))
            .build()
      );
   private final SettingGroup sgTotem = this.settings.createGroup("Imagepast");
   private final Setting<Boolean> totemBypass = this.sgTotem
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Imagepast"))
                  .description("manyTimesAttackImageInvincible, PaperModehaveEffect"))
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
                        .name("Height"))
                     .description("TimesoutsideAttack'sdownHeight"))
                  .defaultValue(9))
               .min(1)
               .sliderRange(1, 100)
               .visible(() -> (Boolean)this.totemBypass.get()))
            .build()
      );
   private final List<Entity> targets = new ArrayList<>();
   private final List<Vec3d> renderPathNodes = new ArrayList<>();
   private Entity currentTarget;
   private int originalSlot = -1;
   private int silentSwapSlot = -1;
   private int silentSwapPrevSlot = -1;
   private int delayTimer = 0;

   public TpAura() {
      super(AddonTemplate.CATEGORY, "TpAura", "Teleports to targets and attacks.");
   }

   public void onActivate() {
      this.originalSlot = -1;
      this.silentSwapSlot = -1;
      this.silentSwapPrevSlot = -1;
      this.delayTimer = 0;
      this.renderPathNodes.clear();
   }

   public void onDeactivate() {
      if (this.silentSwapSlot != -1 && this.mc.player != null) {
         this.swapBackWeapon();
      }

      if (this.originalSlot != -1 && (Boolean)this.autoSwitch.get() && !(Boolean)this.silentSwap.get() && this.mc.player != null) {
         ((InventoryAccessor)this.mc.player.getInventory()).setSelectedSlot(this.originalSlot);
         this.originalSlot = -1;
      }
   }

   private int findWeaponInventorySlot() {
      for (int i = 0; i < 45; i++) {
         String name = this.mc.player.getInventory().getStack(i).getItem().toString().toLowerCase();
         if (name.contains("sword") || name.contains("mace") || name.contains("axe")) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   private boolean checkAndSwapWeapon() {
      String itemMain = this.mc.player.getMainHandStack().getItem().toString().toLowerCase();
      boolean isWeapon = itemMain.contains("sword") || itemMain.contains("mace") || itemMain.contains("axe");
      if (!isWeapon || (Boolean)this.requireMace.get() && !itemMain.contains("mace")) {
         if ((Boolean)this.silentSwap.get()) {
            int slot = this.findWeaponInventorySlot();
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
            FindItemResult weapon = InvUtils.find(s -> {
               String name = s.getItem().toString().toLowerCase();
               return name.contains("sword") || name.contains("mace") || name.contains("axe");
            }, 0, 8);
            if (weapon.found()) {
               if (this.originalSlot == -1) {
                  this.originalSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               }

               InvUtils.swap(weapon.slot(), false);
               return true;
            }
         }

         return false;
      } else {
         return true;
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

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!(Boolean)this.autoSwitch.get() || this.checkAndSwapWeapon()) {
            if (this.attackMode.get() != TpAura.AttackMode.Smart || !(this.mc.player.getAttackCooldownProgress(0.5F) < (Double)this.cooldownThreshold.get())) {
               if (this.delayTimer > 0) {
                  this.delayTimer--;
                  this.swapBackWeapon();
               } else {
                  this.targets.clear();
                  TargetUtils.getList(this.targets, this::entityCheck, SortPriority.LowestDistance, 1);
                  if (this.targets.isEmpty()) {
                     this.currentTarget = null;
                     this.swapBackWeapon();
                  } else {
                     this.currentTarget = this.targets.get(0);
                     this.executeTrouserAttack(this.currentTarget);
                     this.swapBackWeapon();
                     this.delayTimer = (Integer)this.attackDelay.get();
                  }
               }
            }
         }
      }
   }

   private void executeTrouserAttack(Entity target) {
      Vec3d startPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
      double reach = (Double)this.maxRange.get();
      Vec3d finalPos = !this.invalid(targetPos) ? targetPos : this.findNearestPos(targetPos);
      if (finalPos != null) {
         Vec3d highStart = startPos.add(0.0, reach, 0.0);
         Vec3d highTarget = finalPos.add(0.0, reach, 0.0);
         this.renderPathNodes.clear();
         this.renderPathNodes.add(startPos);
         if (this.mode.get() == TpAura.Mode.Paper && (Boolean)this.goUp.get()) {
            this.renderPathNodes.add(highStart);
            this.renderPathNodes.add(highTarget);
         }

         this.renderPathNodes.add(finalPos);
         int spam = this.mode.get() == TpAura.Mode.Paper ? (Integer)this.paperPackets.get() : 4;

         for (int i = 0; i < spam; i++) {
            this.mc.player.networkHandler.sendPacket(new OnGroundOnly(false, this.mc.player.horizontalCollision));
         }

         boolean totemMode = (Boolean)this.totemBypass.get() && this.mode.get() == TpAura.Mode.Paper;
         if (totemMode) {
            int attackCount = (Integer)this.totemAttacks.get();
            int currentHeight = (int)reach;

            for (int i = 0; i < attackCount; i++) {
               int blocks = i == 0 ? (int)reach : currentHeight;
               if (this.mc.world != null) {
                  int worldTop = this.mc.world.getTopYInclusive() - 1;
                  if (finalPos.y + blocks > worldTop) {
                     blocks = (int)(worldTop - finalPos.y);
                     if (blocks < 1) {
                        break;
                     }
                  }
               }

               Vec3d progressiveAbove = finalPos.add(0.0, blocks, 0.0);
               if ((Boolean)this.goUp.get()) {
                  this.sendMove(progressiveAbove);
               }

               this.sendMove(finalPos);
               if ((Boolean)this.swingHand.get()) {
                  this.mc.player.swingHand(Hand.MAIN_HAND);
               }

               this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
               currentHeight += this.totemHeightIncrease.get();
            }
         } else {
            if (this.mode.get() == TpAura.Mode.Paper && (Boolean)this.goUp.get()) {
               this.sendMove(highStart);
               this.sendMove(highTarget);
            }

            this.sendMove(finalPos);
            if ((Boolean)this.swingHand.get()) {
               this.mc.player.swingHand(Hand.MAIN_HAND);
            }

            this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
         }

         if ((Boolean)this.returnPos.get()) {
            if (this.mode.get() == TpAura.Mode.Paper && (Boolean)this.goUp.get() && !totemMode) {
               this.sendMove(highTarget);
               this.sendMove(highStart);
            }

            this.sendMove(startPos);
            if ((Boolean)this.offsetFix.get()) {
               Vec3d offset = this.getOffset(startPos);
               this.sendMove(offset);
               this.mc.player.setPosition(offset.x, offset.y, offset.z);
            } else {
               this.mc.player.setPosition(startPos.x, startPos.y, startPos.z);
            }
         } else if ((Boolean)this.offsetFix.get()) {
            Vec3d offset = this.getOffset(finalPos);
            this.sendMove(offset);
            this.mc.player.setPosition(offset.x, offset.y, offset.z);
         } else {
            this.mc.player.setPosition(finalPos.x, finalPos.y, finalPos.z);
         }
      }
   }

   private void sendMove(Vec3d pos) {
      PlayerMoveC2SPacket packet = new PositionAndOnGround(pos.x, pos.y, pos.z, false, false);
      ((IPlayerMoveC2SPacket)packet).meteor$setTag(1337);
      this.mc.player.networkHandler.sendPacket(packet);
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.currentTarget != null) {
         event.renderer.box(this.currentTarget.getBoundingBox(), (Color)this.targetColor.get(), (Color)this.targetColor.get(), ShapeMode.Lines, 0);
      }

      if ((Boolean)this.renderPath.get() && !this.renderPathNodes.isEmpty()) {
         for (int i = 0; i < this.renderPathNodes.size() - 1; i++) {
            Vec3d n1 = this.renderPathNodes.get(i);
            Vec3d n2 = this.renderPathNodes.get(i + 1);
            event.renderer
               .line(n1.x, n1.y + 1.0, n1.z, n2.x, n2.y + 1.0, n2.z, (Color)this.pathColor.get());
            event.renderer
               .box(
                  new Box(n1.x - 0.2, n1.y, n1.z - 0.2, n1.x + 0.2, n1.y + 2.0, n1.z + 0.2),
                  (Color)this.pathColor.get(),
                  (Color)this.pathColor.get(),
                  ShapeMode.Lines,
                  0
               );
         }
      }
   }

   private Vec3d getOffset(Vec3d base) {
      double dx = 0.05;
      double dy = 0.01;
      List<Vec3d> offsets = Arrays.asList(
         base.add(dx, dy, 0.0), base.add(-dx, dy, 0.0), base.add(0.0, dy, dx), base.add(0.0, dy, -dx)
      );
      Collections.shuffle(offsets);

      for (Vec3d pos : offsets) {
         if (!this.invalid(pos)) {
            return pos;
         }
      }

      return base.add(0.0, dy, 0.0);
   }

   private boolean invalid(Vec3d pos) {
      if (this.mc.world == null) {
         return true;
      } else {
         BlockPos bp = BlockPos.ofFloored(pos.x, pos.y, pos.z);
         if (this.mc.world.getChunk(bp.getX() >> 4, bp.getZ() >> 4) == null) {
            return true;
         } else {
            Box box = this.mc
               .player
               .getBoundingBox()
               .offset(pos.subtract(new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ())));

            for (BlockPos bPos : BlockPos.iterate(
               BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
            )) {
               BlockState state = this.mc.world.getBlockState(bPos);
               if (!state.getCollisionShape(this.mc.world, bPos).isEmpty() || state.isOf(Blocks.LAVA)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private Vec3d findNearestPos(Vec3d desired) {
      for (int dy = 0; dy <= 2; dy++) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               Vec3d test = desired.add(dx, dy, dz);
               if (!this.invalid(test)) {
                  return test;
               }
            }
         }
      }

      return null;
   }

   private boolean entityCheck(Entity entity) {
      if (entity instanceof LivingEntity && entity.isAlive() && entity != this.mc.player) {
         if (!((Set)this.entities.get()).contains(entity.getType())) {
            return false;
         } else if (this.mc.player.distanceTo(entity) > (Double)this.maxRange.get()) {
            return false;
         } else if ((Boolean)this.ignoreFriends.get() && entity instanceof PlayerEntity p && Friends.get().isFriend(p)) {
            return false;
         } else if ((Boolean)this.ignoreNamed.get() && entity.hasCustomName()) {
            return false;
         } else if ((Boolean)this.ignoreTamed.get() && entity instanceof TameableEntity tameable && tameable.isTamed()) {
            return false;
         } else {
            if (entity instanceof PlayerEntity p) {
               if (p.isCreative() || p.isSpectator()) {
                  return false;
               }

               if (!Friends.get().shouldAttack(p)) {
                  return false;
               }

               String name = p.getName().getString();
               List<String> list = Arrays.stream(((String)this.playerList.get()).split(",")).map(String::trim).collect(Collectors.toList());
               if (this.listMode.get() == TpAura.ListMode.Whitelist && !list.contains(name)) {
                  return false;
               }

               if (this.listMode.get() == TpAura.ListMode.Blacklist && list.contains(name)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public String getInfoString() {
      return this.currentTarget != null ? EntityUtils.getName(this.currentTarget) : null;
   }

   public static enum AttackMode {
      Smart("PowerHeavyStrike"),
      Fast("0PowerHit");

      private final String title;

      private AttackMode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }

   public static enum ListMode {
      Whitelist,
      Blacklist,
      Off;
   }

   public static enum Mode {
      Vanilla,
      Paper;
   }
}
