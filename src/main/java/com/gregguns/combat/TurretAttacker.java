package com.gregguns.combat;

/**
 * Marker for turret entities (M6). A shooter that implements it gets the turret damage factor against players instead
 * of the NPC factor ({@link DamageFactors}). Nothing implements it yet.
 */
// Spec: docs/techguns-spec/02-guns/damage-system.md section 5.5
public interface TurretAttacker {
}
