package com.gregguns.icbm.detect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.world.World;

import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;

/**
 * The missile tracker as radars read it (D14, D25): the missiles in flight in one world, taken from the live entity
 * list, never from chunks. A missile counts from the moment it leaves the pad (climb and arc); one still on the pad or
 * warming up is not a contact. Server thread only. The list is a snapshot, nearest first.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Contacts)
public final class Contacts {

    private Contacts() {}

    /**
     * @param world the world to look in; a client world has no contacts
     * @return every missile in flight there, in no particular order
     */
    public static List<Contact> all(World world) {
        List<Contact> out = new ArrayList<Contact>();
        if (world == null || world.isRemote) {
            return out;
        }
        for (Object o : world.loadedEntityList) {
            if (o instanceof EntityMissile) {
                Contact c = of((EntityMissile) o);
                if (c != null) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /**
     * @param world the world
     * @param x     the radar X
     * @param z     the radar Z
     * @param range the radar range, blocks (a flat circle)
     * @return the missiles in flight within range, nearest first
     */
    public static List<Contact> within(World world, double x, double z, double range) {
        List<Contact> out = new ArrayList<Contact>();
        for (Contact c : all(world)) {
            if (ContactMath.inRange(c.x(), c.z(), x, z, range)) {
                out.add(c);
            }
        }
        sortNearest(out, x, z);
        return out;
    }

    /**
     * @param contacts a list from {@link #within} or {@link #all}
     * @param cx       the protected center X
     * @param cz       the protected center Z
     * @param radius   the protected radius
     * @return the contacts whose predicted impact falls inside the circle, in the order given
     */
    public static List<Contact> incoming(List<Contact> contacts, double cx, double cz, double radius) {
        List<Contact> out = new ArrayList<Contact>();
        for (Contact c : contacts) {
            if (c.incoming(cx, cz, radius)) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Sorts by flat distance to a point, nearest first. (ICBM's sort compared the wrong thing, ICBM-05-05.)
     *
     * @param contacts the list, sorted in place
     * @param x        the point X
     * @param z        the point Z
     */
    public static void sortNearest(List<Contact> contacts, final double x, final double z) {
        Collections.sort(contacts, new Comparator<Contact>() {

            @Override
            public int compare(Contact a, Contact b) {
                int c = Double.compare(a.distanceTo(x, z), b.distanceTo(x, z));
                return c != 0 ? c : Integer.compare(a.id(), b.id());
            }
        });
    }

    /**
     * @param m a missile entity
     * @return its contact, or null when it is on the pad, warming up, or dead
     */
    public static Contact of(EntityMissile m) {
        if (m.isDead || m.worldObj.isRemote) {
            return null;
        }
        FlightPhase p = m.phase();
        if (p != FlightPhase.CLIMB && p != FlightPhase.ARC) {
            return null;
        }
        double[] aim = m.aimPoint();
        return new Contact(
            m.getEntityId(),
            m.worldObj.provider.dimensionId,
            m.posX,
            m.posY,
            m.posZ,
            m.motionX,
            m.motionY,
            m.motionZ,
            m.tier()
                .number(),
            aim[0],
            aim[2]);
    }

    /**
     * @param world a world
     * @param id    a contact id
     * @return the live missile with that entity id that is still in flight, else null
     */
    public static EntityMissile missile(World world, int id) {
        net.minecraft.entity.Entity e = world.getEntityByID(id);
        if (e instanceof EntityMissile && of((EntityMissile) e) != null) {
            return (EntityMissile) e;
        }
        return null;
    }
}
