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
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
* Method interceptor.
* A class implementing this interface is able to be notified of method invocations (
* i.e. entries, exits, and errors).
* The listener needs to be register on the instance manager with the
* {@link InstanceManager#register(org.apache.felix.ipojo.parser.MethodMetadata, MethodInterceptor)}
* method.
* Events are sent before the method entry (onEntry), after the method returns (onExit),
* when an error is thrown by the method (onError), and before the after either a returns or an error (onFinally).
*
* Instead of a {@link Method} object, the callbacks received a {@link Member} object which can be either a {@link Method}
* or a {@link Constructor}.
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface MethodInterceptor {

    Object onMethodCall(MethodInvocationContext methodInvocationContext) throws Throwable;

}
