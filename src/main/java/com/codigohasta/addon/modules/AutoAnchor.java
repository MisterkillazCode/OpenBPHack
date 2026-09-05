package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.CombatUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Render3DUtil;
import com.codigohasta.addon.utils.leaveshack.events.RenderLeaves3DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class AutoAnchor extends Module {
   public static AutoAnchor INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("TargetRange")).description("Target Distance")).defaultValue(12.0).sliderRange(1.0, 20.0).build());
   private final Setting<Double> range = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("range")).description("Reach Distance")).defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("delay-ms"))
                  .description("Place Delay"))
               .defaultValue(50))
            .sliderRange(0, 500)
            .build()
      );
   private final Setting<Double> minDamage = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("MinDamage")).description("mostlittleDamage")).defaultValue(4.0).sliderRange(1.0, 36.0).build());
   private final Setting<Double> maxSelfDmg = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("MaxSelfDmg")).description("mostbigSelfHurt")).defaultValue(12.0).sliderRange(1.0, 36.0).build());
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
   private final Setting<Boolean> preferHead = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PreferHead"))
                  .description("Head"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> placeHelper = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PlaceHelper"))
                  .description("PutBlock"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> noSuicide = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("NoSuicide"))
                  .description("DefenseSelfkill"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("rotate"))
                  .description("Head"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> inventory = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("InventorySwap"))
                  .description("PackItemSwitch"))
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
                     .name("Shape Mode"))
                  .description("BlockRender Mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Line"))
               .description("BlockBoxColor"))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Side"))
               .description("BlockFill Color"))
            .defaultValue(new SettingColor(255, 255, 255, 10))
            .build()
      );
   private final Setting<Double> renderSpeed = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("RenderSpeed")).description("BlockRenderSpeed")).defaultValue(0.1).sliderRange(0.0, 1.0).build());
   public PlayerEntity target;
   public BlockPos currentPos;
   public int dmg;
   public final Timer placeTimer = new Timer();
   public AutoAnchor.PosEntry renderPosEntry = new AutoAnchor.PosEntry();

   public AutoAnchor() {
      super(AddonTemplate.CATEGORY, "AutoAnchor", "Automatically places and triggers anchors.");
      INSTANCE = this;
   }

   public void onDeactivate() {
      this.currentPos = null;
   }

   public void onActivate() {
      this.placeTimer.setMs(9999999L);
      this.renderPosEntry = new AutoAnchor.PosEntry();
   }

   public String getInfoString() {
      return this.target == null ? null : "§f[" + this.target.getName().getString() + "]";
   }

   @EventHandler
   private void onMyRender3D(RenderLeaves3DEvent event) {
      if ((Boolean)this.renderDmg.get() && this.currentPos != null) {
         Render3DUtil.renderText3D(this.dmg + "f", this.currentPos.toCenterPos(), ((SettingColor)this.dmgColor.get()).getPacked());
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.currentPos != null) {
         if (this.renderPosEntry.x == 0.0 && this.renderPosEntry.y == 0.0 && this.renderPosEntry.z == 0.0) {
            this.renderPosEntry.x = this.mc.player.getX();
            this.renderPosEntry.y = this.mc.player.getY();
            this.renderPosEntry.z = this.mc.player.getZ();
         }

         this.renderPosEntry.x = this.renderPosEntry.x + (this.currentPos.getX() - this.renderPosEntry.x) * (Double)this.renderSpeed.get();
         this.renderPosEntry.y = this.renderPosEntry.y + (this.currentPos.getY() - this.renderPosEntry.y) * (Double)this.renderSpeed.get();
         this.renderPosEntry.z = this.renderPosEntry.z + (this.currentPos.getZ() - this.renderPosEntry.z) * (Double)this.renderSpeed.get();
         Box renderBox = new Box(
            this.renderPosEntry.x,
            this.renderPosEntry.y,
            this.renderPosEntry.z,
            this.renderPosEntry.x + 1.0,
            this.renderPosEntry.y + 1.0,
            this.renderPosEntry.z + 1.0
         );
         event.renderer.box(renderBox, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
      } else {
         this.renderPosEntry = new AutoAnchor.PosEntry();
      }
   }

   @EventHandler
   public void onTick(Pre event) {
      this.target = CombatUtil.getClosestEnemy((Double)this.targetRange.get());
      if (this.target == null) {
         this.currentPos = null;
      } else if (!this.shouldPause()) {
         int anchor = this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.RESPAWN_ANCHOR) : InventoryUtil.findItem(Items.RESPAWN_ANCHOR);
         int glow = this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.GLOWSTONE) : InventoryUtil.findItem(Items.GLOWSTONE);
         if (anchor != -1 && glow != -1) {
            this.updatePos(this.target);
            if (this.placeTimer.passedMs((long)((Integer)this.delay.get()).intValue())) {
               this.doAnchor(anchor, glow);
            }
         } else {
            this.currentPos = null;
         }
      }
   }

   private void doAnchor(int anchor, int glow) {
      if (this.currentPos != null) {
         if ((Boolean)this.noSuicide.get()
            && DamageUtils.anchorDamage(this.mc.player, this.currentPos.toCenterPos()) > EntityUtils.getTotalHealth(this.mc.player)) {
            return;
         }

         if (this.mc.player.getEyePos().distanceTo(this.currentPos.toCenterPos()) > (Double)this.range.get()
            || !BlockUtil.canPlace(this.currentPos) && !(BlockUtil.getBlock(this.currentPos) instanceof RespawnAnchorBlock)) {
            this.updatePos(this.target);
         }

         if (!(BlockUtil.getBlock(this.currentPos) instanceof RespawnAnchorBlock)) {
            Direction side = BlockUtil.getPlaceSide(this.currentPos, null);
            if (side != null) {
               int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
               this.doSwap(anchor);
               BlockUtil.placeBlock(this.currentPos, side, (Boolean)this.rotate.get());
               if ((Boolean)this.inventory.get()) {
                  this.doSwap(anchor);
               } else {
                  this.doSwap(old);
               }

               this.placeTimer.reset();
            }
         } else if (BlockUtil.getBlock(this.currentPos) instanceof RespawnAnchorBlock) {
            int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            Direction side2 = BlockUtil.getClickSide(this.currentPos);
            if ((Integer)this.mc.world.getBlockState(this.currentPos).get(RespawnAnchorBlock.CHARGES) > 0) {
               BlockUtil.clickBlock(this.currentPos, side2, (Boolean)this.rotate.get());
               this.placeTimer.reset();
               return;
            }

            if (side2 != null) {
               this.doSwap(glow);
               BlockUtil.clickBlock(this.currentPos, side2, (Boolean)this.rotate.get());
               this.mc
                  .world
                  .playSound(
                     null,
                     this.mc.player.getX(),
                     this.mc.player.getY(),
                     this.mc.player.getZ(),
                     SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                     SoundCategory.AMBIENT,
                     5.0F,
                     1.0F
                  );
               if ((Boolean)this.inventory.get()) {
                  this.doSwap(glow);
               } else {
                  this.doSwap(old);
               }

               this.placeTimer.reset();
            }
         }
      }
   }

   private void updatePos(PlayerEntity target) {
      if ((Boolean)this.preferHead.get()) {
         BlockPos head = target.getBlockPos().up(2);
         if (DamageUtils.anchorDamage(target, head.toCenterPos()) > (Double)this.minDamage.get()) {
            if (BlockUtil.canPlace(head) || BlockUtil.getBlock(head) instanceof RespawnAnchorBlock) {
               this.currentPos = head;
               return;
            }

            if ((Boolean)this.placeHelper.get()) {
               for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                  BlockPos temp = head.offset(dir);
                  if (BlockUtil.canPlace(temp) && BlockUtil.isGrimDirection(temp.offset(dir), dir.getOpposite())) {
                     this.placeHelper(temp);
                     return;
                  }
               }
            }
         }
      }

      float bestDmg = Float.MIN_VALUE;
      BlockPos bestPos = null;

      for (BlockPos pos : BlockUtil.getSphere((Double)this.range.get())) {
         if ((BlockUtil.canPlace(pos) || BlockUtil.getBlock(pos) instanceof RespawnAnchorBlock)
            && DamageUtils.anchorDamage(target, pos.toCenterPos()) > bestDmg
            && DamageUtils.anchorDamage(target, pos.toCenterPos()) > (Double)this.minDamage.get()
            && DamageUtils.anchorDamage(this.mc.player, pos.toCenterPos()) < (Double)this.maxSelfDmg.get()) {
            bestDmg = DamageUtils.anchorDamage(target, pos.toCenterPos());
            bestPos = pos;
         }
      }

      if (bestPos != null) {
         this.currentPos = bestPos;
         this.dmg = (int)bestDmg;
      }
   }

   private void placeHelper(BlockPos pos) {
      Direction dir = BlockUtil.getPlaceSide(pos, null);
      if (dir != null) {
         int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
         int anchor = this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.RESPAWN_ANCHOR) : InventoryUtil.findItem(Items.RESPAWN_ANCHOR);
         this.doSwap(anchor);
         BlockUtil.placeBlock(pos, dir, (Boolean)this.rotate.get());
         if ((Boolean)this.inventory.get()) {
            this.doSwap(anchor);
         } else {
            this.doSwap(old);
         }
      }
   }

   private boolean shouldPause() {
      return AutoCrystal.INSTANCE.isActive() && AutoCrystal.INSTANCE.preferMode.get() == AutoCrystal.PreferMode.PreferCrystal
         ? AutoCrystal.INSTANCE.crystalPos != null
         : !(Boolean)this.usingPause.get() || this.checkPause((Boolean)this.onlyMain.get());
   }

   public boolean checkPause(boolean onlyMain) {
      return (this.mc.options.useKey.isPressed() || this.mc.player.isUsingItem())
         && (!onlyMain || this.mc.player.getActiveHand() == Hand.MAIN_HAND);
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

   public static class PosEntry {
      double x = 0.0;
      double y = 0.0;
      double z = 0.0;

      PosEntry() {
      }
   }
}
