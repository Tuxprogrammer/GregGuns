package com.gregguns.icbm.soak;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.gregguns.GregGuns;
import com.gregguns.core.command.GregGunsSubCommand;

/**
 * {@code /gregguns soakstat [label]}: logs one {@code SOAKSTAT} line, a {@link SoakProbe} sample (tick time, GregGuns
 * chunk tickets and forced chunks, loaded chunks, entities, blasts, heap), for the external soak driver to read from
 * the server log. The line is a fixed machine format, not player text, so it is not translated. Dev environment only
 * (registered with the test hooks).
 */
// Spec: docs/gregguns-icbm-spec/01-core/soak.md
public final class SoakStatSubCommand implements GregGunsSubCommand {

    @Override
    public String name() {
        return "soakstat";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        String label = args.length > 0 ? args[0] : "-";
        String line = "SOAKSTAT label=" + label
            + " "
            + SoakProbe.sample()
                .line();
        GregGuns.LOG.info(line);
        sender.addChatMessage(new ChatComponentText(line));
    }

    @Override
    public List<String> complete(ICommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
