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


    def send_port_stats_request(self, datapath):
	    ofp = datapath.ofproto
	    ofp_parser = datapath.ofproto_parser
	    req = ofp_parser.OFPPortStatsRequest(datapath, 0, ofp.OFPP_NONE)
	    datapath.send_msg(req)

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