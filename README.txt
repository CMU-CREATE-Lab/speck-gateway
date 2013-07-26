========================================================================================================================

                                                     Speck Gateway

========================================================================================================================

ABOUT
-----

The Speck Gateway is an application which downloads data samples from a Speck to the user's computer and optionally
uploads the data samples to a BodyTrack Datastore server such as Fluxtream (https://github.com/fluxtream/fluxtream-app)
or a Node Datastore Server (https://github.com/BodyTrack/node-datastore-server).

Data samples are stored in two forms: a local database and a CSV file.  By default, the database and CSV will be in a
subdirectory under the user's home directory, but the location can be overridden (see below for details). The exact path
for the data directory depends on the device ID, but a typical path would be:

      ~/CREATELab/Speck/Speck00343135321504100f17/

In that directory you'll find a data_samples.csv file as well as a database directory.  The database is an Apache Derby
database.  It's not really meant for manual editing, but it certainly can be if you wish.  Just use the Derby "ij" tool
and manipulate it like a normal SQL database.

========================================================================================================================

PREREQUISITES
-------------

You must have the following installed in order to build the application:

   * Java JDK 1.6+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
   * Ant (http://ant.apache.org/)

You must have the following installed in order to run the application:

   * Java JDK 1.6+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)

========================================================================================================================

DOWNLOADING THE GATEWAY APPLICATION
-----------------------------------

The source code is stored in a GitHub code repository at:

   https://github.com/CMU-CREATE-Lab/speck-gateway

To download the gateway, you can either download pre-built binaries from specksensor.org, download a snapshot of the
source code from GitHub, or use Git to fetch a (read-only) copy of the repository.  Instructions for each follow.


Download Pre-Built Binaries
---------------------------

If you don't care about easily updating to new versions of the gateway and/or you don't have (or want) Git and Ant
installed, then you may find it easiest to just download pre-built binaries.  To do so, simply download the zip at:

   http://specksensor.org/speck-gateway.zip

After the file finishes downloading, unzip it to your favorite location.  Then simply double-click the
speck-applications.jar jar file.


Download a Snapshot of the Source Code
--------------------------------------

If you don't care about easily updating to new versions of the gateway and/or you don't have (or want) Git installed,
then it's easiest to just download a snapshot of the source code from GitHub.  To do so, simply do the following:

1) Go to the home page for the repository (https://github.com/CMU-CREATE-Lab/speck-gateway)
2) Click the Zip download button
3) Decompress the archive.

You can now skip to the "Building the Gateway Application" section below.


Download the Source Code Using Git
----------------------------------

If you want to be able to easily update to a newer/different version of the gateway, then you're better off downloading
the source with Git.  Binaries and instructions for installing Git are available from the Git home page.

To get the code with Git, first open a command prompt and change the current directory to wherever you want the source
code to live.  Then do the following:

   $ git clone git://github.com/CMU-CREATE-Lab/speck-gateway.git
   ...

If you have already done a "git clone" before, and simply want to update to the latest revision, change to the
speck-gateway directory and run "git pull origin master".  Please refer to the Git documentation if you want to switch
to a specific commit.

========================================================================================================================

BUILDING THE GATEWAY APPLICATION
--------------------------------

To build the application, open a command prompt and change to the root directory of the source code.  Then change to
the java subdirectory and then run Ant.  It should look similar to the following:

   $ cd speck-gateway
   $ cd java
   $ ant
     Buildfile: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/build.xml
          [echo] Speck Gatway Version Number: 1.2.0

     clean-speck-core:
        [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/build
        [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist

     clean-speck-applications:
        [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build
        [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist

     clean-speck-gateway-webstart:
        [delete] Deleting directory /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/webstart/dist

     clean:

     build-speck-core:
         [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/build
         [javac] Compiling 28 source files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/build

     dist-speck-core:
         [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist
          [copy] Copying 26 files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist
           [jar] Building jar: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/core/dist/speck-core.jar

     build-speck-applications:
         [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build
         [javac] Compiling 5 source files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build
          [copy] Copying 4 files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/build

     dist-speck-applications:
         [mkdir] Created dir: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist
          [copy] Copying 27 files to /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist
           [jar] Building jar: /Users/chris/Documents/Work/Projects/SpeckGateway/speck-gateway/java/code/applications/dist/speck-applications.jar

     dist:

     all:

     BUILD SUCCESSFUL
     Total time: 7 seconds

The binaries are now built and you're ready to run the application.

========================================================================================================================

RUNNING THE GATEWAY APPLICATION
-------------------------------

There are actually three applications created when you build the binaries:

1) The gateway application which auto-connects to the first Speck it finds, and continually downloads data samples from
   the device, caches them locally, and then optionally uploads them to a BodyTrack Datastore server.

2) A command-line version of the gateway application.

3) A simple, command-line client for testing connectivity and basic interaction with a Speck.
   The command line client is good for testing connectivity and basic interaction but does not save data samples or
   provide upload support.

Most users will want to run the GUI gateway application.  To run any of the applications, you must have already built
the binaries. See the "Building the Gateway Application" section above.


Running the Speck Gateway (GUI)
-------------------------------

The easiest way to run the gateway is to simply double-click the speck-applications.jar created by the build.  You'll
find it in speck-gateway/java/code/applications/dist.  If, however, you prefer to launch it from the command line, do
the following...


To run the Speck Gateway, cd to the java directory and run the following shell (Mac/Linux) or batch (Windows) script:

   speck-gateway-gui.sh

   speck-gateway-gui.bat

Once the gateway is running, you shouldn't need to do anything.  It will auto-connect to the first Speck it finds and
begin downloading data samples.  You can optionally enter the host and your login for a BodyTrack Datastore server if
you wish to upload the data samples.


Running the Speck Gateway (Command Line)
----------------------------------------

To run the command line version of the Speck Gateway, cd to the java directory and run the following shell (Mac/Linux)
or batch (Windows) script:

   speck-gateway.sh

   speck-gateway.bat

Once the program is running, use the menu options to connect, configure uploading, etc.


Running the Command Line Client
-------------------------------

To run the command line client, cd to the java directory and run the following shell (Mac/Linux) or batch (Windows)
script:

   command-line-speck.sh

   command-line-speck.bat

The command line client has a menu which lists the various commands you can run to interact with the device.

========================================================================================================================

TIPS AND TRICKS
---------------

This section discusses some useful tips for running the gateway.


Command Line Options
--------------------

The gateway application supports the following command line options:

   --logging-level=<level>   Sets the logging level for the log file.  Has no effect on the console logging.  Valid
                             values are 'trace', 'debug', and 'info'.
   --command-line            The command line version will be used instead of the GUI version
   --config=<path>           Specify a path to a local config file, must be used in conjunction with the --command-line
                             switch. No connection to a device will be attempted (and thus no files will be downloaded).
                             Instead, the gateway will obtain the Speck ID (and thus which database to look in for
                             samples to upload) from this config file.  This is useful for times when you want to upload
                             previously-downloaded samples and/or you don't have the Speck to plug in.


Change the Logging Level
------------------------

You can set the logging level when you run the gateway by specifying the "--logging-level" command line option as
discussed above.  You can also change the logging level at runtime by using the "l" menu option when using the command
line version of the gateway.


View Statistics
---------------

Statistics for the number of files downloaded, uploaded, and deleted are printed periodically by the gateway.  You can
also request the statistics at any time by choosing the "s" command.


Change Data Directory
---------------------

By default, the Speck Gateway stores its files under a "CREATELab" subdirectory of the user's home directory.  This is
problematic for some users (e.g. schools with shared computers, security restrictions, etc.).  To change where files
are stored, you can launch the Speck Gateway and supply it with the "CreateLabHomeDirectory" system property. To do
so, open a command prompt window and navigate to the speck-gateway/java directory. Then run the following command,
replacing PATH_TO_DESIRED_DIRECTORY with the path to the directory in which you want files to be saved:

   java -DCreateLabHomeDirectory=PATH_TO_DESIRED_DIRECTORY -Djna.library.path=./code/applications/dist -Djava.library.path=./code/applications/dist -jar ./code/applications/dist/speck-applications.jar;

========================================================================================================================
