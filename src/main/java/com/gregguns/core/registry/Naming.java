package com.gregguns.core.registry;

import java.util.regex.Pattern;

/**
 * Every name GregGuns derives from a content name such as {@code pistol_9mm}. Pure string logic with no Minecraft
 * types, so the rules are unit-tested and {@link ModuleRegistrar} cannot drift from them.
 *
 * <p>
 * Target lang keys, from CONTRIBUTING.md: {@code item.gregguns.<name>} and {@code tile.gregguns.<name>}. Vanilla
 * 1.7.10 prefixes the unlocalized name with {@code item.} (Item) or {@code tile.} (Block), so the unlocalized name we
 * set is {@code gregguns.<name>}. The registry name passed to {@code GameRegistry} stays the bare {@code <name>}; Forge
 * adds the {@code gregguns:} namespace itself.
 */
// Spec: CONTRIBUTING.md (Assets and localisation: lang key namespace)
public final class Naming {

    /** The mod id, asset domain and lang namespace. Duplicated from {@code GregGuns.MODID} to keep this class pure. */
    public static final String NAMESPACE = "gregguns";

    private static final Pattern CONTENT_NAME = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern SOUND_NAME = Pattern.compile("[a-z][a-z0-9_]*(\\.[a-z0-9_]+)*");

    private Naming() {}

    /**
     * Checks a content name (item, block, entity or tile entity): lower snake case, starting with a letter. This keeps
     * lang keys, texture file names and registry names consistent on case-insensitive file systems.
     *
     * @param name the bare name, for example {@code pistol_9mm}
     * @return {@code name}, for chaining
     * @throws IllegalArgumentException if the name breaks the rule
     */
    public static String requireContentName(String name) {
        if (name == null || !CONTENT_NAME.matcher(name)
            .matches()) {
            throw new IllegalArgumentException("Content name must match [a-z][a-z0-9_]*: " + name);
        }
        return name;
    }

    /**
     * @param name a content name
     * @return the value for {@code Item.setUnlocalizedName} / {@code Block.setBlockName}: {@code gregguns.<name>}
     */
    public static String unlocalizedName(String name) {
        return NAMESPACE + "." + requireContentName(name);
    }

    /**
     * @param name a content name
     * @return the name given to {@code GameRegistry.registerItem/registerBlock} (Forge prefixes the mod id)
     */
    public static String registryName(String name) {
        return requireContentName(name);
    }

    /**
     * @param name a content name
     * @return the full registry id as it appears in the game: {@code gregguns:<name>}
     */
    public static String qualifiedName(String name) {
        return NAMESPACE + ":" + requireContentName(name);
    }

    /**
     * @param name a content name
     * @return the texture name for {@code setTextureName} / {@code setBlockTextureName}: {@code gregguns:<name>}
     */
    public static String textureName(String name) {
        return qualifiedName(name);
    }

    /**
     * @param name an item name
     * @return the lang key {@code item.gregguns.<name>.name} that the in-game item name is looked up under
     */
    public static String itemLangKey(String name) {
        return "item." + unlocalizedName(name) + ".name";
    }

    /**
     * @param name a block name
     * @return the lang key {@code tile.gregguns.<name>.name} that the in-game block name is looked up under
     */
    public static String blockLangKey(String name) {
        return "tile." + unlocalizedName(name) + ".name";
    }

    /**
     * @param name an entity name
     * @return the lang key {@code entity.gregguns.<name>.name}
     */
    public static String entityLangKey(String name) {
        // Source: FML's EntityRegistry registers the entity as "<modid>.<name>" in EntityList, and vanilla looks up
        // "entity.<that string>.name".
        // https://github.com/MinecraftForge/FML/blob/1.7.10/src/main/java/cpw/mods/fml/common/registry/EntityRegistry.java#L163
        return "entity." + unlocalizedName(name) + ".name";
    }

    /**
     * @param name a tile entity name
     * @return the id written to the tile entity's NBT: {@code gregguns:<name>}
     */
    public static String tileEntityId(String name) {
        return qualifiedName(name);
    }

    /**
     * Sounds are declared in {@code sounds.json} and played by event name; 1.7.10 has no sound registry call.
     *
     * @param event a sound event such as {@code gun.pistol.fire} (dots separate path parts)
     * @return the name to pass to the sound system: {@code gregguns:gun.pistol.fire}
     * @throws IllegalArgumentException if the event name is malformed
     */
    public static String soundEvent(String event) {
        if (event == null || !SOUND_NAME.matcher(event)
            .matches()) {
            throw new IllegalArgumentException("Sound event must match [a-z][a-z0-9_]*(.[a-z0-9_]+)*: " + event);
        }
        return NAMESPACE + ":" + event;
    }
}
