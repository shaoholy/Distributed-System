package com.angrycyz;

import java.util.UUID;

public class Transaction {
    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Transaction(String transaction_id, String key, String val, String address, int port) {
        this.transaction_id = transaction_id;
        this.key = key;
        this.val = val;
        this.address = address;
        this.port = port;
    }

    private String transaction_id;
    private String key;
    private String val;
    private String address;
    private int port;

    public boolean isDecision() {
        return decision;
    }

    public void setDecision(boolean decision) {
        this.decision = decision;
    }

    private boolean decision;
}