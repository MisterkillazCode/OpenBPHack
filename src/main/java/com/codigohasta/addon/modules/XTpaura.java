package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
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
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class XTpaura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTpOptions = this.settings.createGroup("TP Options");
   private final SettingGroup sgMace = this.settings.createGroup("Mace Exploit");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final List<Vec3d> renderPath = new ArrayList<>();
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("Target Entity")).description("SelectwantAttack'sEntityType."))
            .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER})
            .build()
      );
   private final Setting<Integer> attackDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Attack Delay"))
                  .description("Attack Delay(), notUseWeaponCooldownTimeSpawnEffect."))
               .defaultValue(800))
            .min(1)
            .sliderRange(1, 2000)
            .build()
      );
   private final Setting<Integer> attackTimes = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("AttackTimesNumber"))
                  .description("SingleTimesMove'sAttackTimesNumber(SendPackTimesNumber)."))
               .defaultValue(1))
            .min(1)
            .sliderRange(1, 200)
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("TargetRange"))
               .description("mostbigEnemyDistance."))
            .defaultValue(50.0)
            .min(1.0)
            .sliderRange(1.0, 100.0)
            .build()
      );
   private final Setting<Boolean> bvr = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Visible/RangeTargetSelectPath"))
                  .description("Visible/RangeTargetSelectPath."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> critical = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("StrikeDamage"))
                  .description("pastSendPackBuildlittledownRealStrike."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> findVecToAttack = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoAttackpoint"))
                  .description("AutoTarget'snopoint, DefenseStop."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> prev = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("TargetMovePositionPreTest"))
               .description("TargetMovePositionPreTest(Tick), UseDelay."))
            .defaultValue(0.0)
            .min(0.0)
            .sliderRange(0.0, 5.0)
            .build()
      );
   private final Setting<Boolean> swingHand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Manual"))
                  .description("AttackTimeatClientProceedManual."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> useCooldown = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("WeaponCooldown"))
                  .description("WeaponSelfBody'sCooldownTimeProceedAttack."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> useCooldownBaseTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("WeaponCooldownThreshold"))
                  .description("WeaponCooldownThreshold."))
               .defaultValue(0.75)
               .min(0.1)
               .sliderRange(0.1, 1.0)
               .visible(this.useCooldown::get))
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
   private final Setting<Boolean> ignoreFriends = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("friend"))
                  .description("friendListin'sPlayer."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> ignoreNamed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("LifeNameEntity"))
                  .description("haveCustomName'sEntity."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> ignoreTamed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ServiceEntity"))
                  .description("byService'sMob."))
               .defaultValue(false))
            .build()
      );
   private final Setting<XTpaura.VClipMode> searchVclipMode = this.sgTpOptions
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("VClipPathMode"))
                  .description("Path'sMode."))
               .defaultValue(XTpaura.VClipMode.UP))
            .build()
      );
   private final Setting<Double> moveDistance = this.sgTpOptions
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("MoveDistance"))
               .description("TimesSendPackSwitch'smostbigDistance(DefensePullKey)."))
            .defaultValue(8.0)
            .min(1.0)
            .sliderRange(1.0, 10.0)
            .build()
      );
   private final Setting<Double> searchFindStep = this.sgTpOptions
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("VClipPathDegreelong"))
               .description("VClipPathDegreelong."))
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 2.0)
            .build()
      );
   private final Setting<Boolean> back = this.sgTpOptions
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("whetherMoveBit"))
                  .description("HitCompletePlayerafterwhetherMoveBit(Realkill)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> allowIntoVoid = this.sgTpOptions
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("whetherAllowEnterVoid"))
                  .description("whetherAllowPathEnterVoid."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> limitPacket = this.sgTpOptions
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("mostbigBitMovePackAmount"))
                  .description("SingleTickAllowSend'smostbigBitMovePackAmount(DefenseKick)."))
               .defaultValue(20))
            .min(5)
            .sliderRange(5, 50)
            .build()
      );
   private final Setting<Boolean> printWhenTooManyPacket = this.sgTpOptions
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("whetherOutPackLimitSystemTimeHint"))
                  .description("OutPackLimitSystemTimeatchatSkyBoxHint."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> useMace = this.sgMace
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("whetherUseMaceinstakill"))
                  .description("Use 1.21 Mace instakill(UseDistancehighAirdownDamage)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("whetherRenderTarget"))
                  .description("RendercurrentTarget."))
               .defaultValue(true))
            .build()
      );
   private Entity target;
   private long lastAttackTime = 0L;

   public XTpaura() {
      super(AddonTemplate.CATEGORY, "XTpaura", "Lets you walk up to 99 blocks away (extended teleport aura range).");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.updateTarget();
         this.doAura();
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.get() && !this.renderPath.isEmpty()) {
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

   public String getInfoString() {
      return this.target != null ? EntityUtils.getName(this.target) : null;
   }

   private void updateTarget() {
      List<Entity> potentialTargets = new ArrayList<>();
      TargetUtils.getList(potentialTargets, this::entityCheck, (SortPriority)this.sortPriority.get(), 1);
      if (!potentialTargets.isEmpty()) {
         this.target = potentialTargets.get(0);
      } else {
         this.target = null;
      }
   }

   private boolean entityCheck(Entity entity) {
      if (entity instanceof LivingEntity && entity.isAlive() && entity != this.mc.player) {
         if (!((Set)this.entities.get()).contains(entity.getType())) {
            return false;
         } else if (this.mc.player.distanceTo(entity) > (Double)this.range.get()) {
            return false;
         } else {
            if (entity instanceof PlayerEntity p) {
               if (p.isCreative() || p.isSpectator()) {
                  return false;
               }

               if ((Boolean)this.ignoreFriends.get() && Friends.get().isFriend(p)) {
                  return false;
               }
            }

            return this.ignoreNamed.get() && entity.hasCustomName()
               ? false
               : !(Boolean)this.ignoreTamed.get() || !(entity instanceof TameableEntity) || !((TameableEntity)entity).isTamed();
         }
      } else {
         return false;
      }
   }

   private boolean isReadyToAttack() {
      return this.useCooldown.get()
         ? this.mc.player.getAttackCooldownProgress(0.0F) >= (Double)this.useCooldownBaseTime.get()
         : System.currentTimeMillis() - this.lastAttackTime >= ((Integer)this.attackDelay.get()).intValue();
   }

   private void doAura() {
      if (this.isReadyToAttack() && this.target != null && !this.target.isRemoved() && this.target.isAlive()) {
         Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         Vec3d targetBasePos = new Vec3d(this.target.getX(), this.target.getY(), this.target.getZ());
         if (!(playerPos.distanceTo(targetBasePos) > (Double)this.range.get())) {
            Vec3d targetVec = new Vec3d(
               targetBasePos.getX() + this.target.getVelocity().x * (Double)this.prev.get(),
               targetBasePos.getY() + this.target.getVelocity().y * (Double)this.prev.get(),
               targetBasePos.getZ() + this.target.getVelocity().z * (Double)this.prev.get()
            );
            Vec3d attackPos = null;
            Vec3d[] attackTries = new Vec3d[]{
               new Vec3d(targetVec.getX(), targetVec.getY() + this.target.getStandingEyeHeight() + 0.5, targetVec.getZ()),
               new Vec3d(targetVec.getX() + 0.2, targetVec.getY(), targetVec.getZ() + 0.2),
               targetVec
            };

            for (Vec3d p : attackTries) {
               if (this.isSpaceEmpty(p)) {
                  attackPos = p;
                  break;
               }
            }

            if (attackPos != null) {
               Vec3d vClipStart = null;
               Vec3d vClipEnd = null;
               boolean foundPath = false;
               double horizontalDist = new Vec3d(playerPos.x, 0.0, playerPos.z)
                  .distanceTo(new Vec3d(attackPos.x, 0.0, attackPos.z));
               double maxHeight = Math.max(playerPos.y, attackPos.y);
               double startSearchHeight = maxHeight + 3.0;

               for (double yLevel = startSearchHeight; yLevel < startSearchHeight + 50.0; yLevel += 2.0) {
                  Vec3d testUp = new Vec3d(playerPos.x, yLevel, playerPos.z);
                  Vec3d testTargetUp = new Vec3d(attackPos.x, yLevel, attackPos.z);
                  if (horizontalDist < 0.8) {
                     if (this.isSpaceEmpty(testUp)) {
                        vClipStart = testUp;
                        vClipEnd = testUp;
                        foundPath = true;
                        break;
                     }
                  } else if (this.isSpaceEmpty(testUp) && this.isSpaceEmpty(testTargetUp) && this.hasClearPath(testUp, testTargetUp)) {
                     vClipStart = testUp;
                     vClipEnd = testTargetUp;
                     foundPath = true;
                     break;
                  }
               }

               if (foundPath) {
                  this.renderPath.clear();
                  this.renderPath.add(playerPos);
                  this.renderPath.add(vClipStart);
                  this.renderPath.add(vClipEnd);
                  this.renderPath.add(attackPos);
                  int maxPackets = (int)(
                        Math.ceil(playerPos.distanceTo(vClipStart) / (Double)this.moveDistance.get())
                           + Math.ceil(vClipStart.distanceTo(vClipEnd) / (Double)this.moveDistance.get())
                           + Math.ceil(vClipEnd.distanceTo(attackPos) / (Double)this.moveDistance.get())
                     )
                     + 5;
                  if (maxPackets > (Integer)this.limitPacket.get()) {
                     if ((Boolean)this.printWhenTooManyPacket.get()) {
                        ChatUtils.warning("100m Kill: Pathpastlong (" + maxPackets + "Pack), Already.", new Object[0]);
                     }
                  } else {
                     this.lastAttackTime = System.currentTimeMillis();

                     for (int i = 0; i < 3; i++) {
                        this.sendC04(playerPos.getX(), playerPos.getY(), playerPos.getZ(), false);
                     }

                     this.sendC04(vClipStart.getX(), vClipStart.getY(), vClipStart.getZ(), false);
                     if (vClipStart.distanceTo(vClipEnd) > 0.1) {
                        this.sendC04(vClipEnd.getX(), vClipEnd.getY(), vClipEnd.getZ(), false);
                     }

                     this.sendC04(attackPos.getX(), attackPos.getY(), attackPos.getZ(), false);
                     if ((Boolean)this.critical.get()) {
                        this.sendC04(attackPos.getX(), attackPos.getY() + 0.01, attackPos.getZ(), false);
                        this.sendC04(attackPos.getX(), attackPos.getY(), attackPos.getZ(), false);
                     }

                     int oldSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                     int maceSlot = -1;
                     if ((Boolean)this.useMace.get()) {
                        for (int i = 0; i < 9; i++) {
                           if (this.mc.player.getInventory().getStack(i).getItem().toString().contains("mace")) {
                              maceSlot = i;
                              break;
                           }
                        }
                     }

                     if (maceSlot != -1) {
                        ((InventoryAccessor)this.mc.player.getInventory()).setSelectedSlot(maceSlot);
                        this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.target, this.mc.player.isSneaking()));
                        ((InventoryAccessor)this.mc.player.getInventory()).setSelectedSlot(oldSlot);
                     } else {
                        for (int ix = 0; ix < this.attackTimes.get(); ix++) {
                           this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.target, this.mc.player.isSneaking()));
                        }
                     }

                     if ((Boolean)this.swingHand.get()) {
                        this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        this.mc.player.swingHand(Hand.MAIN_HAND);
                     }

                     if ((Boolean)this.back.get()) {
                        this.sendC04(vClipEnd.getX(), vClipEnd.getY(), vClipEnd.getZ(), false);
                        this.sendC04(vClipStart.getX(), vClipStart.getY(), vClipStart.getZ(), false);
                        double tinyOffset = 0.01;
                        double finalX = playerPos.getX();
                        double finalY = playerPos.getY() + tinyOffset;
                        double finalZ = playerPos.getZ();
                        this.sendC04(finalX, finalY, finalZ, false);
                        this.mc.player.setPosition(finalX, finalY, finalZ);
                     } else {
                        this.mc.player.setPosition(attackPos.getX(), attackPos.getY(), attackPos.getZ());
                     }

                     this.mc.player.fallDistance = 0.0;
                     this.mc.player.resetTicksSinceLastAttack();
                  }
               }
            }
         }
      }
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

   private void sendC04(double x, double y, double z, boolean onGround) {
      this.mc.getNetworkHandler().sendPacket(new PositionAndOnGround(x, y, z, onGround, this.mc.player.horizontalCollision));
   }

   private boolean isSpaceEmpty(Vec3d pos) {
      Box box = new Box(
         pos.getX() - 0.3, pos.getY(), pos.getZ() - 0.3, pos.getX() + 0.3, pos.getY() + 1.8, pos.getZ() + 0.3
      );
      return this.mc.world.isSpaceEmpty(box);
   }

   private Vec3d findVecToAttack(Vec3d targetVec, double targetHeight) {
      double startY = targetVec.getY();
      double endY = targetVec.getY() + targetHeight + 1.0;
      if (this.isSpaceEmpty(new Vec3d(targetVec.getX(), endY, targetVec.getZ()))) {
         return new Vec3d(targetVec.getX(), endY, targetVec.getZ());
      } else {
         double[][] offsets = new double[][]{{1.0, 0.0}, {-1.0, 0.0}, {0.0, 1.0}, {0.0, -1.0}, {0.7, 0.7}, {-0.7, -0.7}, {0.7, -0.7}, {-0.7, 0.7}};

         for (double y = startY; y <= endY; y++) {
            for (double[] offset : offsets) {
               double checkX = targetVec.getX() + offset[0];
               double checkZ = targetVec.getZ() + offset[1];
               if (this.isSpaceEmpty(new Vec3d(checkX, y, checkZ))) {
                  return new Vec3d(checkX, y, checkZ);
               }
            }
         }

         return null;
      }
   }

   private Vec3d findVClipVecToMove(Vec3d start, Vec3d end, double step, boolean allowVoid) {
      XTpaura.VClipMode mode = (XTpaura.VClipMode)this.searchVclipMode.get();
      if (mode == XTpaura.VClipMode.NONE) {
         return start;
      } else {
         double clipY = start.y;
         boolean foundSafePath = false;
         double maxSearchDistance = 25.0;
         if (mode != XTpaura.VClipMode.UP && mode != XTpaura.VClipMode.NORMAL) {
            if (mode == XTpaura.VClipMode.DOWN) {
               double i = 0.0;

               while (i < maxSearchDistance) {
                  clipY = start.getY() - i;
                  if (!allowVoid && clipY < this.mc.world.getBottomY()) {
                     break;
                  }

                  Vec3d testStart = new Vec3d(start.getX(), clipY, start.getZ());
                  Vec3d testEnd = new Vec3d(end.getX(), clipY, end.getZ());
                  if (this.hasClearPath(testStart, testEnd)) {
                     foundSafePath = true;
                     break;
                  }

                  i += step;
               }
            }
         } else {
            double i = 0.0;

            while (i < maxSearchDistance) {
               clipY = start.getY() + i;
               Vec3d testStart = new Vec3d(start.getX(), clipY, start.getZ());
               Vec3d testEnd = new Vec3d(end.getX(), clipY, end.getZ());
               if (this.hasClearPath(testStart, testEnd)) {
                  foundSafePath = true;
                  break;
               }

               i += step;
            }
         }

         return foundSafePath ? new Vec3d(start.getX(), clipY, start.getZ()) : start;
      }
   }

   public static enum VClipMode {
      NONE,
      NORMAL,
      UP,
      DOWN;
   }
}
