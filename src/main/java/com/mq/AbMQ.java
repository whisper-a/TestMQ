package com.mq;

/**
 * @author sunrui
 * @date 2018-07-25
 * @descprition
 */
public abstract class AbMQ implements IMQ{

    public String generateMsg(int size){

        StringBuilder builder = new StringBuilder();
        while(size-- > 0){
            builder.append("s");
        }
        return builder.toString();
    }
}
