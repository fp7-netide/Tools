#!/bin/bash
ERROR=-1
clear
echo "====================================================="

echo " Compiling Recv.java"
echo ""

rm -r *.class

javac -cp "./lib/rabbitmq-client.jar" Recv.java

echo " Executing Recv.java"
echo ""
echo "============================================"

java -cp "./lib/commons-io-1.2.jar:./lib/commons-cli-1.1.jar:./lib/rabbitmq-client.jar:." Recv

echo ""
echo "[X] Execution Finished"

echo "============================================"
