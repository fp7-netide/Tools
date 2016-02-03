#include "config.h"
#include <epan/packet.h>
#include "packet-netide.h"
#include <epan/dissectors/packet-tcp.h>
#include <epan/prefs.h>
#include <string.h>
#include <stdio.h>
#include <glib.h>

#define NETIDE_PORT 5555
#define OFP_VERSION_1_0 1
#define OFP_VERSION_1_1 2
#define OFP_VERSION_1_2 3
#define OFP_VERSION_1_3 4
#define OFP_VERSION_1_4 5

static int proto_netide = -1;
//static dissector_handle_t data_handle=NULL;
static dissector_handle_t netide_handle;
static dissector_handle_t openflow_v1_handle;
static dissector_handle_t openflow_v4_handle;
static dissector_handle_t openflow_v5_handle;
//static dissector_handle_t openflow_handle;
static dissector_handle_t data_handle;
//void proto_register_netide(void);
//void proto_reg_handoff_netide(void);
static void dissect_netide(tvbuff_t *tvb, packet_info *pinfo, proto_tree *tree);

//static int hf_zmq_header = -1;
//static int hf_zmq_packet = -1;
static int hf_netide_ver = -1;
static int hf_netide_type = -1;
static int hf_netide_length = -1;
static int hf_netide_xid = -1;
static int hf_netide_module_id = -1;
static int hf_netide_datapath_id = -1;
static int hf_netide_format = -1;
//static int hf_netide_openflow_msg = -1;

static gint ett_netide = -1;

static const value_string packettypenames[] = {
    { 1, "NETIDE_HELLO" },
    { 2, "NETIDE_ERROR" },
    { 3, "NETIDE_MGMT" },
    { 4, "MODULE_ANNOUNCEMENT" },
    { 5, "MODULE_ACKNOWLEDGE" },
    { 6, "NETIDE_HEARTBEAT" },
    { 17, "NETIDE_OPENFLOW" },
    { 18, "NETIDE_NETCONF" },
    { 19, "NETIDE_OPFLEX" }
};

static const value_string netidever[] = {
    { 3, "1.2"}
};


static void
dissect_netide(tvbuff_t *tvb, packet_info *pinfo, proto_tree *tree)
{
    guint offset = 0;
    guint length, op_length;
//    guint16 length;
    tvbuff_t *next_tvb;
    guint8 type, of_version;
//    guint8 type_temp, type;

//    type_temp    = tvb_get_guint8(tvb, 2);
//    type = (int)strtol(type_temp, NULL, 16);
    col_set_str(pinfo->cinfo, COL_PROTOCOL, "NETIDE");
    /* Clear out stuff in the info column */
    col_clear(pinfo->cinfo,COL_INFO);
    type = tvb_get_guint8(tvb, 1); //OJOOOOOOOOOOOOOOOO cuidado con los cambios aqui para dissector debugger y dissector core. son 3 y 1
    col_add_fstr(pinfo->cinfo, COL_INFO, "%d -> %d  NetIDE_Type: %s  ",
             pinfo->srcport, pinfo->destport, val_to_str_const(type, packettypenames, "Unknown NetIDE message type"));
    length = tvb_length(tvb);
//hay que coger la version del protocolo openflow tb y llamar a los diferentes dissector de cada version, no directamente al general, porque si llamamos al de openflow general machaca la vision del protocolo NetIDE
    if (tree) { /* we are being asked for details */
        proto_item *netide_item = NULL;
        proto_tree *netide_tree = NULL;

        netide_item = proto_tree_add_item(tree, proto_netide, tvb, 0, -1, ENC_NA);
        netide_tree = proto_item_add_subtree(netide_item, ett_netide);
//        proto_tree_add_item(netide_tree, hf_zmq_header, tvb, offset, 1, ENC_BIG_ENDIAN);
//        offset += 2;
        proto_tree_add_item(netide_tree, hf_netide_ver, tvb, offset, 1, ENC_BIG_ENDIAN);
        offset += 1;
        proto_tree_add_item(netide_tree, hf_netide_type, tvb, offset, 1, ENC_BIG_ENDIAN);
        offset += 1;
        proto_tree_add_item(netide_tree, hf_netide_length, tvb, offset, 2, ENC_BIG_ENDIAN);
        offset += 2;
        proto_tree_add_item(netide_tree, hf_netide_xid, tvb, offset, 4, ENC_BIG_ENDIAN);
        offset += 4;
        proto_tree_add_item(netide_tree, hf_netide_module_id, tvb, offset, 4, ENC_BIG_ENDIAN);
        offset += 4;
        proto_tree_add_item(netide_tree, hf_netide_datapath_id, tvb, offset, 8, ENC_BIG_ENDIAN);
        offset += 8;
//        length = tvb_get_ntohs(tvb, offset);
        op_length = length-offset;
//        next_tvb = tvb_new_subset(tvb, offset, length-offset, length-offset);
        next_tvb = tvb_new_subset(tvb, offset, op_length, op_length);
//        call_dissector(data_handle, next_tvb, pinfo, netide_tree);
        if (type == 17){
            of_version = tvb_get_guint8(next_tvb, 0);
            switch(of_version){
            case OFP_VERSION_1_0:
                call_dissector(openflow_v1_handle, next_tvb, pinfo, netide_tree);
                break;
            case OFP_VERSION_1_3:
                call_dissector(openflow_v4_handle, next_tvb, pinfo, netide_tree);
                break;
            case OFP_VERSION_1_4:
                call_dissector(openflow_v5_handle, next_tvb, pinfo, netide_tree);
                break;
            default:
                call_dissector(data_handle, next_tvb, pinfo, netide_tree);
                break;
            }
        }
        else if (type == 18){
            proto_tree_add_item(netide_tree, hf_netide_format, tvb, offset, 8, ENC_BIG_ENDIAN);
            offset += 8;
            call_dissector(data_handle, next_tvb, pinfo, netide_tree);
	}
        else if (type == 19){
            proto_tree_add_item(netide_tree, hf_netide_format, tvb, offset, 8, ENC_BIG_ENDIAN);
            offset += 8;
            call_dissector(data_handle, next_tvb, pinfo, netide_tree);
	}
        else {
            call_dissector(data_handle, next_tvb, pinfo, netide_tree);
        }
//        proto_tree_add_item(netide_tree, hf_netide_openflow_msg, tvb, offset, -1, ENC_BIG_ENDIAN);
    }
}

void proto_register_netide(void)
{

    static hf_register_info hf[] = {
/*        { &hf_zmq_header,
            { "ZMQ header", "zmq.header",
            FT_UINT16, BASE_HEX,
            NULL, 0x0,
            "ZMQ header", HFILL }
        },*/
        { &hf_netide_ver,
            { "NETIDE Version", "netide.ver",
            FT_UINT8, BASE_HEX,
            VALS(netidever), 0x0,
            "NETIDE Version", HFILL }
        },
        { &hf_netide_type,
            { "Type", "netide.type",
            FT_UINT8, BASE_DEC,
            VALS(packettypenames), 0x0,
            "Package Type", HFILL }
        },
        { &hf_netide_length,
            { "Package Length", "netide.length",
            FT_UINT16, BASE_DEC,
            NULL, 0x0,
            "Package Length", HFILL }
        },
        { &hf_netide_xid,
            { "xid", "netide.xid",
            FT_UINT32, BASE_DEC,
            NULL, 0x0,
            "Xid", HFILL }
        },
        { &hf_netide_module_id,
            { "module_id", "netide.module_id",
            FT_UINT32, BASE_DEC,
            NULL, 0x0,
            "module_id", HFILL }
        },
        { &hf_netide_datapath_id,
            { "datapath_id", "netide.datapath_id",
            FT_UINT64, BASE_DEC,
            NULL, 0x0,
            "datapath_id", HFILL }
        },
        { &hf_netide_format,
            { "Format", "netide.format",
            FT_UINT8, BASE_DEC,
            NULL, 0x0,
            "OpFlex/Netconf-Format", HFILL }
        }
/*        { &hf_netide_openflow_msg,
            { "openflow_msg", "netide.openflow_msg",
            FT_STRING, BASE_NONE,
            NULL, 0x0,
            "openflow_msg", HFILL }
        }*/
    };

    /* Setup protocol subtree array */
    static gint *ett[] = {
        &ett_netide
    };

    proto_netide = proto_register_protocol (
        "NETIDE Protocol", /* name       */
        "NETIDE",      /* short name */
        "netide"       /* abbrev     */
        );

    proto_register_field_array(proto_netide, hf, array_length(hf));
    proto_register_subtree_array(ett, array_length(ett));
    register_dissector("netide", dissect_netide, proto_netide);
}

void proto_reg_handoff_netide(void)
{
    static gboolean initialized=FALSE;
    if (!initialized) {
        netide_handle = create_dissector_handle(dissect_netide, proto_netide);
        dissector_add_uint("zmtp.protocol", NETIDE_PORT, netide_handle);
        data_handle = find_dissector("data");
        openflow_v1_handle = find_dissector("openflow_v1");
        openflow_v4_handle = find_dissector("openflow_v4");
        openflow_v5_handle = find_dissector("openflow_v5");
    }
//    openflow_v1_handle = find_dissector("openflow");
//    openflow_handle = find_dissector("openflow");

/*    if (!initialized) {
        data_handle = find_dissector("data");
        netide_handle = create_dissector_handle(dissect_netide, proto_netide);
        dissector_add_uint("tcp.port", NETIDE_PORT, netide_handle);
    }*/
}
