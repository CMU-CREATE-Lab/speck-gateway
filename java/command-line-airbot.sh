#!/bin/bash

java -Dgnu.io.rxtx.SerialPorts=/dev/tty.usbserial-A4008497 -Djava.library.path=./code/applications/dist -cp ./code/applications/dist/airbot-applications.jar org.bodytrack.applications.airbot.CommandLineAirBot;
