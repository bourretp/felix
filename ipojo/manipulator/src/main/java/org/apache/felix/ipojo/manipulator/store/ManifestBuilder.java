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
package org.apache.felix.ipojo.manipulator.store;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.render.MetadataFilter;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@code ManifestBuilder} is ...
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ManifestBuilder {
    /**
     * Add all given package names in the referred packages list
     * @param packageNames additional packages
     */
    void addReferredPackage(Set<String> packageNames);

    /**
     * Add all given metadata
     * @param metadatas additional metadata
     */
    void addMetada(Collection<Element> metadatas);

    /**
     * Update the given manifest.
     * @param original original manifest to be modified
     * @return modified manifest
     */
    Manifest build(Manifest original);
}
