#!/bin/bash

java -Djna.library.path=./code/applications/dist -Djava.library.path=./code/applications/dist -cp ./code/applications/dist/airbot-applications.jar org.bodytrack.applications.airbot.CommandLineAirBot;
