package com.gregguns.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gregguns.icbm.blast.DamageRamp;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.warhead.WarheadBlast;
import com.gregguns.icbm.warhead.WarheadBlasts;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadFuse;
import com.gregguns.icbm.warhead.WarheadMath;
import com.gregguns.icbm.warhead.WarheadRecipeSpec;
import com.gregguns.icbm.warhead.effects.Effects;

/**
 * The warhead dump for the {@code icbm-warheads} infographic ({@code tools/asset-gen/infographics}): every blast's
 * radius and center damage at every fill, read from the same {@link ExplosiveDef}s the engine runs, plus the casings,
 * the machine that makes each warhead and the effect constants. The sheet prints this instead of a copy that can drift.
 * Test side only; no game needed. Writes the real file only when {@code GREGGUNS_EXPORT_WARHEADS} names one.
 */
class WarheadExportTest {

    /** The dump lists every blast with a curve of the right length. */
    @Test
    void dumpCarriesEveryBlast(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("warheads.json");
        export(file);
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        for (WarheadBlast b : WarheadBlast.values()) {
            assertTrue(json.contains("\"id\": \"" + b.id() + "\""), b.id());
        }
        assertEquals(WarheadBlast.values().length, json.split("\"curve\"", -1).length - 1);
    }

    /** Writes the dump for the infographics build when asked to. */
    @Test
    void exportWhenAsked() throws IOException {
        String target = System.getenv("GREGGUNS_EXPORT_WARHEADS");
        if (target == null || target.isEmpty()) {
            return;
        }
        export(Paths.get(target));
    }

    private static String num(double v) {
        return String.format(Locale.ROOT, "%.6f", v);
    }

    static void export(Path file) throws IOException {
        StringBuilder sb = new StringBuilder("{\n");
        sb.append("  \"casings\": [\n");
        WarheadCasing[] casings = WarheadCasing.values();
        for (int i = 0; i < casings.length; i++) {
            sb.append("    {\"id\": \"")
                .append(casings[i].blockName())
                .append("\", \"tier\": \"")
                .append(casings[i].tier())
                .append("\", \"cap\": ")
                .append(casings[i].fillCap())
                .append(", \"height\": ")
                .append(casings[i].heightBlocks())
                .append(i + 1 < casings.length ? "},\n" : "}\n");
        }
        sb.append("  ],\n  \"fuse\": {\"chainTicks\": ")
            .append(WarheadFuse.CHAIN_TICKS)
            .append("},\n  \"effects\": {\"gasTicks\": ")
            .append(Effects.GAS_TICKS)
            .append(", \"breachDepth\": ")
            .append(Effects.BREACH_DEPTH)
            .append(", \"breachMaxHardness\": ")
            .append(num(Effects.BREACH_MAX_HARDNESS))
            .append(", \"fireMaxBlocks\": ")
            .append(Effects.FIRE_MAX_BLOCKS)
            .append("},\n  \"minRadius\": ")
            .append(num(WarheadMath.MIN_RADIUS))
            .append(",\n  \"coreShare\": ")
            .append(num(DamageRamp.CORE_SHARE))
            .append(",\n  \"blasts\": [\n");
        WarheadBlast[] blasts = WarheadBlast.values();
        for (int i = 0; i < blasts.length; i++) {
            WarheadBlast b = blasts[i];
            ExplosiveDef def = WarheadBlasts.define(b);
            WarheadRecipeSpec spec = null;
            for (WarheadRecipeSpec s : WarheadRecipeSpec.values()) {
                if (s.blast() == b) {
                    spec = s;
                }
            }
            sb.append("    {\"id\": \"")
                .append(b.id())
                .append("\", \"tier\": \"")
                .append(b.tier())
                .append("\", \"shape\": \"")
                .append(b.shape())
                .append("\", \"kind\": \"")
                .append(b.kind())
                .append("\", \"cap\": ")
                .append(b.fillCap())
                .append(", \"machine\": \"")
                .append(spec == null ? "" : spec.machine())
                .append("\", \"machineTier\": \"")
                .append(spec == null ? "" : spec.machineTier())
                .append("\", \"curve\": {");
            for (int f = 1; f <= b.fillCap(); f++) {
                double r = Math.max(WarheadMath.MIN_RADIUS, def.radius(f));
                sb.append(f > 1 ? ", " : "")
                    .append('"')
                    .append(f)
                    .append("\": [")
                    .append(num(r))
                    .append(", ")
                    .append(num(def.damage(f)))
                    .append(']');
            }
            sb.append(i + 1 < blasts.length ? "}},\n" : "}}\n");
        }
        sb.append("  ]\n}\n");
        Files.write(
            file,
            sb.toString()
                .getBytes(StandardCharsets.UTF_8));
    }
}
