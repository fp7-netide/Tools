#!/bin/bash
onos-app $OC1 uninstall eu.netide.statefullfirewall
onos-app $OC1 install! ../demo-stateless-firewall/target/netide-demo-stateless-firewall-1.0-SNAPSHOT.oar