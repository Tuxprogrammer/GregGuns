package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the unification check needs to know about one GregGuns item: its ore-dictionary names and what GT's unificator
 * thinks the stack is. Plain data, no Minecraft types.
 */
public final class UnificationDescriptor {

    private final String registryName;
    private final List<String> oreNames;
    private final String associatedPrefix;
    private final String associatedMaterial;

    /**
     * @param registryName       the full registry name, for example {@code gregguns:part_barrel_iron}
     * @param oreNames           every ore-dictionary name the item is registered under
     * @param associatedPrefix   the GT {@code OrePrefixes} name the unificator associates with the stack (for example
     *                           "plate"), or {@code null} when GT sees it as a plain item
     * @param associatedMaterial the GT material name of that association, or {@code null}
     */
    public UnificationDescriptor(String registryName, List<String> oreNames, String associatedPrefix,
        String associatedMaterial) {
        this.registryName = registryName;
        this.oreNames = Collections.unmodifiableList(new ArrayList<>(oreNames));
        this.associatedPrefix = associatedPrefix;
        this.associatedMaterial = associatedMaterial;
    }

    /** @return the full registry name */
    public String registryName() {
        return registryName;
    }

    /** @return the ore-dictionary names */
    public List<String> oreNames() {
        return oreNames;
    }

    /** @return the GT prefix name the unificator associates with the stack, or {@code null} */
    public String associatedPrefix() {
        return associatedPrefix;
    }

    /** @return the GT material name of the association, or {@code null} */
    public String associatedMaterial() {
        return associatedMaterial;
    }
}
