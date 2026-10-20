package com.gregguns.core.command;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.gregguns.GregGuns;
import com.gregguns.core.config.DebugConfig;
import com.gregguns.core.dump.RecipeDumper;

/**
 * {@code /gregguns dumprecipes <file>}: writes every crafting and GT recipe to a JSON file for the recipe-clash
 * review. Refuses unless {@code debug.recipeDump} is on; the parent command is already op-only. Server side.
 */
public final class DumpRecipesSubCommand implements GregGunsSubCommand {

    @Override
    public String name() {
        return "dumprecipes";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (!DebugConfig.recipeDump) {
            sender.addChatMessage(new ChatComponentText("dumprecipes is disabled: set debug.recipeDump=true"));
            return;
        }
        if (args.length != 1) {
            sender.addChatMessage(new ChatComponentText("usage: /gregguns dumprecipes <file>"));
            return;
        }
        try {
            int n = RecipeDumper.dump(Paths.get(args[0]));
            GregGuns.LOG.info("recipe dump: {} rows written to {}", n, args[0]);
            sender.addChatMessage(new ChatComponentText("recipe dump: " + n + " rows -> " + args[0]));
        } catch (IOException | RuntimeException e) {
            GregGuns.LOG.error("recipe dump failed", e);
            sender.addChatMessage(new ChatComponentText("recipe dump failed: " + e));
        }
    }

    @Override
    public List<String> complete(ICommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
