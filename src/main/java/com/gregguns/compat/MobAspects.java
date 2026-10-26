package com.gregguns.compat;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gregguns.GregGuns;

/**
 * Thaumcraft scan aspects for the GregGuns NPCs, registered in code through Thaumcraft's own
 * {@code ThaumcraftApi.registerEntityTag} (the call ThaumcraftMobAspects makes for its files), so the pack needs no
 * {@code config/ThaumcraftMobAspects/gregguns.cfg}. Thaumcraft is reached by reflection: GregGuns loads without it.
 */
// Source:
// https://github.com/GTNewHorizons/ThaumcraftMobAspects/blob/1.2.1-GTNH/src/main/java/iguanaman/thaumcraftmobaspects/ThaumcraftMobAspects.java#L125-L136
// (builds an AspectList from Aspect.aspects and calls ThaumcraftApi.registerEntityTag(String, AspectList)).
// Spec: docs/gregguns-spec/10-entities-npcs/registry-and-pack-hooks.md
public final class MobAspects {

    /** Thaumcraft's FML mod id. */
    public static final String MOD_ID = "Thaumcraft";

    private MobAspects() {}

    /** @return NPC registry suffix to (aspect tag, amount) pairs, as {@code "tag", n, "tag", n ...} */
    public static Map<String, Object[]> table() {
        Map<String, Object[]> t = new LinkedHashMap<>();
        Object[] zombie = { "exanimis", 3, "humanus", 1, "telum", 1 };
        t.put("ZombieSoldier", zombie);
        t.put("ZombieFarmer", zombie);
        t.put("ZombieMiner", zombie);
        t.put("ZombiePigmanSoldier", new Object[] { "exanimis", 3, "humanus", 1, "telum", 1, "ignis", 2 });
        t.put("SkeletonSoldier", new Object[] { "exanimis", 3, "humanus", 1, "telum", 1, "terra", 1 });
        Object[] human = { "humanus", 3, "telum", 2, "tutamen", 1 };
        for (String n : new String[] { "ArmySoldier", "Commando", "Bandit", "DictatorDave", "Outcast" }) {
            t.put(n, human);
        }
        t.put("PsychoSteve", new Object[] { "humanus", 3, "telum", 2, "ignis", 1, "perditio", 1 });
        t.put("Stormtrooper", new Object[] { "humanus", 2, "machina", 2, "telum", 2, "tutamen", 1 });
        t.put("CyberDemon", new Object[] { "machina", 2, "ignis", 2, "telum", 2, "perditio", 1 });
        Object[] mutant = { "humanus", 1, "bestia", 3, "telum", 1 };
        t.put("SuperMutantBasic", mutant);
        t.put("SuperMutantHeavy", mutant);
        t.put("SuperMutantElite", mutant);
        t.put("AlienBug", new Object[] { "bestia", 3, "alienis", 2 });
        t.put("AttackHelicopter", new Object[] { "machina", 3, "aer", 2, "telum", 2 });
        t.put("SimpleTurret", new Object[] { "machina", 3, "telum", 2, "tutamen", 1 });
        return t;
    }

    private static Method findRegister(Class<?> api, Class<?> listClass) throws NoSuchMethodException {
        for (Method m : api.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getName()
                .equals("registerEntityTag") && p.length >= 2
                && p[0] == String.class
                && p[1] == listClass
                && (p.length == 2 || p.length == 3 && p[2].isArray())) {
                return m;
            }
        }
        throw new NoSuchMethodException("ThaumcraftApi.registerEntityTag(String, AspectList)");
    }

    /**
     * Registers the table with Thaumcraft. Never throws.
     *
     * @return the number of entities registered
     */
    public static int apply() {
        try {
            Class<?> api = Class.forName("thaumcraft.api.ThaumcraftApi");
            Class<?> listClass = Class.forName("thaumcraft.api.aspects.AspectList");
            Class<?> aspectClass = Class.forName("thaumcraft.api.aspects.Aspect");
            Object aspects = aspectClass.getField("aspects")
                .get(null);
            Method get = Map.class.getMethod("get", Object.class);
            Method add = listClass.getMethod("add", aspectClass, int.class);
            Method register = findRegister(api, listClass);
            // TC 4.2 declares (String, AspectList, EntityTagsNBT...); ThaumcraftMobAspects calls it with two arguments.
            Object noNbt = register.getParameterTypes().length == 3
                ? java.lang.reflect.Array.newInstance(register.getParameterTypes()[2].getComponentType(), 0)
                : null;
            int count = 0;
            for (Map.Entry<String, Object[]> e : table().entrySet()) {
                Object list = listClass.newInstance();
                for (int i = 0; i < e.getValue().length; i += 2) {
                    Object aspect = get.invoke(aspects, e.getValue()[i]);
                    if (aspect != null) {
                        add.invoke(list, aspect, e.getValue()[i + 1]);
                    }
                }
                if (noNbt != null) {
                    register.invoke(null, "gregguns." + e.getKey(), list, noNbt);
                } else {
                    register.invoke(null, "gregguns." + e.getKey(), list);
                }
                count++;
            }
            GregGuns.LOG.info("Thaumcraft: scan aspects registered for {} GregGuns NPCs", count);
            return count;
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.warn("Thaumcraft present but NPC aspects could not be registered", e);
            return 0;
        }
    }
}
