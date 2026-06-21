package com.iot.nettymqttdemo.config;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 优化版 MQTT 发布客户端
 * 核心优化：单例长连接 + 自动重连 + 线程安全复用
 */
@Component
public class MqttSendClientV2 {

    private static final Logger logger = LoggerFactory.getLogger(MqttSendClientV2.class);

    @Autowired
    private MqttProperties mqttProperties;

    /**
     * 单例 MQTT 客户端实例（复用）
     */
    private MqttClient mqttClient;

    /**
     * 项目启动时：初始化连接
     */
    @PostConstruct
    public void init() {
        connect();
    }

    /**
     * 项目停止时：优雅断开连接
     */
    @PreDestroy
    public void destroy() {
        disconnect();
    }

    /**
     * 建立长连接
     */
    private void connect() {
        try {
            // 发布端 clientId 加个 "PUB-" 前缀，避免和订阅端冲突
            String pubClientId = "PUB-" + mqttProperties.getClientId();
            mqttClient = new MqttClient(mqttProperties.getHostUrl(), pubClientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(mqttProperties.getUsername());
            options.setPassword(mqttProperties.getPassword().toCharArray());
            options.setConnectionTimeout(mqttProperties.getTimeout());
            options.setKeepAliveInterval(mqttProperties.getKeepAlive());
            
            // 关键配置：开启自动重连
            options.setAutomaticReconnect(true);
            // 发布端建议 cleanSession=true，避免离线消息堆积
            options.setCleanSession(true);

            // 设置回调（处理连接状态、重连等）
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    logger.info("============================= 发布客户端【{}】连接成功（重连：{}）=============================", pubClientId, reconnect);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    logger.warn("发布客户端连接断开，等待自动重连...", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // 发布端一般不需要处理接收消息，空实现即可
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    try {
                        logger.info("向主题【{}】发送消息成功", String.join(",", token.getTopics()));
                    } catch (Exception e) {
                        logger.error("发送回调异常", e);
                    }
                }
            });

            mqttClient.connect(options);
            logger.info("============================= 发布客户端【{}】初始化完成 =============================", pubClientId);

        } catch (Exception e) {
            logger.error("发布客户端初始化失败", e);
            throw new RuntimeException("MQTT发布客户端初始化失败", e);
        }
    }

    /**
     * 发布消息（高性能：直接复用长连接）
     *
     * @param retained 是否保留
     * @param topic    主题
     * @param content  消息内容
     */
    public void publish(boolean retained, String topic, String content) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            logger.error("发布客户端未连接，无法发送消息");
            return;
        }

        try {
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(mqttProperties.getQos());
            message.setRetained(retained);
            
            // 直接复用连接发送，性能极高
            mqttClient.publish(mqttProperties.getServerTopic(topic), message);
            
            logger.debug("发送消息请求已提交 | topic={} | content={}", topic, content);
        } catch (MqttException e) {
            logger.error("发送消息失败", e);
        }
    }

    /**
     * 优雅断开连接
     */
    private void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                logger.info("发布客户端已优雅断开");
            }
        } catch (MqttException e) {
            logger.error("断开连接异常", e);
        }
    }
}