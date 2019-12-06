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

#### installation

    # login as zigbee
    
    # create filesystem on second partition (will be used to store runtime data by rwsync daemon)
    sudo mkfs.ext4 /dev/mmcblk0p2
    
    # install everything required
    sudo curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
    sudo apt-get install -y nodejs git make g++ gcc git python-dev python-pip python-setuptools mosquitto
    sudo pip install paho-mqtt

    # clone this repo        
    sudo git clone https://github.com/dusanmsk/zigbee2udp.git /opt/zigbee2udp
    sudo chown -R zigbee:zigbee /opt/zigbee2udp
    cd /opt/zigbee2udp
    git submodule init
    git submodule update

    # instal everything for zigbee2mqtt    
    cd zigbee2mqtt && npm install && cd ..

    # copy all service files    
    sudo cp orangepi/*.service /etc/systemd/system/
        
    # now prepare everything to switch rootfs to readonly mode
    cd readonlyroot
    sudo ./install.sh
    source /etc/rwsync.conf
    mkdir -p $RWSYNC_LOWER_DIR/zigbee2mqtt
    cp 
    
    sudo systemctl enable zigbee2mqtt.service
    sudo systemctl enable mqtt2udp.service

    # switch rootfs to readonly. From now it will not be possible to change anything on rootfs.
    # all changes will be lost after reboot.
    # to make permanent changes, you have to run overlayroot-chroot, comment out "overlayroot="tmpfs"" in /etc/overlayroot.local.conf and reboot
    # when done, uncomment and reboot again
    
    makeroot_ro    
