package com.angrycyz;

import com.angrycyz.grpc.AcceptLearnPbGrpc;
import com.angrycyz.grpc.LearnReply;
import com.angrycyz.grpc.PaxosMsg;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Proposer implements Runnable{
    private static final Logger logger = LogManager.getLogger("Proposer");
    private long pId = 0;
    private BlockingQueue<QueueMsg> proposerBq;
    private BlockingQueue<Pair<String, Boolean>> proposerReplyBq;
    private BlockingQueue<QueueMsg> acceptorBq;
    private BlockingQueue<QueueMsg> localAcceptorReplyBq;
    private BlockingQueue<QueueMsg> learnerBq;
    private BlockingQueue<String> learnerReplyBq;
    private List<AcceptLearnPbGrpc.AcceptLearnPbBlockingStub> aStubs;
    private long serverId;
    private List<Pair<String, Integer>> addressList;

    Proposer(
            List<AcceptLearnPbGrpc.AcceptLearnPbBlockingStub> aStubs,
            BlockingQueue<QueueMsg> proposerBq,
            BlockingQueue<Pair<String, Boolean>> proposerReplyBq,
            BlockingQueue<QueueMsg> acceptorBq,
            BlockingQueue<QueueMsg> localAcceptorReplyBq,
            BlockingQueue<QueueMsg> learnerBq,
            BlockingQueue<String> learnerReplyBq,
            long serverId,
            List<Pair<String, Integer>> addressList) {
        this.proposerBq = proposerBq;
        this.proposerReplyBq = proposerReplyBq;
        this.acceptorBq = acceptorBq;
        this.localAcceptorReplyBq = localAcceptorReplyBq;
        this.aStubs = aStubs;
        this.learnerBq = learnerBq;
        this.learnerReplyBq = learnerReplyBq;
        this.addressList = addressList;
        /* if there's a tie on pid, use serverId */
        this.serverId = serverId;
    }

    public void run() {
        while (true) {
            try {
                QueueMsg queueMsg = this.proposerBq.take();
                logger.debug("Proposer received request");
                /* send prepare message to all acceptor
                 * except itself, communicate with local
                 * acceptor using blockingqueue
                 */
                String msg = queueMsg.getOperation() + " "
                        + queueMsg.getKey() + " "
                        + queueMsg.getValue();

                /* only ask just more than half acceptor,
                 * could also ask all acceptor, some will fail randomly
                 * here only take n/2 + 1
                 */
                int quorumNum = this.aStubs.size() + 1;
                int updateNum = quorumNum + 1;
                Set<String> downServerSet = new HashSet<String>();
                logger.info("Quorum number:" + Integer.toString(quorumNum));
                int maxRetry = 3;
                int retry = 0;

                while (retry < maxRetry) {
                    if (retry > 0) {
                        logger.debug("Retry the " + Integer.toString(retry) + " time");
                    }
                    /* phase 1 */
                    int promiseCount = 0;
                    long maxPId = pId;

                    PaxosMsg prepareMsg = PaxosMsg.newBuilder()
                            .setAction("prepare")
                            .setPId(pId)
                            .setServerId(this.serverId)
                            .setMsg(msg)
                            .build();
                    /* send to local acceptors */
                    this.acceptorBq.put(new QueueMsg(queueMsg.getKey(),
                            queueMsg.getValue(),
                            queueMsg.getOperation(),
                            pId, this.serverId,
                            1,
                            true));
                    logger.debug("Send proposal message to local acceptor");

                    try {
                        QueueMsg localPromise = this.localAcceptorReplyBq.poll(Utility.ASTUB_TIMEOUT, TimeUnit.SECONDS);
                        if (localPromise.getpId() > maxPId || localPromise.getServerId() > this.serverId) {
                            maxPId = localPromise.getpId();
                            msg = localPromise.getOperation() + " " + localPromise.getKey() + " " + localPromise.getValue();
                            logger.info("Receive higher id offer:" + msg);
                        } else {
                            promiseCount += 1;
                        }
                    } catch (Exception e) {
                        logger.warn("Did not receive promise from local acceptor: "
                                + e.getMessage());
                    }

                    /* send to all other acceptors */
                    for (int i = 0; i < quorumNum - 1; i++) {
                        Pair<String, Integer> address = addressList.get(i);
                        try {
                            PaxosMsg promiseMsg = this.aStubs.get(i)
                                    .withDeadlineAfter(Utility.ASTUB_TIMEOUT, TimeUnit.SECONDS)
                                    .prepare(prepareMsg);
                            logger.debug("Send prepare message to acceptor "
                                            + address.getKey() + " "
                                            + Integer.toString(address.getValue()) + ", "
                                            + "received promise message: " + promiseMsg.getMsg());
                            /* if received new promise from acceptor.
                            * set the value of the largest id as its value
                            * use server id to avoid tie
                            */
                            if (promiseMsg.getPId() > maxPId || promiseMsg.getServerId() > this.serverId) {
                                maxPId = promiseMsg.getPId();
                                msg = promiseMsg.getMsg();
                                logger.info("Receive higher id offer:" + msg);
                            } else {
                                promiseCount += 1;
                            }
                        } catch (io.grpc.StatusRuntimeException ie) {
                            logger.warn("Lose connection to acceptor "
                                    + address.getKey() + " "
                                    + Integer.toString(address.getValue()) + ": "
                                    + ie.getMessage());
                            String addressKey = address.getKey() + " " + Integer.toString(address.getValue());
                            if (!downServerSet.contains(addressKey)) {
                                updateNum -= 1;
                                downServerSet.add(addressKey);
                                logger.info("Quorum number decrease by 1, now: " + Integer.toString(updateNum));
                            }
                            if (updateNum < 3) {
                                logger.warn("Online server number smaller than 3! Waiting for more server...");
                                break;
                            }
                        } catch (Exception e) {
                            /* do not increase the count */
                            logger.warn("Did not receive promise from one acceptor "
                                    + address.getKey() + " "
                                    + Integer.toString(address.getValue()) + ": "
                                    + e.getMessage());
                        }
                    }

                    logger.info("Received " + Integer.toString(promiseCount) + " promises");
                    PaxosMsg proposeMsg = PaxosMsg.newBuilder()
                            .setPId(pId)
                            .setServerId(this.serverId)
                            .setMsg(msg)
                            .setAction("propose")
                            .build();

                    /* if ask all acceptor, and more than half promise are received
                     * then we don't need to ask local acceptor
                     * otherwise, ask local acceptor in a timeout(blocking until timeout)
                     * then check if any promise have larger id
                     * if yes, update proposal value, if no, use current value
                     */

                    if (promiseCount > updateNum/2) {
                        /* enough promise */
                        /* phase 2 */
                        int acceptedCount = 0;
                        boolean rejected = false;
                        /* send to local acceptors */
                        String[] msgArr = msg.split(" ");
                        String operation = msgArr[0];
                        String key = msgArr[1];
                        String value = null;
                        if (msgArr.length > 2) {
                            value = msgArr[2];
                        }
                        this.acceptorBq.put(new QueueMsg(key,
                                value,
                                operation,
                                pId, this.serverId,
                                2,
                                true));
                        logger.debug("Send proposal message to local acceptor");
                        try {
                            QueueMsg localAccepted = this.localAcceptorReplyBq.poll(Utility.ASTUB_TIMEOUT, TimeUnit.SECONDS);
                            if (localAccepted.getpId() > maxPId || localAccepted.getServerId() > this.serverId) {
                                rejected = true;
                                logger.info("Rejected by local acceptor");
                            } else {
                                acceptedCount += 1;
                            }
                        } catch (Exception e) {
                            logger.warn("Did not receive proposal from local acceptor: "
                                    + e.getMessage());
                        }

                        if (!rejected) {
                        /* send to all other acceptors */
                            for (int i = 0; i < quorumNum - 1; i++) {
                                Pair<String, Integer> address = addressList.get(i);
                                try {
                                    PaxosMsg acceptedReply = this.aStubs.get(i)
                                            .withDeadlineAfter(Utility.ASTUB_TIMEOUT, TimeUnit.SECONDS)
                                            .accept(proposeMsg);
                                    logger.debug("Send proposal message to acceptor "
                                            + address.getKey() + " "
                                            + Integer.toString(address.getValue()) + ", "
                                            + "received accept message: " + acceptedReply.getMsg());
                                    if (acceptedReply.getPId() > maxPId || acceptedReply.getServerId() > this.serverId) {
                                        rejected = true;
                                        break;
                                    } else {
                                        acceptedCount += 1;
                                    }
                                } catch (io.grpc.StatusRuntimeException ie) {
                                    logger.warn("Lose connection to acceptor "
                                            + address.getKey() + " "
                                            + Integer.toString(address.getValue()) + ": "
                                            + ie.getMessage());
                                    String addressKey = address.getKey() + " " + Integer.toString(address.getValue());
                                    if (!downServerSet.contains(addressKey)) {
                                        updateNum -= 1;
                                        downServerSet.add(addressKey);
                                        logger.info("Quorum number decrease by 1, now: " + Integer.toString(updateNum));
                                    }
                                    if (updateNum < 3) {
                                        logger.warn("Online server number smaller than 3! Waiting for more server...");
                                        break;
                                    }
                                } catch (Exception e) {
                                    logger.warn("Did not receive proposal from one acceptor "
                                            + address.getKey() + " "
                                            + Integer.toString(address.getValue()) + ": "
                                            + e.getMessage());
                                }
                            }
                        }

                        logger.info("Received " + Integer.toString(acceptedCount) + " accepts");

                        if (rejected) {
                            /* fail message */
                            logger.info("Fail to propose, abandon proposal");
                            this.proposerReplyBq.put(new Pair<String, Boolean>("Fail to propose, abandon proposal", false));
                            retry = maxRetry;
                        }

                        if (acceptedCount > updateNum/2) {
                            /* do not retry, finish */
                            logger.info("Successfully proposed");
                            retry = maxRetry;
                            String learnerReply = multicastToLearner(proposeMsg);
                            this.proposerReplyBq.put(new Pair<String, Boolean>(learnerReply, true));
                        }

                    } else {
                        /* not enough promise */
                        /* fail message */
                        logger.info("Fail to prepare, retrying");
                        this.proposerReplyBq.put(new Pair<String, Boolean>("Fail to prepare, retrying", false));
                    }

                    pId += 1;
                    retry += 1;
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
            }

        }
    }

    private String multicastToLearner(PaxosMsg proposeMsg) {
        String result = "";
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
            result = learnerReplyBq.take();
            logger.debug("Send learn request to local learner");
        } catch (Exception e) {
            logger.error("Fail to announce to local learner: " + e.getMessage());
        }

        /* send to other learner */
        for (int i = 0; i < aStubs.size(); i++) {
            Pair<String, Integer> address = addressList.get(i);
            try {
                LearnReply learnReply = aStubs.get(i)
                        .withDeadlineAfter(Utility.LSTUB_TIMEOUT, TimeUnit.SECONDS)
                        .learnProposal(proposeMsg);
                result = learnReply.getMsg();
                logger.debug("Send learn request to learner "
                        + address.getKey() + " "
                        + Integer.toString(address.getValue())
                        + ", received learn reply: "
                        + learnReply.getMsg());
            } catch (Exception e) {
                logger.error("Fail to announce to learner: "
                        + address.getKey() + " "
                        + Integer.toString(address.getValue()) + ": "
                        + e.getMessage());
            }
        }
        return result;
    }
}

