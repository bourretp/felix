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
* A field interceptor is notified when a monitored field asks for a value or
* receives a new value. A class implementing this interface is able to be 
* notified of field accesses, and is able to inject a value to this field.
* The listener needs to be register on the instance manager. 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface FieldInterceptor {
    
    void onFieldAccess(FieldInvocationContext context, Object value) throws Throwable;

}
