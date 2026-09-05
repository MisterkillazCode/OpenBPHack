package com.codigohasta.addon.modules;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;

public class Backtrack extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgDelays = this.settings.createGroup("Delays");
   private final SettingGroup sgPackets = this.settings.createGroup("Target Packets");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Backtrack.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("mode")).defaultValue(Backtrack.Mode.LagBehind)).build());
   private final Setting<Boolean> smart = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("smart"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> minRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("min-range"))
            .defaultValue(2.5)
            .range(0.5, 10.0)
            .sliderRange(0.5, 10.0)
            .build()
      );
   private final Setting<Double> maxRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("max-range"))
            .defaultValue(7.5)
            .range(0.5, 10.0)
            .sliderRange(0.5, 10.0)
            .build()
      );
   private final Setting<Double> maxReach = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("max-reach"))
            .defaultValue(4.0)
            .range(0.05, 6.0)
            .sliderRange(0.05, 6.0)
            .build()
      );
   private final Setting<Integer> minLagMs = this.sgDelays
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("min-lag-ms"))
               .defaultValue(125))
            .range(5, 5000)
            .sliderRange(5, 1000)
            .build()
      );
   private final Setting<Integer> maxLagMs = this.sgDelays
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("max-lag-ms"))
               .defaultValue(275))
            .range(5, 5000)
            .sliderRange(5, 1000)
            .build()
      );
   private final Setting<Integer> maxPacketData = this.sgDelays
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("max-packet-data"))
               .defaultValue(7))
            .range(1, 100)
            .sliderRange(1, 20)
            .build()
      );
   private final Setting<Boolean> targetMovements = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("movements"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetSwings = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("swings"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetAttacks = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("attacks"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetActions = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("entity-actions"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetDigs = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("digs"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetPlacements = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("placements"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetTransactions = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("transactions"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> targetKeepAlives = this.sgPackets
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("keep-alives"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> renderEsp = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render-esp"))
                  .description("Renders a box at the entity's real server position."))
               .defaultValue(true))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((Builder)((Builder)((Builder)((Builder)new Builder().name("shape-mode")).description("How the box is rendered.")).defaultValue(ShapeMode.Both))
               .visible(this.renderEsp::get))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("side-color"))
                  .description("The side color of the bounding box."))
               .defaultValue(new SettingColor(255, 0, 0, 50))
               .visible(() -> (Boolean)this.renderEsp.get() && (this.shapeMode.get() == ShapeMode.Sides || this.shapeMode.get() == ShapeMode.Both)))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                     .name("line-color"))
                  .description("The line color of the bounding box."))
               .defaultValue(new SettingColor(255, 0, 0, 200))
               .visible(() -> (Boolean)this.renderEsp.get() && (this.shapeMode.get() == ShapeMode.Lines || this.shapeMode.get() == ShapeMode.Both)))
            .build()
      );
   private final List<Backtrack.BacktrackData> packetData = new CopyOnWriteArrayList<>();
   private final List<Backtrack.TimedPacket> inboundPackets = new CopyOnWriteArrayList<>();
   private final List<Backtrack.TimedPacket> outboundPackets = new CopyOnWriteArrayList<>();
   private int randomizedMilliseconds = 0;
   private long lastResetTime = 0L;
   private boolean shouldBacktrackSmart = false;
   private boolean isSending = false;
   private static final Map<Class<?>, Method> idMethodCache = new HashMap<>();

   public Backtrack() {
      super(Categories.Combat, "Backtrack", "Simulates lag for a reach advantage with real position ESP.");
   }

   public void onActivate() {
      this.resetTimer();
      this.packetData.clear();
      this.inboundPackets.clear();
      this.outboundPackets.clear();
      this.shouldBacktrackSmart = false;
   }

   public void onDeactivate() {
      this.flushAll();
   }

   private void resetTimer() {
      this.lastResetTime = System.currentTimeMillis();
      this.randomizedMilliseconds = (Integer)this.minLagMs.get() + (int)(Math.random() * ((Integer)this.maxLagMs.get() - (Integer)this.minLagMs.get() + 1));
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.renderEsp.get() && this.mc.world != null) {
         for (Backtrack.BacktrackData data : this.packetData) {
            if (data.actualPosition != null && !data.movements.isEmpty()) {
               Entity entity = this.mc.world.getEntityById(data.entityId);
               if (entity != null) {
                  double x = data.actualPosition.x;
                  double y = data.actualPosition.y;
                  double z = data.actualPosition.z;
                  double width = entity.getWidth() / 2.0;
                  double height = entity.getHeight();
                  event.renderer
                     .box(
                        x - width,
                        y,
                        z - width,
                        x + width,
                        y + height,
                        z + width,
                        (Color)this.sideColor.get(),
                        (Color)this.lineColor.get(),
                        (ShapeMode)this.shapeMode.get(),
                        0
                     );
               }
            }
         }
      }
   }

   @EventHandler(
      priority = 200
   )
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         long currentTime = System.currentTimeMillis();
         if ((Boolean)this.smart.get()) {
            double closestDist = 999999.0;

            for (Backtrack.BacktrackData data : this.packetData) {
               Entity entity = this.mc.world.getEntityById(data.entityId);
               if (entity != null) {
                  double dist = this.mc.player.distanceTo(entity);
                  if (dist < closestDist) {
                     closestDist = dist;
                  }
               }
            }

            if (closestDist >= (Double)this.maxRange.get()) {
               this.shouldBacktrackSmart = false;
            }

            if (!this.shouldBacktrackSmart) {
               this.flushAll();
               return;
            }
         }

         if (currentTime - this.lastResetTime >= this.randomizedMilliseconds) {
            this.inboundPackets.removeIf(p -> {
               if (currentTime - p.time >= this.randomizedMilliseconds) {
                  this.applyInbound(p.packet);
                  return true;
               } else {
                  return false;
               }
            });
            this.outboundPackets.removeIf(p -> {
               if (currentTime - p.time < this.randomizedMilliseconds) {
                  return false;
               } else {
                  String name = p.packet.getClass().getSimpleName();
                  if (name.contains("PlayerInteractEntity")) {
                     boolean outOfReach = false;

                     for (Backtrack.BacktrackData datax : this.packetData) {
                        if (datax.actualPosition != null) {
                           Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
                           if (datax.actualPosition.distanceTo(playerPos) >= (Double)this.maxReach.get()) {
                              outOfReach = true;
                              break;
                           }
                        }
                     }

                     if (!outOfReach) {
                        this.sendOutbound(p.packet);
                     }
                  } else {
                     this.sendOutbound(p.packet);
                  }

                  return true;
               }
            });
            if (this.mode.get() == Backtrack.Mode.Freeze) {
               for (Backtrack.BacktrackData datax : this.packetData) {
                  for (Packet<?> packet : datax.movements) {
                     this.applyInbound(packet);
                  }

                  datax.movements.clear();
               }
            }

            this.resetTimer();
         }

         if (this.mode.get() == Backtrack.Mode.LagBehind) {
            for (Backtrack.BacktrackData datax : this.packetData) {
               if (datax.movements.size() >= this.randomizedMilliseconds / 50 && !datax.movements.isEmpty()) {
                  this.applyInbound(datax.movements.remove(0));
               }
            }
         }
      }
   }

   @EventHandler(
      priority = 200
   )
   private void onPacketSend(Send event) {
      if (!this.isSending) {
         Packet<?> packet = event.packet;
         String name = packet.getClass().getSimpleName();
         boolean intercept = false;
         if ((Boolean)this.targetMovements.get() && name.contains("PlayerMove")) {
            intercept = true;
         } else if ((Boolean)this.targetSwings.get() && name.contains("HandSwing")) {
            intercept = true;
         } else if ((Boolean)this.targetActions.get() && name.contains("ClientCommand")) {
            intercept = true;
         } else if ((Boolean)this.targetDigs.get() && name.contains("PlayerAction")) {
            intercept = true;
         } else if (!(Boolean)this.targetPlacements.get() || !name.contains("PlayerInteractBlock") && !name.contains("PlayerInteractItem")) {
            if ((Boolean)this.targetTransactions.get() && name.contains("Pong")) {
               intercept = true;
            } else if ((Boolean)this.targetKeepAlives.get() && name.contains("KeepAlive")) {
               intercept = true;
            } else if ((Boolean)this.targetAttacks.get() && name.contains("PlayerInteractEntity")) {
               intercept = true;
               if (!this.shouldBacktrackSmart) {
                  this.shouldBacktrackSmart = true;
               }
            }
         } else {
            intercept = true;
         }

         if (intercept) {
            this.outboundPackets.add(new Backtrack.TimedPacket(packet));
            event.cancel();
         }
      }
   }

   @EventHandler(
      priority = 200
   )
   private void onPacketReceive(Receive event) {
      if (this.mc.player != null && this.mc.world != null) {
         Packet<?> packet = event.packet;
         String name = packet.getClass().getSimpleName();
         if (name.contains("EntityPositionS2CPacket") || name.contains("EntityS2CPacket") || name.contains("EntityTrackerUpdateS2CPacket")) {
            int entityId = this.extractEntityIdSafely(packet);
            if (entityId == -1 || entityId == this.mc.player.getId()) {
               return;
            }

            Entity entity = this.mc.world.getEntityById(entityId);
            if (entity == null) {
               return;
            }

            double dist = this.mc.player.distanceTo(entity);
            if (dist < (Double)this.minRange.get() || dist > (Double)this.maxRange.get()) {
               return;
            }

            Backtrack.BacktrackData data = this.retrieveData(entityId);
            data.actualPosition = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            if (data.movements.size() > (Integer)this.maxPacketData.get()) {
               return;
            }

            data.movements.add(packet);
            event.cancel();
         }
      }
   }

   private void flushAll() {
      for (Backtrack.BacktrackData data : this.packetData) {
         for (Packet<?> packet : data.movements) {
            this.applyInbound(packet);
         }
      }

      this.packetData.clear();

      for (Backtrack.TimedPacket p : this.inboundPackets) {
         this.applyInbound(p.packet);
      }

      this.inboundPackets.clear();

      for (Backtrack.TimedPacket p : this.outboundPackets) {
         this.sendOutbound(p.packet);
      }

      this.outboundPackets.clear();
   }

   private void applyInbound(Packet<?> packet) {
      if (this.mc.getNetworkHandler() != null) {
         try {
            packet.apply(this.mc.getNetworkHandler());
         } catch (Exception var3) {
         }
      }
   }

   private void sendOutbound(Packet<?> packet) {
      if (this.mc.getNetworkHandler() != null) {
         this.isSending = true;
         this.mc.getNetworkHandler().sendPacket(packet);
         this.isSending = false;
      }
   }

   private Backtrack.BacktrackData retrieveData(int entityId) {
      for (Backtrack.BacktrackData data : this.packetData) {
         if (data.entityId == entityId) {
            return data;
         }
      }

      Backtrack.BacktrackData newData = new Backtrack.BacktrackData(entityId);
      this.packetData.add(newData);
      return newData;
   }

   private int extractEntityIdSafely(Packet<?> packet) {
      Class<?> clazz = packet.getClass();
      if (idMethodCache.containsKey(clazz)) {
         Method m = idMethodCache.get(clazz);
         if (m == null) {
            return -1;
         } else {
            try {
               return (Integer)m.invoke(packet);
            } catch (Exception var5) {
               return -1;
            }
         }
      } else {
         try {
            Method m;
            try {
               m = clazz.getMethod("id");
            } catch (NoSuchMethodException var6) {
               m = clazz.getMethod("getEntityId");
            }

            m.setAccessible(true);
            idMethodCache.put(clazz, m);
            return (Integer)m.invoke(packet);
         } catch (Exception var7) {
            idMethodCache.put(clazz, null);
            return -1;
         }
      }
   }

   private static class BacktrackData {
      public final int entityId;
      public Vec3d actualPosition = null;
      public final List<Packet<?>> movements = new CopyOnWriteArrayList<>();

      public BacktrackData(int entityId) {
         this.entityId = entityId;
      }
   }

   public static enum Mode {
      LagBehind,
      Freeze;
   }

   private static class TimedPacket {
      public final Packet<?> packet;
      public final long time;

      public TimedPacket(Packet<?> packet) {
         this.packet = packet;
         this.time = System.currentTimeMillis();
      }
   }
}
