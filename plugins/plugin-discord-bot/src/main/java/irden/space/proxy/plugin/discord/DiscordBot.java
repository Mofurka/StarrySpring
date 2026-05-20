package irden.space.proxy.plugin.discord;


import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.Permissions;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.command_handler.RegisteredCommand;
import irden.space.proxy.plugin.discord.model.DiscordRoleManager;
import irden.space.proxy.plugin.player_manager.model.StarryRole;
import irden.space.proxy.plugin.player_manager.model.UserPermissions;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class DiscordBot extends ListenerAdapter {
    private final Logger log = LoggerFactory.getLogger(DiscordBot.class);
    private final JDA jda;
    private final CommandHandlerPlugin commandHandler;
    private final RoleManager roleManager;
    private final DiscordRoleManager discordRoleManager = new DiscordRoleManager();

    public DiscordBot(String token, CommandHandlerPlugin commandHandler, RoleManager roleManager) {
        EnumSet<GatewayIntent> intents = EnumSet.allOf(GatewayIntent.class);
        var jdaAuth = JDABuilder.createDefault(token, intents)
                .build();
        try {
            this.jda = jdaAuth.awaitReady();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Failed to initialize Discord bot: {}", e.getMessage());
            throw new IllegalStateException("Failed to initialize Discord bot", e);
        }
        this.commandHandler = commandHandler;
        this.roleManager = roleManager;
        registerCommands();
        this.jda.addEventListener(this);

    }

    public void registerCommands() {
        Collection<RegisteredCommand> registeredCommands = commandHandler.allCommands();

        List<CommandData> discordCommands = DiscordCommandExporter.export(registeredCommands);

        if (!discordCommands.isEmpty()) {
            jda.updateCommands()
                    .addCommands(discordCommands)
                    .queue();
        } else {
            log.info("No commands to register for Discord bot.");
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }
        log.info("Received message from {}: {}", event.getAuthor().getAsTag(), event.getMessage().getChannel());
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        log.info(
                "Received slash command interaction: {}, option: {}",
                commandName,
                event.getOptions().stream()
                        .map(OptionMapping::getName)
                        .toList()
        );
        commandHandler.onChatSent(
                createDiscordPacketContext(event)
        );
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        var commandName = event.getName();
        log.info(
                "Received autocomplete interaction for command: {}, option: {}",
                commandName,
                event.getFocusedOption().getName()
        );

        PacketInterceptionContext packetContext = createDiscordPacketAutocompleteContext(event);
        List<String> suggestions = commandHandler.autocomplete(
                packetContext,
                commandName,
                extractLiteralPath(event),
                extractOptionValues(event),
                event.getFocusedOption().getName(),
                event.getFocusedOption().getValue()
        );

        List<Command.Choice> choices = suggestions.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(25)
                .map(value -> new Command.Choice(value, value))
                .toList();

        event.replyChoices(choices).queue();
    }

    private PacketInterceptionContext createDiscordPacketContext(SlashCommandInteractionEvent event) {
        DiscordSessionContext session = createDiscordSessionContext(
                event.getUser().getId(),
                event.getUser().getName(),
                event.getMember(),
                event
        );
        StringBuilder rawInput = new StringBuilder("/").append(event.getName());
            if (event.getSubcommandGroup() != null && !event.getSubcommandGroup().isBlank()) {
                rawInput.append(" ").append(event.getSubcommandGroup());
            }
            if (event.getSubcommandName() != null && !event.getSubcommandName().isBlank()) {
                rawInput.append(" ").append(event.getSubcommandName());
            }
            for (OptionMapping option : event.getOptions()) {
                rawInput.append(" ").append(option.getAsString());
            }
        var payload = new ChatSent(rawInput.toString(), null, null);
        return new PacketInterceptionContext(session, null, payload, PacketDirection.TO_SERVER);
    }

    private PacketInterceptionContext createDiscordPacketAutocompleteContext(CommandAutoCompleteInteractionEvent event) {
        DiscordSessionContext session = createDiscordSessionContext(
                event.getUser().getId(),
                event.getUser().getName(),
                event.getMember(),
                null
        );
        return new PacketInterceptionContext(session, null, event, PacketDirection.TO_SERVER);
    }

    private DiscordSessionContext createDiscordSessionContext(
            String userId,
            String userName,
            Member member,
            Object event
    ) {
        return new DiscordSessionContext(
                userId,
                userName,
                member != null ? member.getEffectiveName() : userName,
                resolveDiscordPermissions(userId, member),
                event
        );
    }

    private PermissionView resolveDiscordPermissions(String userId, Member member) {
        LinkedHashSet<String> resolvedRoleNames = new LinkedHashSet<>();

        String defaultRoleName = roleManager.defaultRoleName();
        if (defaultRoleName != null && !defaultRoleName.isBlank()) {
            resolvedRoleNames.add(defaultRoleName);
        }

        if (member != null) {
            resolvedRoleNames.addAll(
                    discordRoleManager.resolveServerRoleNames(
                            member.getRoles().stream()
                                    .map(Role::getIdLong)
                                    .toList()
                    )
            );
        }

        if (resolvedRoleNames.isEmpty()) {
            return PermissionView.EMPTY;
        }

        List<StarryRole> grantedStarryRoles = new ArrayList<>();
        List<String> unknownRoleNames = new ArrayList<>();

        for (String roleName : resolvedRoleNames) {
            roleManager.findRole(roleName)
                    .ifPresentOrElse(
                            grantedStarryRoles::add,
                            () -> unknownRoleNames.add(roleName)
                    );
        }

        if (!unknownRoleNames.isEmpty()) {
            log.warn("Discord user {} matched unknown server roles from config: {}", userId, unknownRoleNames);
        }

        if (grantedStarryRoles.isEmpty()) {
            return PermissionView.EMPTY;
        }

        return new UserPermissions(grantedStarryRoles, Permissions.none());
    }


    private List<String> extractLiteralPath(CommandAutoCompleteInteractionEvent event) {
        List<String> result = new ArrayList<>(2);

        if (event.getSubcommandGroup() != null && !event.getSubcommandGroup().isBlank()) {
            result.add(event.getSubcommandGroup());
        }

        if (event.getSubcommandName() != null && !event.getSubcommandName().isBlank()) {
            result.add(event.getSubcommandName());
        }

        return List.copyOf(result);
    }

    private Map<String, String> extractOptionValues(CommandAutoCompleteInteractionEvent event) {
        Map<String, String> values = new LinkedHashMap<>();

        for (OptionMapping option : event.getOptions()) {
            values.put(option.getName(), option.getAsString());
        }

        return values;
    }


    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}
