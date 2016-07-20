#!/usr/bin/env python
import sys
import time
import zmq
import csv
import thread

sys.path.insert(0,'../../../Engine/libraries/netip/python/')
sys.path.insert(0,'../../../ryu/ryu/')

from netip import *
from ofproto import ofproto_parser
from ofproto import ofproto_common
from ofproto import ofproto_v1_0, ofproto_v1_0_parser
from ofproto import ofproto_v1_2, ofproto_v1_2_parser
from ofproto import ofproto_v1_3, ofproto_v1_3_parser
from ofproto import ofproto_v1_4, ofproto_v1_4_parser
from ofproto import ofproto_v1_5, ofproto_v1_5_parser

from lib.mac import haddr_to_bin
from lib.packet import packet
from lib.packet import ethernet
from lib.packet import ether_types
from datetime import datetime


class Module(object):
   origin=""
   destination=""
   length=0
   max_length=0
   min_length=0
   counter=0
   def __init__(self, origin, destination, length, max_length, min_length, counter):
      self.origin = origin
      self.destination = destination
      self.length = length
      self.max_length = max_length
      self.min_length = min_length
      self.counter = counter


class Address(object):
   dst = "00:00:00:00:00:00"
   src = "00:00:00:00:00:00"
   def __init__(self, src, dst, counter, timestamp, diff):
      self.src = src
      self.dst = dst
      self.counter = counter
      self.timestamp = timestamp
      self.diff = diff


def module_identification(module_list, origin, destination, length):
   if not module_list:
      module = Module(origin, destination, length, length, length, 1)
      module_list.append(module)
   else:
      validate = False
      for modules in module_list:
         if (modules.origin == origin and destination == modules.destination):
            modules.length = modules.length + length
            if modules.max_length < length:
               modules.max_length = length
            if modules.min_length > length:
               modules.min_length = length
            modules.counter = modules.counter + 1
            validate = True
            break
            
      if validate != True:
         module = Module(origin, destination, length, length, length, 1)
         module_list.append(module)


def print_module_list (module_list):
   for i in module_list:
      print('\033[1;34m%r has sent to %r %r messages. Average length of messages %r Bytes, maximum message size %r Bytes, minimum message size %r Bytes.\033[1;m')% (i.origin.replace("'", ""), i.destination.replace("'", ""), i.counter, i.length/i.counter , i.max_length, i.min_length)


def msg_parser (msg):
   (netide_version, netide_msg_type, netide_msg_len, netide_xid, netide_mod_id, netide_datapath) = NetIDEOps.netIDE_decode_header(msg)
   message_data = msg[NetIDEOps.NetIDE_Header_Size:]
   ret = bytearray(message_data)
   if len(ret) >= ofproto_common.OFP_HEADER_SIZE:
      (version, msg_type, msg_len, xid) = ofproto_parser.header(ret)
      msg_decoded = ofproto_parser.msg(netide_datapath, version, msg_type, msg_len, xid, ret)
      return(msg_decoded, len(ret))
   else:
      return("message", len(ret))


def loop_detection (msg, timestamp, address_list):
   position = str(msg).find("OFPPacketIn", 0, len(str(msg)))
   is_in_list = False
   FMT = '%H:%M:%S'

   if position != -1:
      pkt = packet.Packet(msg.data)
      eth = pkt.get_protocol(ethernet.ethernet)
      if eth.ethertype == ether_types.ETH_TYPE_ARP:
         dst = eth.dst
         src = eth.src
         if not address_list and dst == 'ff:ff:ff:ff:ff:ff':
            address = Address(src, dst, 1, timestamp, 0)
            address_list.append(address)
                        
         else:
            for addr in address_list:
               if addr.src == src:
                  is_in_list = True
                  addr.counter = addr.counter + 1
                  delta_time = datetime.strptime(timestamp, FMT) - datetime.strptime(addr.timestamp, FMT)
                  addr.diff= delta_time.seconds
                  break
            if is_in_list == False and dst == 'ff:ff:ff:ff:ff:ff':
               address = Address(src, dst, 1, timestamp, 0)
               address_list.append(address)


def loop_detection_two(address_list):
   for addr in address_list:
      if addr.counter / addr.diff  >= 50:
         return True


def print_list_address(address_list):
   for addr in address_list:
      print('MAC origen: %r, MAC destino: %r, Counter:%d, Timestamp: %r, Diff: %d')%(addr.src, addr.dst, addr.counter, addr.timestamp, addr.diff)


def IDE_connection(message):
   context = zmq.Context()
   publisher = context.socket(zmq.PUB)
   publisher.bind("tcp://127.0.0.1:5560")
   netip_message = NetIDEOps.netIDE_encode('NETIDE_MGMT', 0, 0, 0, message)
   time.sleep(1)
   publisher.send(netip_message);

def receive_messages(module_list, address_list):
   context = zmq.Context()
   socket = context.socket(zmq.SUB)
   socket.connect("tcp://localhost:5557")
   socket.setsockopt(zmq.SUBSCRIBE, "")

   while True:
      destination, origin, msg = socket.recv_multipart()
      timestamp=time.strftime("%H:%M:%S")
      (msg_decoded, length) = msg_parser(msg)
      module_identification(module_list, origin, destination, length)
      loop_detection(msg_decoded, timestamp, address_list)
            


def menu(module_list, address_list, loop):
   n = 23
   while (n != 0):
      print("------ VERIFICATOR V.1.0 ------")
      print("1 -> Display information about NetIDE Engine.")
      print("2 -> Show if there is a loop in the topology.")
      print("0 -> Exit")
    
      n=input("Choose an option: ")

      if (n == 1):
         print_module_list (module_list)
         #module_list = []
         print('\n')
           
      elif (n == 2):
         loop = loop_detection_two(address_list)
         if loop == True:
            #address_list = []
            print('\033[1;31mYou are receiving multiple copies of the same ARP request. Maybe a loop might exist in your topology!\033[1;m')
            print('\n')
            IDE_connection('You are receiving multiple copies of the same ARP request. Maybe a loop might exist in your topology.')
            
         if loop != True:
            #print('Mensajes = %d')%(address_list[0].counter)
            #address_list = []
            print('\033[1;32mNo loops detected.\033[1;m')
            print('\n')
            IDE_connection('No loops detected.')

      elif (n == 3):
         #receive_messages(module_list, address_list)
         print_list_address(address_list)


def main():
   module_list = []
   address_list = []
   loop = False
   thread.start_new_thread(receive_messages, (module_list, address_list,))
   menu(module_list, address_list, loop)
 
            
main()