package irden.space.proxy.plugin.api;

import javax.sql.DataSource;

public interface StoragePluginContext extends PluginContext {
    DataSource dataSource();

}
