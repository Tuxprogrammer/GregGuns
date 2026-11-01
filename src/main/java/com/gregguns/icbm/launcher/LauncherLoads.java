package com.gregguns.icbm.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every load the crafting grid knows (D150, D227, D338): one launcher of a family plus one launcher warhead of that
 * family. This is the list the NEI page shows, one page per launcher and warhead, in warhead order. Pure.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md (NEI)
public final class LauncherLoads {

    /** One load: a launcher by registry name and the warhead it takes. */
    public static final class Load {

        private final String gun;
        private final LauncherWarhead warhead;

        Load(String gun, LauncherWarhead warhead) {
            this.gun = gun;
            this.warhead = warhead;
        }

        /** @return the launcher's registry name, without the mod id */
        public String gun() {
            return gun;
        }

        /** @return the warhead it loads */
        public LauncherWarhead warhead() {
            return warhead;
        }

        @Override
        public String toString() {
            return gun + "+" + warhead.key();
        }
    }

    private static final List<Load> ALL;

    static {
        List<Load> all = new ArrayList<>();
        for (LauncherWarhead w : LauncherWarhead.values()) {
            for (String gun : w.kind()
                .guns()) {
                all.add(new Load(gun, w));
            }
        }
        ALL = Collections.unmodifiableList(all);
    }

    private LauncherLoads() {}

    /** @return every load, warhead by warhead, each warhead's launchers in family order */
    public static List<Load> all() {
        return ALL;
    }

    /**
     * @param gun     a launcher's registry name, or null for any
     * @param warhead a warhead, or null for any
     * @return the loads that match both
     */
    public static List<Load> matching(String gun, LauncherWarhead warhead) {
        List<Load> out = new ArrayList<>();
        for (Load l : ALL) {
            if ((gun == null || gun.equals(l.gun)) && (warhead == null || warhead == l.warhead)) {
                out.add(l);
            }
        }
        return out;
    }
}
