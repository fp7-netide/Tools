import sys
import zmq
import time

sys.path.insert(0,'../../../Engine/libraries/netip/python/')

from netip import *

# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
socket.connect("tcp://localhost:5560")
socket.setsockopt(zmq.SUBSCRIBE, "")
#time.sleep(3)

while True:
   #ori, dst, msg = socket.recv_multipart()
   netip_msg = socket.recv()
   #print(netip_msg)
   (netide_version, netide_msg_type, netide_msg_len, netide_xid, netide_mod_id, netide_datapath) = NetIDEOps.netIDE_decode_header(netip_msg)
   verificator_msg = netip_msg[NetIDEOps.NetIDE_Header_Size:]
   print(verificator_msg)