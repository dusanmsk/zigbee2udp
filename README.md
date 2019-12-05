# Zigbee to Loxone (UDP) bridge

Simple docker-compose suite to connect zigbee ecosystem to Loxone using UDP packets. 

Project is meant as follow-up to https://www.zigbee2mqtt.io/.

#### How to setup

- You need to follow https://www.zigbee2mqtt.io/ to prepare hardware and setup zigbee2mqtt bridge. When done, continue reading this manual.
- Create new UDP Virtual input in loxone which will listen (for example) on udp port 4444
- Then:


    sudo apt install docker-compose docker.io git
    git clone --recurse-submodules https://github.com/dusanmsk/zigbee2udp.git zigbee2mqtt
    
- Edit mqtt2udp/mqtt2udp.py and set loxone address and port
- optionally set timezone in docker-compose.yml zigbee2mqtt section


    ./run.sh

After you pair some zigbee devices, you shold edit data/configuration.yml to disable pairing and edit friendly names for paired devices
and restart containers.

For example:

    homeassistant: false
    permit_join: false              # <<<<
    mqtt:
      base_topic: zigbee2mqtt
      server: 'mqtt://localhost'
    serial:
      port: /dev/ttyACM0
    devices:
      '0x00158d00044a1146':
        friendly_name: 'livingroom/aquara'      # <<<<
      '0x00158d0003f47db9':
        friendly_name: 'kitchen/aquara'         # <<<<


#### How it works

Zigbee2mqtt receives zigbee messages from zigbee devices and sends them to mqtt topic.
Mqtt2udp listens for that topics and extracts all flat data coming from zigbee device,
mapping it to udp messages which are then sent to loxone miniserver.

Example:

Zigbee device 0x00158d00044a1146 sends payload

    {
        "battery":100,
        "voltage":3055,
        "temperature":25.61,
        "humidity":44.2,
        "pressure":975,
        "linkquality":115
    } 

then following udp messages will be send to loxone:

    zigbee2mqtt/livingroom/aquara/battery 100
    zigbee2mqtt/livingroom/aquara/voltage 3055
    zigbee2mqtt/livingroom/aquara/temperature 25.61
    zigbee2mqtt/livingroom/aquara/humidity 44.2
    zigbee2mqtt/livingroom/aquara/pressure 975
    zigbee2mqtt/livingroom/aquara/linkquality 115
    
You should start UDP monitor in loxone, wait until all required messages will be received
and then create virtual udp command for each value you are interested for.

For example create analog input with command recognition:

    zigbee2mqtt/livingroom/aquara/temperature \v
   
... and you will receive livingroom temperature as analog value.

# TODO

- document digital inputs
- implement bi-directional communication (to control zigbee devices from loxone)
