package com.gregguns.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.core.registry.ModuleRegistrar;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * No config value may change what GregGuns registers. Under every flag combination (all 128) the manager must run the
 * same content hooks, in the same order; only the behavior hooks (recipes, handlers) follow the flags.
 */
class ModuleRegistrationIndependenceTest {

    /** Records which hooks ran, in order. */
    private static final class Recorder implements GregGunsModule {

        private final ModuleId id;
        private final List<String> content;
        private final List<String> behavior;

        Recorder(ModuleId id, List<String> content, List<String> behavior) {
            this.id = id;
            this.content = content;
            this.behavior = behavior;
        }

        @Override
        public ModuleId id() {
            return id;
        }

        @Override
        public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
            content.add("preInit:" + id + ":" + registrar.module());
        }

        @Override
        public void initContent(FMLInitializationEvent event) {
            content.add("content:" + id);
        }

        @Override
        public void init(FMLInitializationEvent event) {
            behavior.add("init:" + id);
        }
    }

    private static List<String> run(int flags, List<String> behavior) {
        List<String> content = new ArrayList<>();
        ModuleManager manager = new ModuleManager(id -> (flags & (1 << id.ordinal())) != 0, mod -> true, null);
        // Registered in reverse on purpose: dispatch order is the ModuleId declaration order.
        ModuleId[] ids = ModuleId.values();
        for (int i = ids.length - 1; i >= 0; i--) {
            manager.register(new Recorder(ids[i], content, behavior));
        }
        manager.preInit(null);
        manager.init(null);
        return content;
    }

    @Test
    void theRegisteredContentIsIdenticalForEveryFlagCombination() {
        List<String> reference = run((1 << ModuleId.values().length) - 1, new ArrayList<>());
        assertEquals(2 * ModuleId.values().length, reference.size());
        for (int flags = 0; flags < (1 << ModuleId.values().length); flags++) {
            assertEquals(reference, run(flags, new ArrayList<>()), "flags " + Integer.toBinaryString(flags));
        }
    }

    @Test
    void onlyTheBehaviorHooksFollowTheFlags() {
        List<String> behavior = new ArrayList<>();
        int everythingButGuns = ((1 << ModuleId.values().length) - 1) & ~(1 << ModuleId.GUNS.ordinal());
        run(everythingButGuns, behavior);
        assertEquals(0, behavior.size(), "every other module needs guns, directly or through machines or npcs");

        behavior.clear();
        run(1 << ModuleId.GUNS.ordinal(), behavior);
        assertEquals(1, behavior.size());
        assertTrue(behavior.contains("init:GUNS"));
    }
}
