#Debugger

The Debugger for NetIDE Core is one of the several tools which are provided in NetIDE project for diagnosing network problems. It captures the messages exchanged between shim layer and backend and send them to a queue. The messages are retrieved and printed into a text file and also converted into a pcap file.


## Installation

1) Install ZMQ libraries:

* Installing on Debian / Ubuntu 

⋅```sudo apt-get install python-pip python-dev python-repoze.lru libxml2-dev libxslt1-dev zlib1g-dev python-zmq python-scapy```
(alternatively, you can install those with ```sudo pip install```)

2) Recompile the wireshark sources to add our custom user class for the NetIDE protocol. To do so, copy ```packet-user_encap.c``` in the ```epan/dissectors/``` folder. And the recompile Wireshark with the following three commands:
```
./configure --prefix=$HOME/wireshark #to specify the location of the compiled Wireshark binary
make
make install
```

3) To check the pcap file created by the Debugger, it necessary to use the Wireshark NetIDE dissector provided in this folder. Use the ```netide.la``` and the ```netide.so``` from this folder and use the readme in [tools/wireshark-NetIDEdissector](https://github.com/fp7-netide/Tools/tree/master/wireshark-NetIDEdissector) to install the dissector properly (which basically indicates that we need to copy those two files in the ```lib/wireshark/plugins/x.y.z``` of the Wireshark binary just compiled (where x.y.z is the specific version)).

###Source code
The source code to generate the ```netide.la``` and ```netide.so``` files is located at [tools/wireshark-NetIDEdissector/netide_plugin_dissec](https://github.com/fp7-netide/Tools/tree/master/wireshark-NetIDEdissector/netide_plugin_dissec). It is only necessary to substitute the ```packet-user_encap.c``` file with the one contained in this folder [debugger/Core](https://github.com/fp7-netide/Tools/tree/master/debugger/Core) to generate the *.la and *.so files.

## Running

To test the debugger this command is neccesary:
* ```python debugger.py```

Note: the shim layer, the backend and the core must be running properly.

## Using the NetIDE wireshark dissector
Currently, the ```packet-user_encap.c``` automatically associates the DLT_USER0 type with the NetIDE protocol. If for some reason this does not work, the first time we execute Wireshark we should also follow the instructions in https://wiki.wireshark.org/HowToDissectAnything to tell Wireshark how to decode our user link-layer header type. We do this by selecting ```Edit->Preferences->Protocols>DLT_USER->Edit Encapsulations Table``` and adding an entry for NetIDE, according to: https://www.wireshark.org/docs/wsug_html_chunked/ChUserDLTsSection.html

Example execution Wireshark loading a *.pcap file obstained by the debugger:
![alt text][debugger]
[debugger]: https://github.com/fp7-netide/Tools/blob/master/debugger/Core/wireshark_example.png "Example execution of the debugger in Wireshark"

## TODO

* This is a first relase of the debugger and other funcionalities will be developed.


## ChangeLog

Debugger for Core Architecture: 2015-03-02 Wed Elisa Rojas <elisa.rojas@telcaria.com>

   * Updated README.md and support of code from now on

Debugger for Core Architecture: 2015-11-16 Mon Andrés Beato Ollero <andres.beato@telcaria.com>

   * Debugger for Core Architecture (First Release)
