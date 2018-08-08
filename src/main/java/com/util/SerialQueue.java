package com.util;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 有序队列
 *
 * @author zhenkun.wei
 *
 */
public class SerialQueue implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(SerialQueue.class);

    private BlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<Runnable>();

    private boolean start = true;

    @Override
    public void run() {
        while (start) {
            try {
                Runnable reqs = runnableQueue.take();
                if (reqs == null) {
                    continue;
                }
                reqs.run();
            } catch (Throwable e) {
                logger.error("QueueHandler error:", e);
            }
        }
    }

    /**
     * 加入队列
     */
    public void offer(Runnable run) {
        runnableQueue.offer(run);
    }

    /**
     * 停止
     * @param waitTime 每个线程池等待的时间
     */
    public boolean stop(long waitTime) {
        long startTime = System.currentTimeMillis();
        while (!runnableQueue.isEmpty() && System.currentTimeMillis() - startTime < waitTime) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("QueueHandler error:", e);
            }
        }
        return runnableQueue.isEmpty();
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public int getQueueSize() {
        return runnableQueue.size();
    }
}

