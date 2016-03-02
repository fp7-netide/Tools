#Profiler

In software engineering the term profiling is known as _the performance analysis of a program_. Its objective is to study the behavior of a software in order to detect failure points or areas where a performance optimization is needed. Commonly, profiling a program refers to gather the execution time or the memory consumption of such program. Therefore, a profiler is a tool that allows to collect relevant data of a software in order to analyze it and extract a conclusion. The collection of data, as opposed to static code analysis, is carried out while the program is being executed. A profiler can provide different outcomes such as: a execution trace or a statistical summary of the collected data. The presentation format depends on the purpose, could be a graphic or a text file. 

In this project, we apply the concept of profiling (inherited from software engineering) to SDN networks. We have two different approaches:
* **Application Profiler** (focused on the control plane) 
* **Network Profiler** (focused on the data plane).
In the following sections, we briefly present both and explain how to use them.

#Application Profiler

The Application Profiler, is based on the profiling concept of software engineering. The goal of this approach is to obtain the execution time of the methods executed in a controller and in their applications. There is an option which offers a major granularity and allows to extract the execution time of each function of a specific application. Furthermore, this tool provides the memory consumption of the controller and the applications running on top of it.
The current implementation of this Profiler is for Ryu Client Controller.

## Installation of the Application Profiler

1) Install the R software. For this visit the [R-project](https://www.r-project.org/) web for more information.
For example, in the case of Ubuntu 14.04 LTS, we first added the repo ```deb http://ftp.cixug.es/CRAN/bin/linux/ubuntu trusty/``` in the /etc/apt/sources.list file.
And then we performed:
```
sudo apt-get update
sudo apt-get install r-base
```
This will install the latest version of R (otherwise, some packages might be missing and the visualization of data of the profiler might not work correctly)
Note: If you obtain a GPG when trying to update apt-get, you might want to check this link: http://askubuntu.com/questions/13065/how-do-i-fix-the-gpg-error-no-pubkey which basically solves it by installing y-ppa-manager and then executing Advanced->Try to import all missing GPG keys

2) Additionally, users who need to compile R packages from source [e.g. package maintainers, or anyone installing packages with install.packages()] should also install the r-base-dev package: ```sudo apt-get install r-base-dev``` (not needed for the profiler)

Installation and compilation of R or some of its packages may require Ubuntu packages from the “backports” repositories. Therefore, it is suggested to activate the backports repositories with an entry like
* ```deb https://<my.favorite.ubuntu.mirror>/ trusty-backports main restricted universe```

3) Finally, install [RStudio](https://www.rstudio.com/). This is optionally, but helps for the visualization of the results.

## Running the Application Profiler

### General profiling (for Ryu and Python-based SDN controllers)
If you want to extract the execution time of the Ryu Client Controller + the applications executed on top of it, you have to follow the common steps for deploying an SDN network by means of the NetIDE Engine, however to execute the Ryu Client Controller (Ryu backend) you have to go to ryu/bin (or ryu/ryu/cmd) where the ryu-manager command is located and introduce the following command in order to run the Ryu backend while the data for the profiling is collected:
```
python -m cProfile -o statistics ryu/bin/ryu-manager --ofp-tcp-listen-port 7733 $NETIDE/Engine/ryu-backend/ryu-backend.py $NETIDE/Engine/ryu-backend/tests/simple_switch.py
```

A binary file called ```statistics``` will be created, which must be placed in the ```Application_Profiler``` folder. The next step is to execute the clean.sh script which is in the same folder:
* ```./clean.sh```

After this step, a 'statistic.txt' file is generated, which contains the execution time of all the functions. So, to visualize the information in graphic format is necessary to run the ```profiler_graphics.R``` with the R program previously installed or directly double clicking to open it with RStudio. This R scripts produces different graphs (explained in the script itself).

Example execution of ```profiler_graphics.R```:
![alt text][profiler]
[profiler]: https://github.com/fp7-netide/Tools/blob/master/profiler/Application_Profiler_profiler_graphics.R_example.png "Example execution of profiler_graphics.R"

### Refined profiling (for Ryu and Python-based SDN controllers)
If the user chooses the option with a major granularity which extracts the execution time of one function of a specific application. Then, it is necessary to import the 'profiler.py' into the application to be profiled and add a 'decorator' on top of the function to be profiled, the decorator is: @do_cprofile. 
For example, suppose you want to obtain the execution time of the funcion "hello_world()" that is in the "simpleswitch.py" application. Therefore within the code of the simpleswitch.py is added:
```from profiler import *``` 

and on top of the function hello_world the following decorator:
```
@do_cprofile
def hello_world():
``` 

As in the first case, a binary file is originated ('statistics'). To visualize the respective graphics you must follow the steps of the first case (beginning from the second step).


#Network Profiler

The Network Profiler focuses on collecting relevant information of the data plane. The set of statistics extracted from the switches are the followings:
• Statistics per table: active entries, packet lookups and packet matches.
• Statistics per flow: received packets, received bytes, duration (in seconds) and duration (in nanoseconds).
• Statistics per queue: received packets, transmitted packets, received bytes, transmitted bytes, dropped packets, received errors, transmitted errors, number of frame alignment errors, number of CRC errors and collisions.
• Statistics per port: transmitted packets, transmitted bytes and transmitted errors. Furthermore, with the above statistics we can elaborate more parameters: the throughput of the different switch ports, the bandwidth of the links or the possible bottlenecks. The Network Profiler sends request statistics messages to the switches, and after receiving the reply, the tool processes the information and computes the aforementioned parameters. For instance, to compute the port throughput, the Network Profiler sends periodically requests asking for the transmitted bytes of a port. Then with the replies, accumulates the bytes received and divides them by the lapsed time, obtaining the bytes per second or throughput.
The first protoype of this tool is composed by two applications running on top of Ryu which extracts port statistics of a switch. This first proof-of-concept has not been integrated into the NetIDE Engine due to the current status of the Core and its current limitations.


## Running the Network Profiler

To run this first prototype of the Network Profiler you must run the following command:
* ```ryu-manager np.py np2.py ../../Engine/ryu-backend/tests/simple_switch.py``` 
This command is for executing the Ryu Controller plus a learning switch application. Moreover, the two applications for collecting information of the data plane are also run. The statistics will be displayed in the same terminal where has been introduced the above command.




## TODO

* This is a first relase of the Profiler Tool designed for Ryu Client Controller. In future releases the Application Profiler will be developed for all Client controllers and the Network Profiler will be prefectly integrated into the NetIDE Engine.


## ChangeLog

Profiler: 2016-03-02 Wed Elisa Rojas <elisa.rojas@telcaria.com>

   * Refinement of documentation and Application_Profiler scripts

Profiler: 2015-12-23 Fri Sergio Tamurejo Moreno <sergio.tamurejo@imdea.org>

   * Profiler for Ryu Controller (First prototype)

