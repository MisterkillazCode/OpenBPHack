package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.BlockPosX;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.CombatUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoCity extends Module {
   public static AutoCity INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> targetRange = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("TargetRange")).description("Target Distance")).defaultValue(6)).min(0).sliderMax(8).build());
   public final Setting<Integer> range = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Range")).description("Reach Distance")).defaultValue(6)).min(0).sliderMax(8).build());
   private final Setting<Boolean> doubleBreak = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("DoubleBreak"))
                  .description("Double Mine"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Boolean> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("CityDelay"))
                  .description("Delay"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> antiCrawl = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AntiCrawl"))
                  .description("AutoAntidown"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> preferSelfClick = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("PreferSelfClick"))
                  .description("LogicManualpointStrike'sMine"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> head = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Head"))
                  .description("Head"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> burrow = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Burrow"))
                  .description("Mine Obsidian Stuck"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> face = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Face"))
                  .description("Mine Face"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> down = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Down"))
                  .description("Mine Feet"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> surround = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Surround"))
                  .description("Pack"))
               .defaultValue(true))
            .build()
      );
   private final Timer cityTimer = new Timer();
   public static final List<Block> hard = Arrays.asList(
      Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
   );

   public AutoCity() {
      super(AddonTemplate.CATEGORY, "AutoCity", "Automatically breaks blocks under targets.");
      INSTANCE = this;
   }

   @EventHandler
   public void onPacketSend(Send event) {
      if (event.packet instanceof PlayerActionC2SPacket packet && packet.getAction() == Action.STOP_DESTROY_BLOCK) {
         this.cityTimer.reset();
      }
   }

   @EventHandler
   public void onTick(Pre event) {
      PlayerEntity player = CombatUtil.getClosestEnemy(((Integer)this.targetRange.get()).intValue());
      if (!(Boolean)this.preferSelfClick.get() || PacketMinePlus.selfClickPos == null) {
         if (!(Boolean)this.delay.get() || this.cityTimer.passedMs((long)((Integer)PacketMinePlus.INSTANCE.mineDelay.get()).intValue())) {
            if ((Boolean)this.antiCrawl.get()
               && this.mc.player.isCrawling()
               && this.canBreak(this.mc.player.getBlockPos().up())
               && !this.mc.player.getBlockPos().up().equals(PacketMinePlus.targetPos)
               && !this.mc.player.getBlockPos().up().equals(PacketMinePlus.secondPos)) {
               PacketMinePlus.selfClickPos = this.mc.player.getBlockPos().up();
               PacketMinePlus.INSTANCE.mine(this.mc.player.getBlockPos().up());
            } else if (player != null) {
               this.doBreak(player);
            }
         }
      }
   }

   private void doBreak(PlayerEntity player) {
      BlockPos pos = player.getBlockPos();
      double[] yOffset = new double[]{-0.8, 0.3, 2.3, 1.1};
      double[] xzOffset = new double[]{0.3, -0.3};
      if (!(Boolean)this.doubleBreak.get()) {
         for (PlayerEntity entity : CombatUtil.getEnemies(((Integer)this.targetRange.get()).intValue())) {
            for (double y : yOffset) {
               for (double x : xzOffset) {
                  for (double z : xzOffset) {
                     BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                     if (this.isObsidian(offsetPos) && BlockUtil.getClickSideStrict(offsetPos) != null && offsetPos.equals(PacketMinePlus.targetPos)) {
                        return;
                     }
                  }
               }
            }
         }
      } else {
         int count = 0;

         for (PlayerEntity entity : CombatUtil.getEnemies(((Integer)this.targetRange.get()).intValue())) {
            for (double y : yOffset) {
               for (double x : xzOffset) {
                  for (double zx : xzOffset) {
                     BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + zx);
                     if (this.isObsidian(offsetPos)
                        && BlockUtil.getClickSideStrict(offsetPos) != null
                        && (offsetPos.equals(PacketMinePlus.targetPos) || offsetPos.equals(PacketMinePlus.secondPos))) {
                        count++;
                     }
                  }
               }
            }
         }

         if (count == 2) {
            return;
         }
      }

      List<Float> yList = new ArrayList<>();
      if ((Boolean)this.down.get()) {
         yList.add(-0.8F);
      }

      if ((Boolean)this.head.get()) {
         yList.add(2.3F);
      }

      if ((Boolean)this.burrow.get()) {
         yList.add(0.3F);
      }

      if ((Boolean)this.face.get()) {
         yList.add(1.1F);
      }

      Iterator var27 = yList.iterator();

      while (var27.hasNext()) {
         double y = ((Float)var27.next()).floatValue();

         for (double offset : xzOffset) {
            BlockPos offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
            if (this.canBreak(offsetPos)) {
               PacketMinePlus.INSTANCE.mine(offsetPos);
               return;
            }
         }
      }

      var27 = yList.iterator();

      while (var27.hasNext()) {
         double y = ((Float)var27.next()).floatValue();

         for (double offsetx : xzOffset) {
            for (double offset2 : xzOffset) {
               BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offsetx);
               if (this.canBreak(offsetPos)) {
                  PacketMinePlus.INSTANCE.mine(offsetPos);
                  return;
               }
            }
         }
      }

      if ((Boolean)this.surround.get()) {
         for (Direction i : Direction.values()) {
            if (i != Direction.UP
               && i != Direction.DOWN
               && !(Math.sqrt(this.mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > ((Integer)this.range.get()).intValue())
               && (this.mc.world.isAir(pos.offset(i)) || pos.offset(i).equals(PacketMinePlus.targetPos))
               && this.canPlaceCrystal(pos.offset(i), false)) {
               if (!(Boolean)this.doubleBreak.get()) {
                  return;
               }

               if (PacketMinePlus.targetPos != null && PacketMinePlus.completed) {
                  return;
               }
            }
         }

         ArrayList<BlockPos> list = new ArrayList<>();

         for (Direction ix : Direction.values()) {
            if (ix != Direction.UP
               && ix != Direction.DOWN
               && !(Math.sqrt(this.mc.player.getEyePos().squaredDistanceTo(pos.offset(ix).toCenterPos())) > ((Integer)this.range.get()).intValue())
               && this.canBreak(pos.offset(ix))
               && this.canPlaceCrystal(pos.offset(ix), true)
               && !this.isSurroundPos(pos.offset(ix))) {
               list.add(pos.offset(ix));
            }
         }

         if (!list.isEmpty()) {
            PacketMinePlus.INSTANCE.mine(list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(this.mc.player.getEyePos()))).get());
         } else {
            for (Direction ixx : Direction.values()) {
               if (ixx != Direction.UP
                  && ixx != Direction.DOWN
                  && !(Math.sqrt(this.mc.player.getEyePos().squaredDistanceTo(pos.offset(ixx).toCenterPos())) > ((Integer)this.range.get()).intValue())
                  && this.canBreak(pos.offset(ixx))
                  && this.canPlaceCrystal(pos.offset(ixx), false)) {
                  list.add(pos.offset(ixx));
               }
            }

            if (!list.isEmpty()) {
               PacketMinePlus.INSTANCE.mine(list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(this.mc.player.getEyePos()))).get());
            }
         }
      }
   }

   private boolean isSurroundPos(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN) {
            BlockPos self = this.getPlayerPos(true);
            if (self.offset(i).equals(pos)) {
               return true;
            }
         }
      }

      return false;
   }

   public BlockPos getPlayerPos(boolean fix) {
      return new BlockPosX(new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ()), fix);
   }

   public Block getBlock(BlockPos pos) {
      return this.mc.world.getBlockState(pos).getBlock();
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean block) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (this.getBlock(obsPos) == Blocks.BEDROCK || this.getBlock(obsPos) == Blocks.OBSIDIAN || !block)
         && BlockUtil.noEntityBlockCrystal(boost, true, true)
         && BlockUtil.noEntityBlockCrystal(boost.up(), true, true);
   }

   private boolean isObsidian(BlockPos pos) {
      return this.mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= ((Integer)PacketMinePlus.INSTANCE.range.get()).intValue()
         && hard.contains(this.mc.world.getBlockState(pos).getBlock())
         && BlockUtil.getClickSideStrict(pos) != null;
   }

   private boolean canBreak(BlockPos pos) {
      return this.isObsidian(pos)
         && BlockUtil.getClickSideStrict(pos) != null
         && !pos.equals(PacketMinePlus.targetPos)
         && !pos.equals(PacketMinePlus.secondPos)
         && ((PacketMinePlus.targetPos == null || PacketMinePlus.secondPos == null) && (Boolean)this.doubleBreak.get() || PacketMinePlus.targetPos == null);
   }
}
