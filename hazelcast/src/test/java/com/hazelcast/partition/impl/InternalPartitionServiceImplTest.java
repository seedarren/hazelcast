/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.partition.impl;

import com.hazelcast.cluster.ClusterState;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.nio.Address;
import com.hazelcast.partition.InternalPartitionService;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.hazelcast.partition.InternalPartition.MAX_REPLICA_COUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class InternalPartitionServiceImplTest extends HazelcastTestSupport {

    private HazelcastInstance instance = createHazelcastInstance();
    private InternalPartitionService partitionService = getPartitionService(instance);

    @Test(expected = HazelcastInstanceNotActiveException.class)
    public void test_getPartitionOwnerOrWait_throwsException_afterNodeShutdown() throws Exception {
        instance.shutdown();
        partitionService.getPartitionOwnerOrWait(0);
    }

    @Test
    public void test_initialAssignment() {
        partitionService.firstArrangement();

        int partitionCount = partitionService.getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            assertTrue(partitionService.isPartitionOwner(i));
        }
    }

    @Test
    public void test_initialAssignment_whenClusterNotActive() {
        instance.getCluster().changeClusterState(ClusterState.FROZEN);

        partitionService.firstArrangement();
        assertNull(partitionService.getPartitionOwner(0));
    }

    @Test(expected = IllegalStateException.class)
    public void test_getPartitionOwnerOrWait_whenClusterNotActive() {
        instance.getCluster().changeClusterState(ClusterState.FROZEN);

        partitionService.firstArrangement();
        partitionService.getPartitionOwnerOrWait(0);
    }

    @Test
    public void test_setInitialState() {
        Address thisAddress = getNode(instance).getThisAddress();
        int partitionCount = partitionService.getPartitionCount();
        Address[][] addresses = new Address[partitionCount][MAX_REPLICA_COUNT];
        for (int i = 0; i < partitionCount; i++) {
            addresses[i][0] = thisAddress;
        }

        InternalPartitionServiceImpl partitionServiceImpl = (InternalPartitionServiceImpl) partitionService;
        partitionServiceImpl.setInitialState(addresses);

        for (int i = 0; i < partitionCount; i++) {
            assertTrue(partitionService.isPartitionOwner(i));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void test_setInitialState_multipleTimes() {
        Address thisAddress = getNode(instance).getThisAddress();
        int partitionCount = partitionService.getPartitionCount();
        Address[][] addresses = new Address[partitionCount][MAX_REPLICA_COUNT];
        for (int i = 0; i < partitionCount; i++) {
            addresses[i][0] = thisAddress;
        }

        InternalPartitionServiceImpl partitionServiceImpl = (InternalPartitionServiceImpl) partitionService;
        partitionServiceImpl.setInitialState(addresses);

        partitionServiceImpl.setInitialState(addresses);
    }

    @Test
    public void test_setInitialState_listenerShouldNOTBeCalled() {
        Address thisAddress = getNode(instance).getThisAddress();
        int partitionCount = partitionService.getPartitionCount();
        Address[][] addresses = new Address[partitionCount][MAX_REPLICA_COUNT];
        for (int i = 0; i < partitionCount; i++) {
            addresses[i][0] = thisAddress;
        }

        InternalPartitionServiceImpl partitionServiceImpl = (InternalPartitionServiceImpl) partitionService;
        TestPartitionListener listener = new TestPartitionListener();
        partitionServiceImpl.addPartitionListener(listener);

        partitionServiceImpl.setInitialState(addresses);
        assertEquals(0, listener.eventCount);
    }

    private static class TestPartitionListener implements PartitionListener {
        private int eventCount;

        @Override
        public void replicaChanged(PartitionReplicaChangeEvent event) {
            eventCount++;
        }
    }
}
