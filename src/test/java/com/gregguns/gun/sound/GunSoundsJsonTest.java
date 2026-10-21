package com.gregguns.gun.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Checks the real {@code assets/gregguns/sounds.json} against the sound pipeline rules, so adding an M2 row is
 * validated automatically: the file exists, a positional file is mono, the event name follows the scheme and avoids the
 * muffler blacklist, and the path avoids the Hodgepodge downmix exclusions.
 */
// Spec: docs/gregguns-spec/03-gun-visuals-audio/gun-sound-pipeline.md (GGS-03-07, 08, 09, 11)
class GunSoundsJsonTest {

    private static final String ROOT = "/assets/gregguns/";

    // Copied from the pack: reference/gtnh/2.9.0-RC-2/.minecraft/config/hodgepodge.cfg, sound.downmixExclusions.
    private static final String[] DOWNMIX_EXCLUSIONS = { "button", "click", "/gui", "menu", "typing", "page" };

    private static final Pattern EVENT = Pattern
        .compile("(guns\\.[a-z][a-z0-9_]*\\.[a-z][a-z0-9_]*|effects\\.[a-z][a-z0-9_]*)");

    private static final List<String> VALID_CATEGORIES = Arrays
        .asList("master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient");

    private static JsonObject events() throws IOException {
        try (Reader r = new InputStreamReader(open(ROOT + "sounds.json"), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(r)
                .getAsJsonObject();
        }
    }

    private static InputStream open(String path) {
        InputStream in = GunSoundsJsonTest.class.getResourceAsStream(path);
        assertNotNull(in, "missing resource " + path);
        return in;
    }

    private static byte[] read(String path) throws IOException {
        try (InputStream in = open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static int identificationHeader(byte[] ogg) {
        // Vorbis identification header: 0x01 "vorbis", version (4 bytes), channels (1), sample rate (4, little endian).
        for (int i = 0; i + 16 < ogg.length; i++) {
            if (ogg[i] == 1 && ogg[i + 1] == 'v'
                && ogg[i + 2] == 'o'
                && ogg[i + 3] == 'r'
                && ogg[i + 4] == 'b'
                && ogg[i + 5] == 'i'
                && ogg[i + 6] == 's') {
                return i;
            }
        }
        throw new IllegalArgumentException("not a Vorbis ogg");
    }

    static int channels(byte[] ogg) {
        return ogg[identificationHeader(ogg) + 11] & 0xff;
    }

    static int sampleRate(byte[] ogg) {
        int i = identificationHeader(ogg);
        return (ogg[i + 12] & 0xff) | (ogg[i + 13] & 0xff) << 8
            | (ogg[i + 14] & 0xff) << 16
            | (ogg[i + 15] & 0xff) << 24;
    }

    /** @return the length in seconds, from the last page's granule position */
    static double seconds(byte[] ogg) {
        for (int i = ogg.length - 14; i >= 0; i--) {
            if (ogg[i] == 'O' && ogg[i + 1] == 'g' && ogg[i + 2] == 'g' && ogg[i + 3] == 'S') {
                long granule = 0;
                for (int b = 7; b >= 0; b--) {
                    granule = granule << 8 | (ogg[i + 6 + b] & 0xff);
                }
                return granule / (double) sampleRate(ogg);
            }
        }
        throw new IllegalArgumentException("no ogg page");
    }

    private static List<String> files(JsonElement event) {
        List<String> out = new ArrayList<>();
        for (JsonElement sound : event.getAsJsonObject()
            .getAsJsonArray("sounds")) {
            String name = sound.isJsonObject() ? sound.getAsJsonObject()
                .get("name")
                .getAsString() : sound.getAsString();
            out.add(name.startsWith("gregguns:") ? name.substring("gregguns:".length()) : name);
        }
        return out;
    }

    @Test
    void registersTheM1Events() throws IOException {
        JsonObject events = events();
        assertTrue(events.has(GunSoundEvents.PISTOL_9MM_FIRE));
        assertTrue(events.has(GunSoundEvents.PISTOL_9MM_RELOAD));
        assertTrue(events.has(GunSoundEvents.BULLET_IMPACT));
        assertEquals(3, files(events.get(GunSoundEvents.PISTOL_9MM_FIRE)).size());
    }

    @Test
    void everyReferencedFileExistsAndIsMono() throws IOException {
        for (Map.Entry<String, JsonElement> e : events().entrySet()) {
            for (String file : files(e.getValue())) {
                byte[] ogg = read(ROOT + "sounds/" + file + ".ogg");
                assertEquals(1, channels(ogg), e.getKey() + ": " + file + " must be mono (GVA-23, GGS-03-07)");
            }
        }
    }

    @Test
    void everyEventFollowsTheNamingScheme() throws IOException {
        for (Map.Entry<String, JsonElement> entry : events().entrySet()) {
            String event = entry.getKey();
            assertTrue(
                EVENT.matcher(event)
                    .matches(),
                event + " must be guns.<weapon>.<action> or effects.<name>");
            assertNull(GunSoundEvents.mufflerBlacklistHit(event), event + " contains a muffler blacklist entry");
        }
    }

    @Test
    void everyEventHasAValidCategory() throws IOException {
        for (Map.Entry<String, JsonElement> e : events().entrySet()) {
            JsonObject event = e.getValue()
                .getAsJsonObject();
            assertTrue(event.has("category"), e.getKey() + " needs a category");
            assertTrue(
                VALID_CATEGORIES.contains(
                    event.get("category")
                        .getAsString()),
                e.getKey() + " has an unknown category");
        }
    }

    @Test
    void noPathContainsADownmixExclusion() throws IOException {
        for (Map.Entry<String, JsonElement> e : events().entrySet()) {
            for (String file : files(e.getValue())) {
                for (String excluded : DOWNMIX_EXCLUSIONS) {
                    assertFalse(("/" + file).contains(excluded), file + " contains downmix exclusion " + excluded);
                }
            }
        }
    }

    @Test
    void noFileIsSharedBetweenFireEvents() throws IOException {
        // GGS-03-09: each weapon's fire sound is its own event, so a player can mute one gun.
        List<String> seen = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : events().entrySet()) {
            if (!e.getKey()
                .endsWith("." + GunSoundEvents.FIRE)) {
                continue;
            }
            // The Nuke Launcher keeps its own event (mutable on its own) but plays the Rocket Launcher's recordings,
            // as Techguns' nukelauncher does (guns.rocketFire); it takes its own files if the asset agent makes them.
            // The Golden Gun keeps its own event but plays the 9mm Pistol's recordings (no new assets).
            // Deviation: the Scatter Laser Rifle fires the Laser Rifle's clips, as in Techguns, until it gets its own.
            if (e.getKey()
                .equals("guns.nuke_launcher.fire")
                || e.getKey()
                    .equals("guns.golden_gun.fire")
                || e.getKey()
                    .equals("guns.scatter_laser_rifle." + GunSoundEvents.FIRE)) {
                continue;
            }
            for (String file : files(e.getValue())) {
                assertFalse(seen.contains(file), file + " is shared between fire events");
                seen.add(file);
            }
        }
    }

    @Test
    void pistolFireLengthFitsTheChannelBudget() throws IOException {
        // The pistol's rate is not known until the gun item lands, so this guards the file lengths at 5 shots/s.
        double longest = 0;
        for (String file : files(events().get(GunSoundEvents.PISTOL_9MM_FIRE))) {
            longest = Math.max(longest, seconds(read(ROOT + "sounds/" + file + ".ogg")));
        }
        assertTrue(longest > 0.5 && longest < 3.0, "unexpected fire length " + longest);
        assertTrue(GunSoundMath.withinChannelBudget(longest, 5.0));
    }
}
