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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class TpBowAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
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
                     .name("WaitInvincible"))
                  .description("WaitTargetStrike'sRedColorInvincibleendafterShootStrike, DefenseArrow."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> shootDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ShootStrikeDelay"))
                  .description("TimesShootStrikeofBetween'smostlittleDelay()."))
               .defaultValue(450))
            .min(0)
            .sliderMax(2000)
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
   private long lastShootTime = 0L;
   private Entity target;
   private final List<Vec3d> renderPath = new ArrayList<>();

   public TpBowAura() {
      super(AddonTemplate.CATEGORY, "TpBowAura", "Teleports to the target body and shoots arrows at them. Feature module (gcore).");
   }

   public String getInfoString() {
      return this.target != null ? EntityUtils.getName(this.target) : null;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         String mainHandName = this.mc.player.getMainHandStack().getItem().toString().toLowerCase();
         String offHandName = this.mc.player.getOffHandStack().getItem().toString().toLowerCase();
         boolean holdingBow = mainHandName.contains("bow") || offHandName.contains("bow");
         if (holdingBow && this.mc.player.isUsingItem()) {
            if (System.currentTimeMillis() - this.lastShootTime >= ((Integer)this.shootDelay.get()).intValue()) {
               this.updateTarget();
               if (this.target != null) {
                  if (!(Boolean)this.waitForHurtTime.get() || ((LivingEntity)this.target).hurtTime <= 1) {
                     Vec3d shootPos = this.getShootPos(this.target);
                     if (shootPos != null) {
                        this.doTpShoot(shootPos);
                     }
                  }
               }
            }
         } else {
            this.target = null;
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
            return this.mc.player.distanceTo(entity) > this.range.get()
               ? false
               : !(entity instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()));
         }
      } else {
         return false;
      }
   }

   private void doTpShoot(Vec3d shootPos) {
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
            ChatUtils.info("§c[TpBow] Targetpastpast, pastmostbigSendPackLimitSystem (" + packetsRequired + ")", new Object[0]);
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
            this.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN, 0));
            this.mc.player.stopUsingItem();
            this.lastShootTime = System.currentTimeMillis();
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
