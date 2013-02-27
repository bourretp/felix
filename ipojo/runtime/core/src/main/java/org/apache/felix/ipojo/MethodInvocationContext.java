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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.felix.ipojo.util.Property;

/**
 * Invocation context during a POJO method call.
 * 
 * @NotThreadSafe
 */
public final class MethodInvocationContext {

  // The list of method interceptors.
  private final List<MethodInterceptor> m_chain;
  // The current position within the chain.
  private int m_currentPosition = 0;

  /**
   * The POJO method that is being intercepted.
   */
  private Method m_method;

  /**
   * Some arbitrary data that can be passed through the interception chain.
   * 
   * <p>
   * Initialized on first demand.
   * 
   * @see MethodInvocationContext#getContextData()
   */
  private Map<String, Object> m_contextData;

  /**
   * The intercepted POJO.
   */
  private final Object m_pojo;

  /**
   * The list of the parameters for the POJO method call. Does not allow
   * structural modifications. Also checks the type of its element so they are
   * compatible with the method paramtere types.
   * 
   * <p>
   * Immutable after {@link #proceed()} has returned.
   * 
   * @see MethodInvocationContext#getParameters()
   * @see MethodParamList
   */
  private List<Object> m_params;

  /**
   * The flag indicating whether the parameter list is fixed or not.
   */
  private boolean m_paramsFixed = false;

  /**
   * Create a new method invocation context.
   * 
   * @param chain
   *          the chain of method interceptors.
   * @param pojo
   *          the intercepted POJO.
   * @param method
   *          the intercepted method.
   * @param params
   *          the initial method parameters.
   */
  MethodInvocationContext(List<MethodInterceptor> chain, Object pojo,
      Method method, Object[] params) {
    m_chain = chain;
    m_pojo = pojo;
    m_method = method;
    List<Class<?>> paramTypes = Collections.unmodifiableList(Arrays
        .asList(m_method.getParameterTypes()));
    m_params = new MethodParamList(paramTypes, Arrays.asList(params));
  }

  /**
   * @return the method that is being called.
   */
  public Method getMethod() {
    return m_method;
  }

  /**
   * Return the list of the method invocation parameters.
   * 
   * @return the parameters of the method invocation.
   */
  public List<Object> getParameters() {
    // Initialize the parameters
    if (m_params == null) {
      m_params = new ArrayList<Object>();
    }
    return m_params;
  }

  /**
   * Call the next interceptor in the chain, or call the actual POJO method.
   * 
   * @return the result of the method invocation.
   * @throws Throwable
   *           if the method has thrown an exception.
   */
  public Object proceed() throws Throwable {
    if (m_pojo != null) {
      throw new IllegalStateException(
          "MethodInvocationContext.proceed() called multiple times");
    }

    if (m_currentPosition == m_chain.size()) {
      // No more interceptors
      return doProceed();
    }

    // Get the next interceptor to call
    MethodInterceptor next = m_chain.get(m_currentPosition);
    m_currentPosition++;

    // Call the interceptor
    Object result;
    try {
      result = next.onMethodCall(this);
    } finally {
      fixParameters();
    }

    return result;
  }

  /**
   * Fix the parameters so they cannot be changed anymore.
   */
  private void fixParameters() {
    if (!m_paramsFixed) {
      // Only fix parameters if they haven't been fixed before.
      m_params = Collections.unmodifiableList(new ArrayList<Object>(m_params));
      m_paramsFixed = true;
    }
  }

  /**
   * The ultimate POJO method, executed at the end of the interception chain.
   * 
   * @return the result of the POJO method.
   * @throws Throwable
   *           if the POJO method has thrown an exception.
   */
  private Object doProceed() throws Throwable {

    // Fix the parameters.
    fixParameters();

    // Invoke the method
    return m_method.invoke(m_pojo, m_params.toArray());
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

  /**
   * List that forbids structural modifications (insertions/removal) and checks
   * element type according to its related method parameter.
   */
  private static class MethodParamList implements List<Object> {

    final List<Class<?>> m_paramTypes;
    final List<Object> m_params;

    MethodParamList(List<Class<?>> paramTypes, List<Object> params) {
      m_paramTypes = paramTypes;
      m_params = params;
    }

    public int size() {
      return m_params.size();
    }

    public boolean isEmpty() {
      return m_params.isEmpty();
    }

    public Object get(int index) {
      return m_params.get(index);
    }

    public Object set(int index, Object element) {
      Class<?> type = m_paramTypes.get(index);
      if (type.isPrimitive() && element == null) {
        throw new NullPointerException();
      } else if (!Property.isAssignable(type, element)) {
        throw new ClassCastException();
      }
      return m_params.set(index, element);
    }

    public int indexOf(Object o) {
      return m_params.indexOf(o);
    }

    public int lastIndexOf(Object o) {
      return m_params.lastIndexOf(o);
    }

    public boolean contains(Object o) {
      return m_params.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
      return m_params.containsAll(c);
    }

    public Object[] toArray() {
      return m_params.toArray();
    }

    public <T> T[] toArray(T[] a) {
      return m_params.toArray(a);
    }

    public List<Object> subList(int fromIndex, int toIndex) {
      List<Object> subParams = m_params.subList(fromIndex, toIndex);
      List<Class<?>> subParamsTypes = m_paramTypes.subList(fromIndex, toIndex);
      return new MethodParamList(subParamsTypes, subParams);
    }

    public Iterator<Object> iterator() {
      return new Iterator<Object>() {
        private final Iterator<? extends Object> i = m_params.iterator();

        public boolean hasNext() {
          return i.hasNext();
        }

        public Object next() {
          return i.next();
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    public ListIterator<Object> listIterator() {
      return listIterator(0);
    }

    public ListIterator<Object> listIterator(final int index) {
      return new ListIterator<Object>() {
        private final ListIterator<? extends Object> i = m_params
            .listIterator(index);

        public boolean hasNext() {
          return i.hasNext();
        }

        public Object next() {
          return i.next();
        }

        public boolean hasPrevious() {
          return i.hasPrevious();
        }

        public Object previous() {
          return i.previous();
        }

        public int nextIndex() {
          return i.nextIndex();
        }

        public int previousIndex() {
          return i.previousIndex();
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }

        public void set(Object e) {
          throw new UnsupportedOperationException();
        }

        public void add(Object e) {
          throw new UnsupportedOperationException();
        }
      };
    }

    // Forbidden write methods.

    public boolean add(Object e) {
      throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends Object> c) {
      throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection<? extends Object> c) {
      throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      throw new UnsupportedOperationException();
    }

    public void add(int index, Object element) {
      throw new UnsupportedOperationException();
    }

    public Object remove(int index) {
      throw new UnsupportedOperationException();
    }

  }

}
