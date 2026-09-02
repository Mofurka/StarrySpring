package irden.space.proxy.plugin.discord.api;

import lombok.Builder;
import lombok.Singular;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Builder(toBuilder = true)
public record DiscordReceivedMessage(
        DiscordMessageRef ref,
        DiscordAuthor author,
        String content,
        String displayContent,
        Instant createdAt,
        Instant editedAt,
        boolean pinned,
        boolean webhook,
        String jumpUrl,
        DiscordMessageRef replyTo,
        @Singular List<DiscordEmbed> embeds,
        @Singular List<DiscordAttachment> attachments
) {

    public DiscordReceivedMessage {
        embeds = embeds == null ? List.of() : List.copyOf(embeds);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        content = content == null ? "" : content;
        displayContent = displayContent == null ? content : displayContent;
    }

    public long channelId() {
        return ref.channelId();
    }

    public long messageId() {
        return ref.messageId();
    }

    public boolean fromBot() {
        return author != null && author.bot();
    }

    public boolean hasText() {
        return !content.isBlank();
    }

    public boolean isReply() {
        return replyTo != null;
    }

    public Optional<Instant> edited() {
        return Optional.ofNullable(editedAt);
    }

    public Optional<DiscordEmbed> firstEmbed() {
        return embeds.isEmpty() ? Optional.empty() : Optional.of(embeds.getFirst());
    }
}
