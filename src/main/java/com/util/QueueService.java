package com.util;

/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueueService {

    private static Logger logger = LoggerFactory.getLogger(QueueService.class);

    private static ThreadPoolExecutor queueExecutor = (ThreadPoolExecutor) Executors
            .newCachedThreadPool();

    private static Map<String, SerialQueue[]> queueData = new ConcurrentHashMap<String, SerialQueue[]>();

    /**
     * 启动指定数据量的有序队列
     *
     * @param key 队列名字
     * @param num 线程数量
     */
    public static void startQueue(String key, int num) {
        SerialQueue[] queueArray = new SerialQueue[num];
        for (int i = 0; i < num; i++) {
            queueArray[i] = new SerialQueue();
            queueExecutor.execute(queueArray[i]);
        }
        queueData.put(key, queueArray);
    }

    /**
     * 添加一个task到有序队列，hash分配
     *
     * @param key 队列名字
     * @param hashKey hash用的key
     * @param task 任务
     */
    public static void addQueueTask(String key, Object hashKey, Runnable task) {
        SerialQueue[] queueArray = queueData.get(key);
        if (queueArray == null) {
            logger.error("queueArray is null,key:{}", key);
            return;
        }
        int h = hash(hashKey);
        h = h > 0 ? h : -h;
        int index = h % queueArray.length;
        queueArray[index].offer(task);
    }

    private static int hash(Object k) {
        int h = 0;
        h ^= k.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        int result = h ^ (h >>> 7) ^ (h >>> 4);
        return Math.abs(result);
    }

    /**
     * 停止队列
     *
     * @param key 队列名字
     * @param everyQueueWaitTime 关闭的时候，每一个队列的等待时间
     *
     */
    public static boolean stopQueue(String key, long everyQueueWaitTime) {
        SerialQueue[] queueArray = queueData.get(key);
        if (queueArray == null) {
            logger.error("queueArray is null,key:{}", key);
            return false;
        }
        boolean result = true;
        for (SerialQueue queue : queueArray) {
            result = queue.stop(everyQueueWaitTime) && result;
        }
        return result;
    }

    public SerialQueue[] getQueues(String key) {
        return queueData.get(key);
    }

    public static Map<String, SerialQueue[]> getQueueData() {
        return queueData;
    }

}

