// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.backup.backroll.model.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BackrollTaskStateTest {

    @Test
    public void of_ReadsTheStateField_Test() {
        assertEquals(TaskState.SUCCESS, TaskState.of("{\"state\":\"SUCCESS\",\"info\":\"PENDING FAILURE\"}"));
        assertEquals(TaskState.PENDING, TaskState.of("{\"state\":\"PENDING\",\"current\":0,\"total\":1}"));
    }

    @Test
    public void of_ReadsTheStateNestedInDetail_Test() {
        // This is how Backroll answers when a task failed
        assertEquals(TaskState.FAILURE, TaskState.of("{\"detail\":{\"state\":\"FAILURE\",\"status\":\"boom\"}}"));
    }

    @Test
    public void of_ReturnsEmptyWhenThereIsNoState_Test() {
        assertEquals("", TaskState.of(null));
        assertEquals("", TaskState.of(""));
        assertEquals("", TaskState.of("not json, but it says SUCCESS"));
        assertEquals("", TaskState.of("[\"PENDING\"]"));
        assertEquals("", TaskState.of("{\"info\":\"PENDING\"}"));
        assertEquals("", TaskState.of("{\"detail\":\"Not Found\"}"));
    }

    @Test
    public void isRunning_Test() {
        assertTrue(TaskState.isRunning(TaskState.PENDING));
        assertTrue(TaskState.isRunning(TaskState.PROGRESS));
        assertFalse(TaskState.isRunning(TaskState.SUCCESS));
        assertFalse(TaskState.isRunning(TaskState.FAILURE));
        assertFalse(TaskState.isRunning(""));
    }
}
