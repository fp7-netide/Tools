#!/bin/bash
hping3 -V -c 5000 -I eth1 -d 120 -2 -p ++0 -s 445 --fast --rand-source 192.168.0.2