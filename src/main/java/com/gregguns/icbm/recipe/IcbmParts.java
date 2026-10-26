package com.gregguns.icbm.recipe;

import java.util.EnumMap;
import java.util.Map;

import com.gregguns.core.registry.ModuleRegistrar;

/** Registers the eight ICBM parts and holds them for the recipes. Common side, preInit. */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Parts)
public final class IcbmParts {

    private static final Map<IcbmPart, ItemIcbmPart> ITEMS = new EnumMap<>(IcbmPart.class);

    private IcbmParts() {}

    /**
     * Registers every part, whatever the module flag says (content always registers).
     *
     * @param registrar the ICBM module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (IcbmPart part : IcbmPart.values()) {
            ItemIcbmPart item = registrar.registerItem(
                part.registryName(),
                part.registryName()
                    .startsWith("guidance_") ? new ItemGuidanceComputer(part) : new ItemIcbmPart(part));
            item.setTextureName("gregguns:" + part.iconName());
            ITEMS.put(part, item);
        }
    }

    /**
     * @param part a part
     * @return its item; {@code null} before registration
     */
    public static ItemIcbmPart get(IcbmPart part) {
        return ITEMS.get(part);
    }
}
