package org.apache.felix.ipojo;

import org.osgi.framework.ServiceReference;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A sorted set service references.
 *
 * <p>
 *     Does <em>not</em> allow {@code null} and duplicated elements.
 * </p>
 *
 * @param <S>
 */
public class ServiceReferenceSet<S> extends AbstractList<ServiceReference<S>> {

    /**
     * The backing list.
     */
    private final List<ServiceReference<S>> m_list = new ArrayList<ServiceReference<S>>();

    // Recommended constructors

    public ServiceReferenceSet() {
    }

    public ServiceReferenceSet(Collection<? extends ServiceReference<S>> c) {
        this();
        addAll(c);
    }

    // Getters

    @Override
    public ServiceReference<S> get(int index) {
        return m_list.get(index);
    }

    @Override
    public int size() {
        return m_list.size();
    }

    // Setter that impose additional constraints on element insertion

    @Override
    public ServiceReference<S> set(int index, ServiceReference<S> element) {
        // CHECK!
        checkElement(element);
        return m_list.set(index, element);
    }

    // Structural modifiers

    @Override
    public void add(int index, ServiceReference<S> element) {
        // CHECK!
        checkElement(element);
        m_list.add(index, element);
    }

    @Override
    public ServiceReference<S> remove(int index) {
        return m_list.remove(index);
    }

    // Additional utility operations

    /**
     * Copies this service reference set.
     *
     * @return an independent copy of this service reference set.
     */
    public ServiceReferenceSet<S> copy() {
        ServiceReferenceSet<S> copy = new ServiceReferenceSet<S>();
        copy.addAll(this);
        return copy;
    }

    /**
     * Checks if the element can be inserted to this service reference set.
     *
     * @param element the element to check
     * @throws NullPointerException if {@code element} is {@code null}
     * @throws IllegalArgumentException if this service reference set already contains {@code element}
     */
    public void checkElement(ServiceReference<S> element) {
        if (element == null) {
            throw new NullPointerException("null element");
        } else if (contains(element)) {
            throw new IllegalArgumentException("duplicate element: " + element);
        }
    }


}
