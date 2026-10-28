package com.gregguns.icbm.worldgen;

import net.minecraft.item.ItemStack;

import com.gregguns.icbm.complex.MachineRole;

import gregtech.api.enums.ItemList;

/**
 * Finds the GT machine item for a {@link MachineRole} at a voltage tier (D129). Resolves {@link ItemList} entries by
 * name so a name GT does not have is simply skipped; from the asked tier it steps down until something resolves, and
 * never goes up, so the ceiling holds. A dead machine, and a role with no matching GT machine, is the tier's hull.
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Machines)
public final class MachinePicker {

    /** GT's short voltage names by voltage index. */
    private static final String[] VN = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV",
        "UMV" };

    private MachinePicker() {}

    /** @return the {@link ItemList} name patterns for a role, tried in order; {T} is the voltage name */
    static String[] patterns(MachineRole role) {
        switch (role) {
            case GENERATOR:
                return new String[] { "Generator_Gas_Turbine_{T}", "Generator_Steam_Turbine_{T}",
                    "Generator_Diesel_{T}", "Generator_Plasma_{P}", "Hull_{T}" };
            case BATTERY_BUFFER:
                return new String[] { "Battery_Buffer_4by4_{T}", "Battery_Buffer_3by3_{T}", "Battery_Buffer_2by2_{T}",
                    "Hull_{T}" };
            case WORKBENCH:
                return new String[] { "Machine_{T}_Assembler", "Hull_{T}" };
            case PUMP:
                return new String[] { "Pump_{T}", "Hull_{T}" };
            default:
                return new String[] { "Hull_{T}" };
        }
    }

    /**
     * @param role the role
     * @param tier the voltage index asked for (1 and up)
     * @param dead true for a husk (the hull)
     * @return the stack and the voltage index it really is at, or null when nothing resolves
     */
    public static Pick pick(MachineRole role, int tier, boolean dead) {
        String[] patterns = dead ? new String[] { "Hull_{T}" } : patterns(role);
        // Real machines first, at the highest tier that has one (never above the ask); the hull only when no
        // machine of the role exists at or below the tier.
        for (int pass = 0; pass < 2; pass++) {
            boolean hullPass = pass == 1;
            for (int t = Math.min(tier, VN.length - 1); t >= 1; t--) {
                for (String pattern : patterns) {
                    if (pattern.startsWith("Hull_") != hullPass) {
                        continue;
                    }
                    ItemList item = lookup(
                        pattern.replace("{T}", VN[t])
                            .replace("{P}", t == 7 ? "ZPMV" : VN[t]));
                    if (item != null && item.hasBeenSet()) {
                        ItemStack s = item.get(1);
                        if (s != null) {
                            return new Pick(s, t, hullPass);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static ItemList lookup(String name) {
        try {
            // GT names the UHV hull Hull_MAX (the old top tier).
            return ItemList.valueOf("Hull_UHV".equals(name) ? "Hull_MAX" : name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** A resolved machine item. */
    public static final class Pick {

        private final ItemStack stack;
        private final int tier;
        private final boolean hull;

        Pick(ItemStack stack, int tier, boolean hull) {
            this.stack = stack;
            this.tier = tier;
            this.hull = hull;
        }

        /** @return one machine item; its damage is the MTE id */
        public ItemStack stack() {
            return stack;
        }

        /** @return the real voltage index, at most the tier asked for */
        public int tier() {
            return tier;
        }

        /** @return true when it fell back to a hull */
        public boolean hull() {
            return hull;
        }
    }
}
