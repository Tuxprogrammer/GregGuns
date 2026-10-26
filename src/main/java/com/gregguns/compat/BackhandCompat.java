package com.gregguns.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gregguns.GregGuns;

/**
 * Registers GregGuns' guns with Backhand in memory, so a server owner does not have to edit {@code backhand.cfg}.
 *
 * <p>
 * Backhand has no API for its two item lists, but they are public static {@code String[]} fields of a GTNHLib
 * {@code @Config} class that it reads on every use. This class appends the gun names to those arrays after Backhand's
 * config has loaded. It writes nothing to disk: the file keeps whatever the owner wrote, and the entries are re-added
 * on the next start. {@link #merge} is the pure part and is unit tested; {@link #apply} is the reflection.
 */
// Source:
// https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/utils/BackhandConfig.java#L28-L48
// (offhandBlacklist and mainhandUseStopsOffhandFallback are public static String[]) and
// https://github.com/GTNewHorizons/Backhand/blob/1.8.16/src/main/java/xonin/backhand/Backhand.java#L59-L77
// (both are matched against stack.getItem().delegate.name(), i.e. "modid:name", on every check).
// Spec: docs/gregguns-spec/02-guns/offhand-backhand.md
public final class BackhandCompat {

    /** Backhand's FML mod id. */
    public static final String MOD_ID = "backhand";
    static final String CONFIG_CLASS = "xonin.backhand.utils.BackhandConfig";
    static final String BLACKLIST_FIELD = "offhandBlacklist";
    static final String STOP_FIELD = "mainhandUseStopsOffhandFallback";

    private BackhandCompat() {}

    /**
     * @param existing the current list, possibly null (never loaded)
     * @param add      names to add
     * @return a new array holding {@code existing} in order, then every name of {@code add} not already present
     */
    public static String[] merge(String[] existing, Collection<String> add) {
        Set<String> out = new LinkedHashSet<>();
        if (existing != null) {
            for (String s : existing) {
                out.add(s);
            }
        }
        out.addAll(add);
        return out.toArray(new String[0]);
    }

    /**
     * Adds {@code gunNames} to both Backhand lists. Safe to call more than once. Never throws: a Backhand whose
     * config class changed shape only costs a warning.
     *
     * @param gunNames qualified registry names ({@code gregguns:revolver})
     * @return true if both lists were updated
     */
    public static boolean apply(List<String> gunNames) {
        try {
            Class<?> config = Class.forName(CONFIG_CLASS);
            boolean a = extend(config, BLACKLIST_FIELD, gunNames);
            boolean b = extend(config, STOP_FIELD, gunNames);
            if (a && b) {
                GregGuns.LOG.info("Backhand: {} GregGuns guns added to its offhand lists (in memory)", gunNames.size());
            }
            return a && b;
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.warn("Backhand present but its config could not be extended; add the guns by hand", e);
            return false;
        }
    }

    /**
     * Appends {@code names} to a public static {@code String[]} field of another mod's config class, in memory.
     *
     * @param config    the config class
     * @param fieldName the field
     * @param names     entries to add
     * @return false if the field is not a {@code String[]}
     * @throws ReflectiveOperationException if the class shape is not as expected
     */
    static boolean extend(Class<?> config, String fieldName, List<String> names) throws ReflectiveOperationException {
        Field field = config.getField(fieldName);
        if (field.getType() != String[].class) {
            return false;
        }
        field.set(null, merge((String[]) field.get(null), new ArrayList<>(names)));
        return true;
    }
}
