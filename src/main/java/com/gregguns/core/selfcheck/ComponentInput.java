package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the component checks ({@link UnificationChecks}, {@link ComponentChecks}) need from the live game, as
 * plain
 * data: the unification descriptors and GT prefix names, the registered components, the allowlist and the components
 * whose recipe is deferred because a module is off.
 */
public final class ComponentInput {

    private final List<UnificationDescriptor> unification;
    private final List<String> gtPrefixNames;
    private final List<String> components;
    private final Map<String, String> allowlist;
    private final List<String> deferred;

    /**
     * @param unification   every GregGuns item with its ore names and GT association
     * @param gtPrefixNames the names of all GT {@code OrePrefixes} constants
     * @param components    full registry names of the registered components
     * @param allowlist     full registry name to reason, for components that need no recipe
     * @param deferred      full registry names of components whose recipe is skipped because a module is off
     */
    public ComponentInput(List<UnificationDescriptor> unification, List<String> gtPrefixNames, List<String> components,
        Map<String, String> allowlist, List<String> deferred) {
        this.unification = Collections.unmodifiableList(new ArrayList<>(unification));
        this.gtPrefixNames = Collections.unmodifiableList(new ArrayList<>(gtPrefixNames));
        this.components = Collections.unmodifiableList(new ArrayList<>(components));
        this.allowlist = Collections.unmodifiableMap(new LinkedHashMap<>(allowlist));
        this.deferred = Collections.unmodifiableList(new ArrayList<>(deferred));
    }

    /** @return the unification descriptors */
    public List<UnificationDescriptor> unification() {
        return unification;
    }

    /** @return the GT prefix names */
    public List<String> gtPrefixNames() {
        return gtPrefixNames;
    }

    /** @return the registered components */
    public List<String> components() {
        return components;
    }

    /** @return the no-recipe allowlist */
    public Map<String, String> allowlist() {
        return allowlist;
    }

    /** @return the components whose recipe is deferred */
    public List<String> deferred() {
        return deferred;
    }
}
