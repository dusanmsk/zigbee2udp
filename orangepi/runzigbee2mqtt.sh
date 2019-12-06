#!/bin/bash

if [ -d data ]; then
  rm -rf data
fi

if [ ! -f data ]; then
  source /etc/rwsync.conf
  ln -s $RWSYNC_UPPER_DIR/zigbee2mqtt data
fi

