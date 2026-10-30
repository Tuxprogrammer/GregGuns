package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * The questbook switch, the {@code questbook} category of {@code config/gregguns.cfg}. On by default and shown in the
 * in-game config GUI. Fields only; no Minecraft types.
 */
// Spec: docs/design/0015-m9-self-contained-polish.md (decision 2: a config key, on by default)
@Config(modid = "gregguns", category = "questbook")
public class QuestbookConfig {

    @Config.Comment({ "Server only. Installs the GregGuns quest line from this jar into BetterQuesting's database",
        "when the server starts. Needs BetterQuesting. Safe to leave on: it updates only the GregGuns quests and keeps",
        "player progress. Turning it off later does not remove the line (see docs/config.md, Questbook install).",
        "Default: true." })
    public static boolean install = true;
}
