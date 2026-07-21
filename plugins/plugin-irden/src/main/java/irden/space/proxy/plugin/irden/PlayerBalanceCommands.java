package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.irden.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerBalanceCommands {
    private final AccountService accountService;

    //TODO: Возможно сделать это через EM вместо команд, но тогда бы желательно написать какой-нибудь MessageHandler, на вроде CommandHandler'a.


}
