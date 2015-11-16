#Debugger for Core Architecture

The Debugger for NetIDE Core is one of the several tools which are provided in NetIDE project for diagnosing network problems. It captures the messages exchanged between shim layer and backend and send them to a queue. The messages are retrieved and printed into a text file and also converted into a pcap file.


## Installation

First install ZMQ libraries:

* Installing on Debian / Ubuntu 

sudo apt-get install python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev python-zmq

## Running

To test the debugger this command is neccesary:
* ```python debugger.py```

Note: the shim layer, the backend and the core must be running properly.

## Use NetIDE wireshark dissector

To check the pcap file created by the Debugger, it necessary to use the Wireshark Netide dissector provided in this folder. Use the netide.la and the netide.so from this folder and use the readme in tools/wireshark-NetIDEdissector to install the dissector properly.

## TODO

* This is a first relase of the debugger and other funcionalities will be developed.


## ChangeLog

Debugger for Core Architecture: 2015-11-16 Mon Andrés Beato Ollero <andres.beato@telcaria.com>

   * Debugger for Core Architecture (First Release)
