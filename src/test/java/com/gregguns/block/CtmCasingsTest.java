package com.gregguns.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * The connected-texture files for the base casings (design record 0023). The pack's CTM engine, Angelica's bundled
 * MCPatcherForge, reads {@code assets/gregguns/mcpatcher/ctm/<name>/<name>.properties} and tiles {@code 0.png} to
 * {@code 46.png} beside it. A typo here fails silently in game (the block just keeps its plain icon), so this test
 * checks what the engine would parse: the method, the block name, the metadata and the tile files.
 */
class CtmCasingsTest {

    private static final Path RES = Paths.get("src/main/resources/assets/gregguns");
    private static final Path CTM = RES.resolve("mcpatcher/ctm");
    private static final Path BASE = RES.resolve("textures/blocks/casings");
    private static final Path CASINGS_SRC = Paths.get("src/main/java/com/gregguns/block/GregGunsCasings.java");

    /** Texture name (also the folder name) to the meta that connects; the walls and plating, nothing else. */
    private static final Map<String, Integer> CONNECTED = new LinkedHashMap<>();
    static {
        CONNECTED.put("works_casing", GregGunsCasings.WORKS_CASING);
        CONNECTED.put("gantry_casing", GregGunsCasings.GANTRY_CASING);
        CONNECTED.put("bastion_plating_iron", GregGunsCasings.BASTION_IRON);
        CONNECTED.put("bastion_plating_steel", GregGunsCasings.BASTION_STEEL);
        CONNECTED.put("bastion_plating_aluminum", GregGunsCasings.BASTION_ALUMINUM);
        CONNECTED.put("bastion_plating_carbon", GregGunsCasings.BASTION_CARBON);
    }

    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files
            .newBufferedReader(CTM.resolve(name + "/" + name + ".properties"), StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    private static BufferedImage image(Path file) throws IOException {
        BufferedImage img = ImageIO.read(file.toFile());
        assertNotNull(img, file.toString());
        return img;
    }

    @Test
    void exactlyTheConnectedCasingsHaveASet() throws IOException {
        List<String> dirs;
        try (Stream<Path> s = Files.list(CTM)) {
            dirs = s.filter(Files::isDirectory)
                .map(
                    p -> p.getFileName()
                        .toString())
                .sorted()
                .collect(Collectors.toList());
        }
        assertEquals(
            CONNECTED.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toList()),
            dirs);
    }

    @Test
    void everyPropertiesFileIsAStandardCtmRuleForItsMetaOnly() throws IOException {
        Set<Integer> metas = new HashSet<>();
        for (Map.Entry<String, Integer> e : CONNECTED.entrySet()) {
            String name = e.getKey();
            Properties p = load(name);
            assertEquals("ctm", p.getProperty("method"), name);
            assertEquals("gregguns:" + GregGunsCasings.NAME, p.getProperty("matchBlocks"), name);
            assertEquals(String.valueOf(e.getValue()), p.getProperty("metadata"), name);
            // connect=block compares block and metadata, so each meta joins only itself
            assertEquals("block", p.getProperty("connect"), name);
            assertEquals("0-46", p.getProperty("tiles"), name);
            assertTrue(metas.add(e.getValue()), "two files for meta " + e.getValue());
            try (Stream<Path> s = Files.list(CTM.resolve(name))) {
                assertEquals(
                    1,
                    s.filter(
                        f -> f.toString()
                            .endsWith(".properties"))
                        .count(),
                    name);
            }
        }
    }

    @Test
    void everyTileExistsIs16x16AndTileZeroIsTheBaseTexture() throws IOException {
        for (String name : CONNECTED.keySet()) {
            for (int i = 0; i < 47; i++) {
                BufferedImage tile = image(CTM.resolve(name + "/" + i + ".png"));
                assertEquals(16, tile.getWidth(), name + " tile " + i);
                assertEquals(16, tile.getHeight(), name + " tile " + i);
            }
            BufferedImage zero = image(CTM.resolve(name + "/0.png"));
            BufferedImage base = image(BASE.resolve(name + ".png"));
            assertEquals(16, base.getWidth(), name);
            assertEquals(16, base.getHeight(), name);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    assertEquals(base.getRGB(x, y), zero.getRGB(x, y), name + " tile 0 at " + x + "," + y);
                }
            }
            // the fully surrounded tile must differ from the isolated one, or the frame was never removed
            boolean differs = false;
            BufferedImage plain = image(CTM.resolve(name + "/26.png"));
            for (int y = 0; y < 16 && !differs; y++) {
                for (int x = 0; x < 16; x++) {
                    differs |= plain.getRGB(x, y) != zero.getRGB(x, y);
                }
            }
            assertTrue(differs, name + " tile 26 equals tile 0");
        }
    }

    @Test
    void theBaseTexturesAreTheOnesTheBlockRegisters() throws IOException {
        String src = new String(Files.readAllBytes(CASINGS_SRC), StandardCharsets.UTF_8);
        for (String name : CONNECTED.keySet()) {
            assertTrue(src.contains("\"gregguns:casings/" + name + "\""), name + " is not a GregGunsCasings texture");
        }
    }
}
