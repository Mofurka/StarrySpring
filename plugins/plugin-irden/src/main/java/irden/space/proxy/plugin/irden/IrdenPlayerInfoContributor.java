package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.irden.service.PlayerAccountService;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import irden.space.proxy.plugin.player_manager.api.PlayerInfoContributor;
import irden.space.proxy.plugin.player_manager.api.PlayerInfoField;
import irden.space.proxy.plugin.player_manager.api.PlayerInfoRegistry;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;


@Component
@RequiredArgsConstructor
public class IrdenPlayerInfoContributor implements PlayerInfoContributor {

    private final PlayerAccountService playerAccountService;
    private final PlayerInfoRegistry playerInfoRegistry;
    private final PluginContext pluginContext;

    @OnLoad
    public void register() {
        playerInfoRegistry.register(this);
        pluginContext.onRemove(() -> playerInfoRegistry.unregister(this));
    }

    @Override
    public String title() {
        return "Irden";
    }

    @Override
    public List<PlayerInfoField> contribute(Player player) {
        if (player.uuid() == null) {
            return List.of();
        }

        String playerUuid = player.uuid().toString();
        List<PlayerInfoField> fields = new ArrayList<>(2);

        OptionalLong applicationId = playerAccountService.findApplicationId(playerUuid);
        if (applicationId.isEmpty()) {
            fields.add(PlayerInfoField.of("Application ID", "not linked"));
            return fields;
        }

        fields.add(PlayerInfoField.of("Application ID", applicationId.getAsLong()));

        try {
            fields.add(PlayerInfoField.of("Balance", playerAccountService.getMainAccount(playerUuid).getBalance()));
        } catch (AccountNotFoundException _) {
            fields.add(PlayerInfoField.of("Balance", "no account"));
        }

        return fields;
    }
}
