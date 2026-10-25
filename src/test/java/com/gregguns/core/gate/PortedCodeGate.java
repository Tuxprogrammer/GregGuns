package com.gregguns.core.gate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The checks of the ported-code gate (CONTRIBUTING § Ported code (MIT)), as plain functions so the gate test can run
 * them on the real sources and on good and bad fixtures alike.
 *
 * <p>
 * A file is "ported" when a comment line carries {@code Ported from ICBM-Classic (MIT): <permalink>}. For each ported
 * file the gate reports a violation when the permalink is not pinned to commit {@code 9c1d985a}, when the notices file
 * has no ICBM-Classic entry, when the top-level type has no Javadoc, when no {@code <Name>Test} class exists in the
 * same package, or when an {@code Avoids: ICBM-xx-nn} ID is missing from the bug register, empty, or never named in
 * the test class (which must also hold a {@code @Test}).
 *
 * <p>
 * The gate must not pass vacuously, so {@link #checkRoot} fails a missing or near-empty source root, and
 * {@link #unmarkedMentions} fails any file that names the upstream project without the header. Each violation string
 * starts with a rule tag ({@link #PERMALINK} and so on) so tests can assert which rule fired.
 */
public final class PortedCodeGate {

    /** Rule tag: the header permalink is missing or not pinned to the allowed commit. */
    public static final String PERMALINK = "permalink";
    /** Rule tag: the notices file lacks the ICBM-Classic entry. */
    public static final String NOTICE = "notice";
    /** Rule tag: the top-level type has no Javadoc. */
    public static final String JAVADOC = "javadoc";
    /** Rule tag: no {@code <Name>Test} class exists. */
    public static final String TEST_CLASS = "test-class";
    /** Rule tag: an {@code Avoids:} ID is not in the bug register. */
    public static final String BUG_ID = "bug-id";
    /** Rule tag: an {@code Avoids:} line names no ID. */
    public static final String AVOIDS_EMPTY = "avoids-empty";
    /** Rule tag: the test class has no {@code @Test}, or never names an {@code Avoids:} ID. */
    public static final String AVOIDS_UNTESTED = "avoids-untested";
    /** Rule tag: the source root is missing or holds too few Java files to be the real tree. */
    public static final String ROOT = "root";
    /** Rule tag: a file names the upstream project but carries no ported-code header. */
    public static final String UNMARKED = "unmarked";

    /** The commit every port is pinned to: the short form is accepted, and so is exactly this full hash. */
    static final String SHORT_COMMIT = "9c1d985a";
    static final String FULL_COMMIT = "9c1d985a375adc2868ff47707b3a636452d0b63a";

    private static final Pattern HEADER = Pattern
        .compile("^\\s*(?://|\\*|/\\*\\*?)\\s*Ported from ICBM-Classic \\(MIT\\):(.*)$", Pattern.MULTILINE);
    private static final Pattern PERMALINK_URL = Pattern
        .compile("https://github\\.com/BuiltBrokenModding/ICBM-Classic/blob/([0-9a-fA-F]+)/\\S+");
    private static final Pattern AVOIDS = Pattern.compile("^\\s*(?://|\\*)\\s*Avoids:(.*)$", Pattern.MULTILINE);
    private static final Pattern UPSTREAM = Pattern.compile("ICBM-Classic|BuiltBrokenModding|icbm\\.classic");
    private static final Pattern BUG_TOKEN = Pattern.compile("ICBM-[0-9A-Za-z]+-[0-9]+");
    private static final Pattern BUG_ROW = Pattern
        .compile("^\\|\\s*(ICBM-[0-9A-Za-z]+-[0-9]+)\\s*\\|", Pattern.MULTILINE);
    private static final Pattern TYPE_DECL = Pattern
        .compile("^(?:public |final |abstract |sealed |strictfp )*(?:class|interface|enum|@interface)\\s");

    private PortedCodeGate() {}

    /**
     * @param source the text of a Java file
     * @return whether it carries a ported-code header
     */
    public static boolean isPorted(String source) {
        return HEADER.matcher(source)
            .find();
    }

    /**
     * Finds the ported files under a source root.
     *
     * @param mainRoot a source root such as {@code src/main/java}
     * @return the ported files, sorted by path
     * @throws IOException if the tree cannot be read
     */
    public static List<Path> portedFiles(Path mainRoot) throws IOException {
        if (!Files.isDirectory(mainRoot)) {
            return new ArrayList<>();
        }
        try (Stream<Path> walk = Files.walk(mainRoot)) {
            List<Path> result = new ArrayList<>();
            for (Path p : walk.filter(
                f -> f.toString()
                    .endsWith(".java"))
                .sorted()
                .collect(Collectors.toList())) {
                if (isPorted(read(p))) {
                    result.add(p);
                }
            }
            return result;
        }
    }

    /**
     * Guards the scan against passing because it looked at nothing.
     *
     * @param root     a source root such as {@code src/main/java}
     * @param minFiles the fewest {@code .java} files a real tree holds
     * @return a {@link #ROOT} violation, or an empty list
     * @throws IOException if the tree cannot be read
     */
    public static List<String> checkRoot(Path root, int minFiles) throws IOException {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            out.add(ROOT + ": " + root + " is not a directory, so nothing was scanned");
            return out;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            long count = walk.filter(
                f -> f.toString()
                    .endsWith(".java"))
                .count();
            if (count < minFiles) {
                out.add(ROOT + ": " + root + " holds " + count + " Java files, expected at least " + minFiles);
            }
        }
        return out;
    }

    /**
     * Finds files that mention the upstream project without the ported-code header, such as "Adapted from
     * ICBM-Classic", which would otherwise skip every rule.
     *
     * @param mainRoot a source root
     * @return one {@link #UNMARKED} violation per such file
     * @throws IOException if the tree cannot be read
     */
    public static List<String> unmarkedMentions(Path mainRoot) throws IOException {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(mainRoot)) {
            return out;
        }
        try (Stream<Path> walk = Files.walk(mainRoot)) {
            for (Path p : walk.filter(
                f -> f.toString()
                    .endsWith(".java"))
                .sorted()
                .collect(Collectors.toList())) {
                String source = read(p);
                if (!isPorted(source) && UPSTREAM.matcher(source)
                    .find()) {
                    out.add(UNMARKED + ": " + p + " names ICBM-Classic but has no 'Ported from' header");
                }
            }
        }
        return out;
    }

    /**
     * @param bugsMarkdown the text of {@code docs/icbm-spec/BUGS.md}
     * @return every bug ID that has a table row
     */
    public static Set<String> bugIds(String bugsMarkdown) {
        Set<String> ids = new HashSet<>();
        Matcher m = BUG_ROW.matcher(bugsMarkdown);
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }

    /**
     * @param notices the text of {@code THIRD-PARTY-NOTICES.md}
     * @return whether it has the ICBM-Classic entry: the heading, the pinned commit and the MIT copyright line
     */
    public static boolean hasIcbmNotice(String notices) {
        return notices.contains("## ICBM-Classic (MIT)") && notices.contains(FULL_COMMIT)
            && notices.contains("Copyright (c) 2023 Built Broken Modding")
            && notices.contains("Permission is hereby granted, free of charge");
    }

    /**
     * Checks one ported file.
     *
     * @param file     the Java file
     * @param mainRoot the source root it lives under (gives the package)
     * @param testRoot the test source root searched for {@code <Name>Test}
     * @param notices  the text of the notices file
     * @param bugIds   the IDs in the bug register
     * @return the violations, each starting with a rule tag; empty when the file passes
     * @throws IOException if a file cannot be read
     */
    public static List<String> check(Path file, Path mainRoot, Path testRoot, String notices, Set<String> bugIds)
        throws IOException {
        String source = read(file);
        List<String> out = new ArrayList<>();
        out.addAll(checkPermalink(source));
        if (!hasIcbmNotice(notices)) {
            out.add(NOTICE + ": THIRD-PARTY-NOTICES.md has no complete '## ICBM-Classic (MIT)' entry");
        }
        if (!hasTypeJavadoc(source)) {
            out.add(JAVADOC + ": the top-level type has no Javadoc");
        }
        Path relative = mainRoot.relativize(file);
        Path test = testRoot.resolve(
            relative.toString()
                .replaceAll("\\.java$", "Test.java"));
        if (!Files.isRegularFile(test)) {
            out.add(TEST_CLASS + ": missing " + testRoot.relativize(test));
        }
        String testSource = Files.isRegularFile(test) ? read(test) : "";
        if (Files.isRegularFile(test) && !testSource.contains("@Test")) {
            out.add(AVOIDS_UNTESTED + ": " + testRoot.relativize(test) + " has no @Test");
        }
        Matcher avoids = AVOIDS.matcher(source);
        while (avoids.find()) {
            Matcher token = BUG_TOKEN.matcher(avoids.group(1));
            boolean any = false;
            while (token.find()) {
                any = true;
                if (!bugIds.contains(token.group())) {
                    out.add(BUG_ID + ": " + token.group() + " is not in docs/icbm-spec/BUGS.md");
                }
                if (!testSource.contains(token.group())) {
                    out.add(AVOIDS_UNTESTED + ": " + token.group() + " is not named in " + test.getFileName());
                }
            }
            if (!any) {
                out.add(AVOIDS_EMPTY + ": an Avoids: line names no ICBM-xx-nn ID");
            }
        }
        return out;
    }

    private static List<String> checkPermalink(String source) {
        List<String> out = new ArrayList<>();
        Matcher header = HEADER.matcher(source);
        while (header.find()) {
            Matcher url = PERMALINK_URL.matcher(header.group(1));
            if (!url.find()) {
                out.add(PERMALINK + ": header has no github.com/BuiltBrokenModding/ICBM-Classic/blob/<commit>/ link");
            } else {
                String commit = url.group(1)
                    .toLowerCase();
                if (!commit.equals(SHORT_COMMIT) && !commit.equals(FULL_COMMIT)) {
                    out.add(PERMALINK + ": pinned to " + commit + ", not " + SHORT_COMMIT);
                }
            }
        }
        return out;
    }

    /**
     * @param source the text of a Java file
     * @return whether the first top-level type declaration is preceded by a {@code /** ... *}{@code /} comment, with
     *         only
     *         annotations, blank lines and {@code //} lines in between
     */
    static boolean hasTypeJavadoc(String source) {
        String[] lines = source.split("\r?\n", -1);
        int decl = -1;
        for (int i = 0; i < lines.length; i++) {
            if (TYPE_DECL.matcher(lines[i])
                .find()) {
                decl = i;
                break;
            }
        }
        if (decl < 0) {
            return false;
        }
        for (int i = decl - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("@")) {
                continue;
            }
            if (!line.endsWith("*/")) {
                return false;
            }
            for (int j = i; j >= 0; j--) {
                String start = lines[j].trim();
                if (start.startsWith("/**")) {
                    return true;
                }
                if (start.startsWith("/*")) {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
