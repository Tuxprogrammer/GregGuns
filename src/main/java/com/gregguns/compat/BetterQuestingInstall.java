package com.gregguns.compat;

import com.gregguns.GregGuns;
import com.gregguns.compat.questbook.BetterQuestingTarget;
import com.gregguns.compat.questbook.QuestbookBundle;
import com.gregguns.compat.questbook.QuestbookException;
import com.gregguns.compat.questbook.QuestbookInstaller;
import com.gregguns.compat.questbook.QuestbookSource;
import com.gregguns.core.config.QuestbookConfig;

import cpw.mods.fml.common.Loader;

/**
 * Installs the GregGuns quest line from our jar into the server's BetterQuesting database (design record 0015). Soft
 * and server side: it does nothing unless {@code questbook.install} is true and BetterQuesting is loaded, and no
 * BetterQuesting class loads otherwise. Call {@link #onServerStarting()} from GregGuns' own server-starting handler;
 * the mod declares {@code after:betterquesting}, so BetterQuesting has already loaded its database by then.
 */
// Spec: docs/gregguns-spec/12-questbook/index.md; docs/design/0015-m9-self-contained-polish.md
// Source (BetterQuesting loads its databases in its own FMLServerStartingEvent handler):
// https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/core/proxies/CommonProxy.java#L50-L59
public final class BetterQuestingInstall {

    private static final String MODID = "betterquesting";

    private BetterQuestingInstall() {}

    public static void onServerStarting() {
        if (!QuestbookConfig.install) {
            return;
        }
        if (!Loader.isModLoaded(MODID)) {
            GregGuns.LOG.info("questbook.install is on but BetterQuesting is not loaded; skipping the quest line");
            return;
        }
        try {
            QuestbookBundle bundle = QuestbookBundle.load(
                new QuestbookSource.Classpath(BetterQuestingInstall.class.getClassLoader(), QuestbookSource.JAR_ROOT));
            if (bundle == null) {
                GregGuns.LOG.warn("questbook.install is on but this jar ships no questbook resources; skipping");
                return;
            }
            QuestbookInstaller.Plan plan = QuestbookInstaller.install(bundle, new BetterQuestingTarget());
            GregGuns.LOG.info(
                "Installed/updated GregGuns quest line: {} quests ({} added, {} updated)",
                plan.questCount(),
                plan.addedQuests.size(),
                plan.replacedQuests.size());
        } catch (QuestbookException e) {
            GregGuns.LOG.error("Could not install the GregGuns quest line; the quest database is unchanged", e);
        } catch (RuntimeException | LinkageError e) {
            GregGuns.LOG.error("Installing the GregGuns quest line failed", e);
        }
    }
}
