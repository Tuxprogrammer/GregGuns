package com.gregguns.core.registry;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.GregGuns;
import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.core.module.ModuleContent;
import com.gregguns.core.module.ModuleId;

import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers one module's items, blocks, entities and tile entities under the GregGuns naming rules in {@link Naming}.
 * The {@code ModuleManager} hands every module its own instance during preInit, whatever the module flags say: flags
 * never change what is registered (they only switch behavior, see {@code ModuleState}), so a client and a server with
 * different flags still agree on every block, item and entity id. Each item and block is remembered under its module
 * ({@code ModuleContent}) so a module that is off can hide and disable it.
 *
 * <p>
 * Sounds have no registration call in 1.7.10: declare them in {@code assets/gregguns/sounds.json} and refer to them
 * with {@link Naming#soundEvent}.
 *
 * <p>
 * Example, inside a module's {@code preInit}:
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Item pistol = registrar.registerItem("pistol_9mm", new ItemPistol());
 *     // lang key item.gregguns.pistol_9mm.name, texture gregguns:pistol_9mm, registry id gregguns:pistol_9mm
 * }
 * </pre>
 *
 * Common side only, preInit only.
 */
// Spec: docs/ROADMAP.md (M0 scope: registries); CONTRIBUTING.md (Registration order, lang keys)
public final class ModuleRegistrar {

    private final ModuleId module;
    private final RegistrationLedger ledger;
    private final CreativeTabs tab;

    /**
     * @param module the module registering
     * @param ledger the shared duplicate check
     * @param tab    the creative tab for items and blocks that do not choose their own
     */
    public ModuleRegistrar(ModuleId module, RegistrationLedger ledger, CreativeTabs tab) {
        this.module = module;
        this.ledger = ledger;
        this.tab = tab;
    }

    /** @return the module this registrar belongs to */
    public ModuleId module() {
        return module;
    }

    /**
     * @return the GregGuns creative tab, for blocks whose own base class registers them (GT casings), which therefore
     *         cannot go through {@link #registerBlock}
     */
    public CreativeTabs tab() {
        return tab;
    }

    /**
     * Registers an item and puts it in the GregGuns creative tab, unless it implements {@link HiddenFromCreativeTab}.
     *
     * <p>
     * The tab is decided by the marker, not by asking the item for its current tab: {@code Item.getCreativeTab()} is
     * client-only and is stripped from the dedicated server, so calling it here would crash {@code runServer}.
     *
     * @param name the bare name, lower snake case (see {@link Naming#requireContentName})
     * @param item the item
     * @param <T>  the item type
     * @return {@code item}
     */
    public <T extends Item> T registerItem(String name, T item) {
        if (!(item instanceof HiddenFromCreativeTab)) {
            item.setCreativeTab(tab);
        }
        return registerItemWithoutTab(name, item);
    }

    /**
     * Registers an item and leaves its creative tab alone. Items hidden on purpose should implement
     * {@link HiddenFromCreativeTab} instead, so the dev self-check knows the omission is deliberate.
     *
     * @param name the bare name
     * @param item the item
     * @param <T>  the item type
     * @return {@code item}
     */
    public <T extends Item> T registerItemWithoutTab(String name, T item) {
        ledger.claimName(RegistrationLedger.Kind.ITEM, name);
        item.setUnlocalizedName(Naming.unlocalizedName(name));
        item.setTextureName(Naming.textureName(name));
        GameRegistry.registerItem(item, Naming.registryName(name));
        ModuleContent.assign(item, module);
        return item;
    }

    /**
     * Registers a block with the plain {@link ItemBlock} and the GregGuns creative tab.
     *
     * @param name  the bare name
     * @param block the block
     * @param <T>   the block type
     * @return {@code block}
     */
    public <T extends Block> T registerBlock(String name, T block) {
        return registerBlock(name, block, ItemBlock.class);
    }

    /**
     * Registers a block with a custom item block class. The class needs a constructor taking the {@link Block}. The
     * block goes in the GregGuns creative tab unless it implements {@link HiddenFromCreativeTab}
     * ({@code Block.getCreativeTabToDisplayOn()} is client-only, so it is not consulted).
     *
     * @param name      the bare name
     * @param block     the block
     * @param itemBlock the item block class
     * @param <T>       the block type
     * @return {@code block}
     */
    public <T extends Block> T registerBlock(String name, T block, Class<? extends ItemBlock> itemBlock) {
        ledger.claimName(RegistrationLedger.Kind.BLOCK, name);
        if (!(block instanceof HiddenFromCreativeTab)) {
            block.setCreativeTab(tab);
        }
        block.setBlockName(Naming.unlocalizedName(name));
        block.setBlockTextureName(Naming.textureName(name));
        GameRegistry.registerBlock(block, itemBlock, Naming.registryName(name));
        ModuleContent.assign(block, module);
        return block;
    }

    /**
     * Registers a tile entity class under {@code gregguns:<name>}.
     *
     * @param name  the bare name
     * @param clazz the tile entity class
     */
    public void registerTileEntity(String name, Class<? extends TileEntity> clazz) {
        ledger.claimName(RegistrationLedger.Kind.TILE_ENTITY, name);
        GameRegistry.registerTileEntity(clazz, Naming.tileEntityId(name));
    }

    /**
     * Registers an entity with a mod-local ID. The ID is chosen by the caller and kept in a constant, so it never
     * changes when other modules are toggled.
     *
     * @param name                 the bare name; Forge exposes it as {@code gregguns.<name>}
     * @param clazz                the entity class
     * @param id                   the mod-local ID, unique across GregGuns
     * @param trackingRange        blocks within which clients track it
     * @param updateFrequency      ticks between position updates
     * @param sendsVelocityUpdates whether velocity is sent
     */
    public void registerEntity(String name, Class<? extends Entity> clazz, int id, int trackingRange,
        int updateFrequency, boolean sendsVelocityUpdates) {
        Naming.requireContentName(name);
        registerEntityUnchecked(name, clazz, id, trackingRange, updateFrequency, sendsVelocityUpdates);
    }

    /**
     * Registers a living entity whose Forge name keeps its capitals ({@code gregguns.ZombieSoldier}), as the NPC
     * names do (spec: docs/gregguns-spec/10-entities-npcs, class simple names matter to Infernal Mobs). Otherwise it is
     * {@link #registerEntity}: same ID ledger, same duplicate check.
     *
     * @param name                 the CamelCase name, letters and digits only
     * @param clazz                the entity class
     * @param id                   the mod-local ID, unique across GregGuns
     * @param trackingRange        blocks within which clients track it
     * @param updateFrequency      ticks between position updates
     * @param sendsVelocityUpdates whether velocity is sent
     */
    public void registerMob(String name, Class<? extends Entity> clazz, int id, int trackingRange, int updateFrequency,
        boolean sendsVelocityUpdates) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Mob name must match [A-Za-z][A-Za-z0-9_]*: " + name);
        }
        registerEntityUnchecked(name, clazz, id, trackingRange, updateFrequency, sendsVelocityUpdates);
    }

    private void registerEntityUnchecked(String name, Class<? extends Entity> clazz, int id, int trackingRange,
        int updateFrequency, boolean sendsVelocityUpdates) {
        ledger.claimName(RegistrationLedger.Kind.ENTITY, name);
        ledger.claimEntityId(id, name);
        // Source:
        // https://github.com/MinecraftForge/FML/blob/1.7.10/src/main/java/cpw/mods/fml/common/registry/EntityRegistry.java#L147
        EntityRegistry.registerModEntity(
            clazz,
            name,
            id,
            GregGuns.instance,
            trackingRange,
            updateFrequency,
            sendsVelocityUpdates);
    }
}
