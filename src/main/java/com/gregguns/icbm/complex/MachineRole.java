package com.gregguns.icbm.complex;

/** The roles of the machine marks (D129); the filler picks the concrete machine within the dimension's ceiling. */
public enum MachineRole {
    GENERATOR,
    NASA_WORKBENCH,
    LAUNCH_CONTROL,
    BATTERY_BUFFER,
    PUMP,
    AIR_HANDLER,
    FUEL_TANK,
    WORKBENCH,
    RADAR,
    COMMS,
    MAGAZINE_CASING,
    /** A GT single-block Chemical Reactor (chem lab, D307). */
    CHEM_REACTOR,
    /** A lab machine: centrifuge, electrolyzer, mixer, distillery, chemical bath or brewery. */
    LAB,
    /** A recycler, electric furnace or arc furnace (incinerator). */
    RECYCLER,
    /** A machine tool: lathe, bender, forming press, wiremill or cutter (workshop). */
    MACHINE_TOOL
}
