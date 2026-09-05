package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class TpAnchor extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTP = this.settings.createGroup("Send");
   private final SettingGroup sgSafety = this.settings.createGroup("Setting");
   private final SettingGroup sgTargets = this.settings.createGroup("TargetSetting");
   private final SettingGroup sgWhitelist = this.settings.createGroup("WhiteNameSingleSetting");
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final Setting<Double> range = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("StrikeDistance")).description("Target'sRadius.")).defaultValue(49.0).min(1.0).sliderMax(100.0).build());
   private final Setting<Integer> attackDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("StrikeBetween"))
               .defaultValue(2))
            .min(0)
            .sliderMax(20)
            .build()
      );
   private final Setting<Boolean> pauseOnEat = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("thingPause"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoRefill = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Auto"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> placeRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("Place Radius"))
               .defaultValue(5))
            .min(2)
            .sliderMax(8)
            .build()
      );
   private final Setting<TpAnchor.Mode> tpMode = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("Mode"))
                  .description("Vanilla = LimitSystem, Paper = highSendPackpast"))
               .defaultValue(TpAnchor.Mode.Vanilla))
            .build()
      );
   private final Setting<Boolean> goUp = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("VClip "))
                  .description("RowVerticalpastGround."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> vanillaPackets = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("trashPack (Vanilla)"))
                  .defaultValue(4))
               .visible(() -> this.tpMode.get() == TpAnchor.Mode.Vanilla))
            .build()
      );
   private final Setting<Integer> paperPackets = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("trashPack (Paper)"))
                  .defaultValue(5))
               .visible(() -> this.tpMode.get() == TpAnchor.Mode.Paper))
            .build()
      );
   private final Setting<Double> horizontalOffset = this.sgTP.add(((Builder)new Builder().name("WaterBitMove")).defaultValue(0.05).build());
   private final Setting<Double> yOffset = this.sgTP.add(((Builder)new Builder().name("VerticalBitMove")).defaultValue(0.01).build());
   private final Setting<Boolean> skipCollisionCheck = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Check"))
                  .description("ifEnable BoatNoclip, pastBlockCheckTest."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> airPlace = this.sgTP
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AirinPut"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> autoProtect = this.sgSafety
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AutoDefense"))
               .defaultValue(true))
            .build()
      );
   private final Setting<List<Block>> shieldBlocks = this.sgSafety
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
               .name("DefenseBlock"))
            .defaultValue(new Block[]{Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK, Blocks.ANVIL})
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder)new meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder()
               .name("Target Entity"))
            .defaultValue(new EntityType[]{EntityType.PLAYER})
            .build()
      );
   private final Setting<Boolean> ignoreFriends = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("friend"))
                  .defaultValue(false))
               .description("EnableafternotfriendForAttack Target"))
            .build()
      );
   private final Setting<Boolean> ignoreNamed = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("LifeName"))
                  .defaultValue(false))
               .description("EnableafternotLifeNameEntityForAttack Target"))
            .build()
      );
   private final Setting<Boolean> ignoreTamed = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Service"))
                  .defaultValue(false))
               .description("EnableafternotService'sMobForAttack Target"))
            .build()
      );
   private final Setting<Boolean> attackSurvival = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackSpawn"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> attackCreative = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("AttackBuild"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> attackAdventure = this.sgTargets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Attack"))
               .defaultValue(false))
            .build()
      );
   private final Setting<TpAnchor.ListMode> listMode = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("WhiteNameSingleMode"))
               .defaultValue(TpAnchor.ListMode.Off))
            .build()
      );
   private final Setting<String> playerList = this.sgWhitelist
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("PlayerNameSingle"))
                  .defaultValue(""))
               .visible(() -> this.listMode.get() != TpAnchor.ListMode.Off))
            .build()
      );
   private final Setting<Boolean> renderTarget = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("RenderTarget"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("FaceColor"))
            .defaultValue(new SettingColor(255, 0, 0, 25))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("LineColor"))
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
      );
   private final Setting<Boolean> renderPath = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("RenderPath"))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("Target Color"))
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
      );
   private final Setting<SettingColor> pathColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("PathColor"))
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .build()
      );
   private final Setting<Boolean> debug = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("Debug Info"))
               .defaultValue(true))
            .build()
      );
   private final List<Vec3d> renderPathNodes = new ArrayList<>();
   private int delayTimer = 0;
   private Entity currentTarget = null;
   private static final int SLOT_ANCHOR = 6;
   private static final int SLOT_GLOWSTONE = 7;
   private static final int SLOT_SHIELD = 8;

   public TpAnchor() {
      super(AddonTemplate.CATEGORY, "TpAnchor", "Teleports to targets and places powered anchors to strike them at range.");
   }

   @EventHandler
   public void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.currentTarget = this.findTarget();
         if (this.delayTimer > 0) {
            this.delayTimer--;
         } else if (!(Boolean)this.pauseOnEat.get() || !this.mc.player.isUsingItem()) {
            if (this.currentTarget != null) {
               FindItemResult anchor = InvUtils.find(new Item[]{Items.RESPAWN_ANCHOR});
               FindItemResult glowstone = InvUtils.find(new Item[]{Items.GLOWSTONE});
               FindItemResult shield = InvUtils.find(
                  stack -> stack.getItem() instanceof BlockItem && ((List)this.shieldBlocks.get()).contains(((BlockItem)stack.getItem()).getBlock())
               );
               boolean missingItem = false;
               if (!anchor.found()) {
                  if ((Boolean)this.debug.get() && this.delayTimer == 0) {
                     this.error("Pack: HeavySpawn (Respawn Anchor)!", new Object[0]);
                  }

                  missingItem = true;
               }

               if (!glowstone.found()) {
                  if ((Boolean)this.debug.get() && this.delayTimer == 0) {
                     this.error("Pack: (Glowstone)!", new Object[0]);
                  }

                  missingItem = true;
               }

               if ((Boolean)this.autoProtect.get() && !shield.found()) {
                  if ((Boolean)this.debug.get() && this.delayTimer == 0) {
                     this.error("Pack: DefenseBlock (Shield Block)!", new Object[0]);
                  }

                  missingItem = true;
               }

               if (missingItem) {
                  this.delayTimer = 40;
               } else {
                  int aSlot = this.getSlot(anchor, 6);
                  int gSlot = this.getSlot(glowstone, 7);
                  int sSlot = this.autoProtect.get() ? this.getSlot(shield, 8) : -1;
                  if (aSlot != -1 && gSlot != -1) {
                     TpAnchor.AttackPos info = this.findBestPos(this.currentTarget);
                     if (info != null) {
                        this.executeTPAuraAttack(info, aSlot, gSlot, sSlot);
                        this.delayTimer = (Integer)this.attackDelay.get();
                     }
                  }
               }
            }
         }
      }
   }

   private void executeTPAuraAttack(TpAnchor.AttackPos info, int aSlot, int gSlot, int sSlot) {
      Entity baseEntity = (Entity)(this.mc.player.hasVehicle() ? this.mc.player.getVehicle() : this.mc.player);
      Vec3d startPos = new Vec3d(baseEntity.getX(), baseEntity.getY(), baseEntity.getZ());
      Vec3d targetStandPos = info.tpPos;
      if (this.invalid(targetStandPos)) {
         targetStandPos = this.findNearestPos(targetStandPos);
         if (targetStandPos == null) {
            return;
         }
      }

      double clipHeight = Math.min((Double)this.range.get(), this.mc.world.getTopYInclusive() - startPos.y - 1.0);
      Vec3d upPos = startPos.add(0.0, clipHeight, 0.0);
      Vec3d targetUpPos = targetStandPos.add(0.0, clipHeight, 0.0);
      this.renderPathNodes.clear();
      this.renderPathNodes.add(startPos);
      if ((Boolean)this.goUp.get()) {
         this.renderPathNodes.add(upPos);
         this.renderPathNodes.add(targetUpPos);
      }

      this.renderPathNodes.add(targetStandPos);
      int spamCount = this.tpMode.get() == TpAnchor.Mode.Vanilla ? (Integer)this.vanillaPackets.get() : (Integer)this.paperPackets.get();

      for (int i = 0; i < spamCount; i++) {
         if (this.mc.player.hasVehicle()) {
            this.mc.player.networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(this.mc.player.getVehicle()));
         } else {
            this.mc
               .player
               .networkHandler
               .sendPacket(new LookAndOnGround(this.mc.player.getYaw(), this.mc.player.getPitch(), false, this.mc.player.horizontalCollision));
         }
      }

      if ((Boolean)this.goUp.get()) {
         this.sendMove(baseEntity, upPos);
      }

      if ((Boolean)this.goUp.get()) {
         this.sendMove(baseEntity, targetUpPos);
      }

      this.sendMove(baseEntity, targetStandPos);
      InvUtils.swap(aSlot, true);
      this.placePacket(info.pos);
      if ((Boolean)this.autoProtect.get() && sSlot != -1) {
         BlockPos shieldPos = this.getShieldPos(info.pos, BlockPos.ofFloored(targetStandPos));
         if (shieldPos != null && this.mc.world.getBlockState(shieldPos).isReplaceable()) {
            InvUtils.swap(sSlot, true);
            this.placePacket(shieldPos);
         }
      }

      InvUtils.swap(gSlot, true);
      Direction side = this.findBestSide(info.pos);
      this.interactPacket(info.pos, side);
      InvUtils.swap(aSlot, true);
      this.interactPacket(info.pos, side);
      InvUtils.swapBack();
      if ((Boolean)this.goUp.get()) {
         this.sendMove(baseEntity, targetUpPos);
         this.sendMove(baseEntity, upPos);
      }

      this.sendMove(baseEntity, startPos);
      Vec3d finalOffset = this.getOffset(startPos);
      this.sendMove(baseEntity, finalOffset);
      baseEntity.setPosition(finalOffset.x, finalOffset.y, finalOffset.z);
   }

   private void sendMove(Entity entity, Vec3d pos) {
      if (this.mc.getNetworkHandler() != null) {
         if (entity instanceof PlayerEntity) {
            PlayerMoveC2SPacket packet = new Full(
               pos.x,
               pos.y,
               pos.z,
               this.mc.player.getYaw(),
               this.mc.player.getPitch(),
               false,
               this.mc.player.horizontalCollision
            );
            ((IPlayerMoveC2SPacket)packet).meteor$setTag(1337);
            this.mc.player.networkHandler.sendPacket(packet);
         } else {
            this.mc
               .player
               .networkHandler
               .sendPacket(
                  new VehicleMoveC2SPacket(pos, this.mc.player.getVehicle().getYaw(), this.mc.player.getVehicle().getPitch(), false)
               );
         }
      }
   }

   private boolean invalid(Vec3d pos) {
      if (this.mc.world == null) {
         return true;
      } else {
         BlockPos bp = BlockPos.ofFloored(pos);
         if (this.mc.world.getChunk(bp.getX() >> 4, bp.getZ() >> 4) == null) {
            return true;
         } else {
            Entity entity = (Entity)(this.mc.player.hasVehicle() ? this.mc.player.getVehicle() : this.mc.player);
            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            Box box = entity.getBoundingBox().offset(pos.subtract(entityPos));

            for (BlockPos b : BlockPos.iterate(
               BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
            )) {
               BlockState state = this.mc.world.getBlockState(b);
               if (state.isOf(Blocks.LAVA)) {
                  return true;
               }

               if (!state.getCollisionShape(this.mc.world, b).isEmpty()) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private Vec3d findNearestPos(Vec3d desired) {
      for (int x = -2; x <= 2; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
               Vec3d test = desired.add(x, y, z);
               if (!this.invalid(test)) {
                  return test;
               }
            }
         }
      }

      return null;
   }

   private Vec3d getOffset(Vec3d base) {
      double d = (Double)this.horizontalOffset.get();
      double dy = (Double)this.yOffset.get();
      Vec3d[] list = new Vec3d[]{
         base.add(d, dy, 0.0),
         base.add(-d, dy, 0.0),
         base.add(0.0, dy, d),
         base.add(0.0, dy, -d),
         base.add(d, dy, d),
         base.add(-d, dy, -d),
         base.add(-d, dy, d),
         base.add(d, dy, -d)
      };
      List<Vec3d> offsets = Arrays.asList(list);
      Collections.shuffle(offsets);

      for (Vec3d p : offsets) {
         if (!this.invalid(p)) {
            return p;
         }
      }

      return base.add(0.0, dy, 0.0);
   }

   private boolean hasClearPath(Vec3d start, Vec3d end) {
      int steps = Math.max(10, (int)(start.distanceTo(end) * 2.5));

      for (int i = 1; i < steps; i++) {
         if (this.invalid(start.lerp(end, (double)i / steps))) {
            return false;
         }
      }

      return true;
   }

   private TpAnchor.AttackPos findBestPos(Entity target) {
      BlockPos tPos = target.getBlockPos();
      double halfHeight = (target.getBoundingBox().maxY - target.getBoundingBox().minY) / 2.0;
      Vec3d targetCenter = new Vec3d(target.getX(), target.getY() + halfHeight, target.getZ());
      List<TpAnchor.AttackPos> candidates = new ArrayList<>();
      int r = (Integer)this.placeRange.get();
      double rSq = r * r;

      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               if (!(x * x + y * y + z * z > rSq)) {
                  BlockPos pos = tPos.add(x, y, z);
                  if (this.checkPlace(pos)) {
                     Vec3d validTpSpot = this.findSmartTpSpot(pos);
                     if (validTpSpot != null) {
                        double score = this.calculateScore(target, targetCenter, pos);
                        candidates.add(new TpAnchor.AttackPos(pos, validTpSpot, score));
                     }
                  }
               }
            }
         }
      }

      if (!candidates.isEmpty()) {
         candidates.sort(Comparator.comparingDouble(p -> p.score));
         return candidates.get(0);
      } else {
         return null;
      }
   }

   private Vec3d findSmartTpSpot(BlockPos anchorPos) {
      BlockPos[] testOffsets = new BlockPos[]{
         anchorPos.up(2),
         anchorPos.north(2),
         anchorPos.south(2),
         anchorPos.east(2),
         anchorPos.west(2),
         anchorPos.up(1),
         anchorPos.north(1),
         anchorPos.south(1),
         anchorPos.east(1),
         anchorPos.west(1)
      };

      for (BlockPos p : testOffsets) {
         Vec3d testVec = new Vec3d(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
         if (!this.invalid(testVec)) {
            return testVec;
         }
      }

      return null;
   }

   private double calculateScore(Entity target, Vec3d targetCenter, BlockPos anchorPos) {
      Vec3d anchorVec = new Vec3d(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5);
      double distSq = anchorVec.squaredDistanceTo(targetCenter);
      double score = distSq;
      if (!this.isExposed(target, anchorPos)) {
         score = distSq + 1000.0;
      }

      int openness = 0;

      for (Direction dir : Direction.values()) {
         if (this.mc.world.getBlockState(anchorPos.offset(dir)).isReplaceable()) {
            openness++;
         }
      }

      return score + (6 - openness) * 2.0;
   }

   private boolean isExposed(Entity target, BlockPos anchorPos) {
      Vec3d start = new Vec3d(target.getX(), target.getY() + target.getEyeHeight(target.getPose()), target.getZ());
      Vec3d end = new Vec3d(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5);
      BlockHitResult result = this.mc.world.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, target));
      return result.getType() == Type.MISS || result.getBlockPos().equals(anchorPos);
   }

   private boolean checkPlace(BlockPos pos) {
      if (!this.mc.world.isInBuildLimit(pos)) {
         return false;
      } else {
         BlockState state = this.mc.world.getBlockState(pos);
         if (!state.isReplaceable()) {
            return false;
         } else if (!this.mc.world.canPlace(Blocks.RESPAWN_ANCHOR.getDefaultState(), pos, ShapeContext.absent())) {
            return false;
         } else {
            if (!(Boolean)this.airPlace.get()) {
               boolean hasNeighbor = false;

               for (Direction d : Direction.values()) {
                  if (!this.mc.world.getBlockState(pos.offset(d)).isReplaceable()) {
                     hasNeighbor = true;
                     break;
                  }
               }

               if (!hasNeighbor) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private BlockPos getShieldPos(BlockPos anchor, BlockPos player) {
      int dx = player.getX() - anchor.getX();
      int dz = player.getZ() - anchor.getZ();
      return dx == 0 && dz == 0 ? anchor.up() : anchor.add(Integer.compare(dx, 0), 0, Integer.compare(dz, 0));
   }

   private Entity findTarget() {
      Iterator var1 = this.mc.world.getEntities().iterator();

      Entity e;
      while (true) {
         if (!var1.hasNext()) {
            return null;
         }

         e = (Entity)var1.next();
         if (e != this.mc.player
            && e.isAlive()
            && e instanceof LivingEntity
            && ((Set)this.entities.get()).contains(e.getType())
            && !(e.distanceTo(this.mc.player) > (Double)this.range.get())
            && (!(Boolean)this.ignoreFriends.get() || !(e instanceof PlayerEntity p) || !Friends.get().isFriend(p))
            && (!(Boolean)this.ignoreNamed.get() || !e.hasCustomName())
            && (!(Boolean)this.ignoreTamed.get() || !(e instanceof TameableEntity tameable) || !tameable.isTamed())) {
            if (!(e instanceof PlayerEntity px)) {
               break;
            }

            GameMode gm = this.getGameMode(px);
            if ((gm != GameMode.SURVIVAL || (Boolean)this.attackSurvival.get())
               && (gm != GameMode.CREATIVE || (Boolean)this.attackCreative.get())
               && (gm != GameMode.ADVENTURE || (Boolean)this.attackAdventure.get())
               && Friends.get().shouldAttack(px)) {
               if (this.listMode.get() == TpAnchor.ListMode.Off) {
                  break;
               }

               List<String> names = Arrays.stream(((String)this.playerList.get()).split(",")).map(String::trim).collect(Collectors.toList());
               if ((this.listMode.get() != TpAnchor.ListMode.Whitelist || names.contains(px.getName().getString()))
                  && (this.listMode.get() != TpAnchor.ListMode.Blacklist || !names.contains(px.getName().getString()))) {
                  break;
               }
            }
         }
      }

      return e;
   }

   private GameMode getGameMode(PlayerEntity p) {
      PlayerListEntry entry = this.mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
      return entry == null ? GameMode.SURVIVAL : entry.getGameMode();
   }

   private int getSlot(FindItemResult res, int pref) {
      if (res.isHotbar()) {
         return res.slot();
      } else if ((Boolean)this.autoRefill.get() && res.found()) {
         InvUtils.move().from(res.slot()).toHotbar(pref);
         return pref;
      } else {
         return -1;
      }
   }

   private Direction findBestSide(BlockPos p) {
      for (Direction d : Direction.values()) {
         if (this.mc.world.getBlockState(p.offset(d)).isReplaceable()) {
            return d;
         }
      }

      return Direction.UP;
   }

   private void placePacket(BlockPos p) {
      this.mc
         .getNetworkHandler()
         .sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(p.toCenterPos(), Direction.UP, p, false), 0));
      this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
   }

   private void interactPacket(BlockPos p, Direction d) {
      this.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(p.toCenterPos(), d, p, false), 0));
      this.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
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

   private static class AttackPos {
      BlockPos pos;
      Vec3d tpPos;
      double score;

      public AttackPos(BlockPos p, Vec3d tp, double s) {
         this.pos = p;
         this.tpPos = tp;
         this.score = s;
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
