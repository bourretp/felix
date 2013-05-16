package org.apache.felix.ipojo;

import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.ServiceReference;

import java.util.List;

/**
 * TODO doc
 *
 * @param <S>
 */
public interface ServiceRankingInterceptor<S> {

    /**
     * Opens the service ranking interception for the given dependency.
     *
     * @param dep the dependency that starts to use this interceptor
     * @param matching the matching set of services for the given dependency
     * @return a reordered subset of {@code matchingRefs}
     */
    ServiceReferenceSet<S> open(DependencyModel dep, ServiceReferenceSet<S> matching);

    /**
     * Manages the arrival of a service.
     *
     * @param dep the dependency that see the service arrival
     * @param matching the matching set of services for the given dependency
     * @param service the arriving service
     * @return a reordered subset of {@code matchingRefs} <em>plus</em> {@code service}
     */
    List<ServiceReference<S>> onServiceArrival(DependencyModel dep, ServiceReferenceSet<S> matching, ServiceReference<S> service);

    /**
     * Manages the modification of a service.
     *
     * @param dep the dependency that see the service modification
     * @param matching the matching set of services for the given dependency
     * @param service the modified service
     * @return a reordered subset of {@code matchingRefs}
     */
    List<ServiceReference<S>> onServiceModification(DependencyModel dep, ServiceReferenceSet<S> matching, ServiceReference<S> service);

    /**
     * Manages the departure of a service.
     *
     * @param dep the dependency that see the service departure
     * @param matching the matching set of services for the given dependency
     * @param service the leaving service
     * @return a reordered subset of {@code matchingRefs} <em>minus</em> {@code service}
     */
    List<ServiceReference<S>> onServiceDeparture(DependencyModel dep, ServiceReferenceSet<S> matching, ServiceReference<S> service);

    /**
     * Closes the service ranking interception for the given dependency.
     *
     * @param dep the dependency that stops to use this interceptor.
     */
    void close(DependencyModel dep);

}
