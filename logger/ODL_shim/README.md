#Logger for ODL shim layer

The Logger for ODL shim layer is one of the several tools which are provided in NetIDE project for diagnosing network problems. It is composed by two modules. The first one, called 'logpub' captures the messages exchanged between shim layer and backend and send them to a queue. The second one retrieves the messages and prints them into a terminal.
RabbitMQ is used in order to develop this tool.

## Installation

###First step is to install the Erlang package

RabbitMQ uses Erlang in order to manage the broker. Installing Erlang in Ubuntu from the terminal:

*sudo apt-get update
*sudo apt-get install Erlang

###Second step is to install the RabbitMQ Server

http://www.rabbitmq.com/releases/rabbitmq-server/v3.4.4/rabbitmq-server_3.4.4-1_all.deb

You can either download it with the link above and install with dpkg (recommended), or use the APT repository (see below).

Add the following line to your /etc/apt/sources.list:
deb http://www.rabbitmq.com/debian/ testing main

To avoid warnings about unsigned packages, add our public key to your trusted key list using apt-key(8), Run the following two commands:
* ```wget https://www.rabbitmq.com/rabbitmq-signing-key-public.asc```
* ```sudo apt-key add rabbitmq-signing-key-public.asc```

Run:

*apt-get update.
Install packages as usual; for instance run the next command ,

*sudo apt-get install rabbitmq-server

###Third step is to add the LogPub into ODL code

Delete previous classes from Karaf
One of the most important thing you have to do any time you recompile old-shim in order to use RabbitMQ is to delete everything in this folder: ~/NetIDE/openflowplugin/distribution/karaf/target/assembly/data$

*cd ~/NetIDE/openflowplugin/distribution/karaf/target/assembly/data
*rm -r -f *

If you don’t do it, odl_shim will not be able to use rabbit once you recompile using
maven.

Adding the class "RabbitLogic.java" into /.../odl-shim/src/main/java/com/telefonica/pyretic/backendchannel

Replacing the current file "Asynchat.java" which is into /.../odl-shim/src/main/java/com/telefonica/pyretic/backendchannel for the file "Asynchat.java" that you can find on GitHub.

Adding RabbitMQ dependencies in pom.xml
Before using maven to compile odl_shim, you have to include some dependencies for using RabbitMQ in the pom.xml file in odl_shim folder.

These are the dependencies lines you have to add in the <dependencies></dependencies> tag:

	<dependency>
		<groupId>com.rabbitmq</groupId>
		<artifactId>amqp-client</artifactId>
		<version>3.5.0</version>
	</dependency>
	
At this point we can compile the odl_shim project using maven as usual:

*cd ~/NetIDE/Engine-development/odl-shim
*mvn clean install


## Running

### Running the RabbitMQ Server
There are two scripts to launch both of rabbitmq-server and a basic listener in
/listenerSimple:
*launchRabbitServer.sh
*launchRecvRabbit.sh 

### Running the ODL Shim with the LogPub

Run karaf:

*cd ~/NetIDE/openflowplugin/distribution/karaf/target/assembly/bin
*./karaf

And install the below bundles:

*bundle:install -s mvn:com.googlecode.json-simple/json-simple/1.1.1
*bundle:install -s mvn:org.apache.commons/commons-lang3/3.3.2
*bundle:install -s mvn:com.rabbitmq/amqp-client/3.5.0
*bundle:install -s mvn:org.opendaylight.openflowplugin/pyretic-odl/0.0.4-Helium-SR1.1

## TODO

* This is a first release of the logger and others features will be developed.

## ChangeLog

Logger: 2015-04-14 Andres Beato Ollero <andres.beato@telcaria.com>

   * Logger for ODL shim layer (First Release)
