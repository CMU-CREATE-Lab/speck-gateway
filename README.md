# Speck Gateway

About
-----

The Speck Gateway is an application which downloads data samples from a Speck to the user's computer and optionally uploads the data samples to a server for storage, visualization, etc.

Prerequisites
-------------

### Hardware

The Speck Gateway should run on any moderately recent computer running Mac OS, Windows, or Linux with a USB port. Internet access is required if you intend to upload data samples.

### Software

The Speck Gateway has been tested on Mac OS 10.8, Mac OS 10.9, Windows XP, Windows 7, and Ubuntu Linux.  It should work on both 32-bit and 64-bit operating systems.

The Speck Gateway is a Java application, so you will need to have Java SE 6 or later installed. For running the Speck Gateway, it doesn't matter whether you install the JRE or the JDK, but the JRE will be a smaller download. If you only ever need to *run* Java applications, then the JRE is fine.  But if you expect to ever build the Speck Gateway from source code, or develop Java applications (i.e. write Java code), then you'll want the JDK.  Either way, you can download Java from: <http://www.oracle.com/technetwork/java/javase/downloads/index.html>

Although the Speck Gateway requires Java to be installed, it does not require the Java Plugin to be installed or enabled in your browser.

Downloading
-----------

We offer two pre-built versions of the Speck Gateway:

* Downloadable, standalone applications for Mac OS and Windows
* A downloadable zip file for Linux users or users having special installation configuration needs (see the *Advanced Use Cases* section below)

Download the software at: <http://specksensor.org/software/>

Installing
----------

Do the following to install the Speck Gateway application:

* **Mac OS**: you will have downloaded a disk image file (.dmg).  Double-click it to mount and open the disk image. Once it is open, simply drag the Speck Gateway application to your computer's Applications folder.  Once it has copied, you may eject the disk image.
* **Windows**: you will have downloaded an installer file (.msi).  Double-click to open it and follow the installation wizard steps to install the Speck Gateway application.
* **Linux**: you will have downloaded a Zip file (.zip).  Unzip the zip file to your preferred location.

Running
-------

First, make sure you have Java SE 6 or later installed.  See the *Prerequisites* section above for details.  If Java is
already installed, do the following to run the Speck Gateway application:

* **Mac OS**: double-click the Speck Gateway application in your Applications folder.
* **Windows**: select the Speck Gateway application in the Start menu, or double-click the icon on your desktop.
* **Linux**: double-click the `speck-applications.jar`

Usage
-----

Once the Speck Gateway is running, it immediately begins searching for a Speck to connect to.  If you haven't already, go ahead and plug your Speck into your computer.  The Speck Gateway should connect to the Speck and immediately begin downloading data samples.  Simply let it continue to download data samples.

Note that the Speck will record new data any time it is powered on, including when plugged in to your computer.  Thus, the Speck Gateway won't ever actually finish downloading because new samples are continuously being recorded.  However, the Speck only makes new data samples available for download every 30 seconds, so you'll see pauses in the downloading of data samples once the Speck Gateway has downloaded all historical data and "caught up" to the present time.

The Speck Gateway can upload downloaded data samples.  To do so, enter the hostname, username, and password for the receiving server and click the *Enable Uploads* button.  Examples of servers which can receive Speck data samples are a BodyTrack Datastore server such as Fluxtream (https://github.com/fluxtream/fluxtream-app), or an instance of a Node Datastore Server (https://github.com/BodyTrack/node-datastore-server).

Some models of the Speck sensor allow you to change the interval at which it logs data samples (default is 1 minute) with the Speck Gateway.  To do so, simply choose a different value in the *Logging Interval* popup menu.  Note that, regardless of the specified logging interval, the Speck will always record at 1 second intervals as long as the Speck Gateway is connected to it.

Data
----

Downloaded data samples are stored in two forms: a local database and a CSV file.  The database and CSV will be in a subdirectory under the user's home directory.  The exact path for the data directory depends on the device ID, but a typical path would be:

      ~/CREATELab/Speck/Speck00343135321504100f17/

In that directory you'll find a `data_samples.csv` file as well as a database directory.  The database is an Apache Derby database.  It's not really meant for manual editing, but it certainly can be if you wish.  Just use the Derby `ij` tool and manipulate it like a normal SQL database.

Advanced Use Cases
------------------

Some users may have more complex installation needs which can currently only be satisfied with the zip file version of the software.  Other users may prefer to build the application from source.  The following sections cover those use cases.  Users whose needs are met with the standalone applications discussed above may safely ignore the rest of this document.

### Using the Zip File Version

The Zip file version of the software is available for download at: <http://specksensor.org/software/speck-gateway.zip>

After downloading and unzipping, the simplest way to run the software is to simply double-click the `speck-applications.jar` Jar file. Doing so will run the Speck Gateway, and it will behave exactly the same as the pre-built, standalone applications discussed above.

Other ways of running the software are discussed below in the *Running* section.  See the *Tips and Tricks* section below for advanced configuration options.

### Building from Source

At a minimum, you must have the following installed in order to build the source code:

   * Java JDK 1.6+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
   * Ant (http://ant.apache.org/)

The source code is stored in a GitHub code repository at: <https://github.com/CMU-CREATE-Lab/speck-gateway>

To build, open a command prompt and change to the root directory of the source code.  Then change to the `java` subdirectory and then run Ant.  It should look similar to the following:

```
$ cd speck-gateway
$ cd java
$ ant
Buildfile: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/build.xml
     [echo] Speck Gatway Version Number: 2.0.0

clean-speck-core:
   [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/build
   [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist

clean-speck-applications:
   [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build
   [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist

clean-speck-gateway-web-distro:
   [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/web-distro/dist

clean:

build-speck-core:
    [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/build
    [javac] Compiling 31 source files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/build
    [javac] Note: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/src/org/specksensor/DataSampleUploadHelper.java uses or overrides a deprecated API.
    [javac] Note: Recompile with -Xlint:deprecation for details.

dist-speck-core:
    [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist
     [copy] Copying 26 files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist
      [jar] Building jar: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist/speck-core.jar

build-speck-applications:
    [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build
    [javac] Compiling 6 source files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build
     [copy] Copying 5 files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build
     [copy] Copying 2 files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build

dist-speck-applications:
    [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist
     [copy] Copying 27 files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist
      [jar] Building jar: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist/speck-applications.jar

dist:

all:

BUILD SUCCESSFUL
Total time: 3 seconds
```

The binaries are now built and you're ready to run the application.  See the *Running* section below for details. See the *Tips and Tricks* section below for advanced configuration options.

Note that doing the above will simply create all the Java binaries (Jar files) required for running the code.  It won't create the standalone application versions of the software such as we provide for Mac OS and Windows.  Details for creating the standalone applications are beyond the scope of this document.

### Running

There are actually three applications created when you build the binaries or work with the jars from the Zip file version:

1. The gateway GUI application (discussed above) which auto-connects to the first Speck it finds, and continually downloads data samples from the device, caches them locally, and then optionally uploads them to a server.
2. A command-line version of the gateway application.
3. A simple, command-line client for testing connectivity and basic interaction with a Speck. The command line client is good for testing connectivity and basic interaction but does not save data samples or provide any upload support.

#### Running the Speck Gateway (GUI)

The easiest way to run the gateway is to simply double-click the speck-applications.jar created by the build.  You'll find it in `speck-gateway/java/code/applications/dist`.  If, however, you prefer to launch it from the command line, do the following...

To run the Speck Gateway, `cd` to the `java` directory and run the `speck-gateway-gui.sh` script (Mac/Linux) or `speck-gateway-gui.sh` batch script (Windows).

Once the Speck Gateway is running, you shouldn't need to do anything.  It will auto-connect to the first Speck it finds and begin downloading data samples.  You can optionally enter the host and your login for the server if you wish to upload the data samples.

#### Running the Speck Gateway (Command Line)

To run the command line version of the Speck Gateway, `cd` to the `java` directory and run the `speck-gateway.sh` script (Mac/Linux) or the `speck-gateway.bat` batch script (Windows).

Once the program is running, use the menu options to connect, configure uploading, etc.

#### Running the Command Line Client

To run the command line client, `cd` to the `java` directory and run the `command-line-speck.sh` script (Mac/Linux) or the `command-line-speck.bat` batch script (Windows).

The command line client has a menu which lists the various commands you can run to interact with the device.

Tips and Tricks
---------------

This section discusses some useful tips for running the Speck Gateway from the command line (either from the jars provided in the Zip file version, or from binaries created by building from source).

### Command Line Options

The gateway application supports the following command line options:

<dl>
  <dt>`--logging-level=<level>`</dt>
  <dd>Sets the logging level for the log file.  Has no effect on the console logging.  Valid values are `trace`, `debug`, and `info`.</dd>
  <dt>`--command-line`</dt>
  <dd>The command line version will be used instead of the GUI version</dd>
  <dt>`--config=<path>`</dt>
  <dd>Specify a path to a local config file, must be used in conjunction with the `--command-line` switch. No connection to a device will be attempted (and thus no files will be downloaded). Instead, the gateway will obtain the Speck ID (and thus which database to look in for samples to upload) from this config file.  This is useful for times when you want to upload previously-downloaded samples and/or you don't have the Speck to plug in.</dd>
</dl>

### Change the Logging Level

You can set the logging level when you run the gateway by specifying the `--logging-level` command line option as discussed above.  You can also change the logging level at runtime by using the `l` menu option when using the command line version of the gateway.

### View Statistics

Statistics for the number of files downloaded, uploaded, and deleted are printed periodically by the gateway.  You can also request the statistics at any time by choosing the `s` command.

### Change Data Directory

By default, the Speck Gateway stores its files under a `CREATELab` subdirectory of the user's home directory.  This is problematic for some users (e.g. schools with shared computers, security restrictions, etc.).  To change where files are stored, you can launch the Speck Gateway and supply it with the `CreateLabHomeDirectory` system property. To do so, open a command prompt window and navigate to the `speck-gateway/java` directory. Then run the following command, replacing `PATH_TO_DESIRED_DIRECTORY` with the path to the directory in which you want files to be saved:

    java -DCreateLabHomeDirectory=PATH_TO_DESIRED_DIRECTORY \
         -Djna.library.path=./code/applications/dist \
         -Djava.library.path=./code/applications/dist \
         -jar ./code/applications/dist/speck-applications.jar;