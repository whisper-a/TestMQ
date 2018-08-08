package com.mq;

import java.io.UnsupportedEncodingException;

/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */
public interface IMQ {

    void init(String[] args)throws Exception;

    void start()throws Exception;

    void publishMsg(String msg, int count) throws UnsupportedEncodingException;

    void consumeMsg(String msg);

    void initProducer()throws Exception;

    void initConsumer()throws Exception;
}
