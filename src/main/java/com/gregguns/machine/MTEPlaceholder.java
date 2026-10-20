package com.gregguns.machine;

import net.minecraft.util.StatCollector;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;

/**
 * An inert stand-in for a GregGuns machine whose module is switched off on this side. It does nothing, has no
 * inventory and no recipes; it exists so that GT's {@code ItemMachines} and block renderer find a machine at the id and
 * an item stack of it renders and tooltips instead of throwing a NullPointerException. Both sides; see
 * {@link MtePlaceholders}.
 */
// Spec: docs/gregguns-spec/08-machines-production/metatileentity-framework.md (MTE ID reservation)
public final class MTEPlaceholder extends MTEHatch {

    private static final String LANG_KEY = "gregguns.machine.placeholder.tooltip";

    /**
     * The prototype that registers the placeholder under {@code id}.
     *
     * @param id   the MTE id, from {@link GregGunsMTEIDs}
     * @param name GT's internal name, unique per id
     */
    public MTEPlaceholder(int id, String name) {
        super(id, name, "GregGuns machine (module off)", 0, 0, new String[] { LANG_KEY });
    }

    private MTEPlaceholder(String name, String[] description, ITexture[][][] textures) {
        super(name, 0, 0, description, textures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity baseMetaTileEntity) {
        return new MTEPlaceholder(mName, mDescriptionArray, mTextures);
    }

    @Override
    public String[] getDescription() {
        // The lang file is not loaded at construction, so the key is translated here.
        return com.gregguns.core.text.AddedBy.append(new String[] { StatCollector.translateToLocal(LANG_KEY) });
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture };
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.MACHINE_CASINGS[0][0];
    }
}
