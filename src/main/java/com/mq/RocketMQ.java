package com.mq;

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
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */
public class RocketMQ extends AbMQ {
    private static Logger logger = LoggerFactory.getLogger(RocketMQ.class);


    private DefaultMQProducer producer;
    private DefaultMQPushConsumer consumer;
    private String nameAddr ="localhost:9876";
    private AtomicInteger atomicProduce = new AtomicInteger();

    private static AtomicInteger sNum = new AtomicInteger(); //成功
    private static AtomicInteger eNum = new AtomicInteger(); //异常
    private static AtomicInteger cNum = new AtomicInteger(); //消费


    private static String producerName = "ProducerTest";
    private static String consumerName = "ConsumerTest";


    private static long startProduce;
    private static long endProduce;
    private static long startConsume;
    private static long endConsume;


    private boolean isProducer;
    private String name;
    private String topicName;
    private String ip;
    private int port;
    private int msgLen;
    private int msgCount;
    private String publishType;
    private int consumeThreadNum= 9;
    private ConcurrentHashMap<String, AtomicInteger> eMap = new ConcurrentHashMap<>();


    public void addException(Throwable e){
        //e.printStackTrace();
        eMap.computeIfAbsent(e.getLocalizedMessage(), k-> new AtomicInteger()).incrementAndGet();
    }

    public void staticsException(){
        logger.info(eMap.toString());
    }

    @Override
    public void init(String[] args) throws Exception {
        logger.info("args:{}", args);
        String cType = args[1];
        isProducer = cType.equalsIgnoreCase("p");
        name = args[2];
        topicName = args[3];
        ip = args[4];
        port = Integer.parseInt(args[5]);
        nameAddr = ip + ":" + port;
        if(isProducer) {
            //rocket p topicName 127.0.0.1 9876 1024 10000 syn
            msgLen = Integer.parseInt(args[6]);
            msgCount = Integer.parseInt(args[7]);
            publishType = args[8];

            producerName = name;
            initProducer();
        }else{
            //rocket c topicName 127.0.0.1 9876 20
            consumeThreadNum = Integer.parseInt(args[6]);

            consumerName = name;
            initConsumer();
        }
    }

    @Override
    public void start() throws Exception {
        if(isProducer){
            String msg = generateMsg(msgLen);
            publishMsg(msg, msgCount);
        }
    }

    @Override
    public void publishMsg(String msg, int count){
        logger.info("use {} publish msg", publishType);
        if(publishType.equals("syn")) {
            synPublish(msg, count);
        }else if(publishType.equals("async")){
            asyncPublish(msg, count);
        }
    }

    public void synPublish(String msg, int count){
        startProduce = System.currentTimeMillis();

        try {
            for (int i = 0; i < count; i++) {
                //Create a message instance, specifying topic, tag and message body.
                Message message = new Message(topicName,
                        "TagA",
                        "syn" + i,
                        msg.getBytes(RemotingHelper.DEFAULT_CHARSET)
                );
                //Call send message to deliver message to one of brokers.
                SendResult sendResult = producer.send(message);
                logger.debug(sendResult.toString());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        logger.info("rocket mq publish syn msg count:{} use time:{}'ms", count, (end - startProduce));
        producer.shutdown();
    }

    public void asyncPublish(String msg, final int count){

        try {
            //producer.setRetryTimesWhenSendAsyncFailed(2);
            producer.setSendMsgTimeout(3000);
            producer.setRetryTimesWhenSendAsyncFailed(0);
            startProduce = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                //Create a message instance, specifying topic, tag and message body.
                Message message = new Message(topicName,
                        "TagA",
                        "async" + i,
                        msg.getBytes(RemotingHelper.DEFAULT_CHARSET));
                producer.send(message, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        atomicProduce.incrementAndGet();
                        sNum.incrementAndGet();
                        endProduce = System.currentTimeMillis();
                        logger.debug("send msg:{} \nsuccess count:{}", sendResult, sNum.get());
                    }

                    @Override
                    public void onException(Throwable e) {
                        //e.printStackTrace();
                        addException(e);
                        atomicProduce.incrementAndGet();
                        eNum.incrementAndGet();
                        endProduce = System.currentTimeMillis();

                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        new Thread(() -> {
            while(true){
                logger.info("rocket mq async success num:{}, exception num:{}, total num:{}, use total time:{}'ms",sNum.get(),eNum.get(), atomicProduce.get(), endProduce- startProduce);
                staticsException();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //Shut down once the producer instance is not longer in use.
        try {
            Thread.sleep(120 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        producer.shutdown();
    }

    @Override
    public void consumeMsg(String msg) {

        if(cNum.get() == 0){
            startConsume = System.currentTimeMillis();
        }
        cNum.incrementAndGet();
        logger.debug("consume msg:{}", msg);
        endConsume = System.currentTimeMillis();
    }

    @Override
    public void initProducer() throws Exception {
        //Instantiate with a producer group name.
        producer = new DefaultMQProducer(producerName);
        //Launch the instance.
        producer.setNamesrvAddr(nameAddr);

        producer.start();
    }

    @Override
    public void initConsumer() throws Exception {
        consumer = new DefaultMQPushConsumer(consumerName);
        consumer.setNamesrvAddr(nameAddr);
        consumer.subscribe(topicName, "*");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        //consumer.setConsumeThreadMin(consumeThreadNum);
        //consumer.setConsumeThreadMax(consumeThreadNum);
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                msgs.forEach( msg-> consumeMsg(new String(msg.getBody())));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();

        new Thread(() -> {
            while(true){
                logger.info("consume msg:{} use time:{}'ms",cNum.get(), endConsume - startConsume);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
