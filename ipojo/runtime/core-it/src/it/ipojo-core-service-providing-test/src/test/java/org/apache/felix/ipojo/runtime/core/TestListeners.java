package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceListener;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;

import java.util.Hashtable;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class TestListeners extends Common {

    int registrations = 0;
    int unregistrations = 0;
    int updates = 0;
    int total = 0;

    private class CountingListener implements ProvidedServiceListener {
        public void serviceRegistered(ProvidedService providedService) {
            registrations++;
        }
        public void serviceModified(ProvidedService providedService) {
            updates++;
        }
        public void serviceUnregistered(ProvidedService providedService) {
            unregistrations++;
        }
    }

    private class TotalCountingListener implements ProvidedServiceListener {
        public void serviceRegistered(ProvidedService providedService) {
            total++;
        }
        public void serviceModified(ProvidedService providedService) {
            total++;
        }
        public void serviceUnregistered(ProvidedService providedService) {
            total++;
        }
    }

    private class ThrowingListener implements ProvidedServiceListener {
        public void serviceRegistered(ProvidedService providedService) {
            throw new RuntimeException("I'm bad");
        }
        public void serviceModified(ProvidedService providedService) {
            throw new RuntimeException("I'm bad");
        }
        public void serviceUnregistered(ProvidedService providedService) {
            throw new RuntimeException("I'm bad");
        }
    }

    @Test
    public void testProvidedServiceListener() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        ProvidedServiceHandlerDescription pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:provides");
        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());

        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        // Register listeners :
        // 1- CountingListener l1
        // 2- ThrowingListener bad
        // 3- TotalCountingListener l2
        ProvidedServiceListener l1 = new CountingListener();
        ps.addListener(l1);
        ProvidedServiceListener bad = new ThrowingListener();
        ps.addListener(bad);
        ProvidedServiceListener l2 = new TotalCountingListener();
        ps.addListener(l2);

        // Check initial valued are untouched
        assertEquals(0, registrations);
        assertEquals(0, unregistrations);
        assertEquals(0, updates);
        assertEquals(0, total);

        // Unregister the service and check.
        assertFalse(check.check());
        assertEquals(0, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);
        assertEquals(1, total);

        // Modify the service while it is unregistered. Nothing should move.
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("change1", "1");
        ps.addProperties(props);
        assertEquals(0, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);
        assertEquals(1, total);

        // Register the service and check.
        assertTrue(check.check());
        assertEquals(1, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);
        assertEquals(2, total);

        // Modify the service while it is REGISTERED
        props.clear();
        props.put("change2", "2");
        ps.addProperties(props);
        assertEquals(1, registrations);
        assertEquals(1, unregistrations);
        assertEquals(1, updates);
        assertEquals(3, total);

        // One more time, just to be sure...
        assertFalse(check.check()); // Unregister
        assertEquals(1, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(4, total);
        assertTrue(check.check()); // Register
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);

        // Unregister the listener
        ps.removeListener(l1);
        ps.removeListener(bad);
        ps.removeListener(l2);

        // Play with the controller and check that nothing moves
        assertFalse(check.check()); // Unregister
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);
        assertTrue(check.check()); // Register
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);
        props.clear(); props.put("change3", "3"); ps.addProperties(props); // Modify
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);

        ci.dispose();
    }

    @Test(expected = NullPointerException.class)
    public void testNullProvidedServiceListener() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        ProvidedServiceHandlerDescription pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:provides");
        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());

        // Should fail!
        ps.addListener(null);
    }

    @Test(expected = NoSuchElementException.class)
    public void testRemoveNonexistentProvidedServiceListener() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        ProvidedServiceHandlerDescription pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:provides");
        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());

        // Should fail!
        ps.removeListener(new ThrowingListener());
    }

    private ProvidedServiceDescription getPS(String itf, ProvidedServiceDescription[] svc) {
        for (int i = 0; i < svc.length; i++) {
            if (svc[i].getServiceSpecifications()[0].equals(itf)) {
                return svc[i];
            }
        }

        fail("Service : " + itf + " not found");
        return null;
    }
}
