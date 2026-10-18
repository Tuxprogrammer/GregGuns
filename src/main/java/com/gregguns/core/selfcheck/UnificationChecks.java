package com.gregguns.core.selfcheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The unification rule (GGS-06-10): no GregGuns item may be ore-dictionaried as a GT part, and GT's unificator must not
 * treat one as a material part. A GregGuns {@code plateSteel} would sit in every GT recipe that accepts the name.
 *
 * <p>
 * An ore name collides when it is a GT {@code OrePrefixes} name, or starts with one followed by an upper-case letter
 * (the shape of {@code plateSteel}, {@code dustTin}, {@code circuitBasic}). Pure logic, both sides; the glue in
 * {@link SelfCheckRunner} reads the live registries.
 */
// Spec: docs/gregguns-spec/06-items/components-and-materials.md (GGS-06-10), docs/design/0002-machine-deconfliction.md
public final class UnificationChecks {

    private UnificationChecks() {}

    /**
     * @param items         every GregGuns item with its ore names and GT association
     * @param gtPrefixNames the names of all GT {@code OrePrefixes} constants
     * @return one violation per colliding ore name and per unified stack
     */
    public static List<String> check(List<UnificationDescriptor> items, Collection<String> gtPrefixNames) {
        List<String> violations = new ArrayList<>();
        for (UnificationDescriptor item : items) {
            for (String ore : item.oreNames()) {
                String prefix = collidingPrefix(ore, gtPrefixNames);
                if (prefix != null) {
                    violations.add(
                        "item " + item.registryName()
                            + ": ore name "
                            + ore
                            + " shares the GT prefix "
                            + prefix
                            + "; GT owns that name (use a namespaced name such as partGunReceiver)");
                }
            }
            if (item.associatedPrefix() != null) {
                violations.add(
                    "item " + item.registryName()
                        + ": the GT unificator treats it as "
                        + item.associatedPrefix()
                        + (item.associatedMaterial() == null ? "" : "." + item.associatedMaterial())
                        + "; a GregGuns item must not be a GT material part");
            }
        }
        return violations;
    }

    /**
     * @param oreName       an ore-dictionary name
     * @param gtPrefixNames the GT prefix names
     * @return the longest GT prefix the name equals or starts with (followed by an upper-case letter), or {@code null}
     */
    public static String collidingPrefix(String oreName, Collection<String> gtPrefixNames) {
        String best = null;
        for (String prefix : gtPrefixNames) {
            if (prefix.isEmpty() || !oreName.startsWith(prefix)) {
                continue;
            }
            boolean exact = oreName.length() == prefix.length();
            boolean camel = !exact && Character.isUpperCase(oreName.charAt(prefix.length()));
            if ((exact || camel) && (best == null || prefix.length() > best.length())) {
                best = prefix;
            }
        }
        return best;
    }
}
