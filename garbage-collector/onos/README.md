# ONOS Garbage Collector

 The ONOS garbage collector is a network application that permits the intelligent cleaning of switches flow tables.
 
## Installation

STEP 1: Download ONOS
```
$ git clone https://gerrit.onosproject.org/onos
$ git checkout v1.3.0
```
STEP 2: ONOS Prerequisites

Follow the ONOS WiKi (https://wiki.onosproject.org/display/ONOS/Installing+and+Running+ONOS) in order to setup your environment to compile and run ONOS

STEP 3: Create a custom ONOS cell

Please refer to the official guide [click here](https://wiki.onosproject.org/display/ONOS/ONOS+from+Scratch#ONOSfromScratch-4.Createacustomcelldefinition) to create a custom ONOS cell.

In this way you can easily deploy ONOS on a VM remote machine. Rember to issue the command ```cell *your cell name*``` after you create the cell. Then continue with this guide.

STEP 3: Download and Install ONOS Garbage Collector app

The Garbage Collector is a standalone project. It can easily compiled through Maven and installed inside ONOS controller.

Please check that 

Download the code:
```
$ git clone https://github.com/fp7-netide/Tools.git
```

Enter the project directory and compile the code:
```
$ cd garbage-collector/onos && mvn clean install -DskipTests
```


## Run a test

STEP 1: Prerequisites

Mininet VM is required. Please refer to this [guide](http://mininet.org/download/#option-1-mininet-vm-installation-easy-recommended). 

STEP 2: Set up your test environment

Enter the *test* folder:

```
$ cd test
```

Copy the file GCTestTopology.py to your Mininet VM and execute it. Add as option your ONOS ip address.

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

STEP 6: Show the Garbage Collector functionalities

Install the application inside ONOS:
```
$ ./install_gc_onos.sh
```

Now the garbage collector is running inside ONOS. In order to check the installation correctness, please use this command inside the ONOS shell:
```
onos> apps -s
```

You should see a star near *eu.netide.gc*

Now repeat the test from STEP 3 to 5 with the Garbage Collector active.


