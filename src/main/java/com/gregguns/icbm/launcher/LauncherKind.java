package com.gregguns.icbm.launcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The launcher families a warhead module fits (D150). The names are the registry names of the guns, so the table
 * needs no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
public enum LauncherKind {

    /** The Rocket Launcher and the Grim Reaper, which fires the same rocket. */
    ROCKET("rocket_launcher", "grim_reaper"),
    /** The 40 mm Grenade Launcher. */
    GRENADE("grenade_launcher"),
    /** The Nuke Launcher. */
    NUKE("nuke_launcher");

    private final List<String> guns;

    LauncherKind(String... guns) {
        this.guns = Collections.unmodifiableList(Arrays.asList(guns));
    }

    /** @return the registry names of the guns of this family */
    public List<String> guns() {
        return guns;
    }

    /**
     * @param gunName a gun's registry name
     * @return whether this family includes it
     */
    public boolean fits(String gunName) {
        return guns.contains(gunName);
    }
}
