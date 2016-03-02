# NetIDE Wireshark Dissector

The Wireshark Netide disssector only works in Wireshark version 1.12.x, due to it needs the last version of the Openflow dissector.

## Installing wireshark from source code

### Install necessary dependences

* ```sudo apt-get install <tool>```
	* git
	* autoconf
	* automake
	* libtool
	* bison
	* flex
	* qt-sdk
	* libgtk-3-dev
	* libpcap-dev

### Building Wireshark from source under UNIX

Download Wireshark source code from https://www.wireshark.org/#download. Remember to download Wireshark version 1.12.x

Use the following general steps to build Wireshark from source under UNIX or Linux:
Unpack the source from its compressed tar file. If you are using Linux or your version of UNIX uses GNU tar you can use the following command:
* ```tar xaf wireshark-1.12.x.tar.bz2```

In other cases you will have to use the following commands:
* ```bzip2 -d wireshark-1.12.x.tar.bz2```
* ```tar xf wireshark-1.12.x.tar```

Change directory to the Wireshark source directory.
* ```cd wireshark-1.12.x```

Configure your source so it will build correctly for your version of UNIX. You can do this with the following commands:
* ```./autogen```
* ```./configure```

Build the sources.
* ```make```

Install the software in its final destination.
* ```make install```

Once you have installed Wireshark with make install above, you should be able to run it by entering wireshark.

IF YOU HAVE THE FOLLOWING ERROR:
* ```sudo wireshark```

* wireshark: error while loading shared libraries: libwiretap.so.4: cannot open shared object file: No such file or directory

RUN THE FOLLOWING TO SOLVE IT:
* ```sudo ldconfig```

## Install NetIDE Wireshark Dissector
We are going to install the NetIDE dissector as a plugin. To install the NetIDE dissector, you just need to copy the files ```netide.so``` and ```netide.la``` which you can find in this repository, in the lib folder of your wireshark installation. Normally the path of the plugin library for most wireshark installation from source code is /usr/local/lib/wireshark/plugins/1.12.x


## TODO

* This is a second relase of the NetIDE Wireshark Dissector.


## ChangeLog

NetIDE Wireshark Dissector: 2015-11-04 Wed Andrés Beato Ollero <andres.beato@telcaria.com>

   * New version of NetIDE dissector to support the NetIDE protocol version 1.1

NetIDE Wireshark Dissector: 2015-08-24 Mon Andrés Beato Ollero <andres.beato@telcaria.com>

   * NetIDE Wireshark Dissector (First Release)
