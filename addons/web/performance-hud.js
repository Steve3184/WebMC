/**
 * performance-hud.js — WebMC Performance HUD
 *
 * Displays real-time performance metrics as an overlay in the browser.
 *
 * Usage:
 *   window.webmcHUD.show()     // Show the HUD
 *   window.webmcHUD.hide()     // Hide the HUD
 *   window.webmcHUD.toggle()   // Toggle visibility
 *   window.webmcHUD.setPosition('top-right')  // Change position
 */

(function () {
    'use strict';

    const HUD = {
        _visible: false,
        _element: null,
        _updateInterval: 200, // ms
        _lastUpdate: 0,
        _position: 'top-right',
        _compactMode: true,
        _showGraphs: false,
        _fpsHistory: [],
        _fpsHistoryMax: 60,

        /**
         * Initialize the HUD
         */
        init: function () {
            if (this._element) return;

            // Create HUD element
            this._element = document.createElement('div');
            this._element.id = 'webmc-perf-hud';
            this._element.style.cssText = `
                position: fixed;
                top: 10px;
                right: 10px;
                background: rgba(0, 0, 0, 0.75);
                color: #00ff00;
                font-family: 'Consolas', 'Monaco', monospace;
                font-size: 12px;
                padding: 8px 12px;
                border-radius: 4px;
                z-index: 10000;
                pointer-events: none;
                min-width: 150px;
                text-align: left;
                line-height: 1.4;
                ${this._position === 'top-left' ? 'left: 10px; right: auto;' : ''}
                ${this._position === 'bottom-right' ? 'top: auto; bottom: 10px;' : ''}
                ${this._position === 'bottom-left' ? 'top: auto; bottom: 10px; left: 10px; right: auto;' : ''}
            `;

            document.body.appendChild(this._element);

            // Initial visibility
            this._visible = true;
            this.update();

            // Listen for messages from Java
            window.addEventListener('message', this._handleMessage.bind(this));

            // console.log('[mc-web/hud] Performance HUD initialized');
        },

        /**
         * Handle messages from Java code
         */
        _handleMessage: function (event) {
            if (event.data && event.data.type === 'webmc:perf') {
                this.updateFromData(event.data);
            }
        },

        /**
         * Update HUD from performance data
         */
        updateFromData: function (data) {
            if (!this._visible || !this._element) return;

            const fps = data.fps || 0;
            const frameTime = data.frameTime || 0;
            const gpuTier = data.gpuTier || 'unknown';

            // Update FPS history for graph
            this._fpsHistory.push(fps);
            if (this._fpsHistory.length > this._fpsHistoryMax) {
                this._fpsHistory.shift();
            }

            // Color based on performance
            let fpsColor = '#00ff00';
            if (fps < 30) fpsColor = '#ff0000';
            else if (fps < 45) fpsColor = '#ffff00';
            else if (fps < 55) fpsColor = '#00ffff';

            if (this._compactMode) {
                this._element.innerHTML = `
                    <div style="color: ${fpsColor}; font-size: 18px; font-weight: bold;">
                        ${fps} FPS
                    </div>
                    <div style="color: #888; font-size: 10px;">
                        ${frameTime.toFixed(1)}ms | ${gpuTier}
                    </div>
                `;
            } else {
                // Full mode
                const drawCalls = data.drawCalls || 0;
                const triangles = data.triangles || 0;
                const memory = data.memory || {};

                this._element.innerHTML = `
                    <div style="border-bottom: 1px solid #333; padding-bottom: 4px; margin-bottom: 4px;">
                        <span style="color: ${fpsColor}; font-size: 20px; font-weight: bold;">${fps} FPS</span>
                    </div>
                    <div>
                        Frame: <span style="color: #fff;">${frameTime.toFixed(2)}ms</span>
                    </div>
                    <div>
                        Draws: <span style="color: #fff;">${drawCalls}</span>
                    </div>
                    <div>
                        Tris: <span style="color: #fff;">${triangles.toLocaleString()}</span>
                    </div>
                    ${memory.usedMB ? `
                    <div>
                        Memory: <span style="color: #fff;">${memory.usedMB}MB</span>
                    </div>
                    ` : ''}
                    <div style="border-top: 1px solid #333; padding-top: 4px; margin-top: 4px; color: #666; font-size: 10px;">
                        GPU: ${gpuTier}
                    </div>
                    ${this._showGraphs ? this._renderFpsGraph() : ''}
                `;
            }
        },

        /**
         * Render FPS graph
         */
        _renderFpsGraph: function () {
            if (this._fpsHistory.length < 2) return '';

            const width = 120;
            const height = 30;
            const maxFps = 60;

            let path = '';
            for (let i = 0; i < this._fpsHistory.length; i++) {
                const x = (i / (this._fpsHistory.length - 1)) * width;
                const y = height - (this._fpsHistory[i] / maxFps) * height;
                if (i === 0) {
                    path += `M ${x} ${y}`;
                } else {
                    path += ` L ${x} ${y}`;
                }
            }

            return `
                <svg width="${width}" height="${height}" style="margin-top: 4px;">
                    <polyline points="${this._fpsHistory.map((f, i) =>
                        `${(i / (this._fpsHistory.length - 1)) * width},${height - (f / maxFps) * height}`
                    ).join(' ')}"
                        fill="none" stroke="#00ff00" stroke-width="1.5"/>
                </svg>
            `;
        },

        /**
         * Update HUD with data from window.webmcPerf if available
         */
        update: function () {
            if (!this._visible) return;

            const perf = window.webmcPerf;
            if (perf) {
                const stats = perf.getStats();
                this.updateFromData({
                    fps: stats.fps,
                    frameTime: stats.frameTime.avg || stats.frameTime.current,
                    drawCalls: stats.drawCalls || 0,
                    triangles: stats.triangles || 0,
                    gpuTier: stats.gpu?.renderer || 'unknown',
                    memory: stats.memory || {}
                });
            }

            // Schedule next update
            setTimeout(this.update.bind(this), this._updateInterval);
        },

        /**
         * Show the HUD
         */
        show: function () {
            this._visible = true;
            if (this._element) {
                this._element.style.display = 'block';
            }
        },

        /**
         * Hide the HUD
         */
        hide: function () {
            this._visible = false;
            if (this._element) {
                this._element.style.display = 'none';
            }
        },

        /**
         * Toggle HUD visibility
         */
        toggle: function () {
            if (this._visible) {
                this.hide();
            } else {
                this.show();
            }
        },

        /**
         * Set HUD position
         */
        setPosition: function (position) {
            this._position = position;
            if (this._element) {
                this._element.style.left = 'auto';
                this._element.style.right = 'auto';
                this._element.style.top = '10px';
                this._element.style.bottom = 'auto';

                switch (position) {
                    case 'top-left':
                        this._element.style.left = '10px';
                        break;
                    case 'bottom-right':
                        this._element.style.top = 'auto';
                        this._element.style.bottom = '10px';
                        break;
                    case 'bottom-left':
                        this._element.style.left = '10px';
                        this._element.style.top = 'auto';
                        this._element.style.bottom = '10px';
                        break;
                    case 'top-right':
                    default:
                        this._element.style.right = '10px';
                        break;
                }
            }
        },

        /**
         * Set compact mode (minimal display)
         */
        setCompact: function (compact) {
            this._compactMode = compact;
        },

        /**
         * Show/hide FPS graph
         */
        setShowGraphs: function (show) {
            this._showGraphs = show;
        },

        /**
         * Set update interval
         */
        setUpdateInterval: function (ms) {
            this._updateInterval = ms;
        },

        /**
         * Send performance data from Java
         */
        updateFromJava: function (fps, frameTime, drawCalls, triangles, gpuTier) {
            this.updateFromData({
                fps: fps,
                frameTime: frameTime,
                drawCalls: drawCalls,
                triangles: triangles,
                gpuTier: gpuTier
            });
        }
    };

    // Export to global scope
    window.webmcHUD = HUD;

    // Auto-init when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', HUD.init.bind(HUD));
    } else {
        HUD.init();
    }

    // Keyboard shortcut: F3 + P to toggle HUD
    document.addEventListener('keydown', function (e) {
        if (e.key === 'F3') {
            e.preventDefault();
            HUD.toggle();
        }
    });

    // console.log('[mc-web/hud] HUD module loaded. Press F3 to toggle.');

})();
