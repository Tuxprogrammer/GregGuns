package com.gregguns.icbm.complex.blueprint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.blueprint.rooms.AirHandling;
import com.gregguns.icbm.complex.blueprint.rooms.Armory;
import com.gregguns.icbm.complex.blueprint.rooms.Brig;
import com.gregguns.icbm.complex.blueprint.rooms.Bunks;
import com.gregguns.icbm.complex.blueprint.rooms.Cafeteria;
import com.gregguns.icbm.complex.blueprint.rooms.ChemLab;
import com.gregguns.icbm.complex.blueprint.rooms.ColdStorage;
import com.gregguns.icbm.complex.blueprint.rooms.CommanderOffice;
import com.gregguns.icbm.complex.blueprint.rooms.Comms;
import com.gregguns.icbm.complex.blueprint.rooms.DeconAirlock;
import com.gregguns.icbm.complex.blueprint.rooms.FuelDepot;
import com.gregguns.icbm.complex.blueprint.rooms.GeneratorHall;
import com.gregguns.icbm.complex.blueprint.rooms.GuardPost;
import com.gregguns.icbm.complex.blueprint.rooms.Hub;
import com.gregguns.icbm.complex.blueprint.rooms.Incinerator;
import com.gregguns.icbm.complex.blueprint.rooms.Infirmary;
import com.gregguns.icbm.complex.blueprint.rooms.LaunchControl;
import com.gregguns.icbm.complex.blueprint.rooms.LegacyRoom;
import com.gregguns.icbm.complex.blueprint.rooms.MessGalley;
import com.gregguns.icbm.complex.blueprint.rooms.MissileAssembly;
import com.gregguns.icbm.complex.blueprint.rooms.MushroomCellar;
import com.gregguns.icbm.complex.blueprint.rooms.PumpRoom;
import com.gregguns.icbm.complex.blueprint.rooms.RadarTracking;
import com.gregguns.icbm.complex.blueprint.rooms.RecLounge;
import com.gregguns.icbm.complex.blueprint.rooms.ServerRoom;
import com.gregguns.icbm.complex.blueprint.rooms.ShootingRange;
import com.gregguns.icbm.complex.blueprint.rooms.ShowersLaundry;
import com.gregguns.icbm.complex.blueprint.rooms.Stockpile;
import com.gregguns.icbm.complex.blueprint.rooms.UndergroundFarm;
import com.gregguns.icbm.complex.blueprint.rooms.VehicleBay;
import com.gregguns.icbm.complex.blueprint.rooms.WarheadMagazine;
import com.gregguns.icbm.complex.blueprint.rooms.WaterTreatment;
import com.gregguns.icbm.complex.blueprint.rooms.Workshop;

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
        put("GENERATOR_HALL", new GeneratorHall());
        put("AIR_HANDLING", new AirHandling());
        put("PUMP_ROOM", new PumpRoom());
        put("WATER_TREATMENT", new WaterTreatment());
        put("WORKSHOP", new Workshop());
        put("FUEL_DEPOT", new FuelDepot());
        put("VEHICLE_BAY", new VehicleBay());
        put("MISSILE_ASSEMBLY", new MissileAssembly());
        put("CHEM_LAB", new ChemLab());
        put("INCINERATOR", new Incinerator());
        put("ARMORY", new Armory());
        put("WARHEAD_MAGAZINE", new WarheadMagazine());
        put("STOCKPILE", new Stockpile());
        put("COLD_STORAGE", new ColdStorage());
        put("UNDERGROUND_FARM", new UndergroundFarm());
        put("MUSHROOM_CELLAR", new MushroomCellar());
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
