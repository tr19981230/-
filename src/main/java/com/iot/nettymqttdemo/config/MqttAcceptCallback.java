package com.iot.nettymqttdemo.config;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * @Description : MQTT接受服务的回调类
 * @Author : Sherlock
 * @Date : 2023/8/1 16:29
 */
@Component
public class MqttAcceptCallback implements MqttCallbackExtended {

    private static final Logger logger = LoggerFactory.getLogger(MqttAcceptCallback.class);

    @Autowired
    private MqttAcceptClient    mqttAcceptClient;

    @Autowired
    private MqttProperties      mqttProperties;

    /**
     * 客户端断开后触发
     *
     * @param throwable
     */
    @Override
    public void connectionLost(Throwable throwable) {
        logger.info("连接断开，可以重连");
        if (MqttAcceptClient.client == null || !MqttAcceptClient.client.isConnected()) {
            logger.info("【emqx重新连接】....................................................");
            mqttAcceptClient.reconnection();
        }
    }

    /**
     * 客户端收到消息触发
     *
     * @param topic       主题
     * @param mqttMessage 消息
     */
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        logger.info("【接收消息主题】:" + topic);
        logger.info("【接收消息Qos】:" + mqttMessage.getQos());
        logger.info("【接收消息内容】:" + new String(mqttMessage.getPayload()));
        /**
         * 从这里接受到服务器消息，这里写业务逻辑
         * */
        // 2. 根据主题/消息类型分发业务逻辑
        dispatchBusiness(topic, "json数据");
    }

    /**
     * 业务逻辑分发器
     */
    private void dispatchBusiness(String topic, String messageDto) {
        // 示例1：根据主题后缀判断业务
        if (topic.endsWith("/report")) {
            handleDeviceReport(messageDto); // 处理设备数据上报
        } else if (topic.endsWith("/alarm")) {
            handleDeviceAlarm(messageDto); // 处理设备告警
        } else if (topic.endsWith("/cmd/result")) {
            handleCmdResult(messageDto); // 处理指令执行结果
        }

        // 示例2：也可以根据消息类型判断
//        if ("report".equals(messageDto.getMsgType())) {
//            handleDeviceReport(messageDto);
//        }
    }

    /**
     * 业务1：处理设备数据上报
     */
    private void handleDeviceReport(String messageDto) {
        logger.info("【处理数据上报】设备ID={} | 数据={}", messageDto ,messageDto+"业务数据 -测试");
        // 你的业务逻辑：
        // 1. 存入数据库
        // 2. 写入 Redis 缓存
        // 3. 判断是否超过阈值，触发告警
    }
    /**
     * 业务2：处理设备告警
     */
    private void handleDeviceAlarm(String messageDto) {
        logger.warn("【处理设备告警】设备ID={} | 告警数据={}", messageDto, "处理设备告警");
        // 你的业务逻辑：
        // 1. 保存告警记录
        // 2. 推送通知给前端/短信/邮件
        // 3. 自动下发处理指令给设备
    }

    /**
     * 业务3：处理指令执行结果
     */
    private void handleCmdResult(String messageDto) {
        logger.info("【处理指令结果】设备ID={} | 结果={}", messageDto,"处理指令执行结果");
        // 你的业务逻辑：
        // 1. 更新指令状态为已完成
        // 2. 通知调用方指令执行结果
    }


    /**
     * 发布消息成功
     *
     * @param token token
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        String[] topics = token.getTopics();
        for (String topic : topics) {
            logger.info("向主题【" + topic + "】发送消息成功！");
        }
        try {
            MqttMessage message = token.getMessage();
            byte[] payload = message.getPayload();
            String s = new String(payload, "UTF-8");
            logger.info("【消息内容】:" + s);
        } catch (Exception e) {
            logger.error("MqttAcceptCallback deliveryComplete error,message:{}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 连接emq服务器后触发
     *
     * @param b
     * @param s
     */
    @Override
    public void connectComplete(boolean b, String s) {
        logger.info("============================= 客户端【" + MqttAcceptClient.client.getClientId() + "】连接成功！=============================");
        // 以/#结尾表示订阅所有以test开头的主题
        // 订阅所有机构主题
        mqttAcceptClient.subscribe(mqttProperties.getDefaultTopic(), mqttProperties.getQos());
    }
}
