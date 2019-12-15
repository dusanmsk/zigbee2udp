# Zigbee to Loxone (UDP) bridge

Simple docker-compose suite to connect zigbee ecosystem to Loxone using UDP packets (for zigbee->loxone communicaiton). 

Project is meant as follow-up to https://www.zigbee2mqtt.io/.

#### What you should know before you start
- you know how to create udp inputs in loxone
- you already have custom flashed zigbee usb token (https://www.zigbee2mqtt.io/) 

#### How to install

###### For baremetal with hdd or ssd (you don't care filesystem lifetime):

    sudo -i
    apt-get install -y docker-compose docker.io git
    mkdir /zigbee2mqtt
    cd /zigbee2mqtt
    git clone https://github.com/dusanmsk/zigbee2udp.git
    cd zigbee2udp
    
    # Edit variables file and set what is required
    # optionally remove mqtt section in docker-compose if you already have some mqtt server running somewhere else
    # optionally remove node-lox-mqtt-gateway section in docker-compose if you are running loxone-grafana suite
        
    ./run.sh
    # check everything is ok then break with ctrl+c
    
    ./start.sh
        
    
###### When using device with sdcard (rpi, orangepi, ...)

Example for OrangePI One:

- flash sdcard with ubuntu based armbian for orangepi one
- do not boot the card yet, run some partitioning software that support ext4 and create new partition behind current one, so:
    - before you begin there is single like this:
        - free space | ext4 (900MMB) | free space
    - now create new partition at the very end of the sdcard, but let there some space behind for wear leveling. Size >1G.
        - free space | ext4 (900MB) | free space | new partition (1+GB) | free space ( at least 200 MB, the more the better )
    - now resize first ext4 partition to gain all free space up to second partition
        - free space | ext4 (2+GB) | new partition (2+ GB) | free (>200MB)
        
*1 - you should use gparted or gparted live cd/usb if you are running windows or natively on linux

- boot the board, login with root:1234, change root password. You should create new user and use it later (or do everything as root like me :D )
- then run following:


    sudo -i
    apt-get -y install docker-compose docker.io git f2fs-tools
    
    # create filesystem on second partition and mountpoint
    mkfs.f2fs -f /dev/mmcblk0p2
    mkdir /zigbee2mqtt/
    echo "/dev/mmcblk0p2   /zigbee2mqtt/   f2fs rw,background_gc=on,user_xattr 0 1" >> /etc/fstab
    mount -a
    
    # move docker to second partition
    systemctl stop docker
    mkdir -p /zigbee2mqtt/var/lib/docker
    rm -rf /var/lib/docker/
    ln -s /zigbee2mqtt/var/lib/docker /var/lib/docker
    systemctl start docker

    # clone and setup    
    cd /zigbee2mqtt/
    git clone https://github.com/dusanmsk/zigbee2udp.git
    cd zigbee2udp
    
    # Edit variables file and set what is required
    # optionally remove mqtt section in docker-compose if you already have some mqtt server running somewhere else
    # optionally remove node-lox-mqtt-gateway section in docker-compose if you are running loxone-grafana suite
        
    ./run.sh
    # check everything is ok then break with ctrl+c
    
    ./start.sh


When done and everything goes ok, docker is running from second partition (check du -hs /mnt/rw/var/lib/docker), you
should switch rootfs to readonly:

    sudo apt-get install -y overlayroot    
    sudo cp overlayroot.local.conf /etc
    reboot
    
Rootfs is now in readonly and should not be modified anyway. If you need to modify rootfs, do

    overlayroot-chroot rm -f /etc/overlayroot.local.conf
    reboot
    
Make your changes, then reenable overlayroot again (previous step).

# How to configure

### zigbee->loxone
Create new UDP Virtual input in loxone on port you specified in configuration. Open UDP monitor and watch for a traffic,
then create virtual commands for messages you are interested in.

### loxone->zigbee
(Optionally) create new user for zigbee (name and password as you specified in variables).
Now create virtal output, which name starts with 'lox2zigbee_'. Whatever is after this name is used
as mqtt path to zigbee device, so for example virtual input with name 'lox2zigbee_led1_set_brightness' will send
mqtt message zigbee/led1/set { "brightness" : VALUE }, which is message for IKEA Tradfri LED bulb named "led1".
    

# How it works

Zigbee2mqtt receives zigbee messages from zigbee devices and sends them to mqtt topic.
Loxonebridge listens for that topics and extracts all flat data coming from zigbee device,
mapping it to udp messages which are then sent to loxone miniserver.

Example:

Zigbee device 0x00158d00044a1146 (named livingroom/aquara) sends payload:

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

For way from loxone to zigbee node-lox-mqtt-gateway is used. It connects to loxone socket as a web browser
and transforms all visible data to mqtt messages. Those are parsed by loxonebridge, preprocessed and sent to
zigbee2mqtt. Whenever any component (mostly virtual output) is named lox2zigbee_*, zigbee mqtt path
is extracted from component name and message is re-send to zigbee2mqtt and to zigbee device.

lox2zigbee_led1_set_brightness --> zigbee/led1/set { "brightness" : VALUE }




## Pairing using android phone

If you don't want to use you laptop everytime you need to pair new device, you should use any mqtt dashboard application capable
to receive and send messages. Here is quick howto for android mqtt dashboard app:

- click on (+) in bottom right corner
- Client ID: whatever you want
- Server: your mqtt server address
- Port: usually 1883
- click CREATE
- in Subscribe section
    - create listener for logs (mqtt address zigbee2mqtt/bridge/log)
- in Publish section
    - create new switch, name "Permit join", topic zigbee2mqtt/bridge/config/permit_join, text on/off, publish value true/false
    - create new text, name "Rename last", topic zigbee2mqtt/bridge/config/rename_last
    
Now connect to mqtt server, go to publish, switch pairing on. Check for logs in subscribe section that bridge confirmed
that pairing is on (message will ends with permit_join:true). Then you should pair zigbee device, you should see result of pairing in subscribe/logs.
You should also rename last joined device to some friendly name using publish/rename last event, simply write new name and publish it.    
And that's all.


# TODO

- document digital inputs
- implement bi-directional communication (to control zigbee devices from loxone)

example ikea led:

zigbee2mqtt/led1/set -m { "brightness" : 1 }

zigbee2udp/led1/set/brightness 1

