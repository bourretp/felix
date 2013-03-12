package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestWhiteBoard extends Common {

    String typeWI = "org.apache.felix.ipojo.runtime.core.test.components.whiteboard.WhiteBoardWIModification";
    String typeWO = "org.apache.felix.ipojo.runtime.core.test.components.whiteboard.WhiteBoardWOModification";
    String typeWhiteboards = "org.apache.felix.ipojo.runtime.core.test.components.whiteboard.WhiteBoards";
    String namespace = "org.apache.felix.ipojo.whiteboard";


    @Test
    public void testMetadataWithOnModification() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  typeWI);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("wbp", namespace);
        assertEquals("Check size", 1, ext.length);
        String filter = ext[0].getAttribute("filter");
        String onArr = ext[0].getAttribute("onArrival");
        String onDep = ext[0].getAttribute("onDeparture");
        String onMod = ext[0].getAttribute("onModification");


        assertEquals("Check filter", "(foo=true)", filter);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
        assertEquals("Check onModification", "onModification", onMod);

    }

    @Test
    public void testMetadataWithoutOnModification() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  typeWO);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("wbp", namespace);
        assertEquals("Check size", 1, ext.length);
        String filter = ext[0].getAttribute("filter");
        String onArr = ext[0].getAttribute("onArrival");
        String onDep = ext[0].getAttribute("onDeparture");
        String onMod = ext[0].getAttribute("onModification");


        assertEquals("Check filter", "(foo=true)", filter);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
        assertNull("Check onModification", onMod);

    }

    @Test
    public void testWhiteboards() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  typeWhiteboards);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("whiteboards", namespace);
        assertEquals("Check size", 1, ext.length);

        // Two sub-element
        Element[] wbps = ext[0].getElements("wbp", namespace);
        assertEquals("Check size", 2, wbps.length);

        String filter = wbps[0].getAttribute("filter");
        String onArr = wbps[0].getAttribute("onArrival");
        String onDep = wbps[0].getAttribute("onDeparture");
        String onMod = wbps[0].getAttribute("onModification");

        assertEquals("Check filter", "(foo=true)", filter);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
        assertNull("Check onModification", onMod);

        filter = wbps[1].getAttribute("filter");
        onArr = wbps[1].getAttribute("onArrival");
        onDep = wbps[1].getAttribute("onDeparture");
        onMod = wbps[1].getAttribute("onModification");

        assertEquals("Check filter", "(foo=true)", filter);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
        assertEquals("Check onModification", "onModification", onMod);
    }


}
