package com.gregguns.icbm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * I7 completeness: every ICBM key in {@code en_US.lang} is used. A key counts as used when the sources hold it as a
 * literal, hold a literal that ends in {@code .} or {@code _} and is a prefix of it (a key built up from a name or an
 * enum), or, for a GT machine's {@code gt.blockmachines.<name>.name}, hold the machine's name as a literal. The other
 * direction (a literal key with no entry) is {@code LangFilesTest}.
 */
class IcbmLangUsageTest {

    private static final Pattern LITERAL = Pattern.compile("\"([A-Za-z0-9_.\\-]+)\"");
    private static final Pattern MACHINE = Pattern.compile("^gt\\.blockmachines\\.(.+)\\.name$");

    @Test
    void everyIcbmLangKeyIsUsed() throws IOException {
        Set<String> literals = literals();
        List<String> prefixes = new ArrayList<>();
        for (String l : literals) {
            if ((l.endsWith(".") || l.endsWith("_")) && l.length() > 8) {
                prefixes.add(l);
            }
        }
        TreeSet<String> unused = new TreeSet<>();
        for (String line : Files
            .readAllLines(Paths.get("src/main/resources/assets/gregguns/lang/en_US.lang"), StandardCharsets.UTF_8)) {
            int eq = line.indexOf('=');
            if (eq <= 0 || line.startsWith("#")) {
                continue;
            }
            String key = line.substring(0, eq)
                .trim();
            if (!key.toLowerCase()
                .contains("icbm") || literals.contains(key)) {
                continue;
            }
            Matcher m = MACHINE.matcher(key);
            if (m.matches() && literals.contains(m.group(1))) {
                continue;
            }
            boolean viaPrefix = false;
            for (String p : prefixes) {
                if (key.startsWith(p)) {
                    viaPrefix = true;
                    break;
                }
            }
            if (!viaPrefix) {
                unused.add(key);
            }
        }
        assertTrue(unused.isEmpty(), "ICBM lang keys that no source uses: " + unused);
    }

    private static Set<String> literals() throws IOException {
        Set<String> out = new HashSet<>();
        Path root = Paths.get("src/main");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : (Iterable<Path>) files.filter(
                f -> f.toString()
                    .matches(".*\\.(java|json|xml|lua)"))::iterator) {
                Matcher m = LITERAL.matcher(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
                while (m.find()) {
                    out.add(m.group(1));
                }
            }
        }
        return out;
    }
}
