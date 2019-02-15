package com.angrycyz;

import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utility {

    private static final Logger logger = LogManager.getLogger("Utility");

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

}