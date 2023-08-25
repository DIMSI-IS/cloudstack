package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Archives {
    @JsonProperty("archives")
    public List<BackupInfos> archives;

    @JsonProperty("encryption")
    public InfosEncryption encryption;

    @JsonProperty("repository")
    public InfosRepository repository;
}
