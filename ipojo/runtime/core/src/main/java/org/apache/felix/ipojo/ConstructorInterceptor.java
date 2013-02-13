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
 * Interface implemented to support constructor parameter injection. When a new
 * POJO object has to be created, all constructor injectors are called to gets
 * the objects to injects as well as the type (to discover the constructor).
 * Handlers willing to inject constructor parameters must register themselves
 * using {@link InstanceManager#register(int, ConstructorInjector)} where the
 * integer is the parameter index. Only one injector can inject a value for a
 * specific index. If several injectors are registered for the same index, the
 * component type is declared as invalid.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ConstructorInterceptor {

  /**
   * Invoked when an intercepted constructor is being called.
   * 
   * <p>
   * Implementations <em>MUST</em> call {@code context.proceed()} unless no POJO
   * can actually be created.
   * 
   * @param context
   *          the context of the POJO construction.
   * @throws Throwable
   *           the exception thrown by the POJO construction.
   */
  void onConstructorCall(ConstructorInvocationContext context) throws Throwable;

}
