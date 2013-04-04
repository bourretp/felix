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

/**
* Field interceptor.
* A class implementing this interface interceptor is able to be notified of field accesses,
* and is able to inject a value to this field.
* The listener needs to be register on the instance manager with the
 * {@link InstanceManager#register(org.apache.felix.ipojo.parser.FieldMetadata, FieldInterceptor)}
 * method.
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface FieldInterceptor {

    /**
     * Invoked when an intercepted field is accessed.
     *
     * <p>
     * Implementations <em>MUST</em> call {@code context.proceed()} unless no field
     * access can occur.
     *
     * @param context
     *          the intercepted field context.
     * @param value the value that should be read/written from/to the accessed field.
     * @throws Throwable
     *           the exception thrown by the field access (should be rare).
     */
    void onFieldAccess(FieldInvocationContext context, Object value) throws Throwable;

}
