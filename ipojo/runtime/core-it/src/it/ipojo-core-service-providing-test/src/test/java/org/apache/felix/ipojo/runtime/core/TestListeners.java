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

import static org.junit.Assert.*;

public class TestListeners extends Common {

    int registrations = 0;
    int unregistrations = 0;
    int updates = 0;

    private class Listener implements ProvidedServiceListener {
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

        // Create and register the listener
        ProvidedServiceListener l = new Listener();
        ps.addListener(l);

        // Check initial valued are untouched
        assertEquals(0, registrations);
        assertEquals(0, unregistrations);
        assertEquals(0, updates);

        // Unregister the service and check.
        assertFalse(check.check());
        assertEquals(0, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);

        // Modify the service while it is unregistered. Nothing should move.
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("change1", "1");
        ps.addProperties(props);
        assertEquals(0, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);

        // Register the service and check.
        assertTrue(check.check());
        assertEquals(1, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);

        // Modify the service while it is REGISTERED
        props.clear();
        props.put("change2", "2");
        ps.addProperties(props);
        assertEquals(1, registrations);
        assertEquals(1, unregistrations);
        assertEquals(1, updates);

        // One more time, just to be sure...
        assertFalse(check.check()); // Unregister
        assertEquals(1, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertTrue(check.check()); // Register
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);

        // Unregister the listener
        ps.removeListener(l);

        // Play with the controller and check that nothing moves
        assertFalse(check.check()); // Unregister
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertTrue(check.check()); // Register
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        props.clear(); props.put("change3", "3"); ps.addProperties(props); // Modify
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);

        ci.dispose();
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
