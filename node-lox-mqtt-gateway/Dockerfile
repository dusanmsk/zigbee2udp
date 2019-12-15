FROM alpine
RUN apk update && apk add nodejs nodejs-npm git
RUN npm config set user 0; npm config set unsafe-perm true
RUN mkdir /tmp/install; cd /tmp/install; git clone https://github.com/alladdin/node-lox-mqtt-gateway.git .; npm -g --unsafe-perm install
CMD [ "lox-mqtt-gateway", "--NODE_CONFIG_DIR=/" ]
