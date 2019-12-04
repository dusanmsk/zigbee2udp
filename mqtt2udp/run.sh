#!/bin/sh
while true; do
  python3 /mqtt2udp.py
  echo "Exited, restart in 60 sec"
  sleep 60
done