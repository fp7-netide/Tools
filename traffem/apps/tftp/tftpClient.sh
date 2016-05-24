#!/bin/bash

function help(){
cat << EOFhelp
Usage $0 serverIP [test|small|medium|big]

Get a TFTP file from a server.
If no other argument is provided, a test file will
be downloaded from the server (1024B).
The other available files are:
	small:	1M
	medium:	10M
	big:	100M

EOFhelp
}
if [ -z `which tftp` ]
then
        err "tftp does not appear to be installed on this system."
        err "Please install it with: sudo apt-get install tftp"
        exit 2
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

tftp $1 <<!
get $file
!

exit 0
