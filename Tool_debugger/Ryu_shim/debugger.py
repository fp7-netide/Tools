#!/usr/bin/env python
import pika
import sys
import time
import binascii
from generate_pcapfile import *

from scapy.utils import wrpcap

fo = open("results.txt", "wb")
bitout = open("results.pcap", 'wb')
#msg = binascii.hexlify('hello')
#port = 9600
#message = (msg) 

connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='logs',type='direct')

result = channel.queue_declare(exclusive=True)
queue_name = result.method.queue

i = 0
port = 9600
msgs = []
severities = sys.argv[1:]
if not severities:
	severities = "0","1"


for severity in severities:
    channel.queue_bind(exchange='logs',queue=queue_name,routing_key=severity)

print ' [*] Waiting for logs. To exit press CTRL+C'

def sum_one(i):
    return i + 1

def callback(ch, method, properties, body):
	t=time.strftime("%H:%M:%S")
	if method.routing_key == "1":
                print '\033[1;32m[%r] [%r] %r\033[1;m'% (t, method.routing_key, body)
                fo.write("[%r] [%r] %r \n"% (t, method.routing_key, body));
                msg = binascii.hexlify(body)
                global i
                #print i
                bytestring = generatePCAP(port,msg,i)
                i = sum_one(i)
                #print bytestring
                bytelist = bytestring.split()
                #print bytelist  
                bytes = binascii.a2b_hex(''.join(bytelist))
                #print bytes
                #wrpcap('results2.pcap',bytes)
                bitout.write(bytes);
                """msgs = body.split(method.routing_key)
                for i in range(len(msgs)):
                    fo.write('[%r] [%r] %r'% (t, method.routing_key, msgs[i]))
                    msg = binascii.hexlify(msgs[i])
                    bytestring = generatePCAP(port,msg)
                    bytelist = bytestring.split()  
                    bytes = binascii.a2b_hex(''.join(bytelist))
                    bitout.write(bytes)"""

	if method.routing_key == "0":
		print '\033[1;33m[%r] [%r] %r\033[1;m'% (t, method.routing_key, body)
                fo.write("[%r] [%r] %r\n"% (t, method.routing_key, body));
                msg = binascii.hexlify(body)
                global i
                #print i
                bytestring = generatePCAP(port,msg,i)
                i = sum_one(i)
                #print bytestring
                bytelist = bytestring.split()
                #print bytelist  
                bytes = binascii.a2b_hex(''.join(bytelist))
                #print bytes
                #wrpcap('results2.pcap',bytes)
                bitout.write(bytes);
                """msgs = body.split(method.routing_key)
                for i in range(len(msgs)):
                    fo.write('[%r] [%r] %r'% (t, method.routing_key, msgs[i]))
                    msg = binascii.hexlify(msgs[i])
                    bytestring = generatePCAP(port,msg)
                    bytelist = bytestring.split()  
                    bytes = binascii.a2b_hex(''.join(bytelist))
                    bitout.write(bytes)"""



channel.basic_consume(callback,queue=queue_name,no_ack=True)

channel.start_consuming()

fo.close()
bitout.close()
