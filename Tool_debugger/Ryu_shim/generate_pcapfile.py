import sys
import binascii
"""----------------------------------------------------------------"""
""" Do not edit below this line unless you know what you are doing """
"""----------------------------------------------------------------"""



#Global header for pcap 2.4
pcap_global_header =   ('D4 C3 B2 A1'   
                        '02 00'         #File format major revision (i.e. pcap <2>.4)  
                        '04 00'         #File format minor revision (i.e. pcap 2.<4>)   
                        '00 00 00 00'     
                        '00 00 00 00'     
                        'FF FF 00 00'     
                        '01 00 00 00')

#pcap packet header that must preface every packet
pcap_packet_header =   ('AA 77 9F 47'     
                        '90 A2 04 00'     
                        'XX XX XX XX'   #Frame Size (little endian) 
                        'YY YY YY YY')  #Frame Size (little endian)

eth_header =   ('00 00 00 00 00 00'     #Source Mac    
                '00 00 00 00 00 00'     #Dest Mac  
                '08 00')                #Protocol (0x0800 = IP)

ip_header =    ('45'                    #IP version and header length (multiples of 4 bytes)   
                '00'                      
                'XX XX'                 #Length - will be calculated and replaced later
                '00 00'                   
                '40 00 40'                
                '06'                    #Protocol (0x06 = TCP)          
                'YY YY'                 #Checksum - will be calculated and replaced later      
                '7F 00 00 01'           #Source IP (Default: 127.0.0.1)         
                '7F 00 00 01')          #Dest IP (Default: 127.0.0.1) 

tcp_header =   ('80 00'                 #Source_Port - will be replaced later  
                'A1 C6'                 #Destination_Port - will be replaced later 
                'XX XX XX XX'           #Secuence number
                'd3 4c 7d 24'           #Acknowledgment number
                '80'                    #Header_Length - will be calculated and replaced later                        
                '18'                    #Flags
                'YY YY'                 #Window size
                '00 00'                 #Cheksum
                '00 00'
                '01 01 08 0a 00 55 5b 3a 00 2d 6d 44') #options
                
def getByteLength(str1):
    return len(''.join(str1.split())) / 2
#    return len(str1)

"""def getByteLength_head(str1):
    print str1
    str1_temp = str1.split()
    print str1_temp
    str1_temp2 = ''.join(str1_temp)
    print str1_temp2
    num_bytes = len(str1_temp2) / 2
    print num_bytes
    num_bits = num_bytes * 8
    return num_bits / 2"""
#    return len(str1)

"""def writeByteStringToFile(bytestring, filename):
    bytelist = bytestring.split()  
    bytes = binascii.a2b_hex(''.join(bytelist))
    bitout = open(filename, 'wb')
    bitout.write(bytes)"""

def generatePCAP(port,message,i): 

#    tcp = tcp_header.replace('XX XX',"%04x"%port)
    n_temp = hex(int('9ef85d74',16) + i).rstrip("L").lstrip("0x") or "0"
    n_seq = ' '.join(s.encode('hex') for s in n_temp.decode('hex'))
#    print n_seq
    tcp = tcp_header.replace('XX XX XX XX',n_seq)
    tcp_head_len = getByteLength(tcp_header)
#    print tcp_head_len
#    tcp = tcp_header.replace('XX',"%x"%tcp_head_len)
#    tcp = tcp_header.replace('XX',str(tcp_head_len))
    tcp_len = getByteLength(message) + tcp_head_len
#    print tcp_len
#    tcp_len_hex = hex(tcp_len).rstrip("L").lstrip("0x") or "0"
#    print tcp_len_hex
    tcp = tcp.replace('YY YY',"%04x"%tcp_len)
#    print getByteLength(tcp_header)
#    print getByteLength(message)

#    print tcp

    ip_len = tcp_len + getByteLength(ip_header)
    ip = ip_header.replace('XX XX',"%04x"%ip_len)
    checksum = ip_checksum(ip.replace('YY YY','00 00'))
    ip = ip.replace('YY YY',"%04x"%checksum)
    
    pcap_len = ip_len + getByteLength(eth_header)
    hex_str = "%08x"%pcap_len
    reverse_hex_str = hex_str[6:] + hex_str[4:6] + hex_str[2:4] + hex_str[:2]
    pcaph = pcap_packet_header.replace('XX XX XX XX',reverse_hex_str)
    pcaph = pcaph.replace('YY YY YY YY',reverse_hex_str)

    if (i==0):
        bytestring = pcap_global_header + pcaph + eth_header + ip + tcp + message
    else:
        bytestring = pcaph + eth_header + ip + tcp + message
    return bytestring
#    writeByteStringToFile(bytestring, pcapfile)

#Splits the string into a list of tokens every n characters
def splitN(str1,n):
    return [str1[start:start+n] for start in range(0, len(str1), n)]

#Calculates and returns the IP checksum based on the given IP Header
def ip_checksum(iph):

    #split into bytes    
    words = splitN(''.join(iph.split()),4)

    csum = 0;
    for word in words:
        csum += int(word, base=16)

    csum += (csum >> 16)
    csum = csum & 0xFFFF ^ 0xFFFF

    return csum

"""------------------------------------------"""
""" End of functions, execution starts here: """
"""------------------------------------------"""
"""
if len(sys.argv) < 2:
        print 'usage: pcapgen.py output_file'
        exit(0)"""
