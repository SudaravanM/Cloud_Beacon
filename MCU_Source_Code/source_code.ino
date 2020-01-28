#include <ESP8266WiFi.h>        // Include the Wi-Fi library

WiFiServer server(8090);

int f = 0;
const char *ssid = "ESP8266 Access Point"; // The name of the Wi-Fi network that will be created
const char *password = "12345678";   // The password required to connect to it, leave blank for an open network

IPAddress local_IP(192,168,4,1);
IPAddress gateway(192,168,4,22);
IPAddress subnet(255,255,255,0);

void setup() {
  Serial.begin(115200);
  delay(5000);
  Serial.println('\n');

  WiFi.softAPConfig(local_IP, gateway, subnet);
  
  WiFi.softAP(ssid, password);             // Start the access point
  Serial.print("Access Point \"");
  Serial.print(ssid);
  Serial.println("\" started");

  Serial.print("IP address:\t");
  Serial.println(WiFi.softAPIP());         // Send the IP address of the ESP8266 to the computer
  Serial.print("\nwaiting");
  while (WiFi.softAPgetStationNum()==0) {
    delay(100);
    Serial.print(".");
  }
  Serial.println("connected");
  server.begin();
}

void loop() { 
  WiFiClient client = server.available();
  if (!client) {
    return;
  }
  WiFi.softAP(client.readStringUntil('$'), password); 

 }
