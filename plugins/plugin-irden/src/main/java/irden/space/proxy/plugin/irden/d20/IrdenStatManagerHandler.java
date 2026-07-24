package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class IrdenStatManagerHandler {
    private static final String STAT_MANAGER_VERSION = "4.25.15";

    @EntityMessageHandler("statmanager")
    public void handleStatManager(EntityMessageContext message) {
        log.info("statmanager receive message: {}", Arrays.toString(message.args()));
    }


}
