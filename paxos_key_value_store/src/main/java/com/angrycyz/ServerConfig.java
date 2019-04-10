package com.angrycyz;

import javafx.util.Pair;
import java.util.List;

public class ServerConfig {

    public ServerConfig(List<Pair<String, Integer>> processes) {
        this.processes = processes;
    }

    public List<Pair<String, Integer>> getProcesses() {
        return processes;
    }

    public void setProcesses(List<Pair<String, Integer>> processes) {
        this.processes = processes;
    }

    List<Pair<String, Integer>> processes;
}