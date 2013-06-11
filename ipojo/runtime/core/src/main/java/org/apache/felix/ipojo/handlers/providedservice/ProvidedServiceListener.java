package org.apache.felix.ipojo.handlers.providedservice;

import org.apache.felix.ipojo.ComponentInstance;

/**
 * Listener interface for services provided by iPOJO component instances.
 */
public interface ProvidedServiceListener {

    /**
     * Called when the service has been registered.
     *
     * @param instance the concerned component instance
     * @param providedService the registered service
     */
    void serviceRegistered(ComponentInstance instance, ProvidedService providedService);

    /**
     * Called when the registered service has been updated.
     *
     * @param instance the concerned component instance
     * @param providedService the updated service
     */
    void serviceModified(ComponentInstance instance, ProvidedService providedService);

    /**
     * Called when the service is unregistered.
     *
     * @param instance the concerned component instance
     * @param providedService the unregistered service
     */
    void serviceUnregistered(ComponentInstance instance, ProvidedService providedService);

}
