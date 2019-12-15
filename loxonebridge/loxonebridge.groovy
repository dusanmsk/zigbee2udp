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
import java.util.concurrent.Executors

/**
 * Zigbee <--> loxone bridge
 *
 * It has 2 parts:
 * - zigbee -> loxone is based on listening on mqtt events from zigbee2mqtt and sending them as udp packets to loxone
 * - loxone -> zigbee is based on listening on mqtt events from node-lox-mqtt-gateway
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
 * Messages from loxone to zigbee are processed by following way:
 * node-lox-mqtt-gateway connects to loxone as "web client" and see all controllers and values as connected user see in web browser
 * it sends all value changes to mqtt topic lox/ROOM/CATEGORY/COMPONENT_NAME/state { value : XYZ }
 * If the component is named "lox2zigbee_anything_after_is_zigbee_mqtt_path", it re-sends that value
 * to topic zigbee/anything/after/is/zigbee/mqtt { path: XYZ}
 *
 * For example if you want to control IKEA led bulb which is named led1 in zigbee2mqtt, its mqtt address is:
 * zigbee/led1/set { brightness: 100 }
 *
 * So create virtual output in loxone named:
 * lox2zigbee_led1_set_brightness
 *
 * and gateway will take care for resending to proper topic
 */

@Slf4j
class LoxoneZigbeeGateway {

    def RECONNECT_TIME_SEC = 30

    def LOXONE_ADDRESS = System.getenv("LOXONE_ADDRESS")
    def LOXONE_PORT = System.getenv("LOXONE_PORT") as Integer

    def slurper = new JsonSlurper()
    DatagramSocket sendUdpSocket        // sending data to loxone
    MqttClient mqttClient
    def sendingPool = Executors.newFixedThreadPool(10 )     // must send mqtt messages in parallel threads

    def setupMqtt() {
        def mqttUrl = "tcp://${System.getenv("MQTT_ADDRESS")}:1883"
        log.info("Connecting to mqtt ${mqttUrl}")
        mqttClient = new MqttClient(mqttUrl, "loxone2zigbee", null)
        mqttClient.connect()
        mqttClient.subscribe("zigbee/#")    // todo property
        mqttClient.subscribe("lox/#")       // todo property
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

    def run() {
        while (true) {
            try {
                setupMqtt()
            } catch (Exception e) {
                log.error("Exception caught", e)
            } finally {
                log.debug("Sleeping for ${RECONNECT_TIME_SEC} seconds ...")
                Thread.sleep(1000 * RECONNECT_TIME_SEC)
            }
        }
    }

    def processMessage(topic, message) {
        println "Topic: ${topic}, message: ${message}"
        if (topic.startsWith("zigbee/")) {
            processZigbeeToLoxone(topic, message)
        } else if (topic.startsWith("lox/") && topic.contains("/lox2zigbee")) {
            processLoxoneToZigbee(topic, message)
        }
    }

    def processLoxoneToZigbee(topic, message) {
        def tmp = topic.split("/")[-2].replace("lox2zigbee_","").split("_")
        def destZigbeeTopic = "zigbee/" + tmp[0..-2].join("/")
        def destValue = tmp[-1]
        def jsonObject = slurper.parseText(message)
        def value = jsonObject['value']
        def destJson = """{ "${destValue}" : "${value}" }"""
        // must send in another thread or publish will block ...
        sendingPool.submit({
            mqttClient.publish(destZigbeeTopic, destJson.getBytes(Charset.defaultCharset()), 2, false)
        })
    }

    def processZigbeeToLoxone(topic, message) {
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

