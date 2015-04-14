#!/usr/bin/env python
import pika
import sys

def open_connection_RabbitMQ():
    	connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    	channel = connection.channel()
	
    	channel.exchange_declare(exchange='logs',type='fanout')
	return channel

    	#message = ' '.join(sys.argv[1:]) or "info: Hello World!"


def sendtoRabbitMQ(message,channel):
	channel.basic_publish(exchange='logs',routing_key='',body=message)
	#print " [x] Sent %r" % (message,)


def close_connection_RabbitMQ():
	connection.close()

