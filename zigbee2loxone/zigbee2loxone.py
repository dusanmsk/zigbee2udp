import paho.mqtt.client as mqtt
import json
import socket
import os

# --------------------------------

LOXONE_ADDRESS = os.getenv("LOXONE_ADDRESS")
LOXONE_PORT = os.getenv("LOXONE_PORT")
MQTT_ADDRESS = os.getenv("MQTT_ADDRESS")
MQTT_PORT = 1883

DEBUG = os.getenv("DEBUG", 0)
def log(*args):
    if DEBUG == 1:
        print(args)

# --------------------------------

log("Starting mqtt2udp bridge")
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

def on_connect(client, userdata, flags, rc):
    log("Connected with result code " + str(rc))
    client.subscribe("zigbee/#")

def on_message(client, userdata, msg):
    try:
        jsonData = json.loads(msg.payload)
        for key in jsonData.keys():
            udpData = msg.topic + "/" + key + " " + str(jsonData[key])
            sock.sendto(udpData.encode('utf-8'), (LOXONE_ADDRESS, LOXONE_PORT))
    except Exception as e:
        log("Error " + e)

client = mqtt.Client("zigbee2loxone")
client.on_connect = on_connect
client.on_message = on_message

log("Connecting ...")
client.connect(MQTT_ADDRESS, MQTT_PORT)
client.loop_forever()
