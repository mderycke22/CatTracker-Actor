# IoT - Cat Tracker Backend

## Installation

### Prerequisites
- Scala IntelliJ plugin
- JDK 21+
- Scala 3.5.2+
- SBT 1.10.3+
- Docker

### Set up the MQTT broker

1. Pull the [Eclipse-Mosquitto](https://hub.docker.com/_/eclipse-mosquitto) Docker image: ``docker pull eclipse-mosquitto``
2. Create a directory ``$PWD/mosquitto/config`` (where ``$PWD is any directory on your machine``) and then create a ``mosquito.conf`` file inside.
3. You can use the following configuration as an example:
```
persistence true
persistence_location /mosquitto/data/
log_dest file /mosquitto/log/mosquitto.log
allow_anonymous true
listener 1883
```
4. Run ``docker run -it -p 1883:1883 -v "$PWD/mosquitto/config:/mosquitto/config" eclipse-mosquitto``
5. Use MQTTX to monitor the connections