package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;


public interface PlayerInfoContributor {


    String title();


    List<PlayerInfoField> contribute(Player player);

    default int order() {
        return 0;
    }
}
