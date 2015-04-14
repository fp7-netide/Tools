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

#! /usr/bin/env python
import pika
import time

connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='logs', type='fanout')

result = channel.queue_declare(exclusive=True)
queue_name = 'qRabbitMQ'

channel.queue_bind(exchange ='', queue=queue_name)

print  ' [*] Waiting for logs. To exit press CTRL + C'

def callback (ch, method, properties, body):
    #print "[x] %r" % (body,)
    t = time.strftime("%H:%M:%S")
    print '\033[1;32m[%r] %r\033[1;m'% (t,body,)

channel.basic_consume (callback, queue=queue_name, no_ack=True)


channel.start_consuming()
