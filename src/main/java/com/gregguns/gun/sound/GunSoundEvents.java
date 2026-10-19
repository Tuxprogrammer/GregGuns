package com.gregguns.gun.sound;

import java.util.regex.Pattern;

import com.gregguns.core.registry.Naming;

/**
 * Sound event names for guns, projectiles and effects, and the convention that M2 follows to add each gun's sounds.
 * Pure string logic with no Minecraft types, both sides.
 *
 * <h2>Convention</h2>
 * <ul>
 * <li>Gun events are {@code guns.<weapon>.<action>}, where {@code <weapon>} is the gun's content name (for example
 * {@code pistol_9mm}) and {@code <action>} is one of {@link #FIRE}, {@link #RELOAD}, {@link #DRY_FIRE}, or a
 * weapon-specific action such as {@code rechamber}. Every weapon owns its fire event so a player can mute one gun with
 * Extreme Sound Muffler (GGS-03-09).</li>
 * <li>Other events are {@code effects.<name>}.</li>
 * <li>Variants are separate {@code .ogg} files under one event, named {@code <weapon>_<action>_a}, {@code _b},
 * {@code _c} (Minecraft picks one at random). The files live in {@code sounds/guns/} or {@code sounds/effects/} and are
 * listed in {@code assets/gregguns/sounds.json} with category {@code player}.</li>
 * <li>Names must not contain a muffler-blacklist entry ({@code ui.}, {@code music.}, {@code ambient.}), and file paths
 * must not contain a Hodgepodge downmix exclusion ({@code button}, {@code click}, {@code /gui}, {@code menu},
 * {@code typing}, {@code page}). {@code GunSoundsJsonTest} enforces both, plus mono files and existence, so adding an
 * M2 row is: drop the files, add the {@code sounds.json} entry, add a constant here if code needs it.</li>
 * </ul>
 *
 * <p>
 * Use {@link #qualified(String)} to turn an event into the {@code gregguns:}-prefixed string that the sound system
 * takes.
 */
// Spec: docs/gregguns-spec/03-gun-visuals-audio/gun-sound-pipeline.md (GGS-03-08, GGS-03-09, GGS-03-11)
public final class GunSoundEvents {

    private static final Pattern PART = Pattern.compile("[a-z][a-z0-9_]*");

    /** Action: one shot. */
    public static final String FIRE = "fire";

    /** Action: the first shot after a pause of a continuous-fire gun (the Flamethrower). */
    public static final String START = "start";
    /** Suffix of the sound a charge gun plays when the charge begins: {@code guns.<name>.charge}. */
    public static final String CHARGE = "charge";

    /** Action: reload start. */
    public static final String RELOAD = "reload";

    /** Action: trigger pulled on an empty magazine. */
    public static final String DRY_FIRE = "dry_fire";

    /** The 9mm Pistol's content name. */
    public static final String PISTOL_9MM = "pistol_9mm";

    /** Event: the 9mm Pistol's shot, three variants. */
    public static final String PISTOL_9MM_FIRE = gun(PISTOL_9MM, FIRE);

    /** Event: the 9mm Pistol's reload. */
    public static final String PISTOL_9MM_RELOAD = gun(PISTOL_9MM, RELOAD);

    /** The Revolver's content name. */
    public static final String REVOLVER = "revolver";

    /** Event: the Revolver's shot, three variants. */
    public static final String REVOLVER_FIRE = gun(REVOLVER, FIRE);

    /** Event: the Revolver's reload. */
    public static final String REVOLVER_RELOAD = gun(REVOLVER, RELOAD);

    /** The Sawed Off's content name. */
    public static final String SAWED_OFF = "sawed_off";

    /** Event: the Sawed Off's shot, three variants. */
    public static final String SAWED_OFF_FIRE = gun(SAWED_OFF, FIRE);

    /** Event: the Sawed Off's reload. */
    public static final String SAWED_OFF_RELOAD = gun(SAWED_OFF, RELOAD);

    /**
     * The Hand Cannon's content name. Its sound files are the {@code handgun_*} set (Techguns' event name is
     * {@code handgunFire}).
     */
    public static final String HANDCANNON = "handcannon";

    /** Event: the Hand Cannon's shot, three variants. */
    public static final String HANDCANNON_FIRE = gun(HANDCANNON, FIRE);

    /** Event: the Hand Cannon's reload. */
    public static final String HANDCANNON_RELOAD = gun(HANDCANNON, RELOAD);

    /** Action: the bolt worked after a shot (a gun that plays a second sound with every shot). */
    public static final String RECHAMBER = "rechamber";

    /** The Bolt Action Rifle's content name. */
    public static final String BOLT_ACTION_RIFLE = "bolt_action_rifle";

    /** Event: the Bolt Action Rifle's shot, three variants. */
    public static final String BOLT_ACTION_RIFLE_FIRE = gun(BOLT_ACTION_RIFLE, FIRE);

    /** Event: the Bolt Action Rifle's reload. */
    public static final String BOLT_ACTION_RIFLE_RELOAD = gun(BOLT_ACTION_RIFLE, RELOAD);

    /** Event: the Bolt Action Rifle's bolt, played with every shot. */
    public static final String BOLT_ACTION_RIFLE_RECHAMBER = gun(BOLT_ACTION_RIFLE, RECHAMBER);

    /** Event: a bullet hitting a block or entity, five variants. */
    public static final String BULLET_IMPACT = effect("bullet_impact");

    /** Event: an explosion, five variants. */
    public static final String EXPLOSION = effect("explosion1");

    /**
     * Vanilla sound played when a gun is pulled on an empty magazine and has no registered {@code dry_fire} event.
     */
    // Deviation: Techguns plays mob.villager.idle (sounds.md, "Empty with no ammo"). A villager grunt is odd for a
    // trigger pull, and no dry-fire asset exists yet, so M1 uses the vanilla click. This is a vanilla name, not a
    // gregguns path, so the Hodgepodge "click" exclusion does not apply.
    public static final String VANILLA_DRY_FIRE = "random.click";

    /** Substrings that Extreme Sound Muffler refuses to mute, from the pack's {@code forbiddenSounds}. */
    static final String[] MUFFLER_BLACKLIST = { "ui.", "music.", "ambient." };

    private GunSoundEvents() {}

    /**
     * @param weapon the gun's content name, for example {@code pistol_9mm}
     * @param action the action, for example {@link #FIRE}
     * @return the event name {@code guns.<weapon>.<action>}
     * @throws IllegalArgumentException if a part is not lower snake case
     */
    public static String gun(String weapon, String action) {
        return "guns." + part(weapon) + "." + part(action);
    }

    /**
     * @param name the effect's name, for example {@code bullet_impact}
     * @return the event name {@code effects.<name>}
     * @throws IllegalArgumentException if the name is not lower snake case
     */
    public static String effect(String name) {
        return "effects." + part(name);
    }

    /**
     * @param event an event name from this class
     * @return the {@code gregguns:}-prefixed name to pass to the sound system
     */
    public static String qualified(String event) {
        return Naming.soundEvent(event);
    }

    /**
     * @param event an event name
     * @return the first muffler-blacklist entry the full name contains (so it could never be muted), or {@code null}
     */
    public static String mufflerBlacklistHit(String event) {
        // Source:
        // https://github.com/Lyfts/ExtremeSoundMuffler-Legacy/blob/1.1.1/src/main/java/com/leobeliik/extremesoundmuffler/mixins/minecraft/SoundMixin.java#L24-L33
        // tests String.contains of each forbidden entry against the full domain:path.
        String full = Naming.NAMESPACE + ":" + event;
        for (String entry : MUFFLER_BLACKLIST) {
            if (full.contains(entry)) {
                return entry;
            }
        }
        return null;
    }

    private static String part(String value) {
        if (value == null || !PART.matcher(value)
            .matches()) {
            throw new IllegalArgumentException("Sound name part must match [a-z][a-z0-9_]*: " + value);
        }
        return value;
    }
}
