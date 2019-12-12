import socket
import paho.mqtt.client as mqtt
import paho.mqtt.publish


# --------------------------------

LISTEN_IP = "0.0.0.0"
LISTEN_PORT = 4445

# --------------------------------

MQTT_ADDRESS = "192.168.10.30"
MQTT_PORT = 1883

# --------------------------------

print("Starting loxone2zigbee bridge")

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind(("", 4445))
sock.listen(1)
conn, addr = sock.accept()
print('Connection address:', addr)
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

        print(mqtt_addr + " - " + mqtt_msg_body)
        paho.mqtt.publish.single(mqtt_addr, payload=mqtt_msg_body, qos=0, retain=False, hostname=MQTT_ADDRESS, port=MQTT_PORT, client_id="udp2mqtt", keepalive=60, will=None, auth=None, tls=None,protocol=mqtt.MQTTv311, transport="tcp")
    except:
        print("Error")

