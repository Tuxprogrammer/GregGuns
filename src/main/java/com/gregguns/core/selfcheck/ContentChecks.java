package com.gregguns.core.selfcheck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Self-check over registered GregGuns items and blocks: a tier, a lang key, a texture, and a creative tab or the hidden
 * flag.
 *
 * <p>
 * Spec: CONTRIBUTING.md (Testing, dev self-check; Assets and localisation). Both sides, no Minecraft types.
 */
public final class ContentChecks {

    /** Classpath location of the English lang file, which is the source of truth for lang keys. */
    public static final String LANG_PATH = "assets/gregguns/lang/en_US.lang";

    private ContentChecks() {}

    /**
     * Reads the keys defined in a {@code .lang} file's text.
     *
     * @param text the file content; lines are {@code key=value}, and lines starting with {@code #} are comments
     * @return the defined keys
     */
    public static Set<String> parseLangKeys(String text) {
        Set<String> keys = new HashSet<>();
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            int equals = trimmed.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || equals <= 0) {
                continue;
            }
            keys.add(
                trimmed.substring(0, equals)
                    .trim());
        }
        return keys;
    }

    /**
     * Resolves an icon string to the classpath path of its texture.
     *
     * @param iconName the icon string, {@code "domain:name"} or just {@code "name"} (which Minecraft reads as the
     *                 {@code minecraft} domain)
     * @param block    {@code true} for a block texture
     * @return {@code assets/<domain>/textures/<items|blocks>/<name>.png}
     */
    public static String texturePath(String iconName, boolean block) {
        int colon = iconName.indexOf(':');
        String domain = colon < 0 ? "minecraft" : iconName.substring(0, colon);
        String name = colon < 0 ? iconName : iconName.substring(colon + 1);
        return "assets/" + domain + "/textures/" + (block ? "blocks" : "items") + "/" + name + ".png";
    }

    /**
     * Checks all content.
     *
     * @param contents  every registered GregGuns item and block
     * @param resources the mod's classpath resources
     * @return one message per violation; empty when all content is complete
     */
    public static List<String> check(List<ContentDescriptor> contents, ResourceSource resources) {
        List<String> violations = new ArrayList<>();
        Set<String> langKeys = null;
        try {
            langKeys = parseLangKeys(resources.readText(LANG_PATH));
        } catch (IOException e) {
            violations.add("cannot read " + LANG_PATH + ": " + e.getMessage());
        }
        for (ContentDescriptor content : contents) {
            String name = (content.isBlock() ? "block " : "item ") + content.registryName() + ": ";
            if (content.tier() == null) {
                violations.add(name + "declares no tier (implement com.gregguns.api.Tiered)");
            }
            if (langKeys != null && !langKeys.contains(content.langKey())) {
                violations.add(name + "lang key " + content.langKey() + " is missing from " + LANG_PATH);
            }
            if (content.iconName() == null) {
                violations.add(name + "has no icon name (set the texture name to gregguns:<name>)");
            } else {
                String texture = texturePath(content.iconName(), content.isBlock());
                if (!resources.exists(texture)) {
                    violations.add(name + "texture " + texture + " is missing");
                }
            }
            if (!content.hasCreativeTab() && !content.isHidden()) {
                violations.add(name + "has no creative tab and is not marked HiddenFromCreativeTab");
            }
        }
        return violations;
    }
}
