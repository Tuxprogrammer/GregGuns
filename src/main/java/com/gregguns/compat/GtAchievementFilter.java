package com.gregguns.compat;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.AchievementList;
import net.minecraftforge.common.AchievementPage;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.Loader;
import gregtech.GTMod;

/**
 * Keeps GregGuns items out of GregTech's auto-generated achievements.
 *
 * <p>
 * GT registers one achievement for the first output of every Assembly Line recipe, named after the output's
 * unlocalized name (so {@code achievement.tile.gregguns.missile_body_t3} for a block-backed item), and awards it
 * whenever a player picks up a stack with that name. Nothing translates the name for a mod GT does not know, so the
 * player sees the raw key. This removes ours from GT's lookup table, from GT's achievement page and from the vanilla
 * list; GT then looks the id up, gets null, and {@code EntityPlayer.triggerAchievement(null)} does nothing. GT's other
 * pickup effect, unhiding the recipe in NEI, is unaffected.
 *
 * <p>
 * Runs at load-complete, after GT's post-load built the table. Only the {@code achievementList}, the page and the
 * vanilla list are touched; the stat itself stays registered so saved player stats keep parsing.
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/misc/GTAchievements.java#L423-L448
// (registerAssAchievement: one achievement per assembly-line output, keyed by unlocalized name) and
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/misc/GTAchievements.java#L450-L455
// (issueAchievement: triggerAchievement(achievementList.get(id)))
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/misc/GTAchievements.java#L787-L797
// (onItemPickup: matches every assline output by unlocalized name)
// Spec: docs/gregguns-spec/PACK-CHANGES.md
public final class GtAchievementFilter {

    private GtAchievementFilter() {}

    /** Removes the GregGuns achievements GT generated. Safe when GT's achievements are off or absent. */
    public static void apply() {
        try {
            if (!Loader.isModLoaded("gregtech") || GTMod.achievements == null) {
                return;
            }
            Map<String, Achievement> table = GTMod.achievements.achievementList;
            int n = strip(table.values(), GtAchievementFilter::isGregGuns);
            if (n > 0) {
                int skipped = stripPages(
                    AchievementPage.getAchievementPages(),
                    AchievementPage::getAchievements,
                    GtAchievementFilter::isGregGuns);
                if (skipped > 0) {
                    // KubaTech's client-only tea page is the known case; any other page is worth a look.
                    GregGuns.LOG.info("Skipped {} achievement pages that could not be read", skipped);
                }
                strip(AchievementList.achievementList, GtAchievementFilter::isGregGuns);
                GregGuns.LOG.info("Removed {} GregGuns items from GregTech's generated achievements", n);
            }
        } catch (LinkageError | RuntimeException e) {
            GregGuns.LOG.warn("GregTech's achievements could not be filtered; GregGuns items may show raw names", e);
        }
    }

    /** @return true if the achievement's icon item is a GregGuns item */
    public static boolean isGregGuns(Achievement a) {
        if (a == null || a.theItemStack == null || a.theItemStack.getItem() == null) {
            return false;
        }
        String name = Item.itemRegistry.getNameForObject(a.theItemStack.getItem());
        return name != null && name.startsWith("gregguns:");
    }

    /**
     * Removes every matching element in place.
     *
     * @param <T>   element type
     * @param items the collection to prune
     * @param match which elements to drop
     * @return how many were removed
     */
    static <T> int strip(Collection<T> items, Predicate<? super T> match) {
        int n = 0;
        for (Iterator<T> it = items.iterator(); it.hasNext();) {
            if (match.test(it.next())) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    /**
     * Strips every page's list, skipping a page whose list cannot be read now, so one mod's page cannot stop the
     * pages after it and the vanilla list from being cleaned. KubaTech's tea page is one: on the client its
     * {@code getAchievements} reads {@code Minecraft.thePlayer}, which is null at load-complete. That page only ever
     * lists KubaTech's teas, so skipping it loses nothing.
     *
     * @param <P>          page type
     * @param <T>          element type
     * @param pages        the pages
     * @param achievements reads one page's list
     * @param match        which elements to drop
     * @return how many pages were skipped
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/kubatech/loaders/item/kubaitem/items/ItemTeaCollection.java#L188-L199
    // (TeaPage.getAchievements: Minecraft.getMinecraft().thePlayer.getStatFileWriter() on the client)
    static <P, T> int stripPages(Iterable<P> pages, Function<? super P, ? extends Collection<T>> achievements,
        Predicate<? super T> match) {
        int skipped = 0;
        for (P page : pages) {
            try {
                strip(achievements.apply(page), match);
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        return skipped;
    }

    /** Overload for a page's list, which may be a plain list. */
    static <T> int strip(List<T> items, Predicate<? super T> match) {
        return strip((Collection<T>) items, match);
    }
}
