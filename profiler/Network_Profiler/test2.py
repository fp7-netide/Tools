import zmq
import time



context = zmq.Context()
publisher = context.socket(zmq.PUB)
publisher.bind("tcp://127.0.0.1:5558")
src = "np1"
dst = "1_"
data_string = "hola"
#for elem in data_string:
#   data = elem.encode("hex")
#print(data)


time.sleep(1)
#publisher.send_multipart(origen, destino, msg)
publisher.send(src, len(src), zmq.SNDMORE);
publisher.send(dst, len(dst), zmq.SNDMORE);
publisher.send(data_string, len(data_string), 0);
#publisher.send ("2")
#publisher.send("WOWOWOWOWOWOWOWWOWO")