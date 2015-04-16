#!/bin/bash
ERROR=-1
echo "====================================================="

echo " Trying to stop rabbitMQ-server"

sudo invoke-rc.d rabbitmq-server stop



echo ""
echo "[V] rabbitMQ stopped"

echo "============================================"
