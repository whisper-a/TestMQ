package com.test;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunrui
 * @date 2018-07-18
 * @descprition
 */
public class TestRocketMQ {
    private static Logger logger = LoggerFactory.getLogger(TestRocketMQ.class);

    private static AtomicInteger sNum = new AtomicInteger();
    private static AtomicInteger eNum = new AtomicInteger();
    private static AtomicInteger cNum = new AtomicInteger();

    private static AtomicInteger atomicProduce = new AtomicInteger();

    public static void syncProducer() throws Exception {

        //Instantiate with a producer group name.
        DefaultMQProducer producer = new DefaultMQProducer("producerSyn");

        //Launch the instance.
        //producer.setNamesrvAddr("localhost:9876");
        producer.start();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            //Create a message instance, specifying topic, tag and message body.
            Message msg = new Message("TopicSr",
                    "TagA" ,
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET)
            );
            //Call send message to deliver message to one of brokers.
            SendResult sendResult = producer.send(msg);
            logger.info("send result:{}", sendResult);
        }
        long end = System.currentTimeMillis();
        System.out.println("send all msg use time:"+ (end - start) + "'ms");
        //Shut down once the producer instance is not longer in use.
        producer.shutdown();
    }

    public static void consumer() throws Exception {
        AtomicInteger count = new AtomicInteger();
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("Consumer");
        consumer.setConsumeThreadMax(20);
        // only subsribe messages have property a, also a >=0 and a <= 3
        //consumer.subscribe("TopicSyn", "*");
        consumer.subscribe("TopicAsync", "*");

        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {

                logger.info("consume msg:{}, \ncount:{}" ,cNum.addAndGet(1));

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
    }


    public static void main(String[] args) {
        try {
            //onewayProducer();
            syncProducer();


            //asyncProducer();
            //consumer();

            //broadcastProducer();
            //broadCastConsumer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String generateMsg(int size){

        StringBuilder builder = new StringBuilder();
        while(size-- > 0){
            builder.append("s");
        }
        return builder.toString();
    }

    public static void asyncProducer() throws Exception {
        //Instantiate with a producer group name.
        DefaultMQProducer producer = new DefaultMQProducer("producerAsync");
        //producer.setSendMsgTimeout(10000);
        //Launch the instance.

        producer.start();
        producer.setRetryTimesWhenSendAsyncFailed(0);
        String str = generateMsg(128);
        for (int i = 0; i < 100000; i++) {
            final int index = i;
            //Create a message instance, specifying topic, tag and message body.
            Message msg = new Message("TopicSr",
                    "TagA",
                    index+"",
                     str.getBytes(RemotingHelper.DEFAULT_CHARSET));
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    atomicProduce.incrementAndGet();
                    sNum.incrementAndGet();
                    logger.debug("send msg:{} \nsuccess count:{}", sendResult, sNum.get());
                    //logger.info("send msg:{} \nsuccess count:{}", sendResult, sNum.addAndGet(1));

                }
                @Override
                public void onException(Throwable e) {
                    atomicProduce.incrementAndGet();
                    eNum.incrementAndGet();
                    logger.error("send msg:{} exception count:{}:", index, eNum.get());
                }
            });
        }
        //Shut down once the producer instance is not longer in use.

        new Thread(() -> {
            while(true){
                System.out.println("success num:"+ sNum.get() + " failed num:" + eNum.get());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(300 * 1000);
        producer.shutdown();

    }


    public static void onewayProducer() throws Exception {
        //Instantiate with a producer group name.
        DefaultMQProducer producer = new DefaultMQProducer("ExampleProducerGroup");
        //Launch the instance.
        producer.start();
        for (int i = 0; i < 100000; i++) {
            //Create a message instance, specifying topic, tag and message body.
            Message msg = new Message("TopicTest" /* Topic */,
                    "TagA" /* Tag */,
                    ("Hello RocketMQ " +
                            i).getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
            );
            //Call send message to deliver message to one of brokers.
            producer.sendOneway(msg);

        }
        //Shut down once the producer instance is not longer in use.
        producer.shutdown();
    }

    public static void broadcastProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("ProducerBroad");
        producer.start();

        for (int i = 0; i < 100; i++){
            Message msg = new Message("TopicBroad",
                    "TagA",
                    "OrderID188",
                    "Hello world".getBytes(RemotingHelper.DEFAULT_CHARSET));
            SendResult sendResult = producer.send(msg);
            System.out.printf("%s%n", sendResult);
        }
        producer.shutdown();
    }

    public static void broadCastConsumer() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("ConsumerBroadNot3");

        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        //set to broadcast mode
        consumer.setMessageModel(MessageModel.BROADCASTING);

        consumer.subscribe("TopicBroad", "TagA || TagC || TagD");

        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                    ConsumeConcurrentlyContext context) {

                System.out.printf(Thread.currentThread().getName() + " Receive New Messages:" +eNum.addAndGet(1) + ": "+ msgs + "%n");
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        System.out.printf("Broadcast Consumer Started.%n");
    }



}
