package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.item.ammo.def.NetherChargeAmmo;
import com.gregguns.recipe.ComponentTable.Spec;
import com.gregguns.recipe.GunLineTable;
import com.gregguns.recipe.ProductionTable.Target;

/** The Nether Blaster against the Techguns sheet and the recipe sheet. */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Nether Blaster); docs/gregguns-spec/02-guns/recipes.md
class NetherBlasterDefinitionTest {

    private static final GunStats S = NetherBlasterDef.STATS;

    @Test
    void carriesTheSheetNumbers() {
        assertEquals(14.0, S.damage());
        assertEquals(8.0, S.damageMin());
        assertEquals(15.0, S.dropStart());
        assertEquals(30.0, S.dropEnd());
        assertEquals(0.0, S.spread());
        assertEquals(0.15, S.penetration());
        assertEquals(FireMode.AUTO, S.fireMode());
        assertEquals(8, S.fireRateTicks());
        assertEquals(35, S.reloadTicks());
        assertEquals(0.85, S.projectileSpeed());
        assertEquals(50, S.lifetimeTicks());
        assertEquals(0.0, S.projectileGravity());
        assertEquals(ProjectileKind.NETHER, S.projectileKind());
        assertTrue(!S.hasScope(), "no zoom");
        assertEquals(Tier.LUV, NetherBlasterDef.DEFINITION.tier());
    }

    @Test
    void oneChargeIsExactlyTheChargesOwnShotCount() {
        // The figure lives on NetherChargeAmmo only; the clip and the per-item rounds are read from it.
        assertEquals(NetherChargeAmmo.SHOTS_PER_CHARGE, S.magazineSize());
        assertEquals(1, S.ammoItemsPerReload());
        assertEquals(NetherChargeAmmo.SHOTS_PER_CHARGE, S.roundsPerItem());
        assertEquals(AmmoIds.NETHER_CHARGE, S.ammoType());
        assertEquals(ReloadMode.MAGAZINE, S.reloadMode());
        assertEquals(NetherChargeAmmo.TYPE.roundItemName(), S.ammoType());
        assertNull(S.emptyContainerType(), "no empty item");
    }

    @Test
    void theGunIsMadeUnloadedOnTheAssemblyLine() {
        assertTrue(
            NetherBlasterDef.DEFINITION.rows()
                .isEmpty(),
            "no Assembler row: the LuV recipe is an Assembly Line spec");
        Spec spec = GunLineTable.byOutput(NetherBlasterDef.NAME);
        assertEquals(
            Target.ASSEMBLY_LINE,
            spec.row()
                .target());
        assertEquals(
            NetherBlasterDef.NAME,
            spec.row()
                .output());
        assertEquals(
            Tier.LUV,
            spec.row()
                .tier());
        assertEquals(
            45,
            spec.row()
                .seconds());
        assertEquals(
            2,
            spec.fluids()
                .size());
        assertEquals(
            576,
            spec.fluids()
                .get(0)
                .amount());
        assertEquals(
            250,
            spec.fluids()
                .get(1)
                .amount());
        assertEquals(
            Tier.IV,
            spec.research()
                .scanTier());
        assertEquals(
            60,
            spec.research()
                .scanSeconds());
        for (com.gregguns.recipe.ProductionTable.Input input : spec.row()
            .inputs()) {
            assertTrue(
                !input.isGregGuns() || !input.itemName()
                    .startsWith("ammo_"),
                "no weapon recipe takes ammo as an input (GGS-02-36)");
        }
    }
}
