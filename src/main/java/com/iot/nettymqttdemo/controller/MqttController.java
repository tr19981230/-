package com.iot.nettymqttdemo.controller;

import com.iot.nettymqttdemo.config.MqttSendClientV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mqtt")
public class MqttController {

    // 注入优化后的 V2 版本
    @Autowired
    private MqttSendClientV2 mqttSendClientV2;

    @GetMapping(value = "/publishTopic")
    public String publishTopic(String topic, String sendMessage) {
        System.out.println("topic:" + topic);
        System.out.println("message:" + sendMessage);
        // 使用 V2 版本发送
        this.mqttSendClientV2.publish(false, topic, sendMessage);
        return "topic:" + topic + "\nmessage:" + sendMessage;
    }
}