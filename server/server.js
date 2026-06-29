/**
 * WebMC WebSocket Server
 * A simple Node.js WebSocket server for multiplayer functionality
 *
 * Usage:
 *   node server.js [port]
 *
 * Default port: 8080
 */

const WebSocket = require('ws');

// Default port
const PORT = process.argv[2] || 8080;

// Create WebSocket server
const wss = new WebSocket.Server({ port: PORT });

// Connected clients
const clients = new Map();

// Player data
const players = new Map();

// Chat history (last 100 messages)
const chatHistory = [];
const MAX_CHAT_HISTORY = 100;

// Server info
const serverInfo = {
  motd: 'WebMC Server',
  version: '1.21.8',
  maxPlayers: 20,
  playerCount: 0
};

console.log(`[WebMC Server] Started on port ${PORT}`);

// Handle new connections
wss.on('connection', (ws, req) => {
  const clientId = generateId();
  clients.set(clientId, ws);

  console.log(`[WebMC Server] Client connected: ${clientId}`);

  // Set up client handlers
  ws.clientId = clientId;
  ws.isAlive = true;

  // Ping/pong for connection health
  ws.on('pong', () => {
    ws.isAlive = true;
  });

  // Handle messages
  ws.on('message', (data) => {
    try {
      const message = parseMessage(data);
      if (!message) return;

      handleMessage(ws, message);
    } catch (err) {
      console.error(`[WebMC Server] Error handling message:`, err);
    }
  });

  // Handle close
  ws.on('close', (code, reason) => {
    console.log(`[WebMC Server] Client disconnected: ${clientId} (${code})`);

    // Remove player
    const player = players.get(clientId);
    if (player) {
      players.delete(clientId);
      serverInfo.playerCount--;

      // Broadcast player leave
      broadcast({
        type: 'player_leave',
        playerId: clientId,
        name: player.name
      });

      // Update server info
      broadcastServerInfo();
    }

    clients.delete(clientId);
  });

  // Handle errors
  ws.on('error', (err) => {
    console.error(`[WebMC Server] Client error: ${clientId}`, err);
  });
});

// Message handler
function handleMessage(ws, message) {
  const type = message.type;

  switch (type) {
    case 'ping':
      ws.send(JSON.stringify({
        type: 'pong',
        timestamp: message.timestamp
      }));
      break;

    case 'player_join':
      handlePlayerJoin(ws, message);
      break;

    case 'position':
      handlePositionUpdate(ws, message);
      break;

    case 'chat':
      handleChatMessage(ws, message);
      break;

    case 'server_info_request':
      ws.send(JSON.stringify({
        type: 'server_info',
        ...serverInfo
      }));
      break;

    case 'get_chat_history':
      // Send chat history to the requesting client
      ws.send(JSON.stringify({
        type: 'chat_history',
        messages: chatHistory.slice(-50) // Last 50 messages
      }));
      break;

    case 'chat_history':
      // Client sent chat history - this is handled on the client side
      // Could be used for synchronization if needed
      break;

    default:
      console.log(`[WebMC Server] Unknown message type: ${type}`);
  }
}

// Handle player join
function handlePlayerJoin(ws, message) {
  const playerId = message.playerId || ws.clientId;
  const name = message.name || `Player${Math.floor(Math.random() * 1000)}`;

  // Create player entry
  const player = {
    id: playerId,
    name: name,
    x: 0,
    y: 64,
    z: 0,
    yaw: 0,
    pitch: 0,
    onGround: true
  };

  players.set(ws.clientId, player);
  serverInfo.playerCount++;

  console.log(`[WebMC Server] Player joined: ${name} (${ws.clientId})`);

  // Send welcome message with existing players
  const existingPlayers = Array.from(players.values()).filter(p => p.id !== playerId);
  ws.send(JSON.stringify({
    type: 'player_list',
    players: existingPlayers
  }));

  // Broadcast new player to others
  broadcast({
    type: 'player_join',
    playerId: playerId,
    name: name,
    x: player.x,
    y: player.y,
    z: player.z
  }, ws);

  // Send system message about player join
  sendSystemMessage(name + ' joined the game');

  // Send chat history to the new player
  if (chatHistory.length > 0) {
    ws.send(JSON.stringify({
      type: 'chat_history',
      messages: chatHistory.slice(-50)
    }));
  }

  // Update server info
  broadcastServerInfo();
}

// Handle position update
function handlePositionUpdate(ws, message) {
  const player = players.get(ws.clientId);
  if (!player) return;

  // Update player position
  player.x = message.x;
  player.y = message.y;
  player.z = message.z;
  player.yaw = message.yaw;
  player.pitch = message.pitch;
  player.onGround = message.onGround;

  // Broadcast to other players
  broadcast({
    type: 'position',
    playerId: player.id,
    x: player.x,
    y: player.y,
    z: player.z,
    yaw: player.yaw,
    pitch: player.pitch,
    onGround: player.onGround
  }, ws);
}

// Handle chat message
function handleChatMessage(ws, message) {
  const player = players.get(ws.clientId);
  if (!player) return;

  const chatMessage = {
    type: 'chat',
    sender: player.name,
    content: message.content,
    timestamp: Date.now()
  };

  // Store in chat history
  chatHistory.push(chatMessage);
  if (chatHistory.length > MAX_CHAT_HISTORY) {
    chatHistory.shift();
  }

  // Broadcast to all players
  broadcast(chatMessage);

  console.log(`[Chat] ${player.name}: ${message.content}`);
}

// Send a system message to all players
function sendSystemMessage(content) {
  const systemMessage = {
    type: 'system',
    content: content,
    timestamp: Date.now()
  };

  chatHistory.push(systemMessage);
  if (chatHistory.length > MAX_CHAT_HISTORY) {
    chatHistory.shift();
  }

  broadcast(systemMessage);
}

// Broadcast message to all clients
function broadcast(message, exclude = null) {
  const data = JSON.stringify(message);

  clients.forEach((client, clientId) => {
    if (client !== exclude && client.readyState === WebSocket.OPEN) {
      client.send(data);
    }
  });
}

// Broadcast server info
function broadcastServerInfo() {
  const info = {
    type: 'server_info',
    motd: serverInfo.motd,
    version: serverInfo.version,
    playerCount: serverInfo.playerCount,
    maxPlayers: serverInfo.maxPlayers
  };

  broadcast(info);
}

// Parse message (handle binary or text)
function parseMessage(data) {
  if (Buffer.isBuffer(data)) {
    // Convert buffer to string
    data = data.toString('utf8');
  }

  if (typeof data === 'string') {
    try {
      return JSON.parse(data);
    } catch (err) {
      // Not JSON, return as raw text
      return { type: 'raw', content: data };
    }
  }

  return null;
}

// Generate unique ID
function generateId() {
  return Math.random().toString(36).substring(2, 15) +
         Math.random().toString(36).substring(2, 15);
}

// Heartbeat to detect dead connections
const heartbeatInterval = setInterval(() => {
  wss.clients.forEach((ws) => {
    if (ws.isAlive === false) {
      console.log(`[WebMC Server] Terminating dead connection: ${ws.clientId}`);
      return ws.terminate();
    }

    ws.isAlive = false;
    ws.ping();
  });
}, 30000);

wss.on('close', () => {
  clearInterval(heartbeatInterval);
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n[WebMC Server] Shutting down...');

  wss.clients.forEach((client) => {
    client.close(1001, 'Server shutting down');
  });

  wss.close(() => {
    console.log('[WebMC Server] Server closed');
    process.exit(0);
  });
});

console.log('[WebMC Server] Ready for connections');
console.log('[WebMC Server] WebSocket path: ws://localhost:' + PORT + '/webmc');