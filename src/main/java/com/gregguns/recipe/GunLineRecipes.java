package com.gregguns.recipe;

import java.util.Arrays;
import java.util.function.Supplier;

import com.gregguns.api.Tier;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.recipe.ComponentTable.FluidInput;
import com.gregguns.recipe.ComponentTable.FluidKind;
import com.gregguns.recipe.ComponentTable.Research;
import com.gregguns.recipe.ComponentTable.Spec;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.ProductionTable.Target;

/**
 * Builds and registers the guns that are made on GT's Assembly Line (LuV and ZPM), as {@link Spec}s. A gun's own
 * definition calls {@link #spec} when it is asked (late, not in a static initializer, because the research item may be
 * another gun) and hands the result to {@link #register} from its recipes hook. The registration is the component
 * table's ({@code ComponentRecipes.addSpec}), so a gun line and a component line behave the same. No Minecraft types
 * here; common side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM, Assembly Line)
public final class GunLineRecipes {

    private GunLineRecipes() {}

    /**
     * An Assembly Line spec for a gun.
     *
     * @param id          the sheet id
     * @param gun         the gun's bare registry name
     * @param tier        the gun's tier, which sets the EU/t
     * @param seconds     duration in seconds
     * @param research    the research item, scanned and consumed
     * @param scanSeconds how long the scan takes
     * @param scanTier    the scanner's tier
     * @param fluids      the fluid inputs (the line needs at least one)
     * @param inputs      the item inputs in slot order
     * @return the spec
     */
    public static Spec spec(String id, String gun, Tier tier, int seconds, Input research, int scanSeconds,
        Tier scanTier, FluidInput[] fluids, Input... inputs) {
        return new Spec(
            new Row(id, gun, 1, tier, Target.ASSEMBLY_LINE, seconds, 0, inputs),
            Arrays.asList(fluids),
            new Research(research, scanSeconds, scanTier),
            null);
    }

    /**
     * @param material a GT material name
     * @param amount   millibuckets
     * @return a molten-metal fluid input
     */
    public static FluidInput molten(String material, int amount) {
        return new FluidInput(material, FluidKind.MOLTEN, amount);
    }

    /**
     * @param material a GT material name
     * @param amount   millibuckets
     * @return a plain GT fluid input
     */
    public static FluidInput fluid(String material, int amount) {
        return new FluidInput(material, FluidKind.FLUID, amount);
    }

    /**
     * Registers the spec on GT's Assembly Line list. Call from a gun definition's recipes hook, which runs on GT's
     * before-postload hook.
     *
     * @param spec the spec from {@link #spec}
     */
    public static void register(Spec spec) {
        ComponentRecipes.addSpec(spec);
    }

    /**
     * The scenario of a gun's Assembly Line recipe: GT's line list holds it with the row's EU/t, time and count, its
     * research item and scan, and the line's EU/t fits the gun's tier voltage (so a lower-tier line cannot run it).
     *
     * @param name        the scenario name, for example {@code gun.nether_blaster.recipe}
     * @param description what it checks, for the docs
     * @param spec        the gun's spec, read when the scenario runs
     * @return the scenario
     */
    public static Scenario scenario(String name, String description, Supplier<Spec> spec) {
        return new Scenario() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public ScenarioResult run(ScenarioContext ctx) {
                ScenarioResult.Builder out = ScenarioResult.builder(name);
                ComponentScenarios.ComponentRecipesScenario.checkAssemblyLine(out, spec.get());
                return out.build();
            }
        };
    }
}
