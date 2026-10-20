package com.gregguns.item.ammo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.ammo.AmmoCompatibility.Use;
import com.gregguns.item.ammo.def.AdvancedAmmo;
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.ammo.def.NetherChargeAmmo;

/**
 * Every number a magazine, round or tank tooltip shows must equal the gun's real stats (owner report: the Advanced
 * Magazine said 3 where the PDW holds 20 and the Pulse Rifle 12).
 */
// Spec: docs/gregguns-spec/06-items/ammo.md (tooltip compatibility notes)
class AmmoTooltipNumbersTest {

    private static final AmmoCompatibility C = AmmoCompatibility.DEFAULT;

    private static GunDefinition gun(String name) {
        for (GunDefinition g : GunDefinitions.ALL) {
            if (g.name()
                .equals(name)) {
                return g;
            }
        }
        throw new AssertionError("no gun " + name);
    }

    private static int clipPerItem(GunDefinition g) {
        // Independent of roundsPerItem(): the gun's clip over the items one reload consumes.
        return g.stats()
            .magazineSize()
            / g.stats()
                .ammoItemsPerReload();
    }

    @Test
    void everyMagazineAndGunPairShowsTheGunsRealCapacity() {
        int pairs = 0;
        for (MagazineType mag : AmmoTypes.allMagazines()
            .values()) {
            for (Use use : C.roundsPerGun(mag.loadedItemName())) {
                assertEquals(clipPerItem(gun(use.name())), use.count(), mag.id() + " / " + use.name());
                pairs++;
            }
        }
        assertTrue(
            pairs >= AmmoTypes.allMagazines()
                .size());
    }

    @Test
    void advancedMagazineIsTwentyInThePdwAndTwelveInThePulseRifle() {
        assertEquals(20, count(AdvancedAmmo.MAGAZINE.loadedItemName(), "pdw"));
        assertEquals(12, count(AdvancedAmmo.MAGAZINE.loadedItemName(), "pulse_rifle"));
    }

    @Test
    void everyDirectItemShowsTheGunsRealCapacity() {
        for (GunDefinition g : GunDefinitions.ALL) {
            if (g.stats()
                .isEnergy()) {
                continue;
            }
            String item = g.stats()
                .ammoType();
            assertEquals(clipPerItem(g), count(item, g.name()), g.name() + " / " + item);
        }
    }

    @Test
    void netherChargeGivesTenShots() {
        assertEquals(NetherChargeAmmo.SHOTS_PER_CHARGE, count("ammo_nether_charge", "nether_blaster"));
    }

    @Test
    void roundTooltipShowsTheLooseItemsPerMagazine() {
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            List<Use> costs = C.magazineCosts(
                def.type()
                    .roundItemName());
            assertEquals(
                def.magazines()
                    .size(),
                costs.size());
            for (int i = 0; i < costs.size(); i++) {
                assertEquals(
                    def.magazines()
                        .get(i)
                        .roundsPerMagazine(),
                    costs.get(i)
                        .count());
            }
        }
    }

    @Test
    void renderedEntryCarriesTheNumberFromTheLangTemplates() throws IOException {
        Properties lang = new Properties();
        lang.load(
            Files.newBufferedReader(
                Paths.get("src/main/resources/assets/gregguns/lang/en_US.lang"),
                StandardCharsets.UTF_8));
        String one = lang.getProperty("gregguns.tooltip.ammo.rounds_one");
        String many = lang.getProperty("gregguns.tooltip.ammo.rounds_many");
        assertEquals("PDW: 20 rounds", AmmoTooltipFormat.roundsEntry("PDW", 20, one, many));
        assertEquals("Shell: 1 round", AmmoTooltipFormat.roundsEntry("Shell", 1, one, many));
        assertFalse(
            lang.containsKey("gregguns.tooltip.magazine.loaded"),
            "the single misleading 'holds N' line must stay gone");
    }

    @Test
    void blockLaysOutOneIndentedLinePerEntry() {
        List<String> lines = AmmoTooltipFormat.block("Used by:", java.util.Arrays.asList("A", "B"), false, "more");
        assertEquals(java.util.Arrays.asList("Used by:", "  A", "  B"), lines);
        assertEquals(
            Collections.emptyList(),
            AmmoTooltipFormat.block("x", Collections.<String>emptyList(), false, "m"));
    }

    private static int count(String item, String gunName) {
        for (Use use : C.roundsPerGun(item)) {
            if (use.name()
                .equals(gunName)) {
                return use.count();
            }
        }
        throw new AssertionError(item + " lists no " + gunName);
    }
}
