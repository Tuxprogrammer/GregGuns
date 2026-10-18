package com.gregguns.core.selfcheck;

import com.gregguns.api.Tier;

/**
 * Minecraft-free description of one registered GregGuns item or block for the dev self-check.
 *
 * <p>
 * Both sides.
 */
public final class ContentDescriptor {

    private final String registryName;
    private final boolean block;
    private final String langKey;
    private final String iconName;
    private final Tier tier;
    private final boolean hidden;
    private final boolean hasCreativeTab;

    /**
     * @param registryName   full registry name, for example {@code "gregguns:pistol_9mm"}
     * @param block          {@code true} for a block (textures live in {@code textures/blocks})
     * @param langKey        the full lang key of the display name, for example {@code "item.gregguns.pistol_9mm.name"}
     * @param iconName       the icon string ({@code "domain:name"}), or {@code null} when none is set
     * @param tier           the declared tier, or {@code null} when the content declares none
     * @param hidden         {@code true} when it carries the explicit hidden flag
     * @param hasCreativeTab {@code true} when a creative tab is assigned
     */
    public ContentDescriptor(String registryName, boolean block, String langKey, String iconName, Tier tier,
        boolean hidden, boolean hasCreativeTab) {
        this.registryName = registryName;
        this.block = block;
        this.langKey = langKey;
        this.iconName = iconName;
        this.tier = tier;
        this.hidden = hidden;
        this.hasCreativeTab = hasCreativeTab;
    }

    /**
     * @return the full registry name
     */
    public String registryName() {
        return registryName;
    }

    /**
     * @return whether this is a block
     */
    public boolean isBlock() {
        return block;
    }

    /**
     * @return the lang key of the display name
     */
    public String langKey() {
        return langKey;
    }

    /**
     * @return the icon string, or {@code null}
     */
    public String iconName() {
        return iconName;
    }

    /**
     * @return the declared tier, or {@code null}
     */
    public Tier tier() {
        return tier;
    }

    /**
     * @return whether the explicit hidden flag is set
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return whether a creative tab is assigned
     */
    public boolean hasCreativeTab() {
        return hasCreativeTab;
    }
}
