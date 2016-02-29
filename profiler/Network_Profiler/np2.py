from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_0
from ryu.ofproto import ofproto_v1_0_parser
from ryu.controller.controller import *
from ryu.controller import *
from ryu import utils
from ryu.lib import hub
import threading
import time

class NetworkProfiler2(app_manager.RyuApp):
    def __init__(self, *args, **kwargs):
        super(NetworkProfiler2, self).__init__(*args, **kwargs)
        self.counter = 0

    @set_ev_cls(ofp_event.EventOFPPortStatsReply, MAIN_DISPATCHER)
    def port_stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body
        self.counter = self.counter + 10
        #throughput = body.stat[4]*8/self.counter
        #print body
        
        ports = []
        #Para cada switch
        for stat in body:
        	ports.append('port_no=%d '
	                     'rx_packets=%d tx_packets=%d '
	                     'rx_bytes=%d tx_bytes=%d '
	                     'rx_dropped=%d tx_dropped=%d '
	                     'rx_errors=%d tx_errors=%d '
	                     'rx_frame_err=%d rx_over_err=%d rx_crc_err=%d '
	                     'collisions=%d throughput=%d time=%d ' %
	                     (stat.port_no,
	                      stat.rx_packets, stat.tx_packets,
	                      stat.rx_bytes, stat.tx_bytes,
	                      stat.rx_dropped, stat.tx_dropped,
	                      stat.rx_errors, stat.tx_errors,
	                      stat.rx_frame_err, stat.rx_over_err,
	                      stat.rx_crc_err, stat.collisions, stat.tx_bytes/self.counter, self.counter))
        #print (ports[0].tx_bytes)

        print'\033[1;32m PortStats: %s \033[1;m'%(ports)
        #print 'Throughput = %d bits/s' (throughput)