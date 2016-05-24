#!/bin/bash

function help(){
cat << EOFhelp
Usage $0 serverIP [option]

Options:
	-h Print help
	-i Simulate internet browsing
	-d Simulate music streaming
	-s Simulate file synchronization
	-r Simulate web radio streaming
	-v Simulate video browsing and streaming
	-e Simulate email traffic

EOFhelp
}


function control_c() {
    clients=`ps aux | grep "/bin/bash ./client.sh" | grep -v grep| awk '{print $2}'`
	for pid in $clients
	do
    	kill -9 $pid
	done
	killall wget
	rm -rf *.html*
	exit 0
}

trap control_c SIGINT

if [ -z "$1" ]
then
	help
	exit 0
fi

if [ "$1" == "-h" ]
then
	help
	exit 0
fi

case $2 in
	-h)
		help
		exit 0
	;;
	-i)
		while true
		do
			wget -nv $1/internet.html
			s=`shuf -i5-60 -n1`
			#echo "sleeping $s second(s)"
			sleep $s
			rm -rf internet.html*
		done
	;;
	-d)
		while true
		do
			song=`shuf -i3-5 -n1`
			wget -nv $1/stream$song.html
			duration=$(($song*60))
			sleep $duration
			rm -rf stream$song.html*
		done
	;;
	-s)
		while true
		do
			wget -nv $1/sync.html
			sleep 10
			curl http://$1:81/sync.html --upload-file sync.html
			sleep 30
		done
	;;
	-r)
		while true
		do
			wget -nv $1/radio.html --limit-rate=128K
			rm -rf radio.html*
		done
	;;
	-v)
		while true
		do
			s=`shuf -i2-5 -n1`
			#echo "Browsing $s videos"
			for i in `seq 2 $p`
			do
				wget -nv $1/video_buf.html --limit-rate=100K
				sleep 3
			done
			#echo "Buffering video"
			wget -nv $1/video_buf.html
			v=`shuf -i1-3 -n1`
			#echo "Watching video #$v"
			wget -nv $1/video$v.html --limit-rate=300K
			rm -rf video*.html*
		done
	;;
	-e)
		while true
		do
			wget -nv $1/email.html
			s=`shuf -i5-60 -n1`
			sleep $s
			curl http://$1:81/email.html --upload-file email.html
			echo "PUT URL:http://$1/email.html"
			s=`shuf -i5-60 -n1`
			sleep $s
		done
	;;
esac
