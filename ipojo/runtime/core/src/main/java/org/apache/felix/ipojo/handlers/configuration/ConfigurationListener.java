package org.apache.felix.ipojo.handlers.configuration;

import org.apache.felix.ipojo.ComponentInstance;

import java.util.Map;

/**
 * Listener interface for configuration updates of iPOJO component instances.
 */
public interface ConfigurationListener {

    /**
     * Called when the component instance is reconfigured.
     *
     * @param instance the concerned instance
     * @param configuration snapshot of the instance configuration. Unmodifiable.
     */
    void configurationChanged(ComponentInstance instance, Map<String, Object> configuration);

}
