/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.cm.impl.helper;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.cm.impl.CaseInsensitiveDictionary;
import org.apache.felix.cm.impl.ConfigurationImpl;
import org.apache.felix.cm.impl.ConfigurationManager;
import org.apache.felix.cm.impl.RankingComparator;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>BaseTracker</code> is the base class for tracking
 * <code>ManagedService</code> and <code>ManagedServiceFactory</code>
 * services. It maps their <code>ServiceRegistration</code> to the
 * {@link ConfigurationMap} mapping their service PIDs to provided
 * configuration.
 */
public abstract class BaseTracker<S> extends ServiceTracker<S, ConfigurationMap>
{
    protected final ConfigurationManager cm;

    private final boolean managedServiceFactory;


    protected BaseTracker( final ConfigurationManager cm, final boolean managedServiceFactory )
    {
        super( cm.getBundleContext(), ( managedServiceFactory ? ManagedServiceFactory.class.getName()
            : ManagedService.class.getName() ), null );
        this.cm = cm;
        this.managedServiceFactory = managedServiceFactory;
        open();
    }


    public ConfigurationMap addingService( ServiceReference<S> reference )
    {
        final String[] pids = getServicePid( reference );
        configure( reference, pids );
        return new ConfigurationMap( pids );
    }


    @Override
    public void modifiedService( ServiceReference<S> reference, ConfigurationMap service )
    {
        String[] pids = getServicePid( reference );
        if ( service.isDifferentPids( pids ) )
        {
            configure( reference, pids );
            service.setConfiguredPids( pids );
        }
    }


    private void configure( ServiceReference<S> reference, String[] pids )
    {
        if ( pids != null )
        {

            /*
             * We get the service here for performance reasons only.
             * Assuming a service is registered as a ServiceFactory with
             * multiple PIDs, it may be instantiated and destroyed multiple
             * times during its inital setup phase. By getting it first
             * and ungetting it at the end we prevent this cycling ensuring
             * all updates go the same service instance.
             */

            S service = getRealService( reference );
            if ( service != null )
            {
                try
                {
                    if ( this.cm.isLogEnabled( LogService.LOG_DEBUG ) )
                    {
                        this.cm.log( LogService.LOG_DEBUG, "configure(ManagedService {0})", new Object[]
                            { ConfigurationManager.toString( reference ) } );
                    }

                    for ( int i = 0; i < pids.length; i++ )
                    {
                        this.cm.configure( pids[i], reference, managedServiceFactory );
                    }
                }
                finally
                {
                    ungetRealService( reference );
                }
            }
        }
    }


    public final List<ServiceReference<S>> getServices( final TargetedPID pid )
    {
        ServiceReference<S>[] refs = this.getServiceReferences();
        if ( refs != null )
        {
            ArrayList<ServiceReference<S>> result = new ArrayList<ServiceReference<S>>( refs.length );
            for ( ServiceReference<S> ref : refs )
            {
                ConfigurationMap map = this.getService( ref );
                if ( map != null
                    && ( map.accepts( pid.getRawPid() ) || ( map.accepts( pid.getServicePid() ) && pid
                        .matchesTarget( ref ) ) ) )
                {
                    result.add( ref );
                }
            }

            if ( result.size() > 1 )
            {
                Collections.sort( result, RankingComparator.SRV_RANKING );
            }

            return result;
        }

        return Collections.emptyList();
    }


    // Updates
    public abstract void provideConfiguration( ServiceReference<S> service, ConfigurationImpl config,
        Dictionary<String, ?> properties );


    public abstract void removeConfiguration( ServiceReference<S> service, ConfigurationImpl config );


    protected final S getRealService( ServiceReference<S> reference )
    {
        return this.context.getService( reference );
    }


    protected final void ungetRealService( ServiceReference<S> reference )
    {
        this.context.ungetService( reference );
    }


    protected final Dictionary getProperties( Dictionary<String, ?> rawProperties, String targetPid,
        ServiceReference service )
    {
        Dictionary props = new CaseInsensitiveDictionary( rawProperties );
        this.cm.callPlugins( props, targetPid, service, null /* config */);
        return props;
    }


    protected final void handleCallBackError( final Throwable error, final ServiceReference target,
        final ConfigurationImpl config )
    {
        if ( error instanceof ConfigurationException )
        {
            final ConfigurationException ce = ( ConfigurationException ) error;
            if ( ce.getProperty() != null )
            {
                this.cm.log( LogService.LOG_ERROR,
                    "{0}: Updating property {1} of configuration {2} caused a problem: {3}", new Object[]
                        { ConfigurationManager.toString( target ), ce.getProperty(), config.getPid(), ce.getReason(),
                            ce } );
            }
            else
            {
                this.cm.log( LogService.LOG_ERROR, "{0}: Updating configuration {1} caused a problem: {2}",
                    new Object[]
                        { ConfigurationManager.toString( target ), config.getPid(), ce.getReason(), ce } );
            }
        }
        else
        {
            {
                this.cm.log( LogService.LOG_ERROR, "{0}: Unexpected problem updating configuration {1}", new Object[]
                    { ConfigurationManager.toString( target ), config, error } );
            }

        }
    }


    /**
     * Returns the <code>service.pid</code> property of the service reference as
     * an array of strings or <code>null</code> if the service reference does
     * not have a service PID property.
     * <p>
     * The service.pid property may be a single string, in which case a single
     * element array is returned. If the property is an array of string, this
     * array is returned. If the property is a collection it is assumed to be a
     * collection of strings and the collection is converted to an array to be
     * returned. Otherwise (also if the property is not set) <code>null</code>
     * is returned.
     *
     * @throws NullPointerException
     *             if reference is <code>null</code>
     * @throws ArrayStoreException
     *             if the service pid is a collection and not all elements are
     *             strings.
     */
    private static String[] getServicePid( ServiceReference reference )
    {
        Object pidObj = reference.getProperty( Constants.SERVICE_PID );
        if ( pidObj instanceof String )
        {
            return new String[]
                { ( String ) pidObj };
        }
        else if ( pidObj instanceof String[] )
        {
            return ( String[] ) pidObj;
        }
        else if ( pidObj instanceof Collection )
        {
            Collection pidCollection = ( Collection ) pidObj;
            return ( String[] ) pidCollection.toArray( new String[pidCollection.size()] );
        }

        return null;
    }

}