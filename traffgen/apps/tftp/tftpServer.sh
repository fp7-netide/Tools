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
rm -rf $DIR/tftp_files
exit 0
}
trap "cleanup; exit 1" SIGHUP SIGINT SIGTERM
DIR=`dirname $0`
mkdir -p $DIR/tftp_files
fallocate -l 1K $DIR/tftp_files/file1k
fallocate -l 1M $DIR/tftp_files/file1
fallocate -l 10M $DIR/tftp_files/file10
fallocate -l 100M $DIR/tftp_files/file100
python $DIR/tftp/tftpgui.py --nogui
cleanup
exit 0
