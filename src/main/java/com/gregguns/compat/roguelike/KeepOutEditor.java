package com.gregguns.compat.roguelike;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The world editor a Roguelike dungeon gets when a silo complex lies within its reach (D303). It is a
 * {@link java.lang.reflect.Proxy} over Roguelike Dungeons' own editor interface, so GregGuns never links against its
 * classes; every call goes to the real editor except:
 *
 * <ul>
 * <li><b>{@code getBiome}</b>, while the dungeon is still looking for its entrance (before its first write): a column
 * within the entrance margin of a complex reads as ocean. Roguelike Dungeons refuses an ocean entrance and tries its
 * next spot, so the dungeon moves; when all of its tries fall near the complex, there is no dungeon.</li>
 * <li><b>{@code setBlock}</b> inside a box plus {@link KeepOut#WRITE_MARGIN}, at every Y, returns false without
 * writing. Its chest, spawner and other tile code checks that return before touching the tile, so a refused chest or
 * spawner is skipped whole.</li>
 * <li><b>{@code setBlockMetadata}</b> there does nothing, and <b>{@code getTileEntity}</b> there returns null, so a
 * dungeon never fills one of our chests or reaches into a GT machine.</li>
 * <li><b>{@code fillDown}</b> on a column there, and <b>{@code spiralStairStep}</b> whose 3 x 3 step meets the margin,
 * do nothing: the real editor writes those through itself, past this proxy.</li>
 * </ul>
 *
 * Coordinates are read from the editor's coordinate objects through their {@code getX} and {@code getZ} methods.
 */
// Source:
// https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/worldgen/IWorldEditor.java
// (the interface), .../worldgen/WorldEditor.java#L87-L121 (spiralStairStep and fillDown write through this),
// .../dungeon/Dungeon.java#L51-L66 and #L165-L176 (generateNear's 50 tries; validLocation refuses OCEAN first),
// .../treasure/TreasureChest.java#L32-L36 and .../worldgen/Spawner.java#L51-L55 (set() checked before the tile).
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Roguelike Dungeons keep out); D303
public final class KeepOutEditor implements InvocationHandler {

    private final Object delegate;
    private final KeepOut keepOut;
    private final int entranceMargin;
    private final Object ocean;
    private boolean locating = true;
    private int refused;
    private int dodged;

    private static final Map<Class<?>, Method[]> COORD_GETTERS = new ConcurrentHashMap<Class<?>, Method[]>();

    /**
     * @param delegate       Roguelike Dungeons' own editor for the world
     * @param keepOut        the complex boxes in reach
     * @param entranceMargin how far from a box the entrance must lie ({@link KeepOut#entranceMargin})
     * @param ocean          what {@code getBiome} answers near a box (the ocean biome)
     */
    public KeepOutEditor(Object delegate, KeepOut keepOut, int entranceMargin, Object ocean) {
        this.delegate = delegate;
        this.keepOut = keepOut;
        this.entranceMargin = entranceMargin;
        this.ocean = ocean;
    }

    /** @return writes refused inside the boxes */
    public int refused() {
        return refused;
    }

    /** @return entrance spots refused for lying near a box */
    public int dodged() {
        return dodged;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        int n = args == null ? 0 : args.length;
        if (method.getDeclaringClass() == Object.class) {
            if (name.equals("equals")) return proxy == args[0];
            if (name.equals("hashCode")) return System.identityHashCode(proxy);
            return "KeepOutEditor(" + delegate + ")";
        }
        switch (name) {
            case "getBiome":
                if (locating && n == 1 && near(args[0], entranceMargin)) {
                    dodged++;
                    return ocean;
                }
                break;
            case "setBlock":
                locating = false;
                if (n >= 1 && near(args[0], KeepOut.WRITE_MARGIN)) {
                    refused++;
                    return Boolean.FALSE;
                }
                break;
            case "setBlockMetadata":
                if (n >= 1 && near(args[0], KeepOut.WRITE_MARGIN)) return null;
                break;
            case "getTileEntity":
                if (n >= 1 && near(args[0], KeepOut.WRITE_MARGIN)) return null;
                break;
            case "fillDown":
                locating = false;
                if (n >= 2 && near(args[1], KeepOut.WRITE_MARGIN)) {
                    refused++;
                    return null;
                }
                break;
            case "spiralStairStep":
                locating = false;
                if (n >= 2) {
                    int[] xz = xz(args[1]);
                    if (xz != null
                        && keepOut.touches(xz[0] - 1, xz[1] - 1, xz[0] + 1, xz[1] + 1, KeepOut.WRITE_MARGIN)) {
                        refused++;
                        return null;
                    }
                }
                break;
            default:
                break;
        }
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private boolean near(Object coord, int margin) {
        int[] xz = xz(coord);
        return xz != null && keepOut.contains(xz[0], xz[1], margin);
    }

    /** @return {@code {x, z}} of a coordinate object, or null when it has no getX and getZ */
    static int[] xz(Object coord) {
        if (coord == null) return null;
        Method[] g = COORD_GETTERS.computeIfAbsent(coord.getClass(), c -> {
            try {
                return new Method[] { c.getMethod("getX"), c.getMethod("getZ") };
            } catch (NoSuchMethodException e) {
                return new Method[0];
            }
        });
        if (g.length < 2) return null;
        try {
            return new int[] { ((Number) g[0].invoke(coord)).intValue(), ((Number) g[1].invoke(coord)).intValue() };
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}
