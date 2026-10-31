package com.gregguns.icbm.complex.blueprint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.blueprint.rooms.Brig;
import com.gregguns.icbm.complex.blueprint.rooms.Bunks;
import com.gregguns.icbm.complex.blueprint.rooms.Cafeteria;
import com.gregguns.icbm.complex.blueprint.rooms.CommanderOffice;
import com.gregguns.icbm.complex.blueprint.rooms.Comms;
import com.gregguns.icbm.complex.blueprint.rooms.DeconAirlock;
import com.gregguns.icbm.complex.blueprint.rooms.GuardPost;
import com.gregguns.icbm.complex.blueprint.rooms.Hub;
import com.gregguns.icbm.complex.blueprint.rooms.Infirmary;
import com.gregguns.icbm.complex.blueprint.rooms.LaunchControl;
import com.gregguns.icbm.complex.blueprint.rooms.LegacyRoom;
import com.gregguns.icbm.complex.blueprint.rooms.MessGalley;
import com.gregguns.icbm.complex.blueprint.rooms.RadarTracking;
import com.gregguns.icbm.complex.blueprint.rooms.RecLounge;
import com.gregguns.icbm.complex.blueprint.rooms.ServerRoom;
import com.gregguns.icbm.complex.blueprint.rooms.ShootingRange;
import com.gregguns.icbm.complex.blueprint.rooms.ShowersLaundry;

/**
 * The blueprint of every room kind (D305), by the kind's name, so a kind's blueprint can be registered before the
 * kind itself lands. A kind with none registered gets {@link LegacyRoom}: the machines, chests and guards it had
 * before the blueprints.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D305
public final class RoomBlueprints {

    private static final Map<String, RoomBlueprint> BY_KIND = new HashMap<String, RoomBlueprint>();

    static {
        // --- crew, command and security (C1) ----------------------------------------------------------------------
        put("ASSEMBLY_BAY", new Hub());
        put("BUNKS", new Bunks());
        put("INFIRMARY", new Infirmary());
        put("MESS_GALLEY", new MessGalley());
        put("CAFETERIA", new Cafeteria());
        put("REC_LOUNGE", new RecLounge());
        put("SHOWERS_LAUNDRY", new ShowersLaundry());
        put("GUARD_POST", new GuardPost());
        put("DECON_AIRLOCK", new DeconAirlock());
        put("BRIG", new Brig());
        put("SHOOTING_RANGE", new ShootingRange());
        put("LAUNCH_CONTROL", new LaunchControl());
        put("RADAR_TRACKING", new RadarTracking());
        put("COMMS", new Comms());
        put("COMMANDER_OFFICE", new CommanderOffice());
        put("SERVER_ROOM", new ServerRoom());
        // --- works, stores and food (C2): register each kind's class here ----------------------------------------
    }

    private RoomBlueprints() {}

    private static void put(String kind, RoomBlueprint b) {
        if (BY_KIND.put(kind, b) != null) throw new IllegalStateException("two blueprints for " + kind);
    }

    /** @return the kind's blueprint: its own, or the stand-in that keeps its earlier machines and chests */
    public static RoomBlueprint of(RoomKind kind) {
        RoomBlueprint b = BY_KIND.get(kind.name());
        return b != null ? b : new LegacyRoom(kind);
    }

    /** @return whether the kind has its own blueprint */
    public static boolean has(RoomKind kind) {
        return BY_KIND.containsKey(kind.name());
    }

    /** @return the registered kind names and their blueprints */
    public static Map<String, RoomBlueprint> all() {
        return Collections.unmodifiableMap(BY_KIND);
    }
}
