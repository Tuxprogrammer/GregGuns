package com.gregguns.icbm.complex.palette;

import static com.gregguns.icbm.complex.palette.BlockRef.e;
import static com.gregguns.icbm.complex.palette.BlockRef.of;
import static com.gregguns.icbm.complex.palette.BlockRef.v;
import static com.gregguns.icbm.complex.palette.Patterns.bands;
import static com.gregguns.icbm.complex.palette.Patterns.borderField;
import static com.gregguns.icbm.complex.palette.Patterns.centerStrip;
import static com.gregguns.icbm.complex.palette.Patterns.checker;
import static com.gregguns.icbm.complex.palette.Patterns.doubleRing;
import static com.gregguns.icbm.complex.palette.Patterns.grid;
import static com.gregguns.icbm.complex.palette.Patterns.plain;
import static com.gregguns.icbm.complex.palette.Patterns.stripes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The style sets and the block chains they are built from (D240, D241). Every chain is pack block, then GregTech, then
 * vanilla; the exact blocks, metas and their mining worth are in palette.md.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Style sets, Block table); D240; D241; D243
// Source: Chisel registry ids and metas, from Features.java at 2.17.34-GTNH
// https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java
// Source: GT concrete, stone and granite metas, from the lang file at 5.09.54.205
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/resources/assets/gregtech/lang/en_US.lang
// Decompiled: Ztones-1.7.10-2.2.2.jar (GTNH 2.9.0-RC-2) has no public source. Each metal set block takes
// setBlockName("<set>Block") and registers under its unlocalized name, so the registry id is
// "Ztones:tile.<set>Block" (javap of ModBlocks and BlockAgon; the live pack's FML registry in level.dat lists
// Ztones:tile.agonBlock and the rest). Every set has 16 metas, textures sets/<set>/<set>_ (0..15).png; the metas
// used here were checked against those textures. Unverified beyond that; re-check when the pack updates Ztones.
final class PaletteSets {

    private PaletteSets() {}

    private static final String CH = "chisel";
    private static final String GT = "gregtech";
    private static final String ZT = "Ztones";

    // ---- chain helpers: pack block, GT block, vanilla block ----

    private static BlockRef chisel(String name, int meta, BlockRef.Entry gt, BlockRef.Entry vanilla) {
        return of(e(CH, name, meta), gt, vanilla);
    }

    private static BlockRef.Entry gtConcrete(int meta) {
        return e(GT, "gt.blockconcretes", meta);
    }

    private static BlockRef.Entry gtStone(int meta) {
        return e(GT, "gt.blockstones", meta);
    }

    private static BlockRef.Entry gtGranite(int meta) {
        return e(GT, "gt.blockgranites", meta);
    }

    private static BlockRef lab(int meta, int gtConcreteMeta) {
        return chisel("laboratoryblock", meta, gtConcrete(gtConcreteMeta), v("quartz_block", 0));
    }

    private static BlockRef factory(int meta) {
        return chisel("factoryblock", meta, gtConcrete(3), v("stonebrick", 0));
    }

    private static BlockRef tyrian(int meta) {
        return chisel("tyrian", meta, gtConcrete(7), v("stonebrick", 0));
    }

    private static BlockRef concrete(int meta, int gtMeta) {
        return chisel("concrete", meta, gtConcrete(gtMeta), v("stonebrick", 0));
    }

    private static BlockRef technical(int meta) {
        return chisel("technical", meta, gtStone(11), v("stonebrick", 2));
    }

    private static BlockRef marble(int meta) {
        return chisel("marble", meta, gtStone(3), v("quartz_block", 0));
    }

    private static BlockRef limestone(int meta) {
        return chisel("limestone", meta, gtConcrete(11), v("sandstone", 2));
    }

    private static BlockRef granite(int meta) {
        return of(gtGranite(meta), v("stonebrick", 3));
    }

    /** @return the registry name of a Ztones metal set block: {@code tile.<set>Block} */
    static String ztonesName(String set) {
        return "tile." + set + "Block";
    }

    /** A Ztones block, then a Chisel block, then GT, then vanilla. */
    private static BlockRef ztones(String set, int meta, BlockRef fallback) {
        List<BlockRef.Entry> l = new ArrayList<>();
        l.add(e(ZT, ztonesName(set), meta));
        l.addAll(fallback.chain());
        return of(l.toArray(new BlockRef.Entry[0]));
    }

    /** A lit full-cube lamp: Chisel's light stone (light 15), else vanilla glowstone (D242). */
    private static BlockRef litLamp(int meta) {
        return of(e(CH, "glowstone", meta), v("glowstone", 0));
    }

    private static final BlockRef WOOD_TABLE = of(v("wooden_slab", 1));
    private static final BlockRef STONE_TABLE = of(v("stone_slab", 7));
    /**
     * Chisel's "abandoned" bookshelf. Chisel's own bookshelf block has variations at metas 1 to 8 only (meta 0 of the
     * group is the vanilla bookshelf), so meta 0 of {@code chisel:bookshelf} would draw the missing texture.
     */
    private static final BlockRef SHELF = of(e(CH, "bookshelf", 5), v("bookshelf", 0));
    /** Bunk blankets: light gray wool (a muted, military palette; no saturated wool). */
    private static final BlockRef BUNK = of(v("wool", 8));
    private static final BlockRef BUNK_GREY = of(v("wool", 7));
    private static final BlockRef SCREEN_DARK = of(e(CH, "laboratoryblock", 10), v("stained_hardened_clay", 15));

    // ---- planet stone (D244) ----

    /** Moon Rock (meta 4) and Moon Dungeon Brick (meta 14, dark gray). */
    static final BlockRef.Entry MOON_STONE = e("GalacticraftCore", "tile.moonBlock", 4);
    static final BlockRef.Entry MOON_BRICK = e("GalacticraftCore", "tile.moonBlock", 14);
    /** Mars Stone (meta 9), Mars Cobblestone (meta 4) and Mars Dungeon Brick (meta 7). */
    static final BlockRef.Entry MARS_STONE = e("GalacticraftMars", "tile.mars", 9);
    static final BlockRef.Entry MARS_COBBLE = e("GalacticraftMars", "tile.mars", 4);
    /**
     * The Mars "brick": Mars Cobblestone (meta 4). Galacticraft's Mars Dungeon Brick (meta 7) is green and mossy, too
     * loud beside the red Mars stone, so the planet's walls and checkers stay in its own dark reds.
     */
    static final BlockRef.Entry MARS_BRICK = MARS_COBBLE;

    // ---- the sets ----

    static List<StyleSet> build() {
        List<StyleSet> l = new ArrayList<>();

        BlockRef lab0 = lab(0, 15), lab1 = lab(1, 15), lab2 = lab(2, 14), lab5 = lab(5, 15), lab6 = lab(6, 14);
        BlockRef lab7 = lab(7, 15), lab11 = lab(11, 7), lab12 = lab(12, 7);
        // The two-tone Laboratory checker tile (meta 8, "checkertile"): dark and light gray quarters.
        BlockRef lab8 = lab(8, 7);
        l.add(
            StyleSet
                .named(
                    "lab_white",
                    StyleGroup.MEDICAL,
                    StyleGroup.SERVER_ROOM,
                    StyleGroup.COMMAND,
                    StyleGroup.CAFETERIA)
                .walls(lab0, lab2, lab5, lab(3, 14), lab11, lab12)
                .floors(
                    plain(lab7),
                    plain(lab8),
                    checker(lab7, lab6, 2),
                    borderField(lab12, lab7, 2),
                    stripes(lab7, lab11, 6, true),
                    doubleRing(lab11, lab12, lab7, 1))
                .ceilings(plain(lab1), grid(lab0, lab1, 5), plain(lab0))
                .lamp(litLamp(15), lab(9, 15))
                .accent(lab(15, 14))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, of(v("wool", 0)))
                .furniture(Furniture.SHELF, SHELF)
                .build());

        // D260: the Laboratory finish with its dark gray steel walls (large and small steel, metas 11 and 12), the
        // two-tone checker tile floor and white panel ceilings, for the rooms a lab suits: medical, server, command.
        l.add(
            StyleSet.named("lab_dark", StyleGroup.MEDICAL, StyleGroup.SERVER_ROOM, StyleGroup.COMMAND)
                .walls(lab11, lab12)
                .floors(
                    plain(lab8),
                    borderField(lab7, lab8, 1),
                    checker(lab8, lab7, 3),
                    centerStrip(lab8, lab6, 3),
                    stripes(lab8, lab12, 5, true))
                .ceilings(plain(lab1), grid(lab0, lab1, 5), plain(lab2), plain(lab5))
                .lamp(litLamp(15), lab(9, 15))
                .accent(lab(15, 14))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, of(v("wool", 0)))
                .furniture(Furniture.SHELF, SHELF)
                .build());

        BlockRef f13 = factory(13), f3 = factory(3), f9 = factory(9), f0 = factory(0), f15 = factory(15);
        BlockRef f6 = factory(6), f7 = factory(7);
        l.add(
            StyleSet
                .named(
                    "factory_tile",
                    StyleGroup.POWER,
                    StyleGroup.WORKSHOP,
                    StyleGroup.VEHICLE_BAY,
                    StyleGroup.MISSILE_ASSEMBLY,
                    StyleGroup.STORAGE,
                    StyleGroup.HALL_MAJOR,
                    StyleGroup.HALL_MINOR,
                    StyleGroup.GALLERY)
                .walls(f13, f9, f3, f0)
                .floors(
                    plain(f13),
                    stripes(f13, f6, 8, true),
                    borderField(f7, f3, 1),
                    checker(f0, f13, 2),
                    centerStrip(f13, f9, 3),
                    bands(f13, f3, 4, false))
                .ceilings(plain(f13), grid(f3, technical(7), 5), plain(technical(9)))
                .lamp(litLamp(8), f0)
                .accent(f6)
                .worn(technical(12))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        BlockRef t15 = tyrian(15), t3 = tyrian(3), t8 = tyrian(8), t12 = tyrian(12), t13 = tyrian(13);
        BlockRef t11 = tyrian(11);
        l.add(
            StyleSet
                .named(
                    "tyrian_dark",
                    StyleGroup.COMMAND,
                    StyleGroup.HUB,
                    StyleGroup.GALLERY,
                    StyleGroup.HALL_MAJOR,
                    StyleGroup.GUARD,
                    StyleGroup.SERVER_ROOM)
                .walls(t15, t3, t8, t12)
                .floors(
                    checker(t12, t13, 2),
                    borderField(t8, t12, 2),
                    centerStrip(t12, t11, 3),
                    plain(t8),
                    doubleRing(t11, t3, t12, 2))
                .ceilings(plain(t12), grid(t13, t3, 4), plain(t13))
                .lamp(litLamp(4), t13)
                .accent(t11)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        BlockRef complexWall = of(e("gregguns", "complex_wall", 0), gtConcrete(8), v("stonebrick", 0));
        BlockRef c0 = concrete(0, 8), c1 = concrete(1, 8), c3 = concrete(3, 11), c4 = concrete(4, 12);
        BlockRef c5 = concrete(5, 13), c10 = concrete(10, 7);
        l.add(
            StyleSet
                .named(
                    "concrete_bunker",
                    StyleGroup.STORAGE,
                    StyleGroup.GUARD,
                    StyleGroup.HALL_MINOR,
                    StyleGroup.CREW,
                    StyleGroup.GALLERY,
                    StyleGroup.VEHICLE_BAY)
                .walls(complexWall, c1, c3, c5)
                .floors(
                    plain(c0),
                    borderField(c1, c0, 1),
                    checker(c0, c4, 4),
                    stripes(c0, c10, 6, false),
                    bands(c0, c1, 3, true))
                .ceilings(plain(c3), plain(c1), grid(c0, c3, 6))
                .lamp(litLamp(14), c4)
                .accent(c10)
                .worn(c4)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, BUNK_GREY)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        BlockRef te9 = technical(9), te12 = technical(12), te5 = technical(5), te7 = technical(7);
        BlockRef te11 = technical(11), te13 = technical(13), te3 = technical(3), te15 = technical(15);
        l.add(
            StyleSet
                .named(
                    "technical_works",
                    StyleGroup.POWER,
                    StyleGroup.WATER_TREATMENT,
                    StyleGroup.VEHICLE_BAY,
                    StyleGroup.WORKSHOP,
                    StyleGroup.STORAGE,
                    StyleGroup.HALL_MINOR)
                .walls(te9, te12, te5, te7)
                .floors(
                    plain(f13),
                    stripes(f13, te13, 4, true),
                    borderField(f6, f13, 1),
                    checker(f13, te15, 3),
                    centerStrip(f13, te13, 4))
                .ceilings(plain(te9), grid(te9, te11, 6), plain(te12))
                .lamp(litLamp(5), te7)
                .accent(te3)
                .worn(te12)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        BlockRef m4 = marble(4), m5 = marble(5), m1 = marble(1), m12 = marble(12), m0 = marble(0), m8 = marble(8);
        BlockRef m9 = marble(9);
        BlockRef li0 = limestone(0), li1 = limestone(1), li15 = limestone(15);
        l.add(
            StyleSet.named("marble_admin", StyleGroup.COMMAND, StyleGroup.CAFETERIA, StyleGroup.MEDICAL, StyleGroup.HUB)
                .walls(m4, m5, m1, m12, li0)
                .floors(
                    checker(m0, m5, 1),
                    borderField(m8, m5, 2),
                    doubleRing(m9, m12, m0, 1),
                    plain(li1),
                    checker(li1, m0, 2))
                .ceilings(plain(li0), grid(li0, m4, 5), plain(li15))
                .lamp(litLamp(15), li15)
                .accent(m8)
                .furniture(Furniture.TABLE, WOOD_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        BlockRef g3 = granite(3), g7 = granite(7), g11 = granite(11), g15 = granite(15), g6 = granite(6);
        l.add(
            StyleSet
                .named("granite_hall", StyleGroup.HALL_MAJOR, StyleGroup.HUB, StyleGroup.GALLERY, StyleGroup.COMMAND)
                .walls(g3, g7, g11, g6)
                .floors(
                    checker(g7, g3, 2),
                    borderField(g11, g7, 2),
                    plain(g7),
                    centerStrip(g7, g15, 3),
                    doubleRing(g15, g11, g7, 2))
                .ceilings(plain(g3), grid(g3, g7, 5))
                .lamp(litLamp(13), g6)
                .accent(g15)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        BlockRef zv1 = ztones("vect", 1, tyrian(13)), zv2 = ztones("vect", 2, tyrian(12));
        BlockRef zv3 = ztones("vect", 3, tyrian(13)), zv0 = ztones("vect", 0, tyrian(12));
        BlockRef zk4 = ztones("korp", 4, tyrian(12)), zk12 = ztones("korp", 12, lab(10, 7));
        l.add(
            StyleSet.named("ztones_server", StyleGroup.SERVER_ROOM, StyleGroup.COMMAND, StyleGroup.GUARD)
                .walls(zv1, zv2, zv3, zk4)
                .floors(checker(zv0, zv2, 2), borderField(zv1, zv0, 2), stripes(zv0, zk4, 4, true), plain(zv0))
                .ceilings(plain(zv0), grid(zv0, zv3, 4))
                .lamp(litLamp(4), zv0)
                .accent(zk12)
                .furniture(Furniture.CONSOLE, zk12)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .build());

        BlockRef tk0 = ztones("tank", 0, f13), tk1 = ztones("tank", 1, f13), tk3 = ztones("tank", 3, f3);
        BlockRef tk5 = ztones("tank", 5, f9), lv0 = ztones("lave", 0, lab(0, 15)), lv1 = ztones("lave", 1, lab(2, 14));
        l.add(
            StyleSet
                .named(
                    "ztones_tank",
                    StyleGroup.WATER_TREATMENT,
                    StyleGroup.POWER,
                    StyleGroup.MISSILE_ASSEMBLY,
                    StyleGroup.WORKSHOP)
                .walls(tk1, tk3, tk5, lv1)
                .floors(plain(tk0), checker(tk0, lv0, 4), borderField(tk3, tk0, 2), centerStrip(tk0, lv1, 4))
                .ceilings(plain(tk3), grid(tk0, tk1, 5))
                .lamp(litLamp(8), tk5)
                .accent(lv1)
                .worn(te12)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        // Agon 12 is a muted tan (11 is a loud orange); 13 is dark gray. Their vanilla stand-ins are brown and gray
        // clay.
        BlockRef agonTan = ztones("agon", 12, of(v("stained_hardened_clay", 12)));
        BlockRef agonGrey = ztones("agon", 13, of(v("stained_hardened_clay", 7)));
        BlockRef li5 = limestone(5), li11 = limestone(11);
        l.add(
            StyleSet.named("crew_warm", StyleGroup.CREW, StyleGroup.CAFETERIA, StyleGroup.MEDICAL)
                .walls(li5, li15, li11, li1)
                .floors(plain(li1), checker(li1, li5, 2), borderField(agonGrey, li1, 1), stripes(li1, agonTan, 7, true))
                .ceilings(plain(li0), grid(li0, li5, 6), plain(li15))
                .lamp(litLamp(10), li5)
                .accent(agonTan)
                .furniture(Furniture.TABLE, WOOD_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, BUNK)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        l.add(
            StyleSet
                .named(
                    "steel_panel",
                    StyleGroup.HUB,
                    StyleGroup.HALL_MAJOR,
                    StyleGroup.GALLERY,
                    StyleGroup.MISSILE_ASSEMBLY,
                    StyleGroup.HALL_MINOR)
                .walls(lab11, lab12, t15)
                .floors(
                    checker(lab12, lab11, 3),
                    borderField(f3, lab12, 2),
                    centerStrip(lab11, f6, 3),
                    bands(lab11, lab12, 5, true))
                .ceilings(plain(lab12), grid(lab11, lab12, 5), plain(lab11))
                .lamp(litLamp(15), lab(9, 15))
                .accent(f6)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        return Collections.unmodifiableList(l);
    }
}
