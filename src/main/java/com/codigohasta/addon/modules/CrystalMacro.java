package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import com.codigohasta.addon.utils.KeyUtils;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class CrystalMacro extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> activateKey = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("activate-key")).description("Key that does the crystalling.")).defaultValue(1))
            .min(-1)
            .max(400)
            .build()
      );
   private final Setting<Double> placeDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("place-delay"))
               .description("The delay in ticks between placing crystals."))
            .defaultValue(0.3)
            .min(0.0)
            .max(20.0)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Double> breakDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("break-delay"))
               .description("The delay in ticks between breaking crystals."))
            .defaultValue(0.03)
            .min(0.0)
            .max(20.0)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Boolean> stopOnKill = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("stop-on-kill"))
                  .description("Pauses the macro when a nearby player dies, then resumes after 5 seconds."))
               .defaultValue(true))
            .build()
      );
   private long lastPlaceTime = 0L;
   private long lastBreakTime = 0L;
   private final Set<PlayerEntity> deadPlayers = new HashSet<>();
   private boolean paused = false;
   private long resumeTime = 0L;

   public CrystalMacro() {
      super(AddonTemplate.CATEGORY, "CrystalMacro", "Macro for fast crystal place/attack.");
   }

   public void onActivate() {
      this.lastPlaceTime = 0L;
      this.lastBreakTime = 0L;
      this.deadPlayers.clear();
      this.paused = false;
      this.resumeTime = 0L;
   }

   public void onDeactivate() {
      this.deadPlayers.clear();
      this.paused = false;
      this.resumeTime = 0L;
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.currentScreen == null) {
         if (this.paused && System.currentTimeMillis() >= this.resumeTime) {
            this.paused = false;
            if (this.mc.player != null) {
               this.mc.player.sendMessage(Text.literal("§7[§bLegitCrystalMacro§7] §aResumed after stop-on-kill"), false);
            }
         }

         if (!this.paused) {
            if (this.isKeyActive()) {
               if (!this.mc.player.isUsingItem()) {
                  String mainHandName = this.mc.player.getMainHandStack().getItem().toString().toLowerCase();
                  if (mainHandName.contains("end_crystal")) {
                     if ((Boolean)this.stopOnKill.get() && this.checkForDeadPlayers()) {
                        this.paused = true;
                        this.resumeTime = System.currentTimeMillis() + 5000L;
                        if (this.mc.player != null) {
                           this.mc
                              .player
                              .sendMessage(Text.literal("§7[§bLegitCrystalMacro§7] §cPaused due to player death (will resume in 5s)"), false);
                        }
                     } else {
                        this.handleInteraction();
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isKeyActive() {
      int d = (Integer)this.activateKey.get();
      return d == -1 || KeyUtils.isKeyPressed(d);
   }

   private void handleInteraction() {
      HitResult crosshairTarget = this.mc.crosshairTarget;
      if (crosshairTarget instanceof BlockHitResult blockHit) {
         this.handleBlockInteraction(blockHit);
      } else if (crosshairTarget instanceof EntityHitResult entityHit) {
         this.handleEntityInteraction(entityHit);
      }
   }

   private void handleBlockInteraction(BlockHitResult blockHitResult) {
      if (blockHitResult.getType() == Type.BLOCK) {
         long currentTime = System.currentTimeMillis();
         long placeDelayMs = (long)((Double)this.placeDelay.get() * 50.0);
         if (currentTime - this.lastPlaceTime >= placeDelayMs) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            String targetBlockName = this.mc.world.getBlockState(blockPos).getBlock().toString().toLowerCase();
            boolean isObsidianOrBedrock = targetBlockName.contains("obsidian") || targetBlockName.contains("bedrock");
            if (isObsidianOrBedrock && this.isValidCrystalPlacement(blockPos)) {
               this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, blockHitResult);
               this.mc.player.swingHand(Hand.MAIN_HAND);
               this.lastPlaceTime = currentTime;
            }
         }
      }
   }

   private void handleEntityInteraction(EntityHitResult entityHitResult) {
      long currentTime = System.currentTimeMillis();
      long breakDelayMs = (long)((Double)this.breakDelay.get() * 50.0);
      if (currentTime - this.lastBreakTime >= breakDelayMs) {
         Entity entity = entityHitResult.getEntity();
         String entityTypeStr = entity.getType().toString().toLowerCase();
         if (entityTypeStr.contains("end_crystal") || entityTypeStr.contains("slime")) {
            this.mc.interactionManager.attackEntity(this.mc.player, entity);
            this.mc.player.swingHand(Hand.MAIN_HAND);
            entity.discard();
            this.lastBreakTime = currentTime;
         }
      }
   }

   private boolean isValidCrystalPlacement(BlockPos blockPos) {
      BlockPos up = blockPos.up();
      if (!this.mc.world.isAir(up)) {
         return false;
      } else {
         int x = up.getX();
         int y = up.getY();
         int z = up.getZ();
         return this.mc.world.getOtherEntities(null, new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0)).isEmpty();
      }
   }

   private boolean checkForDeadPlayers() {
      if (this.mc.world == null) {
         return false;
      } else {
         for (PlayerEntity player : this.mc.world.getPlayers()) {
            if (player != this.mc.player) {
               String name = player.getGameProfile().name();
               if ((player.isDead() || player.getHealth() <= 0.0F) && !this.deadPlayers.contains(player)) {
                  this.deadPlayers.add(player);
                  return true;
               }
            }
         }

         this.deadPlayers.removeIf(p -> !p.isDead() && p.getHealth() > 0.0F);
         return false;
      }
   }
}
