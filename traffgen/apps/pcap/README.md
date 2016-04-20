# Traffic Generator - pcap

This tool is an application which takes .pcap files like those who can be found in .pcap repositories[1][2] or using an external tool like DCT²Gen[3], and create traces playable by a tool like tcpreplay[4].

You can use the file _test.pcap_ and run _./producePCAP -t_ to produce three pcap files.

```
Usage ./producePCAP -p <.pcap> -i <source IP> -m <source MAC> -f <host(s) file>

<.pcap>: the .pcap file

<source IP>: the IP of the desired host from the .pcap

<source MAC>: the MAC of the desired host from the .pcap

<host(s) file>: a file with the output host in this format:
HOSTNAME HOST_IP HOST_MAC

Example:
    ./producePCAP -f hosts.txt -p test.pcap -i 172.16.11.12 -m f8:1e:df:e5:84:3a

With host.txt:
    H1 10.0.0.1 04:08:15:16:23:42
    H2 172.19.3.15 00:01:46:56:62:96
    H3 192.168.3.15 00:09:53:59:84:87
```

## References

[1] http://www.netresec.com/?page=PcapFiles
[2] http://traces.cs.umass.edu/index.php/Network/Network
[3] https://www-old.cs.uni-paderborn.de/en/research-group/research-group-computer-networks/people/dr-philip-wette/dct2gen.html
[4] http://tcpreplay.appneta.com/
