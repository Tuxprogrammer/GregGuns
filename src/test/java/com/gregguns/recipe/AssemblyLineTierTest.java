package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.icbm.production.MissileBuildSpec;

/**
 * GT's Assembly Line, with its scanner research, is a LuV machine in GTNH (pack quest 1412), so no GregGuns recipe
 * below LuV may be on it (D281, the owner's 0.4.0 alpha report). Covers every table row that targets the line and the
 * missile bodies. The live maps are checked by the scenario {@code icbm.production.recipes}.
 */
class AssemblyLineTierTest {

    private static final int LUV = Tier.LUV.voltageIndex();

    @Test
    void everyAssemblyLineTableRowIsLuvOrAbove() {
        List<ProductionTable.Row> rows = new ArrayList<>();
        rows.addAll(ComponentTable.rows());
        rows.addAll(AmmoLineTable.rows());
        rows.addAll(GunLineTable.rows());
        rows.addAll(ProductionTable.ROWS);
        rows.addAll(GunTable.ROWS);
        int lines = 0;
        for (ProductionTable.Row row : rows) {
            if (row.target() == ProductionTable.Target.ASSEMBLY_LINE) {
                lines++;
                assertTrue(
                    row.tier()
                        .voltageIndex() >= LUV,
                    row.id() + " is an Assembly Line row below LuV");
            }
        }
        assertTrue(lines > 0, "no Assembly Line rows found: the check would pass vacuously");
    }

    @Test
    void noMissileBodyBelowLuvIsOnTheAssemblyLine() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            if (s.tier()
                .voltageIndex() < LUV) {
                assertFalse(s.onAssemblyLine(), s + " body is on the Assembly Line below LuV");
            }
        }
    }
}
