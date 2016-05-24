#!/bin/bash

function help(){
cat << EOFhelp
Usage $0 serverIP [test|small|medium|big]

Get a HTTP file from a server.
If no other argument is provided, a test file will
be downloaded from the server (1024B).
The other available files are:
	small:	1M
	medium:	10M
	big:	100M

EOFhelp
}
if [ -z `which wget` ]
then
        echo "wget does not appear to be installed on this system."
        echo "Please install it with: sudo apt-get install wget"
	exit 1
fi
if [ -z "$1" ]
then
	echo "Error in arguments!"
	help
	exit 1
fi
if [ -z "$2" ]
then
	file="file1k"
else
	case $2 in
	test)
		file="file1k"
		;;
	small)
		file="file1"
		;;
	medium)
		file="file10"
		;;
	big)
		file="file100"
		;;
	*)
		echo "Error in file argument!"
		help
		exit 2
		;;
	esac
fi

wget $1/$file

exit 0
