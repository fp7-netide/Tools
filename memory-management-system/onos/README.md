# SDN Memory Management System for ONOS
The Memory Management System (MMS) aims at optimizing the utilization of the Ternary Content Addressable Memory (TCAM) of the SDN-enabled switches by providing two different functionalities: (i) the *memory deallocation* and (ii) the *memory swapping*. 
The memory deallocation automatically deletes the flow entries installed in the TCAM by SDN applications that are no longer running/active. The memory swapping mitigates network performance degradations caused by the network devices operating in full memory condition, by temporarily moving the least frequently matched flow entries to a slower (but larger) memory. This SDN component is currently developed for the ONOS controller and is available for testing under the Apache 2.0 licence.

## Installation
The MMS is implemented as a bundle for the [ONOS controller](http://onosproject.org/) version 1.5.1 Falcon. The following guide has been tested on a Ubuntu 16.04 operating system, Apache Karaf 3.0.5, Mininet 2.2.1 and Oracle Java Runtime Environment build 1.8.0_121-b13.

### STEP 1: Download ONOS and configure your environment

Download and install Apache Karaf 3.0.5:

```
$ cd ~
$ wget https://archive.apache.org/dist/karaf/3.0.5/apache-karaf-3.0.5.tar.gz
$ tar xfz apache-karaf-3.0.5.tar.gz
```

```
$ cd ~
$ git clone https://gerrit.onosproject.org/onos onos-source
$ cd onos-source
$ git checkout 1.5.1
```
Open file ```~/.bashrc``` and add the following lines:

```
export JAVA_HOME=/usr/lib/jvm/java-8-oracle
export ONOS_ROOT=~/onos-source
export KARAF_ROOT=~/apache-karaf-3.0.5
export PATH=$PATH:$KARAF_ROOT/bin

. "$ONOS_ROOT/tools/dev/bash_profile"
```


### STEP 2: ONOS compilation

Compile ONOS by executing the following command from the ONOS folder:
```
cd $ONOS_ROOT
$ mvn clean install
```

### STEP 3: Create a custom ONOS cell

Please refer to the official guide [click here](https://wiki.onosproject.org/display/ONOS/ONOS+from+Scratch#ONOSfromScratch-4.Createacustomcelldefinition) to create a custom ONOS cell.

For instance, to run ONOS on the local machine, go to  ```$ONOS_ROOT/tools/test/cells``` and create a cell file with the following content:

```
export ONOS_NIC=127.0.0.*
export OC1="127.0.0.1"
export ONOS_APPS="drivers,openflow,fwd,proxyarp,mobility"
```

Remember to issue the command ```cell *your cell name*``` after you create the cell and then start ONOS with command:

```
$ onos-karaf
```

### STEP 4: Download and Install the MMS

Download the code:
```
$ cd ~
$ git clone https://github.com/fp7-netide/Tools.git
```

Enter the project directory and compile the code:
```
$ cd Tools/memory-management-system/onos/mms
$ mvn clean install
```

### STEP 5: Install the network application inside ONOS

```
$ onos-app $OC1 install target/onos-app-mms-1.0.0-SNAPSHOT.oar
```

Now the MMS is installed. To activate it, please run the following command in the ONOS CLI:

```
onos> app activate eu.netide.mms
```

## Memory Deallocation Test

STEP 1: Prerequisites

Mininet VM is required. Please refer to this [guide](http://mininet.org/download/#option-1-mininet-vm-installation-easy-recommended). 

STEP 2: Set up your test environment

Enter the *test* folder:

```
$ cd ~/Tools/memory-management-system/onos/test
```

Copy the file GCTestTopology.py to your Mininet VM and execute it:

```
$ sudo ./GCTestTopology.py ONOS_IP
```
where ```ONOS_IP=127.0.0.1``` if Mininet and ONOS are running on the same machine.

STEP 3: Install the simple stateless firewall application on ONOS

Go back to the build machine and issue this command:

```
cd ~/Tools/memory-management-system/onos/test
$ ./demo_mms_startup.sh
```

It compiles the application and installs it on ONOS.

STEP 4: Open ONOS GUI

Open a browser and go to the ONOS GUI at the address http://localhost:8181/onos/ui/login.html (where ```localhost``` must be replaced with the actual IP address of the machine where ONOS is installed and running). Open the device flow table. You should see several flows installed by the stateless firewall without timeouts. Leave it open in order to see what happens during the next steps.

STEP 5: Update the stateless firewall to a dynamic statefull one.

```
cd ~/Tools/memory-management-system/onos/test
$ ./demo_mms_netide.sh
```

As you can see on the GUI, the old flows remain installed on the switch. This may create traffic and security issues in the network.

STEP 6: Show the MMS functionalities

Install the application inside ONOS:
```
$ ./install_mms_onos.sh
```

Now the MMS is running inside ONOS. In order to check the installation correctness, please use this command inside the ONOS shell:
```
onos> apps -s
```

You should see a star near *eu.netide.mms*

Add the stateless firewall application to the MMS deallocation function:
```
onos> addapplication eu.netide.statelessfirewall
```

Now repeat the test from STEP 3 to 5 with the MMS active. When you deactivate the stateless firewall, the flow table has been clean up from the old flow rules.

## Memory Swapping Test

STEP 1: Prerequisites

1. Mininet VM. Please refer to this [guide](http://mininet.org/download/#option-1-mininet-vm-installation-easy-recommended). 

2. Hping3:

```
$ sudo apt-get install hping3
```

3. ONOS up and running with the org.onosproject.fwd NetApp active.

STEP 2: Set up your test environment

Enter the *test* folder:

```
$ $ cd ~/Tools/memory-management-system/onos/test
```

Copy the file GCTestTopology.py to your Mininet VM and execute it:

```
$ sudo ./GCTestTopology.py ONOS_IP
```
where ```ONOS_IP=127.0.0.1``` if Mininet and ONOS are running on the same machine.

STEP 3: Configure the ReactiveForwarding application in ONOS

```
onos> cfg set org.onosproject.fwd.ReactiveForwarding matchIpv4Address true

onos> cfg set org.onosproject.fwd.ReactiveForwarding matchTcpUdpPorts true

onos> cfg set org.onosproject.fwd.ReactiveForwarding flowTimeout 0
```

STEP 4: Go to the Mininet shell and do a pingall to verify that everything is working properly
```
mininet> pingall
```

STEP 5: In order to perform a test, copy the hping_test.sh file in your Mininet VM and open the xterm with command:
```
mininet> xterm host1
```
then go where you copied the script and execute:

```
# sh hping_test.sh
```

Now the pings start flowing and the ReactiveForwarding installs the many rules with infinite timeout in the virtual switch. When the number of rules reaches 1800 in the flow table, the MMS swapping moves the less used flow rules from the memory of the switch to an external database maintained by the MMS itself.

In order to see how the MMS impacts on the number of flow rules installed, the ONOS GUI offers a representation of this data.

`http://ONOS_IP:8181/onos/ui/index.html`
