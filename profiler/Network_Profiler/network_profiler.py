from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_parser
from ryu.ofproto import ofproto_v1_0
from ryu.ofproto import ofproto_v1_0_parser
from ryu.ofproto import ofproto_common
#from ryu.controller.controller import *
from ryu.controller import *
from ryu import utils
from ryu.lib import hub
from ryu.app.ofctl import api
import threading
import zmq
import time
from ryu.app.ofctl import event
import struct
import sys
import logging
import sys
import thread
import json

sys.path.insert(0,'../../../Engine/libraries/netip/python/')

from netip import *


n = -1

def start():
   context = zmq.Context()

#Introduce this in functions
#publisher = context.socket (zmq.PUB)
#publisher.bind("tcp://localhost:5558")
#publisher.send (message)

def send_msg(message):
   context = zmq.Context()
   publisher = context.socket(zmq.PUSH)
   publisher.bind("tcp://127.0.0.1:5558")
   publisher.send(message)

def port_stats_reply_handler(msg, netide_datapath):
   #ofp = msg.datapath.ofproto
   body = msg.body
   #print(body[0])
   #self.counter = self.counter + 10
   #throughput = body.stat[4]*8/self.counter
   #print body
   #print(msg)
   #print(type(msg))

   
   ports = []
   stat_message = []
   #For each port
   for stat in body:
      ports.append('dpid=%d port_no=%d '
	               'rx_packets=%d tx_packets=%d '
	               'rx_bytes=%d tx_bytes=%d '
	               'rx_dropped=%d tx_dropped=%d '
	               'rx_errors=%d tx_errors=%d '
	               'rx_frame_err=%d rx_over_err=%d rx_crc_err=%d '
	               'collisions=%d ' %
	               (netide_datapath, stat.port_no,
	                stat.rx_packets, stat.tx_packets,
	                stat.rx_bytes, stat.tx_bytes,
	                stat.rx_dropped, stat.tx_dropped,
	                stat.rx_errors, stat.tx_errors,
	                stat.rx_frame_err, stat.rx_over_err,
	                stat.rx_crc_err, stat.collisions))
      json_stats = {"Type":"Port Stats"}, {"dpid": netide_datapath}, {"port":stat.port_no}, {"rx_packets": stat.rx_packets}, {"tx_packets": stat.tx_packets}, {"rx_bytes": stat.rx_bytes}, {"tx_bytes": stat.tx_bytes}, {"rx_dropped": stat.rx_dropped}, {"tx_dropped": stat.tx_dropped}, {"rx_errors": stat.rx_errors}, {"tx_errors": stat.tx_errors}, {"rx_frame_err": stat.rx_frame_err}, {"rx_over_err": stat.rx_over_err}, {"rx_crc_err": stat.rx_crc_err}, {"collisions": stat.collisions}
      stats = json.dumps(json_stats)
      stat_message.append(stats)
      #IDE_connection(stats)
   #print (ports[0].tx_bytes)

   print'\033[1;32m PortStats: %s \033[1;m'%(ports)
   print('\n')
   IDE_connection(json.dumps(stat_message))
   
   



def flow_stats_reply_handler(msg, netide_datapath):
    body = msg.body
    flows = []
    stat_message = []

    for stat in body:
        flows.append('table_id=%s match=%s '
                     'duration_sec=%d duration_nsec=%d '
                     'priority=%d '
                     'idle_timeout=%d hard_timeout=%d '
                     'cookie=%d packet_count=%d byte_count=%d '
                     'actions=%s' %
                     (stat.table_id, stat.match, stat.duration_sec, stat.duration_nsec, stat.priority, stat.idle_timeout, stat.hard_timeout,
                     stat.cookie, stat.packet_count, stat.byte_count, stat.actions))
        json_stats = {"Type":"Flow Stats"}, {"dpid": netide_datapath}, {"table_id": stat.table_id}, {"duration_sec": stat.duration_sec}, {"duration_nsec": stat.duration_nsec}, {"priority": stat.priority}, {"idle_timeout": stat.idle_timeout}, {"hard_timeout": stat.hard_timeout}, {"cookie": stat.cookie}, {"packet_count": stat.packet_count}, {"byte_count": stat.byte_count}
        stats = json.dumps(json_stats)
        stat_message.append(stats)

    print'\033[1;36m FlowStats: %s \033[1;m'%(flows)
    print('\n')
    IDE_connection(json.dumps(stat_message))



def aggregate_stats_reply_handler(msg, netide_datapath):
    body = msg.body
    json_stats = {"Type":"Aggregate Stats"}, {"dpid": netide_datapath}, {"packet_count": body[0].packet_count}, {"byte_count": body[0].byte_count}, {"flow_count": body[0].flow_count}
    stats = json.dumps(json_stats)

    print'\033[1;34m AggregateStats: packet_count=%d, byte_count=%d, flow_count=%d\033[1;m'%(body[0].packet_count, body[0].byte_count, body[0].flow_count)
    print('\n')
    IDE_connection(json.dumps(stats))



def table_stats_reply_handler(msg):
    body = msg.body
    tables = []
    for stat in body:
        tables.append('table_id=%d name=%s wildcards=0x%02x '
                      'max_entries=%d active_count=%d '
                      'lookup_count=%d matched_count=%d' %
                      (stat.table_id, stat.name, stat.wildcards,
                       stat.max_entries, stat.active_count,
                       stat.lookup_count, stat.matched_count))
    print'\033[1;37m TableStats: %s \033[1;m'%(tables)
    print('\n')



def queue_stats_reply_handler(msg):
    body = msg.body
    queues = []
    for stat in body:
        queues.append('port_no=%d queue_id=%d '
                      'tx_bytes=%d tx_packets=%d tx_errors=%d ' %
                     (stat.port_no, stat.queue_id,
                      stat.tx_bytes, stat.tx_packets, stat.tx_errors))
    print'\033[1;35m QueueStats: %s \033[1;m'%(queues)
    print('\n')
    #print(len(body))




def OpenFlow_header():
	format = '!BBHI'
	header = struct.pack(format, 1, 16, 20, 1)
	header_msg = NetIDEOps.netIDE_encode('NETIDE_MGMT', 0, 0, 0, header)
	return(header_msg)

#The controller uses this message to query information about ports statistics. 160 bits or 20 bytes. Send a message for each port??? It seems yes.
#The ! is for the order
def port_stats():
	stats_messages = []
	format = '!BBHI3H6x'
	#message = Header: struct.pack(format, version, type, length, xid) Payload:(type, flags (None), port_num, padx6)
	message = struct.pack(format, 1, 16, 20, 76, 4, 0, 65535)
	#netIDE_encode(type, xid, module_id, datapath_id, msg)
	for dpid in datapaths:
		stats_messages.append(NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', 76, 0, dpid, message))
	
	return(stats_messages)

#The controller uses this message to query individual flow statistics.
def flow_stats():
	stats_messages = []
	format = '!BBHI2HIH12BHBxHBB2xIIHHBxH'
	#message = Header: struct.pack(format, version, type, length, xid) Payload:(type, flags (None), match (opcional), tableid, padx1, out_port)
	message = struct.pack(format, 1, 16, 56, 77, 1, 0, 4194303, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 65535)
	#netIDE_encode(type, xid, module_id, datapath_id, msg)
	for dpid in datapaths:
		stats_messages.append(NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', 77, 0, dpid, message))
	
	return(stats_messages)

#The controller uses this message to query aggregate flow statistics.
def aggregate_stats():
	stats_messages = []
	format = '!BBHI2HIH12BHBxHBB2xIIHHBxH'
	#message = Header: struct.pack(format, version, type, length, xid) Payload:(type, flags (None), match (opcional), tableid, padx1, out_port)
	message = struct.pack(format, 1, 16, 56, 78, 2, 0, 4194303, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 65535)
	#netIDE_encode(type, xid, module_id, datapath_id, msg)
	for dpid in datapaths:
		stats_messages.append(NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', 78, 0, dpid, message))
	
	return(stats_messages)

#The controller uses this message to query flow table statictics.
def table_stats():
	stats_messages = []
	format = '!BBHI2H'
	#message = Header: struct.pack(format, version, type, length, xid) Payload:(type, flags (None), match (opcional), tableid, padx1, out_port)
	message = struct.pack(format, 1, 16, 12, 79, 3, 0)
	#netIDE_encode(type, xid, module_id, datapath_id, msg)
	for dpid in datapaths:
		stats_messages.append(NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', 79, 0, dpid, message))
	
	return(stats_messages)

#The controller uses this message to query queue statictics.
def queue_stats():
	stats_messages = []
	format = '!BBHI3H2xI'
	#message = Header: struct.pack(format, version, type, length, xid) Payload:(type, flags (None), port_number, padx2, queue_id)
	message = struct.pack(format, 1, 16, 20, 80, 5, 0, 65535, 4294967295)
	#netIDE_encode(type, xid, module_id, datapath_id, msg)
	for dpid in datapaths:
		stats_messages.append(NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', 80, 0, dpid, message))
	
	return(stats_messages)

#The controller uses this message to query vendor switch features.
"""def vendor_stats():
	format = '!BBHI2H2I'
	#message = Header: struct.pack(format, version, type, length, xid) Payload:(type, flags (None), vendor_id, data[])
	message = struct.pack(format, 1, 16, 20, 81, 65535, 0, 5453, 0)
	#netIDE_encode(type, xid, module_id, datapath_id, msg)
	netip_message = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', 81, 0, 1, message)
	return(netip_message)"""

def IDE_connection(message):
  context = zmq.Context()
  publisher = context.socket(zmq.PUB)
  publisher.bind("tcp://127.0.0.1:5561")
  netip_message = NetIDEOps.netIDE_encode('NETIDE_MGMT', 0, 0, 0, message)
  time.sleep(0.2)
  publisher.send(netip_message);

def packet_in_msg():
	format = '!BBHIIHHBx6B'
	#message = struct.pack(format, 1, 16, 20, xid, 4, None, port_num, None, None, None, None, None, None)
	message = struct.pack(format, 1, 10, 24, 1, 1, 24, 1, 0, 0, 4, 0, 5, 0, 6)
	netip_message = NetIDEOps.netIDE_encode('NETIDE_OPENFLOW', 0, 0, 0, message)
	return(netip_message)

#No puede decodificar bien el mensaje de tipo 16 porque es un Statistic_REQUEST y el controllador no tiene que soportar este tipo de mensajes. Si creamos un statistics_REPLAY del tipo 17 no hay
#ningun problema y lo puede decodificar perfectamente, por tanto los mensajes que estamos creando SON CORRECTOS =)
def decode_sent_message(msg):
	(netide_version, netide_msg_type, netide_msg_len, netide_xid, netide_mod_id, netide_datapath) = NetIDEOps.netIDE_decode_header(msg)
	print(netide_version, netide_msg_type, netide_msg_len, netide_xid, netide_mod_id, netide_datapath)
	message_data = msg[NetIDEOps.NetIDE_Header_Size:]
	ret = bytearray(message_data)
	#print(len(message_data))
	if len(ret) >= ofproto_common.OFP_HEADER_SIZE:
		#prrr = struct.unpack('BBHI', message_data)
		#print(prrr)
		(version, msg_type, msg_len, xid) = ofproto_parser.header(ret)
		print(version, msg_type, msg_len, xid)
		msg_decoded = ofproto_parser.msg(netide_datapath, version, msg_type, msg_len, xid, ret)
		print(msg_decoded)


def msg_parser (msg):
   msg_decoded = ""
   global datapaths
   (netide_version, netide_msg_type, netide_msg_len, netide_xid, netide_mod_id, netide_datapath) = NetIDEOps.netIDE_decode_header(msg)
   message_data = msg[NetIDEOps.NetIDE_Header_Size:]
   ret = bytearray(message_data)
   if len(ret) >= ofproto_common.OFP_HEADER_SIZE:
      (version, msg_type, msg_len, xid) = ofproto_parser.header(ret)
      msg_decoded = ofproto_parser.msg(netide_datapath, version, msg_type, msg_len, xid, ret)
      #print(msg_decoded)
      if str(msg_decoded).find("OFPSwitchFeatures", 0, len(str(msg_decoded))) != -1:
      	datapath = msg_decoded.datapath
      	#print(type(datapath))
        if not datapath in datapaths:
            datapaths.append(datapath)
        
        return (0, msg_decoded, netide_datapath)
      if str(msg_decoded).find("OFPPortStatsReply", 0, len(str(msg_decoded))) != -1:
      	#print(msg_decoded)
      	#print(str(msg_decoded).find("OFPPortStatsReplay", 0, len(str(msg_decoded))))
      	return (1, msg_decoded, netide_datapath)
      if str(msg_decoded).find("OFPFlowStatsReply", 0, len(str(msg_decoded))) != -1:
      	return (2, msg_decoded, netide_datapath)
      if str(msg_decoded).find("OFPAggregateStatsReply", 0, len(str(msg_decoded))) != -1:
      	return (3, msg_decoded, netide_datapath)
      if str(msg_decoded).find("OFPQueueStatsReply", 0, len(str(msg_decoded))) != -1:
      	return (4, msg_decoded, netide_datapath)
      if str(msg_decoded).find("OFPTableStatsReply", 0, len(str(msg_decoded))) != -1:
      	return (5, msg_decoded, netide_datapath)
      else:
      	return (0, msg_decoded, netide_datapath)
   else:
   	  return (0, msg_decoded, netide_datapath)

      
def print_datapath_array():
    for dpid in datapaths:
       print(dpid)

def receive_messages():
   context = zmq.Context()
   socket = context.socket(zmq.SUB)
   socket.connect("tcp://127.0.0.1:5557")
   socket.setsockopt(zmq.SUBSCRIBE, "")

   while True:
      dst_field, src_field, msg = socket.recv_multipart()
      if src_field.startswith("1_", 0, 2) == True:
        (stats_type_code, msg_decoded, netide_datapath) = msg_parser(msg)
        #print(stats_type_code)
        #print(msg_decoded)
        if stats_type_code == 1:
        	port_stats_reply_handler(msg_decoded, netide_datapath)
        if stats_type_code == 2:
        	flow_stats_reply_handler(msg_decoded, netide_datapath)
        if stats_type_code == 3:
        	aggregate_stats_reply_handler(msg_decoded, netide_datapath)
        if stats_type_code == 4:
        	queue_stats_reply_handler(msg_decoded)
        if stats_type_code == 5:
        	table_stats_reply_handler(msg_decoded)




message_netip = []
datapaths = []
#openflow_message = ""
#log = logging.getLogger()
#log.addHandler(logging.StreamHandler(sys.stderr))

thread.start_new_thread(receive_messages, ())
while (n != 0):
    time.sleep(0.7)
    print("------ NETWORK PROFILER V.1.0 ------")
    print("1 -> Send message")
    print("2 -> Create port statistics message")
    print("3 -> Create flow statistics message")
    print("4 -> Create flow aggregate statistics message")
    print("5 -> Create table statistics message")
    print("6 -> Create queue statistics message")
    #print("7 -> create packet_in message")
    #print("8 -> Decode netide message")
    #print("9 -> Print datapaths")
    print("0 -> Exit")    
    
    n=input("Choose an option: ")


    if (n == 1):
    	#send_msg(message_netip)
    	for msgs in message_netip:
    		context = zmq.Context()
    		publisher = context.socket(zmq.PUSH)
    		publisher.connect("tcp://127.0.0.1:5558")
    		publisher.send("1_shim", len("1_shim"), zmq.SNDMORE)
    		publisher.send(msgs, len(msgs), 0)
    		#zmq_close(publisher)      
    elif (n == 2):
       message_netip = port_stats()
       #for msgs in message_netip:
       	#print (msgs)       

    elif (n == 8):
       decode_sent_message(message_netip)
       #openflow_message = decode_sent_message(message_netip)
       #print(openflow_message)

    elif (n == 7):
       message_netip = packet_in_msg()
       #print (message_netip)

    elif (n == 3):
       message_netip = flow_stats()
       #print (message_netip)

    elif (n == 4):
       message_netip = aggregate_stats()
       #print (message_netip)

    elif (n == 5):
       message_netip = table_stats()
       #print (message_netip)

    elif (n == 6):
       message_netip = queue_stats()
       #print (message_netip)

    elif (n == 9):
       print_datapath_array()
