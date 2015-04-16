#Logger for POX shim layer 

The Logger for POX shim layer is one of the several tools which are provided in NetIDE project for diagnosing network problems. It is composed by two modules. The first one, called 'logpub' captures the messages exchanged between shim layer and backend and send them to a queue. The second one retrieves the messages and prints them into a terminal.
RabbitMQ is used in order to develop this tool.

## Installation

###First step is to install the Erlang package

RabbitMQ uses Erlang in order to manage the broker. Installing Erlang in Ubuntu from the terminal:

* ```sudo apt-get update```
* ```sudo apt-get install Erlang```

###Second step is to install the RabbitMQ Server

* Installing on Debian / Ubuntu 

http://www.rabbitmq.com/releases/rabbitmq-server/v3.4.4/rabbitmq-server_3.4.4-1_all.deb

You can either download it with the link above and install with dpkg (recommended), or use the APT repository (see below).

Add the following line to your /etc/apt/sources.list:
deb http://www.rabbitmq.com/debian/ testing main

(optional) To avoid warnings about unsigned packages, add our public key to your trusted key list using apt-key(8), Run the following two commands:
* ```wget https://www.rabbitmq.com/rabbitmq-signing-key-public.asc```
* ```sudo apt-key add rabbitmq-signing-key-public.asc```

Run:
* ```apt-get update.```

Install packages as usual; for instance run the next command ,
* ```sudo apt-get install rabbitmq-server```

###Third step is to add the LogPub into POX code

Replace the 'pox_client.py' which is in the $HOME/NetIDE/pox/ext folder by the new file 'pox_client.py' which is in GitHub.

## Running

Before executing the logger we must install 'pika' (The python library that implements AMQP 0-9-1 protocol). RabbitMQ server used AMQP protocol. Hence, in order to communicate the logger with RabbitMQ server it is necessary to use this library.

* ```sudo pip install pika==0.9.8```

The installation depends on pip and git-core packages, you may need to install them first.

On Ubuntu:
* ```sudo apt-get install python-pip git-core```

On Debian:
* ```sudo apt-get install python-setuptools git-core```
* ```sudo easy_install pip```

Now, you must copy the folder "/listenerSimple" (included its content) in your computer. Now you have two scripts to launch both of rabbitmq-server and a basic listener in /listenerSimple:

* ```launchRabbitServer.sh``` (This starts the RabbitMQ server)
* ```launchRecvRabbit.sh``` (This start the listener, "logger")


## TODO

* This is a first release of the logger and others features will be developed.

## ChangeLog

Logger: 2015-04-14 Andres Beato <andres.beato@telcaria.com>

   * Logger for POX shim layer (First Release)