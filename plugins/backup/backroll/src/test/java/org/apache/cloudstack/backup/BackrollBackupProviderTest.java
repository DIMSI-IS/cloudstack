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

import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.backup.Backup.RestorePoint;
import org.apache.cloudstack.backup.backroll.BackrollClient;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollOffering;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClient;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClient.BackrollHttpClientException;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class BackrollBackupProviderTest {
    @Mock
    BackrollClient clientMock;

    @Spy
    @InjectMocks
    BackrollBackupProvider backupProvider;

    @Mock
    BackrollBackupProvider backupProviderMock;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Mock
    private ConfigKey<String> BackrollUrlConfigKey;

    @Mock
    private ConfigKey<String> BackrollAppNameConfigKey;

    @Mock
    private ConfigKey<String> BackrollPasswordConfigKey;

    @Mock
    BackupDao backupDao;

    @Before
    public void setUp() throws Exception {
        vmInstanceDao = mock(VMInstanceDao.class);
        clientMock = mock(BackrollClient.class);
        backupDao = mock(BackupDao.class);
        backupProvider = new BackrollBackupProvider(backupDao, vmInstanceDao, clientMock, Mockito.mock(Logger.class));
        backupProvider.BackrollAppNameConfigKey = BackrollAppNameConfigKey;
        backupProvider.BackrollPasswordConfigKey = BackrollPasswordConfigKey;
        backupProvider.BackrollUrlConfigKey = BackrollUrlConfigKey;
    }

    @Test
    public void listBackupOfferings_Test() throws BackrollHttpClientException, IOException {
        Mockito.doReturn(Arrays.asList(new BackrollOffering("dummyName", "dummyId"))).when(clientMock).getBackupOfferings();
        List<BackupOffering> results = backupProvider.listBackupOfferings(2L);
        assertTrue(results.size() == 1);
    }

    @Test
    public void takeBackup_Test() throws BackrollHttpClientException, IOException {

        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setInstanceName("test");
        vmInstanceVO.setDataCenterId(2l);
        vmInstanceVO.setUuid(UUID.randomUUID().toString());
        vmInstanceVO.setBackupOfferingId(2L);

        Mockito.doReturn("/status/f32092e4-3e8a-461b-8733-ed93e23fa782").when(clientMock).startBackupJob(Mockito.anyString());
        Mockito.doReturn(new BackupVO()).when(backupDao).persist(Mockito.any(BackupVO.class));
        Pair<Boolean, Backup> result = backupProvider.takeBackup(vmInstanceVO, false);
        assertTrue(result.first());
    }

    @Test
    public void restoreBackedUpVolume_Test() {
        try {
            var backup = new BackupVO();

            backupProvider.restoreBackedUpVolume(backup, new Backup.VolumeInfo("uuid", "path", Volume.Type.ROOT, (long)0, (long)0, "diskOfferingId", (long)0, (long)0),
                    "dummyString", "dummyString", new Pair<String, VirtualMachine.State>("dummyString", VirtualMachine.State.Shutdown));
        } catch (Exception e) {
            assertEquals(CloudRuntimeException.class, e.getClass());
            String expected = String.format("Backroll plugin does not support this feature");
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void getConfigKeys_Test() {
        assertEquals(3, backupProvider.getConfigKeys().length);
    }

    @Test
    public void getConfigComponentName_Test() {
        assertEquals(BackupService.class.getSimpleName(), backupProvider.getConfigComponentName());
    }

    @Test
    public void restoreVMFromBackupTrue_Test() throws BackrollHttpClientException, IOException {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setDataCenterId(2l);

        BackupVO backupVo = new BackupVO();
        backupVo.setExternalId("abc,defgh");

        Mockito.doReturn(true).when(clientMock).restoreVMFromBackup(Mockito.anyString(), Mockito.anyString());

        Boolean result = backupProvider.restoreVMFromBackup(vmInstanceVO, backupVo);

        assertTrue(result);
    }

    @Test
    public void restoreVMFromBackupFalse_Test() throws BackrollHttpClientException, IOException {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setDataCenterId(2l);

        BackupVO backupVo = new BackupVO();
        backupVo.setExternalId("abc,defgh");

        Mockito.doReturn(false).when(clientMock).restoreVMFromBackup(Mockito.anyString(), Mockito.anyString());

        try {
            backupProvider.restoreVMFromBackup(vmInstanceVO, backupVo);
        } catch (Exception e) {
            assertEquals(CloudRuntimeException.class, e.getClass());
            String expected = String.format("Failed to restore VM from Backup");
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void getDescription_Test() {
        assertEquals("Backroll Backup Plugin", backupProvider.getDescription());
    }

    @Test
    public void isValidProviderOffering_Test() {
        assertTrue(backupProvider.isValidProviderOffering(2L, "dummyString"));
    }

    @Test
    public void getName_Test() {
        assertEquals("backroll", backupProvider.getName());
    }

    @Test
    public void assignVMToBackupOffering_Test() {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        BackrollOffering backrollOf = new BackrollOffering("dummyName", UUID.randomUUID().toString());
        assertTrue(backupProvider.assignVMToBackupOffering(vmInstanceVO, backrollOf));
    }

    @Test
    public void removeVMFromBackupOffering_Test() {
        assertTrue(backupProvider.removeVMFromBackupOffering(new VMInstanceVO()));
    }

    @Test
    public void willDeleteBackupsOnOfferingRemoval_Test() {
        assertFalse(backupProvider.willDeleteBackupsOnOfferingRemoval());
    }

    @Test
    public void getClient_Test() {
        /*
         * BackrollClient client = backupProvider.getClient(2L); assertEquals(client,
         * clientMock);
         */
    }

    @Test
    public void deleteBackupTestSuccess_Test() throws BackrollHttpClientException, IOException {
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        vmInstanceVO.setInstanceName("test");
        vmInstanceVO.setDataCenterId(2l);
        vmInstanceVO.setUuid(UUID.randomUUID().toString());
        BackupVO backup = new BackupVO();
        backup.setVmId(1l);
        backup.setExternalId("abc,defgh");
        backup.setType("Full");
        backup.setZoneId(2l);
        backup.setStatus(Backup.Status.Removed);
        Boolean deleteBackupResponse = true;
        Mockito.doReturn(vmInstanceVO).when(vmInstanceDao).findByIdIncludingRemoved(Mockito.anyLong());
        Mockito.doReturn(deleteBackupResponse).when(clientMock).deleteBackup(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(new BackupVO()).when(backupDao).persist(Mockito.any(BackupVO.class));
        boolean result = backupProvider.deleteBackup(backup, true);
        assertEquals(true, result);
    }

    @Test
    public void deleteBackupBackinUp_Test() {
        BackupVO backup = new BackupVO();
        backup.setStatus(Backup.Status.BackingUp);
        try {
            backupProvider.deleteBackup(backup, false);
        } catch (Exception e) {
            assertEquals(CloudRuntimeException.class, e.getClass());
            String expected = String.format("You can't delete a backup while it still BackingUp");
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void listRestorePoints_Test() throws BackrollHttpClientException, IOException {
        List<RestorePoint> rps = Arrays.asList(new RestorePoint("rp1", new Date(), "incremental"), new RestorePoint("rp2", new Date(), "incremental"),
                new RestorePoint("rp3", new Date(), "incremental"), new RestorePoint("rp4", new Date(), "incremental"));

        VMInstanceVO vmInstanceVO3 = new VMInstanceVO();
        vmInstanceVO3.setInstanceName("test3");
        vmInstanceVO3.setDataCenterId(3l);
        vmInstanceVO3.setBackupOfferingId(3l);

        Mockito.doReturn(rps).when(clientMock).listRestorePoints(Mockito.anyString());

        List<RestorePoint> rPoints = backupProvider.listRestorePoints(vmInstanceVO3);

        assertEquals(rPoints.size(), rps.size());

    }

    @Test
    public void createNewBackupEntryForRestorePoint_Test() throws BackrollHttpClientException, IOException {
        RestorePoint restorePoint = new RestorePoint("restore-123", new Date(), "INCREMENTAL");

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-456");
        vm.setDataCenterId(2L);
        vm.setBackupOfferingId(3L);

        BackrollBackupMetrics backupMetrics = new BackrollBackupMetrics(200L, 100L);
        Mockito.doReturn(backupMetrics).when(clientMock).getBackupMetrics(vm.getUuid(), restorePoint.getId());

        Backup result = backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm);

        assertNotNull(result);
        assertEquals(Long.valueOf(vm.getId()), result.getVmId());
        assertEquals(restorePoint.getId(), result.getExternalId());
        assertEquals("INCREMENTAL", result.getType());
        assertEquals(restorePoint.getCreated(), result.getDate());
        assertEquals(Backup.Status.BackedUp, result.getStatus());
        assertEquals(3L, result.getBackupOfferingId());
        assertEquals(vm.getAccountId(), result.getAccountId());
        assertEquals(vm.getDomainId(), result.getDomainId());
        assertEquals(2L, result.getZoneId());
        assertEquals(Long.valueOf(200L), result.getSize());
        assertEquals(Long.valueOf(100L), result.getProtectedSize());
        Mockito.verify(backupDao).persist(Mockito.any(BackupVO.class));
    }

    @Test
    public void createNewBackupEntryForRestorePoint_NoMetrics_Test() throws BackrollHttpClientException, IOException {
        RestorePoint restorePoint = new RestorePoint("restore-789", new Date(), "INCREMENTAL");

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-789");
        vm.setDataCenterId(3L);
        vm.setBackupOfferingId(4L);

        Mockito.doReturn(null).when(clientMock).getBackupMetrics(vm.getUuid(), restorePoint.getId());

        Backup result = backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm);

        // Backroll did not give any metric, the entry is created anyway
        assertNotNull(result);
        assertEquals(restorePoint.getId(), result.getExternalId());
        assertEquals(Backup.Status.BackedUp, result.getStatus());
        assertEquals(4L, result.getBackupOfferingId());
        Mockito.verify(backupDao).persist(Mockito.any(BackupVO.class));
    }

    @Test
    public void createNewBackupEntryForRestorePoint_BackrollHttpClientException_Test() throws BackrollHttpClientException, IOException {

        RestorePoint restorePoint = new RestorePoint("restore-404", new Date(), "INCREMENTAL");

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-404");
        vm.setDataCenterId(4L);
        vm.setBackupOfferingId(5L);

        Mockito.doThrow(new BackrollHttpClient.BackrollHttpClientException(new Exception())).when(clientMock).getBackupMetrics(vm.getUuid(), restorePoint.getId());

        // A failure while fetching metrics must not prevent the backup entry from being created
        Backup result = backupProvider.createNewBackupEntryForRestorePoint(restorePoint, vm);

        assertNotNull(result);
        assertEquals(restorePoint.getId(), result.getExternalId());
        Mockito.verify(backupDao).persist(Mockito.any(BackupVO.class));
    }

    @Test
    public void syncBackupMetrics_SetsEachBackupOwnSize_Test() throws BackrollHttpClientException {
        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-sync");
        vm.setBackupOfferingId(1L);
        Mockito.doReturn(Arrays.asList(vm)).when(vmInstanceDao).listByZoneId(2L);

        BackupVO backup1 = new BackupVO();
        backup1.setExternalId("restore-1");
        backup1.setStatus(Backup.Status.BackedUp);
        BackupVO backup2 = new BackupVO();
        backup2.setExternalId("restore-2");
        backup2.setStatus(Backup.Status.BackedUp);
        Mockito.doReturn(Arrays.asList(backup1, backup2)).when(backupDao).listByVmId(Mockito.any(), Mockito.any());

        Mockito.doReturn(Arrays.asList(new RestorePoint("restore-1", new Date(), "INCREMENTAL"), new RestorePoint("restore-2", new Date(), "INCREMENTAL")))
                .when(clientMock).listRestorePoints("vm-uuid-sync");
        Mockito.doReturn(new BackrollBackupMetrics(100L, 10L)).when(clientMock).getBackupMetrics("vm-uuid-sync", "restore-1");
        Mockito.doReturn(new BackrollBackupMetrics(200L, 20L)).when(clientMock).getBackupMetrics("vm-uuid-sync", "restore-2");

        backupProvider.syncBackupMetrics(2L);

        // Each backup must get its own metrics, not the running total of the previous restore points
        assertEquals(Long.valueOf(100L), backup1.getSize());
        assertEquals(Long.valueOf(10L), backup1.getProtectedSize());
        assertEquals(Long.valueOf(200L), backup2.getSize());
        assertEquals(Long.valueOf(20L), backup2.getProtectedSize());
    }

    @Test
    public void supportsInstanceFromBackup_Test() {
        // restoreBackupToVM() is not implemented, so the feature must not be advertised
        assertFalse(backupProvider.supportsInstanceFromBackup());
    }

    @Test
    public void syncBackupMetrics_IgnoresVmsWithoutBackupOffering_Test() throws BackrollHttpClientException {
        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-no-offering");
        Mockito.doReturn(Arrays.asList(vm)).when(vmInstanceDao).listByZoneId(2L);

        backupProvider.syncBackupMetrics(2L);

        Mockito.verify(clientMock, Mockito.never()).listRestorePoints(Mockito.anyString());
        Mockito.verifyNoInteractions(backupDao);
    }

    @Test
    public void syncBackupMetrics_ContinuesAfterAFailure_Test() throws BackrollHttpClientException {
        VMInstanceVO failingVm = new VMInstanceVO();
        failingVm.setUuid("vm-uuid-failing");
        failingVm.setBackupOfferingId(1L);
        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-ok");
        vm.setBackupOfferingId(1L);
        Mockito.doReturn(Arrays.asList(failingVm, vm)).when(vmInstanceDao).listByZoneId(2L);

        BackupVO backup = new BackupVO();
        backup.setExternalId("restore-1");
        backup.setStatus(Backup.Status.BackedUp);
        Mockito.doReturn(Arrays.asList(backup)).when(backupDao).listByVmId(Mockito.any(), Mockito.any());

        Mockito.doThrow(new BackrollHttpClientException(new Exception("Backroll unreachable"))).when(clientMock).listRestorePoints("vm-uuid-failing");
        Mockito.doReturn(Arrays.asList(new RestorePoint("restore-1", new Date(), "INCREMENTAL"))).when(clientMock).listRestorePoints("vm-uuid-ok");
        Mockito.doReturn(new BackrollBackupMetrics(100L, 10L)).when(clientMock).getBackupMetrics("vm-uuid-ok", "restore-1");

        backupProvider.syncBackupMetrics(2L);

        // The failure of the first VM must not prevent the second one from being synchronized
        assertEquals(Long.valueOf(100L), backup.getSize());
    }

    private BackupVO backupToDelete() {
        BackupVO backup = new BackupVO();
        backup.setVmId(1L);
        backup.setExternalId("restore-1");
        backup.setStatus(Backup.Status.BackedUp);
        backup.setZoneId(2L);
        Mockito.doReturn(backup).when(backupDao).persist(Mockito.any(BackupVO.class));

        VMInstanceVO vm = new VMInstanceVO();
        vm.setUuid("vm-uuid-delete");
        Mockito.doReturn(vm).when(vmInstanceDao).findByIdIncludingRemoved(Mockito.anyLong());
        return backup;
    }

    @Test
    public void deleteBackup_Forced_WhenBackrollFails_Test() throws BackrollHttpClientException {
        BackupVO backup = backupToDelete();
        Mockito.doThrow(new BackrollHttpClientException(new Exception("Backroll unreachable"))).when(clientMock).deleteBackup("vm-uuid-delete", "restore-1");

        assertTrue(backupProvider.deleteBackup(backup, true));
        assertEquals(Backup.Status.Removed, backup.getStatus());
    }

    @Test
    public void deleteBackup_Forced_WhenBackrollDoesNotDelete_Test() throws BackrollHttpClientException {
        BackupVO backup = backupToDelete();
        Mockito.doReturn(false).when(clientMock).deleteBackup("vm-uuid-delete", "restore-1");

        assertTrue(backupProvider.deleteBackup(backup, true));
        assertEquals(Backup.Status.Removed, backup.getStatus());
    }

    @Test
    public void deleteBackup_NotForced_WhenBackrollFails_Test() throws BackrollHttpClientException {
        BackupVO backup = backupToDelete();
        Mockito.doThrow(new BackrollHttpClientException(new Exception("Backroll unreachable"))).when(clientMock).deleteBackup("vm-uuid-delete", "restore-1");

        assertThrows(CloudRuntimeException.class, () -> backupProvider.deleteBackup(backup, false));
        assertEquals(Backup.Status.BackedUp, backup.getStatus());
    }

    @SuppressWarnings("unchecked")
    private BackrollBackupProvider providerWithoutClient(String url, String appName, String password) {
        BackrollBackupProvider provider = new BackrollBackupProvider(backupDao, vmInstanceDao, null, Mockito.mock(Logger.class));
        provider.BackrollUrlConfigKey = Mockito.mock(ConfigKey.class);
        provider.BackrollAppNameConfigKey = Mockito.mock(ConfigKey.class);
        provider.BackrollPasswordConfigKey = Mockito.mock(ConfigKey.class);
        Mockito.doReturn(url).when(provider.BackrollUrlConfigKey).valueIn(2L);
        Mockito.doReturn(appName).when(provider.BackrollAppNameConfigKey).valueIn(2L);
        Mockito.doReturn(password).when(provider.BackrollPasswordConfigKey).valueIn(2L);
        return provider;
    }

    @Test
    public void getClient_GivenClientIsUsedAsIs_Test() {
        assertSame(clientMock, backupProvider.getClient(2L));
    }

    @Test
    public void getClient_RebuildsTheClientWhenTheSettingsChange_Test() {
        BackrollBackupProvider provider = providerWithoutClient("http://backroll.test:5050/api/v1", "app", "secret");

        BackrollClient client = provider.getClient(2L);
        assertNotNull(client);
        assertSame(client, provider.getClient(2L));

        Mockito.doReturn("other-secret").when(provider.BackrollPasswordConfigKey).valueIn(2L);
        assertNotSame(client, provider.getClient(2L));
    }

    @Test
    public void getClient_NotConfigured_Test() {
        BackrollBackupProvider provider = providerWithoutClient("http://backroll.test:5050/api/v1", "app", "");

        assertThrows(CloudRuntimeException.class, () -> provider.getClient(2L));
    }
}
