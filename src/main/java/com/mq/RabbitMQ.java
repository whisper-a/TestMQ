package com.mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */
public class RabbitMQ extends AbMQ {

    private static Logger logger = LoggerFactory.getLogger(RabbitMQ.class);
    public static Connection connection;

    public static Channel producerChannel;

    public static Channel[] consumerChannel;
    private static final String exName = "ExTest";
    private static final String queueName ="QueueTest";
    private static final String key = "key";
    private static ExecutorService executor = Executors.newFixedThreadPool(1);
    private AtomicInteger cNum = new AtomicInteger();
    private static long startConsume;
    private static long endConsume;

    private static int channelCount = 8;


    //params
    private String ip = "127.0.0.1";
    private int port = 5672;
    private int msgLen = 1024;
    private int msgCount = 10000;
    private boolean isProducer= true;


    Set<String> threadSet = new ConcurrentSet<>();
    private static String userName = "test";
    private static String passwd = "test";

    //rabbit p 127.0.0.1 5672 1024 500000
    @Override
    public void init(String[] args) throws Exception {
        String cType = args[1];
        isProducer = cType.equalsIgnoreCase("p");
        this.ip = args[2];
        this.port = Integer.parseInt(args[3]);
        if(isProducer){
            this.msgLen = Integer.parseInt(args[4]);
            this.msgCount = Integer.parseInt(args[5]);
            initProducer();
        }else{
            initConsumer();
        }
    }

    public void initConnection()
            throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(ip);
        factory.setPort(port);
        factory.setUsername(userName);
        factory.setPassword(passwd);
        factory.setSharedExecutor(Executors.newFixedThreadPool(8, new ThreadNameFactory("Consumer-Thread-")));
        //connection = factory.newConnection();
        List<Address> list = new ArrayList<>();
        list.add(new Address("127.0.0.1", 5672));
        connection = factory.newConnection(list);
        if(isProducer) {
            producerChannel = connection.createChannel();
        }else {
            consumerChannel = new Channel[channelCount];
            for(int i = 0; i < channelCount; ++i) {
                consumerChannel[i] = connection.createChannel();
                consumerChannel[i].basicQos(1);
            }
        }
    }

    public void initExchangeAndQueue( )
            throws Exception {
        // 开始创建每个服务器的交换机
        producerChannel.exchangeDeclare(exName,
                BuiltinExchangeType.DIRECT, true, false, null);
        // 创建本服务器的队列
        producerChannel.queueDeclare(queueName, true, false, false, null);
        // 把队列绑定到路由上
        producerChannel.queueBind(queueName, exName, key);

    }

    @Override
    public void initProducer() throws Exception {

        initConnection();
        initExchangeAndQueue();
    }

    public static class LocalConsumer extends DefaultConsumer{

        private Channel subChannel;

        private RabbitMQ mq;
        /**
         * Constructs a new instance and records its association to the passed-in channel.
         *
         * @param channel the channel to which this consumer is attached
         */
        public LocalConsumer(RabbitMQ mq, Channel channel) {
            super(channel);
            this.mq = mq;
            this.subChannel = channel;
        }

        public Channel getSubChannel(){
            return this.subChannel;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                AMQP.BasicProperties properties, byte[] body) throws IOException {
            String msg = new String(body);
            mq.consumeMsg(msg, subChannel);
            mq.threadSet.add(Thread.currentThread().getName());
            subChannel.basicAck(envelope.getDeliveryTag(), false);
        }
    }

    @Override
    public void initConsumer() throws Exception {
        initConnection();
        for(int i = 0; i < channelCount; ++i) {
            Consumer consumer = new LocalConsumer(this, consumerChannel[i]);
            consumerChannel[i].basicQos(1);
            consumerChannel[i].basicConsume(queueName, false, consumer);
        }
        startConsume = System.currentTimeMillis();

        new Thread(() -> {
            while(true){
                //logger.info("consume msg:{} use time:{}'ms, total thread:{}",cNum.get(), endConsume - startConsume, threadSet.size());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static AMQP.BasicProperties buildBasicProperties(String srcServerId) {
        return new AMQP.BasicProperties().builder().deliveryMode(2).replyTo(srcServerId).build();
    }

    @Override
    public void start()throws Exception{
        if(isProducer){
            String msg = generateMsg(msgLen);
            publishMsg(msg, msgCount);
        }
    }

    @Override
    public void publishMsg(String msg, int count) {
        Future<Long> f = executor.submit(() -> {

            try {
                long t1 = System.currentTimeMillis();
                for(int i = 0; i < count; ++i) {
                    String newStr = msg + i;
                    producerChannel.basicPublish(exName, key, buildBasicProperties("sr1"), newStr.getBytes());
                }
                long t2 = System.currentTimeMillis();

                return t2 - t1;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0L;
        });

        try {
            logger.info("rabbit mq publish msg count:{}, use time:{}'ms", count, f.get());
            producerChannel.close();
            executor.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    @Override
    public void consumeMsg(String msg, Object... obj) {

        if(cNum.get() == 0){
            startConsume = System.currentTimeMillis();
        }
        cNum.incrementAndGet();
        logger.debug("channel:{} consume msg:{}", obj[0], msg);
        endConsume = System.currentTimeMillis();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
