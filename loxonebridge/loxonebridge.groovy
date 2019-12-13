@Grab(group = 'org.eclipse.paho', module = 'mqtt-client', version = '0.4.0')
@Grab(group = 'org.slf4j', module = 'slf4j-api', version = '1.6.1')
@Grab(group = 'org.slf4j', module = 'slf4j-simple', version = '1.6.1')

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

import java.nio.charset.Charset

/**
 * Zigbee <--> loxone bridge
 *
 * It has 2 parts:
 * - zigbee -> loxone is based on listening on mqtt events from zigbee2mqtt and sending them as udp packets to loxone
 * - loxone -> zigbee is based on listening for udp packets from loxone and sending them as mqtt messages to zigbee2mqtt
 *
 * Messages from zigbee2mqtt to loxone are processed as following example:
 *
 * zigbee/sensor_1 { "temperature":24.93, "linkquality":47, "battery":95 }*
 * is translated to 3 udp messages:
 *
 * zigbee/sensor_1/temperature 24.93
 * zigbee/sensor_1/linkquality 47
 * zigbee/sensor_1/battery 95
 *
 * They should be easily parsed in loxone as:
 * zigbee/sensor_1/temperature \v
 *
 * UDP commands from loxone are processed as following example:
 *
 * udp command "zigbee/led1/set/brightness 255"
 *
 * is transferred to zigbee as following mqtt message:
 *
 * zigbee/led1/set { "brightness" : "255" }*
 */


@Slf4j
class LoxoneZigbeeGateway {

    def RECONNECT_TIME_SEC = 30
    def UDP_PORT = 4445

    def LOXONE_ADDRESS = System.getenv("LOXONE_ADDRESS")
    def LOXONE_PORT = System.getenv("LOXONE_PORT") as Integer

    def slurper = new JsonSlurper()
    DatagramSocket listenUdpSocket      // listening for data from loxone
    DatagramSocket sendUdpSocket        // sending data to loxone
    MqttClient mqttClient


    // zigbee->loxone
    def setupMqtt() {
        def mqttUrl = "tcp://${System.getenv("MQTT_ADDRESS")}:1883"
        log.info("Connecting to mqtt ${mqttUrl}")
        mqttClient = new MqttClient(mqttUrl, "loxone2zigbee_" + ProcessHandle.current().pid(), null)
        mqttClient.connect()
        mqttClient.subscribe("zigbee/#")
        log.info("Connected to mqtt ${mqttUrl} and ready")

        sendUdpSocket = new DatagramSocket()

        mqttClient.setCallback(new MqttCallback() {
            @Override
            void connectionLost(Throwable throwable) {}

            @Override
            void messageArrived(String topic, MqttMessage mqttMessage) {
                try {
                    processMessage(topic, new String(mqttMessage.payload))
                } catch (Exception e) {
                    log.error("MQTT message processing failed", e)
                }
            }

            @Override
            void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {}
        })

    }

    // loxone->zigbee
    def listenUdp() {
        listenUdpSocket = new DatagramSocket(UDP_PORT);
        while (true) {
            byte[] buffer = new byte[1024]
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            listenUdpSocket.receive(packet);
            String msg = new String(packet.getData(), 0, packet.getLength());
            def splt = msg.trim().split("\\{")
            def mqttTopic = splt[0].trim()
            def jsonValue = "{ ${splt[1].trim()}"
            log.debug("Sending to ${mqttTopic} value ${jsonValue}")
            mqttClient.publish(mqttTopic, new MqttMessage(jsonValue.getBytes(Charset.defaultCharset())))
        }
    }

    def run() {
        while (true) {
            try {
                setupMqtt()
                listenUdp()
            } catch (Exception e) {
                log.error("Exception caught", e)
            } finally {
                mqttClient?.close()
                listenUdpSocket?.close()
                log.debug("Sleeping for ${RECONNECT_TIME_SEC} seconds ...")
                Thread.sleep(1000 * RECONNECT_TIME_SEC)
            }
        }
    }

    def processMessage(topic, message) {
        println "Topic: ${topic}, message: ${message}"
        if (!topic.startsWith("zigbee/")) {
            return;
        }
        if (message.contains("{") && message.contains("}")) {
            def jsonObject = slurper.parseText(message)
            jsonObject.keySet().each { key ->
                def value = jsonObject[key].toString()
                sendToLoxoneUDP("${topic}/${key} ${value}")
            }
        } else {
            sendToLoxoneUDP("${topic} ${message}")
        }
    }

    def sendToLoxoneUDP(String msg) {
        log.debug("Sending UDP ${msg}")
        def packet = new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(LOXONE_ADDRESS), LOXONE_PORT)
        sendUdpSocket.send(packet)
    }
}


// only grab artifacts and exit
if (args.toString().contains("init")) {
    println "Grab artifacts"
    System.exit(0)
}

new LoxoneZigbeeGateway().run()

