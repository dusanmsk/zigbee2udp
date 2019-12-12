import socket
import paho.mqtt.client as mqtt
import paho.mqtt.publish
import os

LISTEN_IP = ""
LISTEN_PORT = 4445
MQTT_ADDRESS = os.getenv("MQTT_ADDRESS")
MQTT_PORT = 1883

DEBUG = os.getenv("DEBUG", 0)
def log(*args):
    if DEBUG == 1:
        print(args)


log("Starting loxone2zigbee bridge")

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind(("", 4445))
sock.listen(1)
conn, addr = sock.accept()
log('Connection address:', addr)
while True:
    try:
        data = conn.recv(1024)
        if not data:
            break
        msg = str(data, 'utf-8').strip()
        msg_splt = msg.split("/")
        adr_splt = msg_splt[:-2]
        mqtt_addr = '/'.join(adr_splt)
        value_name = msg_splt[-2]
        value = msg_splt[-1]
        mqtt_msg_body = '{ "%s" : "%s" }' % (value_name, value)

        log("Sending " + mqtt_addr + " - " + mqtt_msg_body)
        paho.mqtt.publish.single(mqtt_addr, payload=mqtt_msg_body, qos=0, retain=False, hostname=MQTT_ADDRESS, port=MQTT_PORT, client_id="loxone2zigbee", keepalive=60, will=None, auth=None, tls=None,protocol=mqtt.MQTTv311, transport="tcp")
        log("Sent")
    except:
        log("Error")

