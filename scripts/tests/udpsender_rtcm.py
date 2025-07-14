import socket
import time
import os

# Currently set to oim-dev environment's ODE
UDP_IP = os.getenv('DOCKER_HOST_IP')
UDP_PORT = int(os.getenv('UDP_PORT', 44960))
MESSAGE = "001c6d25244028d2d4b29bc25ec0c12c418d300133ed980037bdd1a80c6358121dd4e499ffc6712e91f0d10f4c00f90266005105115939553131053951153939048081393d391400003551492535093114810531313d64513985d880d4b8d0d080bc8109bdbdd080d4b8d0d0023197e4"

print("UDP target IP:", UDP_IP)
print("UDP target port:", UDP_PORT)
print("message:", MESSAGE)

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP

while True:
  sock.sendto(bytes.fromhex(MESSAGE), (UDP_IP, UDP_PORT))
  time.sleep(5)
  print("sending RTCM every 5 seconds")
