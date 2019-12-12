FROM python:3-alpine

RUN python -m pip install --upgrade pip
RUN pip3 install paho-mqtt

COPY loxone2zigbee.py /bridge.py

EXPOSE 4445
ENTRYPOINT [ "python3", "/bridge.py" ]

