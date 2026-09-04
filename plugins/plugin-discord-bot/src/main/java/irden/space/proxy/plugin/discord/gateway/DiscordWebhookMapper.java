package irden.space.proxy.plugin.discord.gateway;

import irden.space.proxy.plugin.discord.api.DiscordWebhook;
import irden.space.proxy.plugin.discord.api.DiscordWebhookType;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class DiscordWebhookMapper {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] GIF_MAGIC = {'G', 'I', 'F', '8'};

    private DiscordWebhookMapper() {
    }

    static DiscordWebhook toWebhook(Webhook webhook) {
        User owner = webhook.getOwnerAsUser();

        return DiscordWebhook.builder()
                .id(webhook.getIdLong())
                .name(webhook.getName())
                .token(webhook.getToken())
                .avatarUrl(webhook.getDefaultUser().getEffectiveAvatarUrl())
                .channelId(webhook.isPartial() ? 0 : webhook.getChannel().getIdLong())
                .guildId(webhook.isPartial() ? 0 : webhook.getGuild().getIdLong())
                .type(toType(webhook.getType()))
                .ownerId(owner == null ? 0 : owner.getIdLong())
                .ownerName(owner == null ? null : owner.getName())
                .partial(webhook.isPartial())
                .build();
    }

    static List<DiscordWebhook> toWebhooks(Collection<Webhook> webhooks) {
        List<DiscordWebhook> result = new ArrayList<>(webhooks.size());
        for (Webhook webhook : webhooks) {
            result.add(toWebhook(webhook));
        }
        return result;
    }

    static Icon toIcon(byte[] avatar) {
        return Icon.from(avatar, startsWith(avatar, PNG_MAGIC)
                ? Icon.IconType.PNG
                : startsWith(avatar, GIF_MAGIC) ? Icon.IconType.GIF : Icon.IconType.JPEG);
    }

    private static boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static DiscordWebhookType toType(WebhookType type) {
        return switch (type) {
            case INCOMING -> DiscordWebhookType.INCOMING;
            case FOLLOWER -> DiscordWebhookType.FOLLOWER;
            case UNKNOWN -> DiscordWebhookType.UNKNOWN;
        };
    }
}
