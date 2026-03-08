Here is a **short, clean README in simple human language** you can paste directly.

````markdown
# Microservice Cluster – Distributed Systems Project

This project implements a simple distributed microservice system using Java.

The system has three main components:

Client → Router → Service Nodes

The client sends requests to the router.  
The router discovers service nodes using UDP heartbeat messages and forwards the request to the correct service node using TCP.  
Service nodes process the request and send the result back through the router to the client.

---

## Services Implemented

BASE64  
CSV  
GZIP  
HMAC  
ENTROPY

Each service runs on its own TCP port.

---

## Technologies Used

Java  
Maven  
TCP Sockets  
UDP Datagram (Heartbeat)  
AWS EC2  
Google Cloud VM  

---

## Ports Used

Router TCP: 5050  
Router UDP (heartbeat): 9000  

BASE64: 5001  
CSV: 5002  
GZIP: 5003  
HMAC: 5004  
ENTROPY: 5005  

---

## Setup

Clone the repository:

```
git clone <REPO_URL>
cd microcluster
```

Build the project:

```
mvn clean package
```

Make sure Java and Maven are installed.

---

## Start Router

Run this on the Router machine (AWS):

```
mvn exec:java -Dexec.mainClass="edu.qu.microcluster.core.Router"
```

Router starts:

- TCP server on port 5050
- UDP heartbeat listener on port 9000

---

## Start Service Nodes

Run one service per machine.

Example commands:

BASE64
```
mvn exec:java -Dexec.mainClass="edu.qu.microcluster.core.ServiceNode" -Dexec.args="<ROUTER_IP> 9000 BASE64 5001"
```

CSV
```
mvn exec:java -Dexec.mainClass="edu.qu.microcluster.core.ServiceNode" -Dexec.args="<ROUTER_IP> 9000 CSV 5002"
```

GZIP
```
mvn exec:java -Dexec.mainClass="edu.qu.microcluster.core.ServiceNode" -Dexec.args="<ROUTER_IP> 9000 GZIP 5003"
```

HMAC
```
mvn exec:java -Dexec.mainClass="edu.qu.microcluster.core.ServiceNode" -Dexec.args="<ROUTER_IP> 9000 HMAC 5004"
```

ENTROPY
```
mvn exec:java -Dexec.mainClass="edu.qu.microcluster.core.ServiceNode" -Dexec.args="<ROUTER_IP> 9000 ENTROPY 5005"
```

Service nodes will automatically register with the router using heartbeat messages.

---

## Run Client

Run the client from your local machine:

```
mvn exec:java -Dexec.mainClass="edu.qu.microcluster.client.Client" -Dexec.args="<ROUTER_IP> 5050"
```

The client connects to the router and allows you to run the services.

---

## Deployment

Router → AWS EC2  
Service Nodes → AWS EC2 + Google Cloud VMs  
Client → Local machine  

This demonstrates a multi-cloud distributed microservice system.

---

## Notes

Start the router first.  
Then start all service nodes.  
Finally run the client.

Make sure firewall rules allow the required ports.
````
