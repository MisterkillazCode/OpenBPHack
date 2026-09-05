package com.codigohasta.addon.utils.leaveshack;

import com.codigohasta.addon.modules.AutoCity;
import com.codigohasta.addon.modules.GlobalSetting;
import com.codigohasta.addon.utils.BlockPosX;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class BlockUtil {
   public static CopyOnWriteArrayList<BlockPos> placeList = new CopyOnWriteArrayList<>();
   private static final double MIN_EYE_HEIGHT = 0.4;
   private static final double MAX_EYE_HEIGHT = 1.62;
   private static final double MOVEMENT_THRESHOLD = 2.0E-4;
   public static final List<Block> shiftBlocks = Arrays.asList(
      Blocks.ENDER_CHEST,
      Blocks.CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.CRAFTING_TABLE,
      Blocks.BIRCH_TRAPDOOR,
      Blocks.BAMBOO_TRAPDOOR,
      Blocks.DARK_OAK_TRAPDOOR,
      Blocks.CHERRY_TRAPDOOR,
      Blocks.ANVIL,
      Blocks.BREWING_STAND,
      Blocks.HOPPER,
      Blocks.DROPPER,
      Blocks.DISPENSER,
      Blocks.ACACIA_TRAPDOOR,
      Blocks.ENCHANTING_TABLE,
      Blocks.WHITE_SHULKER_BOX,
      Blocks.ORANGE_SHULKER_BOX,
      Blocks.MAGENTA_SHULKER_BOX,
      Blocks.LIGHT_BLUE_SHULKER_BOX,
      Blocks.YELLOW_SHULKER_BOX,
      Blocks.LIME_SHULKER_BOX,
      Blocks.PINK_SHULKER_BOX,
      Blocks.GRAY_SHULKER_BOX,
      Blocks.CYAN_SHULKER_BOX,
      Blocks.PURPLE_SHULKER_BOX,
      Blocks.BLUE_SHULKER_BOX,
      Blocks.BROWN_SHULKER_BOX,
      Blocks.GREEN_SHULKER_BOX,
      Blocks.RED_SHULKER_BOX,
      Blocks.BLACK_SHULKER_BOX
   );

   public static Direction getClickSideStrict(BlockPos pos) {
      Direction side = null;
      double minDistance = Double.MAX_VALUE;

      for (Direction i : Direction.values()) {
         if (isGrimDirection(pos, i)) {
            double disSq = MeteorClient.mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos());
            if (!(disSq > minDistance)) {
               side = i;
               minDistance = disSq;
            }
         }
      }

      return side;
   }

   public static Vec3d getClosestPointToBox(Vec3d pos, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      double closestX = Math.max(minX, Math.min(pos.x, maxX));
      double closestY = Math.max(minY, Math.min(pos.y, maxY));
      double closestZ = Math.max(minZ, Math.min(pos.z, maxZ));
      return new Vec3d(closestX, closestY, closestZ);
   }

   public static Vec3d getClosestPointToBox(Vec3d eyePos, Box boundingBox) {
      return getClosestPointToBox(
         eyePos, boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ
      );
   }

   public static Vec3d getClosestPoint(Entity entity) {
      return getClosestPointToBox(MeteorClient.mc.player.getEyePos(), entity.getBoundingBox());
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive()
            && (!ignoreItem || !(entity instanceof ItemEntity))
            && (
               !ignoreCrystal
                  || !(entity instanceof EndCrystalEntity)
                  || !(MeteorClient.mc.player.getEyePos().distanceTo(getClosestPoint(entity)) <= ((Integer)AutoCity.INSTANCE.range.get()).intValue())
            )) {
            return false;
         }
      }

      return true;
   }

   public static boolean canClick(BlockPos pos) {
      return MeteorClient.mc.world.getBlockState(pos).isSolid()
         && (!shiftBlocks.contains(getBlock(pos)) && !(getBlock(pos) instanceof BedBlock) || MeteorClient.mc.player.isSneaking());
   }

   public static boolean canClick(BlockPos pos, boolean ignoreSneak) {
      return MeteorClient.mc.world.getBlockState(pos).isSolid()
         && (!shiftBlocks.contains(getBlock(pos)) && !(getBlock(pos) instanceof BedBlock) || MeteorClient.mc.player.isSneaking() || ignoreSneak);
   }

   public static boolean canPlace(BlockPos pos) {
      return canPlace(pos, null);
   }

   public static boolean hasCrystalPlace(BlockPos pos) {
      for (Entity entity : getEndCrystals(new Box(pos))) {
         if (entity.isAlive() && entity instanceof EndCrystalEntity crystal) {
            return crystal.getBlockPos().equals(pos);
         }
      }

      return false;
   }

   public static boolean clientCanPlace(BlockPos pos, boolean ignoreCrystal) {
      return !canReplace(pos) ? false : !hasEntity(pos, ignoreCrystal);
   }

   public static boolean canReplace(BlockPos pos) {
      return pos.getY() >= 320 ? false : MeteorClient.mc.world.getBlockState(pos).isReplaceable();
   }

   public static boolean canPlace(BlockPos pos, Predicate<Direction> directionPredicate) {
      if (getPlaceSide(pos, directionPredicate) == null) {
         return false;
      } else {
         return !canReplace(pos) ? false : !hasEntity(pos, false);
      }
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal, boolean ignorePlayer) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))
            && (!ignorePlayer || !(entity instanceof PlayerEntity))) {
            return true;
         }
      }

      return false;
   }

   public static List<Entity> getEntities(Box box) {
      List<Entity> list = new ArrayList<>();

      for (Entity entity : MeteorClient.mc.world.getEntities()) {
         if (entity != null && entity.getBoundingBox().intersects(box)) {
            list.add(entity);
         }
      }

      return list;
   }

   public static ArrayList<BlockPos> getSphere(double range) {
      return getSphere(range, MeteorClient.mc.player.getEyePos());
   }

   public static ArrayList<BlockPos> getSphere(double range, Vec3d pos) {
      ArrayList<BlockPos> list = new ArrayList<>();

      for (double x = pos.getX() - range; x < pos.getX() + range; x++) {
         for (double z = pos.getZ() - range; z < pos.getZ() + range; z++) {
            for (double y = pos.getY() - range; y < pos.getY() + range; y++) {
               BlockPos curPos = new BlockPosX(x, y, z);
               if (!(curPos.toCenterPos().distanceTo(pos) > range) && !list.contains(curPos)) {
                  list.add(curPos);
               }
            }
         }
      }

      return list;
   }

   public static boolean hasPlayerEntity(BlockPos pos) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity instanceof PlayerEntity) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasCrystal(BlockPos pos) {
      for (Entity entity : getEndCrystals(new Box(pos))) {
         if (entity.isAlive() && entity instanceof EndCrystalEntity) {
            return true;
         }
      }

      return false;
   }

   public static List<EndCrystalEntity> getEndCrystals(Box box) {
      List<EndCrystalEntity> list = new ArrayList<>();

      for (Entity entity : MeteorClient.mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity crystal && crystal.getBoundingBox().intersects(box)) {
            list.add(crystal);
         }
      }

      return list;
   }

   public static Direction getPlaceSide(BlockPos pos, Predicate<Direction> directionPredicate) {
      if (pos == null) {
         return null;
      } else {
         double dis = 114514.0;
         Direction side = null;

         for (Direction i : Direction.values()) {
            if ((directionPredicate == null || directionPredicate.test(i))
               && canClick(pos.offset(i))
               && !MeteorClient.mc.world.getBlockState(pos.offset(i)).isReplaceable()
               && isGrimDirection(pos.offset(i), i.getOpposite())) {
               double vecDis = MeteorClient.mc
                  .player
                  .getEyePos()
                  .squaredDistanceTo(
                     pos.toCenterPos()
                        .add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5)
                  );
               if (side == null || vecDis < dis) {
                  side = i;
                  dis = vecDis;
               }
            }
         }

         return side;
      }
   }

   public static Direction getPlaceSide(BlockPos pos, Predicate<Direction> directionPredicate, boolean ignoreSneak) {
      if (pos == null) {
         return null;
      } else {
         double dis = 114514.0;
         Direction side = null;

         for (Direction i : Direction.values()) {
            if ((directionPredicate == null || directionPredicate.test(i))
               && canClick(pos.offset(i), ignoreSneak)
               && !MeteorClient.mc.world.getBlockState(pos.offset(i)).isReplaceable()
               && isGrimDirection(pos.offset(i), i.getOpposite())) {
               double vecDis = MeteorClient.mc
                  .player
                  .getEyePos()
                  .squaredDistanceTo(
                     pos.toCenterPos()
                        .add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5)
                  );
               if (side == null || vecDis < dis) {
                  side = i;
                  dis = vecDis;
               }
            }
         }

         return side;
      }
   }

   public static ArrayList<Direction> getPlaceSides(BlockPos pos, Predicate<Direction> directionPredicate) {
      ArrayList<Direction> sides = new ArrayList<>();
      if (pos == null) {
         return sides;
      } else {
         for (Direction i : Direction.values()) {
            if (directionPredicate == null || directionPredicate.test(i)) {
               BlockPos neighbor = pos.offset(i);
               BlockState neighborState = MeteorClient.mc.world.getBlockState(neighbor);
               if (canClick(neighbor) && !neighborState.isReplaceable() && isGrimDirection(neighbor, i.getOpposite())) {
                  sides.add(i);
               }
            }
         }

         return sides;
      }
   }

   public static ArrayList<Direction> getPlaceSides(BlockPos pos, Predicate<Direction> directionPredicate, boolean ignoreSneak) {
      ArrayList<Direction> sides = new ArrayList<>();
      if (pos == null) {
         return sides;
      } else {
         for (Direction i : Direction.values()) {
            if (directionPredicate == null || directionPredicate.test(i)) {
               BlockPos neighbor = pos.offset(i);
               BlockState neighborState = MeteorClient.mc.world.getBlockState(neighbor);
               if (canClick(neighbor, ignoreSneak) && !neighborState.isReplaceable() && isGrimDirection(neighbor, i.getOpposite())) {
                  sides.add(i);
               }
            }
         }

         return sides;
      }
   }

   public static boolean canSee(BlockPos pos, Direction side) {
      Vec3d testVec = pos.toCenterPos()
         .add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
      HitResult result = MeteorClient.mc
         .world
         .raycast(new RaycastContext(getEyesPos(), testVec, ShapeType.COLLIDER, FluidHandling.NONE, MeteorClient.mc.player));
      return result == null || result.getType() == Type.MISS;
   }

   public static Vec3d getEyesPos() {
      return MeteorClient.mc.player.getEyePos();
   }

   public static Direction getClickSide(BlockPos pos) {
      Direction side = null;
      double range = 100.0;

      for (Direction i : Direction.values()) {
         if (canSee(pos, i)
            && !(MathHelper.sqrt((float)MeteorClient.mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range)) {
            side = i;
            range = MathHelper.sqrt((float)MeteorClient.mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()));
         }
      }

      if (side != null) {
         return side;
      } else {
         side = Direction.UP;

         for (Direction ix : Direction.values()) {
            if (isGrimDirection(pos, ix)
               && !(MathHelper.sqrt((float)MeteorClient.mc.player.getEyePos().squaredDistanceTo(pos.offset(ix).toCenterPos())) > range)) {
               side = ix;
               range = MathHelper.sqrt((float)MeteorClient.mc.player.getEyePos().squaredDistanceTo(pos.offset(ix).toCenterPos()));
            }
         }

         return side;
      }
   }

   private static Box getCombinedBox(BlockPos pos, World level) {
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos).offset(pos.getX(), pos.getY(), pos.getZ());
      Box combined = new Box(pos);

      for (Box box : shape.getBoundingBoxes()) {
         double minX = Math.max(box.minX, combined.minX);
         double minY = Math.max(box.minY, combined.minY);
         double minZ = Math.max(box.minZ, combined.minZ);
         double maxX = Math.min(box.maxX, combined.maxX);
         double maxY = Math.min(box.maxY, combined.maxY);
         double maxZ = Math.min(box.maxZ, combined.maxZ);
         combined = new Box(minX, minY, minZ, maxX, maxY, maxZ);
      }

      return combined;
   }

   private static boolean isIntersected(Box bb, Box other) {
      return other.maxX - 1.0E-7 > bb.minX
         && other.minX + 1.0E-7 < bb.maxX
         && other.maxY - 1.0E-7 > bb.minY
         && other.minY + 1.0E-7 < bb.maxY
         && other.maxZ - 1.0E-7 > bb.minZ
         && other.minZ + 1.0E-7 < bb.maxZ;
   }

   public static boolean isGrimDirection(BlockPos pos, Direction direction) {
      Box combined = getCombinedBox(pos, MeteorClient.mc.world);
      ClientPlayerEntity player = MeteorClient.mc.player;
      Box eyePositions = new Box(
            player.getX(),
            player.getY() + 0.4,
            player.getZ(),
            player.getX(),
            player.getY() + 1.62,
            player.getZ()
         )
         .expand(2.0E-4);
      if (isIntersected(eyePositions, combined)) {
         return true;
      } else {
         switch (direction) {
            case NORTH:
               if (!(eyePositions.minZ > combined.minZ)) {
                  return true;
               }
               break;
            case SOUTH:
               if (!(eyePositions.maxZ < combined.maxZ)) {
                  return true;
               }
               break;
            case EAST:
               if (!(eyePositions.maxX < combined.maxX)) {
                  return true;
               }
               break;
            case WEST:
               if (!(eyePositions.minX > combined.minX)) {
                  return true;
               }
               break;
            case UP:
               if (!(eyePositions.maxY < combined.maxY)) {
                  return true;
               }
               break;
            case DOWN:
               if (!(eyePositions.minY > combined.minY)) {
                  return true;
               }
               break;
            default:
               throw new MatchException(null, null);
         }

         return false;
      }
   }

   public static void placeBlock(BlockPos pos, Direction side, boolean rotate) {
      clickBlock(pos.offset(side), side.getOpposite(), rotate);
      placeList.add(pos);
   }

   public static void placeSlabBlock(BlockPos pos, Direction side, Direction slabSide, boolean rotate) {
      clickSlabBlock(pos.offset(side), side.getOpposite(), slabSide, rotate);
      placeList.add(pos);
   }

   public static Block getBlock(BlockPos pos) {
      return MeteorClient.mc.world.getBlockState(pos).getBlock();
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate) {
      Vec3d directionVec = new Vec3d(
         pos.getX() + 0.5 + side.getVector().getX() * 0.5,
         pos.getY() + 0.5 + side.getVector().getY() * 0.5,
         pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
      );
      if (rotate) {
         Rotation.snapAt(directionVec);
      }

      EntityUtil.placeSwingHand();
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if ((Boolean)GlobalSetting.INSTANCE.packetPlace.get()) {
         PendingUpdateManager mgr = MeteorClient.mc.world.getPendingUpdateManager().incrementSequence();

         try {
            MeteorClient.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, mgr.getSequence()));
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
      } else {
         MeteorClient.mc.interactionManager.interactBlock(MeteorClient.mc.player, Hand.MAIN_HAND, result);
      }

      if (rotate) {
         Rotation.snapBack();
      }
   }

   public static boolean needSneak(Block in) {
      return shiftBlocks.contains(in);
   }

   public static void clickSlabBlock(BlockPos pos, Direction side, Direction slabSide, boolean rotate) {
      double yOffset = 0.5;
      if (slabSide == Direction.UP) {
         yOffset += 0.1;
      }

      if (slabSide == Direction.DOWN) {
         yOffset -= 0.1;
      }

      Vec3d directionVec = new Vec3d(
         pos.getX() + 0.5 + side.getVector().getX() * 0.5,
         pos.getY() + yOffset + side.getVector().getY() * 0.5,
         pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
      );
      if (rotate) {
         Rotation.snapAt(directionVec);
      }

      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if ((Boolean)GlobalSetting.INSTANCE.packetPlace.get()) {
         PendingUpdateManager mgr = MeteorClient.mc.world.getPendingUpdateManager().incrementSequence();

         try {
            MeteorClient.mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, mgr.getSequence()));
         } catch (Throwable var12) {
            if (mgr != null) {
               try {
                  mgr.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (mgr != null) {
            mgr.close();
         }

         MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
      } else {
         MeteorClient.mc.interactionManager.interactBlock(MeteorClient.mc.player, Hand.MAIN_HAND, result);
      }

      if (rotate) {
         Rotation.snapBack();
      }
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      if (!MeteorClient.mc.world.isAir(pos)) {
         return false;
      } else {
         BlockPos obsPos = pos.down();
         BlockPos boost = obsPos.up();
         return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
            && getClickSideStrict(obsPos) != null
            && MeteorClient.mc.world.isAir(boost)
            && !hasEntityBlockCrystal(boost, false)
            && !hasEntityBlockCrystal(boost.up(), false);
      }
   }

   public static boolean hasEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive() && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))) {
            return true;
         }
      }

      return false;
   }
}
