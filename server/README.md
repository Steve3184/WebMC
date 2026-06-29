# WebMC Server

A simple Node.js WebSocket server for WebMC multiplayer functionality.

## Installation

```bash
cd server
npm install
```

## Running the Server

```bash
# Default port (8080)
npm start

# Custom port
npm start 3000
```

## Features

- WebSocket-based real-time communication
- Player position synchronization
- Chat messaging
- Player join/leave notifications
- Server info broadcasting
- Connection health checking (heartbeat)

## Protocol

### Message Format

All messages are JSON strings with a `type` field:

#### Client to Server

```json
// Player join
{ "type": "player_join", "playerId": "uuid", "name": "PlayerName" }

// Position update
{ "type": "position", "playerId": "uuid", "x": 0, "y": 64, "z": 0, "yaw": 0, "pitch": 0, "onGround": true }

// Chat message
{ "type": "chat", "content": "Hello world!" }

// Ping
{ "type": "ping", "timestamp": 1234567890 }
```

#### Server to Client

```json
// Player joined
{ "type": "player_join", "playerId": "uuid", "name": "PlayerName", "x": 0, "y": 64, "z": 0 }

// Player left
{ "type": "player_leave", "playerId": "uuid", "name": "PlayerName" }

// Position update
{ "type": "position", "playerId": "uuid", "x": 0, "y": 64, "z": 0, "yaw": 0, "pitch": 0, "onGround": true }

// Chat message
{ "type": "chat", "sender": "PlayerName", "content": "Hello world!", "timestamp": 1234567890 }

// Server info
{ "type": "server_info", "motd": "Server Name", "version": "1.21.8", "playerCount": 1, "maxPlayers": 20 }

// Player list
{ "type": "player_list", "players": [...] }

// Pong
{ "type": "pong", "timestamp": 1234567890 }
```

## WebSocket URL

```
ws://localhost:8080/webmc
```

## Notes

- This is a basic reference implementation
- For production use, add authentication, encryption, and persistence
- Consider using a more robust server framework (Express, Fastify, etc.)
