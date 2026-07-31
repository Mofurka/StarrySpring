package irden.space.proxy.plugin.irden.integration.web.client.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IrdenAppRoutes {
    public static final String API = "/api";

    public static class Linker {
        public static final String LINK = API + "/link";
    }

    public static class Storage {
        public static final String ROOT =  API + "/storages";
        public static final String FIND =  ROOT + "/find";
        public static final String REQUEST =  ROOT + "/requests";
    }

}
