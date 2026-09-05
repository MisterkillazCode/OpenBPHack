package com.codigohasta.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

public class OreVeinESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBaritone = this.settings.createGroup("Baritone Auto");
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final Setting<OreVeinESP.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Mode")).description("ModeOnlyScanatFace's.")).defaultValue(OreVeinESP.Mode.Legit)).build());
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)((meteordevelopment.meteorclient.settings.BlockListSetting.Builder)new meteordevelopment.meteorclient.settings.BlockListSetting.Builder()
                  .name("TargetBlock"))
               .description("SelectneedScan's."))
            .defaultValue(
               new Block[]{
                  Blocks.DIAMOND_ORE,
                  Blocks.DEEPSLATE_DIAMOND_ORE,
                  Blocks.ANCIENT_DEBRIS,
                  Blocks.GOLD_ORE,
                  Blocks.DEEPSLATE_GOLD_ORE,
                  Blocks.NETHER_GOLD_ORE,
                  Blocks.IRON_ORE,
                  Blocks.DEEPSLATE_IRON_ORE,
                  Blocks.RAW_IRON_BLOCK,
                  Blocks.COPPER_ORE,
                  Blocks.DEEPSLATE_COPPER_ORE,
                  Blocks.RAW_COPPER_BLOCK,
                  Blocks.EMERALD_ORE,
                  Blocks.DEEPSLATE_EMERALD_ORE,
                  Blocks.LAPIS_ORE,
                  Blocks.DEEPSLATE_LAPIS_ORE,
                  Blocks.REDSTONE_ORE,
                  Blocks.DEEPSLATE_REDSTONE_ORE,
                  Blocks.COAL_ORE,
                  Blocks.DEEPSLATE_COAL_ORE,
                  Blocks.NETHER_QUARTZ_ORE
               }
            )
            .build()
      );
   private final Setting<Integer> radius = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Scan Radius"))
                  .description("WaterScan Radius (Chunk)."))
               .defaultValue(6))
            .min(2)
            .sliderMax(16)
            .build()
      );
   private final Setting<Integer> yMin = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("mostlowHeight"))
                  .description("Scan'smostlow Y Height."))
               .defaultValue(-64))
            .sliderMin(-64)
            .sliderMax(320)
            .build()
      );
   private final Setting<Integer> yMax = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("mosthighHeight"))
                  .description("Scan'smosthigh Y Height."))
               .defaultValue(120))
            .sliderMin(-64)
            .sliderMax(320)
            .build()
      );
   private final Setting<Integer> scanDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("ScanDelay (Tick)"))
                  .description("afterScan'sTimeBetween."))
               .defaultValue(20))
            .min(5)
            .build()
      );
   private final Setting<Boolean> autoMine = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Auto Mine"))
                  .description("let Baritone AutogoScanto'sFace."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Integer> autoMineRange = this.sgBaritone
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("Auto MineDistance"))
                     .description("OnlyMineDistancePlayermanyfewinside's(too'snotwant)."))
                  .defaultValue(16))
               .min(5)
               .sliderMax(64)
               .visible(this.autoMine::get))
            .build()
      );
   private final Setting<Integer> renderLimit = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("RenderupLimit"))
                  .description("DefenseStopRenderpastmany FPS down."))
               .defaultValue(10000))
            .min(100)
            .sliderMax(20000)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(((Builder)((Builder)((Builder)new Builder().name("Render")).description("Block'sRenderDirection.")).defaultValue(ShapeMode.Lines)).build());
   private final Setting<Boolean> useAutoColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("AutoColor"))
                  .description("TypeAutoDisplayColor(Blue, YellowGoldYellow)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> customColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("OneColor"))
                  .description("DisableAutoColorafter, haveBlockDisplay'sOneColor."))
               .defaultValue(new SettingColor(255, 255, 255))
               .visible(() -> !(Boolean)this.useAutoColor.get()))
            .build()
      );
   private final Map<ChunkPos, List<OreVeinESP.RenderBlock>> cachedChunks = new ConcurrentHashMap<>();
   private final Map<Block, Color> colorCache = new HashMap<>();
   private int timer = 0;
   private int baritoneTimer = 0;

   public OreVeinESP() {
      super(AddonTemplate.SC_CATEGORY, "OreVeinESP", "Scans for and highlights ore veins, with optional auto-mining.");
   }

   public void onDeactivate() {
      this.cachedChunks.clear();
      this.colorCache.clear();
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.world != null && this.mc.player != null) {
         this.timer++;
         if (this.timer >= (Integer)this.scanDelay.get()) {
            this.timer = 0;
            MeteorExecutor.execute(this::scanSurroundings);
         }

         if ((Boolean)this.autoMine.get() && BaritoneUtils.IS_AVAILABLE) {
            if (this.baritoneTimer > 0) {
               this.baritoneTimer--;
            } else {
               this.baritoneTimer = 10;
               this.handleBaritoneLogic();
            }
         }
      }
   }

   private void handleBaritoneLogic() {
      BlockPos playerPos = this.mc.player.getBlockPos();
      BlockPos closestVisiblePos = null;
      double closestVisibleDistSq = Double.MAX_VALUE;
      double maxDistSq = Math.pow(((Integer)this.autoMineRange.get()).intValue(), 2.0);

      for (List<OreVeinESP.RenderBlock> list : this.cachedChunks.values()) {
         if (list != null && !list.isEmpty()) {
            for (OreVeinESP.RenderBlock rb : list) {
               double distSq = rb.pos.getSquaredDistance(playerPos);
               if (distSq <= maxDistSq && distSq < closestVisibleDistSq) {
                  closestVisibleDistSq = distSq;
                  closestVisiblePos = rb.pos;
               }
            }
         }
      }

      if (closestVisiblePos != null) {
         boolean shouldOverride = false;
         if (!BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            shouldOverride = true;
         } else {
            Goal currentGoal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
            if (currentGoal != null && currentGoal instanceof GoalBlock) {
               BlockPos currentTarget = ((GoalBlock)currentGoal).getGoalPos();
               double currentTargetDistSq = currentTarget.getSquaredDistance(playerPos);
               if (closestVisibleDistSq < currentTargetDistSq - 2.0) {
                  shouldOverride = true;
               }
            } else {
               shouldOverride = true;
            }
         }

         if (shouldOverride && !this.isMiningTarget(closestVisiblePos)) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(closestVisiblePos));
         }
      }
   }

   private boolean isMiningTarget(BlockPos target) {
      if (!BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
         return false;
      } else {
         Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
         if (goal == null) {
            return false;
         } else {
            return goal instanceof GoalBlock goalBlock ? goalBlock.getGoalPos().equals(target) : false;
         }
      }
   }

   @EventHandler
   private void onBlockUpdate(Receive event) {
      if (event.packet instanceof BlockUpdateS2CPacket packet) {
         this.removeBlockFromCache(packet.getPos());
      } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket packet) {
         packet.visitUpdates((pos, state) -> {
            if (!((List)this.blocks.get()).contains(state.getBlock())) {
               this.removeBlockFromCache(pos);
            }
         });
      }
   }

   private void scanSurroundings() {
      if (this.mc.player != null && this.mc.world != null) {
         ChunkPos center = new ChunkPos(this.mc.player.getBlockPos());
         int r = (Integer)this.radius.get();
         int minY = (Integer)this.yMin.get();
         int maxY = (Integer)this.yMax.get();
         Set<Block> targetBlocks = new HashSet<>((Collection<? extends Block>)this.blocks.get());
         if (!targetBlocks.isEmpty()) {
            this.cachedChunks
               .keySet()
               .removeIf(pos -> Math.abs(pos.x - center.x) > r + 2 || Math.abs(pos.z - center.z) > r + 2);

            for (int x = -r; x <= r; x++) {
               for (int z = -r; z <= r; z++) {
                  ChunkPos chunkPos = new ChunkPos(center.x + x, center.z + z);
                  if (!this.cachedChunks.containsKey(chunkPos) && this.isChunkAndNeighborsLoaded(chunkPos)) {
                     WorldChunk chunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);
                     if (chunk != null) {
                        List<OreVeinESP.RenderBlock> found = new ArrayList<>();
                        int startX = chunkPos.getStartX();
                        int startZ = chunkPos.getStartZ();
                        int chunkTopY = chunk.getBottomY() + chunk.getHeight();
                        int actualMinY = Math.max(minY, chunk.getBottomY());
                        int actualMaxY = Math.min(maxY, chunkTopY);

                        for (int bx = 0; bx < 16; bx++) {
                           for (int bz = 0; bz < 16; bz++) {
                              for (int by = actualMinY; by < actualMaxY; by++) {
                                 BlockPos localPos = new BlockPos(startX + bx, by, startZ + bz);
                                 Block block = chunk.getBlockState(localPos).getBlock();
                                 if (targetBlocks.contains(block) && (this.mode.get() != OreVeinESP.Mode.Legit || this.isExposed(localPos))) {
                                    found.add(new OreVeinESP.RenderBlock(localPos, block));
                                 }
                              }
                           }
                        }

                        if (!found.isEmpty()) {
                           this.cachedChunks.put(chunkPos, found);
                        } else {
                           this.cachedChunks.put(chunkPos, Collections.emptyList());
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isChunkAndNeighborsLoaded(ChunkPos center) {
      return this.mc.world.getChunkManager().isChunkLoaded(center.x, center.z)
         && this.mc.world.getChunkManager().isChunkLoaded(center.x + 1, center.z)
         && this.mc.world.getChunkManager().isChunkLoaded(center.x - 1, center.z)
         && this.mc.world.getChunkManager().isChunkLoaded(center.x, center.z + 1)
         && this.mc.world.getChunkManager().isChunkLoaded(center.x, center.z - 1);
   }

   private boolean isExposed(BlockPos pos) {
      int bottomY = this.mc.world.getBottomY();
      int topY = bottomY + this.mc.world.getHeight();

      for (Direction dir : Direction.values()) {
         BlockPos neighbor = pos.offset(dir);
         if (neighbor.getY() >= bottomY
            && neighbor.getY() < topY
            && !this.mc.world.getBlockState(neighbor).isSideSolidFullSquare(this.mc.world, neighbor, dir.getOpposite())) {
            return true;
         }
      }

      return false;
   }

   private void removeBlockFromCache(BlockPos pos) {
      ChunkPos chunkPos = new ChunkPos(pos);
      List<OreVeinESP.RenderBlock> blocks = this.cachedChunks.get(chunkPos);
      if (blocks != null && !blocks.isEmpty()) {
         try {
            List<OreVeinESP.RenderBlock> newBlocks = new ArrayList<>(blocks);
            if (newBlocks.removeIf(rb -> rb.pos.equals(pos))) {
               this.cachedChunks.put(chunkPos, newBlocks);
            }
         } catch (Exception var5) {
         }
      }
   }

   private Color getOreColor(Block block) {
      return this.colorCache.computeIfAbsent(block, b -> {
         if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return new Color(0, 255, 255);
         } else if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE || b == Blocks.RAW_GOLD_BLOCK) {
            return new Color(255, 215, 0);
         } else if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE || b == Blocks.RAW_IRON_BLOCK) {
            return new Color(210, 180, 160);
         } else if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE || b == Blocks.RAW_COPPER_BLOCK) {
            return new Color(255, 100, 0);
         } else if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) {
            return new Color(0, 255, 0);
         } else if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE || b == Blocks.LAPIS_BLOCK) {
            return new Color(0, 0, 255);
         } else if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return new Color(255, 0, 0);
         } else if (b == Blocks.ANCIENT_DEBRIS) {
            return new Color(160, 32, 240);
         } else if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) {
            return new Color(30, 30, 30);
         } else if (b == Blocks.NETHER_QUARTZ_ORE) {
            return new Color(220, 220, 220);
         } else {
            int mapColor = b.getDefaultMapColor().color;
            return mapColor == 0 ? new Color(255, 0, 255) : new Color(mapColor >> 16 & 0xFF, mapColor >> 8 & 0xFF, mapColor & 0xFF, 255);
         }
      });
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      int count = 0;
      int limit = (Integer)this.renderLimit.get();

      for (List<OreVeinESP.RenderBlock> list : this.cachedChunks.values()) {
         if (list != null && !list.isEmpty()) {
            for (OreVeinESP.RenderBlock rb : list) {
               if (count >= limit) {
                  return;
               }

               Color color;
               if ((Boolean)this.useAutoColor.get()) {
                  color = this.getOreColor(rb.block);
               } else {
                  color = (Color)this.customColor.get();
               }

               event.renderer.box(rb.pos, color, color, (ShapeMode)this.shapeMode.get(), 0);
               count++;
            }
         }
      }
   }

   public static enum Mode {
      Legit("Mode"),
      Blatant("PowerMode");

      private final String title;

      private Mode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }

   private record RenderBlock(BlockPos pos, Block block) {
   }
}
