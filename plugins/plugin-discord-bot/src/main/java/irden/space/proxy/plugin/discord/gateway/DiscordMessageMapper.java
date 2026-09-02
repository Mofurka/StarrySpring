package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.time.Instant;
import java.time.OffsetDateTime;
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

    static DiscordReceivedMessage toReceived(Message message) {
        MessageReference reference = message.getMessageReference();

        return DiscordReceivedMessage.builder()
                .ref(new DiscordMessageRef(message.getChannelIdLong(), message.getIdLong()))
                .author(toAuthor(message))
                .content(message.getContentRaw())
                .displayContent(message.getContentDisplay())
                .createdAt(toInstant(message.getTimeCreated()))
                .editedAt(toInstant(message.getTimeEdited()))
                .pinned(message.isPinned())
                .webhook(message.isWebhookMessage())
                .jumpUrl(message.getJumpUrl())
                .replyTo(reference == null
                        ? null
                        : new DiscordMessageRef(reference.getChannelIdLong(), reference.getMessageIdLong()))
                .embeds(toApiEmbeds(message.getEmbeds()))
                .attachments(toAttachments(message.getAttachments()))
                .build();
    }

    static List<DiscordReceivedMessage> toReceived(List<Message> messages) {
        List<DiscordReceivedMessage> result = new ArrayList<>(messages.size());
        for (Message message : messages) {
            result.add(toReceived(message));
        }
        return result;
    }

    private static DiscordAuthor toAuthor(Message message) {
        User author = message.getAuthor();
        String displayName = message.getMember() != null
                ? message.getMember().getEffectiveName()
                : author.getEffectiveName();

        return new DiscordAuthor(
                author.getIdLong(),
                author.getName(),
                displayName,
                author.getEffectiveAvatarUrl(),
                author.isBot(),
                author.isSystem()
        );
    }

    private static List<DiscordAttachment> toAttachments(List<Message.Attachment> attachments) {
        List<DiscordAttachment> result = new ArrayList<>(attachments.size());
        for (Message.Attachment attachment : attachments) {
            result.add(new DiscordAttachment(
                    attachment.getIdLong(),
                    attachment.getFileName(),
                    attachment.getUrl(),
                    attachment.getProxyUrl(),
                    attachment.getContentType(),
                    attachment.getSize(),
                    attachment.isImage(),
                    attachment.isVideo(),
                    attachment.isSpoiler()
            ));
        }
        return result;
    }

    private static List<DiscordEmbed> toApiEmbeds(List<MessageEmbed> embeds) {
        List<DiscordEmbed> result = new ArrayList<>(embeds.size());
        for (MessageEmbed embed : embeds) {
            result.add(toApiEmbed(embed));
        }
        return result;
    }

    private static DiscordEmbed toApiEmbed(MessageEmbed embed) {
        DiscordEmbed.DiscordEmbedBuilder builder = DiscordEmbed.builder()
                .title(embed.getTitle())
                .titleUrl(embed.getUrl())
                .description(embed.getDescription())
                .color(embed.getColorRaw() == Role.DEFAULT_COLOR_RAW ? null : embed.getColorRaw())
                .timestamp(toInstant(embed.getTimestamp()));

        MessageEmbed.AuthorInfo author = embed.getAuthor();
        if (author != null) {
            builder.authorName(author.getName())
                    .authorUrl(author.getUrl())
                    .authorIconUrl(author.getIconUrl());
        }

        MessageEmbed.Thumbnail thumbnail = embed.getThumbnail();
        if (thumbnail != null) {
            builder.thumbnailUrl(thumbnail.getUrl());
        }

        MessageEmbed.ImageInfo image = embed.getImage();
        if (image != null) {
            builder.imageUrl(image.getUrl());
        }

        MessageEmbed.Footer footer = embed.getFooter();
        if (footer != null) {
            builder.footerText(footer.getText()).footerIconUrl(footer.getIconUrl());
        }

        for (MessageEmbed.Field field : embed.getFields()) {
            builder.field(new DiscordEmbedField(
                    field.getName() == null ? "" : field.getName(),
                    field.getValue() == null ? "" : field.getValue(),
                    field.isInline()
            ));
        }

        return builder.build();
    }

    private static Instant toInstant(OffsetDateTime time) {
        return time == null ? null : time.toInstant();
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
