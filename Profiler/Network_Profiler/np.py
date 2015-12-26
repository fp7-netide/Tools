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

class NetworkProfiler(app_manager.RyuApp):
    def __init__(self, *args, **kwargs):
        super(NetworkProfiler, self).__init__(*args, **kwargs)
        self.counter = 0


    # enviar mensaje de peticion de estadisticas al dataplane
	"""def send_features_request(self, datapath):
	    ofp_parser = datapath.ofproto_parser

	    req = ofp_parser.OFPFeaturesRequest(datapath)
	    datapath.send_msg(req)"""

	# recibir la respuesta al anterior mensaje de estadisticas
	"""@set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
	def switch_features_handler(self, ev):
	    msg = ev.msg

	    self.logger.debug('OFPSwitchFeatures received: '
	                      'datapath_id=0x%016x n_buffers=%d '
	                      'n_tables=%d capabilities=0x%08x ports=%s',
	                      msg.datapath_id, msg.n_buffers, msg.n_tables,
	                      msg.capabilities, msg.ports)"""
    #print'\033[1;32m %r \033[1;m'%("HOLa")
    

    # Estadisticas por flow
    """@set_ev_cls(ofp_event.EventOFPAggregateStatsReply, MAIN_DISPATCHER)
    def aggregate_stats_reply_handler(self, ev):
	    #msg = ev.msg
	    #ofp = msg.datapath.ofproto
	    content = ev.msg.body
	    print'\033[1;32m %r \033[1;m'%("The message has been received.")
	    print'AggregateStats: packet_count=%d byte_count=%d flow_count=%d'%(content[0].packet_count, content[0].byte_count, content[0].flow_count)
	    
    def send_aggregate_stats_request(self, datapath):
	    ofp = datapath.ofproto
	    ofp_parser = datapath.ofproto_parser

	    cookie = cookie_mask = 0
	    match = ofp_parser.OFPMatch(in_port=1)
	    req = ofp_parser.OFPAggregateStatsRequest(datapath, 0, match, 0xff, ofp.OFPP_NONE)
	    datapath.send_msg(req)
	    print'\033[1;32m %r \033[1;m'%("The message has been sent.")"""

    """def send_echo_request(self, datapath, data):
	    ofp_parser = datapath.ofproto_parser

	    req = ofp_parser.OFPEchoRequest(datapath, data)
	    datapath.send_msg(req)
	    #threading.Timer(1, send_echo_request).start()"""

    def send_port_stats_request(self, datapath):
	    ofp = datapath.ofproto
	    ofp_parser = datapath.ofproto_parser
	    req = ofp_parser.OFPPortStatsRequest(datapath, 0, ofp.OFPP_NONE)
	    datapath.send_msg(req)
	    #threading.Timer(1, self.send_port_stats_request).start()


    """@set_ev_cls(ofp_event.EventOFPEchoReply, MAIN_DISPATCHER)
    def echo_reply_handler(self, ev):
        print'\033[1;32m OFPEchoReply received: data=%s \033[1;m'%(ev.msg.data)"""

    """@set_ev_cls(ofp_event.EventOFPPortStatsReply, MAIN_DISPATCHER)
    def port_stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body
        ports = []
        for stat in body:
        	ports.append('port_no=%d '
	                     'rx_packets=%d tx_packets=%d '
	                     'rx_bytes=%d tx_bytes=%d '
	                     'rx_dropped=%d tx_dropped=%d '
	                     'rx_errors=%d tx_errors=%d '
	                     'rx_frame_err=%d rx_over_err=%d rx_crc_err=%d '
	                     'collisions=%d' %
	                     (stat.port_no,
	                      stat.rx_packets, stat.tx_packets,
	                      stat.rx_bytes, stat.tx_bytes,
	                      stat.rx_dropped, stat.tx_dropped,
	                      stat.rx_errors, stat.tx_errors,
	                      stat.rx_frame_err, stat.rx_over_err,
	                      stat.rx_crc_err, stat.collisions))
        print'\033[1;32m PortStats: %s \033[1;m'%(ports)"""



    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def _switch_features_handler(self, ev):
        msg = ev.msg
        dp = msg.datapath
        ofp = dp.ofproto
        data = "hola"
        ofp_parser = dp.ofproto_parser
	    #self.send_aggregate_stats_request(dp)
        #self.send_port_stats_request(dp)
        while True:
        	self.send_port_stats_request(dp)
        	hub.sleep(10)
        #threading.Timer(5, self.send_port_stats_request(dp)).start()

    #@set_ev_cls(ofp_event.EventOFPPortStatsReply, MAIN_DISPATCHER)
    #def port_stats_reply_handler(self, ev):
    	#print ("hola")