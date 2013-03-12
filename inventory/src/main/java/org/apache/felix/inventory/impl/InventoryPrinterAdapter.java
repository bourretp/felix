/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.inventory.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.PrinterMode;
import org.apache.felix.inventory.ZipAttachmentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Helper class for a inventory printer.
 *
 * The adapter simplifies accessing and working with the inventory printer.
 */
public class InventoryPrinterAdapter implements InventoryPrinterHandler, Comparable
{

    /**
     * The name of the method to look for in non-InventoryPrinter services.
     *
     * @see #PRINTER_SIGNATURE
     */
    private static final String PRINTER_METHOD = "print";

    /**
     * The signature of the {@link #PRINTER_METHOD} method to be called.
     * This is similar to the
     * {@link InventoryPrinter#print(PrinterMode, PrintWriter, boolean)} method
     * where the first argument is the string value of the {@link PrinterMode}.
     *
     * @see InventoryPrinter#print(PrinterMode, PrintWriter, boolean)
     */
    private static final Class[] PRINTER_SIGNATURE = new Class[]
        { String.class, PrintWriter.class, Boolean.TYPE };

    /**
     * The name of the method to look for in non-ZipAttachmentProvider services.
     *
     * @see #ATTACHMENT_SIGNATURE
     */
    private static final String ATTACHMENT_METHOD = "addAttachments";

    /**
     * The signature of the {@link #ATTACHMENT_METHOD} method to be called.
     * This is similar to the
     * {@link ZipAttachmentProvider#addAttachments(String, ZipOutputStream)}
     * method.
     *
     * @see ZipAttachmentProvider#addAttachments(String, ZipOutputStream)
     */
    private static final Class[] ATTACHMENT_SIGNATURE = new Class[]
        { String.class, ZipOutputStream.class };

    /**
     * Formatter pattern to render the current time of inventory generation.
     */
    static final DateFormat DISPLAY_DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG,
        Locale.US);

    /**
     * Create a new adapter if the provided service is either a printer or
     * provides
     * the print method.
     *
     * @return An adapter or <code>null</code> if the method is missing.
     */
    public static InventoryPrinterAdapter createAdapter(final InventoryPrinterDescription description,
        final Object service)
    {

        Method printMethod = null;
        if (!(service instanceof InventoryPrinter))
        {

            // print(String, PrintWriter)
            printMethod = ClassUtils.searchMethod(service.getClass(), PRINTER_METHOD, PRINTER_SIGNATURE);
            if (printMethod == null)
            {
                return null;
            }
        }
        Method attachmentMethod = null;
        if (!(service instanceof ZipAttachmentProvider))
        {

            // addAttachments()
            attachmentMethod = ClassUtils.searchMethod(service.getClass(), ATTACHMENT_METHOD, ATTACHMENT_SIGNATURE);
        }
        return new InventoryPrinterAdapter(description, service, printMethod, attachmentMethod);
    }

    /**
     * Comparator for adapters based on the service ranking.
     */
    public static final Comparator RANKING_COMPARATOR = new Comparator()
    {

        public int compare(final Object o1, final Object o2)
        {
            return ((InventoryPrinterAdapter) o1).description.compareTo(((InventoryPrinterAdapter) o2).description);
        }
    };

    /** The Inventory printer service. */
    private final Object printer;

    /** The printer description. */
    private final InventoryPrinterDescription description;

    /** The method to use if printer does not implement the service interface. */
    private final Method printMethod;

    private final Method attachmentMethod;

    /** Service registration for the web console. */
    private ServiceRegistration registration;

    /**
     * Constructor.
     */
    public InventoryPrinterAdapter(final InventoryPrinterDescription description, final Object printer,
        final Method printMethod, final Method attachmentMethod)
    {
        this.description = description;
        this.printer = printer;
        this.printMethod = printMethod;
        this.attachmentMethod = attachmentMethod;
    }

    public void registerConsole(final BundleContext context, final InventoryPrinterManagerImpl manager)
    {
        if (this.registration == null)
        {
            final Object value = this.description.getServiceReference().getProperty(InventoryPrinter.CONFIG_WEBCONSOLE);
            if (value == null || !"false".equalsIgnoreCase(value.toString()))
            {
                this.registration = WebConsolePlugin.register(context, manager, this.description);
            }
        }
    }

    public void unregisterConsole()
    {
        if (this.registration != null)
        {
            this.registration.unregister();
            this.registration = null;
        }
    }

    /**
     * The human readable title for the inventory printer.
     */
    public String getTitle()
    {
        return this.description.getTitle();
    }

    /**
     * The unique name of the printer.
     */
    public String getName()
    {
        return this.description.getName();
    }

    /**
     * All supported modes.
     */
    public PrinterMode[] getModes()
    {
        return this.description.getModes();
    }

    /**
     * @see org.apache.felix.inventory.ZipAttachmentProvider#addAttachments(java.lang.String,
     *      java.util.zip.ZipOutputStream)
     */
    public void addAttachments(final String namePrefix, final ZipOutputStream zos) throws IOException
    {
        // check if printer implements ZipAttachmentProvider
        if (printer instanceof ZipAttachmentProvider)
        {
            ((ZipAttachmentProvider) printer).addAttachments(namePrefix, zos);
        }
        else if (this.attachmentMethod != null)
        {
            ClassUtils.invoke(this.printer, this.attachmentMethod, new Object[]
                { namePrefix, zos });
        }
    }

    /**
     * Whether the printer supports this mode.
     */
    public boolean supports(final PrinterMode mode)
    {
        for (int i = 0; i < this.description.getModes().length; i++)
        {
            if (this.description.getModes()[i] == mode)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(org.apache.felix.inventory.PrinterMode,
     *      java.io.PrintWriter)
     */
    public void print(final PrinterMode mode, final PrintWriter printWriter, final boolean isZip)
    {
        if (this.supports(mode))
        {
            if (this.printer instanceof InventoryPrinter)
            {
                ((InventoryPrinter) this.printer).print(mode, printWriter, isZip);
            }
            else
            {
                ClassUtils.invoke(this.printer, this.printMethod, new Object[]
                    { mode.toString(), printWriter, Boolean.valueOf(isZip) });
            }
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return printer.getClass() + "(" + super.toString() + ")";
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object spa)
    {
        return this.description.getSortKey().compareTo(((InventoryPrinterAdapter) spa).description.getSortKey());
    }

    public InventoryPrinterDescription getDescription()
    {
        return this.description;
    }
}
