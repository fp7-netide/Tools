from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import DEAD_DISPATCHER
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
        self.datapaths = {}
        #time.sleep(1)
        self.monitor_thread = hub.spawn(self.menu)
        #self.menu()


    
    #The controller uses this message to query information about ports statistics.
    def send_port_stats_request(self, datapath):
	    ofp = datapath.ofproto
	    ofp_parser = datapath.ofproto_parser
	    req = ofp_parser.OFPPortStatsRequest(datapath, 0, ofp.OFPP_NONE)
	    datapath.send_msg(req)


    #The controller sends a feature request to the switch upon session establishment.
    def send_features_request(self, datapath):
        ofp_parser = datapath.ofproto_parser
        req = ofp_parser.OFPFeaturesRequest(datapath)
        datapath.send_msg(req)


    #The controller uses this message to query description of the switch.
    def send_desc_stats_request(self, datapath):
        ofp_parser = datapath.ofproto_parser
        req = ofp_parser.OFPDescStatsRequest(datapath)
        datapath.send_msg(req)


    #The controller uses this message to query individual flow statistics.
    def send_flow_stats_request(self, datapath):
        ofp = datapath.ofproto
        ofp_parser = datapath.ofproto_parser

        match = ofp_parser.OFPMatch(in_port=1)
        table_id = 0xff
        out_port = ofp.OFPP_NONE
        req = ofp_parser.OFPFlowStatsRequest(datapath, 0, match, table_id, out_port)
        datapath.send_msg(req)


    #The controller uses this message to query aggregate flow statistics.
    def send_aggregate_stats_request(self, datapath):
        ofp = datapath.ofproto
        ofp_parser = datapath.ofproto_parser

        cookie = cookie_mask = 0
        match = ofp_parser.OFPMatch(in_port=1)
        req = ofp_parser.OFPAggregateStatsRequest(datapath, 0, match, 0xff, ofp.OFPP_NONE)
        datapath.send_msg(req)


    #The controller uses this message to query flow table statictics.
    def send_table_stats_request(self, datapath):
        ofp_parser = datapath.ofproto_parser
        req = ofp_parser.OFPTableStatsRequest(datapath, 0)
        datapath.send_msg(req)


    #The controller uses this message to query queue statictics.
    def send_queue_stats_request(self, datapath):
	    ofp = datapath.ofproto
	    ofp_parser = datapath.ofproto_parser
	    #In the official documentation of RYU specifies the third parameter as OFPT_ALL. Which is an error
	    req = ofp_parser.OFPQueueStatsRequest(datapath, 0, 2, ofp.OFPQ_ALL)
	    datapath.send_msg(req)



    @set_ev_cls(ofp_event.EventOFPStateChange, [MAIN_DISPATCHER, DEAD_DISPATCHER])
    def _state_change_handler(self, ev):
        datapath = ev.datapath
        if ev.state == MAIN_DISPATCHER:
            if not datapath.id in self.datapaths:
                self.logger.debug('register datapath: %016x', datapath.id)
                self.datapaths[datapath.id] = datapath
        elif ev.state == DEAD_DISPATCHER:
            if datapath.id in self.datapaths:
                self.logger.debug('unregister datapath: %016x', datapath.id)
                del self.datapaths[datapath.id]




    def menu(self):
        
        while True:
            for dp in self.datapaths.values():
                self.send_port_stats_request(dp)
                self.send_flow_stats_request(dp)
                self.send_aggregate_stats_request(dp)
                self.send_table_stats_request(dp)
                self.send_queue_stats_request(dp)
            time.sleep(10)

        '''



        n = -1

        while True:
	    
	        print("------ NETWORK PROFILER V.1.0 ------")
	        print("1 -> Show me statistics per port")
	        print("2 -> Show me statistics per flow")
	        print("3 -> Show me statistics per flow (aggregate)")
	        print("4 -> Show me statistics per table")
	        print("5 -> Show me statistics per queue")
	        print("0 -> Exit")
	    
	        n=input("Choose an option: ")


	        if (n == 1):
	    	    for dp in self.datapaths.values():
	    	        self.send_port_stats_request(dp)
	        
	        elif (n == 2):
	    	    send_flow_stats_request(dp)

	        elif (n == 3):
	    	    send_aggregate_stats_request(dp)

	        elif (n == 4):
	    	    send_table_stats_request(dp)

	        elif (n == 5):
	    	    send_queue_stats_request(dp)
	    	'''



