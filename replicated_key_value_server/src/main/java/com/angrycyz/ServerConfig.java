package com.angrycyz;

import javafx.util.Pair;
import java.util.List;

public class ServerConfig {

    public ServerConfig(Pair<String, Integer> coordinator, List<Pair<String, Integer>> participants) {
        this.coordinator = coordinator;
        this.participants = participants;
    }

    public Pair<String, Integer> getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(Pair<String, Integer> coordinator) {
        this.coordinator = coordinator;
    }

    public List<Pair<String, Integer>> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Pair<String, Integer>> participants) {
        this.participants = participants;
    }

    public Pair<String, Integer> coordinator;
    public List<Pair<String, Integer>> participants;
}