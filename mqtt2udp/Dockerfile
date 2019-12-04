FROM python:3-alpine

RUN python -m pip install --upgrade pip
RUN pip3 install paho-mqtt

COPY mqtt2udp.py /mqtt2udp.py
COPY run.sh /run.sh

ENTRYPOINT [ "/bin/sh", "/run.sh" ]

