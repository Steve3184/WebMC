/**
 * WebMC BetaProxy-Style WebSocket Bridge Server
 *
 * Acts as a bridge between browser WebSocket clients and standard Minecraft servers.
 * Implements the betaProxy architecture for multiplayer connectivity.
 *
 * Usage:
 *   node beta-proxy-server.js [options]
 *
 * Options:
 *   --ws-port <port>    WebSocket server port (default: 8080)
 *   --mc-host <host>    Minecraft server host (default: localhost)
 *   --mc-port <port>    Minecraft server port (default: 25565)
 *   --max-clients <n>   Maximum concurrent clients (default: 100)
 *   --rate-limit <n>    Max messages per second per client (default: 20)
 *   --heartbeat <ms>    Heartbeat interval in ms (default: 30000)
 */

const WebSocket = require('ws');
const net = require('net');
const http = require('http');
const crypto = require('crypto');

// Command line arguments
const args = process.argv.slice(2).reduce((acc, arg, i, arr) => {
  if (arg.startsWith('--')) {
    const key = arg.slice(2);
    const next = arr[i + 1];
    if (next && !next.startsWith('--')) {
      acc[key] = isNaN(next) ? next : Number(next);
    } else {
      acc[key] = true;
    }
  }
  return acc;
}, {});

// Configuration
const CONFIG = {
  wsPort: args['ws-port'] || 8080,
  mcHost: args['mc-host'] || 'localhost',
  mcPort: args['mc-port'] || 25565,
  maxClients: args['max-clients'] || 100,
  rateLimit: args['rate-limit'] || 20,
  heartbeatInterval: args['heartbeat'] || 30000,
  connectionTimeout: 60000,
  reconnectDelay: 3000,
  maxReconnectAttempts: 5
};

// State
const clients = new Map();
const mcServers = new Map();
const serverInfo = {
  motd: 'WebMC Proxy',
  version: '1.21.8',
  maxPlayers: CONFIG.maxClients,
  playerCount: 0
};

// Rate limiting state
const rateLimits = new Map();

console.log('===========================================');
console.log('  WebMC BetaProxy-Style Bridge Server');
console.log('===========================================');
console.log(`  WebSocket Port: ${CONFIG.wsPort}`);
console.log(`  Minecraft Host: ${CONFIG.mcHost}:${CONFIG.mcPort}`);
console.log(`  Max Clients: ${CONFIG.maxClients}`);
console.log(`  Rate Limit: ${CONFIG.rateLimit} msg/sec`);
console.log('===========================================');

// Create HTTP server for health checks and stats
const httpServer = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      clients: clients.size,
      connectedServers: mcServers.size,
      uptime: process.uptime()
    }));
  } else if (req.url === '/stats') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      ...serverInfo,
      connectedClients: clients.size,
      activeServers: Array.from(mcServers.entries()).map(([id, s]) => ({
        id,
        host: s.host,
        port: s.port,
        connected: s.socket && !s.socket.destroyed,
        players: s.players?.size || 0
      }))
    }));
  } else {
    res.writeHead(404);
    res.end('Not Found');
  }
});

// Create WebSocket server
const wss = new WebSocket.Server({ server: httpServer });

// ==================== CLIENT MANAGEMENT ====================

/**
 * Create a new browser client connection
 */
function createClientConnection(ws, req) {
  if (clients.size >= CONFIG.maxClients) {
    ws.close(1008, 'Server at capacity');
    console.log('[Client] Rejected: server at capacity');
    return null;
  }

  const clientId = generateClientId();
  const client = {
    id: clientId,
    ws,
    socket: null,  // Minecraft server connection when bridged
    playerData: null,
    state: 'handshake', // handshake, status, login, play, disconnecting
    connectedAt: Date.now(),
    lastActivity: Date.now(),
    messageCount: 0,
    reconnectAttempts: 0,
    serverId: null  // Which MC server this client is connected to
  };

  clients.set(clientId, client);
  rateLimits.set(clientId, { count: 0, resetAt: Date.now() + 1000 });

  console.log(`[Client] Connected: ${clientId} from ${req.socket.remoteAddress}`);

  return client;
}

/**
 * Handle client disconnection
 */
function disconnectClient(clientId, reason = 'unknown') {
  const client = clients.get(clientId);
  if (!client) return;

  console.log(`[Client] Disconnected: ${clientId} (${reason})`);

  // Close Minecraft server connection if exists
  if (client.socket && !client.socket.destroyed) {
    client.socket.destroy();
  }

  // Notify server if connected
  if (client.serverId && mcServers.has(client.serverId)) {
    const server = mcServers.get(client.serverId);
    server.players.delete(clientId);
    broadcastToServer(client.serverId, {
      type: 'player_leave',
      playerId: clientId,
      name: client.playerData?.name || 'Unknown'
    });
  }

  // Broadcast to other clients
  broadcast({
    type: 'player_leave',
    playerId: clientId,
    name: client.playerData?.name || 'Unknown'
  }, clientId);

  clients.delete(clientId);
  rateLimits.delete(clientId);
}

// ==================== MINECRAFT SERVER BRIDGE ====================

/**
 * Connect to a Minecraft server and bridge to client
 */
async function bridgeToMinecraftServer(client, host, port) {
  if (client.socket && !client.socket.destroyed) {
    client.socket.destroy();
  }

  return new Promise((resolve, reject) => {
    const socket = net.createConnection({
      host: host,
      port: port,
      timeout: 10000
    }, () => {
      console.log(`[Bridge] Connected to MC server ${host}:${port} for client ${client.id}`);
      client.socket = socket;
      client.serverId = `${host}:${port}`;
      client.state = 'handshake';
      resolve(true);
    });

    socket.on('error', (err) => {
      console.error(`[Bridge] MC server error for client ${client.id}:`, err.message);
      client.ws.send(JSON.stringify({
        type: 'error',
        message: `Cannot connect to server: ${err.message}`
      }));
      reject(err);
    });

    socket.on('timeout', () => {
      console.error(`[Bridge] Connection timeout for client ${client.id}`);
      reject(new Error('Connection timeout'));
    });

    socket.on('close', () => {
      console.log(`[Bridge] MC server connection closed for client ${client.id}`);
      if (client.state !== 'disconnecting') {
        client.ws.send(JSON.stringify({
          type: 'server_disconnect',
          message: 'Lost connection to server'
        }));
      }
    });

    // Forward MC server data to WebSocket client
    socket.on('data', (data) => {
      try {
        handleMCServerData(client, data);
      } catch (err) {
        console.error(`[Bridge] Error handling MC data for ${client.id}:`, err);
      }
    });
  });
}

/**
 * Handle data from Minecraft server
 */
function handleMCServerData(client, data) {
  client.lastActivity = Date.now();

  // Wrap raw MC protocol data for browser
  const packet = {
    type: 'mc_packet',
    clientId: client.id,
    timestamp: Date.now(),
    data: data.toString('base64')  // Base64 encode binary MC protocol data
  };

  // Forward to browser client
  if (client.ws.readyState === WebSocket.OPEN) {
    client.ws.send(JSON.stringify(packet));
  }
}

/**
 * Send data to Minecraft server
 */
function sendToMinecraftServer(client, data) {
  if (!client.socket || client.socket.destroyed) {
    console.warn(`[Bridge] Cannot send: no MC connection for client ${client.id}`);
    return false;
  }

  try {
    // Decode base64 if needed
    const payload = typeof data === 'string' ? Buffer.from(data, 'base64') : data;
    client.socket.write(payload);
    client.lastActivity = Date.now();
    return true;
  } catch (err) {
    console.error(`[Bridge] Error sending to MC server:`, err);
    return false;
  }
}

/**
 * Broadcast message to all clients connected to a specific MC server
 */
function broadcastToServer(serverId, message) {
  const data = JSON.stringify(message);
  clients.forEach((client) => {
    if (client.serverId === serverId && client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(data);
    }
  });
}

// ==================== MESSAGE HANDLING ====================

/**
 * Handle incoming WebSocket message from browser client
 */
function handleClientMessage(client, rawData) {
  // Rate limiting
  if (!checkRateLimit(client.id)) {
    client.ws.send(JSON.stringify({
      type: 'error',
      message: 'Rate limit exceeded'
    }));
    return;
  }

  client.lastActivity = Date.now();

  let message;
  try {
    message = parseMessage(rawData);
  } catch (err) {
    console.error(`[Client] Failed to parse message from ${client.id}:`, err.message);
    return;
  }

  if (!message || !message.type) return;

  switch (message.type) {
    case 'ping':
      client.ws.send(JSON.stringify({
        type: 'pong',
        timestamp: message.timestamp || Date.now()
      }));
      break;

    case 'player_join':
      handlePlayerJoin(client, message);
      break;

    case 'position':
      handlePositionUpdate(client, message);
      break;

    case 'chat':
      handleChatMessage(client, message);
      break;

    case 'mc_connect':
      handleMCConnect(client, message);
      break;

    case 'mc_packet':
      handleMCPacket(client, message);
      break;

    case 'mc_disconnect':
      handleMCDisconnect(client);
      break;

    case 'server_info_request':
      client.ws.send(JSON.stringify({
        type: 'server_info',
        ...serverInfo,
        connectedClients: clients.size
      }));
      break;

    case 'list_players':
      handleListPlayers(client);
      break;

    case 'direct_connect':
      handleDirectConnect(client, message);
      break;

    default:
      console.log(`[Client] Unknown message type: ${message.type}`);
  }
}

/**
 * Handle player join (local mode)
 */
function handlePlayerJoin(client, message) {
  client.playerData = {
    id: client.id,
    name: message.name || `Player${Math.floor(Math.random() * 1000)}`,
    x: message.x || 0,
    y: message.y || 64,
    z: message.z || 0,
    yaw: message.yaw || 0,
    pitch: message.pitch || 0,
    onGround: message.onGround ?? true
  };

  console.log(`[Player] Joined: ${client.playerData.name} (${client.id})`);

  // Send existing players
  const existingPlayers = Array.from(clients.values())
    .filter(c => c.id !== client.id && c.playerData)
    .map(c => c.playerData);

  client.ws.send(JSON.stringify({
    type: 'player_list',
    players: existingPlayers
  }));

  // Broadcast join to others
  broadcast({
    type: 'player_join',
    ...client.playerData
  }, client.id);

  // Send chat history
  if (chatHistory.length > 0) {
    client.ws.send(JSON.stringify({
      type: 'chat_history',
      messages: chatHistory.slice(-50)
    }));
  }
}

/**
 * Handle position update
 */
function handlePositionUpdate(client, message) {
  if (!client.playerData) return;

  client.playerData.x = message.x ?? client.playerData.x;
  client.playerData.y = message.y ?? client.playerData.y;
  client.playerData.z = message.z ?? client.playerData.z;
  client.playerData.yaw = message.yaw ?? client.playerData.yaw;
  client.playerData.pitch = message.pitch ?? client.playerData.pitch;
  client.playerData.onGround = message.onGround ?? client.playerData.onGround;

  broadcast({
    type: 'position',
    playerId: client.id,
    ...client.playerData
  }, client.id);
}

/**
 * Handle chat message
 */
function handleChatMessage(client, message) {
  if (!client.playerData) return;

  const chatMessage = {
    type: 'chat',
    sender: client.playerData.name,
    content: message.content,
    timestamp: Date.now()
  };

  chatHistory.push(chatMessage);
  if (chatHistory.length > 100) chatHistory.shift();

  broadcast(chatMessage);
  console.log(`[Chat] ${client.playerData.name}: ${message.content}`);
}

/**
 * Handle MC server connection request
 */
async function handleMCConnect(client, message) {
  const host = message.host || CONFIG.mcHost;
  const port = message.port || CONFIG.mcPort;

  console.log(`[Bridge] Client ${client.id} connecting to MC server ${host}:${port}`);

  client.ws.send(JSON.stringify({
    type: 'mc_connecting',
    host,
    port
  }));

  try {
    await bridgeToMinecraftServer(client, host, port);
    client.ws.send(JSON.stringify({
      type: 'mc_connected',
      host,
      port
    }));
  } catch (err) {
    // Error already sent in bridge function
    console.error(`[Bridge] Failed to connect client ${client.id} to MC server`);
  }
}

/**
 * Handle raw MC packet from client
 */
function handleMCPacket(client, message) {
  if (!client.socket || client.socket.destroyed) {
    client.ws.send(JSON.stringify({
      type: 'error',
      message: 'Not connected to MC server'
    }));
    return;
  }

  sendToMinecraftServer(client, message.data);
}

/**
 * Handle MC disconnect request
 */
function handleMCDisconnect(client) {
  if (client.socket && !client.socket.destroyed) {
    client.socket.destroy();
    client.socket = null;
    client.serverId = null;
    client.state = 'disconnected';

    client.ws.send(JSON.stringify({
      type: 'mc_disconnected'
    }));
  }
}

/**
 * Handle direct server connection (bypassing local mode)
 */
function handleDirectConnect(client, message) {
  // Full bridge mode - no local state, pure MC proxy
  client.state = 'bridged';

  const host = message.host || CONFIG.mcHost;
  const port = message.port || CONFIG.mcPort;

  bridgeToMinecraftServer(client, host, port)
    .then(() => {
      client.ws.send(JSON.stringify({
        type: 'direct_connected',
        host,
        port
      }));
    })
    .catch(() => {
      // Error handled in bridge
    });
}

/**
 * Handle list players request
 */
function handleListPlayers(client) {
  const players = Array.from(clients.values())
    .filter(c => c.playerData)
    .map(c => ({
      id: c.id,
      name: c.playerData.name,
      server: c.serverId || 'local'
    }));

  client.ws.send(JSON.stringify({
    type: 'player_list_detailed',
    players
  }));
}

// ==================== UTILITIES ====================

/**
 * Rate limiting check
 */
function checkRateLimit(clientId) {
  const now = Date.now();
  let limit = rateLimits.get(clientId);

  if (!limit || now > limit.resetAt) {
    limit = { count: 0, resetAt: now + 1000 };
    rateLimits.set(clientId, limit);
  }

  limit.count++;
  return limit.count <= CONFIG.rateLimit;
}

/**
 * Broadcast message to all clients
 */
function broadcast(message, excludeId = null) {
  const data = JSON.stringify(message);
  clients.forEach((client, id) => {
    if (id !== excludeId && client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(data);
    }
  });
}

/**
 * Parse incoming message
 */
function parseMessage(data) {
  if (Buffer.isBuffer(data)) {
    data = data.toString('utf8');
  }
  if (typeof data === 'string') {
    try {
      return JSON.parse(data);
    } catch {
      return { type: 'raw', content: data };
    }
  }
  return null;
}

/**
 * Generate unique client ID
 */
function generateClientId() {
  return 'c_' + crypto.randomBytes(8).toString('hex');
}

/**
 * Cleanup inactive clients
 */
function cleanupInactiveClients() {
  const now = Date.now();
  const timeout = CONFIG.connectionTimeout;

  clients.forEach((client, id) => {
    if (now - client.lastActivity > timeout) {
      console.log(`[Cleanup] Removing inactive client: ${id}`);
      disconnectClient(id, 'inactivity timeout');
    }
  });
}

// Chat history
const chatHistory = [];

// ==================== WEBSOCKET SERVER ====================

wss.on('connection', (ws, req) => {
  const client = createClientConnection(ws, req);
  if (!client) return;

  ws.on('message', (data) => {
    handleClientMessage(client, data);
  });

  ws.on('close', (code, reason) => {
    disconnectClient(client.id, `code: ${code}`);
  });

  ws.on('error', (err) => {
    console.error(`[Client] WebSocket error for ${client.id}:`, err.message);
  });

  // Send welcome
  ws.send(JSON.stringify({
    type: 'connected',
    clientId: client.id,
    serverTime: Date.now()
  }));
});

wss.on('error', (err) => {
  console.error('[Server] WebSocket server error:', err);
});

// ==================== HEARTBEAT SYSTEM ====================

const heartbeatInterval = setInterval(() => {
  let activeClients = 0;
  let activeMCConnections = 0;

  clients.forEach((client, id) => {
    if (ws.isAlive === false) {
      disconnectClient(id, 'heartbeat timeout');
      return;
    }

    // Ping WebSocket client
    if (client.ws.readyState === WebSocket.OPEN) {
      client.ws.isAlive = false;
      client.ws.ping();
    }

    activeClients++;

    // Check MC connection
    if (client.socket && !client.socket.destroyed) {
      activeMCConnections++;
    }
  });

  // Cleanup inactive
  cleanupInactiveClients();

  console.log(`[Heartbeat] Clients: ${clients.size}, MC Bridges: ${activeMCConnections}`);
}, CONFIG.heartbeatInterval);

// Handle pong responses
wss.on('connection', (ws) => {
  ws.on('pong', () => {
    ws.isAlive = true;
  });
});

// ==================== GRACEFUL SHUTDOWN ====================

process.on('SIGINT', () => {
  console.log('\n[Server] Shutting down...');

  clearInterval(heartbeatInterval);

  // Close all client connections
  clients.forEach((client, id) => {
    if (client.ws.readyState === WebSocket.OPEN) {
      client.ws.close(1001, 'Server shutting down');
    }
    if (client.socket && !client.socket.destroyed) {
      client.socket.destroy();
    }
  });

  wss.close(() => {
    httpServer.close(() => {
      console.log('[Server] Server closed');
      process.exit(0);
    });
  });
});

process.on('uncaughtException', (err) => {
  console.error('[Server] Uncaught exception:', err);
});

// Start server
httpServer.listen(CONFIG.wsPort, () => {
  console.log(`[Server] WebSocket server listening on port ${CONFIG.wsPort}`);
  console.log(`[Server] Health check: http://localhost:${CONFIG.wsPort}/health`);
  console.log(`[Server] Stats: http://localhost:${CONFIG.wsPort}/stats`);
  console.log('[Server] Ready for connections');
});