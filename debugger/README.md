Three of the NetIDE tools are tightly related as they share part of the code: the **Logger**, the **Debugger** and the **Verificator**. 
The NetIDE LogPub is publishing content in a queue and the file ```debugger.py``` is in charge of obtaining all the distributed messages. 
Although called ```debugger.py```, it serves the three tools at the same time, let us explain how:

- It prints all the messages received on runtime and generates a plan text file for the **Logger**: ```results.txt```
- It creates a PCAP file for the **Debugger**: ```results.pcap```
- It produces a *postcard* file for the **Verificator**: ```results.card```

This folder contains two architectural approaches of these tools: 
- The initial one in which no Core component was developed and every shim (at the Server Controller) provided the messaging queue and thus the ```Ryu_shim``` folder contains the code for this approach and the Ryu shim (deprecated!).
- The current approach with the Core component. Therefore, the NetIDE Network Engine currently uses the code in the ```Core``` folder.
