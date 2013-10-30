#!/bin/bash

java -Djna.library.path=./code/applications/dist -Djava.library.path=./code/applications/dist -cp ./code/applications/dist/speck-applications.jar org.specksensor.applications.CommandLineSpeck --set-interval=$1;
