package com.gregguns.core.command;

import java.util.List;

import net.minecraft.command.ICommandSender;

/**
 * One {@code /gregguns <name>} subcommand. Later milestones add theirs with
 * {@link GregGunsCommand#register(GregGunsSubCommand)}. Server side.
 */
public interface GregGunsSubCommand {

    /** @return the subcommand word, lower case */
    String name();

    /**
     * Runs the subcommand.
     *
     * @param sender the player or console
     * @param args   the arguments after the subcommand word
     */
    void execute(ICommandSender sender, String[] args);

    /**
     * @param sender the player or console
     * @param args   the arguments after the subcommand word, the last being completed
     * @return the completions, never {@code null}
     */
    List<String> complete(ICommandSender sender, String[] args);
}
