package irden.space.proxy.plugin.discord.listener;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.discord.DiscordSessionContext;
import irden.space.proxy.plugin.discord.DiscordSessionFactory;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;


@Component
public class DiscordSlashCommandListener extends ListenerAdapter implements DisposableBean {

    private static final int MAX_CHOICES = 25;
    private static final Logger log = LoggerFactory.getLogger(DiscordSlashCommandListener.class);

    private final CommandHandlerPlugin commandHandler;
    private final DiscordSessionFactory sessionFactory;
    private final ExecutorService commandExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public DiscordSlashCommandListener(CommandHandlerPlugin commandHandler, DiscordSessionFactory sessionFactory) {
        this.commandHandler = commandHandler;
        this.sessionFactory = sessionFactory;
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

        event.deferReply().queue(
                ignored -> submitCommand(event),
                error -> log.error(
                        "Failed to defer Discord slash command interaction: {}",
                        commandName,
                        error
                )
        );
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        String commandName = event.getName();
        log.info(
                "Received autocomplete interaction for command: {}, option: {}",
                commandName,
                event.getFocusedOption().getName()
        );

        PacketInterceptionContext packetContext = createAutocompleteContext(event);
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
                .limit(MAX_CHOICES)
                .map(value -> new Command.Choice(value, value))
                .toList();

        event.replyChoices(choices).queue();
    }

    private void submitCommand(SlashCommandInteractionEvent event) {
        try {
            commandExecutor.execute(() -> executeCommand(event));
        } catch (RejectedExecutionException exception) {
            log.error("Discord command executor rejected command: {}", event.getName(), exception);
            sendCommandError(event, "Бот завершает работу. Попробуйте выполнить команду позднее.");
        }
    }

    private void executeCommand(SlashCommandInteractionEvent event) {
        try {
            commandHandler.onChatSent(createCommandContext(event));
        } catch (Exception exception) {
            log.error("Failed to execute Discord slash command: {}", event.getName(), exception);
            sendCommandError(event, "При выполнении команды произошла ошибка.");
        }
    }

    private void sendCommandError(SlashCommandInteractionEvent event, String message) {
        event.getHook().sendMessage(message).queue(
                ignored -> {
                },
                error -> log.error(
                        "Failed to send Discord command error response: {}",
                        event.getName(),
                        error
                )
        );
    }

    private PacketInterceptionContext createCommandContext(SlashCommandInteractionEvent event) {
        DiscordSessionContext session = sessionFactory.create(event.getUser(), event.getMember(), event);

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

        ChatSent payload = new ChatSent(rawInput.toString(), null, null);
        return new PacketInterceptionContext(session, null, payload, PacketDirection.TO_SERVER);
    }

    private PacketInterceptionContext createAutocompleteContext(CommandAutoCompleteInteractionEvent event) {
        DiscordSessionContext session = sessionFactory.create(event.getUser(), event.getMember(), null);
        return new PacketInterceptionContext(session, null, event, PacketDirection.TO_SERVER);
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

    @Override
    public void destroy() {
        commandExecutor.shutdown();
    }
}
