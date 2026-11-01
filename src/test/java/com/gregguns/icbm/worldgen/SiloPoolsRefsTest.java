package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.loot.LootEntry;

/**
 * Every item a silo room table names exists in the pack (D316): pack items by the pack's own id list, GregGuns items
 * by the lang file. GT parts go through the unificator and vanilla items through the registry; the in-game scenario
 * {@code icbm.complex.chest_loot} resolves every line of every table.
 */
class SiloPoolsRefsTest {

    private static final String PACK = "tools/questbook/pack_items.txt";
    private static final String LANG = "src/main/resources/assets/gregguns/lang/en_US.lang";

    /**
     * Ids the pack list does not hold, each checked another way:
     * <ul>
     * <li>{@code IC2:itemScrap}: IC2 2.2.828's registry name of Scrap (its lang key {@code itemScrap}; GT reads it as
     * {@code getIC2Item("scrap")}).</li>
     * <li>{@code BiblioCraft:item.BiblioGlasses} metas 0 to 2: as the pack's own Roguelike loot config names them
     * (config/roguelike_dungeons).</li>
     * <li>{@code GalacticraftMars:item.itemBasicAsteroids} metas 1 and 2 (Tier 2 engine and fins) and
     * {@code GalacticraftCore:item.buggymat} meta 1 (seat): {@code ItemBasicAsteroids.names} and
     * {@code ItemBuggyMaterial.names} at 3.4.34-GTNH.
     * Source:
     * https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/planets/asteroids/items/ItemBasicAsteroids.java#L22
     * Source:
     * https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/items/ItemBuggyMaterial.java#L19</li>
     * </ul>
     * GT metaitems ({@code gregtech:gt.metaitem.01} and {@code .03}) are checked against {@code IDMetaItem01} and
     * {@code IDMetaItem03} at 5.09.54.205 (see {@link SiloPools}).
     */
    private static final Set<String> CHECKED = new HashSet<>(
        Arrays.asList(
            "IC2:itemScrap",
            "BiblioCraft:item.BiblioGlasses",
            "BiblioCraft:item.BiblioGlasses#1",
            "BiblioCraft:item.BiblioGlasses#2",
            "GalacticraftMars:item.itemBasicAsteroids#1",
            "GalacticraftMars:item.itemBasicAsteroids#2",
            "GalacticraftCore:item.buggymat#1"));

    private static List<String> allRefs() {
        List<String> out = new ArrayList<>();
        for (String key : SiloLootTables.KEYS) {
            for (int c : new int[] { SiloCeiling.EV, SiloCeiling.UMV }) {
                for (LootEntry e : SiloLootTables.category(key, c)
                    .entries()) {
                    out.add(e.ref());
                    if (e.companion() != null) out.add(
                        e.companion()
                            .ref());
                }
            }
        }
        for (LootEntry e : SiloLootTables.junk()
            .entries()) {
            out.add(e.ref());
        }
        out.add(SiloPools.ROTTEN);
        out.add(SiloPools.BOWL);
        out.add(SiloPools.BOTTLE);
        out.add(SiloPools.SCRAP);
        out.add(SiloPools.DEAD_BATTERY);
        return out;
    }

    @Test
    void everyPackItemIsInThePack() throws IOException {
        Set<String> pack = new HashSet<>();
        for (String l : Files.readAllLines(Paths.get(PACK), StandardCharsets.UTF_8)) {
            if (l.isEmpty() || l.startsWith("#")) continue;
            pack.add(l);
            String[] p = l.split(":");
            if (p.length > 2) pack.add(p[0] + ":" + p[1]);
        }
        List<String> missing = new ArrayList<>();
        for (String ref : allRefs()) {
            if (ref.startsWith("gregguns:") || ref.startsWith("gt:") || ref.startsWith("minecraft:")) continue;
            if (CHECKED.contains(ref) || ref.startsWith("gregtech:gt.metaitem.0")) continue;
            String base = ref.contains("#") ? ref.substring(0, ref.indexOf('#')) : ref;
            String meta = ref.contains("#") ? ref.substring(ref.indexOf('#') + 1) : "0";
            if (!pack.contains("0".equals(meta) ? base : base + ":" + meta)) missing.add(ref);
        }
        assertTrue(missing.isEmpty(), "not in the pack: " + missing);
    }

    @Test
    void everyGregGunsItemHasAName() throws IOException {
        String lang = new String(Files.readAllBytes(Paths.get(LANG)), StandardCharsets.UTF_8);
        List<String> missing = new ArrayList<>();
        for (String ref : allRefs()) {
            if (!ref.startsWith("gregguns:")) continue;
            String name = ref.substring("gregguns:".length());
            name = name.replace("~full", "");
            if (name.contains("#")) name = name.substring(0, name.indexOf('#'));
            if (!lang.contains("item.gregguns." + name + ".name=") && !lang.contains("tile.gregguns." + name + ".name=")
                && !lang.contains("item.gregguns:" + name + ".name=")
                && !lang.contains("tile.gregguns:" + name + ".name=")) {
                missing.add(ref);
            }
        }
        assertTrue(missing.isEmpty(), "no GregGuns item named: " + missing);
    }
}
