FROM groovy:3.0-jre13

COPY loxonebridge.groovy /
RUN groovy /loxonebridge.groovy init

ENTRYPOINT [ "groovy", "/loxonebridge.groovy" ]

