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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Property;
import org.osgi.framework.BundleContext;

/**
 * Invocation context during a POJO construction.
 * 
 * @NotThreadSafe
 */
public final class ConstructorInvocationContext {

  /**
   * The instance manager creating the POJO instance.
   */
  private final InstanceManager m_manager;

  // The list of constructor interceptors.
  private final List<ConstructorInterceptor> m_chain;
  // The current position within the chain.
  private int m_currentPosition = 0;

  private final boolean m_doCallConstructor;

  /**
   * The constructor that has been invoked to create the POJO. {@code null}
   * until the chain has called the last interceptor of the chain.
   */
  private Constructor<?> m_constructor;

  /**
   * Some arbitrary data that can be passed through the interception chain.
   * 
   * <p>
   * Initialized on first demand.
   * 
   * @see ConstructorInvocationContext#getContextData()
   */
  private Map<String, Object> m_contextData;

  /**
   * The constructed POJO. {@code null} before the POJO is created.
   */
  private Object m_pojo = null;

  /**
   * The list of the parameters for the POJO constructor.
   * 
   * <p>
   * Initialized on first demand.
   * 
   * @see ConstructorInvocationContext#getParameters()
   */
  private List<Object> m_params = null;

  /**
   * Create a new constructor invocation context.
   * 
   * @param manager
   *          the instance manager creating the POJO instance.
   * @param chain
   *          the chain of constructor interceptors.
   */
  ConstructorInvocationContext(InstanceManager manager,
      List<ConstructorInterceptor> chain) {
    m_manager = manager;
    m_chain = chain;
    m_doCallConstructor = true;
  }

  /**
   * Create a new pseudo-constructor invocation context. The POJO is already
   * created, so no constructor is actually invoked.
   * 
   * @param manager
   *          the instance manager creating the POJO instance.
   * @param chain
   *          the chain of constructor interceptors.
   * @param pojo
   *          the POJO
   */
  ConstructorInvocationContext(InstanceManager manager,
      List<ConstructorInterceptor> chain, Object pojo) {
    m_manager = manager;
    m_chain = chain;
    m_pojo = pojo;
    m_doCallConstructor = false;
  }

  /**
   * @return the constructor that has been called, or {@code null} if no
   *         constructor has not been invoked yet.
   */
  public Constructor<?> getConstructor() {
    return m_constructor;
  }

  /**
   * Return the list of the constructor invocation parameters.
   * 
   * @return the parameters of the constructor invocation.
   */
  public List<Object> getParameters() {
    // Initialize the parameters
    if (m_params == null) {
      m_params = new ArrayList<Object>();
    }
    return m_params;
  }

  /**
   * Call the next interceptor int the chain, or find and call a suitable POJO
   * constructor.
   * 
   * 
   * Proceed to the constructor invocation.
   * 
   * @return the created POJO object created by the constructor invocation.
   * @throws Throwable
   *           if the constructor has thrown an exception.
   */
  public Object proceed() throws Throwable {
    if (m_pojo != null && m_doCallConstructor) {
      throw new IllegalStateException(
          "ConstructorInvocationContext.proceed() called multiple times");
    }

    if (m_currentPosition == m_chain.size()) {
      // No more interceptors
      return doProceed();
    }

    // Get the next interceptor to call
    ConstructorInterceptor next = m_chain.get(m_currentPosition);
    m_currentPosition++;

    // Call the interceptor
    next.onConstructorCall(this);

    // ... and check the POJO has been constructed
    if (m_pojo == null) {
      throw new IllegalStateException(
          "constructor interceptor MUST call ConstructorInvocationContext.proceed(): "
              + next);
    }

    return m_pojo;
  }

  /**
   * The ultimate POJO constructor method, executed at the end of the
   * interception chain.
   * 
   * @return the constructed POJO.
   * @throws Throwable
   *           if the POJO cannot be constructed for any reason.
   */
  private Object doProceed() throws Throwable {

    // Inject the instance manager as the first parameter.
    getParameters().add(0, m_manager);

    if (!m_doCallConstructor) {
      // Do not create the POJO, as a custom POJO is being injected.
      return m_pojo;
    }

    // Find the suitable constructor.
    m_constructor = findSuitableConstructor();
    if (!m_constructor.isAccessible()) {
      m_constructor.setAccessible(true);
    }
    String methodId = MethodMetadata.computeMethodId(m_constructor);

    // Fix the parameters.
    m_params = Collections.unmodifiableList(new ArrayList<Object>(m_params));

    // Invoke the constructor
    Object[] params = m_params.toArray();
    m_manager.onEntry(null, methodId, params);
    m_pojo = m_constructor.newInstance(params);
    m_manager.onExit(m_pojo, methodId, m_pojo);

    return m_pojo;
  }

  // Find the constructor to invoke, according to the parameters.
  private Constructor findSuitableConstructor() throws NoSuchMethodException {
    List<Constructor> ctors = new ArrayList<Constructor>(
        Arrays.asList(m_manager.getClazz().getDeclaredConstructors()));

    // Filter out incompatible constructors.
    next_ctor: for (Iterator<Constructor> i = ctors.iterator(); i.hasNext();) {

      Constructor ctor = i.next();
      Class[] ctorParamTypes = ctor.getParameterTypes();

      // Number of parameters must at least size of m_params.
      // AND First parameter MUST be the instance manager.
      if (ctorParamTypes.length < m_params.size()
          || ctorParamTypes[0] != InstanceManager.class) {
        i.remove();
        continue;
      }

      // Copy list and fill with null values until the expected number of
      // constructor parameters is reached.
      List<Object> params = new ArrayList<Object>(m_params);
      while (params.size() < ctorParamTypes.length) {
        params.add(null);
      }

      // Check parameters are compatible with constructor expected ones.
      for (int j = 0; j < ctorParamTypes.length; j++) {
        if (!Property.isAssignable(ctorParamTypes[j], params.get(j))) {
          i.remove();
          continue next_ctor;
        }
      }
    }

    if (ctors.isEmpty()) {
      // No suitable constructor found!
      // TODO more details
      throw new NoSuchMethodException();
    } else if (ctors.size() > 1) {
      // Ambiguous constructor invocation!

      // Try to sort the candidates according to the expected parameter number.
      // The less parameters are, the best the constructor is.
      // The sort method do not modify order of "equals" elements, so the
      // constructor declaration order is kept.
      int ref = m_params.size();
      Collections.sort(ctors, new Comparator<Constructor>() {
        public int compare(Constructor c1, Constructor c2) {
          return c1.getParameterTypes().length - c2.getParameterTypes().length;
        }
      });
      
      // Warn, because choosing a constructor and randomness are not so different.
      m_manager.getLogger().log(
          Logger.WARNING,
          "Multiple constructors match for parameters " + m_params.toString()
              + ": " + ctors);
    }

    // At last, constructor has been chosen!!!!!
    Constructor ctor = ctors.get(0);
    
    // Fill m_params with null values until the expected number of the chosen
    // constructor
    // parameters is reached.
    Class[] ctorParamTypes = ctor.getParameterTypes();
    while (m_params.size() < ctorParamTypes.length) {
      m_params.add(null);
    }

    // Handle BundleContext injection.
    for (int j = 0; j < ctorParamTypes.length; j++) {
      if (ctorParamTypes[j] == BundleContext.class && m_params.get(j) == null) {
        m_params.set(j, m_manager.getContext());
        break;
      }
    }

    return ctor;
  }

  /**
   * @return a map of arbitrary data that can be passed through the interception
   *         chain. The map is modifiable.
   */
  public Map<String, Object> getContextData() {
    if (m_contextData == null) {
      m_contextData = new HashMap<String, Object>();
    }
    return m_contextData;
  }

}
