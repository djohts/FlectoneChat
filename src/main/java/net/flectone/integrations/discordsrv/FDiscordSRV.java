package net.flectone.integrations.discordsrv;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import net.flectone.Main;
import net.flectone.integrations.HookInterface;
import net.flectone.misc.advancement.FAdvancement;
import net.flectone.utils.ObjectUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.function.BiFunction;

import static net.flectone.managers.FileManager.locale;

/*

DiscordSRV - https://github.com/DiscordSRV/DiscordSRV

Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
This file is part of DiscordDRV

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public
License along with this program.  If not, see
<http://www.gnu.org/licenses/gpl-3.0.html>.

 */

public class FDiscordSRV implements Listener, HookInterface {

    private static boolean isEnable = false;

    public static void sendDeathMessage(@NotNull Player player, @NotNull String message, @Nullable Entity finalEntity, @Nullable Material finalBlock, @Nullable Entity killer, @Nullable ItemStack killerItem) {
        message = message.replace("<player>", player.getName());
        if (finalEntity != null) message = message
                .replace("<killer>", finalEntity.getName())
                .replace("<projectile>", finalEntity.getName());

        if (finalBlock != null) message = message
                .replace("<block>", finalBlock.name());

        if (killer != null) {
            String dueToMessage = locale.getFormatString("death.due-to", null);
            message = message.replace("<due_to>", dueToMessage.replace("<killer>", killer.getName()));
        }

        if (killerItem != null) {
            String byItemMessage = locale.getFormatString("death.by-item", null);

            String itemName = killerItem.getItemMeta() != null && !killerItem.getItemMeta().getDisplayName().isEmpty()
                    ? killerItem.getItemMeta().getDisplayName()
                    : killerItem.getType().name();

            message = message.replace("<by_item>", byItemMessage.replace("<item>", itemName));
        }

        String deathMessage = ObjectUtil.formatString(message, null)
                .replace("<killer>", "")
                .replace("<projectile>", "")
                .replace("<block>", "")
                .replace("<due_to>", "")
                .replace("<by_item>", "");

        deathMessage = deathMessage.length() > 255 ? deathMessage.substring(0, 255) : deathMessage;

        String channelName = DiscordSRV.getPlugin().getOptionalChannel("deaths");
        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerDeathMessage");
        if (messageFormat == null) return;

        String finalDeathMessage = StringUtils.isNotBlank(deathMessage) ? deathMessage : "";
        String avatarUrl = DiscordSRV.getAvatarUrl(player);
        String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
        String displayName = StringUtils.isNotBlank(player.getDisplayName()) ? MessageUtil.strip(player.getDisplayName()) : "";

        TextChannel destinationChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) return null;
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%username%", needsEscape ? DiscordUtil.escapeMarkdown(player.getName()) : player.getName())
                    .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName)
                    .replace("%usernamenoescapes%", player.getName())
                    .replace("%displaynamenoescapes%", displayName)
                    .replace("%world%", player.getWorld().getName())
                    .replace("%deathmessage%", MessageUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(finalDeathMessage) : finalDeathMessage))
                    .replace("%deathmessagenoescapes%", MessageUtil.strip(finalDeathMessage))
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl)
                    .replace("%botname%", botName);
            if (destinationChannel != null)
                content = DiscordUtil.translateEmotes(content, destinationChannel.getGuild());
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        };
        Message discordMessage = DiscordSRV.translateMessage(messageFormat, translator);
        if (discordMessage == null) return;

        if (DiscordSRV.getLength(discordMessage) < 3) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Not sending death message, because it's less than three characters long. Message: " + messageFormat);
            return;
        }

        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        DiscordUtil.queueMessage(textChannel, discordMessage, true);
    }

    public static void sendModerationMessage(@NotNull String message) {
        message = ObjectUtil.formatString(message, null);
        message = PlaceholderUtil.replacePlaceholdersToDiscord(message);

        String channelName = DiscordSRV.getPlugin().getOptionalChannel("global");
        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);

        Message messageFormat = new MessageBuilder()
                .setEmbeds(new EmbedBuilder().setColor(Color.RED).setDescription(message).build())
                .build();

        DiscordUtil.queueMessage(textChannel, messageFormat, true);
    }

    public static void sendAdvancementMessage(@NotNull Player player, @NotNull FAdvancement fAdvancement, @NotNull String lastAdvancement) {
        String channelName = DiscordSRV.getPlugin().getOptionalChannel("awards");

        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerAchievementMessage");
        if (messageFormat == null) return;

        String advancementTitle = fAdvancement.getTitle();

        lastAdvancement = ObjectUtil.formatString(lastAdvancement, null)
                .replace("<player>", player.getName())
                .replace("<advancement>", advancementTitle);
        lastAdvancement = PlaceholderUtil.replacePlaceholdersToDiscord(lastAdvancement);
        lastAdvancement = lastAdvancement.length() > 255 ? lastAdvancement.substring(0, 255) : lastAdvancement;

        String finalAchievementName = StringUtils.isNotBlank(advancementTitle) ? advancementTitle : "";
        String avatarUrl = DiscordSRV.getAvatarUrl(player);
        String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
        String displayName = StringUtils.isNotBlank(player.getDisplayName()) ? MessageUtil.strip(player.getDisplayName()) : "";

        TextChannel destinationChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) return null;
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%username%", needsEscape ? DiscordUtil.escapeMarkdown(player.getName()) : player.getName())
                    .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName)
                    .replace("%usernamenoescapes%", player.getName())
                    .replace("%displaynamenoescapes%", displayName)
                    .replace("%world%", player.getWorld().getName())
                    .replace("%achievement%", MessageUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(finalAchievementName) : finalAchievementName))
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl)
                    .replace("%botname%", botName);
            if (destinationChannel != null)
                content = DiscordUtil.translateEmotes(content, destinationChannel.getGuild());
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        };

        Message discordMessage = DiscordSRV.translateMessage(messageFormat, translator);

        MessageEmbed embed = discordMessage.getEmbeds().get(0);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(embed.getColor());

        MessageEmbed.AuthorInfo authorInfo = embed.getAuthor();
        embedBuilder.setAuthor(lastAdvancement, authorInfo.getUrl(), authorInfo.getIconUrl());

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setEmbeds(embedBuilder.build());

        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        DiscordUtil.queueMessage(textChannel, messageBuilder.build(), true);
    }

    @Subscribe(priority = ListenerPriority.HIGHEST)
    public void onChatMessageFromInGame(@NotNull GameChatMessagePreProcessEvent event) {
        Player player = event.getPlayer();

        String message = event.getMessage();

        message = ObjectUtil.buildFormattedMessage(player, "discordchat", message, player.getInventory().getItemInMainHand());

        event.setMessage(message);
    }

    public static boolean isEnable() {
        return isEnable;
    }

    @Override
    public void hook() {
        DiscordSRV.api.subscribe(this);
        isEnable = true;
        Main.info("\uD83D\uDD12 DiscordSRV detected and hooked");
    }
}
