#!/bin/bash

apt-get -y install rsync overlayroot

install rwsync.conf /etc/
install rwsync /usr/local/bin
install makeroot_rw /usr/local/bin
install makeroot_ro /usr/local/bin
install -m 0777 rwsync.service /etc/systemd/system/



