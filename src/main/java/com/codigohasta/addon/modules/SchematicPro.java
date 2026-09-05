package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SchematicPro extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("RenderSetting");
   private final Setting<SchematicPro.OperationMode> operationMode = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("OperationMode")).defaultValue(SchematicPro.OperationMode.Automatic)).build());
   private final Setting<Integer> interactRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("ExchangeRange"))
                     .description("OnlyhaveatthisRangeinside'sRedColorTolerateDevicewillbyAutoOpen."))
                  .defaultValue(5))
               .min(1)
               .sliderMax(8)
               .visible(() -> this.operationMode.get() == SchematicPro.OperationMode.Automatic))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("InstallDelay (Tick)"))
                  .description("InstallSpeed, Suggest 4-5 ."))
               .defaultValue(4))
            .min(2)
            .build()
      );
   private final Setting<Boolean> autoClose = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("CompleteAutoDisable"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> renderRange = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("RenderRange"))
                  .description("canlooktoTolerateDevice'sRange (BlueColorPre)."))
               .defaultValue(128))
            .min(8)
            .sliderMax(256)
            .build()
      );
   private final Setting<SettingColor> previewSideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Pre()-FaceColor"))
               .description("needgoInstall, DistancetooCannotExchange."))
            .defaultValue(new SettingColor(0, 100, 255, 25))
            .build()
      );
   private final Setting<SettingColor> previewLineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("Pre()-LineColor"))
            .defaultValue(new SettingColor(0, 100, 255, 150))
            .build()
      );
   private final Setting<SettingColor> targetSideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("Target()-FaceColor"))
               .description("atExchangeRangeinside, insideTolerate(byAutoOpen)."))
            .defaultValue(new SettingColor(255, 0, 0, 40))
            .build()
      );
   private final Setting<SettingColor> targetLineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("Target()-LineColor"))
            .defaultValue(new SettingColor(255, 0, 0, 200))
            .build()
      );
   private final Setting<SettingColor> matchSideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("AlreadyCompleteComplete-FaceColor"))
            .defaultValue(new SettingColor(0, 255, 0, 25))
            .build()
      );
   private final Setting<SettingColor> matchLineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
               .name("AlreadyCompleteComplete-LineColor"))
            .defaultValue(new SettingColor(0, 255, 0, 150))
            .build()
      );
   private int timer = 0;
   private BlockPos currentTarget = null;
   private boolean isWorking = false;
   private final Set<BlockPos> finishedCache = new HashSet<>();

   public SchematicPro() {
      super(AddonTemplate.SC_CATEGORY, "SchematicPro", "Builds from a schematic by auto-filling containers and swapping items, with a render preview.");
   }

   public void onActivate() {
      this.finishedCache.clear();
      this.isWorking = false;
      this.currentTarget = null;
   }

   public WWidget getWidget(GuiTheme theme) {
      WButton btn = theme.button("ResethaveEnterDegree");
      btn.action = () -> this.finishedCache.clear();
      return btn;
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (this.operationMode.get() == SchematicPro.OperationMode.Manual) {
         if (this.mc.player.isCreative()) {
            BlockPos pos = event.result.getBlockPos();
            if (this.mc.world.getBlockEntity(pos) instanceof LootableContainerBlockEntity) {
               if (this.shouldSkipContainer(pos)) {
                  return;
               }

               this.currentTarget = pos;
               this.isWorking = true;
               this.timer = (Integer)this.delay.get();
            }
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.isWorking && this.currentTarget != null) {
            this.handleFillingInGui();
         } else {
            if (this.operationMode.get() == SchematicPro.OperationMode.Automatic) {
               if (this.timer > 0) {
                  this.timer--;
                  return;
               }

               this.findAndOpenTarget();
            }
         }
      }
   }

   private void findAndOpenTarget() {
      Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
      double interactSq = Math.pow(((Integer)this.interactRange.get()).intValue(), 2.0);

      for (BlockEntity be : Utils.blockEntities()) {
         BlockPos pos = be.getPos();
         if (!(playerPos.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > interactSq)
            && be instanceof LootableContainerBlockEntity
            && !this.finishedCache.contains(pos)
            && !this.shouldSkipContainer(pos)) {
            if (this.needsFixing(pos)) {
               this.currentTarget = pos;
               this.isWorking = true;
               this.timer = (Integer)this.delay.get();
               this.mc
                  .interactionManager
                  .interactBlock(
                     this.mc.player,
                     Hand.MAIN_HAND,
                     new BlockHitResult(
                        new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, false
                     )
                  );
               return;
            }

            this.finishedCache.add(pos);
         }
      }
   }

   private boolean shouldSkipContainer(BlockPos pos) {
      WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
      if (schematicWorld == null) {
         return true;
      } else if (!(schematicWorld.getBlockEntity(pos) instanceof Inventory schemInv)) {
         return true;
      } else {
         boolean isEmpty = true;

         for (int i = 0; i < schemInv.size(); i++) {
            if (!schemInv.getStack(i).isEmpty()) {
               isEmpty = false;
               break;
            }
         }

         return isEmpty;
      }
   }

   private boolean needsFixing(BlockPos pos) {
      WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
      if (schematicWorld == null) {
         return false;
      } else {
         BlockEntity realBe = this.mc.world.getBlockEntity(pos);
         BlockEntity schemBe = schematicWorld.getBlockEntity(pos);
         if (realBe instanceof Inventory realInv && schemBe instanceof Inventory schemInv) {
            for (int i = 0; i < schemInv.size(); i++) {
               ItemStack need = schemInv.getStack(i);
               ItemStack have = realInv.getStack(i);
               if ((!need.isEmpty() || !have.isEmpty()) && !ItemStack.areEqual(need, have)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void handleFillingInGui() {
      if (this.mc.currentScreen instanceof HandledScreen<?> screen) {
         if (this.timer > 0) {
            this.timer--;
         } else {
            WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
            if (schematicWorld != null && this.currentTarget != null) {
               if (!(schematicWorld.getBlockEntity(this.currentTarget) instanceof Inventory schemInv)) {
                  this.closeContainer();
               } else {
                  ScreenHandler handler = screen.getScreenHandler();
                  boolean allCorrect = true;

                  for (int i = 0; i < schemInv.size() && i < handler.slots.size(); i++) {
                     ItemStack targetStack = schemInv.getStack(i);
                     ItemStack realStack = handler.getSlot(i).getStack();
                     if (!ItemStack.areEqual(targetStack, realStack)) {
                        allCorrect = false;
                        if (!targetStack.isEmpty()) {
                           ItemStack stackToSend = targetStack.copy();
                           this.mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36, stackToSend));
                           this.mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.SWAP, this.mc.player);
                           this.timer = (Integer)this.delay.get();
                           return;
                        }
                     }
                  }

                  if (allCorrect) {
                     this.finishedCache.add(this.currentTarget);
                     if ((Boolean)this.autoClose.get()) {
                        this.closeContainer();
                     } else {
                        this.isWorking = false;
                        this.currentTarget = null;
                     }
                  }
               }
            } else {
               this.closeContainer();
            }
         }
      } else {
         this.isWorking = false;
         this.currentTarget = null;
      }
   }

   private void closeContainer() {
      if (this.mc.player != null) {
         this.mc.player.closeHandledScreen();
      }

      this.isWorking = false;
      this.currentTarget = null;
      this.timer = (Integer)this.delay.get();
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
         if (schematicWorld != null) {
            Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
            double renderSq = Math.pow(((Integer)this.renderRange.get()).intValue(), 2.0);
            double interactSq = Math.pow(((Integer)this.interactRange.get()).intValue(), 2.0);

            for (BlockEntity be : Utils.blockEntities()) {
               BlockPos pos = be.getPos();
               double distSq = playerPos.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
               if (!(distSq > renderSq) && be instanceof LootableContainerBlockEntity && !this.shouldSkipContainer(pos)) {
                  if (this.finishedCache.contains(pos)) {
                     event.renderer.box(pos, (Color)this.matchSideColor.get(), (Color)this.matchLineColor.get(), ShapeMode.Both, 0);
                  } else if (!this.needsFixing(pos)) {
                     this.finishedCache.add(pos);
                     event.renderer.box(pos, (Color)this.matchSideColor.get(), (Color)this.matchLineColor.get(), ShapeMode.Both, 0);
                  } else if (distSq <= interactSq) {
                     event.renderer.box(pos, (Color)this.targetSideColor.get(), (Color)this.targetLineColor.get(), ShapeMode.Both, 0);
                  } else {
                     event.renderer.box(pos, (Color)this.previewSideColor.get(), (Color)this.previewLineColor.get(), ShapeMode.Both, 0);
                  }
               }
            }
         }
      }
   }

   public static enum OperationMode {
      Automatic("AutoScanOpen"),
      Manual("ManualRightKeyTrigger");

      private final String title;

      private OperationMode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
