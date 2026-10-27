package com.gregguns.block;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.registry.Naming;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockCasingsAbstract;
import gregtech.common.blocks.ItemCasings;
import gregtech.common.blocks.MaterialCasings;

/**
 * The GregGuns multiblock casings: one block with eleven metas (the Works, Gantry and Bastion Plating families; see the
 * constants). It extends GT's casing
 * base so a broken casing re-checks the multiblock around it, and it claims casing texture page {@value #PAGE} so GT
 * hatches placed in a casing position can copy the casing's look.
 *
 * <p>
 * Usage: {@code GregGunsCasings.create(tab)} once in the guns module's pre-init; {@code GregGunsCasings.INSTANCE}
 * after that. Hatch casing index: {@link #textureIndex(int)}. Common side; icons are client only.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Casings), metatileentity-framework.md (GGS-08-04)
// Deviation: the Ram Housing's animated "active" texture is not wired. GT casings have no per-block active state, so
// the
// sheet's frame strip needs the controller's renderer to draw it; that is client work left to the animation pass.
// Deviation: rendered as a plain cube (render type 0) with no tint. GT's casing renderer and color multiplier tint a
// gray casing with the machine-metal color, which would stain GregGuns' colored textures.
public final class GregGunsCasings extends BlockCasingsAbstract implements Tiered {

    /** GregGuns' texture page for casings. */
    public static final int PAGE = 50;
    /** Meta of the Works Casing. */
    public static final int WORKS_CASING = 0;
    /** Meta of the Ram Housing. */
    public static final int RAM_HOUSING = 1;
    /**
     * Meta of the retired Reaction Casing: inert and decorative, kept so placed Retorts leave no holes. Meta 2 is
     * reserved forever (design record 0012).
     */
    public static final int REACTION_CASING = 2;
    /** Meta of the Gantry Casing (the Armory Gantry's walls and floors; hatches go here). */
    public static final int GANTRY_CASING = 3;
    /** Meta of the Gantry Beam (the rail across the top). */
    public static final int GANTRY_BEAM = 4;
    /** Meta of the Gantry Hoist (the crane head on the beam). */
    public static final int GANTRY_HOIST = 5;
    /** Meta of the Repair Cradle (the pad under each bay; its top differs from its sides). */
    public static final int REPAIR_CRADLE = 6;
    /** Meta of the Iron Bastion Plating (the Rampart's wall; armor value 5). */
    public static final int BASTION_IRON = 7;
    /** Meta of the Steel Bastion Plating (armor value 10). */
    public static final int BASTION_STEEL = 8;
    /** Meta of the Aluminum Bastion Plating (armor value 15). */
    public static final int BASTION_ALUMINUM = 9;
    /** Meta of the Carbon Bastion Plating (armor value 20). */
    public static final int BASTION_CARBON = 10;
    /** Meta of the Radar Casing (the radar tower's walls; hatches go here). */
    public static final int RADAR_CASING = 11;
    /** Number of metas; every meta has one page-50 texture index equal to the meta. */
    public static final int META_COUNT = 12;
    /** The bare content name; the registry id is {@code gregguns:casings}. */
    public static final String NAME = "casings";

    private static final String[] TEXTURES = { "gregguns:casings/works_casing", "gregguns:casings/ram_housing",
        "gregguns:casings/reaction_casing", "gregguns:casings/gantry_casing", "gregguns:casings/gantry_beam",
        "gregguns:casings/gantry_hoist", "gregguns:casings/repair_cradle_side", "gregguns:casings/bastion_plating_iron",
        "gregguns:casings/bastion_plating_steel", "gregguns:casings/bastion_plating_aluminum",
        "gregguns:casings/bastion_plating_carbon", "gregguns:casings/thirdparty/ntm/radar_casing" };
    private static final String CRADLE_TOP = "gregguns:casings/repair_cradle_top";

    /** The block, set by {@link #create}; {@code null} before {@link #create}. */
    public static GregGunsCasings INSTANCE;

    private final IIcon[] icons = new IIcon[META_COUNT];
    private IIcon cradleTop;
    /** False while GT's base constructor registers the block, so it is registered as {@code casings}. */
    private boolean constructed;

    private GregGunsCasings() {
        super(ItemCasings.class, claimPage(NAME), MaterialCasings.INSTANCE, META_COUNT);
        constructed = true;
    }

    private static String claimPage(String name) {
        GTUtility.addTexturePage((byte) PAGE);
        return name;
    }

    /**
     * Creates and registers the block. Call once, from pre-init.
     *
     * @param tab the GregGuns creative tab
     * @return the block
     */
    public static GregGunsCasings create(CreativeTabs tab) {
        if (INSTANCE != null) {
            throw new IllegalStateException("The Works casing block is already created");
        }
        GregGunsCasings block = new GregGunsCasings();
        block.setCreativeTab(tab);
        block.setBlockTextureName(TEXTURES[WORKS_CASING]);
        INSTANCE = block;
        return block;
    }

    /**
     * @param meta a casing meta
     * @return GT's casing texture index for it, which a hatch adder passes to {@code casingIndex}
     */
    public static int textureIndex(int meta) {
        return (PAGE << 7) | meta;
    }

    @Override
    public Tier getTier() {
        // The block reports its highest meta (Carbon Bastion Plating, an EV recipe); the self-check reads one tier per
        // block.
        return Tier.EV;
    }

    @Override
    public String getUnlocalizedName() {
        // GT's constructor registers the block under this name, so the bare name is used until construction ends.
        return constructed ? Naming.unlocalizedName(NAME) : NAME;
    }

    @Override
    public int getTextureIndex(int meta) {
        return textureIndex(meta);
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        for (int i = 0; i < icons.length; i++) {
            icons[i] = register.registerIcon(TEXTURES[i]);
        }
        cradleTop = register.registerIcon(CRADLE_TOP);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (meta == REPAIR_CRADLE && side <= 1) {
            return cradleTop;
        }
        return icons[Math.max(0, Math.min(icons.length - 1, meta))];
    }
}
