package com.team3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;

public class LoadBalancer {
    private static final Logger logger = LogManager.getLogger("LoadBalancer");

    public static void main(String[] args) {
        /* set log configuration file location */
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        String propLocation = "src/main/resources/log4j2.xml";
        File file = new File(propLocation);

        context.setConfigLocation(file.toURI());

        logger.info("Log properties file location: " + propLocation);

        /* parse server config file */
        String configPath = "etc/server_config.json";
        ServerConfig serverConfig = Utility.readConfig(configPath);
    }
}