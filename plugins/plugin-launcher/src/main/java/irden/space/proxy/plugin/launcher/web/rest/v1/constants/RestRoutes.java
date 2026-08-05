package irden.space.proxy.plugin.launcher.web.rest.v1.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RestRoutes {
    public static final String API = "/api";
    public static final String V1 = API + "/v1";
    public static final String PUB = "/public";


    @UtilityClass
    public static final class OpenStarboundV1 {
        private static final String ROOT = "/open-starbound";
        public static final String PRIVATE = V1 + ROOT;
        public static final String PUBLIC = V1 + PUB + ROOT;
    }


}
