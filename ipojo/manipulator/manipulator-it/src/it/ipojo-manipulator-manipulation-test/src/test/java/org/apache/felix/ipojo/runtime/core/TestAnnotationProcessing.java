package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.runtime.core.components.Marker;
import org.apache.felix.ipojo.runtime.core.components.SubMarker;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestAnnotationProcessing extends Common {

    private Class clazz;

    @Before
    public void setUp() {
        try {
            clazz = bc.getBundle().
                    loadClass("org.apache.felix.ipojo.runtime.core.components.Annotation");
        } catch (ClassNotFoundException e) {
            fail("Cannot load the annotation class : " + e.getMessage());
        }
    }


    @Test
    public void testAnnotationOnMethod() {
        Method method = null;
        try {
            method = this.clazz.getMethod("doSomething", new Class[0]);
        } catch (Exception e) {
            fail("Cannot find the doSomething method : " + e.getMessage());
        }
        assertNotNull("Check method existence", method);

        java.lang.annotation.Annotation[] annotations = method.getDeclaredAnnotations();
        assertNotNull("Check annotations size - 1", annotations);
        assertEquals("Check annotations size - 2", 2, annotations.length); // Invisible is not visible
        
        /*
            @Marker(name="marker", type=Type.BAR, 
            sub=@SubMarker(subname="foo"),
            arrayOfObjects={"foo", "bar", "baz"},
            arrayOfAnnotations= {@SubMarker(subname="foo")}
            )
            @SubMarker(subname="bar")
            @Invisible
         */

        Marker marker = getMarkerAnnotation(annotations);
        assertNotNull("Check marker", marker);

        assertEquals("Check marker name", "marker", marker.name());
        assertEquals("Check marker type", Marker.Type.BAR, marker.type());
        assertEquals("Check sub marker attribute", "foo", marker.sub().subname());
        assertEquals("Check objects [0]", "foo", marker.arrayOfObjects()[0]);
        assertEquals("Check objects [1]", "bar", marker.arrayOfObjects()[1]);
        assertEquals("Check objects [2]", "baz", marker.arrayOfObjects()[2]);
        assertEquals("Check annotations[0]", "foo", marker.arrayOfAnnotations()[0].subname());

        SubMarker sub = getSubMarkerAnnotation(annotations);
        assertNotNull("Check submarker", sub);
        assertEquals("Check submarker", "bar", sub.subname());

    }

    @Test
    public void testAnnotationOnConstructor() {
        Constructor method = null;
        try {
            method = clazz.getConstructor(new Class[0]);
        } catch (Exception e) {
            fail("Cannot find the constructor method : " + e.getMessage());
        }
        assertNotNull("Check method existence", method);

        java.lang.annotation.Annotation[] annotations = method.getDeclaredAnnotations();
        assertNotNull("Check annotations size - 1", annotations);
        assertEquals("Check annotations size - 2", 2, annotations.length); // Invisible is not visible
        
        /*
            @Marker(name="marker", type=Type.BAR, 
            sub=@SubMarker(subname="foo"),
            arrayOfObjects={"foo", "bar", "baz"},
            arrayOfAnnotations= {@SubMarker(subname="foo")}
            )
            @SubMarker(subname="bar")
            @Invisible
         */

        Marker marker = getMarkerAnnotation(annotations);
        assertNotNull("Check marker", marker);

        assertEquals("Check marker name", "marker", marker.name());
        assertEquals("Check marker type", Marker.Type.BAR, marker.type());
        assertEquals("Check sub marker attribute", "foo", marker.sub().subname());
        assertEquals("Check objects [0]", "foo", marker.arrayOfObjects()[0]);
        assertEquals("Check objects [1]", "bar", marker.arrayOfObjects()[1]);
        assertEquals("Check objects [2]", "baz", marker.arrayOfObjects()[2]);
        assertEquals("Check annotations[0]", "foo", marker.arrayOfAnnotations()[0].subname());

        SubMarker sub = getSubMarkerAnnotation(annotations);
        assertNotNull("Check submarker", sub);
        assertEquals("Check submarker", "bar", sub.subname());
    }

    @Test
    public void testParameterAnnotations() {
        Method method = null;
        try {
            method = this.clazz.getMethod("doSomethingWithParams", new Class[]{String.class, String.class, String.class});
        } catch (Exception e) {
            fail("Cannot find the doSomethingWithParams method : " + e.getMessage());
        }
        assertNotNull("Check method existence", method);

        java.lang.annotation.Annotation[][] annotations = method.getParameterAnnotations();
        assertNotNull("Check annotations size - 1", annotations);
        assertEquals("Check annotations size - 3", 3, annotations.length);

        // Check internals
        // First parameter (foo)
        java.lang.annotation.Annotation[] fooAnns = annotations[0];
        assertEquals("Check fooAnns length", 1, fooAnns.length);
        Marker marker = (Marker) fooAnns[0];
        assertNotNull("Check marker", marker);
        assertEquals("Check marker name", "marker", marker.name());
        assertEquals("Check marker type", Marker.Type.BAR, marker.type());
        assertEquals("Check sub marker attribute", "foo", marker.sub().subname());
        assertEquals("Check objects [0]", "foo", marker.arrayOfObjects()[0]);
        assertEquals("Check objects [1]", "bar", marker.arrayOfObjects()[1]);
        assertEquals("Check objects [2]", "baz", marker.arrayOfObjects()[2]);
        assertEquals("Check annotations[0]", "foo", marker.arrayOfAnnotations()[0].subname());

        // Second parameter (bar), no annotation (invisible)
        java.lang.annotation.Annotation[] barAnns = annotations[1];
        assertEquals("Check barAnns length", 0, barAnns.length);

        // Third parameter (baz), two annotations
        java.lang.annotation.Annotation[] bazAnns = annotations[2];
        System.out.println(Arrays.toString(bazAnns));
        assertEquals("Check bazAnns length", 2, bazAnns.length);
    }

    private Marker getMarkerAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().getName().equals("org.apache.felix.ipojo.runtime.core.components.Marker")) {
                return (Marker) annotations[i];
            }
        }
        return null;
    }

    private SubMarker getSubMarkerAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().getName().equals("org.apache.felix.ipojo.runtime.core.components.SubMarker")) {
                return (SubMarker) annotations[i];
            }
        }
        return null;
    }

}
