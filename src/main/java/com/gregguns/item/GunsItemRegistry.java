package com.gregguns.item;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.item.ammo.AmmoItems;
import com.gregguns.item.grenade.GrenadeItems;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.melee.MeleeItems;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;

/**
 * Registers every item of the guns module in the order {@link GunsItemOrder} derives (tier by tier; each gun followed
 * by its akimbo variant, magazines and ammo), so the item ids, NEI's default sort and the creative tab all read in
 * build order. Common side, preInit.
 */
// Spec: docs/gregguns-spec/06-items/index.md (registration order)
public final class GunsItemRegistry {

    private GunsItemRegistry() {}

    /**
     * Registers the guns, ammo, magazines, parts, components, melee tools and grenade items, in order. Call once from
     * {@code GunsModule.preInit}; the dev probe is registered last.
     *
     * @param registrar the guns module's registrar
     */
    public static void registerAll(ModuleRegistrar registrar) {
        for (GunsItemOrder.Slot slot : GunsItemOrder.build()) {
            String name = slot.name();
            switch (slot.kind()) {
                case GUN:
                    GunItems.registerOne(registrar, name);
                    break;
                case MAGAZINE_EMPTY:
                case MAGAZINE:
                case AMMO:
                    AmmoItems.registerOne(registrar, name);
                    break;
                case MELEE:
                    MeleeItems.registerOne(registrar, name);
                    break;
                case GRENADE:
                    GrenadeItems.registerOne(registrar, name);
                    break;
                case PART:
                    PartItems.registerOne(registrar, name);
                    break;
                case COMPONENT:
                    ComponentItems.registerOne(registrar, name);
                    break;
                default:
                    throw new IllegalStateException("unhandled item kind " + slot.kind());
            }
        }
        GunItems.registerProbe(registrar);
        GrenadeItems.registerEntities(registrar);
    }
}
