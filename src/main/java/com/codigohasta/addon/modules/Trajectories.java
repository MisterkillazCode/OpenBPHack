package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.BreezeWindChargeEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.LingeringPotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jetbrains.annotations.Nullable;

public class Trajectories extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> handBow = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("HandBow")).description("HandHoldTimeDisplayThingLinePreTest")).defaultValue(true)).build());
   private final Setting<SettingColor> handBowColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("HandBowColor"))
                  .description("'sThingLineColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.handBow::get))
            .build()
      );
   private final Setting<Boolean> handCrossbow = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("HandCrossbow")).description("HandHoldTimeDisplayThingLinePreTest")).defaultValue(true)).build());
   private final Setting<SettingColor> handCrossbowColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("HandCrossbowColor"))
                  .description("'sThingLineColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.handCrossbow::get))
            .build()
      );
   private final Setting<Boolean> handPearl = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("HandPearl")).description("HandHoldTimeDisplayThingLinePreTest")).defaultValue(true)).build());
   private final Setting<SettingColor> handPearlColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("HandPearlColor"))
                  .description("ThingLineColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.handPearl::get))
            .build()
      );
   private final Setting<Boolean> handTrident = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("HandTrident")).description("HandHoldTridentTimeDisplayThingLinePreTest")).defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> handTridentColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("HandTridentColor"))
                  .description("TridentThingLineColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.handTrident::get))
            .build()
      );
   private final Setting<Boolean> handThrowable = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("HandThrowable")).description("HandHold//Potion/TimeDisplayThingLinePreTest")).defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> handThrowableColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("HandThrowableColor"))
                  .description("ThingThingLineColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.handThrowable::get))
            .build()
      );
   private final Setting<Boolean> pearlEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Pearl")).description("Display'sThingLine")).defaultValue(true)).build());
   private final Setting<SettingColor> pearlColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("PearlColor"))
                  .description("Color"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.pearlEnabled::get))
            .build()
      );
   private final Setting<Boolean> arrowEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Arrow")).description("DisplayArrow'sThingLine")).defaultValue(true)).build());
   private final Setting<SettingColor> arrowColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("ArrowColor"))
                  .description("ArrowColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.arrowEnabled::get))
            .build()
      );
   private final Setting<Boolean> xpEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("XP")).description("Display'sThingLine")).defaultValue(true)).build());
   private final Setting<SettingColor> xpColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("XPColor"))
                  .description("Color"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.xpEnabled::get))
            .build()
      );
   private final Setting<Boolean> windChargeEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("WindCharge")).description("Display'sThingLine")).defaultValue(true)).build());
   private final Setting<SettingColor> windChargeColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("WindChargeColor"))
                  .description("Color"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.windChargeEnabled::get))
            .build()
      );
   private final Setting<Boolean> throwableEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Throwable")).description("Display//Potion'sThingLine")).defaultValue(true)).build());
   private final Setting<SettingColor> throwableColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("ThrowableColor"))
                  .description("//PotionColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.throwableEnabled::get))
            .build()
      );
   private final Setting<Boolean> tridentEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Trident")).description("DisplayTrident'sThingLine")).defaultValue(true)).build());
   private final Setting<SettingColor> tridentColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("TridentColor"))
                  .description("TridentColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.tridentEnabled::get))
            .build()
      );
   private final Setting<Boolean> otherEnabled = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("Other")).description("DisplayOtherShootThing's(Arrow////Head/)")).defaultValue(true)).build());
   private final Setting<SettingColor> otherColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("OtherColor"))
                  .description("OtherShootThingColor"))
               .defaultValue(new SettingColor(255, 255, 255, 255))
               .visible(this.otherEnabled::get))
            .build()
      );

   public Trajectories() {
      super(AddonTemplate.CATEGORY, "Trajectories", "Draws the predicted flight path of bows, crossbows, pearls, tridents and throwables.");
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         this.renderFlyingProjectiles(event);
         this.renderHandTrajectory(event);
      }
   }

   private void renderFlyingProjectiles(Render3DEvent event) {
      if ((Boolean)this.pearlEnabled.get()
         || (Boolean)this.arrowEnabled.get()
         || (Boolean)this.xpEnabled.get()
         || (Boolean)this.windChargeEnabled.get()
         || (Boolean)this.throwableEnabled.get()
         || (Boolean)this.tridentEnabled.get()
         || (Boolean)this.otherEnabled.get()) {
         for (Entity en : this.mc.world.getEntities()) {
            if (en instanceof EnderPearlEntity && (Boolean)this.pearlEnabled.get()) {
               this.calcTrajectory(en, (SettingColor)this.pearlColor.get(), event, true);
            } else if (en instanceof ExperienceBottleEntity && (Boolean)this.xpEnabled.get()) {
               this.calcTrajectory(en, (SettingColor)this.xpColor.get(), event, false);
            } else if (en instanceof ArrowEntity && (Boolean)this.arrowEnabled.get()) {
               this.calcTrajectory(en, (SettingColor)this.arrowColor.get(), event, true);
            } else if ((en instanceof WindChargeEntity || en instanceof BreezeWindChargeEntity) && (Boolean)this.windChargeEnabled.get()) {
               this.calcTrajectory(en, (SettingColor)this.windChargeColor.get(), event, false);
            } else if ((en instanceof SnowballEntity || en instanceof EggEntity || en instanceof SplashPotionEntity || en instanceof LingeringPotionEntity)
               && (Boolean)this.throwableEnabled.get()) {
               this.calcTrajectory(en, (SettingColor)this.throwableColor.get(), event, false);
            } else if (en instanceof TridentEntity && (Boolean)this.tridentEnabled.get()) {
               this.calcTrajectory(en, (SettingColor)this.tridentColor.get(), event, true);
            } else if ((Boolean)this.otherEnabled.get() && this.isOtherProjectile(en)) {
               this.calcTrajectory(en, (SettingColor)this.otherColor.get(), event, false);
            }
         }
      }
   }

   private boolean isOtherProjectile(Entity en) {
      return en instanceof SpectralArrowEntity
         || en instanceof FireworkRocketEntity
         || en instanceof FishingBobberEntity
         || en instanceof LlamaSpitEntity
         || en instanceof ShulkerBulletEntity
         || en instanceof FireballEntity
         || en instanceof SmallFireballEntity
         || en instanceof WitherSkullEntity
         || en instanceof DragonFireballEntity;
   }

   private void renderHandTrajectory(Render3DEvent event) {
      if (this.mc.options.getPerspective().isFirstPerson()) {
         for (Hand checkHand : new Hand[]{Hand.MAIN_HAND, Hand.OFF_HAND}) {
            ItemStack stack = checkHand == Hand.MAIN_HAND ? this.mc.player.getMainHandStack() : this.mc.player.getOffHandStack();
            Item item = stack.getItem();
            SettingColor color = this.getHandColorForItem(item);
            if (color != null) {
               float tickDelta = event.tickDelta;
               double x = MathHelper.lerp(tickDelta, this.mc.player.lastRenderX, this.mc.player.getX());
               double y = MathHelper.lerp(tickDelta, this.mc.player.lastRenderY, this.mc.player.getY());
               double z = MathHelper.lerp(tickDelta, this.mc.player.lastRenderZ, this.mc.player.getZ());
               if (item instanceof CrossbowItem) {
                  Registry<Enchantment> registry = this.mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                  boolean multishot = EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.MULTISHOT), stack) != 0;
                  if (multishot) {
                     this.calcTrajectory(item, this.mc.player.getYaw() - 10.0F, x, y, z, color, event);
                     this.calcTrajectory(item, this.mc.player.getYaw(), x, y, z, color, event);
                     this.calcTrajectory(item, this.mc.player.getYaw() + 10.0F, x, y, z, color, event);
                  } else {
                     this.calcTrajectory(item, this.mc.player.getYaw(), x, y, z, color, event);
                  }
               } else {
                  this.calcTrajectory(item, this.mc.player.getYaw(), x, y, z, color, event);
               }

               return;
            }
         }
      }
   }

   @Nullable
   private SettingColor getHandColorForItem(Item item) {
      if (item instanceof BowItem && (Boolean)this.handBow.get()) {
         return (SettingColor)this.handBowColor.get();
      } else if (item instanceof CrossbowItem && (Boolean)this.handCrossbow.get()) {
         return (SettingColor)this.handCrossbowColor.get();
      } else if (item instanceof EnderPearlItem && (Boolean)this.handPearl.get()) {
         return (SettingColor)this.handPearlColor.get();
      } else if (item instanceof TridentItem && (Boolean)this.handTrident.get()) {
         return (SettingColor)this.handTridentColor.get();
      } else {
         return (
                  item instanceof ExperienceBottleItem
                     || item instanceof SnowballItem
                     || item instanceof EggItem
                     || item instanceof SplashPotionItem
                     || item instanceof LingeringPotionItem
               )
               && this.handThrowable.get()
            ? (SettingColor)this.handThrowableColor.get()
            : null;
      }
   }

   private void calcTrajectory(Entity e, SettingColor color, Render3DEvent event, boolean arrowPhysics) {
      double motionX = e.getVelocity().x;
      double motionY = e.getVelocity().y;
      double motionZ = e.getVelocity().z;
      if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
         boolean noDragNoGravity = e instanceof WitherSkullEntity;
         boolean noGravity = e instanceof WindChargeEntity || e instanceof BreezeWindChargeEntity;
         double x = e.getX();
         double y = e.getY();
         double z = e.getZ();

         for (int i = 0; i < 300; i++) {
            Vec3d lastPos = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            if (!noDragNoGravity) {
               if (this.mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() == Blocks.WATER) {
                  motionX *= 0.8;
                  motionY *= 0.8;
                  motionZ *= 0.8;
               } else {
                  motionX *= 0.99;
                  motionY *= 0.99;
                  motionZ *= 0.99;
               }
            }

            if (!noDragNoGravity && !noGravity) {
               motionY -= arrowPhysics ? 0.05F : 0.03F;
            }

            Vec3d pos = new Vec3d(x, y, z);
            if (y <= -65.0) {
               break;
            }

            BlockHitResult bhr = this.mc
               .world
               .raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, this.mc.player));
            if (bhr != null && (bhr.getType() == Type.BLOCK || bhr.getType() == Type.ENTITY)) {
               break;
            }

            int alpha = MathHelper.clamp((int)(255.0F * ((i + 1) / 10.0F)), 0, 255);
            event.renderer
               .line(
                  lastPos.x,
                  lastPos.y,
                  lastPos.z,
                  pos.x,
                  pos.y,
                  pos.z,
                  new SettingColor(color.r, color.g, color.b, alpha)
               );
         }
      }
   }

   private void calcTrajectory(Item item, float yaw, double x, double y, double z, SettingColor color, Render3DEvent event) {
      y = y + this.mc.player.getEyeHeight(this.mc.player.getPose()) - 0.1000000014901161;
      if (item == this.mc.player.getMainHandStack().getItem()) {
         x -= MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
         z -= MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
      } else {
         x += MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
         z += MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
      }

      float maxDist = this.getDistance(item);
      double motionX = -MathHelper.sin(yaw / 180.0F * (float) Math.PI)
         * MathHelper.cos(this.mc.player.getPitch() / 180.0F * (float) Math.PI)
         * maxDist;
      double motionY = -MathHelper.sin((this.mc.player.getPitch() - this.getThrowPitch(item)) / 180.0F * 3.141593F) * maxDist;
      double motionZ = MathHelper.cos(yaw / 180.0F * (float) Math.PI)
         * MathHelper.cos(this.mc.player.getPitch() / 180.0F * (float) Math.PI)
         * maxDist;
      float power = this.mc.player.getItemUseTime() / 20.0F;
      power = (power * power + power * 2.0F) / 3.0F;
      if (power > 1.0F) {
         power = 1.0F;
      }

      float distance = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= distance;
      motionY /= distance;
      motionZ /= distance;
      float pow = (item instanceof BowItem ? power * 2.0F : (item instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getThrowVelocity(item);
      motionX *= pow;
      motionY *= pow;
      motionZ *= pow;
      motionX += this.mc.player.getVelocity().getX();
      motionY += this.mc.player.getVelocity().getY();
      motionZ += this.mc.player.getVelocity().getZ();
      boolean arrowPhysics = item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem;

      for (int i = 0; i < 300; i++) {
         Vec3d lastPos = new Vec3d(x, y, z);
         x += motionX;
         y += motionY;
         z += motionZ;
         if (this.mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() == Blocks.WATER) {
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
         } else {
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
         }

         motionY -= arrowPhysics ? 0.05F : 0.03F;
         Vec3d pos = new Vec3d(x, y, z);

         for (Entity ent : this.mc.world.getEntities()) {
            if (!(ent instanceof ArrowEntity)
               && !ent.equals(this.mc.player)
               && ent.getBoundingBox().intersects(new Box(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3))) {
               Box bb = ent.getBoundingBox();
               event.renderer.box(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, color, color, ShapeMode.Lines, 0);
               break;
            }
         }

         BlockHitResult bhr = this.mc
            .world
            .raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, this.mc.player));
         if (bhr != null && bhr.getType() == Type.BLOCK) {
            Box bb = new Box(bhr.getBlockPos());
            event.renderer.box(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, color, color, ShapeMode.Lines, 0);
            break;
         }

         if (y <= -65.0) {
            break;
         }

         if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
            event.renderer.line(lastPos.x, lastPos.y, lastPos.z, pos.x, pos.y, pos.z, color);
         }
      }
   }

   private float getDistance(Item item) {
      return item instanceof BowItem ? 1.0F : 0.4F;
   }

   private float getThrowVelocity(Item item) {
      if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return 0.5F;
      } else if (item instanceof ExperienceBottleItem) {
         return 0.59F;
      } else {
         return item instanceof TridentItem ? 2.0F : 1.5F;
      }
   }

   private int getThrowPitch(Item item) {
      return !(item instanceof SplashPotionItem) && !(item instanceof LingeringPotionItem) && !(item instanceof ExperienceBottleItem) ? 0 : 20;
   }
}
