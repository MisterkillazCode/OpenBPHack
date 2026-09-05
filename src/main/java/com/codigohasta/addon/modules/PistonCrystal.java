package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.mixin.InventoryAccessor;
import com.codigohasta.addon.utils.Timer;
import com.codigohasta.addon.utils.leaveshack.BlockUtil;
import com.codigohasta.addon.utils.leaveshack.CombatUtil;
import com.codigohasta.addon.utils.leaveshack.InventoryUtil;
import com.codigohasta.addon.utils.leaveshack.Rotation;
import java.util.ArrayList;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;

public class PistonCrystal extends Module {
   public static PistonCrystal INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("TargetRange")).description("Target Distance")).defaultValue(6.0).sliderRange(1.0, 6.0).build());
   private final Setting<Double> range = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("range")).description("PutDistance")).defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("delay-ms"))
                  .description("Place Delay"))
               .defaultValue(50))
            .sliderRange(0, 500)
            .build()
      );
   private final Setting<Integer> breakDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("breakDelay-ms"))
                  .description("Delay"))
               .defaultValue(300))
            .sliderRange(0, 500)
            .build()
      );
   private final Setting<Double> minDamage = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("MinDamage")).description("mostlittletoEnemyDamage")).defaultValue(4.0).sliderRange(1.0, 36.0).build());
   private final Setting<Double> maxSelfDmg = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("MaxSelfDmg")).description("mostbigSelfHurt")).defaultValue(12.0).sliderRange(1.0, 36.0).build());
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("UsingPause"))
                  .description("UsePause"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> onlyMain = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("OnlyMain"))
                     .description("CheckHand"))
                  .defaultValue(true))
               .visible(this.usingPause::get))
            .build()
      );
   private final Setting<Boolean> noSuicide = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("NoSuicide"))
                  .description("DefenseSelfkill"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("rotate"))
                  .description("Head"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> inventory = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("InventorySwap"))
                  .description("PackHand"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> mine = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("Mine"))
                  .description("Auto Mine"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> yawDeceive = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("YawDeceive"))
                  .description("Facing"))
               .defaultValue(true))
            .build()
      );
   private final Setting<PistonCrystal.RedstoneMode> redStoneMode = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("redStone"))
                  .description("RedMode"))
               .defaultValue(PistonCrystal.RedstoneMode.Block))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render"))
                  .description("Render"))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("shape-mode"))
                  .description("Mode"))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> crystalColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("crystal"))
               .description("WaterColor"))
            .defaultValue(new SettingColor(255, 0, 0, 80))
            .build()
      );
   private final Setting<SettingColor> pistonColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("piston"))
               .description("Color"))
            .defaultValue(new SettingColor(255, 255, 255, 80))
            .build()
      );
   private final Setting<SettingColor> redstoneColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("redstone"))
               .description("RedColor"))
            .defaultValue(new SettingColor(255, 100, 0, 80))
            .build()
      );
   private long lastAction = 0L;
   private BlockPos crystalPos;
   private BlockPos pistonPos;
   private BlockPos redstonePos;
   private BlockPos lastPiston;
   private BlockPos lastRedstone;
   private BlockPos lastCrystal;
   private Direction face;
   private final Timer breakTimer = new Timer();
   private PlayerEntity target;

   public PistonCrystal() {
      super(AddonTemplate.CATEGORY, "PistonCrystal", "Uses pistons to place crystals.");
      INSTANCE = this;
   }

   public void onActivate() {
      this.breakTimer.setMs(99999999L);
   }

   public String getInfoString() {
      return this.target == null ? null : "§f[" + this.target.getName().getString() + "]";
   }

   @EventHandler
   private void onTick(Post event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (System.currentTimeMillis() - this.lastAction >= ((Integer)this.delay.get()).intValue()) {
            this.target = CombatUtil.getClosestEnemy((Double)this.targetRange.get());
            int redStone = this.findRedstone();
            int crystal = this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
            int piston = this.inventory.get() ? InventoryUtil.findClassInventory(PistonBlock.class) : InventoryUtil.findClass(PistonBlock.class);
            if (!this.shouldPause()) {
               if (redStone != -1 && crystal != -1 && piston != -1) {
                  if (this.crystalPos != null
                     && this.redstonePos != null
                     && this.pistonPos != null
                     && (!BlockUtil.canPlaceCrystal(this.crystalPos) || !BlockUtil.canPlace(this.redstonePos) || !BlockUtil.canPlace(this.pistonPos))) {
                     this.pistonPos = null;
                     this.crystalPos = null;
                     this.redstonePos = null;
                  }

                  if (this.target != null) {
                     if (this.pistonPos == null && this.crystalPos == null && this.redstonePos == null) {
                        this.doPistonCrystal(this.target);
                     }

                     if (this.lastPiston != null
                        && BlockUtil.getBlock(this.lastPiston.offset(this.face.getOpposite())) instanceof PistonHeadBlock
                        && BlockUtil.getBlock(this.lastPiston) instanceof PistonBlock
                        && (Boolean)this.mine.get()) {
                        Direction side = BlockUtil.getClickSide(this.lastPiston);
                        this.mc.interactionManager.attackBlock(this.lastPiston, side);
                        this.lastPiston = null;
                        this.lastCrystal = null;
                        this.lastRedstone = null;
                     } else {
                        if (this.pistonPos != null && BlockUtil.canPlace(this.pistonPos)) {
                           this.place(Items.PISTON, piston, this.pistonPos, this.face);
                           this.lastPiston = this.pistonPos;
                        }

                        if (this.redstonePos != null && BlockUtil.canPlace(this.redstonePos)) {
                           if (this.redStoneMode.get() == PistonCrystal.RedstoneMode.Torch) {
                              this.placeTorch(this.redstonePos, redStone);
                           } else {
                              this.place(Items.REDSTONE_BLOCK, redStone, this.redstonePos, null);
                           }

                           this.lastRedstone = this.redstonePos;
                        }

                        if (this.crystalPos != null && BlockUtil.canPlaceCrystal(this.crystalPos)) {
                           this.placeCrystal(this.crystalPos, crystal);
                           this.lastCrystal = this.crystalPos;
                           this.lastAction = System.currentTimeMillis();
                        } else {
                           if (this.breakTimer.passedMs((long)((Integer)this.breakDelay.get()).intValue())) {
                              if (BlockUtil.hasCrystal(this.target.getBlockPos().up())) {
                                 CombatUtil.attackCrystal(this.target.getBlockPos().up(), true, false);
                                 this.lastAction = System.currentTimeMillis();
                                 this.pistonPos = null;
                                 this.crystalPos = null;
                                 this.redstonePos = null;
                                 this.breakTimer.reset();
                              }

                              if (BlockUtil.hasCrystal(this.target.getBlockPos().up(2))) {
                                 CombatUtil.attackCrystal(this.target.getBlockPos().up(2), true, false);
                                 this.lastAction = System.currentTimeMillis();
                                 this.pistonPos = null;
                                 this.crystalPos = null;
                                 this.redstonePos = null;
                                 this.breakTimer.reset();
                              }
                           }
                        }
                     }
                  }
               } else {
                  this.pistonPos = null;
                  this.crystalPos = null;
                  this.redstonePos = null;
               }
            }
         }
      }
   }

   private int findRedstone() {
      if (this.redStoneMode.get() == PistonCrystal.RedstoneMode.Torch) {
         return this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.REDSTONE_TORCH) : InventoryUtil.findItem(Items.REDSTONE_TORCH);
      } else {
         return this.inventory.get() ? InventoryUtil.findItemInventorySlot(Items.REDSTONE_BLOCK) : InventoryUtil.findItem(Items.REDSTONE_BLOCK);
      }
   }

   @EventHandler
   private void onRender(Render3DEvent e) {
      if ((Boolean)this.render.get()) {
         if (this.crystalPos != null) {
            e.renderer.box(this.crystalPos, (Color)this.crystalColor.get(), (Color)this.crystalColor.get(), (ShapeMode)this.shapeMode.get(), 0);
         }

         if (this.pistonPos != null) {
            e.renderer.box(this.pistonPos, (Color)this.pistonColor.get(), (Color)this.pistonColor.get(), (ShapeMode)this.shapeMode.get(), 0);
         }

         if (this.redstonePos != null) {
            e.renderer.box(this.redstonePos, (Color)this.redstoneColor.get(), (Color)this.redstoneColor.get(), (ShapeMode)this.shapeMode.get(), 0);
         }
      }
   }

   private void doPistonCrystal(PlayerEntity target) {
      BlockPos base = target.getBlockPos();
      BlockPos tempCrystalPos = null;
      BlockPos tempPistonPos = null;
      BlockPos tempRedstonePos = null;
      Vec3d vec = new Vec3d(base.up().getX() + 0.5, base.up().getY(), base.up().getZ() + 0.5);
      float damage1 = DamageUtils.crystalDamage(target, vec);
      float selfDmg1 = DamageUtils.crystalDamage(this.mc.player, vec);
      if (damage1 > (Double)this.minDamage.get() && selfDmg1 <= (Double)this.maxSelfDmg.get()) {
         for (Direction dir : Type.HORIZONTAL) {
            if ((Boolean)this.yawDeceive.get() || dir == this.mc.player.getHorizontalFacing()) {
               BlockPos temp1 = base.offset(dir).up();
               if (BlockUtil.canPlaceCrystal(temp1) && !(this.mc.player.getEyePos().distanceTo(temp1.toCenterPos()) > (Double)this.range.get())) {
                  label295:
                  for (Direction dir2 : Type.HORIZONTAL) {
                     if (dir2 != dir.getOpposite()) {
                        BlockPos temp2 = temp1.offset(dir).offset(dir2);
                        if (this.mc.world.isAir(temp2.offset(dir.getOpposite()))
                           || this.mc.world.getBlockState(temp2.offset(dir.getOpposite())).isReplaceable()) {
                           if (!BlockUtil.canPlace(temp2) && !(BlockUtil.getBlock(temp2) instanceof PistonBlock)) {
                              for (Direction help : Direction.values()) {
                                 if (help != dir.getOpposite()
                                    && BlockUtil.isGrimDirection(temp2.offset(help), help.getOpposite())
                                    && BlockUtil.canPlace(temp2.offset(help))
                                    && !(this.mc.player.getEyePos().distanceTo(temp2.offset(help).toCenterPos()) > (Double)this.range.get())) {
                                    BlockPos helpPos = temp2.offset(help);
                                    int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                                    Direction side = BlockUtil.getPlaceSide(helpPos, null);
                                    this.doSwap(this.findRedstone());
                                    BlockUtil.placeBlock(helpPos, side, (Boolean)this.rotate.get());
                                    if ((Boolean)this.inventory.get()) {
                                       this.doSwap(this.findRedstone());
                                    } else {
                                       this.doSwap(old);
                                    }

                                    return;
                                 }
                              }
                           } else if (!(this.mc.player.getEyePos().distanceTo(temp2.toCenterPos()) > (Double)this.range.get())) {
                              tempPistonPos = temp1.offset(dir).offset(dir2);

                              for (Direction dir3 : Direction.values()) {
                                 if (dir3 != dir.getOpposite()) {
                                    BlockPos temp3 = temp2.offset(dir3);
                                    if (BlockUtil.getBlock(temp3) instanceof RedstoneBlock && this.redStoneMode.get() == PistonCrystal.RedstoneMode.Block
                                       || BlockUtil.getBlock(temp3) instanceof RedstoneTorchBlock
                                          && this.redStoneMode.get() == PistonCrystal.RedstoneMode.Torch) {
                                       tempRedstonePos = tempPistonPos.offset(dir3);
                                       break label295;
                                    }

                                    if (BlockUtil.canPlace(temp3)
                                       && !(this.mc.player.getEyePos().distanceTo(temp3.toCenterPos()) > (Double)this.range.get())) {
                                       tempRedstonePos = tempPistonPos.offset(dir3);
                                       break label295;
                                    }
                                 }
                              }
                              break;
                           }
                        }
                     }
                  }

                  if (tempPistonPos == null) {
                     tempCrystalPos = null;
                     tempRedstonePos = null;
                  } else if (tempRedstonePos == null) {
                     tempCrystalPos = null;
                     tempPistonPos = null;
                  } else {
                     if (temp1 != null) {
                        if (selfDmg1 > EntityUtils.getTotalHealth(this.mc.player) && (Boolean)this.noSuicide.get()) {
                           return;
                        }

                        this.face = dir;
                        this.crystalPos = temp1;
                        this.pistonPos = tempPistonPos;
                        this.redstonePos = tempRedstonePos;
                        return;
                     }

                     tempPistonPos = null;
                     tempRedstonePos = null;
                  }
               }
            }
         }
      }

      Vec3d vec2 = new Vec3d(base.up(2).getX() + 0.5, base.up(2).getY(), base.up(2).getZ() + 0.5);
      float damage2 = DamageUtils.crystalDamage(target, vec2);
      float selfDmg2 = DamageUtils.crystalDamage(this.mc.player, vec2);
      if (!(selfDmg2 > EntityUtils.getTotalHealth(this.mc.player)) || !(Boolean)this.noSuicide.get()) {
         if (damage2 > (Double)this.minDamage.get()
            && selfDmg2 <= (Double)this.maxSelfDmg.get()
            && this.crystalPos == null
            && this.pistonPos == null
            && this.redstonePos == null) {
            for (Direction dirx : Type.HORIZONTAL) {
               if ((Boolean)this.yawDeceive.get() || dirx == this.mc.player.getHorizontalFacing()) {
                  BlockPos temp1 = base.offset(dirx).up(2);
                  if (BlockUtil.canPlaceCrystal(temp1) && !(this.mc.player.getEyePos().distanceTo(temp1.toCenterPos()) > (Double)this.range.get())) {
                     label225:
                     for (Direction dir2x : Type.HORIZONTAL) {
                        if (dir2x != dirx.getOpposite()) {
                           BlockPos temp2 = temp1.offset(dirx).offset(dir2x);
                           if (this.mc.world.isAir(temp2.offset(dirx.getOpposite()))
                              || this.mc.world.getBlockState(temp2.offset(dirx.getOpposite())).isReplaceable()) {
                              if (!BlockUtil.canPlace(temp2) && !(BlockUtil.getBlock(temp2) instanceof PistonBlock)) {
                                 for (Direction helpx : Direction.values()) {
                                    if (helpx != dirx.getOpposite()
                                       && BlockUtil.isGrimDirection(temp2.offset(helpx), helpx.getOpposite())
                                       && BlockUtil.canPlace(temp2.offset(helpx))
                                       && !(this.mc.player.getEyePos().distanceTo(temp2.offset(helpx).toCenterPos()) > (Double)this.range.get())
                                       )
                                     {
                                       BlockPos helpPos = temp2.offset(helpx);
                                       int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                                       Direction side = BlockUtil.getPlaceSide(helpPos, null);
                                       this.doSwap(this.findRedstone());
                                       BlockUtil.placeBlock(helpPos, side, (Boolean)this.rotate.get());
                                       if ((Boolean)this.inventory.get()) {
                                          this.doSwap(this.findRedstone());
                                       } else {
                                          this.doSwap(old);
                                       }

                                       return;
                                    }
                                 }
                              } else if (!(this.mc.player.getEyePos().distanceTo(temp2.toCenterPos()) > (Double)this.range.get())) {
                                 tempPistonPos = temp1.offset(dirx).offset(dir2x);

                                 for (Direction dir3x : Direction.values()) {
                                    if (dir3x != dirx.getOpposite()) {
                                       BlockPos temp3x = temp2.offset(dir3x);
                                       if (BlockUtil.getBlock(temp3x) instanceof RedstoneBlock && this.redStoneMode.get() == PistonCrystal.RedstoneMode.Block
                                          || BlockUtil.getBlock(temp3x) instanceof RedstoneTorchBlock
                                             && this.redStoneMode.get() == PistonCrystal.RedstoneMode.Torch) {
                                          tempRedstonePos = tempPistonPos.offset(dir3x);
                                          break label225;
                                       }

                                       if (BlockUtil.canPlace(temp3x)
                                          && !(this.mc.player.getEyePos().distanceTo(temp3x.toCenterPos()) > (Double)this.range.get())) {
                                          tempRedstonePos = tempPistonPos.offset(dir3x);
                                          break label225;
                                       }
                                    }
                                 }
                                 break;
                              }
                           }
                        }
                     }

                     if (tempPistonPos == null) {
                        tempCrystalPos = null;
                        tempRedstonePos = null;
                     } else if (tempRedstonePos == null) {
                        tempCrystalPos = null;
                        tempPistonPos = null;
                     } else {
                        if (temp1 != null) {
                           if (selfDmg1 > EntityUtils.getTotalHealth(this.mc.player) && (Boolean)this.noSuicide.get()) {
                              return;
                           }

                           this.face = dirx;
                           this.crystalPos = temp1;
                           this.pistonPos = tempPistonPos;
                           this.redstonePos = tempRedstonePos;
                           return;
                        }

                        tempPistonPos = null;
                        tempRedstonePos = null;
                     }
                  }
               }
            }
         }
      }
   }

   private void place(Item item, int slot, BlockPos pos, Direction dir) {
      Direction side = BlockUtil.getPlaceSide(pos, d -> true);
      if (side != null) {
         if (slot != -1) {
            int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            this.doSwap(slot);
            if ((Boolean)this.rotate.get()) {
               Rotation.snapAt(
                  pos.toCenterPos()
                     .add(
                        new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5)
                     )
               );
            }

            if (item == Items.PISTON && (Boolean)this.yawDeceive.get() && (Boolean)this.rotate.get()) {
               pistonFacing(dir);
            }

            BlockUtil.placeBlock(pos, side, false);
            if ((Boolean)this.rotate.get()) {
               Rotation.snapBack();
            }

            if ((Boolean)this.inventory.get()) {
               this.doSwap(slot);
            } else {
               this.doSwap(old);
            }
         }
      }
   }

   private boolean shouldPause() {
      return !(Boolean)this.usingPause.get() || this.checkPause((Boolean)this.onlyMain.get());
   }

   public boolean checkPause(boolean onlyMain) {
      return (this.mc.options.useKey.isPressed() || this.mc.player.isUsingItem())
         && (!onlyMain || this.mc.player.getActiveHand() == Hand.MAIN_HAND);
   }

   public static void pistonFacing(Direction i) {
      if (i == Direction.EAST) {
         Rotation.snapAt(-90.0F, 5.0F);
      } else if (i == Direction.WEST) {
         Rotation.snapAt(90.0F, 5.0F);
      } else if (i == Direction.NORTH) {
         Rotation.snapAt(180.0F, 5.0F);
      } else if (i == Direction.SOUTH) {
         Rotation.snapAt(0.0F, 5.0F);
      }
   }

   private void placeCrystal(BlockPos pos, int slot) {
      BlockPos base = pos.down();
      Direction side = BlockUtil.getClickSide(base);
      if (side != null) {
         if (slot != -1) {
            int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
            this.doSwap(slot);
            BlockUtil.clickBlock(base, side, (Boolean)this.rotate.get());
            if ((Boolean)this.inventory.get()) {
               this.doSwap(slot);
            } else {
               this.doSwap(old);
            }
         }
      }
   }

   private void placeTorch(BlockPos pos, int slot) {
      if (BlockUtil.canPlace(pos)) {
         ArrayList<Direction> sides = BlockUtil.getPlaceSides(pos, null);
         if (!sides.isEmpty()) {
            for (Direction side : sides) {
               if (!(BlockUtil.getBlock(pos.offset(side)) instanceof PistonBlock) && side != Direction.UP) {
                  if (slot == -1) {
                     return;
                  }

                  int old = ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot();
                  this.doSwap(slot);
                  BlockUtil.placeBlock(pos, side, (Boolean)this.rotate.get());
                  if ((Boolean)this.inventory.get()) {
                     this.doSwap(slot);
                  } else {
                     this.doSwap(old);
                  }

                  return;
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (slot != -1) {
         if (!(Boolean)this.inventory.get()) {
            InventoryUtil.switchToSlot(slot);
         } else {
            InventoryUtil.inventorySwap(slot, ((InventoryAccessor)this.mc.player.getInventory()).getSelectedSlot());
         }
      }
   }

   public static enum RedstoneMode {
      Torch,
      Block;
   }
}
