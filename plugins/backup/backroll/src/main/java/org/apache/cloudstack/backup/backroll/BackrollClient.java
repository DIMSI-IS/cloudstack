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
package org.apache.cloudstack.backup.backroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollOffering;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.model.response.BackrollTaskRequestResponse;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.backroll.model.response.archive.BackrollBackupsFromVMResponse;
import org.apache.cloudstack.backup.backroll.model.response.backup.BackrollBackupStatusResponse;
import org.apache.cloudstack.backup.backroll.model.response.backup.BackrollBackupStatusSuccessResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.backup.BackrollBackupMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.BackrollVmMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.CacheStats;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.BackupInfos;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.VirtualMachineBackupsResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackrollBackupPolicyResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackupPoliciesResponse;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClient;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClient.BackrollHttpClientException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackrollClient {
    protected Logger logger = LogManager.getLogger(BackrollClient.class);

    private BackrollHttpClient backrollHttpClient;

    public class BackrollClientException extends Exception {
        public BackrollClientException(Throwable cause) {
            super(cause);
        }
    }

    public BackrollClient(BackrollHttpClient backrollHttpClient) {
        this.backrollHttpClient = backrollHttpClient;
    }

    public String startBackupJob(final String jobId) throws BackrollHttpClientException {
        logger.info("startBackupJob : Trying to start backup for Backroll job: {}", jobId);
        String backupJob = "";
        BackrollTaskRequestResponse requestResponse = backrollHttpClient.post(String.format("/tasks/singlebackup/%s", jobId),
                null, BackrollTaskRequestResponse.class);
        logger.info("startBackupJob : BackupJob status link: {}", requestResponse.location);

        var backupExternalId = requestResponse.location.replace("/api/v1/status/", "");

        return StringUtils.isEmpty(backupExternalId) ? null : backupExternalId;
    }

    public List<BackupOffering> getBackupOfferings() throws BackrollHttpClientException {

        logger.info("Trying to get backroll backup policies url");
        String urlTask = "";
        BackrollTaskRequestResponse requestResponse = backrollHttpClient.getParse("/backup_policies", BackrollTaskRequestResponse.class);
        logger.info("BackrollClient:getBackupOfferingUrl:Apres Parse:  " + requestResponse.location);
        urlTask = requestResponse.location.replace("/api/v1", "");

        if (StringUtils.isEmpty(urlTask)) {
            return new ArrayList<BackupOffering>();
        }

        logger.info("Trying to list backroll backup policies");
        final List<BackupOffering> policies = new ArrayList<>();
        BackupPoliciesResponse backupPoliciesResponse = backrollHttpClient.getWaitParse(urlTask, BackupPoliciesResponse.class);
        logger.info("BackrollClient:getBackupOfferings:Apres Parse:  " + backupPoliciesResponse.backupPolicies.get(0).name);
        for (final BackrollBackupPolicyResponse policy : backupPoliciesResponse.backupPolicies) {
            policies.add(new BackrollOffering(policy.name, policy.id));
        }

        return policies;
    }

    public boolean restoreVMFromBackup(final String vmId, final String backupName) throws BackrollHttpClientException
             {
        logger.info("Start restore backup with backroll with backup {} for vm {}", backupName, vmId);

        boolean isRestoreOk = false;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("virtual_machine_id", vmId);
            jsonBody.put("backup_name", backupName);
            jsonBody.put("storage", "");
            jsonBody.put("mode", "single");

        } catch (JSONException e) {
            logger.error("Backroll Error: {}", e.getMessage());
        }

        BackrollTaskRequestResponse requestResponse = backrollHttpClient.post(String.format("/tasks/restore/%s", vmId),
                jsonBody, BackrollTaskRequestResponse.class);
        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        String result = backrollHttpClient.getWait(urlToRequest);
        if (result.contains("SUCCESS")) {
            logger.debug("RESTORE SUCCESS content : " + result);
            logger.debug("RESTORE SUCCESS");
            isRestoreOk = true;
        }

        return isRestoreOk;
    }

    public BackrollTaskStatus checkBackupTaskStatus(String taskId) throws BackrollClientException {
        try {
            logger.info("Trying to get backup status for Backroll task: {}", taskId);

            BackrollTaskStatus status = new BackrollTaskStatus();

            String backupResponse = backrollHttpClient.get("/status/" + taskId);

        if (backupResponse.contains(TaskState.FAILURE) || backupResponse.contains(TaskState.PENDING)) {
            BackrollBackupStatusResponse backupStatusRequestResponse = new ObjectMapper().readValue(backupResponse, BackrollBackupStatusResponse.class);
            status.setState(backupStatusRequestResponse.state);
        } else {
            BackrollBackupStatusSuccessResponse backupStatusSuccessRequestResponse = new ObjectMapper().readValue(backupResponse, BackrollBackupStatusSuccessResponse.class);
            status.setState(backupStatusSuccessRequestResponse.state);
            status.setInfo(backupStatusSuccessRequestResponse.info);
        }

            return StringUtils.isEmpty(status.getState()) ? null : status;
        } catch (Exception exception) {
            logger.error(exception);
            exception.printStackTrace();
            throw new BackrollClientException(exception);
        }
    }

    public boolean deleteBackup(final String vmId, final String backupName) throws BackrollHttpClientException
             {
        logger.info("BACKROLL: Trying to delete backup {} for vm {} using Backroll", vmId, backupName);
        boolean isBackupDeleted = false;

        BackrollTaskRequestResponse requestResponse = backrollHttpClient.delete(
                String.format("/virtualmachines/%s/backups/%s", vmId, backupName), BackrollTaskRequestResponse.class);
        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        BackrollBackupsFromVMResponse backrollBackupsFromVMResponse = backrollHttpClient.getWaitParse(urlToRequest,
                BackrollBackupsFromVMResponse.class);
        logger.debug(backrollBackupsFromVMResponse.state);
        isBackupDeleted = backrollBackupsFromVMResponse.state.equals(TaskState.SUCCESS);

        return isBackupDeleted;
    }

    public Metric getVirtualMachineMetrics(final String vmId) throws BackrollHttpClientException  {
        logger.info("Trying to retrieve virtual machine metric from Backroll for vm {}", vmId);

        Metric metric = new Metric(0L, 0L);

        BackrollTaskRequestResponse requestResponse = backrollHttpClient
                .getParse(String.format("/virtualmachines/%s/repository", vmId), BackrollTaskRequestResponse.class);

        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        BackrollVmMetricsResponse vmMetricsResponse = backrollHttpClient.getWaitParse(urlToRequest,
                BackrollVmMetricsResponse.class);

        if (vmMetricsResponse != null && vmMetricsResponse.state.equals(TaskState.SUCCESS)) {
            logger.debug("SUCCESS ok");
            CacheStats stats = null;
            try {
                stats = vmMetricsResponse.infos.cache.stats;
            } catch (NullPointerException e) {
            }
            if (stats != null) {
                long size = Long.parseLong(stats.totalSize);
                metric = new Metric(size, size);
            }
        }

        return metric;
    }

    public BackrollBackupMetrics getBackupMetrics(String vmId, String backupId) throws BackrollHttpClientException
             {
        logger.info("Trying to get backup metrics for VM: {}, and backup: {}", vmId, backupId);

        BackrollBackupMetrics metrics = null;

        BackrollTaskRequestResponse requestResponse = backrollHttpClient.getParse(
                String.format("/virtualmachines/%s/backups/%s", vmId, backupId), BackrollTaskRequestResponse.class);

        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        logger.debug(urlToRequest);

        BackrollBackupMetricsResponse metricsResponse = backrollHttpClient.getWaitParse(urlToRequest,
                BackrollBackupMetricsResponse.class);
        if (metricsResponse.info != null) {
            metrics = new BackrollBackupMetrics(Long.parseLong(metricsResponse.info.originalSize), Long.parseLong(metricsResponse.info.deduplicatedSize));
        }
        return metrics;
    }

    public List<BackrollVmBackup> getAllBackupsfromVirtualMachine(String vmId) throws BackrollHttpClientException
            {
        List<BackrollVmBackup> backups = new ArrayList<BackrollVmBackup>();
        List<BackupInfos> backupInfos = getBackupInfosFromVm(vmId);
        if (backupInfos != null && backupInfos.size() > 0) {
            for (BackupInfos infos : backupInfos) {
                var dateStart = new DateTime(infos.start);
                backups.add(new BackrollVmBackup(infos.id, infos.name, dateStart.toDate()));
            }
        }
        return backups;
    }

    public List<BackupInfos> getBackupInfosFromVm(String vmId) throws BackrollHttpClientException
             {
        logger.info("Trying to retrieve all backups for vm {}", vmId);
        BackrollTaskRequestResponse requestResponse = backrollHttpClient
                .getParse(String.format("/virtualmachines/%s/backups", vmId), BackrollTaskRequestResponse.class);

        String urlToRequest = requestResponse.location.replace("/api/v1", "");
        logger.debug(urlToRequest);
        VirtualMachineBackupsResponse virtualMachineBackupsResponse = backrollHttpClient.getWaitParse(urlToRequest,
                VirtualMachineBackupsResponse.class);
        if (virtualMachineBackupsResponse.state.equals(TaskState.SUCCESS)) {
            return virtualMachineBackupsResponse.info.archives;
        }
        return null;

    }

    public List<Backup.RestorePoint> listRestorePoints(String vmId) throws BackrollHttpClientException  {
        List<Backup.RestorePoint> backups = new ArrayList<Backup.RestorePoint>();
        List<BackupInfos> backupInfos = getBackupInfosFromVm(vmId);
        if (backupInfos != null && backupInfos.size() > 0) {
            for (BackupInfos infos : backupInfos) {
                var dateStart = new DateTime(infos.start);
                backups.add(new Backup.RestorePoint(infos.name, dateStart.toDate(), "INCREMENTAL"));
            }
        }
        return backups;
    }
}
