package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.LaunchTarget;
import com.gregguns.icbm.missile.LaunchResult;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileStatus;
import com.gregguns.icbm.missile.MissileTier;

/** The silo's computer aim runs the Launch Control's gate (GGI-01-04) over a fake site. */
class SiloAimTest {

    private static final class Site implements LaunchSite {

        MissileStatus status = MissileStatus.NONE;
        LaunchResult hold = LaunchResult.OK;
        int launched;

        @Override
        public double centerX() {
            return 0.5;
        }

        @Override
        public double centerZ() {
            return 0.5;
        }

        @Override
        public MissileStatus status() {
            return status;
        }

        @Override
        public int maxRange() {
            return 3000;
        }

        @Override
        public double expectedRadius(double x, double z) {
            return 10;
        }

        @Override
        public LaunchResult hold() {
            return hold;
        }

        @Override
        public LaunchResult launch(double x, double z, EntityPlayer source) {
            launched++;
            return LaunchResult.OK;
        }
    }

    @Test
    void theGateRunsInTheLaunchControlsOrder() {
        SiloAim aim = new SiloAim();
        Site site = new Site();
        assertEquals(ControlStatus.NO_TARGET, aim.fire(true, site));
        aim.set(LaunchTarget.of(1000, 0));
        assertEquals(ControlStatus.NO_PAD, aim.fire(false, site), "an unformed silo is no pad");
        assertEquals(ControlStatus.NO_MISSILE, aim.fire(true, site));
        site.status = new MissileStatus(true, MissileTier.T3, 100, 100, 0, "he", 1);
        site.hold = LaunchResult.SILO_TIER;
        assertEquals(ControlStatus.SILO_TIER, aim.fire(true, site), "the hatch tier refuses like the Launch Control");
        assertEquals(0, site.launched, "a refused call launches nothing");
        site.hold = LaunchResult.OK;
        assertEquals(ControlStatus.FIRED, aim.fire(true, site));
        assertEquals(1, site.launched);
    }

    @Test
    void theAimSurvivesNbt() {
        SiloAim a = new SiloAim();
        a.set(LaunchTarget.of(-12, 34));
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        a.save(tag);
        SiloAim b = new SiloAim();
        b.load(tag);
        assertEquals(
            -12,
            b.target()
                .x());
        assertEquals(
            34,
            b.target()
                .z());
        b.load(new net.minecraft.nbt.NBTTagCompound());
        assertEquals(
            false,
            b.target()
                .isSet());
    }
}
