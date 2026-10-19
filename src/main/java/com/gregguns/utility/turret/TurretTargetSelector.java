package com.gregguns.utility.turret;

import java.util.UUID;

import com.gregguns.utility.security.FriendProvider;
import com.gregguns.utility.security.OwnerAccess;
import com.gregguns.utility.security.SecurityLevel;

/**
 * Decides whether a Turret shoots a candidate. Pure logic: no Minecraft types, so every row of the rules table is unit
 * tested with stubs. The entity adapts a living entity to {@link Candidate}.
 *
 * <p>
 * Rules, in order: (1) any hostile mob is a target; (2) with an owner and a security level above public, another
 * Turret is a target unless the two owners are friends, and a player is a target unless they are the owner or a
 * friend, and the decision ends there; (3) with Target Animals on, a passive creature is a target unless it is tamed
 * or owned; (4) anything else is not. A Turret with no base ({@code settings == null}) shoots hostile mobs only.
 */
// Spec: docs/techguns-spec/09-machines-utility/turret.md (Targeting rules), docs/gregguns-spec/09-machines-utility/
// turret.md (GGS-09-03, GGS-09-04), docs/techguns-spec/10-entities-npcs/ai.md (TurretTargetSelector)
// Deviation: friends come from ServerUtilities, and with no provider only the owner is a friend; the Techguns dummy
// provider made every pair friends, so its turrets never shot players (NPC-13).
public final class TurretTargetSelector {

    private TurretTargetSelector() {}

    /** What the selector needs to know about a candidate. */
    public interface Candidate {

        /** @return true for a hostile mob ({@code IMob}) */
        boolean isHostileMob();

        /** @return true for a Turret entity */
        boolean isTurret();

        /** @return for a Turret candidate, the owner of its base; null when unlinked or ownerless */
        UUID turretOwner();

        /** @return true for a player who may be shot (not creative and not spectating) */
        boolean isShootablePlayer();

        /** @return for a player, their UUID */
        UUID playerId();

        /** @return true for a passive creature ({@code IAnimals}) that is not a Turret and not hostile */
        boolean isPassiveAnimal();

        /** @return true for a tamed pet, tamed horse or anything with an owner */
        boolean isTamedOrOwned();
    }

    /** The base's settings that the rules read. */
    public static final class Settings {

        private final SecurityLevel level;
        private final UUID owner;
        private final boolean attackAnimals;
        private final FriendProvider friends;

        /**
         * @param level         the base's security level; null means public
         * @param owner         the base owner's UUID; null for none
         * @param attackAnimals the Target Animals toggle
         * @param friends       who counts as a friend; null means nobody but the owner
         */
        public Settings(SecurityLevel level, UUID owner, boolean attackAnimals, FriendProvider friends) {
            this.level = level == null ? SecurityLevel.PUBLIC : level;
            this.owner = owner;
            this.attackAnimals = attackAnimals;
            this.friends = friends;
        }

        /** @return the base owner's UUID; null for none */
        public UUID owner() {
            return owner;
        }
    }

    /**
     * @param settings the base's settings; null when the Turret has no base
     * @param c        the candidate; null is never a target
     * @return true when the Turret should shoot the candidate
     */
    public static boolean isTarget(Settings settings, Candidate c) {
        if (c == null) {
            return false;
        }
        if (c.isHostileMob()) {
            return true;
        }
        if (settings == null) {
            return false;
        }
        if (settings.owner != null && settings.level != SecurityLevel.PUBLIC) {
            if (c.isTurret()) {
                UUID other = c.turretOwner();
                if (other != null) {
                    return !OwnerAccess.isFriend(settings.owner, other, settings.friends);
                }
            } else if (c.isShootablePlayer()) {
                return OwnerAccess.isPlayerTarget(settings.level, settings.owner, c.playerId(), settings.friends);
            }
        }
        if (settings.attackAnimals && c.isPassiveAnimal() && !c.isTurret()) {
            return !c.isTamedOrOwned();
        }
        return false;
    }
}
