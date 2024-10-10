import socket
import time
import os

BSM_MESSAGE = "0022e12d18466c65c1493800000e00e4616183e85a8f0100c000038081bc001480b8494c4c950cd8cde6e9651116579f22a424dd78fffff00761e4fd7eb7d07f7fff80005f11d1020214c1c0ffc7c016aff4017a0ff65403b0fd204c20ffccc04f8fe40c420ffe6404cefe60e9a10133408fcfde1438103ab4138f00e1eec1048ec160103e237410445c171104e26bc103dc4154305c2c84103b1c1c8f0a82f42103f34262d1123198103dac25fb12034ce10381c259f12038ca103574251b10e3b2210324c23ad0f23d8efffe0000209340d10000004264bf00"
MAP_MESSAGE = "001283c138003000205e9c014d3eab092ca624b5518202dc3658042800000400023622c60ca009f66d48abfaf81388d8ad18070027d9b2ffcfe9804f13667b1ffd009ec2c76e3ffc82c4e0001004b00c5000000800066c4574101813ecd8b757fae027d9b30e6ff5604ec363561fe7809ec6cd69bfec813c4d8a617fc9027d9b2147008604fb163666000016250000802580228000001000096229e1309b51a6fe4204dd361cf1fe5009f6018e1000096020a00000080004d88a57f84027d9b3827002804ec36087600a009f62c289407282c310001c0440188800000006c46dbe02813ec5816d800710052200000001b11b6fad404fb16054a0000401c8800000006c47b3d24813ec5816d801b100c4200000000af890f12c580007e87100d4200000008af4c0f12c580077e7a2c0004000160002001cb028d000000800052c160bc40b5fffd8a9409d86bfebb5b40141457fef53b76c008b467014145800080002bffcbffc82c6a0001804b024d000000800036c2213c3b013ecd80096d64027d9affd8cdfc04f635ff7983bc09f66c0082aa2014280b1b80006012c0b3400000100004b02bcf0f6d7fe065d602788b0138eb900b1240001012c083400000080009b0c2af0b804fb15fe6de171afff6c63e04ec15fe1de670060e40002581ea8000004000135da6df0180a0a6adc2c00d0143cd51897fda028c8abb25001a0b0680008012c105400000200009aedbefae005053540ee003c0a326a9cf3fed8143c5667780010582c0004009608aa00000080004d76de7ee402829aba88ffdc050f354525fff80a322bcf23fa602c690000c04b0395000000200016bb4fbd4e01414d3215800802940ab108fff2030d2000110126200000001aee5103be050a15f6f1ffc8404d8800000006bb97c18e0142857dfa800010146200000001aee89099a050a15f8720000b05dd000000800046be3743b781428d80e1b00002879b00514b4404f63600827d8c09e22c000400015ffe6007016190000402582ce8000004000135ecee1de80a146c02e54758143cd8059ad3e027b1b00613dd004f102c360000804b055d000000200046bcc7c3c781428d80108c6e02829b002b2ece050a16019a4b29b00ab5c3604f136004e410409ec018a10000960c3a00000080004d7de9878602851b003923cc05053601623b440a0a6bfb8c3a5014140b0640005012c197400000100005afe570ef2050a36003a47c80a0a6bfd2c45f014140b054000501101a8200000001b05a90edc050535ffe605800a0a101b8200000001b08a30ec0050535ffe605300a0a101c8200000005b0c6f0ea4050515ffca0568b0001000e"
SPAT_MESSAGE = "001338000817a780000089680500204642b342b34802021a15a955a940181190acd0acd20100868555c555c00104342aae2aae002821a155715570"
PSM_MESSAGE = "011d0000201a0000021bd86891de75f84da101c13f042e2214141fff00022c2000270000000163b2cc7986010000"
SSM_MESSAGE = "001e120000000005e9c04071a26614c06000040ba0"
SRM_MESSAGE = "001d2130000010090bd341080d00855c6c0c6899853000a534f7c24cb29897694759b7c0"
TIM_MESSAGE = "005f498718cca69ec1a04600000100105d9b46ec5be401003a0103810040038081d4001f80d07016da410000000000000bbc2b0f775d9b0309c271431fa166ee0a27fff93f136b8205a0a107fb2ef979f4c5bfaeec97e4ad70c2fb36cd9730becdb355cc2fd2a7556b160b98b46ab98ae62c185fa55efb468d5b4000000004e2863f42cddc144ff7980040401262cdd7b809c509f5c62cdd35519c507b9062cdcee129c505cf262cdca5ff9c50432c62cdc5d3d9c502e3e62cdc13e79c501e9262cdbca2d9c5013ee62cdb80359c500e6a62cdb36299c500bc862cdaec1d9c50093c62cdaa2109c5006ea1080203091a859eeebb36006001830001aad27f4ff7580001aad355e39b5880a30029d6585009ef808332d8d9f80c3855151b38c772f765007967ec1170bcb7937f5cb880a25a52863493bcb87570dbcb5abc6bfb2faec606cfa34eb95a24790b2017366d3aabe7729e"
BAD_MESSAGE = "0000badc0de0000000000000000000000000000000"

# Currently set to oim-dev environment's ODE
UDP_IP = os.getenv('DOCKER_HOST_IP')
UDP_PORT = 44990
print("UDP target IP:", UDP_IP)
print("UDP target port:", UDP_PORT)
#print("message:", MESSAGE)

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP

while True:
  time.sleep(5)
  print("sending Messages every 5 second")
  sock.sendto(bytes.fromhex(BSM_MESSAGE), (UDP_IP, UDP_PORT))
  sock.sendto(bytes.fromhex(MAP_MESSAGE), (UDP_IP, UDP_PORT))
  sock.sendto(bytes.fromhex(SPAT_MESSAGE), (UDP_IP, UDP_PORT))
  sock.sendto(bytes.fromhex(PSM_MESSAGE), (UDP_IP, UDP_PORT))
  sock.sendto(bytes.fromhex(SSM_MESSAGE), (UDP_IP, UDP_PORT))
  sock.sendto(bytes.fromhex(SRM_MESSAGE), (UDP_IP, UDP_PORT))
  sock.sendto(bytes.fromhex(TIM_MESSAGE), (UDP_IP, UDP_PORT))
  sock.sendto(bytes.fromhex(BAD_MESSAGE), (UDP_IP, UDP_PORT))
