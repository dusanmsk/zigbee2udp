# work in progress and maybe never be finished

# how to install on orangepi one

It is not possible to run docker on orangepi when using overlayroot so docker-compose is not an option.
We need to install everything on bare metal and then switch to overlayroot for sdcard/boot protection.

- flash sdcard with ubuntu based armbian for orangepi one
- do not boot the card yet, run some partitioning software (*1) and:
    - at the beginning there should be partitions like this:
        - free space | primary ext4 | free space
    - now create new partition at the very end of the sdcard (but let there some space behind for wear leveling), size by your needs, will be used to store all persistent data, for example:
        - free space | primary ext4 | free space | primary ext4 (512 MB) | free space ( at least 200 MB, better more )
    - now resize first ext4 partition to gain all free space up to second ext4 partition
        - free space | ext4 (x GB) | ext4 (512MB) | free (>200MB)
    
- boot the board, login with root:1234, change root password and create user 'zigbee'
- logout then login back as zigbee

*1 - you should use gparted or gparted live cd/usb or any windows partitioning software able to handle ext4 partitions

#### zigbee2mqtt installation
follow https://www.zigbee2mqtt.io/getting_started/running_zigbee2mqtt.html for reference, quick instructions are:

    sudo apt install -y git python-dev python-pip python-setuptools mosquitto

    sudo mkdir /opt/zigbee2mqtt && chown zigbee:zigbee /opt/zigbee2mqtt
    cd /opt/zigbee2mqtt
    git clone --recurse-submodules https://github.com/dusanmsk/zigbee2udp.git .
    
    cd zigbee2mqtt
    sudo curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
    sudo apt-get install -y nodejs git make g++ gcc
    npm install
    cd ..
    
    
    # sudo journalctl -u zigbee2mqtt.service -f
    
    pip install paho-mqtt
    
    sudo cp *.service /etc/systemd/system/
    sudo systemctl enable zigbee2mqtt.service
    sudo systemctl enable mqtt2udp.service
    TODO systemctl enable 

You have to edit zigbee2mqtt/data/configuration.yaml if required and mqtt2udp/mqtt2udp.py to set loxone address and udp port where packets will be send to.    
    
    