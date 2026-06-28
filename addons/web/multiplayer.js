/**
 * WebMC Multiplayer UI Handler
 * Manages multiplayer menu, server list, and connection UI
 */
(function () {
  'use strict';

  // Server list data (would be persisted in real implementation)
  var serverList = [
    { id: 'local', address: 'localhost:8080', name: 'Localhost Demo', status: 'offline', ping: -1, players: '0/10' }
  ];

  var selectedServer = null;
  var multiplayerMenu = null;
  var serverListEl = null;
  var directConnectDialog = null;
  var connectionStatus = null;

  // DOM Elements
  var elements = {};

  /**
   * Initialize multiplayer UI
   */
  function init() {
    // Get DOM elements
    elements.multiplayerMenu = document.getElementById('multiplayer-menu');
    elements.serverList = document.getElementById('server-list');
    elements.directConnect = document.getElementById('direct-connect');
    elements.dcAddress = document.getElementById('dc-address');
    elements.connectionStatus = document.getElementById('connection-status');

    // Button elements
    elements.btnDirectConnect = document.getElementById('btn-direct-connect');
    elements.btnAddServer = document.getElementById('btn-add-server');
    elements.btnRefresh = document.getElementById('btn-refresh');
    elements.btnJoin = document.getElementById('btn-join');
    elements.btnBack = document.getElementById('btn-back');
    elements.dcConnect = document.getElementById('dc-connect');
    elements.dcCancel = document.getElementById('dc-cancel');

    // Attach event listeners
    attachEventListeners();

    // Set up WebMC event listeners
    setupWebMCListeners();

    console.log('[WebMC/Multiplayer] UI initialized');
  }

  /**
   * Attach button event listeners
   */
  function attachEventListeners() {
    if (elements.btnDirectConnect) {
      elements.btnDirectConnect.addEventListener('click', showDirectConnect);
    }
    if (elements.btnAddServer) {
      elements.btnAddServer.addEventListener('click', promptAddServer);
    }
    if (elements.btnRefresh) {
      elements.btnRefresh.addEventListener('click', refreshServers);
    }
    if (elements.btnJoin) {
      elements.btnJoin.addEventListener('click', joinSelectedServer);
    }
    if (elements.btnBack) {
      elements.btnBack.addEventListener('click', hideMultiplayerMenu);
    }
    if (elements.dcConnect) {
      elements.dcConnect.addEventListener('click', connectDirect);
    }
    if (elements.dcCancel) {
      elements.dcCancel.addEventListener('click', hideDirectConnect);
    }
    if (elements.dcAddress) {
      elements.dcAddress.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') connectDirect();
        if (e.key === 'Escape') hideDirectConnect();
      });
    }
  }

  /**
   * Set up WebMC event listeners for multiplayer events
   */
  function setupWebMCListeners() {
    if (window.WebMC) {
      WebMC.addEventListener('playerJoin', function(data) {
        addChatMessage(data.name + ' joined the game', 'info');
      });

      WebMC.addEventListener('playerLeave', function(data) {
        addChatMessage(data.playerId + ' left the game', 'info');
      });

      WebMC.addEventListener('chatMessage', function(data) {
        // Add to chat with sender highlighted
        if (window.__webmcAddChatMessage) {
          window.__webmcAddChatMessage(data.content, 'chat', data.sender);
        } else {
          addChatMessage('<' + data.sender + '> ' + data.content, 'chat');
        }
      });

      WebMC.addEventListener('systemMessage', function(data) {
        // Add system message
        if (window.__webmcAddChatMessage) {
          window.__webmcAddChatMessage(data.content, 'system', null);
        } else {
          addChatMessage(data.content, 'system');
        }
      });

      // Handle chat history on connection
      WebMC.addEventListener('chatHistory', function(data) {
        if (data.messages && Array.isArray(data.messages)) {
          console.log('[WebMC/Multiplayer] Loading chat history:', data.messages.length, 'messages');
          data.messages.forEach(function(msg) {
            if (msg.type === 'chat' && msg.sender && msg.content) {
              if (window.__webmcAddChatMessage) {
                window.__webmcAddChatMessage(msg.content, 'chat', msg.sender);
              }
            } else if (msg.type === 'system' && msg.content) {
              if (window.__webmcAddChatMessage) {
                window.__webmcAddChatMessage(msg.content, 'system', null);
              }
            }
          });
        }
      });

      WebMC.addEventListener('serverInfo', function(data) {
        console.log('[WebMC/Multiplayer] Server info:', data);
      });

      WebMC.addEventListener('positionUpdate', function(data) {
        // Handle position updates from other players
        // This would be forwarded to the game engine
      });
    }
  }

  /**
   * Show multiplayer menu
   */
  function showMultiplayerMenu() {
    if (!elements.multiplayerMenu) return;
    elements.multiplayerMenu.classList.add('show');
    renderServerList();
  }

  /**
   * Hide multiplayer menu
   */
  function hideMultiplayerMenu() {
    if (!elements.multiplayerMenu) return;
    elements.multiplayerMenu.classList.remove('show');
    hideDirectConnect();
  }

  /**
   * Render server list
   */
  function renderServerList() {
    if (!elements.serverList) return;

    elements.serverList.innerHTML = '';

    serverList.forEach(function(server) {
      var item = document.createElement('li');
      item.className = 'server-item' + (selectedServer === server ? ' selected' : '');
      item.dataset.id = server.id;

      var statusClass = server.status === 'online' ? 'online' : (server.status === 'pending' ? 'pending' : 'offline');
      var pingClass = server.ping < 0 ? 'none' : (server.ping < 100 ? 'good' : (server.ping < 300 ? 'medium' : 'bad'));
      var pingText = server.ping < 0 ? '--' : server.ping + 'ms';

      item.innerHTML =
        '<div class="status ' + statusClass + '"></div>' +
        '<div class="info">' +
          '<div class="name">' + escapeHtml(server.name) + '</div>' +
          '<div class="address">' + escapeHtml(server.address) + '</div>' +
          '<div class="players">' + (server.players || '0/10') + ' players</div>' +
        '</div>' +
        '<div class="ping ' + pingClass + '">' + pingText + '</div>';

      item.addEventListener('click', function() {
        selectServer(server);
      });

      item.addEventListener('dblclick', function() {
        selectServer(server);
        joinSelectedServer();
      });

      elements.serverList.appendChild(item);
    });
  }

  /**
   * Select a server
   */
  function selectServer(server) {
    selectedServer = server;
    renderServerList();
    if (elements.btnJoin) {
      elements.btnJoin.disabled = server.status !== 'online';
    }
  }

  /**
   * Show direct connect dialog
   */
  function showDirectConnect() {
    if (!elements.directConnect) return;
    elements.directConnect.classList.add('show');
    if (elements.dcAddress) {
      elements.dcAddress.value = '';
      elements.dcAddress.focus();
    }
  }

  /**
   * Hide direct connect dialog
   */
  function hideDirectConnect() {
    if (!elements.directConnect) return;
    elements.directConnect.classList.remove('show');
  }

  /**
   * Prompt for adding a new server
   */
  function promptAddServer() {
    var name = prompt('Enter server name:');
    if (!name) return;
    var address = prompt('Enter server address (host:port):');
    if (!address) return;

    var newServer = {
      id: 'server_' + Date.now(),
      address: address,
      name: name,
      status: 'pending',
      ping: -1,
      players: '0/10'
    };

    serverList.push(newServer);
    renderServerList();
    selectServer(newServer);

    // Ping the new server
    pingServer(newServer);
  }

  /**
   * Connect directly to a server
   */
  function connectDirect() {
    var address = elements.dcAddress ? elements.dcAddress.value.trim() : '';
    if (!address) {
      alert('Please enter a server address');
      return;
    }

    hideDirectConnect();
    hideMultiplayerMenu();

    // Connect via SocketRedirect
    connectToServer(address);
  }

  /**
   * Join the selected server
   */
  function joinSelectedServer() {
    if (!selectedServer) return;

    hideMultiplayerMenu();
    connectToServer(selectedServer.address);
  }

  /**
   * Connect to a server
   */
  function connectToServer(address) {
    console.log('[WebMC/Multiplayer] Connecting to:', address);
    showConnectionStatus('Connecting...', 'connecting');

    if (window.SocketRedirect) {
      SocketRedirect.initMultiplayer(address);

      // Set up connection callbacks
      WebMC.addEventListener('serverInfo', function onConnect(data) {
        showConnectionStatus('Connected: ' + address, 'connected');
        console.log('[WebMC/Multiplayer] Connected to', address);

        // Notify game engine
        if (window.__webmcOnMultiplayerConnect) {
          window.__webmcOnMultiplayerConnect(address);
        }

        // Show in-game player list hint
        addChatMessage('Connected to ' + address, 'info');

        // Clean up listener after first successful connection
        setTimeout(function() {
          WebMC.removeEventListener('serverInfo', onConnect);
        }, 5000);
      });

      // Timeout after 10 seconds
      setTimeout(function() {
        var status = elements.connectionStatus;
        if (status && status.textContent === 'Connecting...') {
          showConnectionStatus('Connection failed', 'error');
          addChatMessage('Failed to connect to ' + address, 'error');
        }
      }, 10000);
    } else {
      showConnectionStatus('SocketRedirect not available', 'error');
      addChatMessage('Multiplayer not available in this build', 'error');
    }
  }

  /**
   * Refresh server list
   */
  function refreshServers() {
    console.log('[WebMC/Multiplayer] Refreshing servers...');

    serverList.forEach(function(server) {
      server.status = 'pending';
      server.ping = -1;
    });

    renderServerList();

    // Ping all servers (simplified - would use proper ping in real impl)
    serverList.forEach(function(server) {
      pingServer(server);
    });
  }

  /**
   * Ping a server to check status
   */
  function pingServer(server) {
    var startTime = Date.now();

    // Simulated ping - in real implementation would use WebSocket ping
    setTimeout(function() {
      server.ping = Date.now() - startTime + Math.random() * 20;
      server.status = 'online';
      server.players = Math.floor(Math.random() * 5) + '/' + Math.floor(Math.random() * 10 + 5);
      renderServerList();
    }, 200 + Math.random() * 300);
  }

  /**
   * Show connection status indicator
   */
  function showConnectionStatus(text, statusClass) {
    if (!elements.connectionStatus) return;

    elements.connectionStatus.textContent = text;
    elements.connectionStatus.className = 'show ' + (statusClass || '');

    // Auto-hide connected status after 5 seconds
    if (statusClass === 'connected') {
      setTimeout(function() {
        if (elements.connectionStatus.textContent === text) {
          elements.connectionStatus.classList.remove('show');
        }
      }, 5000);
    }
  }

  /**
   * Add a message to chat
   */
  function addChatMessage(text, type) {
    if (window.__webmcAddChatMessage) {
      window.__webmcAddChatMessage(text, type);
    }
    console.log('[Chat] ' + text);
  }

  /**
   * Escape HTML to prevent XSS
   */
  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Get current connection state
   */
  function getConnectionState() {
    if (window.SocketRedirect) {
      return SocketRedirect.getState();
    }
    return 'disconnected';
  }

  /**
   * Disconnect from server
   */
  function disconnect() {
    if (window.SocketRedirect && window.SocketRedirect._ws) {
      window.SocketRedirect._ws.close();
    }
    showConnectionStatus('', '');
    addChatMessage('Disconnected from server', 'info');
  }

  // Expose API for external use
  window.WebMCMultiplayer = {
    init: init,
    showMenu: showMultiplayerMenu,
    hideMenu: hideMultiplayerMenu,
    getConnectionState: getConnectionState,
    disconnect: disconnect,
    getServerList: function() { return serverList; },
    addServer: function(id, address, name) {
      serverList.push({ id: id, address: address, name: name, status: 'offline', ping: -1, players: '0/10' });
      renderServerList();
    }
  };

  // Auto-initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
