---

# PeerDrop

A cross-platform desktop application that enables instant peer-to-peer (P2P) messaging and file sharing across a Local Area Network (LAN).

**Note on Project Purpose:** While local P2P applications are a well-explored domain, this project was deliberately built from scratch to demonstrate a fundamental, hands-on implementation of network programming (TCP/UDP), application design pattern (MVVM), resource management and concurrency. It showcases the ability to manage state, design application protocols, and bridge low-level network sockets with a modern graphical interface.

---

## 🎯 High-Level Summary

This application allows users connected to the same Wi-Fi or wired network to automatically discover each other, chat, and share files without routing traffic through the public internet or external servers.

### The Tech Stack

* **Language:** Java
* **UI Framework:** JavaFX
* **Architecture:** MVVM (Model-View-ViewModel)
* **Networking:** Standard Java Networking (Sockets, Datagrams, internal HTTP server)
* **Packaging:** `jpackage` (Native binaries for Windows)

---


## 📸 Preview

### Connection Screen
*Join a session by entering your handle.*

![Screenshot of the connection screen, featuring a text input field for the session handle and a 'Connect' button](/assets/images/Gateway.PNG)

<br>

### Main Chat Interface
*The core communication hub, featuring real-time messaging.*

![Screenshot of the main interface, displaying a central chat area with message history, a text input field, and buttons for sending or attaching files. A side panel shows the active peer list, alongside 'Leave' and 'Settings' buttons](/assets/images/CentralHub-peer-a1.PNG)

<br>

### Settings Page
*Change network configurations.*

![Screenshot of the settings page, illustrating various network configuration options](/assets/images/Settings1(Redacted).png)

---

## 🧠 How It Works: The Bridge

To make the system reliable and efficient, the application uses three different networking protocols, each chosen for a specific job:

1. **Finding Peers (UDP Multicast):** Instead of requiring users to enter IP addresses, the app uses UDP Multicast. Think of this as walking into a room and announcing, "I'm here!" Everyone else using the app in that "room" (the local network) hears the announcement, registers the new user, and says "Hello" back.
2. **Sending Messages (TCP):** Once peers know about each other, text messages are sent using TCP. TCP guarantees that the message arrives exactly as it was sent, without missing pieces.
3. **Sharing Files (HTTP):** Instead of pushing a heavy file over a standard socket, the app handles files like a website does. The sender briefly turns into a mini web-server, tells the recipient exactly where to "download" the file, and shuts the server down as soon as the transfer is complete.

---

## ⚙️ Architecture & Technical Decisions

### Design Pattern: MVVM

The application strictly follows the **Model-View-ViewModel (MVVM)** design pattern to decouple the visual interface, application layer and service layer.

* **View (JavaFX):** Handles only user inputs and rendering.
* **ViewModel:** Bridges the UI and the core logic, using Java/javafx properties and observables to store current application state and reactively update the UI.
* **Model:** application layer and service layers, Data.

### Networking Implementation Details

* **Discovery (UDP Multicast):** Peers listen on a specific multicast group. When a peer boots, it broadcasts a heartbeat payload containing its username and listening ports.
* **Messaging (Ephemeral TCP & Framing):**
* Connections are *ephemeral*--a new TCP socket is opened for each message and immediately closed.
* **Framing Protocol:** The app uses length-prefixed framing for messaging.--Every connection begins with a fixed-length header that specifies the exact byte array length of the incoming message. The receiver reads this header, allocates the exact buffer size needed, and reads the payload.


* **File Sharing (HTTP Transfer):**
* Files are not pushed; they are pulled.
* **The Flow:** `Sender hosts file` → `Sender sends a 'file offer' TCP message with a token and file metadata` → `Recipient accepts and issues an HTTP GET request` → `File downloads` → `Sender revokes the HTTP endpoint`.
* **Memory Management:** Files aren't loaded into memory (large files would cause OutOfMemory error). Both sending and receiving sides utilize a buffer bucket. Recipient writes directly to disk. 



---

## ⚖️ Trade-offs & Future Considerations

No system is perfect. Here are the deliberate trade-offs made during development and what I would prioritize next:

* **Ephemeral vs. Persistent TCP:** Spinning up a new TCP socket per message creates slight network overhead (the TCP 3-way handshake). However, it drastically simplifies state management. By not keeping long-lived TCP connections open, the app doesn't have to handle complex socket-drop detection or connection pooling.
* **Security (Lack of E2EE):** Traffic is currently sent in plain text. Because the app is strictly LAN-based, the physical network acts as the security boundary. For a production release, implementing TLS/SSL over the sockets would be the next step to prevent packet sniffing by other users on the same Wi-Fi.
* **Scope Limitation (No NAT Traversal):** The application relies on local broadcast domains. It does not implement STUN/TURN servers for cross-internet communication, keeping the infrastructure requirements strictly to zero.

---

## 🚀 Getting Started

### Option 1: Run the Pre-Packaged Release (Easiest)

You do not need Java installed to run the pre-packaged versions.
** Note: There's only releases for windows at this time, Linux and macOS users please [build from source] (#option-2-build-from-source).

1. Navigate to the [Releases](https://github.com/thefloat/peer-drop/releases/latest) tab on this repository.
2. **Windows:** Download the `PeerDrop.zip` file.
3. Extract the ZIP file and run `PeerDrop.exe` (no installation required). 
   *Ensure your firewall allows the application to communicate on private networks.*

### Option 2: Build from Source

If you want to review the code and run it locally or are on Linux or macOS:

**Prerequisites:**

* JDK 17 or higher
* Gradle

```shell
# 1. Clone the repository
git clone https://github.com/thefloat/peer-drop.git

# 2. Navigate into the directory
cd peer-drop

```

Linux and macOS:
```shell
# 3. Compile and run via your build tool
./gradlew.bat run

```

PowerShell:
```shell
# 3. Compile and run via your build tool
.\gradlew run

```

cmd:
```shell
# 3. Compile and run via your build tool
.\gradlew.bat run

```

To test locally, you can run two instances of the application on the same machine.

---