package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.CombatUtil;
import com.codigohasta.addon.utils.leaveshack.DamageUtil;
import com.codigohasta.addon.utils.leaveshack.EntityUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Render3DUtil;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import com.codigohasta.addon.utils.leaveshack.events.RenderLeaves3DEvent;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoCrystal extends Module {
   public static AutoCrystal INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("TargetRange")).description("Target Distance")).defaultValue(12.0).sliderRange(1.0, 20.0).build());
   private final Setting<Double> placeRange = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("PlaceRange")).description("PutDistance")).defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Double> breakRange = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("BreakRange")).description("Distance")).defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Integer> placeDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("PlaceDelay"))
                  .description("Place Delay"))
               .defaultValue(50))
            .sliderRange(0, 500)
            .build()
      );
   private final Setting<Integer> breakDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("BreakDelay"))
                  .description("Delay"))
               .defaultValue(50))
            .sliderRange(0, 500)
            .build()
      );
   public final Setting<AutoCrystal.CalcMode> calcMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("CalcMode"))
                  .description("Mode"))
               .defaultValue(AutoCrystal.CalcMode.AlienV4))
            .build()
      );
   private final Setting<Double> minDamage = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("MinDamage")).description("mostlittleDamage")).defaultValue(4.0).sliderRange(1.0, 36.0).build());
   private final Setting<Double> maxSelfDmg = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("MaxSelfDmg")).description("mostbigSelfHurt")).defaultValue(12.0).sliderRange(1.0, 36.0).build());
   private final Setting<Integer> predict = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Predict"))
                  .description("Predict"))
               .defaultValue(4))
            .sliderRange(0, 12)
            .build()
      );
   public final Setting<AutoCrystal.PreferMode> preferMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("PreferMode"))
                  .description("Mode"))
               .defaultValue(AutoCrystal.PreferMode.PreferAnchor))
            .build()
      );
   private final Setting<Boolean> autoBase = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoBase"))
                  .description("Auto"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> baseDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("BaseDelay"))
                  .description("Delay"))
               .defaultValue(500))
            .sliderRange(0, 1000)
            .build()
      );
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("UsingPause"))
                  .description("UsePause"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> onlyMain = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("OnlyMain"))
                     .description("Hand"))
                  .defaultValue(true))
               .visible(this.usingPause::get))
            .build()
      );
   private final Setting<Boolean> noSuicide = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("NoSuicide"))
                  .description("DefenseStopSelfkill"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> inventory = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("InventorySwap"))
                  .description("SwitchPackItem"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> placeAfterBreak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PlaceAfterBreak"))
                  .description("afterPut"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> grimFix = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("GrimFix"))
                  .description("Stop sprint and pre-rotate to reduce Grim flags"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Render"))
                  .description("Render"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderDmg = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RenderDmg"))
                  .description("RenderDamage"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> dmgColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("DamageColor"))
               .description("DamageTextColor"))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("shape-mode"))
                  .description("Render Mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("side-color"))
               .description("BoxinsideColor"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("line-color"))
               .description("BoxColor"))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<Double> renderSpeed = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("RenderSpeed")).description("RenderSpeed")).defaultValue(0.05).sliderRange(0.0, 1.0).build());
   private final Setting<Double> renderH = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("RenderHeight")).description("RenderHeight")).defaultValue(0.1).sliderRange(0.0, 1.0).build());
   private final Timer placeTimer = new Timer();
   private final Timer breakTimer = new Timer();
   private final Timer baseTimer = new Timer();
   private boolean pendingPlace = false;
   private int pendingPlaceSlot = -1;
   private int dmg = 0;
   private PlayerEntity target;
   public BlockPos crystalPos;
   public BlockPos lastBestPos;
   private final AutoCrystal.RenderPos renderPos = new AutoCrystal.RenderPos();

   public AutoCrystal() {
      super(AddonTemplate.CATEGORY, "AutoCrystal", "Automatically places and attacks crystals.");
      INSTANCE = this;
   }

   public String getInfoString() {
      return this.target == null ? null : "§f[" + this.target.getName().getString() + "]";
   }

   public void onDeactivate() {
      this.crystalPos = null;
   }

   public void onActivate() {
      this.breakTimer.setMs(9999999L);
      this.breakTimer.setMs(9999999L);
      this.placeTimer.setMs(9999999L);
      this.renderPos.x = 0.0;
      this.renderPos.y = 0.0;
      this.renderPos.z = 0.0;
   }

   @EventHandler
   private void onRender(RenderLeaves3DEvent event) {
      if ((Boolean)this.renderDmg.get() && this.crystalPos != null) {
         Vec3d vec = new Vec3d(this.renderPos.x + 0.5, this.renderPos.y + (1.0 - (Double)this.renderH.get() / 2.0), this.renderPos.z + 0.5);
         Render3DUtil.renderText3D(this.dmg + "f", vec, ((SettingColor)this.dmgColor.get()).getPacked());
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.get()) {
         if (this.crystalPos == null) {
            this.renderPos.x = 0.0;
            this.renderPos.y = 0.0;
            this.renderPos.z = 0.0;
         } else {
            if (this.renderPos.x == 0.0 && this.renderPos.y == 0.0 && this.renderPos.z == 0.0) {
               this.renderPos.x = this.mc.player.getX();
               this.renderPos.y = this.mc.player.getY();
               this.renderPos.z = this.mc.player.getZ();
            }

            this.renderPos.x = this.renderPos.x + (this.crystalPos.getX() - this.renderPos.x) * (Double)this.renderSpeed.get();
            this.renderPos.y = this.renderPos.y + (this.crystalPos.getY() - 1 - this.renderPos.y) * (Double)this.renderSpeed.get();
            this.renderPos.z = this.renderPos.z + (this.crystalPos.getZ() - this.renderPos.z) * (Double)this.renderSpeed.get();
            Box box = new Box(
               this.renderPos.x,
               this.renderPos.y + (1.0 - (Double)this.renderH.get()),
               this.renderPos.z,
               this.renderPos.x + 1.0,
               this.renderPos.y + 1.0,
               this.renderPos.z + 1.0
            );
            event.renderer.box(box, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.crystalPos != null && !PlayerUtils.isWithin(this.crystalPos.toCenterPos(), (Double)this.placeRange.get())) {
            this.crystalPos = null;
            this.lastBestPos = null;
         }

         if (this.crystalPos != null && !PlayerUtils.isWithin(this.crystalPos.toCenterPos(), (Double)this.breakRange.get())) {
            this.crystalPos = null;
            this.lastBestPos = null;
         }

         if (this.crystalPos != null
            && (
               BlockUtil.getBlock(this.crystalPos.down()) != Blocks.OBSIDIAN
                     && BlockUtil.getBlock(this.crystalPos.down()) != Blocks.BEDROCK
                  || !this.mc.world.isAir(this.crystalPos)
            )) {
            this.crystalPos = null;
            this.lastBestPos = null;
         }

         this.target = CombatUtil.getClosestEnemy((Double)this.targetRange.get());
         if (this.target == null) {
            this.crystalPos = null;
            this.lastBestPos = null;
         } else if (!this.shouldPause()) {
            int crystalSlot = this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
            if (crystalSlot == -1) {
               this.crystalPos = null;
            } else if (this.pendingPlace) {
               this.pendingPlace = false;
               if (this.crystalPos != null && this.pendingPlaceSlot != -1 && !BlockUtil.hasCrystal(this.crystalPos)) {
                  this.placeCrystal(this.crystalPos, this.pendingPlaceSlot, true);
                  this.baseTimer.reset();
                  this.placeTimer.reset();
               }

               this.pendingPlaceSlot = -1;
            } else if (this.breakTimer.passedMs((long)((Integer)this.breakDelay.get()).intValue()) && this.breakCrystal(crystalSlot)) {
               this.breakTimer.reset();
            } else if (!this.findBestPos()) {
               if (this.crystalPos != null
                  && this.placeTimer.passedMs((long)((Integer)this.placeDelay.get()).intValue())
                  && !BlockUtil.hasCrystal(this.crystalPos)) {
                  this.placeCrystal(this.crystalPos, crystalSlot, true);
                  this.baseTimer.reset();
                  this.placeTimer.reset();
               }
            }
         }
      }
   }

   private boolean findBestPos() {
      PlayerEntity predictTarget = this.predictTarget(this.target);
      float bestDamage = 0.0F;
      BlockPos best = null;
      ArrayList<BlockPos> placeList = new ArrayList<>();

      for (BlockPos pos : BlockUtil.getSphere((Double)this.placeRange.get())) {
         if ((Boolean)this.autoBase.get()
            && !BlockUtil.canPlaceCrystal(pos)
            && BlockUtil.canPlace(pos.down())
            && !BlockUtil.hasEntity(pos, true)
            && this.mc.world.isAir(pos)
            && pos.getY() <= this.target.getY()
            && this.baseTimer.passedMs((long)((Integer)this.baseDelay.get()).intValue())) {
            placeList.add(pos);
         }

         if (BlockUtil.canPlaceCrystal(pos) || BlockUtil.hasCrystalPlace(pos) && this.mc.world.isAir(pos)) {
            placeList.add(pos);
         }
      }

      if (placeList.isEmpty()) {
         return false;
      } else {
         for (BlockPos pos : placeList) {
            if ((Boolean)this.autoBase.get()) {
               CombatUtil.modifyPos = pos.down();
               CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
            }

            Vec3d vec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            float dmg = this.calcMode.get() == AutoCrystal.CalcMode.Meteor
               ? DamageUtils.crystalDamage(predictTarget, vec, false, pos.down())
               : DamageUtil.calculateDamage(pos, predictTarget);
            float self = this.calcMode.get() == AutoCrystal.CalcMode.Meteor
               ? DamageUtils.crystalDamage(this.mc.player, vec, false, pos.down())
               : DamageUtil.calculateDamage(pos, this.mc.player);
            if ((Boolean)this.autoBase.get()) {
               CombatUtil.modifyPos = null;
            }

            if (!(dmg < (Double)this.minDamage.get())
               && !(self > (Double)this.maxSelfDmg.get())
               && (!(Boolean)this.noSuicide.get() || !(self > EntityUtils.getTotalHealth(this.mc.player)))
               && dmg > bestDamage) {
               bestDamage = dmg;
               best = pos;
            }
         }

         if (this.lastBestPos != null) {
            if ((Boolean)this.autoBase.get()) {
               CombatUtil.modifyPos = this.lastBestPos.down();
               CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
            }

            Vec3d vecx = new Vec3d(this.lastBestPos.getX() + 0.5, this.lastBestPos.getY(), this.lastBestPos.getZ() + 0.5);
            float last = this.calcMode.get() == AutoCrystal.CalcMode.Meteor
               ? DamageUtils.crystalDamage(predictTarget, vecx, false, this.lastBestPos.down())
               : DamageUtil.calculateDamage(this.lastBestPos, predictTarget);
            float lastSelf = this.calcMode.get() == AutoCrystal.CalcMode.Meteor
               ? DamageUtils.crystalDamage(this.mc.player, vecx, false, this.lastBestPos.down())
               : DamageUtil.calculateDamage(this.lastBestPos, this.mc.player);
            if ((Boolean)this.autoBase.get()) {
               CombatUtil.modifyPos = null;
            }

            if (best != null
               && last >= bestDamage * 0.95
               && lastSelf < (Double)this.maxSelfDmg.get()
               && (!(Boolean)this.noSuicide.get() || lastSelf < EntityUtils.getTotalHealth(this.mc.player))) {
               this.crystalPos = this.lastBestPos;
               this.dmg = (int)last;
               return false;
            }

            if (best == null && last >= (Double)this.minDamage.get()) {
               this.crystalPos = this.lastBestPos;
               this.dmg = (int)last;
               return false;
            }
         }

         if (best != null
            && (Boolean)this.autoBase.get()
            && BlockUtil.canPlace(best.down())
            && this.baseTimer.passedMs((long)((Integer)this.baseDelay.get()).intValue())
            && this.doBase(best.down())) {
            this.lastBestPos = best;
            this.crystalPos = best;
            this.dmg = (int)bestDamage;
            return true;
         } else {
            this.lastBestPos = best;
            this.crystalPos = best;
            this.dmg = (int)bestDamage;
            return false;
         }
      }
   }

   private PlayerEntity predictTarget(PlayerEntity target) {
      if ((Integer)this.predict.get() <= 0) {
         return target;
      } else {
         int ticks = (Integer)this.predict.get();
         Vec3d vel = target.getVelocity();
         double predictX = target.getX() + vel.x * ticks;
         double predictY = target.getY() + vel.y * ticks;
         double predictZ = target.getZ() + vel.z * ticks;
         OtherClientPlayerEntity fake = new OtherClientPlayerEntity(this.mc.world, new GameProfile(UUID.randomUUID(), "Predict"));
         fake.refreshPositionAndAngles(predictX, predictY, predictZ, target.getYaw(), target.getPitch());
         fake.setBodyYaw(target.bodyYaw);
         fake.setHeadYaw(target.headYaw);
         fake.setPose(target.getPose());
         fake.setOnGround(target.isOnGround());
         fake.setVelocity(target.getVelocity());
         fake.getAttributes().setFrom(target.getAttributes());
         fake.setHealth(target.getHealth());

         for (StatusEffectInstance se : target.getStatusEffects()) {
            fake.addStatusEffect(new StatusEffectInstance(se));
         }

         fake.getInventory().clone(target.getInventory());
         fake.calculateDimensions();
         return fake;
      }
   }

   private boolean doBase(BlockPos pos) {
      if ((Boolean)this.grimFix.get() && !this.mc.player.isOnGround()) {
         return false;
      } else {
         int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
         if (old < 0 || old > 8) {
            old = 0;
         }

         int obsSlot = this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.OBSIDIAN) : InventoryUtil.findItem(Items.OBSIDIAN);
         Direction side = BlockUtil.getPlaceSide(pos, null);
         if (obsSlot != -1 && side != null) {
            this.doSwap(obsSlot);
            BlockUtil.placeBlock(pos, side, true);
            if ((Boolean)this.inventory.get()) {
               this.doSwap(obsSlot);
            } else {
               this.doSwap(old);
            }

            this.breakTimer.reset();
            return true;
         } else {
            return false;
         }
      }
   }

   private void placeCrystal(BlockPos pos, int slot, boolean rotate) {
      if (!(Boolean)this.grimFix.get() || this.mc.player.isOnGround()) {
         BlockPos base = pos.down();
         Direction side = BlockUtil.getClickSide(base);
         if (side != null) {
            int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            if (old < 0 || old > 8) {
               old = 0;
            }

            boolean holdItem = this.checkItem(old);
            if (!holdItem) {
               this.doSwap(slot);
            }

            Vec3d clickVec = new Vec3d(
               base.getX() + 0.5 + side.getVector().getX() * 0.5,
               base.getY() + 0.5 + side.getVector().getY() * 0.5,
               base.getZ() + 0.5 + side.getVector().getZ() * 0.5
            );
            if ((Boolean)this.grimFix.get()) {
               this.stopSprint();
               if (rotate) {
                  this.grimRotateTo(clickVec);
               }

               this.grimClickBlock(base, side, clickVec);
            } else {
               BlockUtil.clickBlock(base, side, rotate);
            }

            if ((Boolean)this.inventory.get() && !holdItem) {
               this.doSwap(slot);
            } else {
               this.doSwap(old);
            }
         }
      }
   }

   private boolean checkItem(int slot) {
      return slot >= 0 && slot <= 8 ? this.mc.player.getInventory().getStack(slot).getItem() == Items.END_CRYSTAL : false;
   }

   private boolean shouldPause() {
      return this.preferMode.get() == AutoCrystal.PreferMode.PreferAnchor
         ? AutoAnchor.INSTANCE.currentPos != null
         : !(Boolean)this.usingPause.get() || this.checkPause((Boolean)this.onlyMain.get());
   }

   public boolean checkPause(boolean onlyMain) {
      return (this.mc.options.useKey.isPressed() || this.mc.player.isUsingItem())
         && (!onlyMain || this.mc.player.getActiveHand() == Hand.MAIN_HAND);
   }

   private boolean breakCrystal(int crystalSlot) {
      for (Entity entity : this.mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity crystal
            && CombatUtil.isValid(entity, (Double)this.breakRange.get())
            && (!(Boolean)this.grimFix.get() || this.mc.player.isOnGround())) {
            PlayerEntity predictTarget = this.predictTarget(this.target);
            Vec3d crystalVec = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
            float damage = this.calcMode.get() == AutoCrystal.CalcMode.Meteor
               ? DamageUtils.crystalDamage(predictTarget, crystalVec, false, crystal.getBlockPos().down())
               : DamageUtil.calculateDamage(crystalVec, predictTarget);
            float self = this.calcMode.get() == AutoCrystal.CalcMode.Meteor
               ? DamageUtils.crystalDamage(this.mc.player, crystalVec, false, crystal.getBlockPos().down())
               : DamageUtil.calculateDamage(crystalVec, this.mc.player);
            if (!(damage < (Double)this.minDamage.get())
               && !(self > (Double)this.maxSelfDmg.get())
               && (!(Boolean)this.noSuicide.get() || !(self > EntityUtils.getTotalHealth(this.mc.player)))) {
               if ((Boolean)this.grimFix.get()) {
                  this.stopSprint();
                  Vec3d aim = new Vec3d(crystal.getX(), crystal.getY() + 0.25, crystal.getZ());
                  this.grimRotateTo(aim);
                  this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, this.mc.player.isSneaking()));
                  EntityUtil.attackSwingHand();
               } else {
                  CombatUtil.attackCrystal(crystal, true, false);
               }

               if (this.crystalPos != null && (Boolean)this.placeAfterBreak.get() && BlockUtil.hasCrystal(this.crystalPos)) {
                  this.pendingPlace = true;
                  this.pendingPlaceSlot = crystalSlot;
               }

               return true;
            }
         }
      }

      return false;
   }

   private void stopSprint() {
      if (this.mc.player.isSprinting()) {
         this.mc.player.setSprinting(false);
      }
   }

   private void grimRotateTo(Vec3d target) {
      float[] rot = Rotation.getRotation(target);
      float targetYaw = rot[0];
      float targetPitch = MathHelper.clamp(rot[1], -90.0F, 90.0F);
      float currentYaw = this.mc.player.getYaw();
      float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
      float sendYaw = currentYaw + deltaYaw;
      this.mc.getNetworkHandler().sendPacket(new LookAndOnGround(sendYaw, targetPitch, this.mc.player.isOnGround(), this.mc.player.horizontalCollision));
   }

   private void grimClickBlock(BlockPos pos, Direction side, Vec3d clickVec) {
      BlockHitResult result = new BlockHitResult(clickVec, side, pos, false);
      PendingUpdateManager mgr = this.mc.world.getPendingUpdateManager().incrementSequence();

      try {
         this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, mgr.getSequence()));
      } catch (Throwable var9) {
         if (mgr != null) {
            try {
               mgr.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (mgr != null) {
         mgr.close();
      }

      EntityUtil.placeSwingHand();
   }

   private void doSwap(int slot) {
      if (slot != -1) {
         if (!(Boolean)this.inventory.get()) {
            InventoryUtil.switchToSlot(slot);
         } else {
            InventoryUtil.inventorySwap(slot, ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot());
         }
      }
   }

   public static enum CalcMode {
      AlienV4,
      Meteor;
   }

   public static enum PreferMode {
      PreferCrystal,
      PreferAnchor;
   }

   private static class RenderPos {
      double x;
      double y;
      double z;
   }
}
