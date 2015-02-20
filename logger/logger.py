#!/usr/bin/env python
import pika
import time

connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='logs',type='fanout')

result = channel.queue_declare(exclusive=True)
queue_name = result.method.queue

channel.queue_bind(exchange='logs',queue=queue_name)

print ' [*] Waiting for logs. To exit press CTRL+C'

def callback(ch, method, properties, body):
    #print " [x] %r" % (body,)
    t=time.strftime("%H:%M:%S")
    print '\033[1;32m[%r] %r\033[1;m'% (t,body,)

channel.basic_consume(callback,queue=queue_name,no_ack=True)

channel.start_consuming()
