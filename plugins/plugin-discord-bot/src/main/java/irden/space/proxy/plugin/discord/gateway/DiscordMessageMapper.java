package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.DiscordButton;
import irden.space.proxy.plugin.discord.api.DiscordEmbed;
import irden.space.proxy.plugin.discord.api.DiscordEmbedField;
import irden.space.proxy.plugin.discord.api.DiscordMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.ArrayList;
import java.util.List;


final class DiscordMessageMapper {

    private DiscordMessageMapper() {
    }

    static MessageCreateData toCreateData(DiscordMessage message) {
        requireNotEmpty(message);

        MessageCreateBuilder builder = new MessageCreateBuilder();

        if (hasText(message.content())) {
            builder.setContent(message.content());
        }
        if (!message.embeds().isEmpty()) {
            builder.setEmbeds(toEmbeds(message.embeds()));
        }
        if (!message.buttons().isEmpty()) {
            builder.setComponents(toComponents(message.buttons()));
        }

        return builder.build();
    }

    static MessageEditData toEditData(DiscordMessage message) {
        requireNotEmpty(message);

        MessageEditBuilder builder = new MessageEditBuilder();

        builder.setContent(hasText(message.content()) ? message.content() : "");
        builder.setEmbeds(toEmbeds(message.embeds()));
        builder.setComponents(toComponents(message.buttons()));

        return builder.build();
    }

    private static List<ActionRow> toComponents(List<DiscordButton> buttons) {
        if (buttons.isEmpty()) {
            return List.of();
        }

        List<ActionRowChildComponent> mapped = new ArrayList<>(buttons.size());
        for (DiscordButton button : buttons) {
            mapped.add(toButton(button));
        }

        return ActionRow.partitionOf(mapped);
    }

    private static Button toButton(DiscordButton button) {
        Button mapped = button.isLink()
                ? Button.link(button.url(), button.label())
                : switch (button.style()) {
            case PRIMARY -> Button.primary(button.id(), button.label());
            case SUCCESS -> Button.success(button.id(), button.label());
            case DANGER -> Button.danger(button.id(), button.label());
            case SECONDARY -> Button.secondary(button.id(), button.label());
        };

        if (hasText(button.emoji())) {
            mapped = mapped.withEmoji(Emoji.fromFormatted(button.emoji()));
        }
        if (button.disabled()) {
            mapped = mapped.withDisabled(true);
        }

        return mapped;
    }

    private static List<MessageEmbed> toEmbeds(List<DiscordEmbed> embeds) {
        List<MessageEmbed> result = new ArrayList<>(embeds.size());
        for (DiscordEmbed embed : embeds) {
            result.add(toEmbed(embed));
        }
        return result;
    }

    private static MessageEmbed toEmbed(DiscordEmbed embed) {
        EmbedBuilder builder = new EmbedBuilder();

        if (hasText(embed.title())) {
            builder.setTitle(embed.title(), embed.titleUrl());
        }
        if (hasText(embed.description())) {
            builder.setDescription(embed.description());
        }
        if (embed.color() != null) {
            builder.setColor(embed.color());
        }
        if (hasText(embed.authorName())) {
            builder.setAuthor(embed.authorName(), embed.authorUrl(), embed.authorIconUrl());
        }
        if (hasText(embed.thumbnailUrl())) {
            builder.setThumbnail(embed.thumbnailUrl());
        }
        if (hasText(embed.imageUrl())) {
            builder.setImage(embed.imageUrl());
        }
        if (hasText(embed.footerText())) {
            builder.setFooter(embed.footerText(), embed.footerIconUrl());
        }
        if (embed.timestamp() != null) {
            builder.setTimestamp(embed.timestamp());
        }

        for (DiscordEmbedField field : embed.fields()) {
            builder.addField(field.name(), field.value(), field.inline());
        }

        return builder.build();
    }

    private static void requireNotEmpty(DiscordMessage message) {
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Discord message needs content or at least one embed");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
