# Traffic Emulator

This tool is composed of applications which can be used to generate traffic for the Emulator.

The current applications available are

1. **http**: a client / server application which simulate HTTP traffic from a host to a server. The traffic patterns are similar to what can be found in this work[1].
2. **pcap**: an application which takes .pcap files like those who can be found in .pcap repositories[2][3] or using an external tool like DCT²Gen[4], and create traces playable by a tool like tcpreplay[5].
3. **tftp**: a client / server application which simulate TFTP traffic.
4. **iperfp**: a application which uses _iperf_  to simulate common flow patterns (VoIP, video, ...) in terms of packet size and bandwidth.

## References
[1] https://hal.archives-ouvertes.fr/hal-00685658/document

[2] http://www.netresec.com/?page=PcapFiles

[3] http://traces.cs.umass.edu/index.php/Network/Network

[4] https://www-old.cs.uni-paderborn.de/en/research-group/research-group-computer-networks/people/dr-philip-wette/dct2gen.html

[5] http://tcpreplay.appneta.com/
