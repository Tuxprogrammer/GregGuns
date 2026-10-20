package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guards the "every GregGuns machine exists on both sides" rule by reading the registration sources: a client that
 * lacks a machine at an id crashes GT's item render (the Retort crash).
 */
class MteRegistrationTest {

    private static final Path SRC = Paths.get("src", "main", "java", "com", "gregguns");
    private static final Pattern CONSTANT = Pattern.compile("GregGunsMTEIDs[.]([A-Z0-9_]+)");
    private static final String[] SIDE_WORDS = { "SideOnly", "Side.CLIENT", "Side.SERVER", "isClient", "isServer",
        "isRemote", "getEffectiveSide", "ClientProxy" };

    private static List<Path> sources() throws IOException {
        try (Stream<Path> s = Files.walk(SRC)) {
            return s.filter(
                p -> p.toString()
                    .endsWith(".java"))
                .collect(Collectors.toList());
        }
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    /** The registration classes: every non-test, non-scenario file that constructs an MTE with a GregGunsMTEIDs id. */
    private static List<Path> registrars() throws IOException {
        Pattern ctor = Pattern.compile("new MTE[A-Za-z]+[(][ \t\r\n]*(?:[A-Za-z]+[.])?GregGunsMTEIDs[.]");
        return sources().stream()
            .filter(
                p -> !p.getFileName()
                    .toString()
                    .endsWith("Scenarios.java"))
            .filter(p -> {
                try {
                    return ctor.matcher(read(p))
                        .find();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            })
            .collect(Collectors.toList());
    }

    @Test
    void everyIdHasAConstructorCallInCommonRegistrationCode() throws IOException {
        Set<String> constructed = new HashSet<>();
        for (Path p : registrars()) {
            Matcher m = CONSTANT.matcher(read(p));
            while (m.find()) {
                constructed.add(m.group(1));
            }
        }
        // Tier tables (AmmoPressTier, Repair/Rampart/Turret specs) hold the ids that are built in a loop.
        for (Path p : sources()) {
            String name = p.getFileName()
                .toString();
            if (name.endsWith("Tier.java") || name.endsWith("Spec.java")) {
                Matcher m = CONSTANT.matcher(read(p));
                while (m.find()) {
                    constructed.add(m.group(1));
                }
            }
        }
        for (java.lang.reflect.Field f : GregGunsMTEIDs.class.getFields()) {
            if (f.getType() == int.class && !f.getName()
                .startsWith("BLOCK_")) {
                assertTrue(constructed.contains(f.getName()), f.getName() + " is never registered");
            }
        }
    }

    @Test
    void registrationCodeDoesNotBranchOnSide() throws IOException {
        List<Path> all = registrars();
        assertFalse(all.isEmpty());
        all.add(SRC.resolve(Paths.get("machine", "MtePlaceholders.java")));
        all.add(SRC.resolve(Paths.get("machine", "MachinesModule.java")));
        for (Path p : all) {
            String text = read(p);
            for (String word : SIDE_WORDS) {
                // Javadoc may mention "ClientProxy"; strip comments before looking.
                String code = text.replaceAll("(?s)/[*].*?[*]/", "")
                    .replaceAll("(?m)//.*$", "");
                assertFalse(code.contains(word), p.getFileName() + " branches on side: " + word);
            }
        }
    }

    @Test
    void thePlaceholderPassFillsExactlyTheFreeIds() {
        Set<Integer> taken = new HashSet<>();
        taken.add(GregGunsMTEIDs.RETORT);
        taken.add(GregGunsMTEIDs.AMMO_WORKS);
        List<Integer> missing = MtePlaceholders.missing(taken::contains);
        assertEquals(GregGunsMTEIDs.all().length - 2, missing.size());
        assertFalse(missing.contains(GregGunsMTEIDs.RETORT));
        assertTrue(missing.contains(GregGunsMTEIDs.RAMPART));
        assertTrue(
            MtePlaceholders.missing(i -> true)
                .isEmpty());
    }

    @Test
    void theCommonProxyRunsThePlaceholderPassAfterTheModules() throws IOException {
        String proxy = read(SRC.resolve("CommonProxy.java"));
        int modules = proxy.indexOf("modules.init(event)");
        int holders = proxy.indexOf("MtePlaceholders.registerMissing()");
        assertTrue(modules >= 0 && holders > modules);
    }
}
