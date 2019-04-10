package com.angrycyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Learner implements Runnable {
    private static final Logger logger = LogManager.getLogger("Learner");
    private BlockingQueue<QueueMsg> learnerBq;
    private BlockingQueue<String> learnerReplyBq;
    private ConcurrentHashMap<String, String> map;

    Learner(BlockingQueue<QueueMsg> learnerBq,
            BlockingQueue<String> learnerReplyBq,
            ConcurrentHashMap<String, String> map) {
        this.learnerBq = learnerBq;
        this.learnerReplyBq = learnerReplyBq;
        this.map = map;
    }

    public void run() {
        while (true) {
            try {
                QueueMsg announceMsg = this.learnerBq.take();
                logger.info("Learner received announce message");
                if (announceMsg.getOperation().equals("put")) {
                    map.put(announceMsg.getKey(), announceMsg.getValue());
                    learnerReplyBq.put("Success");
                } else if (announceMsg.getOperation().equals("delete")) {
                    if (map.containsKey(announceMsg.getKey())) {
                        map.remove(announceMsg.getKey());
                        learnerReplyBq.put("Success");
                    } else {
                        learnerReplyBq.put("No such key");
                    }
                } else if (announceMsg.getOperation().equals("get")) {
                    if (map.containsKey(announceMsg.getKey())) {
                        learnerReplyBq.put(map.get(announceMsg.getKey()));
                    } else {
                        learnerReplyBq.put("No such key");
                    }
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }
}