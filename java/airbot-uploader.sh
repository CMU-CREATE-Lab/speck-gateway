#!/bin/bash

java -Dgnu.io.rxtx.SerialPorts=/dev/tty.usbserial-A4008497 -Djava.library.path=./code/applications/dist -jar ./code/applications/dist/airbot-applications.jar --command-line;

