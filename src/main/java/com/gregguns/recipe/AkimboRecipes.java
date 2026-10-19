package com.gregguns.recipe;

import java.util.Arrays;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import com.gregguns.GregGuns;
import com.gregguns.gun.logic.AkimboNbt;
import com.gregguns.item.gun.ItemGun;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The akimbo crafting-table recipes: two single guns combine into one akimbo gun, and one akimbo gun splits back into
 * two. They stay crafting-table recipes because a machine recipe cannot carry the guns' NBT (GGS-02-07); the tier is
 * therefore the single gun's, not a machine's. The NBT arithmetic is {@link AkimboNbt}.
 *
 * <h2>Combine</h2>
 * Shapeless, exactly two copies of the single gun in any slots, any NBT. The result carries the first gun's tag with
 * the rounds summed and the second gun's whole tag stored inside it.
 *
 * <h2>Split</h2>
 * Shapeless, exactly one akimbo gun. The recipe's result is the first single gun; the second is handed over by
 * {@link #onCrafted} (a crafting recipe has one output), on the server only. If the player's inventory is full, the
 * extra gun is dropped at their feet, never voided.
 *
 * <p>
 * Both recipes extend {@link ShapelessRecipes} so NEI lists them; they override the match and the result. Common side.
 * Registered by the akimbo definitions' {@code recipes} hook on GT's before-postload; {@link #registerHandler()} is
 * called once from the guns module.
 *
 * <p>
 * Deviation: GUN-26. See {@link AkimboNbt}; and the extra gun is added only on the server, so no phantom item shows on
 * the client.
 */
// Spec: docs/techguns-spec/02-guns/recipes.md (RecipeAkimboGun, RecipeAkimboSplit); docs/gregguns-spec/02-guns/
// recipes.md (Akimbo combine and split), GGS-02-07
// ShapelessRecipes is vanilla 1.7.10 (no pack source to cite); matches and getCraftingResult are overridden so the
// NBT is read, and the recipe still lists in NEI as a shapeless recipe.
public final class AkimboRecipes {

    private AkimboRecipes() {}

    /**
     * Registers the combine and split recipes of one pair. Call from a {@code recipes} hook.
     *
     * @param singleName the single gun's registry name, for example {@code "pistol_9mm"}
     * @param akimboName the akimbo gun's registry name, for example {@code "akimbo_pistols"}
     */
    public static void register(String singleName, String akimboName) {
        Item single = GameRegistry.findItem(GregGuns.MODID, singleName);
        Item akimbo = GameRegistry.findItem(GregGuns.MODID, akimboName);
        if (!(single instanceof ItemGun) || !(akimbo instanceof ItemGun)) {
            GregGuns.LOG.error("Skipping the {} combine/split recipes: an item is missing", akimboName);
            return;
        }
        GameRegistry.addRecipe(new Combine((ItemGun) single, (ItemGun) akimbo));
        GameRegistry.addRecipe(new Split((ItemGun) single, (ItemGun) akimbo));
    }

    /** Registers the crafted-event handler that gives the second gun of a split. Call once, common side. */
    public static void registerHandler() {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(new AkimboRecipes());
    }

    /**
     * The tag of the akimbo gun that combining the two stacks would make, for a recipe result and for tests.
     *
     * @param single the single gun's item
     * @param akimbo the akimbo gun's item
     * @param first  the first single gun
     * @param second the second single gun
     * @return a new akimbo stack
     */
    public static ItemStack combine(ItemGun single, ItemGun akimbo, ItemStack first, ItemStack second) {
        ItemStack a = first.copy();
        ItemStack b = second.copy();
        ItemGun.ensureTag(a, single.stats());
        ItemGun.ensureTag(b, single.stats());
        ItemStack out = new ItemStack(akimbo);
        out.setTagCompound(AkimboNbt.combine(a.getTagCompound(), b.getTagCompound(), single.stats(), akimbo.stats()));
        return out;
    }

    /**
     * The two single guns an akimbo stack splits into.
     *
     * @param single the single gun's item
     * @param akimbo the akimbo gun's item
     * @param pair   the akimbo stack
     * @return two new stacks: the recipe's result and the one handed over besides
     */
    public static ItemStack[] split(ItemGun single, ItemGun akimbo, ItemStack pair) {
        ItemStack copy = pair.copy();
        ItemGun.ensureTag(copy, akimbo.stats());
        NBTTagCompound[] tags = AkimboNbt.split(copy.getTagCompound(), single.stats(), akimbo.stats());
        ItemStack first = new ItemStack(single);
        first.setTagCompound(tags[0]);
        ItemStack second = new ItemStack(single);
        second.setTagCompound(tags[1]);
        return new ItemStack[] { first, second };
    }

    /** Gives the second gun of a split to the player, on the server only (GUN-26: nothing is added on the client). */
    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player == null || event.player.worldObj.isRemote
            || event.crafting == null
            || !(event.crafting.getItem() instanceof ItemGun)) {
            return;
        }
        ItemGun result = (ItemGun) event.crafting.getItem();
        ItemStack pair = null;
        for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
            ItemStack in = event.craftMatrix.getStackInSlot(i);
            if (in != null) {
                if (pair != null) {
                    return; // more than one ingredient: a combine, not a split
                }
                pair = in;
            }
        }
        if (pair == null || !(pair.getItem() instanceof ItemGun)) {
            return;
        }
        ItemGun akimbo = (ItemGun) pair.getItem();
        if (!result.definition()
            .name()
            .equals(
                akimbo.definition()
                    .singleGun())) {
            return;
        }
        ItemStack second = split(result, akimbo, pair)[1];
        if (!event.player.inventory.addItemStackToInventory(second)) {
            event.player.dropPlayerItemWithRandomChoice(second, false);
        }
    }

    /** Two single guns in any slots make the akimbo gun. */
    private static final class Combine extends ShapelessRecipes {

        private final ItemGun single;
        private final ItemGun akimbo;

        Combine(ItemGun single, ItemGun akimbo) {
            super(
                new ItemStack(akimbo),
                Arrays.asList(
                    new ItemStack(single, 1, OreDictionary.WILDCARD_VALUE),
                    new ItemStack(single, 1, OreDictionary.WILDCARD_VALUE)));
            this.single = single;
            this.akimbo = akimbo;
        }

        @Override
        public boolean matches(InventoryCrafting inv, World world) {
            int found = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) {
                    continue;
                }
                if (s.getItem() != single) {
                    return false;
                }
                found++;
            }
            return found == 2;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            ItemStack first = null;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) {
                    continue;
                }
                if (first == null) {
                    first = s;
                } else {
                    return combine(single, akimbo, first, s);
                }
            }
            return null;
        }
    }

    /** One akimbo gun in any slot makes the first single gun (the second comes from {@link #onCrafted}). */
    private static final class Split extends ShapelessRecipes {

        private final ItemGun single;
        private final ItemGun akimbo;

        Split(ItemGun single, ItemGun akimbo) {
            super(new ItemStack(single), Arrays.asList(new ItemStack(akimbo, 1, OreDictionary.WILDCARD_VALUE)));
            this.single = single;
            this.akimbo = akimbo;
        }

        @Override
        public boolean matches(InventoryCrafting inv, World world) {
            int found = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) {
                    continue;
                }
                if (s.getItem() != akimbo) {
                    return false;
                }
                found++;
            }
            return found == 1;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s != null) {
                    return split(single, akimbo, s)[0];
                }
            }
            return null;
        }
    }
}
