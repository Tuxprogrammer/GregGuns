package com.gregguns.core.text;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.MetaTileEntity;

/**
 * Checks that every registered GregGuns machine carries the mod name in its description, so NEI's text search for
 * "gregguns" finds it. Machines of a module that is switched off are not registered and are skipped. Dev environment
 * only. Server side.
 */
public final class AddedByScenario implements Scenario {

    @Override
    public String name() {
        return "machine.addedby";
    }

    @Override
    public String description() {
        return "Every registered GregGuns machine's tooltip contains 'GregGuns' (NEI search by mod name)";
    }

    @Override
    public ScenarioResult run(ScenarioContext context) {
        int checked = 0;
        StringBuilder missing = new StringBuilder();
        for (int id : GregGunsMTEIDs.all()) {
            MetaTileEntity mte = GregTechAPI.METATILEENTITIES[id] instanceof MetaTileEntity
                ? (MetaTileEntity) GregTechAPI.METATILEENTITIES[id]
                : null;
            if (mte == null) continue;
            checked++;
            String[] lines = mte.getDescription();
            boolean found = false;
            for (String line : lines) {
                if (line != null && line.toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("§.", "")
                    .contains("gregguns")) {
                    found = true;
                }
            }
            if (!found) missing.append(' ')
                .append(id);
        }
        if (checked == 0) return ScenarioResult.fail(name(), "no GregGuns machine is registered");
        if (missing.length() > 0) return ScenarioResult.fail(name(), "no 'GregGuns' in the tooltip of ids:" + missing);
        return ScenarioResult.pass(name());
    }
}
