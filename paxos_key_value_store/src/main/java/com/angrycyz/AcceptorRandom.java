package com.angrycyz;

import com.angrycyz.grpc.PaxosMsg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class AcceptorRandom implements Runnable{
    private static final Logger logger = LogManager.getLogger("Acceptor");
    private BlockingQueue<QueueMsg> acceptorBq;
    private BlockingQueue<QueueMsg> acceptorReplyBq;
    private BlockingQueue<QueueMsg> localAcceptorReplyBq;
    private int curPhase = 0;
    private QueueMsg curPromise = null;

    AcceptorRandom(BlockingQueue<QueueMsg> acceptorBq,
             BlockingQueue<QueueMsg> acceptorReplyBq,
             BlockingQueue<QueueMsg> localAcceptorReplyBq) {
        this.acceptorBq = acceptorBq;
        this.acceptorReplyBq = acceptorReplyBq;
        this.localAcceptorReplyBq = localAcceptorReplyBq;
    }

    public void run() {
        while(true) {
            try {
                QueueMsg queueMsg = this.acceptorBq.take();
                logger.debug("Acceptor received prepare or propose message");
                int phase = queueMsg.getPhase();

                Random rand = new Random();
                /* 50% */
                boolean changeCurPromise = rand.nextInt(100) < 90;

                logger.info("change: " + changeCurPromise);
                if (changeCurPromise || this.curPromise == null) {
                    this.curPromise = queueMsg;
                } else {
                    this.curPromise = new QueueMsg(queueMsg.getKey(),
                            queueMsg.getValue(),
                            queueMsg.getOperation(),
                            queueMsg.getpId(),
                            queueMsg.getServerId() + 1,
                            queueMsg.getPhase(),
                            queueMsg.isLocal());
                }

                if (queueMsg.isLocal()) {
                    localAcceptorReplyBq.put(new QueueMsg(curPromise.getKey(),
                            curPromise.getValue(),
                            curPromise.getOperation(),
                            curPromise.getpId(),
                            curPromise.getServerId(),
                            curPhase,
                            true));
                } else {
                    acceptorReplyBq.put(new QueueMsg(curPromise.getKey(),
                            curPromise.getValue(),
                            curPromise.getOperation(),
                            curPromise.getpId(),
                            curPromise.getServerId(),
                            curPhase));
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
                System.exit(1);
            }
        }

    }

}