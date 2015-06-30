#!/usr/bin/env python
import pika
import sys
import time

connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='logs',type='direct')

result = channel.queue_declare(exclusive=True)
queue_name = result.method.queue

severities = sys.argv[1:]
if not severities:
	severities = "0","1"


for severity in severities:
    channel.queue_bind(exchange='logs',queue=queue_name,routing_key=severity)

print ' [*] Waiting for logs. To exit press CTRL+C'

def callback(ch, method, properties, body):
	t=time.strftime("%H:%M:%S")
	if method.routing_key == "1":
		print '\033[1;32m[%r] [%r] %r\033[1;m'% (t, method.routing_key, body)

	if method.routing_key == "0":
		print '\033[1;33m[%r] [%r] %r\033[1;m'% (t, method.routing_key, body)

	
channel.basic_consume(callback,queue=queue_name,no_ack=True)

channel.start_consuming()
