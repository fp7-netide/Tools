#!/bin/bash

#Uninstall firewall stateless

onos-app $OC1 uninstall eu.netide.statelessfirewall

#Install statefull

onos-app $OC1 install! ../demo-statefull-firewall/target/netide-demo-statefull-firewall-1.0-SNAPSHOT.oar