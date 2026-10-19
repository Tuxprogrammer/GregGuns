package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.ammo.def.NetherChargeAmmo;
import com.gregguns.item.ammo.def.NukeRocketAmmo;

/** The LuV and ZPM ammo rows (RC-35, RC-36) against recipes.md. */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-35, RC-36, GGS-06-43)
class AmmoLineTableTest {

    private static ComponentTable.Spec spec(String id) {
        for (ComponentTable.Spec spec : AmmoLineTable.SPECS) {
            if (spec.row()
                .id()
                .equals(id)) {
                return spec;
            }
        }
        throw new AssertionError("no row " + id);
    }

    @Test
    void idsAndItemNamesMatchTheSheet() {
        assertEquals(AmmoIds.NETHER_CHARGE, NetherChargeAmmo.TYPE.roundItemName());
        assertEquals(AmmoIds.ROCKET_NUKE, NukeRocketAmmo.TYPE.roundItemName());
        assertEquals(Tier.LUV, NetherChargeAmmo.TYPE.tier());
        assertEquals(Tier.ZPM, NukeRocketAmmo.TYPE.tier());
        assertEquals(10, NetherChargeAmmo.SHOTS_PER_CHARGE);
        assertNotNull(AmmoTypes.ammo("nether_charge"));
        assertNotNull(AmmoTypes.ammo("rocket_nuke"));
        assertTrue(AmmoDefinitions.ALL.contains(NetherChargeAmmo.DEFINITION));
    }

    @Test
    void neitherAmmoHasAMagazineOrAnAmmoPressRow() {
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            assertTrue(
                !row.output()
                    .equals(AmmoIds.NETHER_CHARGE)
                    && !row.output()
                        .equals(AmmoIds.ROCKET_NUKE),
                row.id() + " must not be an Ammo Press row: the press stops at EV");
        }
        assertTrue(
            NetherChargeAmmo.DEFINITION.magazines()
                .isEmpty());
        assertTrue(
            NukeRocketAmmo.DEFINITION.magazines()
                .isEmpty());
        assertNull(AmmoTypes.magazine("nether_charge"));
    }

    @Test
    void nukeRocketRowMatchesTheSheet() {
        ComponentTable.Spec spec = spec("RC-35");
        ProductionTable.Row row = spec.row();
        assertEquals("ammo_rocket_nuke", row.output());
        assertEquals(1, row.outputCount());
        assertEquals(Tier.ZPM, row.tier());
        assertEquals(30, row.seconds());
        assertEquals(ProductionTable.Target.ASSEMBLY_LINE, row.target());
        assertEquals(
            5,
            row.inputs()
                .size());
        assertEquals(
            "ammo_rocket",
            row.inputs()
                .get(0)
                .itemName());
        assertEquals(
            "part_nuclear_warhead",
            row.inputs()
                .get(1)
                .itemName());
        assertEquals(
            "NaquadahAlloy",
            row.inputs()
                .get(2)
                .material());
        assertEquals(
            "Ultimate",
            row.inputs()
                .get(4)
                .material());
        assertEquals(
            "ammo_rocket",
            spec.research()
                .item()
                .itemName());
        assertEquals(
            288,
            spec.fluids()
                .get(0)
                .amount());
    }

    @Test
    void netherChargeRowMatchesTheSheet() {
        ComponentTable.Spec spec = spec("RC-36");
        ProductionTable.Row row = spec.row();
        assertEquals("ammo_nether_charge", row.output());
        assertEquals(16, row.outputCount());
        assertEquals(Tier.LUV, row.tier());
        assertEquals(20, row.seconds());
        // Source: GT's Materials.Polyethylene is "Plastic" and Materials.TNT has no dust (I12); these names are checked
        // against MaterialsInit at 5.09.54.205: Netherrack and Gunpowder have dusts (SoulSand does not resolve: vanilla
        // block), HSSG has plates.
        assertEquals(
            "Netherrack",
            row.inputs()
                .get(0)
                .material());
        assertEquals(
            16,
            row.inputs()
                .get(0)
                .count());
        assertEquals(
            "mc:soul_sand",
            row.inputs()
                .get(1)
                .special());
        assertEquals(
            "HSSG",
            row.inputs()
                .get(2)
                .material());
        assertEquals(
            "Master",
            row.inputs()
                .get(3)
                .material());
        assertEquals(
            "Gunpowder",
            row.inputs()
                .get(4)
                .material());
        assertEquals(
            "ammo_advanced_bullet",
            spec.research()
                .item()
                .itemName());
        assertEquals(
            ComponentTable.FluidKind.REGISTRY,
            spec.fluids()
                .get(0)
                .kind());
        assertEquals(
            "lava",
            spec.fluids()
                .get(0)
                .material());
        assertEquals(
            4000,
            spec.fluids()
                .get(0)
                .amount());
    }

    @Test
    void bothRowsMeetTheGateTheLineLimitsAndScanBelowTheirTier() {
        Map<String, ProductionTable.Row> all = new LinkedHashMap<>(ProductionTable.byOutput());
        for (ComponentTable.Spec spec : AmmoLineTable.SPECS) {
            ProductionTable.Row row = spec.row();
            assertTrue(GateRule.satisfied(row, all, ProductionTable.GATE_EXEMPT), row.id());
            assertTrue(
                row.inputs()
                    .size() >= 4
                    && row.inputs()
                        .size() <= 16,
                row.id());
            assertNotNull(spec.research(), row.id());
            assertTrue(
                spec.research()
                    .scanTier()
                    .ordinal()
                    < row.tier()
                        .ordinal(),
                row.id() + ": research is scanned at a lower tier");
            assertTrue(
                !spec.fluids()
                    .isEmpty(),
                row.id());
        }
    }
}
