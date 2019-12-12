FROM python:3-alpine

RUN python -m pip install --upgrade pip
RUN pip3 install paho-mqtt

COPY zigbee2loxone.py /bridge.py

ENTRYPOINT [ "python3", "/bridge.py" ]

