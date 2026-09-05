package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.CamUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class Follower extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTargets = this.settings.createGroup("TargetSelect");
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final List<Entity> targets = new ArrayList<>();
   private final Setting<Set<EntityType<?>>> entities = this.sgTargets
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Target Entity")).description("Select'sMobwillbyViewForTarget"))
               .defaultValue(Set.of(EntityType.PLAYER)))
            .build()
      );
   private final Setting<Boolean> attackSurvival = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackSpawnMode"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> attackCreative = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackBuildMode"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> attackAdventure = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackMode"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Priority"))
               .defaultValue(SortPriority.ClosestAngle))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
               .name("CheckTestRange"))
            .defaultValue(50.0)
            .range(0.0, 192.0)
            .build()
      );
   private final Setting<Boolean> dynamic = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("MoveEnemy"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> onlyAir = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("LimitAirin"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> preventGround = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("notGround"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Render"))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Render Mode"))
                  .defaultValue(ShapeMode.Both))
               .visible(this.render::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(160, 0, 225, 35))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Color"))
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
      );
   private final Setting<Integer> fireworkTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("NormalBetween"))
               .min(0)
               .sliderMax(200)
               .defaultValue(50))
            .build()
      );
   private final Setting<Integer> waspSprint = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("Between"))
               .min(0)
               .sliderMax(200)
               .defaultValue(20))
            .build()
      );
   private int timer;

   public Follower() {
      super(AddonTemplate.CATEGORY, "Follower", "Follows a target player. Feature module.");
   }

   public void onActivate() {
      this.targets.clear();
      this.timer = 0;
   }

   public void onDeactivate() {
      this.targets.clear();
      CamUtils.rem(this);
      this.mc.options.sneakKey.setPressed(false);
      this.mc.options.jumpKey.setPressed(false);
   }

   private void findTarget() {
      this.targets.clear();
      Entity bestTarget = null;
      double closestDiff = Double.MAX_VALUE;

      for (Entity entity : this.mc.world.getEntities()) {
         if (entity != this.mc.player && entity.isAlive() && entity instanceof LivingEntity) {
            double dist = this.mc.player.distanceTo(entity);
            if (!(dist > (Double)this.range.get()) && ((Set)this.entities.get()).contains(entity.getType())) {
               if (entity instanceof PlayerEntity player) {
                  if (!Friends.get().shouldAttack(player)) {
                     continue;
                  }

                  GameMode gm = this.getGameMode(player);
                  if (gm == GameMode.CREATIVE && !(Boolean)this.attackCreative.get()
                     || gm == GameMode.SURVIVAL && !(Boolean)this.attackSurvival.get()
                     || gm == GameMode.ADVENTURE && !(Boolean)this.attackAdventure.get()
                     || gm == GameMode.SPECTATOR) {
                     continue;
                  }
               }

               if (dist < closestDiff) {
                  closestDiff = dist;
                  bestTarget = entity;
               }
            }
         }
      }

      if (bestTarget != null) {
         this.targets.add(bestTarget);
      }
   }

   private GameMode getGameMode(PlayerEntity p) {
      if (this.mc.getNetworkHandler() == null) {
         return GameMode.DEFAULT;
      } else {
         PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
         if (entry == null) {
            return GameMode.DEFAULT;
         } else {
            GameMode gm = entry.getGameMode();
            return gm != null ? gm : GameMode.DEFAULT;
         }
      }
   }

   @EventHandler
   private void onRender3d(Render3DEvent event) {
      if (this.mc.player != null && this.mc.player.isAlive()) {
         if ((Boolean)this.dynamic.get() || this.targets.isEmpty()) {
            this.findTarget();
         }

         if (this.targets.isEmpty()) {
            CamUtils.rem(this);
            this.mc.options.sneakKey.setPressed(false);
            this.mc.options.jumpKey.setPressed(false);
         } else {
            CamUtils.add(this);
            Entity primary = this.targets.getFirst();
            if ((!(Boolean)this.onlyAir.get() || !this.mc.player.isOnGround()) && (!(Boolean)this.preventGround.get() || !primary.isOnGround())) {
               this.mc.options.sneakKey.setPressed(CamUtils.pitch() > 0.0F);
               this.mc.options.jumpKey.setPressed(CamUtils.pitch() <= 0.0F);
               MeteorClient.mc.player.setYaw((float)Rotations.getYaw(primary));
               MeteorClient.mc
                  .player
                  .setPitch(primary.isOnGround() && this.preventGround.get() ? -90.0F : (float)Rotations.getPitch(primary, Target.Body));
            }
         }

         if ((Boolean)this.render.get() && !this.targets.isEmpty() && this.targets.getFirst() != null) {
            Entity target = this.targets.getFirst();
            Vec3d lerped = target.getLerpedPos(event.tickDelta);
            double x = lerped.x - target.getX();
            double y = lerped.y - target.getY();
            double z = lerped.z - target.getZ();
            Box box = target.getBoundingBox();
            event.renderer
               .box(
                  x + box.minX,
                  y + box.minY,
                  z + box.minZ,
                  x + box.maxX,
                  y + box.maxY,
                  z + box.maxZ,
                  (Color)this.sideColor.get(),
                  (Color)this.lineColor.get(),
                  (ShapeMode)this.shapeMode.get(),
                  0
               );
         }
      }
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null) {
         int countdown = !this.targets.isEmpty() ? (Integer)this.waspSprint.get() : (Integer)this.fireworkTime.get();
         if (this.mc.player.isGliding() && this.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            if (this.timer < 0 && this.mc.options.forwardKey.isPressed()) {
               this.quickUse(Items.FIREWORK_ROCKET);
               this.timer = countdown;
            }

            this.timer--;
         } else {
            this.timer = -1;
         }
      }
   }

   public String getInfoString() {
      return !this.targets.isEmpty() ? EntityUtils.getName(this.targets.getFirst()) : null;
   }

   void quickUse(Item item) {
      FindItemResult result = InvUtils.find(new Item[]{item});
      if (result.found()) {
         int selectedSlot = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
         int itemSlot = result.slot();
         boolean wasHeld = result.isMainHand();
         if (!wasHeld) {
            InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
         }

         this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
         if (!wasHeld) {
            InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
         }
      }
   }
}
