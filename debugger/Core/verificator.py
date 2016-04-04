import csv

class Module(object):
   origin=""
   destination=""
   length=0
   max_length=0
   min_length=0
   counter=0
   def __init__(self, origin, destination, length, max_length, min_length, counter):
      self.origin = origin
      self.destination = destination
      self.length = length
      self.max_length = max_length
      self.min_length = min_length
      self.counter = counter

module_list = []

def module_identification(module_list, origin, destination, length):
   if not module_list:
      module = Module(origin, destination, length, length, length, 1)
      module_list.append(module)
   else:
      validate = False
      for modules in module_list:
         if (modules.origin == origin and destination == modules.destination):
            modules.length = modules.length + length
            if modules.max_length < length:
               modules.max_length = length
            if modules.min_length > length:
               modules.min_length = length
            modules.counter = modules.counter + 1
            validate = True
            break
            
      if validate != True:
         module = Module(origin, destination, length, length, length, 1)
         module_list.append(module)

def print_module_list (module_list):
   for i in module_list:
      print('\033[1;34m%r has sent to %r %r messages. Average length of messages %r Bytes, maximum message size %r Bytes, minimum message size %r Bytes.\033[1;m')% (i.origin, i.destination, i.counter, i.length/i.counter , i.max_length, i.min_length)
      print('\n')
      #print (i.origin, i.destination, i.length, i.max_length, i.min_length, i.counter)

with open('results.card') as csvfile:
   reader = csv.DictReader(csvfile)
   for row in reader:
      module_identification(module_list, row['origin'], row['destination'], int(row['length'] or 0))

print_module_list(module_list)




