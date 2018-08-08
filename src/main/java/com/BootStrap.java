package com;

import com.mq.IMQ;
import com.mq.MQFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */
public class BootStrap {
    private static Logger logger = LoggerFactory.getLogger(BootStrap.class);


    private static String mqType;


    public static void main(String[] args) {
        try {
            mqType = args[0];
            IMQ mq = MQFactory.createMQ(mqType);
            mq.init(args);
            mq.start();

/*            while (true){
                System.out.println("main thread sleep...");
                Thread.sleep(5000);
            }*/

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
