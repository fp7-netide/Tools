# Traffic Generator - http

This tool is a client / server application which simulate HTTP traffic from a host to a server. The traffic patterns are similar to what can be found in this work[1].

## Usage

On the server(s) run:
`
./HTTPserver.sh
`
you can quit by either pressing Enter of ctrl+c.
The server will create multiple files in a folder called HTTP_files. This folder will be deleted upon quitting the application.

On the client(s) run:
`
./HTTPclient.sh <serverIP>
`
you can quit by either pressing Enter of ctrl+c.
The client will connect to the provided IP and simulate the traffic from a user. The user will browse the internet and send/receive email (mandatory patterns). It will also randomly use a media pattern between listening to a web radio, stream music or watch a video. It will also randomly use a file synchronization service. 
The patterns available are:
<pre>
Mandatory pattern(s):
    Internet:   Simulate the loading of a web page (size 2MB)
                and wait for 5-60s to simulate reading
    Email:      Simulate sending and receiving an email (75KB)
                continuously

Random pattern(s):
    Radio:      Simulate a web radio. This pattern will
                download a file at a low rate (20KB/s) constantly.

    Music:      Simulate a streaming service. This patten will
                download a "song" (3-5MB file) and wait before 
                loading another "song" (3-5mn)

    Sync:       Simulate the synchronization of data

    Video:      Simulate the browsing and streaming of a video
</pre>
all files created on the host will be deleted upon closing the application.

## References

[1] https://hal.archives-ouvertes.fr/hal-00685658/document

