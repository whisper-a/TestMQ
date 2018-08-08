/*
package com.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MsgProcessorManager {

    public Logger logger = LoggerFactory.getLogger(MsgProcessorManager.class);

    public Map<Short, IMsgProcessor> processorMap = new ConcurrentHashMap<Short, IMsgProcessor>();

    public abstract void init();

    public void registerProcessor(short msgCode, IMsgProcessor processor) {
        processorMap.put(msgCode, processor);
    }

    public void removeProcessor(short msgCode) {
        processorMap.remove(msgCode);
    }

    public void messageReceived(String replyTo, String message) {
        IMsgProcessor action = processorMap.get(message.getMsgCode());
        try {
            if (action != null) {
                long s = System.currentTimeMillis();
                action.process(replyTo, message);
                if (logger.isDebugEnabled()) {
                    logger.debug("消息：{} 处理时间:{}", message.getMsgCode(),
                            System.currentTimeMillis() - s);
                }
            }
        } catch (Throwable e) {
            logger.error("处理接收到的消息异常，消息号:" + message.getMsgCode(), e);
        }
    }

}*/
