import paho.mqtt.client as mqtt
import json
import socket

LOXONE_ADDRESS = "192.168.2.2"
LOXONE_PORT = 4444

MQTT_ADDRESS = "mosquitto"
MQTT_PORT = 1883

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))
    client.subscribe("zigbee2mqtt/#")

def on_message(client, userdata, msg):
    try:
        jsonData = json.loads(msg.payload)
        for key in jsonData.keys():
            udpData = msg.topic + "/" + key + " " + str(jsonData[key])
            sock.sendto(udpData.encode('utf-8'), (LOXONE_ADDRESS, LOXONE_PORT))
    except Exception as e:
        print("Error " + e)

client = mqtt.Client("mqtt2udp")
client.on_connect = on_connect
client.on_message = on_message

print("Connecting ...")
client.connect(MQTT_ADDRESS, MQTT_PORT, 60)
client.loop_forever(60)
