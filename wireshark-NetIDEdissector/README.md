# NetIDE Wireshark Dissector

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

Building Wireshark requires the proper build environment including a compiler and many supporting libraries. See the Developer’s Guide at https://www.wireshark.org/docs/ for more information.

Use the following general steps to build Wireshark from source under UNIX or Linux:
Unpack the source from its compressed tar file. If you are using Linux or your version of UNIX uses GNU tar you can use the following command:
* ```tar xaf wireshark-X.tar.bz2```

In other cases you will have to use the following commands:
* ```bzip2 -d wireshark-x.tar.bz2```
* ```tar xf wireshark-X.tar```

Change directory to the Wireshark source directory.
* ```cd wireshark-X```

Configure your source so it will build correctly for your version of UNIX. You can do this with the following command:
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
We are going to install the NetIDE dissector as a plugin.
The NetIDE dissector plugin should be placed in a new plugins/netide directory which should contain at least the following files:
* ```AUTHORS```
* ```COPYING```
* ```ChangeLog```
* ```CMakeLists.txt```
* ```Makefile.am```
* ```Makefile.common```
* ```Makefile.nmake```
* ```moduleinfo.h```
* ```moduleinfo.nmake```
* ```plugin.rc.in```

And of course the source and header files for the NetIDE dissector.
Examples of these files can be found in plugins/gryphon.
You can find all files on Github, in netide folder

* AUTHORS, COPYING, and ChangeLog:
The AUTHORS, COPYING, and ChangeLog are the standard sort of GPL project files.
* CMakeLists.txt:
For our plugins/netide/CMakeLists.txt file, see the corresponding file in plugins/gryphon.  Replace all occurrences of "gryphon" in those files with "netide" and add your source files to the DISSECTOR_SRC variable.
* Makefile.am:
For our plugins/netide/Makefile.am file, see the corresponding file in plugins/gryphon.  Replace all occurrences of "gryphon" in those files with "netide".
* Makefile.common:
Our plugins/netide/Makefile.common should only list the main source file(s), which exports register_*() and handoff_*(), for your dissector in the DISSECTOR_SRC variable.  All other supporting source files should be listed in the DISSECTOR_SUPPORT_SRC variable.
The header files for our dissector, if any, must be listed in the
DISSECTOR_INCLUDES variable.  The DISSECTOR_INCLUDES variable should not include moduleinfo.h.
* Makefile.nmake:
For our plugins/netide/Makefile.nmake file, see the corresponding file in plugins/gryphon.  No modifications are needed here.
* moduleinfo.h:
Our plugins/netide/moduleinfo.h file is used to set the version information for the plugin.
* moduleinfo.nmake:
Our plugins/netide/moduleinfo.nmake is used to set the version information for building the plugin.  Its contents should match that in moduleinfo.h
* plugin.rc.in:
Our plugins/netide/plugin.rc.in is the Windows resource template file used to add the plugin specific information as resources to the DLL.
No modifications are needed here.

### Install the plugin
In order to be able to permanently add a plugin take the following steps.
You will need to change the following files:
* ```configure.ac```
* ```CMakeLists.txt```
* ```epan/Makefile.am```
* ```Makefile.am```
* ```packaging/nsis/Makefile.nmake```
* ```packaging/nsis/wireshark.nsi```
* ```plugins/Makefile.am```
* ```plugins/Makefile.nmake```

Changes to plugins/Makefile.am:
The plugins directory contains a Makefile.am.  You need to add to SUBDIRS (in alphabetical order) the name of your plugin:
* ```SUBDIRS = $(_CUSTOM_SUBDIRS_) \´´´
	* ```...```
	* ```mate \```
	* ```netide \```
	* ```opcua \```
	* ```profinet \```
	* ```stats_tree \```
	* ```…```

Changes to plugins/Makefile.nmake:
In plugins/Makefile.nmake you need to add to PLUGINS_LIST (in alphabetical order) the name of your plugin:
* ```PLUGIN_LIST = \```
	* ```...```
	* ```mate        \```
	* ```netide      \```
	* ```opcua       \```
	* ```profinet    \```
	* ```….```

Changes to the top level Makefile.am:
Add your plugin (in alphabetical order) to plugin_ldadd:
* ```if HAVE_PLUGINS```
* ```plugin_ldadd = $(_CUSTOM_plugin_ldadd_) \```
	* ```...```
	* ```-dlopen plugins/mate/mate.la \```
	* ```-dlopen plugins/netide/netide.la \```
	* ```-dlopen plugins/opcua/opcua.la \```
	* ```-dlopen plugins/profinet/profinet.la \```
	* ```...```

Changes to the top level configure.ac:
You need to add your plugins Makefile (in alphbetical order) to the AC_OUTPUT rule in the configure.ac:
* ```AC_OUTPUT( ```
  * ```...```
  * ```plugins/mate/Makefile```
  * ```plugins/netide/Makefile```
  * ```plugins/opcua/Makefile```
  * ```plugins/profinet/Makefile  ...```
  * ```,)```

Changes to epan/Makefile.am:
Add the relative path of all your plugin source files (in alphabetical order) to plugin_src:
* ```plugin_src = \```
    * ```...```
	* ```../plugins/m2m/packet-m2m.c \```
	* ```../plugins/m2m/wimax_tlv.c \```
	* ```../plugins/netide/packet-netide.c \```
	* ```../plugins/wimax/crc.c \```
	* ```../plugins/wimax/crc_data.c \```
    * ```...```

Changes to CMakeLists.txt:
Add your plugin (in alphabetical order) to the PLUGIN_SRC_DIRS:
* ```if(ENABLE_PLUGINS)```
        * ```...```
        * ```set(PLUGIN_SRC_DIRS```
        	* ```...```
			* ```plugins/mate```
    		* ```plugins/netide```
			* ```plugins/opcua```
			* ```plugins/profinet```
            * ```...```

Changes to the installers:
If you want to include your plugin in an installer you have to add lines in the NSIS installer Makefile.nmake and wireshark.nsi files.
* Changes to packaging/nsis/Makefile.nmake:
Add the relative path of your plugin DLL (in alphbetical order) to PLUGINS:
* ```PLUGINS= \```
		* ```...
		* ```../../plugins/mate/mate.dll \```
		* ```../../plugins/netide/netide.dll \```
		* ```../../plugins/opcua/opcua.dll \```
		* ```../../plugins/profinet/profinet.dll \```
		* ```….```
* Changes to packaging/nsis/wireshark.nsi:
Add the relative path of your plugin DLL (in alphbetical order) to the list of "File" statements in the "Dissector Plugins" section:
* ```File "${STAGING_DIR}\plugins\${VERSION}\m2m.dll"```
* ```File "${STAGING_DIR}\plugins\${VERSION}\netide.dll"```
* ```File "${STAGING_DIR}\plugins\${VERSION}\opcua.dll"```
* ```File "${STAGING_DIR}\plugins\${VERSION}\profinet.dll"```

## TODO

* This is a first relase of the NetIDE Wireshark Dissector.


## ChangeLog

NetIDE Wireshark Dissector: 2015-08-24 Mon Andrés Beato Ollero <andres.beato@telcaria.com>

   * NetIDE Wireshark Dissector (First Release)
