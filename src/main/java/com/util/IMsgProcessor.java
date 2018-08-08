package com.util;

/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */
public interface IMsgProcessor {

    void process(String replyTo, BinaryMsg msg);

}
