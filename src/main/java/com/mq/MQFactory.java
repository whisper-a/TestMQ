package com.mq;

/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */
public class MQFactory {

    public static IMQ createMQ(String type){
        if(type.equalsIgnoreCase("rabbit")){
            return new RabbitMQ();
        }else if(type.equalsIgnoreCase("rocket")){
            return new RocketMQ();
        }

        return null;
    }
}
