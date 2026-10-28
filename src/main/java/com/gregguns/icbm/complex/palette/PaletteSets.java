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

    // ---- chain helpers (D265) ----
    //
    // A room's floor, wall and ceiling are three different blocks, whatever the pack holds. So each role of a set
    // takes its own block at every level of the chain: its own Chisel block, its own GregTech block and its own
    // vanilla stand-in. Walls fall back to GT concrete and stone bricks (or quartz, smooth sandstone), floors to GT
    // marble or basalt and smooth double slabs (or clay), ceilings to quartz (or smooth sandstone) with no GT step,
    // except where a set says otherwise. RoleBlocksTest checks every set's three roles share no block at any level.
    //
    // GregTech's stone layers overwrite plain stone, stained clay, cobblestone and its own granite and marble/basalt
    // blocks wherever a blob passes, after the complex is written. So at most one role of a set may name any of
    // those blocks (GT_REPLACEABLE), or a blob through two roles would leave them sharing its block.

    private static BlockRef.Entry gtConcrete(int meta) {
        return e(GT, "gt.blockconcretes", meta);
    }

    private static BlockRef.Entry gtStone(int meta) {
        return e(GT, "gt.blockstones", meta);
    }

    private static BlockRef.Entry gtGranite(int meta) {
        return e(GT, "gt.blockgranites", meta);
    }

    /** Vanilla stand-ins, by role. */
    private static final BlockRef.Entry WALL_BRICK = v("stonebrick", 0);
    private static final BlockRef.Entry WALL_QUARTZ = v("quartz_block", 0);
    private static final BlockRef.Entry WALL_SANDSTONE = v("sandstone", 2);
    private static final BlockRef.Entry FLOOR_SLAB = v("double_stone_slab", 0);
    private static final BlockRef.Entry CEILING_QUARTZ = v("quartz_block", 0);
    private static final BlockRef.Entry CEILING_SANDSTONE = v("sandstone", 2);

    /**
     * The blocks GregTech's stone layer generator replaces with its granite or marble/basalt blobs (and its ore veins
     * with ores), by registry name. At most one role of a set names any of them.
     */
    // Source: WorldgenStone replaces stone, stained clay, cobblestone, end stone, netherrack and GT granites and
    // stones, at 5.09.54.205:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/WorldgenStone.java#L223-L230
    static final java.util.Set<String> GT_REPLACEABLE = java.util.Collections.unmodifiableSet(
        new java.util.HashSet<>(
            java.util.Arrays.asList(
                "minecraft:stone",
                "minecraft:stained_hardened_clay",
                "minecraft:cobblestone",
                "minecraft:end_stone",
                "minecraft:netherrack",
                "gregtech:gt.blockgranites",
                "gregtech:gt.blockstones")));

    private static BlockRef ch(String name, int meta, BlockRef.Entry... tail) {
        BlockRef.Entry[] l = new BlockRef.Entry[tail.length + 1];
        l[0] = e(CH, name, meta);
        System.arraycopy(tail, 0, l, 1, tail.length);
        return of(l);
    }

    /** A dead lamp or console screen of the Laboratory family (not a building role). */
    private static BlockRef lab(int meta, int gtConcreteMeta) {
        return ch("laboratoryblock", meta, gtConcrete(gtConcreteMeta), v("quartz_block", 0));
    }

    /** A dead lamp or accent of the Factory family. */
    private static BlockRef factory(int meta) {
        return ch("factoryblock", meta, gtConcrete(3), v("stonebrick", 0));
    }

    private static BlockRef technical(int meta) {
        return ch("technical", meta, gtStone(11), v("stonebrick", 2));
    }

    private static BlockRef tyrian(int meta) {
        return ch("tyrian", meta, gtConcrete(7), v("stonebrick", 0));
    }

    /** A Ztones block, then a fallback chain. */
    private static BlockRef ztones(String set, int meta, BlockRef fallback) {
        List<BlockRef.Entry> l = new ArrayList<>();
        l.add(e(ZT, ztonesName(set), meta));
        l.addAll(fallback.chain());
        return of(l.toArray(new BlockRef.Entry[0]));
    }

    /** @return the registry name of a Ztones metal set block: {@code tile.<set>Block} */
    static String ztonesName(String set) {
        return "tile." + set + "Block";
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

        // Shared floor and ceiling families. Each set below takes each family in one role only (D265).
        // Nucrete (light 8..15, dark 0..7) and Andesite as floors with GT marble or basalt and smooth slabs behind
        // them.
        BlockRef nl9 = ch("nucrete", 9, gtStone(7), FLOOR_SLAB), nl11 = ch("nucrete", 11, gtStone(7), FLOOR_SLAB);
        BlockRef nl12 = ch("nucrete", 12, gtStone(7), FLOOR_SLAB), nl13 = ch("nucrete", 13, gtStone(7), FLOOR_SLAB);
        BlockRef nl15 = ch("nucrete", 15, gtStone(7), FLOOR_SLAB);
        BlockRef nd3 = ch("nucrete", 3, gtStone(15), FLOOR_SLAB), nd4 = ch("nucrete", 4, gtStone(15), FLOOR_SLAB);
        BlockRef nd5 = ch("nucrete", 5, gtStone(15), FLOOR_SLAB), nd7 = ch("nucrete", 7, gtStone(15), FLOOR_SLAB);
        // Marble panels as ceilings over the pale rooms.
        BlockRef mc0 = ch("marble", 0, CEILING_SANDSTONE), mc4 = ch("marble", 4, CEILING_SANDSTONE);
        BlockRef mc7 = ch("marble", 7, CEILING_SANDSTONE);
        // Technical insulation, vents and cables as ceilings over the works.
        BlockRef tc9 = ch("technical", 9, CEILING_QUARTZ), tc7 = ch("technical", 7, CEILING_QUARTZ);
        BlockRef tc3 = ch("technical", 3, CEILING_QUARTZ);

        // Pale lab rooms: Laboratory wall panels and its dark gray steel (11, 12) as walls, light Nucrete floors,
        // Marble panel ceilings.
        BlockRef lw0 = lab(0, 15), lw2 = lab(2, 15), lw3 = lab(3, 15), lw5 = lab(5, 15), lw11 = lab(11, 7);
        BlockRef lw12 = lab(12, 7);
        l.add(
            StyleSet
                .named(
                    "lab_white",
                    StyleGroup.MEDICAL,
                    StyleGroup.SERVER_ROOM,
                    StyleGroup.COMMAND,
                    StyleGroup.CAFETERIA)
                .walls(lw0, lw2, lw5, lw3, lw11, lw12)
                .floors(
                    plain(nl15),
                    checker(nl15, nl11, 2),
                    borderField(nl9, nl15, 1),
                    stripes(nl15, nl13, 6, true),
                    doubleRing(nl11, nl9, nl15, 1),
                    plain(nl12))
                .ceilings(plain(mc7), grid(mc0, mc4, 5), plain(mc4))
                .lamp(litLamp(15), lab(9, 15))
                .accent(lab(15, 14))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, of(v("wool", 0)))
                .furniture(Furniture.SHELF, SHELF)
                .build());

        // D260, D265: the Laboratory two-tone checker tile floor (meta 8) with its floor tiles, under dark Nucrete
        // panel walls and Marble ceilings, for the rooms a lab suits. The Laboratory's steel walls are one block with
        // its floor tiles, so they dress lab_white and steel_panel rooms instead.
        BlockRef lf6 = ch("laboratoryblock", 6, gtStone(15), FLOOR_SLAB);
        BlockRef lf7 = ch("laboratoryblock", 7, gtStone(15), FLOOR_SLAB);
        BlockRef lf8 = ch("laboratoryblock", 8, gtStone(15), FLOOR_SLAB);
        BlockRef ndw1 = ch("nucrete", 1, gtConcrete(7), WALL_BRICK), ndw2 = ch("nucrete", 2, gtConcrete(7), WALL_BRICK);
        BlockRef ndw3 = ch("nucrete", 3, gtConcrete(7), WALL_BRICK), ndw5 = ch("nucrete", 5, gtConcrete(7), WALL_BRICK);
        l.add(
            StyleSet.named("lab_dark", StyleGroup.MEDICAL, StyleGroup.SERVER_ROOM, StyleGroup.COMMAND)
                .walls(ndw1, ndw2, ndw3, ndw5)
                .floors(
                    plain(lf8),
                    borderField(lf7, lf8, 1),
                    checker(lf8, lf7, 3),
                    centerStrip(lf8, lf6, 3),
                    stripes(lf8, lf7, 5, true))
                .ceilings(plain(mc0), grid(mc0, mc7, 5), plain(mc7), plain(mc4))
                .lamp(litLamp(15), lab(9, 15))
                .accent(lab(15, 14))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, of(v("wool", 0)))
                .furniture(Furniture.SHELF, SHELF)
                .build());

        // Plant rooms: Factory plating walls, Chisel stone-panel floors, Technical insulation and vent ceilings.
        BlockRef f13 = factory(13), f3 = factory(3), f9 = factory(9), f0 = factory(0);
        BlockRef s12 = ch("stonebricksmooth", 12, gtStone(11), FLOOR_SLAB);
        BlockRef s13 = ch("stonebricksmooth", 13, gtStone(11), FLOOR_SLAB);
        BlockRef s5 = ch("stonebricksmooth", 5, gtStone(11), FLOOR_SLAB);
        BlockRef s4 = ch("stonebricksmooth", 4, gtStone(11), FLOOR_SLAB);
        // A worn patch: weathered Chisel concrete, a floor-only block in every set that wears.
        BlockRef wornConcrete = ch("concrete", 4, FLOOR_SLAB);
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
                    plain(s12),
                    stripes(s12, s13, 8, true),
                    borderField(s13, s12, 1),
                    checker(s12, s5, 2),
                    centerStrip(s12, s4, 3),
                    bands(s12, s5, 4, false))
                .ceilings(plain(tc9), grid(tc9, tc7, 5), grid(tc9, tc3, 6))
                .lamp(litLamp(8), f0)
                .accent(factory(6))
                .worn(wornConcrete)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        // Command and halls: dark Tyrian plate walls, dark Nucrete tile floors, Technical cinder-panel ceilings.
        BlockRef t15 = tyrian(15), t3 = tyrian(3), t8 = tyrian(8), t12 = tyrian(12);
        BlockRef k2 = ch("technical4", 2, CEILING_QUARTZ), k9 = ch("technical4", 9, CEILING_QUARTZ);
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
                    checker(nd7, nd3, 2),
                    borderField(nd4, nd7, 2),
                    centerStrip(nd7, nd5, 3),
                    plain(nd3),
                    doubleRing(nd5, nd4, nd7, 2))
                .ceilings(plain(k2), grid(k9, k2, 4), plain(k9))
                .lamp(litLamp(4), tyrian(13))
                .accent(tyrian(11))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        // Bunker rooms: the Silo Complex Wall (D205) or Chisel concrete walls, Andesite floors, light Nucrete
        // ceilings.
        BlockRef complexWall = of(e("gregguns", "complex_wall", 0), gtConcrete(8), WALL_BRICK);
        BlockRef c1 = ch("concrete", 1, gtConcrete(8), WALL_BRICK), c3 = ch("concrete", 3, gtConcrete(11), WALL_BRICK);
        BlockRef c5 = ch("concrete", 5, gtConcrete(13), WALL_BRICK);
        BlockRef a10 = ch("andesite", 10, gtStone(11), FLOOR_SLAB), a6 = ch("andesite", 6, gtStone(11), FLOOR_SLAB);
        BlockRef a1 = ch("andesite", 1, gtStone(11), FLOOR_SLAB), a3 = ch("andesite", 3, gtStone(11), FLOOR_SLAB);
        BlockRef ncl8 = ch("nucrete", 8, CEILING_QUARTZ), ncl9 = ch("nucrete", 9, CEILING_QUARTZ);
        BlockRef ncl11 = ch("nucrete", 11, CEILING_QUARTZ);
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
                    plain(a10),
                    borderField(a6, a10, 1),
                    checker(a10, a1, 4),
                    stripes(a10, a3, 6, false),
                    bands(a10, a6, 3, true))
                .ceilings(plain(ncl11), plain(ncl9), grid(ncl11, ncl8, 6))
                .lamp(litLamp(14), ch("concrete", 4, gtConcrete(12), WALL_BRICK))
                .accent(ch("concrete", 10, gtConcrete(7), WALL_BRICK))
                .worn(ch("andesite", 11, FLOOR_SLAB))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, BUNK_GREY)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        // Works rooms: Technical insulation, rusty plate and pipe walls, Factory plating floors, dark Nucrete ceilings.
        BlockRef te9 = technical(9), te12 = technical(12), te5 = technical(5), te7 = technical(7);
        BlockRef ff13 = ch("factoryblock", 13, gtConcrete(3), FLOOR_SLAB);
        BlockRef ff9 = ch("factoryblock", 9, gtConcrete(3), FLOOR_SLAB);
        BlockRef ff3 = ch("factoryblock", 3, gtConcrete(3), FLOOR_SLAB);
        BlockRef ncd3 = ch("nucrete", 3, CEILING_QUARTZ), ncd1 = ch("nucrete", 1, CEILING_QUARTZ);
        BlockRef ncd7 = ch("nucrete", 7, CEILING_QUARTZ);
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
                    plain(ff13),
                    stripes(ff13, ff9, 4, true),
                    borderField(ff3, ff13, 1),
                    checker(ff13, ff9, 3),
                    centerStrip(ff13, ff3, 4))
                .ceilings(plain(ncd3), grid(ncd3, ncd1, 6), plain(ncd7))
                .lamp(litLamp(5), technical(7))
                .accent(technical(3))
                .worn(wornConcrete)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        // Offices: Marble panel walls, polished Andesite floors, Limestone ceilings.
        BlockRef m4 = ch("marble", 4, gtStone(3), WALL_QUARTZ), m5 = ch("marble", 5, gtStone(3), WALL_QUARTZ);
        BlockRef m1 = ch("marble", 1, gtStone(3), WALL_QUARTZ), m12 = ch("marble", 12, gtStone(3), WALL_QUARTZ);
        BlockRef ap10 = ch("andesite", 10, gtConcrete(15), FLOOR_SLAB);
        BlockRef ap6 = ch("andesite", 6, gtConcrete(15), FLOOR_SLAB);
        BlockRef ap1 = ch("andesite", 1, gtConcrete(15), FLOOR_SLAB);
        BlockRef ap4 = ch("andesite", 4, gtConcrete(15), FLOOR_SLAB);
        BlockRef li0 = ch("limestone", 0, CEILING_SANDSTONE), li15 = ch("limestone", 15, CEILING_SANDSTONE);
        l.add(
            StyleSet.named("marble_admin", StyleGroup.COMMAND, StyleGroup.CAFETERIA, StyleGroup.MEDICAL, StyleGroup.HUB)
                .walls(m4, m5, m1, m12)
                .floors(
                    checker(ap10, ap6, 2),
                    borderField(ap1, ap10, 2),
                    doubleRing(ap4, ap6, ap10, 1),
                    plain(ap10),
                    plain(ap6))
                .ceilings(plain(li0), grid(li0, li15, 5), plain(li15))
                .lamp(litLamp(15), li15)
                .accent(ch("marble", 8, gtStone(3), WALL_QUARTZ))
                .furniture(Furniture.TABLE, WOOD_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        // Great halls: GregTech black and red granite walls, GT concrete floors, Technical cinder-panel ceilings. The
        // floor is not GT basalt: a GT granite blob through wall and floor would leave both granite.
        BlockRef g3 = of(gtGranite(3), v("stonebrick", 3)), g7 = of(gtGranite(7), v("stonebrick", 3));
        BlockRef g11 = of(gtGranite(11), v("stonebrick", 3)), g6 = of(gtGranite(6), v("stonebrick", 3));
        BlockRef b3 = of(gtConcrete(3), FLOOR_SLAB), b7 = of(gtConcrete(7), FLOOR_SLAB),
            b6 = of(gtConcrete(6), FLOOR_SLAB);
        BlockRef cc2 = ch("technical4", 2, CEILING_QUARTZ), cc9 = ch("technical4", 9, CEILING_QUARTZ);
        l.add(
            StyleSet
                .named("granite_hall", StyleGroup.HALL_MAJOR, StyleGroup.HUB, StyleGroup.GALLERY, StyleGroup.COMMAND)
                .walls(g3, g7, g11, g6)
                .floors(
                    checker(b7, b3, 2),
                    borderField(b6, b7, 2),
                    plain(b7),
                    centerStrip(b7, b3, 3),
                    doubleRing(b6, b3, b7, 2))
                .ceilings(plain(cc2), grid(cc9, cc2, 5))
                .lamp(litLamp(13), g6)
                .accent(of(gtGranite(15), v("stonebrick", 3)))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        // Server rooms: Ztones Vect dark panel walls, Tank plating floors, Korp dark ceilings.
        BlockRef zv1 = ztones("vect", 1, tyrian(13)), zv2 = ztones("vect", 2, tyrian(12));
        BlockRef zv3 = ztones("vect", 3, tyrian(13));
        BlockRef ztf0 = ztones("tank", 0, nd7), ztf1 = ztones("tank", 1, nd3);
        BlockRef zk4 = ztones("korp", 4, k2);
        BlockRef zk12 = ztones("korp", 12, lab(10, 7));
        l.add(
            StyleSet.named("ztones_server", StyleGroup.SERVER_ROOM, StyleGroup.COMMAND, StyleGroup.GUARD)
                .walls(zv1, zv2, zv3)
                .floors(checker(ztf0, ztf1, 2), borderField(ztf1, ztf0, 2), stripes(ztf0, ztf1, 4, true), plain(ztf0))
                .ceilings(plain(zk4), grid(zk4, k9, 4))
                .lamp(litLamp(4), ztones("vect", 0, tyrian(12)))
                .accent(zk12)
                .furniture(Furniture.CONSOLE, zk12)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .build());

        // Plant rooms: Ztones Tank plating walls, Lave pale tile floors, Vect dark ceilings.
        BlockRef tk1 = ztones("tank", 1, f13), tk3 = ztones("tank", 3, f3), tk5 = ztones("tank", 5, f9);
        BlockRef lv0 = ztones("lave", 0, nl15), lv1 = ztones("lave", 1, nl11);
        BlockRef zvc0 = ztones("vect", 0, tc9);
        l.add(
            StyleSet
                .named(
                    "ztones_tank",
                    StyleGroup.WATER_TREATMENT,
                    StyleGroup.POWER,
                    StyleGroup.MISSILE_ASSEMBLY,
                    StyleGroup.WORKSHOP)
                .walls(tk1, tk3, tk5)
                .floors(plain(lv0), checker(lv0, lv1, 4), borderField(lv1, lv0, 2), centerStrip(lv0, lv1, 4))
                .ceilings(plain(zvc0), grid(zvc0, tc7, 5))
                .lamp(litLamp(8), tk5)
                .accent(lv1)
                .worn(wornConcrete)
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        // Crew rooms: Limestone walls, light Nucrete floors with Ztones Agon tan and gray rows, Marble ceilings.
        // Agon 12 is a muted tan (11 is a loud orange); 13 is dark gray. Their vanilla stand-ins are brown and gray
        // clay.
        BlockRef agonTan = ztones("agon", 12, of(v("stained_hardened_clay", 12)));
        BlockRef agonGrey = ztones("agon", 13, of(v("stained_hardened_clay", 7)));
        BlockRef li5 = ch("limestone", 5, gtConcrete(11), WALL_SANDSTONE);
        BlockRef lw15 = ch("limestone", 15, gtConcrete(11), WALL_SANDSTONE);
        BlockRef li11 = ch("limestone", 11, gtConcrete(11), WALL_SANDSTONE);
        BlockRef li1 = ch("limestone", 1, gtConcrete(11), WALL_SANDSTONE);
        BlockRef mq0 = ch("marble", 0, CEILING_QUARTZ), mq7 = ch("marble", 7, CEILING_QUARTZ);
        BlockRef mq4 = ch("marble", 4, CEILING_QUARTZ);
        l.add(
            StyleSet.named("crew_warm", StyleGroup.CREW, StyleGroup.CAFETERIA, StyleGroup.MEDICAL)
                .walls(li5, lw15, li11, li1)
                .floors(
                    plain(nl15),
                    checker(nl15, nl11, 2),
                    borderField(agonGrey, nl15, 1),
                    stripes(nl15, agonTan, 7, true))
                .ceilings(plain(mq0), grid(mq0, mq7, 6), plain(mq4))
                .lamp(litLamp(10), li5)
                .accent(agonTan)
                .furniture(Furniture.TABLE, WOOD_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .furniture(Furniture.BUNK, BUNK)
                .furniture(Furniture.SHELF, SHELF)
                .build());

        // Halls and bays: the Laboratory's large and small dark gray steel and Tyrian plate as walls, dark Nucrete
        // floors, Technical insulation and vent ceilings.
        BlockRef sw11 = ch("laboratoryblock", 11, gtConcrete(7), WALL_BRICK);
        BlockRef sw12 = ch("laboratoryblock", 12, gtConcrete(7), WALL_BRICK);
        l.add(
            StyleSet
                .named(
                    "steel_panel",
                    StyleGroup.HUB,
                    StyleGroup.HALL_MAJOR,
                    StyleGroup.GALLERY,
                    StyleGroup.MISSILE_ASSEMBLY,
                    StyleGroup.HALL_MINOR)
                .walls(sw11, sw12, t15)
                .floors(
                    checker(nd3, nd7, 3),
                    borderField(nd5, nd7, 2),
                    centerStrip(nd7, nd5, 3),
                    bands(nd7, nd3, 5, true))
                .ceilings(plain(tc9), grid(tc9, tc7, 5), plain(tc7))
                .lamp(litLamp(15), lab(9, 15))
                .accent(factory(6))
                .furniture(Furniture.TABLE, STONE_TABLE)
                .furniture(Furniture.CONSOLE, SCREEN_DARK)
                .build());

        return Collections.unmodifiableList(l);
    }
}
