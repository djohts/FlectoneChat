package net.flectone.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.flectone.Main;
import net.flectone.commands.CommandChatcolor;
import net.flectone.managers.FPlayerManager;
import net.flectone.managers.HookManager;
import net.flectone.messages.MessageBuilder;
import net.flectone.misc.entity.FPlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.flectone.managers.FileManager.config;
import static net.flectone.managers.FileManager.locale;

public class ObjectUtil {

    @NotNull
    public static String convertTimeToString(int timeInSeconds) {
        if (timeInSeconds < 0) return "";

        int days = (timeInSeconds / 86400);
        int hours = (timeInSeconds / 3600) % 24;
        int minutes = (timeInSeconds / 60) % 60;
        int seconds = timeInSeconds % 60;

        StringBuilder stringBuilder = new StringBuilder();

        if (days > 0) stringBuilder.append(" ").append(days).append(locale.getString("command.online.format.day"));
        if (hours > 0) stringBuilder.append(" ").append(hours).append(locale.getString("command.online.format.hour"));
        if (minutes > 0) stringBuilder.append(" ").append(minutes).append(locale.getString("command.online.format.minute"));
        if (seconds > 0) stringBuilder.append(" ").append(seconds).append(locale.getString("command.online.format.second"));

        return stringBuilder.substring(1);
    }

    @NotNull
    public static String convertTimeToString(long time) {
        String timeoutSecondsString = String.valueOf((time - System.currentTimeMillis()) / 1000).substring(1);
        int timeoutSeconds = Integer.parseInt(timeoutSecondsString);
        return convertTimeToString(timeoutSeconds);
    }

    @NotNull
    public static String toString(@NotNull String[] strings) {
        if (strings.length == 0) return "";
        return toString(strings, 0);
    }

    @NotNull
    public static String toString(@NotNull String[] strings, int start) {
        return toString(strings, start, " ");
    }

    public static String toString(@NotNull String[] strings, int start, @NotNull String delimiter) {
        return Arrays.stream(strings, start, strings.length)
                .collect(Collectors.joining(delimiter));
    }

    @NotNull
    public static String translateHexToColor(@NotNull String string) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(string);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String hexCode = matcher.group();
            String replaceSharp = hexCode.replace('#', 'x');

            StringBuilder builder = new StringBuilder();
            for (char c : replaceSharp.toCharArray()) {
                builder.append("&").append(c);
            }
            matcher.appendReplacement(sb, builder.toString());
        }
        matcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    @NotNull
    public static String formatString(@NotNull String string, @Nullable CommandSender recipient, @Nullable CommandSender sender) {
        return formatString(false, string, recipient, sender);
    }

    @NotNull
    public static String formatString(boolean neededPermission, @NotNull String string, @Nullable CommandSender recipient, @Nullable CommandSender sender) {
        String[] colors = null;

        if (recipient instanceof Player playerRecipient) {
            if (HookManager.enabledPlaceholderAPI && sender instanceof Player playerSender
                    && (!neededPermission || sender.hasPermission("flectonechat.placeholders"))) {

                string = PlaceholderAPI.setPlaceholders(playerSender, string);
                string = PlaceholderAPI.setRelationalPlaceholders(playerSender, playerRecipient, string);
            }

            FPlayer fPlayer = FPlayerManager.getPlayer(playerRecipient);
            colors = fPlayer != null ? fPlayer.getColors() : null;
        }

        colors = colors != null ? colors : CommandChatcolor.getDefaultColors();

        return translateHexToColor(string
                .replace("&&1", colors[0])
                .replace("&&2", colors[1]));
    }

    @NotNull
    public static String formatString(@NotNull String string, @Nullable CommandSender sender) {
        return formatString(string, sender, sender);
    }

    public static int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static void playSound(@Nullable Player player, @NotNull String command) {
        if (player == null || !config.getBoolean("sound." + command + ".enable")) return;

        String[] params = config.getString("sound." + command + ".type").split(":");
        if (params.length < 3) {
            Main.warning("Update the sound string sound." + command + ".type to a new format SOUND:VOLUME:PITCH");
            params = new String[]{params[0], "1", "1"};
        }

        try {
            player.playSound(player.getLocation(), Sound.valueOf(params[0]), Float.parseFloat(params[1]), Float.parseFloat(params[2]));
        } catch (IllegalArgumentException exception) {
            Main.warning("Incorrect sound " + params[0] + " for " + command + ".sound.type");
            exception.printStackTrace();
        }

    }

    @NotNull
    public static String buildFormattedMessage(@NotNull Player player, @NotNull String command, @Nullable String text, @NotNull ItemStack itemStack) {
        if (text == null) return "";

        MessageBuilder messageBuilder = new MessageBuilder(command, text, player, itemStack, false);
        return messageBuilder.getMessage("");
    }

    @NotNull
    public static ArrayList<String> splitLine(@NotNull String line, @NotNull ArrayList<String> placeholders) {
        ArrayList<String> split = new ArrayList<>(List.of(line));

        for (String placeholder : placeholders) {
            split = (ArrayList<String>) split.stream().flatMap(part -> {
                String[] sp = part.split("((?=" + placeholder + ")|(?<=" + placeholder + "))");
                return Arrays.stream(sp);
            }).collect(Collectors.toList());
        }

        return split;
    }

    @NotNull
    public static String generateSortString(int rank, String playerName) {
        String paddedRank = String.format("%010d", Integer.MAX_VALUE - rank);
        String paddedName = String.format("%-16s", playerName);
        return paddedRank + paddedName;
    }

    @NotNull
    public static String getOfflinePlayerName(String uuid) {
        if (uuid == null) return "CONSOLE";

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        String name = offlinePlayer.getName();
        return name != null ? name : "Unknown";
    }
}
