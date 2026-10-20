package com.gregguns.item.part;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gregguns.GregGuns;
import com.gregguns.api.GregGunsComponent;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;

/**
 * A GregGuns fluid tank (Fuel, Bio or Compressed Air): a 1,000 mB Forge {@link IFluidContainerItem} that accepts only
 * the GT fluids listed for it. GT's Fluid Canner fills an empty tank on the fly through the interface, so GregGuns
 * registers no Canner recipe (RC-56 to RC-58).
 *
 * <p>
 * The contents live in the stack's NBT under {@code Fluid}; an emptied tank loses its tag so it stacks with fresh
 * ones again. The empty texture is {@code <name>_empty}. Common side, with client-only icon methods.
 */
// Spec: docs/gregguns-spec/06-items/fluids.md (GGS-06-16, GGS-06-17), docs/gregguns-spec/06-items/recipes.md (RC-52 to
// RC-58, GGS-06-40)
// Deviation: the capacity is the constant 1,000 mB (design record 0010 decision 3), not a config value, and the
// accepted
// fluids are fixed per tank instead of config-listed, until M2 balances the fuel set. No FluidContainerRegistry
// entries:
// the Canner's dynamic IFluidContainerItem path covers every fill and drain.
public final class ItemTank extends Item implements Tiered, IFluidContainerItem, GregGunsComponent {

    private static final String FLUID_TAG = "Fluid";

    private final Tier tier;
    private final List<String> acceptedMaterials;
    private final boolean gas;
    private final String name;
    private final Predicate<String> fluidFilter;
    private Set<String> acceptedFluidNames;
    private IIcon emptyIcon;

    /**
     * @param tier              the D1 tier
     * @param name              the bare registry name, which names the textures {@code <name>} and {@code <name>_empty}
     * @param gas               {@code true} to look the accepted materials up as gases ({@code getGas}) first
     * @param acceptedMaterials GT {@code Materials} names ({@code mName}, as {@code Materials.get} takes them: Diesel
     *                          is "Fuel") whose fluid or gas the tank takes
     */
    public ItemTank(Tier tier, String name, boolean gas, List<String> acceptedMaterials) {
        this(tier, name, gas, acceptedMaterials, null);
    }

    /**
     * @param tier              the D1 tier
     * @param name              the bare registry name
     * @param gas               {@code true} to look the accepted materials up as gases first
     * @param acceptedMaterials GT {@code Materials} names whose fluid the tank takes
     * @param fluidFilter       when not {@code null}, decides by Forge fluid name instead of {@code acceptedMaterials}
     *                          (the Bio Tank, whose culture fluid comes from BartWorks, not from a GT material)
     */
    public ItemTank(Tier tier, String name, boolean gas, List<String> acceptedMaterials,
        Predicate<String> fluidFilter) {
        this.fluidFilter = fluidFilter;
        this.tier = tier;
        this.name = name;
        this.gas = gas;
        this.acceptedMaterials = Collections.unmodifiableList(acceptedMaterials);
        setMaxStackSize(16);
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    /** @return the GT material names whose fluids this tank accepts */
    public List<String> acceptedMaterials() {
        return acceptedMaterials;
    }

    @Override
    public FluidStack getFluid(ItemStack container) {
        if (container == null || container.stackTagCompound == null || !container.stackTagCompound.hasKey(FLUID_TAG)) {
            return null;
        }
        return FluidStack.loadFluidStackFromNBT(container.stackTagCompound.getCompoundTag(FLUID_TAG));
    }

    @Override
    public int getCapacity(ItemStack container) {
        return TankMath.CAPACITY;
    }

    @Override
    public int fill(ItemStack container, FluidStack resource, boolean doFill) {
        if (container == null || resource == null || resource.getFluid() == null || !accepts(resource.getFluid())) {
            return 0;
        }
        FluidStack current = getFluid(container);
        if (current != null && !current.isFluidEqual(resource)) {
            return 0;
        }
        int stored = current == null ? 0 : current.amount;
        int accepted = TankMath.accept(stored, resource.amount, TankMath.CAPACITY);
        if (doFill && accepted > 0) {
            FluidStack next = new FluidStack(resource, stored + accepted);
            if (container.stackTagCompound == null) {
                container.stackTagCompound = new NBTTagCompound();
            }
            container.stackTagCompound.setTag(FLUID_TAG, next.writeToNBT(new NBTTagCompound()));
        }
        return accepted;
    }

    @Override
    public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
        FluidStack current = getFluid(container);
        if (current == null) {
            return null;
        }
        int taken = TankMath.take(current.amount, maxDrain);
        if (taken <= 0) {
            return null;
        }
        if (doDrain) {
            int left = current.amount - taken;
            if (left <= 0) {
                container.stackTagCompound.removeTag(FLUID_TAG);
                if (container.stackTagCompound.hasNoTags()) {
                    container.stackTagCompound = null;
                }
            } else {
                container.stackTagCompound
                    .setTag(FLUID_TAG, new FluidStack(current, left).writeToNBT(new NBTTagCompound()));
            }
        }
        return new FluidStack(current, taken);
    }

    private boolean accepts(Fluid fluid) {
        if (fluidFilter != null) {
            return fluidFilter.test(fluid.getName());
        }
        if (acceptedFluidNames == null) {
            Set<String> names = new HashSet<>();
            for (String material : acceptedMaterials) {
                Materials gt = Materials.get(material);
                FluidStack stack = gas ? gt.getGas(1) : gt.getFluid(1);
                if (stack == null) {
                    stack = gas ? gt.getFluid(1) : gt.getGas(1);
                }
                if (stack == null || stack.getFluid() == null) {
                    GregGuns.LOG.warn("Tank {} accepts {}, but GT has no fluid for it; skipping", name, material);
                    continue;
                }
                names.add(
                    stack.getFluid()
                        .getName());
            }
            acceptedFluidNames = names;
        }
        return acceptedFluidNames.contains(fluid.getName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        emptyIcon = register.registerIcon("gregguns:" + name + "_empty");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        return getFluid(stack) == null && emptyIcon != null ? emptyIcon : super.getIconIndex(stack);
    }
}
