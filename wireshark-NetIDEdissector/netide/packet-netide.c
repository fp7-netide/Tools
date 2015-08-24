#include "config.h"

#include <epan/packet.h>
#include "packet-netide.h"
#include <string.h>
#include <stdio.h>
#include <glib.h>

#define NETIDE_PORT 41414

static int proto_netide = -1;
//static dissector_handle_t data_handle=NULL;
static dissector_handle_t netide_handle;
static dissector_handle_t openflow_v1_handle;
//void proto_register_netide(void);
//void proto_reg_handoff_netide(void);
static void dissect_netide(tvbuff_t *tvb, packet_info *pinfo, proto_tree *tree);

static int hf_netide_ver = -1;
static int hf_netide_type = -1;
static int hf_netide_length = -1;
static int hf_netide_xid = -1;
static int hf_netide_datapath_id = -1;
//static int hf_netide_openflow_msg = -1;

static gint ett_netide = -1;

static const value_string packettypenames[] = {
    { 17, "NETIDE_OPENFLOW" },
    { 18, "NETIDE_NETCONF" },
    { 19, "NETIDE_HELLO" },
    { 20, "NETIDE_ERROR" },
    { 21, "NETIDE_OPFLEX" }
};


static void
dissect_netide(tvbuff_t *tvb, packet_info *pinfo, proto_tree *tree)
{
    gint offset = 0;
    int length, op_length;
    tvbuff_t *next_tvb;
//    guint8 type_temp, type;

//    type_temp    = tvb_get_guint8(tvb, 2);
//    type = (int)strtol(type_temp, NULL, 16);
    col_set_str(pinfo->cinfo, COL_PROTOCOL, "NETIDE");
    /* Clear out stuff in the info column */
    col_clear(pinfo->cinfo,COL_INFO);
    col_add_fstr(pinfo->cinfo, COL_INFO, "%d -> %d ",
             pinfo->srcport, pinfo->destport);

    length = tvb_length(tvb);

    if (tree) { /* we are being asked for details */
        proto_item *netide_item = NULL;
        proto_tree *netide_tree = NULL;

        netide_item = proto_tree_add_item(tree, proto_netide, tvb, 0, -1, ENC_NA);
        netide_tree = proto_item_add_subtree(netide_item, ett_netide);
        proto_tree_add_item(netide_tree, hf_netide_ver, tvb, offset, 1, ENC_BIG_ENDIAN);
        offset += 1;
        proto_tree_add_item(netide_tree, hf_netide_type, tvb, offset, 1, ENC_BIG_ENDIAN);
        offset += 1;
        proto_tree_add_item(netide_tree, hf_netide_length, tvb, offset, 2, ENC_BIG_ENDIAN);
        offset += 2;
        proto_tree_add_item(netide_tree, hf_netide_xid, tvb, offset, 4, ENC_BIG_ENDIAN);
        offset += 4;
        proto_tree_add_item(netide_tree, hf_netide_datapath_id, tvb, offset, 8, ENC_BIG_ENDIAN);
        offset += 8;
        op_length = length-offset;
//        next_tvb = tvb_new_subset(tvb, offset, length-offset, length-offset);
        next_tvb = tvb_new_subset(tvb, offset, op_length, op_length);
        call_dissector(openflow_v1_handle, next_tvb, pinfo, netide_tree);
//        proto_tree_add_item(netide_tree, hf_netide_openflow_msg, tvb, offset, -1, ENC_BIG_ENDIAN);
    }
}

void proto_register_netide(void)
{

    static hf_register_info hf[] = {
        { &hf_netide_ver,
            { "NETIDE Version", "netide.ver",
            FT_UINT8, BASE_HEX,
            NULL, 0x0,
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
        { &hf_netide_datapath_id,
            { "datapath_id", "netide.datapath_id",
            FT_UINT64, BASE_DEC,
            NULL, 0x0,
            "datapath_id", HFILL }
        },
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
//    static gboolean initialized=FALSE;

    netide_handle = create_dissector_handle(dissect_netide, proto_netide);
    dissector_add_uint("tcp.port", NETIDE_PORT, netide_handle);
    openflow_v1_handle = find_dissector("openflow_v1");
/*    if (!initialized) {
        data_handle = find_dissector("data");
        netide_handle = create_dissector_handle(dissect_netide, proto_netide);
        dissector_add_uint("tcp.port", NETIDE_PORT, netide_handle);
    }*/
}
