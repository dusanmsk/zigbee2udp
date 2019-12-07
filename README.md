# Zigbee to Loxone (UDP) bridge

Simple docker-compose suite to connect zigbee ecosystem to Loxone using UDP packets. 

Project is meant as follow-up to https://www.zigbee2mqtt.io/.

#### How to install

- You need to follow https://www.zigbee2mqtt.io/ to prepare hardware and setup zigbee2mqtt bridge. When done, continue reading this manual.
- Create new UDP Virtual input in loxone which will listen (for example) on udp port 4444
- Then:


###### For baremetal with hdd or ssd (you don't care filesystem lifetime):

    sudo -i
    apt-get install -y docker-compose docker.io git
    mkdir /zigbee2mqtt
    cd /zigbee2mqtt
    git clone https://github.com/dusanmsk/zigbee2udp.git
    cd zigbee2udp
    
    # Edit mqtt2udp/mqtt2udp.py and set loxone address and port
    # optionally set timezone in docker-compose.yml zigbee2mqtt section
    
    ./start.sh
    
    
###### When using device with sdcard (rpi, orangepi, ...)

Example for OrangePI One:

- flash sdcard with ubuntu based armbian for orangepi one
- do not boot the card yet, run some partitioning software (*1) and create empty second partion after first one
    - before you begin there are be partitions like this:
        - free space | small primary linux | free space
    - now create new partition at the very end of the sdcard, but let there some space behind for wear leveling. Size >1G.
        - free space | primary linux | free space | primary linux | free space ( at least 200 MB, the more the better )
    - now resize first ext4 partition to gain all free space up to second partition (you should skip this so armbian will do it on first boot)
        - free space | ext4 (rootfs) | linux (x GB) | free (>200MB)
        
*1 - you should use gparted or gparted live cd/usb or any windows partitioning software able to handle ext4 partitions        
    
- boot the board, login with root:1234, change root password. You should create new user and use it later (or do everything as root like me :D )
- then run following:


    sudo -i
    apt-get -y install docker-compose docker.io git f2fs-tools
    mkfs.f2fs -f /dev/mmcblk0p2
    mkdir /zigbee2mqtt/
    echo "/dev/mmcblk0p2   /zigbee2mqtt/   f2fs rw,background_gc=on,user_xattr 0 1" >> /etc/fstab
    mount -a
    mkdir -p /zigbee2mqtt//var/lib/docker
    systemctl stop docker
    rm -rf /var/lib/docker/
    ln -s /zigbee2mqtt/var/lib/docker /var/lib/docker
    systemctl start docker
    docker ps -a
    
    cd /zigbee2mqtt/
    git clone https://github.com/dusanmsk/zigbee2udp.git
    cd zigbee2udp
    
    # Edit mqtt2udp/mqtt2udp.py and set loxone address and port
    # optionally set timezone in docker-compose.yml zigbee2mqtt section
        
    ./start.sh


When done and everything goes ok, docker is running from second partition (check du -hs /mnt/rw/var/lib/docker), you
should switch rootfs to readonly:
    
    sudo cp overlayroot.local.conf /etc
    reboot
    
Rootfs is now in readonly and should not be modified anyway. If you need to modify rootfs, do

    overlayroot-chroot rm -f /etc/overlayroot.local.conf
    reboot
    
Make your changes, then reenable overlayroot again (previous step).    

## TODO pairing and renaming using android phone


#### How it works

Zigbee2mqtt receives zigbee messages from zigbee devices and sends them to mqtt topic.
Mqtt2udp listens for that topics and extracts all flat data coming from zigbee device,
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

# TODO mqtt messages for configuration

# TODO

- document digital inputs
- implement bi-directional communication (to control zigbee devices from loxone)
