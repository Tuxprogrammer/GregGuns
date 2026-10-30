package com.gregguns.compat;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
                for (AchievementPage page : AchievementPage.getAchievementPages()) {
                    strip(page.getAchievements(), GtAchievementFilter::isGregGuns);
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

    /** Overload for a page's list, which may be a plain list. */
    static <T> int strip(List<T> items, Predicate<? super T> match) {
        return strip((Collection<T>) items, match);
    }
}
