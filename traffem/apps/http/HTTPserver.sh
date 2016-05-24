#!/bin/bash
if [ -z `which python` ]
then
        err "python does not appear to be installed on this system."
        err "Please install it with: sudo apt-get install python"
        exit 2
fi
if [ `id -u` -ne 0 ]
then
        echo "$0 needs to be executed as root"
        exit 1
fi
function cleanup(){
	cd ..
	rm -rf HTTP_files
	servers=`ps aux | grep "python ../server" | grep -v grep| awk '{print $2}'`
    for pid in $servers
    do
        kill -9 $pid
    done
	exit 0
}
trap cleanup SIGHUP SIGINT SIGTERM
DIR=`dirname $0`
mkdir -p $DIR/HTTP_files
cd $DIR/HTTP_files
for i in 3 4 5
do
	fallocate -l "$i"M stream$i.html
done
fallocate -l 2M internet.html
fallocate -l 75K email.html
fallocate -l 100M radio.html
fallocate -l 300K video_buf.html
fallocate -l 10M video1.html
fallocate -l 25M video2.html
fallocate -l 50M video3.html
fallocate -l 1M sync.html
echo "Serving HTTP on 127.0.0.1 port 80 ..."
python ../serverGET.py &
python ../serverPUT.py &
read line
cleanup
exit 0
