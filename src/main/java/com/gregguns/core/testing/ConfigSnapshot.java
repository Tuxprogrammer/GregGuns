package com.gregguns.core.testing;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.gregguns.GregGuns;
import com.gregguns.core.config.GregGunsConfig;

/**
 * A copy of the static fields of config classes, taken before a scenario and put back after it, so a scenario that
 * changes a config switch and fails to undo it (on some exit path, or at all) cannot leak the change into the next
 * scenario. Every static, non-final field of the given classes is copied; arrays are copied element-wise so an in-place
 * edit is undone too. Plain Java.
 */
// Spec: CONTRIBUTING.md (Testing: scenarios restore every global switch they change)
public final class ConfigSnapshot {

    private final List<Field> fields = new ArrayList<>();
    private final List<Object> values = new ArrayList<>();

    private ConfigSnapshot() {}

    /** @return a snapshot of every {@link GregGunsConfig#CLASSES} class */
    public static ConfigSnapshot ofAll() {
        return of(GregGunsConfig.CLASSES);
    }

    /**
     * @param classes the config classes to copy
     * @return a snapshot of their static, non-final fields
     */
    public static ConfigSnapshot of(List<Class<?>> classes) {
        ConfigSnapshot s = new ConfigSnapshot();
        for (Class<?> c : classes) {
            for (Field f : c.getDeclaredFields()) {
                int m = f.getModifiers();
                if (!Modifier.isStatic(m) || Modifier.isFinal(m) || f.isSynthetic()) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object value = copy(f.get(null));
                    s.fields.add(f);
                    s.values.add(value);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    GregGuns.LOG.warn("Config snapshot skips {}.{}: {}", c.getSimpleName(), f.getName(), e.toString());
                }
            }
        }
        return s;
    }

    /** @return how many fields the snapshot holds */
    public int size() {
        return fields.size();
    }

    /**
     * Puts every field back to the value it had when the snapshot was taken.
     *
     * @return the names ({@code Class.field}) of the fields that had changed, empty when nothing leaked
     */
    public List<String> restore() {
        List<String> changed = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            try {
                Object now = f.get(null);
                Object saved = values.get(i);
                if (!same(now, saved)) {
                    changed.add(
                        f.getDeclaringClass()
                            .getSimpleName() + "."
                            + f.getName());
                }
                f.set(null, copy(saved));
            } catch (ReflectiveOperationException | RuntimeException e) {
                GregGuns.LOG.warn("Config restore of {} failed: {}", f.getName(), e.toString());
            }
        }
        return changed;
    }

    private static Object copy(Object v) {
        if (v == null || !v.getClass()
            .isArray()) {
            return v;
        }
        int n = Array.getLength(v);
        Object out = Array.newInstance(
            v.getClass()
                .getComponentType(),
            n);
        for (int i = 0; i < n; i++) {
            Array.set(out, i, copy(Array.get(v, i)));
        }
        return out;
    }

    private static boolean same(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getClass()
            .isArray()
            && b.getClass()
                .isArray()) {
            int n = Array.getLength(a);
            if (n != Array.getLength(b)) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (!same(Array.get(a, i), Array.get(b, i))) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }
}
