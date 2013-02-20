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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.util.Property;

/**
 * Invocation context during a POJO field access.
 * 
 * @NotThreadSafe
 */
public final class FieldInvocationContext {

  /**
   * The field access types.
   * 
   * @see #getType()
   */
  public enum Type {
    READ, WRITE
  }

  /**
   * The instance manager handling the accessed POJO instance.
   */
  private final InstanceManager m_manager;

  // The list of field interceptors.
  private final List<FieldInterceptor> m_chain;
  // The current position within the chain.
  private int m_currentPosition = 0;

  /**
   * The field that is being accessed.
   */
  private Field m_field;
  
  /**
   * The value that has been set/get.
   * 
   * <p>
   * No set until the tip of the chain has been reached.
   */
  private Object m_finalValue = null;

  /**
   * Some arbitrary data that can be passed through the interception chain.
   * 
   * <p>
   * Initialized on first demand.
   * 
   * @see FieldInvocationContext#getContextData()
   */
  private Map<String, Object> m_contextData;

  /**
   * The accessed POJO.
   */
  private Object m_pojo;

  /**
   * The type of this field access.
   * 
   * @see #getType()
   */
  private Type m_type;

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
   * @param type
   *          the type of this field access.
   */
  FieldInvocationContext(InstanceManager manager, List<FieldInterceptor> chain,
      Object pojo, Type type) {
    m_manager = manager;
    m_chain = chain;
    m_pojo = pojo;
    m_type = type;
  }

  /**
   * @return the type of this field access.
   */
  public Type getType() {
    return m_type;
  }

  /**
   * @return the constructor that has been called, or {@code null} if no
   *         constructor has not been invoked yet.
   */
  public Field getField() {
    return m_field;
  }

  /**
   * Call the next interceptor in the chain, or proceed to the field access.
   * 
   * @param value
   *          the value that should be get/set, depending on the type of the
   *          field access.
   * @return the value that has effectively been get/set.
   * @throws Throwable
   *           if the constructor has thrown an exception.
   */
  public Object proceed(Object value) throws Throwable {
    
    // Check before!
    checkValue(value);

    if (m_currentPosition != m_chain.size()) {
      // Get the next interceptor to call
      FieldInterceptor next = m_chain.get(m_currentPosition);
      m_currentPosition++;

      // Call the interceptor
      next.onFieldAccess(this, value);

      // Check all the chain has proceeded
      if (m_currentPosition != m_chain.size()) {
        throw new IllegalStateException("Bad handler: " + next);
      }

    } else {
      // No more interceptors
      m_finalValue = value;
      m_manager.doSetField(m_pojo, m_field, m_finalValue);
    }

    return m_finalValue;
  }

  private void checkValue(Object value) {

    // Check that the value to get/set is compatible with the field type.
    Class<?> type = m_field.getType();
    if (type.isPrimitive()) {
      if (value == null) {
        throw new NullPointerException(
            "Cannot set a primitive typed field to null");
      } else if (!Property.isAssignable(type, value)) {
        throw new IllegalArgumentException(
            "Cannot set a primitive typed field to " + value);
      }
    } else if (value != null && !type.isInstance(value)) {
      throw new IllegalArgumentException("Incompatible field access: expected "
          + type.getName() + ", got " + value);
    }
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
