# SDN Memory Management System for ONOS
The Memory Management System is a Network Application (NetApp) that works on top of ONOS. It overcomes two important issues that may arise in SDN networks: the limited memory of network devices and the unwanted rules left by other NetApps inside the network.

The functionalities that the MMS proposes to overcome these limitations are: 

* Memory Deallocation
* Memory Swapping

## Memory Deallocation
This functionality removes the flow rules erroneously left by deactivated/uninstalled NetApps for Network Operating Systems. The Memory Deallocation can be activated only for specific NetApp that the network administrator wants to control.

## Memory Swapping
The Memory Swapping is an algorithm that overcomes the limited memory size of SDN network devices by creating a virtual flow rule memory between the Network Operating System and the device flow table. The algorithm is automatically activated when an OpenFlow TABLE_FULL is intercepted by the MMS. By default it swaps out the 20% of the less used rules inside the TCAM.

## Installation
The MMS is implemented as a NetApp for the [ONOS controller](http://onosproject.org/) by using the libraries included in its [source code](https://wiki.onosproject.org/display/ONOS/Downloads). The MMS NetApp is tested for ONOS 1.5.1.

### STEP 1: Download ONOS
```
$ git clone https://gerrit.onosproject.org/onos
$ git checkout v1.5.1
```
### STEP 2: ONOS Prerequisites

Follow the ONOS WiKi (https://wiki.onosproject.org/display/ONOS/Installing+and+Running+ONOS) in order to setup your environment to compile and run ONOS

### STEP 3: Create a custom ONOS cell

Please refer to the official guide [click here](https://wiki.onosproject.org/display/ONOS/ONOS+from+Scratch#ONOSfromScratch-4.Createacustomcelldefinition) to create a custom ONOS cell.

In this way you can easily deploy ONOS on a VM or a remote machine. Rember to issue the command ```cell *your cell name*``` after you create the cell. Then continue with this guide.

### STEP 4: Download and Install MMS app

The MMS is a standalone NetApp. It can be easily compiled with Maven and installed inside ONOS.

Download the code:
```
$ git clone https://github.com/fp7-netide/Tools.git
```

Enter the project directory and compile the code:
```
$ cd mms && mvn clean install
```

### STEP 5: Install the network application inside ONOS

```
$ onos-app $OC1 install target/onos-app-mms-1.0.0-SNAPSHOT.oar
```

Now the MMS is installed. To activate it, please use the following command:

```
onos> app activate eu.netide.mms
```

## Memory Deallocation Test

STEP 1: Prerequisites

Mininet VM is required. Please refer to this [guide](http://mininet.org/download/#option-1-mininet-vm-installation-easy-recommended). 

STEP 2: Set up your test environment

Enter the *test* folder:

```
$ cd test
```

Copy the file GCTestTopology.py to your Mininet VM and execute it:

```
$ ./GCTestTopology.py *ONOS IP*
```

STEP 3: Install the simple stateless firewall application on ONOS

Go back to the build machine and issue this command:

```
$ ./demo_gc_startup.sh
```

It compiles the application and installs it on ONOS.

STEP 4: Open ONOS GUI

Open a browser and go to the ONOS GUI at the address http://*onos ip*:8181/ui/index.html. Open the device flow table. You should see several flows installed by the stateless firewall without timeouts. Leave it open in order to see what happens during the next steps.

STEP 5: Update the stateless firewall to a dynamic statefull one.

```
$ ./demo_gc_netide.sh
```

As you can see on the GUI, the old flows remains installed on the switch. This may create traffic and security issues for the traffic inside the network.

STEP 6: Show the MMS functionalities

Install the application inside ONOS:
```
$ ./install_gc_onos.sh
```

Now the MMS is running inside ONOS. In order to check the installation correctness, please use this command inside the ONOS shell:
```
onos> apps -s
```

You should see a star near *eu.netide.mms*

Add the stateless firewall application to the MMS deallocation function:
```
onos> addtomms eu.netide.statelessfirewall
```

Now repeat the test from STEP 3 to 5 with the MMS active. When you deactivate the stateless firewall, the Flow Table is clean up from the old flow rules.

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
$ cd test
```

Copy the file GCTestTopology.py to your Mininet VM and execute it:

```
$ ./GCTestTopology.py *ONOS IP*
```

STEP 3: Configure the forwarding NetApp for ONOS

```
onos> cfg set org.onosproject.fwd.ReactiveForwarding matchIpv4Address true

onos> cfg set org.onosproject.fwd.ReactiveForwarding matchTcpUdpPorts true

onos> cfg set org.onosproject.fwd.ReactiveForwarding flowTimeout 180
```

STEP 4: Go to the Mininet shell and do a pingall and open a Xterm shell on admin
```
mininet> pingall
mininet> xterm host1
```

STEP 5: In order to perform a test, copy the hping_test.sh file in your Mininet VM and open the xterm shell that you created before and issue this command:
```
# sh hping_test.sh
```

Now the pings start flowing and the forwarding NetApp installs the associated rules in the virtual switch. When the number of rules reaches 1500 in the flow table, the MMS swapping cleans up all the less used rules.

In order to see how the MMS impacts on the number of flow rules installed, the ONOS GUI offers a representation of this data.

`http://ONOS_IP:8181/onos/ui/index.html`