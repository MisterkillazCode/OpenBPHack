package com.codigohasta.addon.modules;

import com.codigohasta.addon.AddonTemplate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class InventorySort extends Module {
   public static InventorySort INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<InventorySort.SortMode> sortMode = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("Mode")).description("Once = sort a single time then turn off; Loop = re-sort on an interval."))
               .defaultValue(InventorySort.SortMode.Once))
            .build()
      );
   private final Setting<InventorySort.SortBy> sortBy = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("SortBy")).description("What to sort by: item ID / item name / stack size."))
               .defaultValue(InventorySort.SortBy.ItemId))
            .build()
      );
   private final Setting<Boolean> includeHotbar = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("IncludeHotbar"))
                  .description("Include the hotbar in the sort."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> mergeStacks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("MergeStacks"))
                  .description("Merge partial stacks into full ones first."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("Delay"))
                  .description("Ticks to wait between two actions, 2 by default."))
               .defaultValue(2))
            .min(0)
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Integer> loopDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("LoopDelay"))
                     .description("In Loop mode, how often to re-sort, in seconds."))
                  .defaultValue(30))
               .min(1)
               .sliderRange(1, 300)
               .visible(() -> this.sortMode.get() == InventorySort.SortMode.Loop))
            .build()
      );
   private final Deque<int[]> queue = new ArrayDeque<>();
   private int timer = 0;
   private int loopTimer = 0;
   private boolean started = false;

   public InventorySort() {
      super(
         AddonTemplate.SC_CATEGORY,
         "InventorySort",
         "Sorts your inventory: merges partial stacks, then orders items. Simulates normal dragging, no screen needed."
      );
      INSTANCE = this;
   }

   public void onActivate() {
      this.timer = 0;
      this.started = false;
      this.loopTimer = 0;
      this.queue.clear();
   }

   public void onDeactivate() {
      this.queue.clear();
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.mc.player.currentScreenHandler != this.mc.player.playerScreenHandler) {
            this.queue.clear();
         } else {
            if (!this.started) {
               this.started = true;
               this.loopTimer = (Integer)this.loopDelay.get() * 20;
               this.buildPlan();
            }

            if (this.queue.isEmpty()) {
               if (this.sortMode.get() == InventorySort.SortMode.Once) {
                  this.info("背包整理完成。", new Object[0]);
                  this.toggle();
               } else {
                  if (--this.loopTimer <= 0) {
                     this.loopTimer = (Integer)this.loopDelay.get() * 20;
                     this.buildPlan();
                  }
               }
            } else if (this.timer > 0) {
               this.timer--;
            } else {
               int[] action = this.queue.poll();
               if (action != null) {
                  InvUtils.move().from(action[0]).to(action[1]);
                  this.timer = (Integer)this.delay.get();
               }
            }
         }
      }
   }

   private void buildPlan() {
      this.queue.clear();
      List<Integer> range = new ArrayList<>();
      int start = this.includeHotbar.get() ? 0 : 9;

      for (int i = start; i <= 35; i++) {
         range.add(i);
      }

      ItemStack[] sim = new ItemStack[41];

      for (int i = 0; i < sim.length; i++) {
         sim[i] = this.mc.player.getInventory().getStack(i).copy();
      }

      if ((Boolean)this.mergeStacks.get()) {
         for (int i : range) {
            if (!sim[i].isEmpty()) {
               for (int j : range) {
                  if (j > i && !sim[j].isEmpty() && ItemStack.areItemsAndComponentsEqual(sim[i], sim[j])) {
                     if (sim[i].getCount() >= sim[i].getMaxCount()) {
                        break;
                     }

                     this.queue.add(new int[]{j, i});
                     this.applyMove(sim, j, i);
                  }
               }
            }
         }
      }

      for (int a = 0; a < range.size(); a++) {
         int best = a;

         for (int b = a + 1; b < range.size(); b++) {
            if (this.compare(sim[range.get(b)], sim[range.get(best)]) < 0) {
               best = b;
            }
         }

         if (best != a) {
            int from = range.get(best);
            int to = range.get(a);
            this.queue.add(new int[]{from, to});
            ItemStack tmp = sim[from];
            sim[from] = sim[to];
            sim[to] = tmp;
         }
      }
   }

   private int compare(ItemStack a, ItemStack b) {
      boolean emptyA = a.isEmpty();
      boolean emptyB = b.isEmpty();
      if (emptyA && emptyB) {
         return 0;
      } else if (emptyA) {
         return 1;
      } else if (emptyB) {
         return -1;
      } else {
         return switch ((InventorySort.SortBy)this.sortBy.get()) {
            case Name -> a.getName().getString().compareToIgnoreCase(b.getName().getString());
            case Count -> Integer.compare(b.getCount(), a.getCount());
            default -> Integer.compare(Item.getRawId(a.getItem()), Item.getRawId(b.getItem()));
         };
      }
   }

   private void applyMove(ItemStack[] sim, int from, int to) {
      ItemStack f = sim[from];
      ItemStack t = sim[to];
      if (t.isEmpty()) {
         sim[to] = f;
         sim[from] = ItemStack.EMPTY;
      } else if (ItemStack.areItemsAndComponentsEqual(f, t) && t.getCount() < t.getMaxCount()) {
         int space = t.getMaxCount() - t.getCount();
         int moved = Math.min(space, f.getCount());
         ItemStack merged = t.copy();
         merged.setCount(t.getCount() + moved);
         sim[to] = merged;
         if (f.getCount() - moved <= 0) {
            sim[from] = ItemStack.EMPTY;
         } else {
            ItemStack left = f.copy();
            left.setCount(f.getCount() - moved);
            sim[from] = left;
         }
      } else {
         sim[to] = f;
         sim[from] = t;
      }
   }

   public static enum SortBy {
      ItemId,
      Name,
      Count;
   }

   public static enum SortMode {
      Once,
      Loop;
   }
}
