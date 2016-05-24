# Traffic Generator - tftp

This tool is a client / server application which simulate TFTP traffic.

## Usage

On the server run:
`
./tftpServer.sh
`

On the client(s) run:
`
./tftpClient.sh <serverIP> [test|small|medium|big]
`
By default the test file is downloaded.
The other available files are small (1MB), medium (10MB) and big (100MB).

##
