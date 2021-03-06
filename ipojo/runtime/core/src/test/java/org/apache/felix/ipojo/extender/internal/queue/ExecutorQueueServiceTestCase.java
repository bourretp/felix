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

package org.apache.felix.ipojo.extender.internal.queue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;
import java.util.concurrent.Future;

import org.apache.felix.ipojo.extender.internal.queue.callable.SleepingCallable;
import org.apache.felix.ipojo.extender.internal.queue.callable.StringCallable;
import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import junit.framework.TestCase;

/**
 * Checks the Executor Queue Service.
 */
public class ExecutorQueueServiceTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;

    @Mock
    private ServiceRegistration<?> m_registration;

    @Mock
    private Callback<String> m_callback;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testQueueServiceRegistration() throws Exception {
        ExecutorQueueService queueService = new ExecutorQueueService(m_bundleContext);

        Mockito.<ServiceRegistration<?>>when(m_bundleContext.registerService(eq(QueueService.class.getName()), eq(queueService), any(Dictionary.class))).thenReturn(m_registration);

        queueService.start();

        verify(m_bundleContext).registerService(eq(QueueService.class.getName()), eq(queueService), any(Dictionary.class));

        queueService.stop();

        verify(m_registration).unregister();

    }

    public void testCallbackIsInvoked() throws Exception {
        ExecutorQueueService queueService = new ExecutorQueueService(m_bundleContext);
        queueService.start();

        Future<String> future = queueService.submit(new StringCallable(), m_callback, "hello");

        // Wait for callable to finish
        assertEquals("hello", future.get());
        verify(m_callback).success(any(JobInfo.class), eq("hello"));
        verify(m_callback, never()).error(any(JobInfo.class), any(Exception.class));

        queueService.stop();
    }

    public void testStatistics() throws Exception {
        ExecutorQueueService queueService = new ExecutorQueueService(m_bundleContext, 2);
        queueService.start();

        // We create 4 job, so that we have the 2 first execution while the 2 others are waiting
        Future<String> one = queueService.submit(new SleepingCallable(50, "1"), m_callback, "First");
        Future<String> two = queueService.submit(new SleepingCallable(50, "2"), m_callback, "Second");
        Future<String> three = queueService.submit(new SleepingCallable(50, "3"), m_callback, "Third");
        Future<String> four = queueService.submit(new SleepingCallable(50, "4"), m_callback, "Fourth");

        // Wait for callable to finish
        one.get();
        two.get();
        assertEquals(2, queueService.getFinished());
        assertEquals(2, queueService.getCurrents());

        three.get();
        four.get();

        assertEquals(4, queueService.getFinished());
        assertEquals(0, queueService.getCurrents());
        assertEquals(0, queueService.getWaiters());

        queueService.stop();
    }

}


