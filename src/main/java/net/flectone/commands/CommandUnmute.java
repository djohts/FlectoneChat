package net.flectone.commands;

import net.flectone.integrations.voicechats.plasmovoice.FPlasmoVoice;
import net.flectone.managers.FPlayerManager;
import net.flectone.misc.commands.FCommand;
import net.flectone.misc.commands.FTabCompleter;
import net.flectone.misc.entity.FPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CommandUnmute implements FTabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        FCommand fCommand = new FCommand(commandSender, command.getName(), s, strings);

        if (fCommand.isInsufficientArgs(1)) return true;

        FPlayer fPlayer = FPlayerManager.getPlayerFromName(strings[0]);

        if (fPlayer == null) {
            fCommand.sendMeMessage("command.null-player");
            return true;
        }

        if (fPlayer.getMuteTime() < 0) {
            fCommand.sendMeMessage("command.unmute.not-muted");
            return true;
        }

        if (fCommand.isHaveCD()) return true;

        if (FPlasmoVoice.isEnable()) {
            FPlasmoVoice.unmute(fPlayer.getRealName());
        }

        fPlayer.unmute();

        fCommand.sendMeMessage("command.unmute.message", "<player>", fPlayer.getRealName());

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        wordsList.clear();

        if (strings.length == 1) {
            FPlayerManager.getMutedPlayers().parallelStream()
                    .forEach(fPlayer -> isStartsWith(strings[0], fPlayer.getRealName()));
        }

        Collections.sort(wordsList);

        return wordsList;
    }

    @NotNull
    @Override
    public String getCommandName() {
        return "unmute";
    }
}
