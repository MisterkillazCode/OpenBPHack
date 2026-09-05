package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class TpMachineGun extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgWeapon = this.settings.createGroup("WeaponSendSetting (Weapon)");
   private final SettingGroup sgTp = this.settings.createGroup("MoveSetting (TP Options)");
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting (Render)");
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("Target Entity")).description("SelectwantShootStrike'sEntityType."))
            .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER})
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("EnemyRange"))
               .description("mostbigTarget'sDistance."))
            .defaultValue(30.0)
            .min(1.0)
            .sliderMax(100.0)
            .build()
      );
   private final Setting<Boolean> waitForHurtTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("WaitInvincible (DefenseArrow)"))
                  .description("MakeWeaponAlreadyup, alsowillWaitTargetRedColorStrikeMoveendafterMoveOpen."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SortPriority> sortPriority = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("TargetPriority"))
                  .description("SelectTarget'sPriorityDirection."))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Integer> delay = this.sgWeapon
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ShootStrikeCooldownDelay"))
                  .description("TimesOpenafterWait's Tick Amount. littleShootfast, ToleratebyServerPack."))
               .defaultValue(3))
            .min(0)
            .sliderMax(10)
            .build()
      );
   private final Setting<Integer> tolerance = this.sgWeapon
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Pull/Load Tolerance"))
                  .description("ForDefenseStopDelaynotPull, outsidemanyPull's Tick Number."))
               .defaultValue(4))
            .min(0)
            .sliderMax(10)
            .build()
      );
   private final Setting<Double> moveDistance = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("Movelong (DefensePull)"))
               .description("PackTimeTimesSwitch'smostbigDistance, UseAntiDoSpeedCheckTest."))
            .defaultValue(8.0)
            .min(1.0)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Integer> limitPacket = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("mostbigSendPackLimitSystem"))
                  .description("SingleTimesAttackAllowSend'smostbigPackAmount, pastDisappearAttackDefenseKick."))
               .defaultValue(50))
            .min(10)
            .sliderMax(200)
            .build()
      );
   private final Setting<Boolean> back = this.sgTp
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("MoveBit (Blink Back)"))
                  .description("ShootStrikeafterBetweenBit, RealStrike."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("RenderPath"))
                  .description("atinRenderyourPathMove."))
               .defaultValue(true))
            .build()
      );
   private int timer = 0;
   private Entity target;
   private final List<Vec3d> renderPath = new ArrayList<>();

   public TpMachineGun() {
      super(AddonTemplate.CATEGORY, "TpMachineGun", "Teleports to targets and machine-gun attacks them. Incomplete (gcore).");
   }

   public void onActivate() {
      this.timer = 0;
   }

   public void onDeactivate() {
      this.mc.options.useKey.setPressed(false);
      if (this.mc.player != null) {
         this.mc.interactionManager.stopUsingItem(this.mc.player);
      }
   }

   public String getInfoString() {
      return this.target != null ? EntityUtils.getName(this.target) : null;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.timer > 0) {
            this.timer--;
            this.mc.options.useKey.setPressed(false);
         } else {
            Hand hand = Hand.MAIN_HAND;
            ItemStack stack = this.mc.player.getMainHandStack();
            if (!this.isWeapon(stack.getItem())) {
               stack = this.mc.player.getOffHandStack();
               hand = Hand.OFF_HAND;
            }

            if (!this.isWeapon(stack.getItem())) {
               this.target = null;
               this.mc.options.useKey.setPressed(false);
            } else {
               this.updateTarget();
               if (this.target == null) {
                  this.mc.options.useKey.setPressed(false);
               } else {
                  boolean isCrossbow = stack.getItem() instanceof CrossbowItem;
                  boolean isReadyToShoot = false;
                  if (isCrossbow) {
                     if (CrossbowItem.isCharged(stack)) {
                        isReadyToShoot = true;
                        this.mc.options.useKey.setPressed(false);
                     } else {
                        this.mc.options.useKey.setPressed(true);
                        if (!this.mc.player.isUsingItem()) {
                           this.mc.interactionManager.interactItem(this.mc.player, hand);
                        } else {
                           int requiredTime = this.getPullTime(stack) + (Integer)this.tolerance.get();
                           if (this.mc.player.getItemUseTime() >= requiredTime) {
                              this.mc.interactionManager.stopUsingItem(this.mc.player);
                           }
                        }
                     }
                  } else {
                     this.mc.options.useKey.setPressed(true);
                     if (!this.mc.player.isUsingItem()) {
                        this.mc.interactionManager.interactItem(this.mc.player, hand);
                     } else {
                        int requiredTime = this.getPullTime(stack) + (Integer)this.tolerance.get();
                        if (this.mc.player.getItemUseTime() >= requiredTime) {
                           isReadyToShoot = true;
                        }
                     }
                  }

                  if (isReadyToShoot) {
                     if ((Boolean)this.waitForHurtTime.get() && ((LivingEntity)this.target).hurtTime > 1) {
                        return;
                     }

                     Vec3d shootPos = this.getShootPos(this.target);
                     if (shootPos == null) {
                        return;
                     }

                     this.doTpShoot(shootPos, hand, isCrossbow);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.get() && this.renderPath.size() >= 2) {
         for (int i = 0; i < this.renderPath.size() - 1; i++) {
            event.renderer
               .line(
                  this.renderPath.get(i).getX(),
                  this.renderPath.get(i).getY(),
                  this.renderPath.get(i).getZ(),
                  this.renderPath.get(i + 1).getX(),
                  this.renderPath.get(i + 1).getY(),
                  this.renderPath.get(i + 1).getZ(),
                  Color.RED
               );
         }
      }
   }

   private boolean isWeapon(Item item) {
      return item instanceof CrossbowItem || item instanceof BowItem;
   }

   private void updateTarget() {
      List<Entity> potentialTargets = new ArrayList<>();
      TargetUtils.getList(potentialTargets, this::entityCheck, (SortPriority)this.sortPriority.get(), 1);
      this.target = potentialTargets.isEmpty() ? null : potentialTargets.get(0);
   }

   private boolean entityCheck(Entity entity) {
      if (entity instanceof LivingEntity && entity.isAlive() && entity != this.mc.player) {
         if (!((Set)this.entities.get()).contains(entity.getType())) {
            return false;
         } else {
            Vec3d myPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            return myPos.distanceTo(entityPos) > this.range.get() ? false : !(entity instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()));
         }
      } else {
         return false;
      }
   }

   private int getPullTime(ItemStack stack) {
      if (stack.getItem() instanceof BowItem) {
         return 20;
      } else {
         try {
            Registry<Enchantment> registry = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            Reference<Enchantment> quickChargeEntry = registry.getOrThrow(Enchantments.QUICK_CHARGE);
            int level = EnchantmentHelper.getLevel(quickChargeEntry, stack);
            return Math.max(0, 25 - 5 * level);
         } catch (Exception var5) {
            return 25;
         }
      }
   }

   private void doTpShoot(Vec3d shootPos, Hand hand, boolean isCrossbow) {
      Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      Vec3d vClipStart = null;
      Vec3d vClipEnd = null;
      boolean foundPath = false;
      if (this.hasClearPath(playerPos, shootPos)) {
         vClipStart = playerPos;
         vClipEnd = shootPos;
         foundPath = true;
      } else {
         double maxHeight = Math.max(playerPos.y, shootPos.y);
         double startSearchHeight = maxHeight + 1.0;

         for (double yLevel = startSearchHeight; yLevel < startSearchHeight + 50.0; yLevel++) {
            Vec3d testUp = new Vec3d(playerPos.x, yLevel, playerPos.z);
            Vec3d testTargetUp = new Vec3d(shootPos.x, yLevel, shootPos.z);
            if (this.isSpaceEmpty(testUp) && this.isSpaceEmpty(testTargetUp) && this.hasClearPath(testUp, testTargetUp)) {
               vClipStart = testUp;
               vClipEnd = testTargetUp;
               foundPath = true;
               break;
            }
         }
      }

      if (foundPath) {
         this.renderPath.clear();
         this.renderPath.add(playerPos);
         if (vClipStart != playerPos) {
            this.renderPath.add(vClipStart);
         }

         if (vClipEnd != shootPos) {
            this.renderPath.add(vClipEnd);
         }

         this.renderPath.add(shootPos);
         double totalDist = playerPos.distanceTo(vClipStart) + vClipStart.distanceTo(vClipEnd) + vClipEnd.distanceTo(shootPos);
         int packetsRequired = (int)Math.ceil(totalDist / (Double)this.moveDistance.get()) + 3;
         if (packetsRequired > (Integer)this.limitPacket.get()) {
            ChatUtils.info("§c[TpMachineGun] Targetpastpast, pastmostbigSendPackLimitSystem (" + packetsRequired + ")", new Object[0]);
         } else {
            for (int i = 0; i < packetsRequired; i++) {
               this.sendPosPacket(playerPos.x, playerPos.y, playerPos.z, false);
            }

            if (vClipStart != playerPos) {
               this.sendPosPacket(vClipStart.x, vClipStart.y, vClipStart.z, false);
            }

            if (vClipStart.distanceTo(vClipEnd) > 0.1) {
               this.sendPosPacket(vClipEnd.x, vClipEnd.y, vClipEnd.z, false);
            }

            this.sendPosPacket(shootPos.x, shootPos.y, shootPos.z, false);
            Vec3d targetCenter = this.target.getBoundingBox().getCenter();
            double dX = targetCenter.x - shootPos.x;
            double dY = targetCenter.y - (shootPos.y + 1.62);
            double dZ = targetCenter.z - shootPos.z;
            float yaw = (float)(Math.toDegrees(Math.atan2(dZ, dX)) - 90.0);
            float pitch = (float)(-Math.toDegrees(Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ))));
            this.mc
               .getNetworkHandler()
               .sendPacket(new Full(shootPos.x, shootPos.y, shootPos.z, yaw, pitch, false, this.mc.player.horizontalCollision));
            if (isCrossbow) {
               this.mc.interactionManager.interactItem(this.mc.player, hand);
            } else {
               this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN, 0));
               this.mc.player.stopUsingItem();
            }

            this.mc.player.swingHand(hand);
            this.mc.options.useKey.setPressed(false);
            this.timer = (Integer)this.delay.get();
            if ((Boolean)this.back.get()) {
               if (vClipStart.distanceTo(vClipEnd) > 0.1) {
                  this.sendPosPacket(vClipEnd.x, vClipEnd.y, vClipEnd.z, false);
               }

               if (vClipStart != playerPos) {
                  this.sendPosPacket(vClipStart.x, vClipStart.y, vClipStart.z, false);
               }

               this.sendPosPacket(playerPos.x, playerPos.y + 0.01, playerPos.z, false);
               this.sendPosPacket(playerPos.x, playerPos.y, playerPos.z, true);
               this.mc.player.setPosition(playerPos.x, playerPos.y, playerPos.z);
            } else {
               this.mc.player.setPosition(shootPos.x, shootPos.y, shootPos.z);
            }

            this.mc.player.fallDistance = 0.0;
         }
      }
   }

   private Vec3d getShootPos(Entity target) {
      List<Vec3d> validPositions = new ArrayList<>();
      int centerX = (int)Math.floor(target.getX());
      int centerY = (int)Math.floor(target.getY());
      int centerZ = (int)Math.floor(target.getZ());
      int border = 2;
      Vec3d targetPosVec = new Vec3d(target.getX(), target.getY(), target.getZ());

      for (int x = centerX - border; x <= centerX + border; x++) {
         for (int y = centerY - 1; y <= centerY + border; y++) {
            for (int z = centerZ - border; z <= centerZ + border; z++) {
               Vec3d vec = new Vec3d(x + 0.5, y, z + 0.5);
               if (!(vec.distanceTo(targetPosVec) > 4.0) && this.isSpaceEmpty(vec)) {
                  Vec3d eyePos = vec.add(0.0, 1.62, 0.0);
                  Vec3d targetCenter = target.getBoundingBox().getCenter();
                  if (this.canSee(eyePos, targetCenter)) {
                     validPositions.add(vec);
                  }
               }
            }
         }
      }

      Vec3d myPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      validPositions.sort(Comparator.comparingDouble(v -> v.distanceTo(myPos)));
      return validPositions.isEmpty() ? null : validPositions.get(0);
   }

   private boolean isSpaceEmpty(Vec3d pos) {
      Box box = new Box(
         pos.getX() - 0.3, pos.getY(), pos.getZ() - 0.3, pos.getX() + 0.3, pos.getY() + 1.8, pos.getZ() + 0.3
      );
      return this.mc.world.isSpaceEmpty(box);
   }

   private boolean hasClearPath(Vec3d start, Vec3d end) {
      double dist = start.distanceTo(end);
      int steps = (int)(dist * 2.5);

      for (int i = 0; i <= steps; i++) {
         Vec3d check = start.lerp(end, (double)i / steps);
         if (!this.isSpaceEmpty(check)) {
            return false;
         }
      }

      return true;
   }

   private boolean canSee(Vec3d start, Vec3d end) {
      RaycastContext context = new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player);
      return this.mc.world.raycast(context).getType() == Type.MISS;
   }

   private void sendPosPacket(double x, double y, double z, boolean onGround) {
      this.mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, onGround, this.mc.player.horizontalCollision));
   }
}
