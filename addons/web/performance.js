/**
 * performance.js — WebMC Performance Metrics Collector
 *
 * Collects real-time performance metrics including FPS, frame times,
 * memory usage, and WebGL context information.
 *
 * Usage:
 *   window.webmcPerf.start()   // Start collecting metrics
 *   window.webmcPerf.stop()    // Stop collecting metrics
 *   window.webmcPerf.getStats() // Get current statistics
 *   window.webmcPerf.reset()   // Reset all statistics
 */

(function () {
    'use strict';

    const PerfCollector = {
        // State
        _isRunning: false,
        _animationFrameId: null,
        _lastTimestamp: 0,
        _frameCount: 0,
        _fpsUpdateInterval: 500, // ms between FPS updates
        _lastFpsUpdate: 0,

        // FPS tracking
        _fps: 0,
        _fpsHistory: [],
        _fpsHistoryMax: 60,

        // Frame time tracking (in milliseconds)
        _frameTimes: [],
        _frameTimesMax: 120,
        _frameTimeMin: Infinity,
        _frameTimeMax: 0,
        _frameTimeSum: 0,
        _frameTimeCount: 0,

        // FPS samples for rolling average
        _fpsSamples: [],
        _fpsSamplesMax: 60,

        // Memory (if available via performance.memory)
        _memoryUsed: 0,
        _memoryLimit: 0,
        _memorySamples: [],

        // WebGL info
        _webglInfo: null,

        // Timestamps for various milestones
        _milestones: {},
        _milestoneOrder: [],

        /**
         * Collect WebGL context information
         */
        _collectWebGLInfo: function () {
            const canvas = document.getElementById('canvas');
            if (!canvas) {
                return null;
            }

            try {
                const gl = canvas.getContext('webgl2') || canvas.getContext('webgl');
                if (!gl) {
                    return { error: 'WebGL not available' };
                }

                const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');

                return {
                    version: gl.getParameter(gl.VERSION),
                    shadingLanguageVersion: gl.getParameter(gl.SHADING_LANGUAGE_VERSION),
                    renderer: debugInfo ? gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) : 'unknown',
                    vendor: debugInfo ? gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) : 'unknown',
                    contextType: gl instanceof WebGL2RenderingContext ? 'WebGL2' : 'WebGL1',
                    maxTextureSize: gl.getParameter(gl.MAX_TEXTURE_SIZE),
                    maxViewportDims: gl.getParameter(gl.MAX_VIEWPORT_DIMS),
                    aliasedLineWidthRange: gl.getParameter(gl.ALIASED_LINE_WIDTH_RANGE),
                    aliasedPointSizeRange: gl.getParameter(gl.ALIASED_POINT_SIZE_RANGE),
                    antialiasing: gl.getContextAttributes()?.antialias || false,
                    alpha: gl.getContextAttributes()?.alpha || false,
                    depth: gl.getContextAttributes()?.depth || false,
                    stencil: gl.getContextAttributes()?.stencil || false,
                    preserveDrawingBuffer: gl.getContextAttributes()?.preserveDrawingBuffer || false,
                    powerPreference: gl.getContextAttributes()?.powerPreference || 'default'
                };
            } catch (e) {
                return { error: String(e) };
            }
        },

        /**
         * Collect memory information if available
         */
        _collectMemoryInfo: function () {
            // Chrome/Edge provide performance.memory
            if (performance.memory) {
                return {
                    usedJSHeapSize: performance.memory.usedJSHeapSize,
                    totalJSHeapSize: performance.memory.totalJSHeapSize,
                    jsHeapSizeLimit: performance.memory.jsHeapSizeLimit,
                    usedMB: Math.round(performance.memory.usedJSHeapSize / 1048576 * 100) / 100,
                    totalMB: Math.round(performance.memory.totalJSHeapSize / 1048576 * 100) / 100,
                    limitMB: Math.round(performance.memory.jsHeapSizeLimit / 1048576 * 100) / 100,
                    usagePercent: Math.round(performance.memory.usedJSHeapSize / performance.memory.jsHeapSizeLimit * 10000) / 100
                };
            }

            // Firefox provides performance.memory (experimental)
            if (performance.deviceMemory !== undefined) {
                return {
                    deviceMemoryGB: performance.deviceMemory,
                    note: 'Device memory API available, detailed heap not available'
                };
            }

            return null;
        },

        /**
         * Main animation frame loop
         */
        _tick: function (timestamp) {
            if (!this._isRunning) {
                return;
            }

            // Calculate frame time
            if (this._lastTimestamp > 0) {
                const frameTime = timestamp - this._lastTimestamp;

                // Update frame time statistics
                this._frameTimes.push(frameTime);
                if (this._frameTimes.length > this._frameTimesMax) {
                    this._frameTimes.shift();
                }

                this._frameTimeMin = Math.min(this._frameTimeMin, frameTime);
                this._frameTimeMax = Math.max(this._frameTimeMax, frameTime);
                this._frameTimeSum += frameTime;
                this._frameTimeCount++;

                // Update FPS at intervals
                if (timestamp - this._lastFpsUpdate >= this._fpsUpdateInterval) {
                    const elapsed = timestamp - this._lastFpsUpdate;
                    this._fps = Math.round((this._frameCount * 1000) / elapsed);
                    this._fpsHistory.push(this._fps);
                    if (this._fpsHistory.length > this._fpsHistoryMax) {
                        this._fpsHistory.shift();
                    }
                    this._fpsSamples.push(this._fps);
                    if (this._fpsSamples.length > this._fpsSamplesMax) {
                        this._fpsSamples.shift();
                    }
                    this._frameCount = 0;
                    this._lastFpsUpdate = timestamp;
                }

                this._frameCount++;
            } else {
                this._lastFpsUpdate = timestamp;
            }

            this._lastTimestamp = timestamp;

            // Collect memory info periodically
            const memInfo = this._collectMemoryInfo();
            if (memInfo) {
                this._memoryUsed = memInfo.usedMB || 0;
                this._memoryLimit = memInfo.limitMB || 0;
                if (memInfo.usedMB !== undefined) {
                    this._memorySamples.push(memInfo.usedMB);
                    if (this._memorySamples.length > 60) {
                        this._memorySamples.shift();
                    }
                }
            }

            // Continue the loop
            this._animationFrameId = requestAnimationFrame(this._tick.bind(this));
        },

        /**
         * Start performance monitoring
         */
        start: function () {
            if (this._isRunning) {
                return;
            }

            this._isRunning = true;
            this._lastTimestamp = 0;
            this._frameCount = 0;
            this._lastFpsUpdate = 0;
            this._webglInfo = this._collectWebGLInfo();

            this.mark('perf:start');
            this._animationFrameId = requestAnimationFrame(this._tick.bind(this));

            // console.log('[mc-web/perf] Performance monitoring started');
        },

        /**
         * Stop performance monitoring
         */
        stop: function () {
            if (!this._isRunning) {
                return;
            }

            this._isRunning = false;
            if (this._animationFrameId) {
                cancelAnimationFrame(this._animationFrameId);
                this._animationFrameId = null;
            }

            this.mark('perf:stop');
            // console.log('[mc-web/perf] Performance monitoring stopped');
        },

        /**
         * Record a milestone timestamp
         */
        mark: function (name) {
            const now = performance.now();
            this._milestones[name] = now;

            // Track order of marks
            if (!this._milestoneOrder.includes(name)) {
                this._milestoneOrder.push(name);
            }

            return now;
        },

        /**
         * Get time elapsed since a milestone
         */
        getElapsedSince: function (name) {
            const start = this._milestones[name];
            if (start === undefined) {
                return null;
            }
            return performance.now() - start;
        },

        /**
         * Get all milestone durations
         */
        getMilestones: function () {
            const result = {};
            const baseTime = this._milestones['perf:start'] || this._milestones[this._milestoneOrder[0]] || 0;

            for (const name of this._milestoneOrder) {
                const time = this._milestones[name];
                if (time !== undefined) {
                    result[name] = {
                        timestamp: time,
                        elapsedFromStart: time - baseTime,
                        elapsedFromPrev: name === this._milestoneOrder[0] ? 0 :
                            time - (this._milestones[this._milestoneOrder[this._milestoneOrder.indexOf(name) - 1]] || baseTime)
                    };
                }
            }

            return result;
        },

        /**
         * Calculate statistics for an array of numbers
         */
        _calculateStats: function (arr) {
            if (!arr || arr.length === 0) {
                return null;
            }

            const sorted = [...arr].sort((a, b) => a - b);
            const sum = sorted.reduce((a, b) => a + b, 0);
            const avg = sum / sorted.length;

            return {
                count: sorted.length,
                min: sorted[0],
                max: sorted[sorted.length - 1],
                avg: Math.round(avg * 100) / 100,
                median: sorted[Math.floor(sorted.length / 2)],
                p1: sorted[Math.floor(sorted.length * 0.01)],
                p5: sorted[Math.floor(sorted.length * 0.05)],
                p95: sorted[Math.floor(sorted.length * 0.95)],
                p99: sorted[Math.floor(sorted.length * 0.99)]
            };
        },

        /**
         * Get comprehensive performance statistics
         */
        getStats: function () {
            const frameTimeStats = this._calculateStats(this._frameTimes);
            const fpsStats = this._calculateStats(this._fpsHistory);

            return {
                // FPS
                fps: this._fps,
                fpsHistory: this._fpsHistory.slice(),
                fpsStats: fpsStats,
                fpsSamples: this._fpsSamples.slice(),

                // Frame times
                frameTime: {
                    current: this._frameTimes.length > 0 ?
                        Math.round(this._frameTimes[this._frameTimes.length - 1] * 100) / 100 : 0,
                    min: this._frameTimeMin === Infinity ? 0 : Math.round(this._frameTimeMin * 100) / 100,
                    max: Math.round(this._frameTimeMax * 100) / 100,
                    avg: this._frameTimeCount > 0 ?
                        Math.round((this._frameTimeSum / this._frameTimeCount) * 100) / 100 : 0,
                    stats: frameTimeStats
                },

                // Memory
                memory: this._collectMemoryInfo(),
                memorySamples: this._memorySamples.slice(),
                memoryUsedMB: this._memoryUsed,
                memoryLimitMB: this._memoryLimit,

                // WebGL
                webgl: this._webglInfo,

                // Milestones
                milestones: this.getMilestones(),

                // Status
                isRunning: this._isRunning,
                uptimeMs: this._isRunning ?
                    Math.round((performance.now() - (this._milestones['perf:start'] || 0)) * 100) / 100 : 0,

                // Summary string for console display
                summary: this._getSummaryString()
            };
        },

        /**
         * Generate a one-line summary string
         */
        _getSummaryString: function () {
            // FIX: Avoid calling getStats() which creates infinite recursion
            const fps = this._fps || 0;
            const frameTimeAvg = this._frameTimeCount > 0 ?
                Math.round((this._frameTimeSum / this._frameTimeCount) * 100) / 100 : 0;
            const memoryUsedMB = this._memoryUsed || 0;
            const memoryLimitMB = this._memoryLimit || 0;

            let summary = `FPS: ${fps}`;

            if (frameTimeAvg > 0) {
                summary += ` | Frame: ${frameTimeAvg.toFixed(1)}ms`;
            }

            if (memoryUsedMB > 0) {
                summary += ` | Mem: ${memoryUsedMB}MB / ${memoryLimitMB}MB`;
            }

            return summary;
        },

        /**
         * Reset all statistics
         */
        reset: function () {
            this._fps = 0;
            this._fpsHistory = [];
            this._fpsSamples = [];
            this._frameTimes = [];
            this._frameTimeMin = Infinity;
            this._frameTimeMax = 0;
            this._frameTimeSum = 0;
            this._frameTimeCount = 0;
            this._memorySamples = [];
            this._memoryUsed = 0;
            this._memoryLimit = 0;
            this._milestones = {};
            this._milestoneOrder = [];
            this._frameCount = 0;
            this._lastTimestamp = 0;
            this._lastFpsUpdate = 0;

            // console.log('[mc-web/perf] Statistics reset');
        },

        /**
         * Log current stats to console
         */
        log: function () {
            const stats = this.getStats();

            // console.log('=== WebMC Performance Stats ===');
            // console.log(`FPS: ${stats.fps} (avg: ${stats.fpsStats?.avg || 'N/A'})`);
            // console.log(`Frame Time: ${stats.frameTime.avg}ms avg, ${stats.frameTime.min}ms min, ${stats.frameTime.max}ms max`);

            if (stats.memory) {
                if (stats.memory.usedMB !== undefined) {
                    // console.log(`Memory: ${stats.memory.usedMB}MB / ${stats.memory.limitMB}MB (${stats.memory.usagePercent}%)`);
                } else if (stats.memory.deviceMemoryGB) {
                    // console.log(`Device Memory: ~${stats.memory.deviceMemoryGB}GB (detailed heap unavailable)`);
                }
            }

            if (stats.webgl) {
                if (stats.webgl.error) {
                    // console.log(`WebGL: ${stats.webgl.error}`);
                } else {
                    // console.log(`WebGL: ${stats.webgl.contextType} - ${stats.webgl.renderer}`);
                    // console.log(`  Max Texture Size: ${stats.webgl.maxTextureSize}px`);
                }
            }

            if (Object.keys(stats.milestones).length > 0) {
                // console.log('Milestones:', stats.milestones);
            }

            // console.log(`Uptime: ${(stats.uptimeMs / 1000).toFixed(1)}s`);
            // console.log('================================');
        },

        /**
         * Enable periodic console logging
         */
        startLogging: function (intervalMs) {
            const interval = intervalMs || 2000;
            this._logIntervalId = setInterval(() => {
                if (this._isRunning) {
                    // console.log('[mc-web/perf] ' + this._getSummaryString());
                }
            }, interval);
            // console.log(`[mc-web/perf] Periodic logging started (every ${interval}ms)`);
        },

        /**
         * Stop periodic console logging
         */
        stopLogging: function () {
            if (this._logIntervalId) {
                clearInterval(this._logIntervalId);
                this._logIntervalId = null;
                // console.log('[mc-web/perf] Periodic logging stopped');
            }
        }
    };

    // Export to global scope
    window.webmcPerf = PerfCollector;

    // Auto-start if diagnostics are enabled
    if (window.webmcDiagnostics) {
        PerfCollector.start();
    }

    // console.log('[mc-web/perf] Performance module loaded. Access via window.webmcPerf');
})();