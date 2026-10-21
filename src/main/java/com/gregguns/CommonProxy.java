package com.gregguns;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gregguns.armor.ArmorModule;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.GregGunsTab;
import com.gregguns.core.command.GregGunsCommand;
import com.gregguns.core.command.TestSubCommand;
import com.gregguns.core.config.GregGunsConfig;
import com.gregguns.core.config.ModuleFlags;
import com.gregguns.core.module.ModuleGate;
import com.gregguns.core.module.ModuleManager;
import com.gregguns.core.module.ModuleSyncLogic;
import com.gregguns.core.module.PacketModuleSync;
import com.gregguns.core.selfcheck.SelfCheckRunner;
import com.gregguns.core.testing.HarnessScenarios;
import com.gregguns.core.testing.ScenarioChunks;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.entity.NpcModule;
import com.gregguns.gun.GunsModule;
import com.gregguns.machine.MachinesModule;
import com.gregguns.machine.MtePlaceholders;
import com.gregguns.network.GregGunsNetwork;
import com.gregguns.utility.UtilityModule;
import com.gregguns.worldgen.WorldgenModule;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class CommonProxy {

    private ModuleManager modules;

    public void preInit(FMLPreInitializationEvent event) {
        GregGuns.LOG.info("{} {} loading", GregGuns.NAME, Tags.VERSION);
        // Order per CONTRIBUTING.md: config first, then the modules register items, blocks and entities.
        GregGunsConfig.register();
        GregGunsNetwork.init();
        modules = new ModuleManager(ModuleFlags::isEnabled, new GregGunsTab());
        registerModules(modules);
        modules.preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        modules.init(event);
        // Retired ids get an inert machine so old worlds cannot crash GT's item render (see MtePlaceholders). Still
        // inside GT's registration window.
        MtePlaceholders.registerMissing();
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        // Existing items of a module that is off do nothing; also on a client whose server has other flags.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ModuleGate.INSTANCE);
        if (DevEnvironment.testHooksEnabled()) {
            // Dev scenario runs force-load their area; the ticket callback must exist before any world loads.
            ScenarioChunks.registerCallback();
        }
    }

    /** @return the bit mask of the modules active on this side's own config, see ModuleState */
    public int enabledModuleMask() {
        return ModuleSyncLogic.serverMask();
    }

    /** Called on the client when the server's module flags arrive or are dropped; refreshes what depends on them. */
    public void moduleStateChanged() {}

    /** @param event a player logging in; tells their client which modules are active on the server */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            GregGunsNetwork.channel()
                .sendTo(new PacketModuleSync(enabledModuleMask()), (EntityPlayerMP) event.player);
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        modules.postInit(event);
    }

    /**
     * Registers the dev-only {@code /gregguns} command (with its {@code test} subcommand) and the built-in scenarios.
     * Does nothing outside the dev environment unless {@code -Dgregguns.testHooks=true} is set.
     */
    public void serverStarting(FMLServerStartingEvent event) {
        if (!DevEnvironment.testHooksEnabled()) {
            if (com.gregguns.core.config.DebugConfig.recipeDump) {
                GregGunsCommand dump = new GregGunsCommand();
                dump.register(new com.gregguns.core.command.DumpRecipesSubCommand());
                event.registerServerCommand(dump);
                GregGuns.LOG.warn("debug.recipeDump is on: /gregguns dumprecipes is available to ops");
            }
            return;
        }
        HarnessScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        com.gregguns.fx.FxScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        com.gregguns.machine.BuildScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        GregGunsCommand command = new GregGunsCommand();
        command.register(new TestSubCommand(ScenarioRegistry.SCENARIOS));
        command.register(new com.gregguns.core.command.BuildSubCommand());
        command.register(new com.gregguns.core.command.DumpRecipesSubCommand());
        event.registerServerCommand(command);
        GregGuns.LOG.info(
            "Registered /gregguns (dev environment or -Dgregguns.testHooks), {} scenarios",
            ScenarioRegistry.SCENARIOS.size());
    }

    /**
     * Adds every module class. A milestone adds its module here, for example
     * {@code manager.register(new GunsModule())}.
     * {@code ClientProxy} overrides this to add the matching {@code ClientModule}s.
     *
     * @param manager the manager to register with
     */
    protected void registerModules(ModuleManager manager) {
        manager.register(new GunsModule());
        manager.register(new MachinesModule());
        manager.register(new ArmorModule());
        manager.register(new UtilityModule());
        manager.register(new NpcModule());
        manager.register(new WorldgenModule());
    }

    /**
     * Runs the dev self-check once every mod has registered its recipes.
     *
     * <p>
     * Deviation: CONTRIBUTING.md (Testing) says the self-check runs in postInit. It runs here, in load-complete,
     * because GT buffers its crafting-table recipes and registers the second pass in its own load-complete handler, and
     * other mods add recipes in their postInit; postInit would check an incomplete recipe set. See
     * {@link SelfCheckRunner}.
     */
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (DevEnvironment.isActive()) {
            SelfCheckRunner.run();
        }
    }
}
