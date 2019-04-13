package com.angrycyz;

import com.angrycyz.grpc.PaxosMsg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class Acceptor implements Runnable{
    private static final Logger logger = LogManager.getLogger("Acceptor");
    private BlockingQueue<QueueMsg> acceptorBq;
    private BlockingQueue<QueueMsg> acceptorReplyBq;
    private BlockingQueue<QueueMsg> localAcceptorReplyBq;
    private int curPhase = 0;
    private QueueMsg curPromise;

    Acceptor(BlockingQueue<QueueMsg> acceptorBq,
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
                logger.info("Acceptor received prepare or propose message");
                int phase = queueMsg.getPhase();
                if (phase == 1) {
                    /* promise phase */
                    if (this.curPhase == 1) {
                        /* only promise with higher pid */
                        if (queueMsg.getpId() <= this.curPromise.getpId()) {
                            /* promise with current promise */
                        } else {
                            /* update current promise, send it */
                            this.curPromise = queueMsg;
                        }
                    } else {
                        /* 0 or 2, can take any pid*/
                        this.curPromise = queueMsg;
                    }
                    /* now curPromise has always the higher pid */
                    this.curPhase = 1;
                } else {
                    /* accept phase */
                    this.curPhase = 2;
                    if (queueMsg.getpId() > this.curPromise.getpId()) {
                        this.curPromise = queueMsg;
                    }
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