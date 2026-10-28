package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.production.MissileBuildSpec;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.tools.ToolRecipeSpec;
import com.gregguns.icbm.warhead.WarheadRecipeSpec;
import com.gregguns.utility.TurretPlate;
import com.gregguns.utility.rampart.BastionPlating;

/**
 * GT's programmed circuit ({@code ItemIntegratedCircuit.MAX_CIRCUIT_NUMBER}) only goes from 1 to 24, so a recipe on
 * any other number cannot be fed in survival. This is the global check: every table row and spec enum, and every
 * literal in a {@code .circuit(n)} call or a circuit constant in the main sources, must fall in that range.
 */
class CircuitRangeTest {

    private static final int MAX = 24;
    private static final Pattern CALL = Pattern.compile("\\.circuit\\(\\s*(\\d+)\\s*\\)");
    private static final Pattern CONSTANT = Pattern.compile("[A-Z_]*CIRCUIT[A-Z_]*\\s*=\\s*(\\d+)\\s*;");
    private static final Pattern CASING = Pattern.compile("(?:WORKS_CASING|GANTRY_CASING)\\s*=\\s*(\\d+)");

    private static void check(List<String> bad, String what, int circuit) {
        if (circuit < 1 || circuit > MAX) {
            bad.add(what + " = " + circuit);
        }
    }

    @Test
    void tableRowsAndSpecsStayInRange() {
        List<String> bad = new ArrayList<>();
        List<ProductionTable.Row> rows = new ArrayList<>();
        rows.addAll(ProductionTable.ROWS);
        rows.addAll(GunTable.ROWS);
        rows.addAll(AmmoLineTable.rows());
        rows.addAll(ComponentTable.rows());
        rows.addAll(EquipmentTable.ROWS);
        rows.addAll(FlightTable.ROWS);
        rows.addAll(GrenadeTable.ROWS);
        rows.addAll(MeleeTable.ROWS);
        rows.addAll(ArmorTable.COMBAT);
        rows.addAll(ArmorTable.T3_COMBAT);
        rows.addAll(ArmorTable.T3_HEV);
        rows.addAll(GunLineTable.rows());
        for (ProductionTable.Row row : rows) {
            if (row.circuit() > 0) {
                check(bad, row.id(), row.circuit());
            }
        }
        for (WarheadRecipeSpec s : WarheadRecipeSpec.values()) {
            check(bad, "warhead " + s, s.circuit());
        }
        for (IcbmPart p : IcbmPart.values()) {
            check(bad, "part " + p, p.circuit());
        }
        for (ToolRecipeSpec s : ToolRecipeSpec.values()) {
            check(bad, "tool " + s, s.circuit());
        }
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            check(bad, "missile fit " + s, s.fitCircuit());
        }
        for (TurretPlate p : TurretPlate.values()) {
            check(bad, "turret plate " + p, p.circuit());
        }
        for (BastionPlating p : BastionPlating.values()) {
            check(bad, "bastion plating " + p, p.circuit());
        }
        assertTrue(bad.isEmpty(), "circuits outside 1 to " + MAX + ": " + bad);
    }

    @Test
    void sourceLiteralsStayInRange() throws IOException {
        List<String> bad = new ArrayList<>();
        int seen = 0;
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java"))) {
            for (Path p : (Iterable<Path>) files.filter(
                f -> f.toString()
                    .endsWith(".java"))::iterator) {
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                for (Pattern pattern : new Pattern[] { CALL, CONSTANT, CASING }) {
                    Matcher m = pattern.matcher(text);
                    while (m.find()) {
                        seen++;
                        check(bad, p.getFileName() + ": " + m.group(), Integer.parseInt(m.group(1)));
                    }
                }
            }
        }
        assertTrue(seen > 10, "the scan found only " + seen + " circuit literals; the path or patterns are stale");
        assertTrue(bad.isEmpty(), "circuits outside 1 to " + MAX + ": " + bad);
    }
}
