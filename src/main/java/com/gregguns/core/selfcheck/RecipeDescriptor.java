package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minecraft-free description of one recipe that involves a GregGuns item, as the dev self-check sees it.
 *
 * <p>
 * Both sides.
 */
public final class RecipeDescriptor {

    private final String id;
    private final long eut;
    private final boolean emptySlotsAllowed;
    private final List<StackDescriptor> inputs;
    private final List<StackDescriptor> outputs;

    /**
     * @param id                recipe name for messages, for example {@code "gt:assembler #12"}
     * @param eut               EU/t of a machine recipe; zero for the crafting table
     * @param emptySlotsAllowed {@code true} for shaped grids, where a {@code null} entry means an empty slot
     * @param inputs            item, ore-dict and fluid inputs
     * @param outputs           item and fluid outputs
     */
    public RecipeDescriptor(String id, long eut, boolean emptySlotsAllowed, List<StackDescriptor> inputs,
        List<StackDescriptor> outputs) {
        this.id = id;
        this.eut = eut;
        this.emptySlotsAllowed = emptySlotsAllowed;
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    /**
     * @return the recipe name for messages
     */
    public String id() {
        return id;
    }

    /**
     * @return the recipe's EU/t; zero for the crafting table
     */
    public long eut() {
        return eut;
    }

    /**
     * @return whether {@link StackDescriptor.Kind#EMPTY_SLOT} inputs are legitimate in this recipe
     */
    public boolean emptySlotsAllowed() {
        return emptySlotsAllowed;
    }

    /**
     * @return the inputs, unmodifiable
     */
    public List<StackDescriptor> inputs() {
        return inputs;
    }

    /**
     * @return the outputs, unmodifiable
     */
    public List<StackDescriptor> outputs() {
        return outputs;
    }
}
