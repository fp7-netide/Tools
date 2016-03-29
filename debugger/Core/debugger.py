#!/usr/bin/env python
import zmq
import sys
import time
import binascii
import argparse
import csv
from scapy.utils import wrpcap

sys.path.insert(0,'../../../Engine/libraries/netip/python/')
sys.path.insert(0,'../../../ryu/ryu/')

from netip import *
from ofproto import ofproto_parser
from ofproto import ofproto_common
from ofproto import ofproto_protocol
#from ofproto import ofproto_utils
from ofproto import ofproto_v1_0, ofproto_v1_0_parser
from ofproto import ofproto_v1_2, ofproto_v1_2_parser
from ofproto import ofproto_v1_3, ofproto_v1_3_parser
from ofproto import ofproto_v1_4, ofproto_v1_4_parser
from ofproto import ofproto_v1_5, ofproto_v1_5_parser


###################### headers for pcap creation ####################################

#Global header for pcap 2.4
pcap_global_header =   ('D4 C3 B2 A1'   
                        '02 00'         #File format major revision (i.e. pcap <2>.4)  
                        '04 00'         #File format minor revision (i.e. pcap 2.<4>)   
                        '00 00 00 00'     
                        '00 00 00 00'     
                        'FF FF 00 00'     
                        '93 00 00 00') #user_protocol selected, without Ip and tcp headers

#pcap packet header that must preface every packet
pcap_packet_header =   ('AA 77 9F 47'     
                        '90 A2 04 00'     
                        'XX XX XX XX'   #Frame Size (little endian) 
                        'YY YY YY YY')  #Frame Size (little endian)

#netide packet header that must preface every packet
netide_header =   ('01'                 #netide protocol version 1.1
                   '11'                 #openflow type
                   'XX XX'              #Frame Size (little endian) 
                   '01 00 00 00'        #xid 
                   '00 00 00 00 00 00 00 06') #datapath_id   

######################################################################################

###################### PCAP generation ########################################
def getByteLength(str1):
    return len(''.join(str1.split())) / 2
#    return len(str1)

def generatePCAP(message,i): 

    msg_len = getByteLength(message)
#    netide = netide_header.replace('XX XX',"%04x"%msg_len)
#    net_len = getByteLength(netide_header)
#    pcap_len = net_len + msg_len
    hex_str = "%08x"%msg_len
    reverse_hex_str = hex_str[6:] + hex_str[4:6] + hex_str[2:4] + hex_str[:2]
    pcaph = pcap_packet_header.replace('XX XX XX XX',reverse_hex_str)
    pcaph = pcaph.replace('YY YY YY YY',reverse_hex_str)

    if (i==0):
#        bytestring = pcap_global_header + pcaph + eth_header + ip + tcp + message
#        bytestring = pcap_global_header + pcaph + netide + message
        bytestring = pcap_global_header + pcaph + message
    else:
#        bytestring = pcaph + eth_header + ip + tcp + message
#        bytestring = pcaph + netide + message
        bytestring = pcaph + message
    return bytestring
#    writeByteStringToFile(bytestring, pcapfile)

#Splits the string into a list of tokens every n characters
def splitN(str1,n):
    return [str1[start:start+n] for start in range(0, len(str1), n)]

def sum_one(i):
    return i + 1

##############################################################################

parser = argparse.ArgumentParser(description='Launch the NetIDE debugger')
parser.add_argument('-o', help='Output Folder', default=".")

args = parser.parse_args()


fo = open(args.o+"/results.txt", "wb")
bitout = open(args.o+"/results.pcap", 'wb')
csvfile = open(args.o+"/results.card", "wb")
fieldnames = ['timestamp', 'origin', 'destination', 'msg']
writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
writer.writeheader()
#results = open(args.o+"/results.card", "wb")
#msg = binascii.hexlify('hello')

# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)
socket.connect("tcp://localhost:5557")
socket.setsockopt(zmq.SUBSCRIBE, "")
i = 0

print ' [*] Waiting for logs. To exit press CTRL+C'
while True:
    device_id, msg = socket.recv_multipart()
    device_id_str = str(device_id)
    msg_str = str(msg)
    (netide_version, netide_msg_type, netide_msg_len, netide_xid, netide_mod_id, netide_datapath) = NetIDEOps.netIDE_decode_header(msg)
    netide_msg_type_v2 = NetIDEOps.key_by_value(NetIDEOps.NetIDE_type, netide_msg_type)
    message_data = msg[NetIDEOps.NetIDE_Header_Size:]
    ret = bytearray(message_data)
    #datapath = decoded_header[NetIDEOps.NetIDE_header['DPID']]
    #xid_netide = decoded_header[NetIDEOps.NetIDE_header['XID']]
    #mod_id = decoded_header[NetIDEOps.NetIDE_header['MOD_ID']]
   

    if len(ret) >= ofproto_common.OFP_HEADER_SIZE:
       (version, msg_type, msg_len, xid) = ofproto_parser.header(ret)
       msg_decoded = ofproto_parser.msg(netide_datapath, version, msg_type, msg_len, xid, ret)
       #msg_type_v2 = 0
       #if msg_type in ofproto_v1_0.ofp_type._members_ == True:
          #msg_type_v2 = ofproto_v1_0.ofp_type._members_[msg_type]
       #msg_header_decoded = (version, msg_type, msg_len, xid)

    #for a in msg:
    #   msg_decimal.append(str(ord(a)))

    #for a in msg_decimal:
    #   msg_ascii.append(str(a))
    #print "-message " + str(msg) + "  received from " + device_id_str
    t=time.strftime("%H:%M:%S")
    if device_id_str[2:] == "shim":
        if 'msg_decoded' in locals() or 'msg_decoded' in globals():
           print "New message from shim %r at %r"%(device_id_str, t)
           print "\033[1;32mNetIDE header: Version = %r, Type of msg = %r, Length = %r Bytes, XID = %r, Module ID = %r, Datapath = %r\033[1;m"% (netide_version, netide_msg_type_v2, netide_msg_len, netide_xid, netide_mod_id, netide_datapath)
           print '\033[1;32mOpenFlow message header: Version = %r, Type of msg = %r, Length = %r Bytes, XID = %r\033[1;m'% (version, msg_type, msg_len, xid)
           print '\033[1;32mOpenFlow message: %r \033[1;m'% (msg_decoded)
           print "\n"
           writer.writerow({'timestamp':t, 'origin':device_id_str, 'destination':'NULL', 'msg':msg_decoded})
           #results.write("timestamp: %r,%r,%r,%r\n"% (t, device_id_str, "NULL", msg))
           #print '\033[1;34m[%r] [%r] %r\033[1;m'% (xid_netide, mod_id, msg_header_decoded)+'\n'
           #print mod_id           
           #print ' '.join([str(ord(a)) for a in msg_str])
           #print ' '.join(unicode(a, "utf-8") for a in msg_str)
           #print ' '.join(a.decode('ascii') for a in msg)
           #print binascii.hexlify(msg).decode("ascii")
           #print ' '.join(a.decode('hex') for a in msg)
           #print type(msg_str)
        fo.write("[%r] [%r] %r \n"% (t, device_id_str, msg));
        msg_cap = binascii.hexlify(msg)
        #global i
        #print i
        bytestring = generatePCAP(msg_cap,i)
        i = sum_one(i)
        #print bytestring
        bytelist = bytestring.split()
        #print bytelist  
        bytes = binascii.a2b_hex(''.join(bytelist))
        #print bytes
        bitout.write(bytes);
    else:
        if 'msg_decoded' in locals() or 'msg_decoded' in globals():
           print "New message from backend %r at %r"%(device_id_str, t)
           print "\033[1;36mNetIDE header: Version = %r, Type of msg = %r, Length = %r Bytes, XID = %r, Module ID = %r, Datapath = %r\033[1;m"% (netide_version, netide_msg_type_v2, netide_msg_len, netide_xid, netide_mod_id, netide_datapath)
           print '\033[1;36mOpenFlow message header: Version = %r, Type of msg = %r, Length = %r Bytes, XID = %r\033[1;m'% (version, msg_type, msg_len, xid)
           print '\033[1;36mOpenFlow message: %r \033[1;m'% (msg_decoded)
           print "\n"
           writer.writerow({'timestamp':t, 'origin':device_id_str, 'destination':'NULL', 'msg':msg_decoded})
           #results.write("timestamp: %r,%r,%r,%r\n"% (t, device_id_str, "NULL", msg))
           #print '\033[1;34m[%r] [%r] %r\033[1;m'% (xid_netide, mod_id, msg_header_decoded)+'\n'
        fo.write("[%r] [%r] %r\n"% (t, device_id_str, msg));
        msg_cap = binascii.hexlify(msg)
        #print i
        bytestring = generatePCAP(msg_cap,i)
        i = sum_one(i)
        #print bytestring
        bytelist = bytestring.split()
        #print bytelist  
        bytes = binascii.a2b_hex(''.join(bytelist))
        #print bytes
        bitout.write(bytes);

fo.close()
bitout.close()
