#!/bin/bash

function help(){
cat << EOFhelp
Usage $0 serverIP

Simulate an client with different HTTP patterns

Mandatory pattern(s):
    Internet:   Simulate the loading of a web page (size 2MB)
                and wait for 5-60s to simulate reading
    Email:      Simulate sending and receiving an email (75KB)
                continuously

Random pattern(s):
    Radio:      Simulate a web radio. This pattern will
                download a file at a low rate (20KB/s) constantly.

    Music:      Simulate a streaming service. This patten will
                download a "song" (3-5MB file) and wait before 
                loading another "song" (3-5mn)

    Sync:       Simulate the synchronization of data

    Video:      Simulate the browsing and streaming of a video

EOFhelp
}
function cleanup() {
    clients=`ps aux | grep "/bin/bash ./client.sh" | grep -v grep| awk '{print $2}'`
    for pid in $clients
    do
        kill -9 $pid
    done
    killall wget
	rm -rf *.html*
	exit
}

trap cleanup SIGINT

if [ -z `which wget` ]
then
        echo "wget does not appear to be installed on this system."
        echo "Please install it with: sudo apt-get install wget"
	exit 1
fi
if [ -z `which curl` ]
then
        echo "curl does not appear to be installed on this system."
        echo "Please install it with: sudo apt-get install curl"
	exit 2
fi
if [ -z `which shuf` ]
then
        echo "shuf does not appear to be installed on this system."
	exit 3
fi
if [ -z "$1" ]
then
	echo "Error in arguments!"
	help
	exit 4
fi

echo "Launching Internet client"
./client.sh $1 -i &
echo "Launching Email client"
./client.sh $1 -e &

sync=`shuf -i1-3 -n1`
case $sync in
	1)
	    echo "Launching sync client"
        ./client.sh $1 -s &
	;;
esac
player=`shuf -i1-3 -n1`
case $player in
	1)
		echo "Launching web radio client"
		./client.sh $1 -r &
	;;
	2)
		echo "Launching music streaming client"
		./client.sh $1 -d &
	;;
	3)
		echo "Launching video streaming client"
		./client.sh $1 -v &
	;;
esac
echo "press [Enter] to quit"
read line
cleanup
exit 0
