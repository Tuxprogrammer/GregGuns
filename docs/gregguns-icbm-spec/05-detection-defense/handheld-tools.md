# Handheld tools

The kept tools, per the [baseline](../../icbm-spec/05-detection-defense/handheld-tools.md).

## Radar gun (D93)

- **Kept:** yes. A laser range finder, HV item, one per stack.
- **Use:** right-click aims up to 200 blocks and stores the X and Z of the block hit, in the same tag a GT data stick carries for a target (`TargetStick`). The trace runs on the server from the player's eyes, so a client cannot send a position (fixes ICBM-05-07 and ICBM-05-12). Sneak and right-click clears it.
- **Launch Control:** right-clicking a Launch Control with a gun that holds a target copies it there (same dimension, within use distance).
- **Recipe:** Assembler, HV: sensor, emitter, Advanced circuit, 2 stainless plates.
- **Tests:** scenario `icbm.defense.radar_gun`.

The remote detonator and the defuser are written by the EMP and outputs track.
