#
# Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
# Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
# Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für 
# Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Authors:
#    	Sergio Tamurejo
#		Rafael Leon Miranda
#		Andres Beato Ollero
#

#!/usr/bin/env python
import pika
import sys

def open_connection_RabbitMQ():
    	connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    	channel = connection.channel()

    	channel.exchange_declare(exchange='logs',type='direct')
	return channel

    	#message = ' '.join(sys.argv[1:]) or "info: Hello World!"


def sendtoRabbitMQ(message,channel):
	channel.basic_publish(exchange='',routing_key='qRabbitMQ',body=message)
	print " [x] Sent %r" % (message,)


def close_connection_RabbitMQ():
	connection.close()
