package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.ClientGunDefinitions;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.part.PartDefinition;
import com.gregguns.item.part.PartDefinitions;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.GunTable;
import com.gregguns.recipe.ProductionTable;

/**
 * Table-driven checks over {@link GunDefinitions#ALL}: whatever a new gun adds is held to the same rules, with no test
 * edit. Covers the definition against the ammo it names, its recipe row, its sounds, its camo skins (against the
 * texture files on disk and the client definition) and the client list.
 */
// Spec: docs/gregguns-spec/02-guns/index.md; docs/gregguns-spec/09-machines-utility/index.md (Camo Bench)
class GunDefinitionsTest {

    private static final String ROOT = "/assets/gregguns/";

    private static boolean exists(String resource) {
        return GunDefinitionsTest.class.getResource(resource) != null;
    }

    private static JsonObject sounds() throws IOException {
        try (Reader r = new InputStreamReader(
            GunDefinitionsTest.class.getResourceAsStream(ROOT + "sounds.json"),
            StandardCharsets.UTF_8)) {
            return new JsonParser().parse(r)
                .getAsJsonObject();
        }
    }

    private static Properties lang() throws IOException {
        Properties lang = new Properties();
        try (Reader r = new InputStreamReader(
            GunDefinitionsTest.class.getResourceAsStream(ROOT + "lang/en_US.lang"),
            StandardCharsets.UTF_8)) {
            lang.load(r);
        }
        return lang;
    }

    @Test
    void namesAreUniqueAndLookupFindsEachDefinition() {
        Set<String> seen = new HashSet<>();
        for (GunDefinition def : GunDefinitions.ALL) {
            assertTrue(seen.add(def.name()), "duplicate gun " + def.name());
            assertEquals(def, GunDefinitions.byName(def.name()));
            assertTrue(
                def.name()
                    .matches("[a-z][a-z0-9_]*"),
                def.name());
        }
        assertNull(GunDefinitions.byName("no_such_gun"));
    }

    @Test
    void theFirstFiveGunsKeepTheirRegistryNamesTiersAndDamageTypes() {
        assertEquals(
            "pistol_9mm",
            GunDefinitions.ALL.get(0)
                .name());
        assertEquals(
            "revolver",
            GunDefinitions.ALL.get(1)
                .name());
        assertEquals(
            "sawed_off",
            GunDefinitions.ALL.get(2)
                .name());
        assertEquals(
            "handcannon",
            GunDefinitions.ALL.get(3)
                .name());
        assertEquals(
            "bolt_action_rifle",
            GunDefinitions.ALL.get(4)
                .name());
        assertEquals(
            com.gregguns.api.Tier.STEAM,
            GunDefinitions.byName("handcannon")
                .tier());
        assertEquals(
            com.gregguns.api.Tier.LV,
            GunDefinitions.byName("bolt_action_rifle")
                .tier());
        assertTrue(
            GunDefinitions.byName("bolt_action_rifle")
                .rechambers());
        assertFalse(
            GunDefinitions.byName("pistol_9mm")
                .rechambers());
    }

    @Test
    void everyGunNamesAnAmmoItemAndItsMagazineItems() {
        Set<String> items = new HashSet<>();
        for (AmmoDefinition ammo : AmmoDefinitions.ALL) {
            items.add(
                ammo.type()
                    .roundItemName());
            ammo.magazines()
                .forEach(m -> {
                    items.add(m.loadedItemName());
                    items.add(m.emptyItemName());
                });
        }
        // A fluid tank is a Forge fluid container item, not an ammo-package item: it is ammo while full, and the same
        // item
        // empty is what a reload hands back.
        items.add(com.gregguns.gun.logic.GunCatalog.AmmoIds.FUEL_TANK);
        items.add(com.gregguns.gun.logic.GunCatalog.AmmoIds.AIR_TANK);
        for (GunDefinition def : GunDefinitions.ALL) {
            GunStats stats = def.stats();
            if (stats.isEnergy()) {
                continue; // the gun is its own battery: its ammo type is a marker, no item exists
            }
            assertTrue(items.contains(stats.ammoType()), def.name() + " ammoType " + stats.ammoType());
            assertTrue(items.contains(stats.looseAmmoType()), def.name() + " looseAmmoType " + stats.looseAmmoType());
            if (stats.emptyContainerType() != null) {
                assertTrue(items.contains(stats.emptyContainerType()), def.name() + " emptyContainerType");
            }
        }
    }

    @Test
    void everyAssemblyRowMakesItsGunAndMeetsTheGateRule() {
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        Set<String> circuits = new HashSet<>();
        for (GunDefinition def : GunDefinitions.ALL) {
            for (ProductionTable.Row row : def.rows()) {
                assertEquals(def.name(), row.output());
                assertEquals(ProductionTable.Target.ASSEMBLER, row.target());
                assertTrue(GateRule.satisfied(row, all, ProductionTable.GATE_EXEMPT), row.id());
                assertTrue(
                    circuits.add(row.tier() + "/" + row.circuit()),
                    row.id() + " reuses circuit " + row.circuit() + " at " + row.tier());
                for (ProductionTable.Input input : row.inputs()) {
                    if (input.isGregGuns()) {
                        assertTrue(
                            all.containsKey(input.itemName())
                                || ProductionTable.CRAFTED_PARTS.contains(input.itemName())
                                || GunDefinitions.byName(input.itemName()) != null // an upgrade consumes a gun
                                || com.gregguns.recipe.ComponentTable.byOutput()
                                    .containsKey(input.itemName()),
                            row.id() + " input " + input.itemName() + " has no recipe");
                    }
                }
            }
        }
        assertEquals(
            GunDefinitions.ALL.stream()
                .mapToInt(
                    d -> d.rows()
                        .size())
                .sum(),
            GunTable.ROWS.size());
    }

    @Test
    void everyGunOwnsItsFireAndReloadSoundsAndRechamberWhenItHasOne() throws IOException {
        JsonObject events = sounds();
        for (GunDefinition def : GunDefinitions.ALL) {
            assertEquals("guns." + def.textureName() + ".fire", def.fireEvent()); // an akimbo gun shares its single
                                                                                  // gun's
            assertEquals("guns." + def.name() + ".reload", def.reloadEvent());
            assertTrue(events.has(def.fireEvent()), def.fireEvent());
            assertTrue(events.has(def.reloadEvent()), def.reloadEvent());
            if (def.startsLoop()) {
                assertEquals("guns." + def.name() + ".start", def.startEvent());
                assertTrue(events.has(def.startEvent()), def.startEvent());
            } else {
                assertNull(def.startEvent());
            }
            if (def.rechambers()) {
                assertEquals("guns." + def.name() + ".rechamber", def.rechamberEvent());
                assertTrue(events.has(def.rechamberEvent()), def.rechamberEvent());
            } else {
                assertNull(def.rechamberEvent());
            }
        }
    }

    @Test
    void camoCountMatchesTheTextureFilesOnDisk() {
        for (GunDefinition def : GunDefinitions.ALL) {
            String base = ROOT + "textures/models/guns/" + def.textureName();
            assertTrue(exists(base + ".png"), "missing base texture of " + def.textureName());
            int onDisk = 1;
            while (exists(base + "_camo" + onDisk + ".png")) {
                onDisk++;
            }
            assertEquals(
                onDisk,
                def.camoCount(),
                def.name() + ": camoCount must equal the base texture plus the camoN files");
        }
    }

    @Test
    void everyCamoBeyondTheDefaultHasAnEnglishName() throws IOException {
        Properties lang = lang();
        for (GunDefinition def : GunDefinitions.ALL) {
            if (def.camoCount() > 1) {
                for (int i = 0; i < def.camoCount(); i++) {
                    String key = "item.gregguns." + def.name() + ".camoname." + i;
                    assertNotNull(lang.getProperty(key), key);
                }
            }
        }
    }

    @Test
    void theClientListPlusTheFlatIconFallbackIsTheCommonListAndTheProfilesTexturesAreTheCamos() {
        assertEquals(
            GunDefinitions.ALL.size(),
            ClientGunDefinitions.ALL.size() + ClientGunDefinitions.FLAT_ICON_FALLBACK.size());
        for (GunDefinition def : GunDefinitions.ALL) {
            ClientGunDefinition client = ClientGunDefinitions.byName(def.name());
            if (client == null) {
                assertTrue(
                    ClientGunDefinitions.FLAT_ICON_FALLBACK.contains(def.name()),
                    "no client definition for " + def.name() + " and it is not on the flat-icon fallback list");
                continue;
            }
            assertFalse(
                ClientGunDefinitions.FLAT_ICON_FALLBACK.contains(def.name()),
                def.name() + " has a profile, so it must leave the fallback list");
            String[] textures = client.textures();
            assertEquals(def.camoCount(), textures.length, def.name() + " client textures vs camo count");
            for (int i = 0; i < textures.length; i++) {
                String expected = "gregguns:textures/models/guns/" + def.textureName()
                    + (i == 0 ? "" : "_camo" + i)
                    + ".png";
                assertEquals(expected, textures[i]);
                assertTrue(exists("/assets/" + textures[i].replace(':', '/')), textures[i]);
            }
        }
    }

    /**
     * The M2 exit gate. While the four B3 guns await their clean-room models this lists them; the M2 close changes the
     * expectation to an empty list. Until then it documents exactly which guns draw as a flat icon.
     */
    @Test
    void theFlatIconFallbackListsExactlyTheGunsStillWithoutAModel() {
        java.util.List<String> without = new java.util.ArrayList<>();
        for (GunDefinition def : GunDefinitions.ALL) {
            if (ClientGunDefinitions.byName(def.name()) == null) {
                without.add(def.name());
            }
        }
        assertEquals(ClientGunDefinitions.FLAT_ICON_FALLBACK, without);
        assertEquals(java.util.Arrays.asList(), without);
        assertFalse(ClientGunDefinitions.usesFallback("aug"));
        assertFalse(ClientGunDefinitions.usesFallback("scar"));
        assertFalse(ClientGunDefinitions.usesFallback("vector"));
        assertFalse(ClientGunDefinitions.usesFallback("infiltrator"));
        assertFalse(ClientGunDefinitions.usesFallback("m4"));
        assertFalse(ClientGunDefinitions.usesFallback("pistol_9mm"));
    }

    @Test
    void partsAndAmmoDefinitionsAreConsistentWithTheirIndexes() {
        PartDefinitions.ALL.forEach(part -> {
            assertEquals(part, PartDefinitions.byName(part.name()));
            part.rows()
                .forEach(row -> assertEquals(part.name(), row.output()));
        });
        for (AmmoDefinition ammo : AmmoDefinitions.ALL) {
            assertEquals(
                ammo.type(),
                AmmoTypes.ammo(
                    ammo.type()
                        .id()));
            ammo.magazines()
                .forEach(m -> assertEquals(m, AmmoTypes.magazine(m.id())));
            for (ProductionTable.Row row : ammo.rows()) {
                assertTrue(ProductionTable.ROWS.contains(row), row.id() + " is collected into ProductionTable.ROWS");
                assertEquals(ProductionTable.Target.AMMO_PRESS, row.target());
            }
        }
        for (PartDefinition part : PartDefinitions.ALL) {
            part.rows()
                .forEach(row -> assertTrue(ProductionTable.ROWS.contains(row), row.id()));
        }
    }
}
