package com.codigohasta.addon;

import com.codigohasta.addon.commands.CommandExample;
import com.codigohasta.addon.commands.GrimAcCommand;
import com.codigohasta.addon.commands.HomeCommand;
import com.codigohasta.addon.hud.HudExample;
import com.codigohasta.addon.hud.TargetHud;
import com.codigohasta.addon.i18n.Chinese;
import com.codigohasta.addon.modules.AdaPacketMine;
import com.codigohasta.addon.modules.AdvancedCriticals;
import com.codigohasta.addon.modules.AdvancedFakePlayer;
import com.codigohasta.addon.modules.AirPlace;
import com.codigohasta.addon.modules.AlienSprint;
import com.codigohasta.addon.modules.AlienV4PacketMine;
import com.codigohasta.addon.modules.Ambience;
import com.codigohasta.addon.modules.AntiAntiXray;
import com.codigohasta.addon.modules.AntiLag;
import com.codigohasta.addon.modules.ArrowDmg;
import com.codigohasta.addon.modules.AttackRangeIndicator;
import com.codigohasta.addon.modules.AttractAura;
import com.codigohasta.addon.modules.AutoAnchor;
import com.codigohasta.addon.modules.AutoBed;
import com.codigohasta.addon.modules.AutoBonemeal;
import com.codigohasta.addon.modules.AutoBucket;
import com.codigohasta.addon.modules.AutoChestAura;
import com.codigohasta.addon.modules.AutoChorus;
import com.codigohasta.addon.modules.AutoCity;
import com.codigohasta.addon.modules.AutoCrystal;
import com.codigohasta.addon.modules.AutoDeoxidizer;
import com.codigohasta.addon.modules.AutoDoubleHand;
import com.codigohasta.addon.modules.AutoDrop;
import com.codigohasta.addon.modules.AutoFire;
import com.codigohasta.addon.modules.AutoFirework;
import com.codigohasta.addon.modules.AutoHarvest;
import com.codigohasta.addon.modules.AutoInvTotem;
import com.codigohasta.addon.modules.AutoJump;
import com.codigohasta.addon.modules.AutoKouZi;
import com.codigohasta.addon.modules.AutoLibrarian;
import com.codigohasta.addon.modules.AutoMace;
import com.codigohasta.addon.modules.AutoMessage;
import com.codigohasta.addon.modules.AutoMilk;
import com.codigohasta.addon.modules.AutoNod;
import com.codigohasta.addon.modules.AutoPearl;
import com.codigohasta.addon.modules.AutoPotion;
import com.codigohasta.addon.modules.AutoRefreshTrade;
import com.codigohasta.addon.modules.AutoRepair;
import com.codigohasta.addon.modules.AutoRespawn;
import com.codigohasta.addon.modules.AutoServer;
import com.codigohasta.addon.modules.AutoSmithing;
import com.codigohasta.addon.modules.AutoTPAccept;
import com.codigohasta.addon.modules.AutoTorch;
import com.codigohasta.addon.modules.AutoVault;
import com.codigohasta.addon.modules.BMWSprint;
import com.codigohasta.addon.modules.BPHackChams;
import com.codigohasta.addon.modules.BPHackFakePlayer;
import com.codigohasta.addon.modules.BPHackPopChams;
import com.codigohasta.addon.modules.BPHackTips;
import com.codigohasta.addon.modules.BPHackTotemParticle;
import com.codigohasta.addon.modules.BPHackWorldStats;
import com.codigohasta.addon.modules.Backtrack;
import com.codigohasta.addon.modules.BanPlayer;
import com.codigohasta.addon.modules.Bombing_people;
import com.codigohasta.addon.modules.BreakESP;
import com.codigohasta.addon.modules.CameraClip;
import com.codigohasta.addon.modules.ChatFilter;
import com.codigohasta.addon.modules.ChatHider;
import com.codigohasta.addon.modules.ChatHighlight;
import com.codigohasta.addon.modules.ChatPrefixCustom;
import com.codigohasta.addon.modules.Criticals;
import com.codigohasta.addon.modules.CrossbowAura;
import com.codigohasta.addon.modules.CrystalMacro;
import com.codigohasta.addon.modules.CustomFishingBot;
import com.codigohasta.addon.modules.CustomFov;
import com.codigohasta.addon.modules.CustomItemESP;
import com.codigohasta.addon.modules.DeathWaypoint;
import com.codigohasta.addon.modules.ElytraFly;
import com.codigohasta.addon.modules.ElytraFlyPlus;
import com.codigohasta.addon.modules.ElytraFollower;
import com.codigohasta.addon.modules.EntityTags;
import com.codigohasta.addon.modules.FastCrossbow;
import com.codigohasta.addon.modules.FeedbackBlocker;
import com.codigohasta.addon.modules.FillESP;
import com.codigohasta.addon.modules.FireworkElytraFly;
import com.codigohasta.addon.modules.FireworklessFlight;
import com.codigohasta.addon.modules.FlightAntiKick;
import com.codigohasta.addon.modules.Follower;
import com.codigohasta.addon.modules.Freeze;
import com.codigohasta.addon.modules.GlobalSetting;
import com.codigohasta.addon.modules.Grim2Speed;
import com.codigohasta.addon.modules.GrimAc;
import com.codigohasta.addon.modules.GrimCriticals;
import com.codigohasta.addon.modules.GrimDisabler;
import com.codigohasta.addon.modules.GrimFly;
import com.codigohasta.addon.modules.GrimSpeed;
import com.codigohasta.addon.modules.HexChat;
import com.codigohasta.addon.modules.HitboxESP;
import com.codigohasta.addon.modules.HomeWaypoint;
import com.codigohasta.addon.modules.IPlist;
import com.codigohasta.addon.modules.InfiniteChat;
import com.codigohasta.addon.modules.InventorySort;
import com.codigohasta.addon.modules.ItemDespawnTimer;
import com.codigohasta.addon.modules.KillFX;
import com.codigohasta.addon.modules.KnockbackDirection;
import com.codigohasta.addon.modules.LavaESP;
import com.codigohasta.addon.modules.LegitNoFall;
import com.codigohasta.addon.modules.LegitNoFallLeaves;
import com.codigohasta.addon.modules.LightningTracker;
import com.codigohasta.addon.modules.MaceAura;
import com.codigohasta.addon.modules.MaceBreakerPro;
import com.codigohasta.addon.modules.MaceDMGPlus;
import com.codigohasta.addon.modules.MacroAnchor;
import com.codigohasta.addon.modules.MassTpa;
import com.codigohasta.addon.modules.MineESP;
import com.codigohasta.addon.modules.MobHud;
import com.codigohasta.addon.modules.ModuleList;
import com.codigohasta.addon.modules.MotionCamera;
import com.codigohasta.addon.modules.MusicPlayer;
import com.codigohasta.addon.modules.NBTEditor;
import com.codigohasta.addon.modules.NoHurtCam;
import com.codigohasta.addon.modules.ODMGear;
import com.codigohasta.addon.modules.OreVeinESP;
import com.codigohasta.addon.modules.PacketEat;
import com.codigohasta.addon.modules.PacketMinePlus;
import com.codigohasta.addon.modules.Panic;
import com.codigohasta.addon.modules.PearlPhase;
import com.codigohasta.addon.modules.PistonCrystal;
import com.codigohasta.addon.modules.Pitcher;
import com.codigohasta.addon.modules.PlaceRender;
import com.codigohasta.addon.modules.PolarSpeed;
import com.codigohasta.addon.modules.PortalESP;
import com.codigohasta.addon.modules.PortalGodMode;
import com.codigohasta.addon.modules.PrinterLeaves;
import com.codigohasta.addon.modules.RTsearch;
import com.codigohasta.addon.modules.SafeMine;
import com.codigohasta.addon.modules.ScaffoldPlus;
import com.codigohasta.addon.modules.ScaffoldPlusLeaves;
import com.codigohasta.addon.modules.SchematicPro;
import com.codigohasta.addon.modules.ScreenActions;
import com.codigohasta.addon.modules.ServerFix;
import com.codigohasta.addon.modules.ServerLagger;
import com.codigohasta.addon.modules.ServerLaggerBook;
import com.codigohasta.addon.modules.ServerLaggerCommand;
import com.codigohasta.addon.modules.ServerLaggerInteract;
import com.codigohasta.addon.modules.ServerLaggerMisc;
import com.codigohasta.addon.modules.ShieldESP;
import com.codigohasta.addon.modules.ShulkerStash;
import com.codigohasta.addon.modules.ShulkerViewer;
import com.codigohasta.addon.modules.SpearExploit;
import com.codigohasta.addon.modules.SpearKill;
import com.codigohasta.addon.modules.SprintPlus;
import com.codigohasta.addon.modules.SprintStatusModule;
import com.codigohasta.addon.modules.Stuck;
import com.codigohasta.addon.modules.SwordGap;
import com.codigohasta.addon.modules.TargetStrafe;
import com.codigohasta.addon.modules.TntBomber;
import com.codigohasta.addon.modules.TpAnchor;
import com.codigohasta.addon.modules.TpAura;
import com.codigohasta.addon.modules.TpBowAura;
import com.codigohasta.addon.modules.TpMachineGun;
import com.codigohasta.addon.modules.Trajectories;
import com.codigohasta.addon.modules.VelocityAlien;
import com.codigohasta.addon.modules.VillagerTrader;
import com.codigohasta.addon.modules.XCarry;
import com.codigohasta.addon.modules.XTpaura;
import com.codigohasta.addon.modules.adaAttributeSwap;
import com.codigohasta.addon.modules.adaAutoHotbar;
import com.codigohasta.addon.modules.adaManualCrystal;
import com.codigohasta.addon.modules.lightning_strike;
import com.codigohasta.addon.modules.xhEntityList;
import com.codigohasta.addon.modules.xhPacketMinePlus;
import com.codigohasta.addon.utils.alien.AlienBreakManager;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
   public static final Logger LOG = LogUtils.getLogger();
   private boolean sentWelcome = false;
   private Modules modules;
   public static final Category CATEGORY = new Category("BPHack", Items.PLAYER_HEAD.getDefaultStack());
   public static final Category OP_CATEGORY = new Category("OP", Items.COMMAND_BLOCK.getDefaultStack());
   public static final Category SC_CATEGORY = new Category("SC", Items.GRASS_BLOCK.getDefaultStack());
   public static final HudGroup HUD_GROUP = new HudGroup("BPHack");

   private void addModule(Module module) {
      if (this.modules != null) {
         if (this.modules.get(module.getClass()) != null) {
            LOG.warn("Skipping duplicate module registration: {}", module.getClass().getSimpleName());
         } else {
            this.modules.add(module);
         }
      }
   }

   public void onInitialize() {
      LOG.info("Initializing BPHack Addon");
      LOG.info("BPHack is loading");
      this.modules = Modules.get();
      this.addModule(new Chinese());
      this.addModule(new AdvancedCriticals());
      this.addModule(new AdvancedFakePlayer());
      this.addModule(new AntiAntiXray());
      this.addModule(new AttackRangeIndicator());
      this.addModule(new AttractAura());
      this.addModule(new AutoChestAura());
      this.addModule(new AutoChorus());
      this.addModule(new AutoDeoxidizer());
      this.addModule(new AutoFirework());
      this.addModule(new AutoJump());
      this.addModule(new AutoKouZi());
      this.addModule(new AutoMessage());
      this.addModule(new AutoNod());
      this.addModule(new AutoRespawn());
      this.addModule(new AutoServer());
      this.addModule(new AutoSmithing());
      this.addModule(new AutoTPAccept());
      this.addModule(new ChatFilter());
      this.addModule(new ChatHider());
      this.addModule(new ChatHighlight());
      this.addModule(new ChatPrefixCustom());
      this.addModule(new CrossbowAura());
      this.addModule(new CustomFov());
      this.addModule(new CustomItemESP());
      this.addModule(new ElytraFollower());
      this.addModule(new FastCrossbow());
      this.addModule(new FeedbackBlocker());
      this.addModule(new FlightAntiKick());
      this.addModule(new GrimCriticals());
      this.addModule(new Criticals());
      this.addModule(new HexChat());
      this.addModule(new HitboxESP());
      this.addModule(new InfiniteChat());
      this.addModule(new ItemDespawnTimer());
      this.addModule(new KnockbackDirection());
      this.addModule(new MaceAura());
      this.addModule(new MaceBreakerPro());
      this.addModule(new MaceDMGPlus());
      this.addModule(new MassTpa());
      this.addModule(new MineESP());
      this.addModule(new ModuleList());
      this.addModule(new MusicPlayer());
      this.addModule(new OreVeinESP());
      this.addModule(new PearlPhase());
      this.addModule(new PortalESP());
      this.addModule(new RTsearch());
      this.addModule(new ShieldESP());
      this.addModule(new SpearExploit());
      this.addModule(new SwordGap());
      this.addModule(new TargetStrafe());
      this.addModule(new TntBomber());
      this.addModule(new TpAnchor());
      this.addModule(new TpAura());
      this.addModule(new VelocityAlien());
      this.addModule(new xhEntityList());
      this.addModule(new FillESP());
      this.addModule(new ScaffoldPlus());
      this.addModule(new LegitNoFall());
      this.addModule(new xhPacketMinePlus());
      this.addModule(new ElytraFlyPlus());
      this.addModule(new adaAttributeSwap());
      this.addModule(new adaManualCrystal());
      this.addModule(new adaAutoHotbar());
      this.addModule(new ODMGear());
      this.addModule(new AdaPacketMine());
      this.addModule(new AlienV4PacketMine());
      this.addModule(new SchematicPro());
      this.addModule(new MacroAnchor());
      this.addModule(new ElytraFly());
      this.addModule(new Follower());
      this.addModule(new ArrowDmg());
      this.addModule(new Pitcher());
      this.addModule(new VillagerTrader());
      this.addModule(new BPHackWorldStats());
      this.addModule(new XTpaura());
      this.addModule(new XCarry());
      this.addModule(new EntityTags());
      this.addModule(new TpBowAura());
      this.addModule(new TpMachineGun());
      this.addModule(new AutoLibrarian());
      this.addModule(new SpearKill());
      this.addModule(new AntiLag());
      this.addModule(new SprintStatusModule());
      this.addModule(new Backtrack());
      this.addModule(new PortalGodMode());
      this.addModule(new AutoDoubleHand());
      this.addModule(new CrystalMacro());
      this.addModule(new AutoInvTotem());
      this.addModule(new KillFX());
      this.addModule(new AlienSprint());
      this.addModule(new CameraClip());
      this.addModule(new MotionCamera());
      this.addModule(new Panic());
      this.addModule(new Trajectories());
      this.addModule(new BPHackChams());
      this.addModule(new BPHackPopChams());
      this.addModule(new BPHackTips());
      this.addModule(new BPHackTotemParticle());
      this.addModule(new BPHackFakePlayer());
      this.addModule(new GlobalSetting());
      this.addModule(new PlaceRender());
      this.addModule(new AutoTorch());
      this.addModule(new AutoRefreshTrade());
      this.addModule(new AutoCrystal());
      this.addModule(new AutoAnchor());
      this.addModule(new PistonCrystal());
      this.addModule(new ScaffoldPlusLeaves());
      this.addModule(new LegitNoFallLeaves());
      this.addModule(new PrinterLeaves());
      this.addModule(new PacketMinePlus());
      this.addModule(new AutoCity());
      this.addModule(new FireworkElytraFly());
      this.addModule(new Stuck());
      this.addModule(new Ambience());
      this.addModule(new Freeze());
      this.addModule(new BMWSprint());
      this.addModule(new AutoVault());
      this.addModule(new ScreenActions());
      this.addModule(new MobHud());
      this.addModule(new LavaESP());
      this.addModule(new CustomFishingBot());
      this.addModule(new SprintPlus());
      this.addModule(new ServerLagger());
      this.addModule(new ServerLaggerBook());
      this.addModule(new ServerLaggerInteract());
      this.addModule(new ServerLaggerCommand());
      this.addModule(new ServerLaggerMisc());
      this.addModule(new ServerFix());
      this.addModule(new BanPlayer());
      this.addModule(new IPlist());
      this.addModule(new NBTEditor());
      this.addModule(new NoHurtCam());
      this.addModule(new lightning_strike());
      this.addModule(new Bombing_people());
      this.addModule(new PacketEat());
      this.addModule(new GrimSpeed());
      this.addModule(new AirPlace());
      this.addModule(new GrimAc());
      this.addModule(new LightningTracker());
      this.addModule(new Grim2Speed());
      this.addModule(new PolarSpeed());
      this.addModule(new GrimDisabler());
      this.addModule(new GrimFly());
      this.addModule(new AutoMace());
      this.addModule(new AutoPotion());
      this.addModule(new AutoMilk());
      this.addModule(new AutoBucket());
      this.addModule(new AutoFire());
      this.addModule(new SafeMine());
      this.addModule(new AutoPearl());
      this.addModule(new AutoDrop());
      this.addModule(new InventorySort());
      this.addModule(new ShulkerStash());
      this.addModule(new AutoRepair());
      this.addModule(new AutoHarvest());
      this.addModule(new AutoBonemeal());
      this.addModule(new AutoBed());
      this.addModule(new DeathWaypoint());
      this.addModule(new HomeWaypoint());
      this.addModule(new FireworklessFlight());
      this.addModule(new ShulkerViewer());
      this.addModule(new BreakESP());
      new AlienBreakManager();
      Commands.add(new CommandExample());
      Commands.add(new GrimAcCommand());
      Commands.add(new HomeCommand());
      Hud.get().register(HudExample.INFO);
      Hud.get().register(TargetHud.INFO);
      ChatPrefixCustom cp = (ChatPrefixCustom)Modules.get().get(ChatPrefixCustom.class);
      if (cp == null || !cp.isActive()) {
         ChatPrefixCustom.registerDefault();
      }

      MeteorClient.EVENT_BUS.subscribe(this);
   }

   public void onRegisterCategories() {
      Modules.registerCategory(CATEGORY);
      Modules.registerCategory(OP_CATEGORY);
      Modules.registerCategory(SC_CATEGORY);
   }

   public String getPackage() {
      return "com.codigohasta.addon";
   }

   public GithubRepo getRepo() {
      return new GithubRepo("adaxiaohu", "BPHack");
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      if (!this.sentWelcome) {
         ChatUtils.forceNextPrefixClass(this.getClass());
         ChatUtils.sendMsg(this.createGradientText("BPHack is loading"));
         this.sentWelcome = true;
      }
   }

   @EventHandler
   private void onGameLeave(GameLeftEvent event) {
      this.sentWelcome = false;
   }

   private Text createGradientText(String text) {
      Color startColor = new Color(0, 255, 255);
      Color endColor = new Color(255, 0, 255);
      MutableText result = Text.empty();

      for (int i = 0; i < text.length(); i++) {
         float f = (float)i / text.length();
         int r = (int)(startColor.r + (endColor.r - startColor.r) * f);
         int g = (int)(startColor.g + (endColor.g - startColor.g) * f);
         int b = (int)(startColor.b + (endColor.b - startColor.b) * f);
         Color stepColor = new Color(r, g, b, 255);
         result.append(
            Text.literal(String.valueOf(text.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(stepColor.getPacked())))
         );
      }

      return result;
   }
}
