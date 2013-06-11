package org.apache.felix.ipojo.handlers.providedservice;

/**
 * Listener interface for services provided by iPOJO component instances.
 */
public interface ProvidedServiceListener {

    /**
     * Called when the service has been registered.
     *
     * @param providedService the registered service.
     */
    void serviceRegistered(ProvidedService providedService);

    /**
     * Called when the registered service has been updated.
     *
     * @param providedService the updated service.
     */
    void serviceModified(ProvidedService providedService);

    /**
     * Called when the service is unregistered.
     *
     * @param providedService the unregistered service.
     */
    void serviceUnregistered(ProvidedService providedService);

}
