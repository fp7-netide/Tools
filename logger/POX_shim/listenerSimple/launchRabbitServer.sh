#!/bin/bash
ERROR=-1
echo "====================================================="

echo " Starting rabbitMQ-server"

sudo invoke-rc.d rabbitmq-server start


echo " Enable interfaz web"

sudo rabbitmq-plugins enable rabbitmq_management
echo ""
echo "[V] rabbitMQ started"

echo "============================================"
