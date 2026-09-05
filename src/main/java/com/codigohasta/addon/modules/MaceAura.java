package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos.Mutable;

public class MaceAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCrit = this.settings.createGroup("Criticals");
   private final SettingGroup sgTargeting = this.settings.createGroup("Targeting");
   private final SettingGroup sgWhitelist = this.settings.createGroup("Whitelist");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> range = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("range")).description("AttackRange")).defaultValue(4.5).min(0.0).sliderMax(6.0).build());
   private final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-switch"))
                  .description("AutoSwitchto Mace"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> allowFlight = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("allow-flight"))
                  .description("AllowatFlyAirinTimeSendMoveAttack."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> swingHand = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("swing-hand"))
                  .description("DisplayManual"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> attackDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("attack-delay"))
                  .description("Attack Delay (Tick). Suggest 15 Right."))
               .defaultValue(15))
            .min(0)
            .sliderRange(0, 40)
            .build()
      );
   private final Setting<Double> critHeight = this.sgCrit
      .add(
         ((Builder)((Builder)new Builder().name("crit-height")).description("BuilddownHeight. Note: toohigh(>100)CancanwillTimeOut."))
            .defaultValue(15.0)
            .min(2.0)
            .sliderMax(50.0)
            .max(300.0)
            .build()
      );
   private final Setting<Boolean> autoHeight = this.sgCrit
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("auto-height"))
                  .description("AutoCheckTestHeadBlockUseHeightProceedStrike."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> players = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("players"))
                  .description("AttackPlayer"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
                  .name("entities"))
               .description("SelectwantAttack'sMob"))
            .onlyAttackable()
            .defaultValue(new EntityType[]{EntityType.PLAYER})
            .build()
      );
   private final Setting<Boolean> throughWalls = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("through-walls"))
                  .description("Attack (noViewViewLineCheckTest)"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> ignoreNamed = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ignore-named"))
                  .description("notAttackbyLifeName'sMob."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> ignoreTamed = this.sgTargeting
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("ignore-tamed"))
                  .description("notAttackbyService'sMob (Thing)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<MaceAura.ListMode> listMode = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("mode"))
                  .description("WhiteNameSingle/BlackNameSingleMode."))
               .defaultValue(MaceAura.ListMode.Off))
            .build()
      );
   private final Setting<String> playerList = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                        .name("player-list"))
                     .description("Player List, UseText(,)."))
                  .defaultValue(""))
               .visible(() -> this.listMode.get() != MaceAura.ListMode.Off))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render"))
                  .description("RendercurrentTarget"))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                        .name("shape-mode"))
                     .description("Render Mode"))
                  .defaultValue(ShapeMode.Lines))
               .visible(this.render::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("side-color"))
                  .description("Fill Color"))
               .defaultValue(new SettingColor(255, 0, 0, 75))
               .visible(this.render::get))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("line-color"))
                  .description("LineBoxColor"))
               .defaultValue(new SettingColor(255, 0, 0, 255))
               .visible(this.render::get))
            .build()
      );
   private int timer;
   private int originalSlot = -1;
   private final List<Entity> targets = new ArrayList<>();
   private Entity currentTarget;
   private int switchCooldown;

   public MaceAura() {
      super(AddonTemplate.CATEGORY, "MaceAura", "Mace aura at range. Not a great module to use.");
   }

   public void onActivate() {
      this.timer = 0;
      this.originalSlot = -1;
      this.targets.clear();
      this.currentTarget = null;
      this.switchCooldown = 0;
   }

   public void onDeactivate() {
      if (this.originalSlot != -1 && (Boolean)this.autoSwitch.get() && this.mc.player != null) {
         InvUtils.swap(this.originalSlot, false);
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.timer > 0) {
            this.timer--;
         } else if (this.switchCooldown > 0) {
            this.switchCooldown--;
         } else if ((Boolean)this.allowFlight.get() || this.mc.player.isOnGround()) {
            if ((Boolean)this.autoSwitch.get()) {
               if (!this.checkAndSwapWeapon()) {
                  return;
               }
            } else if (!this.mc.player.getMainHandStack().getItem().toString().contains("mace")) {
               return;
            }

            this.targets.clear();
            TargetUtils.getList(this.targets, this::entityCheck, SortPriority.ClosestAngle, 1);
            if (this.targets.isEmpty()) {
               this.currentTarget = null;
            } else {
               this.currentTarget = this.targets.get(0);
               this.doMaceCritAttack(this.currentTarget);
               this.timer = (Integer)this.attackDelay.get();
            }
         }
      }
   }

   private boolean checkAndSwapWeapon() {
      if (this.mc.player.getMainHandStack().getItem().toString().contains("mace")) {
         return true;
      } else {
         FindItemResult mace = InvUtils.find(itemStack -> itemStack.getItem().toString().contains("mace"), 0, 8);
         if (mace.found()) {
            this.originalSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            InvUtils.swap(mace.slot(), false);
            this.switchCooldown = 2;
            return true;
         } else {
            return false;
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if ((Boolean)this.render.get() && this.currentTarget != null) {
         event.renderer.box(this.currentTarget.getBoundingBox(), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
      }
   }

   private void doMaceCritAttack(Entity target) {
      double x = this.mc.player.getX();
      double y = this.mc.player.getY();
      double z = this.mc.player.getZ();
      double height = (Double)this.critHeight.get();
      if ((Boolean)this.autoHeight.get()) {
         double safeHeight = 0.0;
         Mutable checkPos = new Mutable(this.mc.player.getX(), this.mc.player.getEyeY(), this.mc.player.getZ());

         for (int i = 0; i < 256 && this.mc.world.getBlockState(checkPos.move(0, 1, 0)).isAir(); i++) {
            safeHeight++;
         }

         height = Math.min((Double)this.critHeight.get(), safeHeight);
      }

      boolean wasOnGround = this.mc.player.isOnGround();
      this.mc.player.setVelocity(0.0, 0.0, 0.0);
      double yaw = Rotations.getYaw(target);
      double pitch = Rotations.getPitch(target);
      this.mc.getNetworkHandler().sendPacket(new LookAndOnGround((float)yaw, (float)pitch, wasOnGround, this.mc.player.horizontalCollision));
      this.sendPacket(x, y, z, true);
      double currentY = y;
      double targetY = y + height;
      double maxStep = 8.0;

      while (currentY < targetY) {
         currentY = Math.min(currentY + maxStep, targetY);
         this.sendPacket(x, currentY, z, false);
      }

      double hitY = y + 1.1;
      double droppingY = targetY;

      while (droppingY > hitY + maxStep) {
         droppingY -= maxStep;
         this.sendPacket(x, droppingY, z, false);
      }

      this.sendPacket(x, hitY, z, false);
      this.mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, this.mc.player.isSneaking()));
      if ((Boolean)this.swingHand.get()) {
         this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }

      this.sendPacket(x, y, z, true);
   }

   private void sendPacket(double x, double y, double z, boolean onGround) {
      this.mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, onGround, this.mc.player.horizontalCollision));
   }

   private boolean entityCheck(Entity entity) {
      if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
         return false;
      } else if (entity.equals(this.mc.player)) {
         return false;
      } else if (this.mc.player.distanceTo(entity) > (Double)this.range.get()) {
         return false;
      } else if (!(Boolean)this.throughWalls.get() && !this.mc.player.canSee(entity)) {
         return false;
      } else if (entity instanceof PlayerEntity p) {
         if (!(Boolean)this.players.get()) {
            return false;
         } else if (p.isCreative()) {
            return false;
         } else if (!Friends.get().shouldAttack(p)) {
            return false;
         } else {
            String name = p.getName().getString();
            List<String> players = Arrays.stream(((String)this.playerList.get()).split(",")).map(String::trim).collect(Collectors.toList());
            switch ((MaceAura.ListMode)this.listMode.get()) {
               case Whitelist:
                  return players.contains(name);
               case Blacklist:
                  return !players.contains(name);
               default:
                  return true;
            }
         }
      } else if ((Boolean)this.ignoreNamed.get() && entity.hasCustomName()) {
         return false;
      } else {
         return this.ignoreTamed.get() && entity instanceof TameableEntity && ((TameableEntity)entity).isTamed()
            ? false
            : ((Set)this.entities.get()).contains(entity.getType());
      }
   }

   public String getInfoString() {
      return this.currentTarget != null ? EntityUtils.getName(this.currentTarget) : null;
   }

   public static enum ListMode {
      Whitelist,
      Blacklist,
      Off;
   }
}
