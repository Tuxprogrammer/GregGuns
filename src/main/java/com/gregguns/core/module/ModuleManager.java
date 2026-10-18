package com.gregguns.core.module;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.creativetab.CreativeTabs;

import com.gregguns.GregGuns;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.registry.RegistrationLedger;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Holds the module classes and dispatches the FML lifecycle to the enabled ones. Which modules are enabled is decided
 * by {@link ModuleResolver}; this class is the thin Minecraft glue around that decision.
 */
// Spec: docs/ROADMAP.md (M0 scope); CONTRIBUTING.md (Modules, Registration order)
public final class ModuleManager {

    private final Predicate<ModuleId> flag;
    private final CreativeTabs tab;
    private final RegistrationLedger ledger = new RegistrationLedger();
    private final Map<ModuleId, GregGunsModule> modules = new EnumMap<>(ModuleId.class);
    private final Map<ModuleId, ClientModule> clientModules = new EnumMap<>(ModuleId.class);
    private ModuleResolution resolution;

    /**
     * @param flag the config switch for a module (see {@code ModuleFlags::isEnabled})
     * @param tab  the creative tab given to registered items and blocks
     */
    public ModuleManager(Predicate<ModuleId> flag, CreativeTabs tab) {
        this.flag = flag;
        this.tab = tab;
    }

    /**
     * Adds a module. Call before {@link #preInit}.
     *
     * @param module the module class
     * @throws IllegalStateException if resolution already happened or the id is already registered
     */
    public void register(GregGunsModule module) {
        if (resolution != null) {
            throw new IllegalStateException("Modules must be registered before preInit");
        }
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalStateException("Module registered twice: " + module.id());
        }
    }

    /**
     * Adds a client half. Call from {@code ClientProxy} before {@link #preInit}.
     *
     * @param module the client hooks
     */
    public void registerClient(ClientModule module) {
        if (resolution != null) {
            throw new IllegalStateException("Client modules must be registered before preInit");
        }
        if (clientModules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalStateException("Client module registered twice: " + module.id());
        }
    }

    /**
     * Resolves which modules run, then runs their preInit.
     *
     * @param event the FML event
     */
    public void preInit(FMLPreInitializationEvent event) {
        resolution = ModuleResolver.resolve(modules.keySet(), flag);
        resolution.skipped()
            .forEach(this::logSkipped);
        for (ModuleId id : resolution.enabled()) {
            GregGuns.LOG.info("Module {} enabled", id.configKey());
            modules.get(id)
                .preInit(event, new ModuleRegistrar(id, ledger, tab));
        }
        dispatchClient(ClientModule::preInit);
    }

    /**
     * Runs init on enabled modules.
     *
     * @param event the FML event
     */
    public void init(FMLInitializationEvent event) {
        for (GregGunsModule module : enabledModules()) {
            module.init(event);
        }
        dispatchClient(ClientModule::init);
    }

    /**
     * Runs postInit on enabled modules.
     *
     * @param event the FML event
     */
    public void postInit(FMLPostInitializationEvent event) {
        for (GregGunsModule module : enabledModules()) {
            module.postInit(event);
        }
        dispatchClient(ClientModule::postInit);
    }

    /** @return the ids of the modules that run; empty before {@link #preInit} */
    public List<ModuleId> enabledIds() {
        return resolution == null ? new ArrayList<>() : new ArrayList<>(resolution.enabled());
    }

    private List<GregGunsModule> enabledModules() {
        if (resolution == null) {
            throw new IllegalStateException("preInit has not run");
        }
        List<GregGunsModule> result = new ArrayList<>();
        for (ModuleId id : resolution.enabled()) {
            result.add(modules.get(id));
        }
        return result;
    }

    private void dispatchClient(Consumer<ClientModule> phase) {
        for (ModuleId id : resolution.enabled()) {
            ClientModule client = clientModules.get(id);
            if (client != null) {
                phase.accept(client);
            }
        }
    }

    private void logSkipped(ModuleId id, ModuleResolution.SkipReason reason) {
        if (reason == ModuleResolution.SkipReason.DISABLED_BY_CONFIG) {
            GregGuns.LOG.info("Module {} disabled by modules.{}", id.configKey(), id.configKey());
        } else {
            GregGuns.LOG.warn("Module {} is on but a module it requires is off; skipping it", id.configKey());
        }
    }
}
