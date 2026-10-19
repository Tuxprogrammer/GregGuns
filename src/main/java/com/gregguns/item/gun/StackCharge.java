package com.gregguns.item.gun;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.gun.logic.ChargeSource;

/**
 * The {@link ChargeSource} of one energy gun stack: it reads and draws the stack's IC2 charge through
 * {@link ElectricGear}, so a shot spends exactly what a GT charger put in. Server for {@link #spending}, either side
 * for {@link #view} (the client prediction mirror only counts).
 */
// Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-12)
// Source: IC2 API ic2.api.item.ElectricItem / IElectricItemManager (api-source, IC2 2.2.828-experimental)
public final class StackCharge implements ChargeSource {

    private final ItemStack stack;
    private final EntityLivingBase user;
    private final boolean spends;

    private StackCharge(ItemStack stack, EntityLivingBase user, boolean spends) {
        this.stack = stack;
        this.user = user;
        this.spends = spends;
    }

    /**
     * @param stack the gun stack
     * @param user  the shooter
     * @return a source whose drains really remove EU from the stack
     */
    public static StackCharge spending(ItemStack stack, EntityLivingBase user) {
        return new StackCharge(stack, user, true);
    }

    /**
     * @param stack the gun stack
     * @return a source that reports the stack's charge and drains nothing, for client prediction
     */
    public static StackCharge view(ItemStack stack) {
        return new StackCharge(stack, null, false);
    }

    @Override
    public long stored() {
        return stack == null ? 0L : (long) Math.floor(ElectricGear.charge(stack));
    }

    /** All or nothing, like IC2's {@code use}: a partial shot never happens. */
    @Override
    public long drain(long eu) {
        if (eu <= 0 || stack == null || stored() < eu) {
            return 0L;
        }
        if (!spends) {
            return eu;
        }
        return ElectricGear.use(stack, eu, user) ? eu : 0L;
    }
}
