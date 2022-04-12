FROM openjdk:16

COPY build/dist/observer-1.0-SNAPSHOT /run

CMD bash ./run/bin/observer