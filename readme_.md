# Microservices Cluster - Setup Guide

## Prerequisites
- Java 17+
- Maven 3.6+
- VS Code with Java Extension Pack

---

## Quick Setup

### 1. Clone Repository
```bash
git clone https://github.com/YOUR-USERNAME/microcluster.git
cd microcluster
```

### 2. Build Project
```bash
mvn clean package
```

This creates JARs in `target/`:
- `microcluster-1.0-SNAPSHOT.jar` (all-in-one JAR)

---

## Running Locally

### Start Router
```bash
java -cp target/microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.Router
```

### Start Service Nodes
```bash
# Terminal 2 - BASE64
java -cp target/microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.ServiceNode localhost 9000 BASE64 8080

# Terminal 3 - CSV
java -cp target/microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.ServiceNode localhost 9000 CSV 8081

# Terminal 4 - GZIP
java -cp target/microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.ServiceNode localhost 9000 GZIP 8082

# Terminal 5 - HMAC
java -cp target/microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.ServiceNode localhost 9000 HMAC 8083

# Terminal 6 - ENTROPY
java -cp target/microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.ServiceNode localhost 9000 ENTROPY 8084
```

### Start Client
```bash
# Terminal 7
java -cp target/microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.client.Client localhost 5000
```

---

## VS Code Setup

### 1. Install Extensions
- Extension Pack for Java (Microsoft)
- Maven for Java

### 2. Open Project
```
File → Open Folder → Select microcluster/
```

### 3. Build
- Press `Ctrl+Shift+P`
- Type: `Java: Clean Java Language Server Workspace`
- Press `Ctrl+Shift+P`
- Type: `Maven: Execute Commands` → `package`

### 4. Run Components

**Router:**
1. Open `Router.java`
2. Click "Run" above `main()` method
3. Or press `F5`

**ServiceNode:**
1. Open `ServiceNode.java`
2. Click "Run" → "Run Without Debugging"
3. When prompted for args: `localhost 9000 BASE64 8080`
4. Repeat for other services (change name/port)

**Client:**
1. Open `Client.java`
2. Click "Run"
3. When prompted for args: `localhost 5000`

---

## Cloud Deployment

### Upload JAR
```bash
# Copy JAR to cloud instance
scp target/microcluster-1.0-SNAPSHOT.jar user@instance-ip:~/
```

### Run on Cloud

**Router (GCP):**
```bash
java -cp microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.Router
```

**ServiceNode (AWS/GCP):**
```bash
java -cp microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.core.ServiceNode <ROUTER_IP> 9000 BASE64 8080
```

**Client (Local):**
```bash
java -cp microcluster-1.0-SNAPSHOT.jar edu.qu.microcluster.client.Client <ROUTER_IP> 5000
```

---

## Firewall Rules

**Router:**
- UDP 9000 (heartbeats)
- TCP 5000 (clients)

**ServiceNodes:**
- TCP 8080-8084 (from Router IP)

---

## Troubleshooting

**"mvn: command not found"**
- Use VS Code's Maven extension instead
- Or install Maven: https://maven.apache.org/install.html

**"Port already in use"**
```bash
# Find process
lsof -i :5000
# Kill it
kill <PID>
```

**"Connection refused"**
- Check Router is running first
- Verify IP address and port
- Check firewall allows connection

---

## Quick Test

1. Start Router
2. Start 1+ ServiceNodes (wait 15 seconds for heartbeat)
3. Start Client
4. Choose option 1 → Should list services
5. Choose option 2 → Test a service