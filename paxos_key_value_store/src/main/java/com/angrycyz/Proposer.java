package com.angrycyz;

import com.angrycyz.grpc.AcceptLearnPbGrpc;
import com.angrycyz.grpc.LearnReply;
import com.angrycyz.grpc.PaxosMsg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Proposer implements Runnable{
    private static final Logger logger = LogManager.getLogger("Proposer");
    private long pId = 0;
    private BlockingQueue<QueueMsg> proposerBq;
    private BlockingQueue<Boolean> proposerReplyBq;
    private BlockingQueue<QueueMsg> learnerBq;
    private List<AcceptLearnPbGrpc.AcceptLearnPbBlockingStub> aStubs;
    public final int ASTUB_TIMEOUT = 3;
    public final int LSTUB_TIMEOUT = 3;
    private long serverId;

    Proposer(
             List<AcceptLearnPbGrpc.AcceptLearnPbBlockingStub> aStubs,
             BlockingQueue<QueueMsg> proposerBq,
             BlockingQueue<Boolean> proposerReplyBq,
             BlockingQueue<QueueMsg> learnerBq,
             long serverId) {
        this.proposerBq = proposerBq;
        this.proposerReplyBq = proposerReplyBq;
        this.aStubs = aStubs;
        this.learnerBq = learnerBq;
        /* if there's a tie on pid, use serverId */
        this.serverId = serverId;
    }

    public void run() {
        while (true) {
            try {
                QueueMsg queueMsg = this.proposerBq.take();
                logger.info("Proposer received request");
                /* send prepare message to all acceptor
                 * except itself, communicate with local
                 * acceptor using blockingqueue
                 */
                String msg = queueMsg.getOperation() + " "
                        + queueMsg.getKey() + " "
                        + queueMsg.getValue();

                pId += 1;
                PaxosMsg prepareMsg = PaxosMsg.newBuilder()
                        .setAction("prepare")
                        .setPId(pId)
                        .setServerId(this.serverId)
                        .setMsg(msg)
                        .build();
                int promiseCount = 0;
                long maxPId = pId;
                /* only ask just more than half acceptor,
                 * could also ask all acceptor, some will fail randomly
                 * here only take n/2 + 1
                 */
                int quorumNum = (this.aStubs.size() + 1)/2 + 1;
                boolean retry = true;

                while (retry) {
                    /* phase 1 */
                    for (int i = 0; i < quorumNum; i++) {
                        try {
                            PaxosMsg promiseMsg = this.aStubs.get(i)
                                    .withDeadlineAfter(ASTUB_TIMEOUT, TimeUnit.SECONDS)
                                    .prepare(prepareMsg);
                            logger.info("Send prepare message to acceptor, " +
                                    "received promise message: " + promiseMsg.getMsg());
                            /* if received new promise from acceptor.
                            * set the value of the largest id as its value
                            * use server id to avoid tie
                            */
                            if (promiseMsg.getPId() > maxPId || promiseMsg.getPId() > this.serverId) {
                                maxPId = promiseMsg.getPId();
                                msg = promiseMsg.getMsg();
                            } else {
                                promiseCount += 1;
                            }
                        } catch (Exception e) {
                            /* do nothing, and do not increase the count */
                            logger.warn("Does not receive from one acceptor: " + e.getMessage());
                        }
                    }

                    PaxosMsg proposeMsg = PaxosMsg.newBuilder()
                            .setPId(pId)
                            .setServerId(this.serverId)
                            .setMsg(msg)
                            .setAction("accept")
                            .build();

                    /* if ask all acceptor, and more than half promise are received
                     * then we don't need to ask local acceptor
                     * otherwise, ask local acceptor in a timeout(blocking until timeout)
                     * then check if any promise have larger id
                     * if yes, update proposal value, if no, use current value
                     */

                    if (promiseCount == quorumNum) {
                    /* enough promise */
                    /* phase 2 */
                        int acceptedCount = 0;
                        boolean rejected = false;
                        for (int i = 0; i < quorumNum; i++) {
                            try {
                                PaxosMsg acceptedReply = this.aStubs.get(i)
                                        .withDeadlineAfter(ASTUB_TIMEOUT, TimeUnit.SECONDS)
                                        .accept(proposeMsg);
                                logger.info("Send proposal message to acceptor, " +
                                        "received accept message: " + acceptedReply.getMsg());
                                if (acceptedReply.getAction().equals("rejected")) {
                                    rejected = true;
                                    break;
                                } else {
                                    acceptedCount += 1;
                                }
                            } catch (Exception e) {
                                logger.warn("Does not receive from one acceptor: " + e.getMessage());
                            }
                        }

                        if (rejected) {
                            /* fail message */
                            this.proposerReplyBq.put(false);
                            retry = false;
                        }

                        if (acceptedCount == quorumNum) {
                            /* do not retry, finish */
                            retry = false;
                            multicastToLearner(proposeMsg);
                        }

                    } else {
                        /* not enough promise */
                        /* fail message */
                        this.proposerReplyBq.put(false);
                        retry = false;
                    }

                    pId += 1;
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
                System.exit(1);
            }

        }
    }

    private void multicastToLearner(PaxosMsg proposeMsg) {
        try {
            /* send to local learner */
            String[] msgArr = proposeMsg.getMsg().split(" ");
            String operation = msgArr[0];
            String key = msgArr[1];
            String value = null;
            if (msgArr.length > 2) {
                value = msgArr[2];
            }
            QueueMsg queueMsg = new QueueMsg(key, value, operation, pId, serverId, -1);
            this.learnerBq.put(queueMsg);

        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        /* send to other learner */
        for (int i = 0; i < aStubs.size(); i++) {
            try {
                LearnReply learnReply = aStubs.get(i)
                        .withDeadlineAfter(LSTUB_TIMEOUT, TimeUnit.SECONDS)
                        .learnProposal(proposeMsg);
                logger.info("Send learn request, received learn reply: " + learnReply.getMsg());
            } catch (Exception e) {
                logger.error("Fail to announce to learner: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}