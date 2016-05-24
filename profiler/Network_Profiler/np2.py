from ryu.base import app_manager
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_0
from ryu.ofproto import ofproto_v1_0_parser
from ryu.ofproto.ofproto_v1_0_parser import *
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

    #@set_ev_cls(ofp_event.EventOFPPortStatsReply, MAIN_DISPATCHER)
    def port_stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body
        self.counter = self.counter + 10
        #throughput = body.stat[4]*8/self.counter
        #print body
        
        ports = []
        #For each switch
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
        print('\n')
        #print 'Throughput = %d bits/s' (throughput)


    #@set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def switch_features_handler(self, ev):
        msg = ev.msg
        feautures = []
        feautures.append('datapath_id=0x%016x n_buffers=%d '
                         'n_tables=%d capabilities=0x%08x ports=%s' %
                         (msg.datapath_id, msg.n_buffers, msg.n_tables,
                          msg.capabilities, msg.ports))
        print'\033[1;36m OFPSwitchFeatures received: %s \033[1;m'%(feautures)
        print('\n')


    #@set_ev_cls(ofp_event.EventOFPDescStatsReply, MAIN_DISPATCHER)
    def desc_stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body

        print('DescStats: mfr_desc=%s hw_desc=%s sw_desc=%s '
                          'serial_num=%s dp_desc=%s',
                          body.mfr_desc, body.hw_desc, body.sw_desc,
                          body.serial_num, body.dp_desc)
        print('\n')


    #@set_ev_cls(ofp_event.EventOFPFlowStatsReply, MAIN_DISPATCHER)
    def flow_stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body

        flows = []
        for stat in body:
            flows.append('table_id=%s match=%s '
                         'duration_sec=%d duration_nsec=%d '
                         'priority=%d '
                         'idle_timeout=%d hard_timeout=%d '
                         'cookie=%d packet_count=%d byte_count=%d '
                         'actions=%s' %
                         (stat.table_id, stat.match, stat.duration_sec, stat.duration_nsec, stat.priority, stat.idle_timeout, stat.hard_timeout,
                         stat.cookie, stat.packet_count, stat.byte_count, stat.actions))
        print'\033[1;36m FlowStats: %s \033[1;m'%(flows)
        print('\n')


    #@set_ev_cls(ofp_event.EventOFPAggregateStatsReply, MAIN_DISPATCHER)
    def aggregate_stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body

        print'\033[1;34m AggregateStats: packet_count=%d, byte_count=%d, flow_count=%d\033[1;m'%(body[0].packet_count, body[0].byte_count, body[0].flow_count)
        print('\n')


    #@set_ev_cls(ofp_event.EventOFPTableStatsReply, MAIN_DISPATCHER)
    def table_stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body
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


    @set_ev_cls(ofp_event.EventOFPQueueStatsReply, MAIN_DISPATCHER)
    def stats_reply_handler(self, ev):
        msg = ev.msg
        ofp = msg.datapath.ofproto
        body = ev.msg.body
        queues = []
        for stat in body:
            queues.append('port_no=%d queue_id=%d '
                          'tx_bytes=%d tx_packets=%d tx_errors=%d ' %
                         (stat.port_no, stat.queue_id,
                          stat.tx_bytes, stat.tx_packets, stat.tx_errors))
        print'\033[1;35m QueueStats: %s \033[1;m'%(queues)
        print('\n')
        print(len(body))



