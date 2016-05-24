#!/bin/bash

function help(){
cat << EOFhelp
Usage $0 [--server] serverIP [--imix|--voip|--video]

Simulate internet traffic.

Options:
    --server: run in server mode
    --imix:  play the Internet Mix pattern.
            7 packets of 40B
            4 packets of 576B
            1 packet  of 1500B
    --voip:  play a VoIP pattern (G.726 audio codec).
            130B packets @ 55KB/s
    --video: play a video straming pattern (H.264 video codec).
            1200B packets @ 471KB/s

EOFhelp
}
function server(){
	echo "Starting iperf server"
	echo "Press Ctrl+C to quit..."
	iperf -u -s > /dev/null 2>&1
}
function imix(){
	echo "Sending IMIX pattern to $1"
	echo "Press Crtl+C to quit..."
	while true
	do
		iperf -u -c $1 -l 40 -n 40 > /dev/null 2>&1
		iperf -u -c $1 -l 40 -n 40 > /dev/null 2>&1
		iperf -u -c $1 -l 40 -n 40 > /dev/null 2>&1
		iperf -u -c $1 -l 40 -n 40 > /dev/null 2>&1
		iperf -u -c $1 -l 40 -n 40 > /dev/null 2>&1
		iperf -u -c $1 -l 40 -n 40 > /dev/null 2>&1
		iperf -u -c $1 -l 40 -n 40 > /dev/null 2>&1
		iperf -u -c $1 -l 576 -n 576 > /dev/null 2>&1
		iperf -u -c $1 -l 576 -n 576 > /dev/null 2>&1
		iperf -u -c $1 -l 576 -n 576 > /dev/null 2>&1
		iperf -u -c $1 -l 576 -n 576 > /dev/null 2>&1
		iperf -u -c $1 -l 1500 -n 1500 > /dev/null 2>&1
	done
}
function voip(){
	echo "Sending VoIP traffic (codec G.726) to $1"
	echo "Press Ctrl+C to quit..."
	while true
	do
		iperf -u -c $1 -b 55K -l 130 -t 120 > /dev/null 2>&1
	done
}
function video(){
	echo "Sending video traffic (codec H.264) to $1"
	echo "Press Ctrl+C to quit..."
	while true
	do
		iperf -u -c $1 -b 471K -l 1200 -t 120 > /dev/null 2>&1
	done
}
function control_c() {
	exit 0
}

trap control_c SIGINT
if [ -z `which iperf` ]
then
        echo "iperf does not appear to be installed on this system."
        echo "Please install it with: sudo apt-get install iperf"
	exit 1
fi
if [ -z "$1" ]
then
	echo "Error in arguments!"
	help
	exit 1
fi
if [ "$1" == "--server" ]
then
	server
fi
if [ -z "$2" ]
then
	echo "Empty pattern"
	help
	exit 1
else
	case $2 in
	--imix)
		imix $1
		;;
	--voip)
		voip $1
		;;
	--video)
		video $1
		;;
	*)
		echo "Error in file argument!"
		help
		exit 2
		;;
	esac
fi
