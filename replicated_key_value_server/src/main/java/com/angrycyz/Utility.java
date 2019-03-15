package com.angrycyz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Utility {

    private static final Logger logger = LogManager.getLogger("Utility");
    private static final String PARTICIPANT = "participants";
    private static final String COORDINATOR = "coordinator";

    /* check if port is valid integer and between 0 - 2^16 */
    public static Pair<Boolean, Integer> isPortValid(String portString) {
        int port;
        try {
            port = Integer.parseInt(portString);
            if (port > 65535 || port < 0) {
                return new Pair<Boolean, Integer>(false, 0);
            }
        } catch (Exception e) {
            return new Pair<Boolean, Integer>(false, 0);
        }
        return new Pair<Boolean, Integer>(true, port);
    }

    public static ServerConfig readConfig(String configPath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String configStr = new String(Files.readAllBytes(Paths.get(configPath)));
            JsonNode configNode = mapper.readTree(configStr);
            JsonNode participantsNode = configNode.get(PARTICIPANT);
            List<Pair<String, Integer>> participants = new ArrayList<Pair<String, Integer>>();
            if (participantsNode.isArray()) {
                for (JsonNode elementNode: participantsNode) {
                    participants.add(
                            new Pair<String, Integer>(
                                    elementNode.get("address").asText(),
                                    elementNode.get("port").asInt()));
                }
            }
            JsonNode coordinatorNode = configNode.get(COORDINATOR);
            Pair<String, Integer> coordinator = new Pair<String, Integer>(
                    coordinatorNode.get("address").asText(),
                    coordinatorNode.get("port").asInt());
            return new ServerConfig(coordinator, participants);

        } catch (Exception e) {
            logger.error("Cannot parse config file" +
                    configPath + ": ", e.getMessage());
        }
        return null;
    }
}