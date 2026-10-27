package com.gregguns.icbm.tools;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.gregguns.api.Tier;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

/** Registers the remote detonators and the defuser and holds them for the recipes. Common side, preInit. */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md
public final class ToolRegistration {

    /** The tiers a remote detonator is made in. */
    public static final Tier[] DETONATOR_TIERS = { Tier.HV, Tier.EV, Tier.IV };

    private static final Map<Tier, ItemRemoteDetonator> DETONATORS = new EnumMap<>(Tier.class);
    private static ItemDefuser defuser;

    private ToolRegistration() {}

    /**
     * Registers the items whatever the module flag says (content always registers).
     *
     * @param registrar the ICBM module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        for (Tier t : DETONATOR_TIERS) {
            ItemRemoteDetonator item = registrar.registerItem(ItemRemoteDetonator.name(t), new ItemRemoteDetonator(t));
            item.setTextureName("gregguns:thirdparty/ntm/remote_detonator");
            DETONATORS.put(t, item);
        }
        defuser = registrar.registerItem(ItemDefuser.NAME, new ItemDefuser());
        defuser.setTextureName("gregguns:thirdparty/ntm/defuser");
    }

    /** Recipes and recycling data, queued for GT's before-postload hook. Enabled module only. */
    public static void init() {
        ToolRecipes.init();
    }

    /** Dev-only scenarios. */
    public static void registerScenarios(ScenarioRegistry registry) {
        ToolScenarios.registerAll(registry);
    }

    /**
     * @param tier HV, EV or IV
     * @return that tier's detonator; null before registration
     */
    public static ItemRemoteDetonator detonator(Tier tier) {
        return DETONATORS.get(tier);
    }

    /** @return the defuser; null before registration */
    public static ItemDefuser defuser() {
        return defuser;
    }

    /**
     * @param tier a detonator tier
     * @return one detonator
     */
    public static ItemStack detonatorStack(Tier tier) {
        return new ItemStack(DETONATORS.get(tier));
    }

    /** @return one defuser */
    public static ItemStack defuserStack() {
        return new ItemStack(defuser);
    }
}
