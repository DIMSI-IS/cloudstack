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
package org.apache.cloudstack.backup;

import org.junit.Assert;
import org.junit.Test;

public class BackupManagerTest {

    @Test
    public void validateBackupProviderConfigAcceptsKnownProviders() {
        for (String provider : new String[] {"dummy", "veeam", "networker", "nas", "backroll"}) {
            BackupManager.validateBackupProviderConfig(provider);
        }
    }

    @Test
    public void validateBackupProviderConfigAcceptsNull() {
        BackupManager.validateBackupProviderConfig(null);
    }

    @Test
    public void validateBackupProviderConfigRejectsUnknownProvider() {
        try {
            BackupManager.validateBackupProviderConfig("unknown");
            Assert.fail("An unknown backup provider should be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("backroll"));
        }
    }

    @Test
    public void validateBackupProviderConfigRejectsMultipleProviders() {
        for (String value : new String[] {"veeam,backroll", "veeam backroll"}) {
            try {
                BackupManager.validateBackupProviderConfig(value);
                Assert.fail("Several backup providers should be rejected: " + value);
            } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage().contains("Multiple backup provider plugins"));
            }
        }
    }
}
