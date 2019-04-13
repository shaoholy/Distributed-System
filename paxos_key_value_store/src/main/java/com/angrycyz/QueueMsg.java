package com.angrycyz;

public class QueueMsg {
    public QueueMsg(String key, String value, String operation, long pId, long serverId, int phase) {
        this.key = key;
        this.value = value;
        this.operation = operation;
        this.pId = pId;
        this.serverId = serverId;
        this.phase = phase;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public long getpId() {
        return pId;
    }

    public void setpId(long pId) {
        this.pId = pId;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }


    public QueueMsg(String key, String value, String operation) {
        this.key = key;
        this.value = value;
        this.operation = operation;
    }

    private String key;
    private String value;
    private String operation;
    private long pId;
    private long serverId;
    private int phase;

    public QueueMsg(String key, String value, String operation, long pId, long serverId, int phase, boolean local) {
        this.key = key;
        this.value = value;
        this.operation = operation;
        this.pId = pId;
        this.serverId = serverId;
        this.phase = phase;
        this.local = local;
    }

    private boolean local = false;
}