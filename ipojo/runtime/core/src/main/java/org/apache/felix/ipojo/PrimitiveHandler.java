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
package org.apache.felix.ipojo;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Logger;


/**
* This class defines common mechanisms of primitive handlers.
* Primitive handlers are handler composing the container of primitive
* component instances (declared by the 'component' element inside the
* iPOJO descriptor).
* Note that this class also defines default method implementation.
* Classes overriding this class can change the behavior of those methods.
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public abstract class PrimitiveHandler extends Handler implements FieldInterceptor, MethodInterceptor,
    ConstructorInterceptor {

    /**
     * The "Primitive" Handler type (value).
     */
    public static final String HANDLER_TYPE = "primitive";

    /**
     * The reference on the instance manager.
     */
    private InstanceManager m_manager;


    /**
     * The factory of the instance manager.
     */
    private ComponentFactory m_factory;

    /**
     * Instance Logger used by the handler.
     */
    private Logger m_instanceLogger;

    /**
     * Attaches the current handler to the given instance.
     * This method must be called only once, and should not be overridden.
     * @param manager the instance on which the current handler will be attached.
     * @see org.apache.felix.ipojo.Handler#attach(org.apache.felix.ipojo.ComponentInstance)
     */
    protected void attach(ComponentInstance manager) {
        m_manager = (InstanceManager) manager;
        m_instanceLogger = m_manager.getLogger();
    }

    /**
     * Sets the factory of the managed instance.
     * @param factory the factory
     * @see org.apache.felix.ipojo.Handler#setFactory(org.apache.felix.ipojo.Factory)
     */
    public final void setFactory(Factory factory) {
        m_factory = (ComponentFactory) factory;
    }

    /**
     * Gets the logger of the managed instance.
     * IF no instance attached yet, use the factory logger.
     * @return the logger to use to log messages.
     * @see org.apache.felix.ipojo.Handler#getLogger()
     */
    public Logger getLogger() {
        if (m_instanceLogger == null) {
            return m_factory.getLogger();
        }
        return m_instanceLogger;
    }

    /**
     * Gets the instance manager managing the instance.
     * @return the instance manager
     */
    public InstanceManager getInstanceManager() {
        return m_manager;
    }

    /**
     * Gets the factory which creates the managed instance.
     * @return the factory which creates the managed instance.
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }

    /**
     * Gets the PojoMetadata of the content of the managed
     * instance. This method allows getting manipulation
     * metadata.
     * @return the information about the content of the
     * managed instance.
     */
    public PojoMetadata getPojoMetadata() {
        return m_factory.getPojoMetadata();
    }

    /**
     * Gets a plugged handler of the same container.
     * This method must be called only in the start method (or after).
     * In the configure method, this method can be inconsistent
     * as all handlers are not initialized.
     * @param name the name of the handler to find (class name or qualified
     * handler name (<code>ns:name</code>)).
     * @return the handler object or <code>null</code> if the handler is not found.
     */
    public final Handler getHandler(String name) {
        return m_manager.getHandler(name);
    }

    /**
     * Callback method called when an instance of the component is being created.
     * The default implementation just proceeds.
     * @param context the context of the instance construction
     */
    public void onConstructorCall(ConstructorInvocationContext context) throws Throwable {
      // Default implementation
      context.proceed();
    }

    /**
     * Callback method called when a field of an instance of the component is being accessed.
     * The default implementation just proceeds.
     * @param context the context of the field access
     * @param value the value
     */
    public void onFieldAccess(FieldInvocationContext context, Object value) throws Throwable {
      // Default implementation
      context.proceed(value);
    }

    /**
     * Callback method called when a field of an instance of the component is being accessed.
     * The default implementation just proceeds.
     * @param context the context of the method invocation
     * @return the result of the method invocation
     */
    public Object onMethodCall(MethodInvocationContext context) throws Throwable {
      // Default implementation
      return context.proceed();
    }

}
