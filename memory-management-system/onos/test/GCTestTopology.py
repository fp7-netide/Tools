#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.node import RemoteController
from mininet.link import TCLink

import os
import sys
import signal
import time
import json
import subprocess 
from subprocess import call
import re




class MyTopology(Topo):
    	
    def __init__(self, **opts):
        
        Topo.__init__(self, **opts)
        edgeRouter = self.addSwitch('edge', dpid = '0000000000000001' )

        #Hosts
        host1 = self.addHost('host1', ip='192.168.0.1/24')
        host2 = self.addHost('host2', ip='192.168.0.2/24')
                    

        self.addLink(host1, edgeRouter, port2=1)
        self.addLink(edgeRouter, host2, port1=2)
      

        
        
def startMininet(controller_ip):
    "Create and test a simple network"
    topo = MyTopology()
    net = Mininet(topo=topo, controller=lambda c1: RemoteController( c1, ip=controller_ip),link=TCLink)
    net.start()
    net.get("host1").setARP(ip='192.168.0.3',mac='60:57:18:B1:A4:22')
    net.get("edge").cmd("ovs-vsctl -- --id=@ft create Flow_Table flow_limit=1500 overflow_policy=refuse -- set Bridge admin flow_tables=0=@ft")
    CLI(net)
    net.stop()
    
    
 
if __name__ == '__main__':
    # Tell mininet to print useful information
    setLogLevel('info')
    startMininet(sys.argv[1])
