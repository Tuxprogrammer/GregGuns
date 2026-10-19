package com.gregguns.client.accessory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.armor.flight.ItemAntiGravityDevice;
import com.gregguns.core.registry.Naming;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Which worn model and texture belong to a GregGuns accessory stack. Items are matched by registry name, so the
 * Galacticraft and the fallback variants of the Oxygen Mask and Tanks (same names) share one look without this class
 * linking Galacticraft. Client only.
 */
@SideOnly(Side.CLIENT)
public final class AccessoryLooks {

    private AccessoryLooks() {}

    /** A model and the index of the texture the stack wears. */
    public static final class Look {

        private final AccessoryDef def;
        private final int texture;

        Look(AccessoryDef def, int texture) {
            this.def = def;
            this.texture = texture;
        }

        /** @return the model */
        public AccessoryDef def() {
            return def;
        }

        /** @return the texture index, see {@link AccessoryDef#textures()} */
        public int texture() {
            return texture;
        }
    }

    /**
     * @param stack any stack, or null
     * @return its worn look, or null when the stack is not a GregGuns accessory with a worn model
     */
    public static Look of(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        Item item = stack.getItem();
        Object name = Item.itemRegistry.getNameForObject(item);
        if (name == null) {
            return null;
        }
        String path = name.toString();
        String prefix = Naming.NAMESPACE + ":";
        if (!path.startsWith(prefix)) {
            return null;
        }
        AccessoryDef def = byItemName(path.substring(prefix.length()));
        if (def == null) {
            return null;
        }
        int texture = 0;
        if (item instanceof ItemAntiGravityDevice) {
            texture = ((ItemAntiGravityDevice) item).getCamo(stack);
        }
        return new Look(def, texture);
    }

    /**
     * @param itemName an item registry name without the mod id
     * @return the model for it, or null
     */
    static AccessoryDef byItemName(String itemName) {
        switch (itemName) {
            case "gas_mask":
                return AccessoryDefs.GAS_MASK;
            case "oxygen_mask":
                return AccessoryDefs.OXYGEN_MASK;
            case "tactical_mask":
                return AccessoryDefs.TACTICAL_MASK;
            case "night_vision_goggles":
                return AccessoryDefs.NIGHT_VISION_GOGGLES;
            case "oxygen_tanks":
                return AccessoryDefs.OXYGEN_TANKS;
            case "jump_pack":
                return AccessoryDefs.JUMP_PACK;
            case "anti_gravity_device":
                return AccessoryDefs.ANTI_GRAVITY_PACK;
            case "glider":
                return AccessoryDefs.GLIDER;
            default:
                return null;
        }
    }
}
