package org.apache.cloudstack.backup.backroll.model;

public class BackrollVmBackup {
    private String id;
    private String name;
    private String date;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public BackrollVmBackup(String id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }
}