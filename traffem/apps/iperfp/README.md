# Traffic Generator - iperfp

This tool is a client / server application which simulate internet UDP traffic.

## Usage

On the server run:
`
./iperfp.sh --server
`

On the client(s) run:
`
./iperfp.sh <serverIP> [--imix|--voip|--video]
`

`
Options:
    --server: run in server mode
    --imix:  play the Internet Mix pattern.
            7 packets of 40B
            4 packets of 576B
            1 packet  of 1500B
    --voip:  play a VoIP pattern (G.726 audio codec).
            130B packets @ 55KB/s
    --video: play a video straming pattern (H.264 video codec).
            1200B packets @ 471KB/s
`
##
