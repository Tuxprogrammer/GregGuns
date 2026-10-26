package com.gregguns.core.gate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The ported-code gate (ICBM roadmap I0, CONTRIBUTING § Ported code (MIT)). The real scan covers every main source file
 * with a {@code Ported from ICBM-Classic (MIT):} header; the fixtures under {@code src/test/resources/portedgate}
 * prove the checks catch a bad port and accept a good one.
 */
class PortedCodeGateTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Path TEST = Paths.get("src/test/java");
    private static final Path FIXTURES = Paths.get("src/test/resources/portedgate");

    /** The real tree holds about a thousand files; far below that means the scan is looking at the wrong place. */
    private static final int MIN_MAIN_FILES = 500;

    private static String text(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static Set<String> realBugs() throws IOException {
        return PortedCodeGate.bugIds(text("docs/icbm-spec/BUGS.md"));
    }

    @Test
    void everyPortedMainFileMeetsTheGate() throws IOException {
        String notices = text("THIRD-PARTY-NOTICES.md");
        Set<String> bugs = realBugs();
        List<String> violations = new ArrayList<>();
        // The scan must have looked at the real tree, and no file may name the upstream project without the header.
        violations.addAll(PortedCodeGate.checkRoot(MAIN, MIN_MAIN_FILES));
        violations.addAll(PortedCodeGate.unmarkedMentions(MAIN));
        for (Path file : PortedCodeGate.portedFiles(MAIN)) {
            for (String v : PortedCodeGate.check(file, MAIN, TEST, notices, bugs)) {
                violations.add(file + ": " + v);
            }
        }
        assertTrue(violations.isEmpty(), "ported-code gate violations:\n" + String.join("\n", violations));
    }

    @Test
    void theNoticesFileHasTheIcbmEntry() throws IOException {
        assertTrue(PortedCodeGate.hasIcbmNotice(text("THIRD-PARTY-NOTICES.md")));
    }

    @Test
    void theBugRegisterIsReadable() throws IOException {
        assertTrue(realBugs().contains("ICBM-01-41"));
        assertTrue(realBugs().size() > 50);
    }

    @Test
    void theGoodSamplePasses() throws IOException {
        Path main = FIXTURES.resolve("good/main");
        List<Path> ported = PortedCodeGate.portedFiles(main);
        assertEquals(1, ported.size());
        List<String> violations = PortedCodeGate
            .check(ported.get(0), main, FIXTURES.resolve("good/test"), text("THIRD-PARTY-NOTICES.md"), realBugs());
        assertTrue(violations.isEmpty(), violations.toString());
    }

    @Test
    void theBadSampleFailsEveryRule() throws IOException {
        Path main = FIXTURES.resolve("bad/main");
        List<Path> ported = PortedCodeGate.portedFiles(main);
        assertEquals(1, ported.size());
        // An empty notices file stands in for a missing ICBM-Classic entry.
        List<String> violations = PortedCodeGate
            .check(ported.get(0), main, FIXTURES.resolve("bad/test"), "# Third-party notices\n", realBugs());
        for (String rule : new String[] { PortedCodeGate.PERMALINK, PortedCodeGate.NOTICE, PortedCodeGate.JAVADOC,
            PortedCodeGate.TEST_CLASS, PortedCodeGate.BUG_ID }) {
            assertTrue(
                violations.stream()
                    .anyMatch(v -> v.startsWith(rule + ":")),
                "rule " + rule + " should fire, got " + violations);
        }
        assertTrue(
            violations.stream()
                .anyMatch(v -> v.contains("ICBM-99-99")),
            "the invented bug ID is named");
        assertFalse(
            violations.stream()
                .anyMatch(v -> v.startsWith(PortedCodeGate.BUG_ID) && v.contains("ICBM-01-41")),
            "the real bug ID passes");
    }

    @Test
    void aMissingPermalinkIsCaughtToo() throws IOException {
        // Header present but no link at all.
        String source = "/** Doc. */\n// Ported from ICBM-Classic (MIT): see upstream\npublic final class X {}\n";
        assertTrue(PortedCodeGate.isPorted(source));
        assertTrue(PortedCodeGate.hasTypeJavadoc(source));
        assertFalse(PortedCodeGate.hasTypeJavadoc("// Ported from ICBM-Classic (MIT): x\npublic final class X {}\n"));
        assertFalse(PortedCodeGate.hasTypeJavadoc("/* not javadoc */\npublic final class X {}\n"));
    }

    @Test
    void textMentioningTheHeaderMidSentenceIsNotAPort() {
        assertFalse(PortedCodeGate.isPorted("// We never say 'Ported from ICBM-Classic (MIT):' here\nclass X {}\n"));
    }

    private static List<String> checkFixture(String name) throws IOException {
        Path main = FIXTURES.resolve(name + "/main");
        List<Path> ported = PortedCodeGate.portedFiles(main);
        assertEquals(1, ported.size());
        return PortedCodeGate
            .check(ported.get(0), main, FIXTURES.resolve(name + "/test"), text("THIRD-PARTY-NOTICES.md"), realBugs());
    }

    private static void assertRule(List<String> violations, String rule) {
        assertTrue(
            violations.stream()
                .anyMatch(v -> v.startsWith(rule + ":")),
            "rule " + rule + " should fire, got " + violations);
    }

    @Test
    void aMissingSourceRootFailsInsteadOfPassing() throws IOException {
        assertRule(PortedCodeGate.checkRoot(FIXTURES.resolve("does-not-exist"), 1), PortedCodeGate.ROOT);
    }

    @Test
    void aTooSmallSourceRootFails() throws IOException {
        assertRule(PortedCodeGate.checkRoot(FIXTURES.resolve("good/main"), 2), PortedCodeGate.ROOT);
        assertTrue(
            PortedCodeGate.checkRoot(FIXTURES.resolve("good/main"), 1)
                .isEmpty());
    }

    @Test
    void aFileNamingUpstreamWithoutTheHeaderFails() throws IOException {
        assertRule(PortedCodeGate.unmarkedMentions(FIXTURES.resolve("adapted/main")), PortedCodeGate.UNMARKED);
        assertTrue(
            PortedCodeGate.unmarkedMentions(FIXTURES.resolve("good/main"))
                .isEmpty());
    }

    @Test
    void anEmptyAvoidsLineFails() throws IOException {
        assertRule(checkFixture("emptyavoids"), PortedCodeGate.AVOIDS_EMPTY);
    }

    @Test
    void anAvoidsIdTheTestNeverNamesFails() throws IOException {
        assertRule(checkFixture("untested"), PortedCodeGate.AVOIDS_UNTESTED);
    }

    @Test
    void aTestClassWithoutATestFails() throws IOException {
        assertRule(checkFixture("notest"), PortedCodeGate.AVOIDS_UNTESTED);
    }
}
