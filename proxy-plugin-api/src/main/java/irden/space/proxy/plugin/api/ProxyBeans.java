package irden.space.proxy.plugin.api;


public final class ProxyBeans {

    private static final String CGLIB_CLASS_SEPARATOR = "$$";

    private ProxyBeans() {
    }

    public static Class<?> userClass(Object bean) {
        return userClass(bean.getClass());
    }

    public static Class<?> userClass(Class<?> type) {
        if (type != null && type.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            Class<?> superclass = type.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return type;
    }
}
