package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public class PortalESP extends Module {
   private final SettingGroup sgNether = this.settings.createGroup("downSend");
   private final SettingGroup sgEnd = this.settings.createGroup("GroundSend");
   private final SettingGroup sgGateway = this.settings.createGroup("Ground");
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final Setting<Boolean> netherEnabled = this.sgNether
      .add(((Builder)((Builder)((Builder)new Builder().name("EnabledownSend")).description("DisplaydownSendBlock'sViewBox.")).defaultValue(true)).build());
   private final Setting<ShapeMode> netherShapeMode = this.sgNether
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Render Mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> netherSideColor = this.sgNether
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Fill Color"))
               .defaultValue(new SettingColor(200, 0, 255, 50))
               .visible(() -> this.netherShapeMode.get() != ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> netherLineColor = this.sgNether
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("BoxColor"))
               .defaultValue(new SettingColor(200, 0, 255, 255))
               .visible(() -> this.netherShapeMode.get() != ShapeMode.Sides))
            .build()
      );
   private final Setting<Boolean> endEnabled = this.sgEnd
      .add(((Builder)((Builder)((Builder)new Builder().name("EnableGroundSend")).description("DisplayGroundSendBlock'sViewBox.")).defaultValue(true)).build());
   private final Setting<ShapeMode> endShapeMode = this.sgEnd
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Render Mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> endSideColor = this.sgEnd
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Fill Color"))
               .defaultValue(new SettingColor(0, 255, 200, 50))
               .visible(() -> this.endShapeMode.get() != ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> endLineColor = this.sgEnd
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("BoxColor"))
               .defaultValue(new SettingColor(0, 255, 200, 255))
               .visible(() -> this.endShapeMode.get() != ShapeMode.Sides))
            .build()
      );
   private final Setting<Boolean> gatewayEnabled = this.sgGateway
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("EnableGround")).description("DisplayGroundBlock(End Gateway)'sViewBox.")).defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> gatewayShapeMode = this.sgGateway
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                  .name("Render Mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> gatewaySideColor = this.sgGateway
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Fill Color"))
               .defaultValue(new SettingColor(255, 255, 0, 50))
               .visible(() -> this.gatewayShapeMode.get() != ShapeMode.Lines))
            .build()
      );
   private final Setting<SettingColor> gatewayLineColor = this.sgGateway
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("BoxColor"))
               .defaultValue(new SettingColor(255, 255, 0, 255))
               .visible(() -> this.gatewayShapeMode.get() != ShapeMode.Sides))
            .build()
      );
   private final Setting<Integer> renderDistance = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("RenderDistance"))
                  .description("OnlyRenderDistanceinside'sSendBlock."))
               .defaultValue(128))
            .min(32)
            .max(512)
            .build()
      );
   private final Set<BlockPos> netherPortals = Collections.synchronizedSet(new HashSet<>());
   private final Set<BlockPos> endPortals = Collections.synchronizedSet(new HashSet<>());
   private final Set<BlockPos> gateways = Collections.synchronizedSet(new HashSet<>());

   public PortalESP() {
      super(AddonTemplate.SC_CATEGORY, "PortalESP", "Highlights portal frames and ground block boxes.");
   }

   public void onActivate() {
      this.clearPortals();
      this.reloadChunks();
   }

   public void onDeactivate() {
      this.clearPortals();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.clearPortals();
   }

   private void reloadChunks() {
      if (this.mc.world != null && this.mc.player != null) {
         int renderDist = this.mc.options.getClampedViewDistance();
         ChunkPos playerPos = this.mc.player.getChunkPos();

         for (int x = -renderDist; x <= renderDist; x++) {
            for (int z = -renderDist; z <= renderDist; z++) {
               WorldChunk chunk = this.mc.world.getChunk(playerPos.x + x, playerPos.z + z);
               if (chunk != null && !chunk.isEmpty()) {
                  this.scanChunk(chunk);
               }
            }
         }
      }
   }

   @EventHandler
   private void onChunkData(ChunkDataEvent event) {
      if (this.mc.world != null) {
         WorldChunk chunk = event.chunk();
         if (chunk != null) {
            this.scanChunk(chunk);
         }
      }
   }

   @EventHandler
   private void onBlockUpdate(BlockUpdateEvent event) {
      BlockPos pos = event.pos;
      BlockState newState = event.newState;
      BlockState oldState = event.oldState;
      if (oldState.getBlock() == Blocks.NETHER_PORTAL && newState.getBlock() != Blocks.NETHER_PORTAL) {
         this.netherPortals.remove(pos);
      } else if (newState.getBlock() == Blocks.NETHER_PORTAL) {
         this.netherPortals.add(pos.toImmutable());
      }

      if (oldState.getBlock() == Blocks.END_PORTAL && newState.getBlock() != Blocks.END_PORTAL) {
         this.endPortals.remove(pos);
      } else if (newState.getBlock() == Blocks.END_PORTAL) {
         this.endPortals.add(pos.toImmutable());
      }

      if (oldState.getBlock() == Blocks.END_GATEWAY && newState.getBlock() != Blocks.END_GATEWAY) {
         this.gateways.remove(pos);
      } else if (newState.getBlock() == Blocks.END_GATEWAY) {
         this.gateways.add(pos.toImmutable());
      }
   }

   private void scanChunk(WorldChunk chunk) {
      ChunkSection[] sections = chunk.getSectionArray();
      int chunkX = chunk.getPos().x;
      int chunkZ = chunk.getPos().z;

      for (int i = 0; i < sections.length; i++) {
         ChunkSection section = sections[i];
         if (!section.isEmpty()) {
            int yOffset = chunk.sectionIndexToCoord(i) << 4;

            for (int x = 0; x < 16; x++) {
               for (int y = 0; y < 16; y++) {
                  for (int z = 0; z < 16; z++) {
                     BlockState state = section.getBlockState(x, y, z);
                     Block block = state.getBlock();
                     if (block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL || block == Blocks.END_GATEWAY) {
                        BlockPos pos = new BlockPos(chunkX * 16 + x, yOffset + y, chunkZ * 16 + z);
                        if (block == Blocks.NETHER_PORTAL) {
                           this.netherPortals.add(pos);
                        } else if (block == Blocks.END_PORTAL) {
                           this.endPortals.add(pos);
                        } else if (block == Blocks.END_GATEWAY) {
                           this.gateways.add(pos);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void clearPortals() {
      this.netherPortals.clear();
      this.endPortals.clear();
      this.gateways.clear();
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.player != null) {
         if ((Boolean)this.netherEnabled.get()) {
            synchronized (this.netherPortals) {
               this.renderSet(
                  event,
                  this.netherPortals,
                  (SettingColor)this.netherSideColor.get(),
                  (SettingColor)this.netherLineColor.get(),
                  (ShapeMode)this.netherShapeMode.get()
               );
            }
         }

         if ((Boolean)this.endEnabled.get()) {
            synchronized (this.endPortals) {
               this.renderSet(
                  event, this.endPortals, (SettingColor)this.endSideColor.get(), (SettingColor)this.endLineColor.get(), (ShapeMode)this.endShapeMode.get()
               );
            }
         }

         if ((Boolean)this.gatewayEnabled.get()) {
            synchronized (this.gateways) {
               this.renderSet(
                  event,
                  this.gateways,
                  (SettingColor)this.gatewaySideColor.get(),
                  (SettingColor)this.gatewayLineColor.get(),
                  (ShapeMode)this.gatewayShapeMode.get()
               );
            }
         }
      }
   }

   private void renderSet(Render3DEvent event, Set<BlockPos> positions, SettingColor sideColor, SettingColor lineColor, ShapeMode shapeMode) {
      int rangeSq = (Integer)this.renderDistance.get() * (Integer)this.renderDistance.get();
      Vec3d cameraPos = this.mc.player.getEyePos();
      double cameraX = cameraPos.x;
      double cameraY = cameraPos.y;
      double cameraZ = cameraPos.z;

      for (BlockPos pos : positions) {
         double distSq = pos.getSquaredDistance(cameraX, cameraY, cameraZ);
         if (!(distSq > rangeSq)) {
            event.renderer.box(pos, sideColor, lineColor, shapeMode, 0);
         }
      }
   }
}
