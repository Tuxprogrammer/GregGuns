package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

/** The catalog against the spec sheet, the lang file and the asset folders. */
class ArmorCatalogTest {

    private static final String ASSETS = "/assets/gregguns/";

    private static boolean exists(String path) {
        return ArmorCatalogTest.class.getResource(ASSETS + path) != null;
    }

    private static Properties lang() throws IOException {
        Properties p = new Properties();
        try (InputStream in = ArmorCatalogTest.class.getResourceAsStream(ASSETS + "lang/en_US.lang")) {
            assertNotNull(in);
            p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void theThreeT1SetsHaveFourPiecesAndTheRightTiers() {
        assertEquals(12, ArmorCatalog.ALL.size());
        for (SetDef set : ArmorCatalog.ALL) {
            assertEquals(set == ArmorCatalog.T2_BERET ? 1 : 4, set.pieceCount(), set.id());
        }
        assertEquals(Tier.STEAM, ArmorCatalog.T1_SCOUT.tier());
        assertEquals(Tier.STEAM, ArmorCatalog.T1_MINER.tier());
        assertEquals(Tier.LV, ArmorCatalog.T1_COMBAT.tier());
    }

    @Test
    void pieceNamesAreUniqueAndFollowTheSpecRegistryNames() {
        Set<String> seen = new HashSet<>();
        for (SetDef set : ArmorCatalog.ALL) {
            for (ArmorSlot slot : set.pieces()
                .keySet()) {
                assertTrue(seen.add(set.pieceName(slot)), set.pieceName(slot));
            }
        }
        assertEquals("t1_combat_helmet", ArmorCatalog.T1_COMBAT.pieceName(ArmorSlot.HELMET));
        assertEquals("t1_scout_chestplate", ArmorCatalog.T1_SCOUT.pieceName(ArmorSlot.CHESTPLATE));
        assertEquals("t1_miner_leggings", ArmorCatalog.T1_MINER.pieceName(ArmorSlot.LEGGINGS));
        assertEquals("t1_miner_boots", ArmorCatalog.T1_MINER.pieceName(ArmorSlot.BOOTS));
    }

    @Test
    void everyPieceHasAnIconAndALangKeyWithTheCatalogName() throws IOException {
        Properties lang = lang();
        for (SetDef set : ArmorCatalog.ALL) {
            for (PieceDef p : set.pieces()
                .values()) {
                String name = set.pieceName(p.slot());
                assertTrue(exists("textures/items/" + name + ".png"), "icon " + name);
                assertEquals(p.englishName(), lang.getProperty("item.gregguns." + name + ".name"), name);
            }
        }
        assertEquals("Heavy Cloth", lang.getProperty("item.gregguns.part_heavy_cloth.name"));
        assertTrue(exists("textures/items/part_heavy_cloth.png"));
    }

    @Test
    void everyCamoHasBothLayerTexturesAndAName() throws IOException {
        Properties lang = lang();
        for (SetDef set : ArmorCatalog.ALL) {
            for (ArmorSlot slot : ArmorSlot.values()) {
                for (int camo = 0; camo < set.camoCount(); camo++) {
                    String path = set.layerTexture(slot, camo)
                        .replace("gregguns:", "");
                    assertTrue(exists(path), path);
                }
            }
            if (set.camoCount() > 1) {
                for (int camo = 0; camo < set.camoCount(); camo++) {
                    assertNotNull(lang.getProperty("item.gregguns." + set.id() + ".camoname." + camo), set.id());
                }
            }
        }
    }

    @Test
    void leggingsUseTheSecondLayerAndOthersTheFirst() {
        assertTrue(
            ArmorCatalog.T1_COMBAT.layerTexture(ArmorSlot.LEGGINGS, 0)
                .endsWith("t1_combat_2.png"));
        assertTrue(
            ArmorCatalog.T1_COMBAT.layerTexture(ArmorSlot.HELMET, 0)
                .endsWith("t1_combat_1.png"));
        assertTrue(
            ArmorCatalog.T1_SCOUT.layerTexture(ArmorSlot.BOOTS, 2)
                .endsWith("t1_scout_snow_1.png"));
    }

    @Test
    void doesNotReproduceARM12_anOutOfRangeCamoFallsBackToTheFirst() {
        assertEquals(
            ArmorCatalog.T1_SCOUT.layerTexture(ArmorSlot.HELMET, 0),
            ArmorCatalog.T1_SCOUT.layerTexture(ArmorSlot.HELMET, 99));
        assertEquals(
            ArmorCatalog.T1_SCOUT.layerTexture(ArmorSlot.HELMET, 0),
            ArmorCatalog.T1_SCOUT.layerTexture(ArmorSlot.HELMET, -1));
        assertEquals(0, ArmorCatalog.T1_COMBAT.clampCamo(3));
    }

    @Test
    void t1SetsGiveNoGtHazmatAndNoSetClaimsSpace() {
        // hazard-protection.md flag table: T1 sets have no flags. GGS-05-10 and GGS-05-12.
        for (SetDef set : new SetDef[] { ArmorCatalog.T1_SCOUT, ArmorCatalog.T1_MINER, ArmorCatalog.T1_COMBAT }) {
            for (PieceDef p : set.pieces()
                .values()) {
                assertTrue(
                    p.partialHazards()
                        .isEmpty(),
                    set.id());
                assertTrue(
                    p.fullHazards()
                        .isEmpty(),
                    set.id());
            }
        }
        for (HazardFlag f : HazardFlag.values()) {
            assertFalse(
                f.name()
                    .equals("SPACE"));
        }
    }

    @Test
    void t1SetsHaveNoFullSetBonusAsInTechguns() {
        for (SetDef set : ArmorCatalog.ALL) {
            assertTrue(
                set.fullSetBonus()
                    .isEmpty(),
                set.id());
        }
    }

    @Test
    void miningAndSpeedBonusesAreTheSpecNumbers() {
        PieceDef scoutBoots = ArmorCatalog.T1_SCOUT.pieces()
            .get(ArmorSlot.BOOTS);
        assertEquals(
            0.125,
            scoutBoots.bonuses()
                .get(ArmorBonus.SPEED),
            1e-12);
        assertEquals(
            0.10,
            scoutBoots.bonuses()
                .get(ArmorBonus.JUMP),
            1e-12);
        PieceDef minerHelmet = ArmorCatalog.T1_MINER.pieces()
            .get(ArmorSlot.HELMET);
        assertEquals(
            0.08,
            minerHelmet.bonuses()
                .get(ArmorBonus.SPEED),
            1e-12);
        assertEquals(
            0.05,
            minerHelmet.bonuses()
                .get(ArmorBonus.BREAK_SPEED),
            1e-12);
        assertEquals(
            0.20,
            ArmorCatalog.T1_COMBAT.pieces()
                .get(ArmorSlot.CHESTPLATE)
                .bonuses()
                .get(ArmorBonus.KNOCKBACK_RESISTANCE),
            1e-12);
    }

    @Test
    void everyBonusHasATooltipLabel() throws IOException {
        Properties lang = lang();
        for (ArmorBonus b : ArmorBonus.values()) {
            assertNotNull(lang.getProperty(b.langKey()), b.langKey());
        }
        for (String key : new String[] { "gregguns.armor.tooltip.broken", "gregguns.armor.tooltip.protection",
            "gregguns.armor.tooltip.bonus", "gregguns.armor.tooltip.camo" }) {
            assertNotNull(lang.getProperty(key), key);
        }
    }
}
