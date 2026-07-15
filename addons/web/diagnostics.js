/**
 * diagnostics.js — WebMC Enhanced Diagnostics Module
 *
 * Provides comprehensive diagnostic logging including:
 * - Startup timeline tracking (extending bootstrap.js timeline)
 * - VFS load time measurement
 * - Shader compilation time tracking
 * - Network latency monitoring
 * - Error tracking and reporting
 *
 * Usage:
 *   window.webmcDiagnostics.start()     // Start diagnostics collection
 *   window.webmcDiagnostics.getReport() // Get full diagnostic report
 *   window.webmcDiagnostics.log()       // Log report to console
 */

(function () {
    'use strict';

    const Diagnostics = {
        // Configuration
        _enabled: false,
        _verbose: false,

        // Timeline events (extends bootstrap timeline)
        _events: [],
        _maxEvents: 500,

        // Startup timing
        _startTime: 0,
        _bootstrapReady: false,

        // VFS tracking
        _vfsLoadStart: 0,
        _vfsLoadEnd: 0,
        _vfsLoadBytes: 0,
        _vfsLoadAttempts: 0,

        // Shader compilation tracking
        _shaderCompilations: [],
        _totalShaderCompileTime: 0,
        _shaderCount: 0,
        _shaderErrors: 0,

        // Network latency samples
        _networkLatencySamples: [],
        _maxLatencySamples: 100,
        _lastPingTime: 0,

        // Errors
        _errors: [],
        _maxErrors: 50,
        _warnings: [],
        _maxWarnings: 50,

        // WebGL state
        _webglExtensions: [],
        _webglErrors: [],

        // Memory snapshots
        _memorySnapshots: [],
        _maxSnapshots: 30,

        // Frame metrics (delegates to performance.js if available)
        _frameMetrics: null,

        /**
         * Initialize diagnostics
         */
        init: function (options) {
            options = options || {};
            this._enabled = options.enabled !== false;
            this._verbose = options.verbose || false;

            if (this._enabled) {
                this._startTime = Date.now();
                this.record('diagnostics:init', 'Diagnostics module initialized');

                // Try to get reference to performance module
                if (window.webmcPerf) {
                    this._frameMetrics = window.webmcPerf;
                    this.record('diagnostics:perf-linked', 'Performance module linked');
                }

                // Set up error handlers
                this._setupErrorHandlers();

                // Set up memory polling
                this._setupMemoryPolling();

                // Set up network monitoring
                this._setupNetworkMonitoring();

                // //console.log('[mc-web/diagnostics] Enhanced diagnostics initialized');
            }
        },

        /**
         * Start diagnostics collection
         */
        start: function () {
            if (this._enabled) {
                return;
            }
            this.init({ enabled: true, verbose: false });
        },

        /**
         * Set up global error handlers
         */
        _setupErrorHandlers: function () {
            const self = this;

            // Capture JavaScript errors
            window.addEventListener('error', function (e) {
                self.recordError('javascript', e.message, {
                    filename: e.filename,
                    lineno: e.lineno,
                    colno: e.colno,
                    stack: e.error?.stack
                });
            });

            // Capture unhandled promise rejections
            window.addEventListener('unhandledrejection', function (e) {
                self.recordError('unhandled-promise', String(e.reason), {
                    stack: e.reason?.stack
                });
            });

            // Capture WebGL errors via console error interception
            const originalError = console.error.bind(console);
            console.error = function (...args) {
                const msg = args.join(' ');
                if (msg.includes('WebGL') || msg.includes('gl.getError') || msg.includes('shader')) {
                    self._webglErrors.push({
                        message: msg,
                        timestamp: Date.now()
                    });
                }
                originalError.apply(console, args);
            };
        },

        /**
         * Set up periodic memory snapshots
         */
        _setupMemoryPolling: function () {
            const self = this;
            setInterval(function () {
                if (self._enabled && performance.memory) {
                    self._memorySnapshots.push({
                        timestamp: Date.now(),
                        usedJSHeapSize: performance.memory.usedJSHeapSize,
                        totalJSHeapSize: performance.memory.totalJSHeapSize,
                        jsHeapSizeLimit: performance.memory.jsHeapSizeLimit,
                        usedMB: Math.round(performance.memory.usedJSHeapSize / 1048576 * 100) / 100
                    });
                    if (self._memorySnapshots.length > self._maxSnapshots) {
                        self._memorySnapshots.shift();
                    }
                }
            }, 5000); // Every 5 seconds
        },

        /**
         * Set up network request monitoring
         */
        _setupNetworkMonitoring: function () {
            const self = this;
            const originalFetch = window.fetch;

            window.fetch = function (input, init) {
                const startTime = performance.now();
                const url = typeof input === 'string' ? input : input.url;

                return originalFetch.apply(window, arguments)
                    .then(function (response) {
                        const latency = performance.now() - startTime;
                        self.recordNetworkLatency(url, latency, response.status);
                        return response;
                    })
                    .catch(function (error) {
                        const latency = performance.now() - startTime;
                        self.recordNetworkLatency(url, latency, 0, String(error));
                        throw error;
                    });
            };
        },

        /**
         * Record a timeline event
         */
        record: function (name, detail, data) {
            if (!this._enabled) {
                return;
            }

            const now = Date.now();
            const perfNow = performance.now();
            const elapsed = this._startTime > 0 ? now - this._startTime : 0;

            const event = {
                name: String(name || ''),
                detail: String(detail || ''),
                data: data || null,
                timestamp: now,
                perfTimestamp: perfNow,
                elapsedMs: elapsed
            };

            // Add to local events
            this._events.push(event);
            if (this._events.length > this._maxEvents) {
                this._events.shift();
            }

            // Also add to bootstrap timeline if available
            if (typeof window.__webmcStartupMark === 'function') {
                window.__webmcStartupMark(name, detail);
            }

            // Log if verbose
            if (this._verbose) {
                // //console.log(`[mc-web/diag] ${elapsed}ms ${name}${detail ? ': ' + detail : ''}`);
            }

            return event;
        },

        /**
         * Record an error
         */
        recordError: function (type, message, details) {
            const error = {
                type: String(type || 'unknown'),
                message: String(message || ''),
                details: details || null,
                timestamp: Date.now(),
                elapsedMs: this._startTime > 0 ? Date.now() - this._startTime : 0
            };

            this._errors.push(error);
            if (this._errors.length > this._maxErrors) {
                this._errors.shift();
            }

            console.error(`[mc-web/diagnostics/error] ${type}: ${message}`, details || '');
            return error;
        },

        /**
         * Record a warning
         */
        recordWarning: function (type, message, details) {
            const warning = {
                type: String(type || 'unknown'),
                message: String(message || ''),
                details: details || null,
                timestamp: Date.now(),
                elapsedMs: this._startTime > 0 ? Date.now() - this._startTime : 0
            };

            this._warnings.push(warning);
            if (this._warnings.length > this._maxWarnings) {
                this._warnings.shift();
            }

            // //console.warn(`[mc-web/diagnostics/warn] ${type}: ${message}`, details || '');
            return warning;
        },

        /**
         * Start VFS load timing
         */
        startVfsLoad: function (url, bytes) {
            this._vfsLoadStart = performance.now();
            this._vfsLoadAttempts++;
            this._vfsLoadBytes = bytes || 0;
            this.record('vfs:load:start', url, { bytes: bytes });
        },

        /**
         * End VFS load timing
         */
        endVfsLoad: function (success, bytesLoaded) {
            this._vfsLoadEnd = performance.now();
            const duration = this._vfsLoadEnd - this._vfsLoadStart;

            this.record('vfs:load:end', `success=${success} duration=${duration.toFixed(0)}ms`, {
                durationMs: duration,
                bytes: bytesLoaded || this._vfsLoadBytes,
                throughputMBps: bytesLoaded ? (bytesLoaded / 1048576 / (duration / 1000)).toFixed(2) : null
            });

            return duration;
        },

        /**
         * Record shader compilation
         */
        recordShaderCompile: function (shaderType, success, compileTimeMs, sourceLength) {
            const entry = {
                type: String(shaderType || 'unknown'),
                success: Boolean(success),
                compileTimeMs: Number(compileTimeMs) || 0,
                sourceLength: Number(sourceLength) || 0,
                timestamp: Date.now()
            };

            this._shaderCompilations.push(entry);
            this._shaderCount++;
            if (!success) {
                this._shaderErrors++;
            }
            this._totalShaderCompileTime += entry.compileTimeMs;

            this.record('shader:compile', `${shaderType} ${success ? 'success' : 'FAILED'}`, entry);
        },

        /**
         * Record network latency
         */
        recordNetworkLatency: function (url, latencyMs, status, error) {
            // Normalize URL for grouping
            const normalizedUrl = this._normalizeUrl(url);

            const sample = {
                url: normalizedUrl,
                latencyMs: Number(latencyMs) || 0,
                status: Number(status) || 0,
                error: error || null,
                timestamp: Date.now()
            };

            this._networkLatencySamples.push(sample);
            if (this._networkLatencySamples.length > this._maxLatencySamples) {
                this._networkLatencySamples.shift();
            }
        },

        /**
         * Normalize URL for grouping
         */
        _normalizeUrl: function (url) {
            if (!url) return 'unknown';
            try {
                const parsed = new URL(url, window.location.href);
                // Remove query params and hash for grouping
                return parsed.pathname + parsed.search;
            } catch {
                return url.substring(0, 100);
            }
        },

        /**
         * Measure ping latency to server
         */
        measurePing: function (url) {
            const self = this;
            const startTime = performance.now();

            return fetch(url, { method: 'HEAD', cache: 'no-cache' })
                .then(function (response) {
                    const latency = performance.now() - startTime;
                    self.recordNetworkLatency('PING:' + url, latency, response.status);
                    return latency;
                })
                .catch(function (error) {
                    const latency = performance.now() - startTime;
                    self.recordNetworkLatency('PING:' + url, latency, 0, String(error));
                    return latency;
                });
        },

        /**
         * Record WebGL extension
         */
        recordWebGLExtension: function (extensionName) {
            if (!this._webglExtensions.includes(extensionName)) {
                this._webglExtensions.push(extensionName);
                this.record('webgl:extension', extensionName);
            }
        },

        /**
         * Get events by category/prefix
         */
        getEventsByPrefix: function (prefix) {
            return this._events.filter(function (e) {
                return e.name.startsWith(prefix);
            });
        },

        /**
         * Calculate statistics for an array
         */
        _calculateStats: function (arr, valueKey) {
            if (!arr || arr.length === 0) {
                return null;
            }

            const values = arr.map(function (item) {
                return typeof item === 'number' ? item : item[valueKey];
            }).filter(function (v) {
                return typeof v === 'number' && isFinite(v);
            });

            if (values.length === 0) {
                return null;
            }

            const sorted = [...values].sort(function (a, b) { return a - b; });
            const sum = sorted.reduce(function (a, b) { return a + b; }, 0);

            return {
                count: sorted.length,
                min: sorted[0],
                max: sorted[sorted.length - 1],
                avg: sum / sorted.length,
                median: sorted[Math.floor(sorted.length / 2)],
                p95: sorted[Math.floor(sorted.length * 0.95)] || sorted[sorted.length - 1],
                p99: sorted[Math.floor(sorted.length * 0.99)] || sorted[sorted.length - 1]
            };
        },

        /**
         * Get comprehensive diagnostic report
         */
        getReport: function () {
            const now = Date.now();
            const totalElapsed = this._startTime > 0 ? now - this._startTime : 0;

            // Calculate network latency stats
            const networkStats = {};
            const urlGroups = {};

            for (const sample of this._networkLatencySamples) {
                const url = sample.url;
                if (!urlGroups[url]) {
                    urlGroups[url] = [];
                }
                urlGroups[url].push(sample.latencyMs);
            }

            for (const url in urlGroups) {
                networkStats[url] = this._calculateStats(urlGroups[url]);
            }

            // Get frame metrics from performance module
            let frameMetrics = null;
            if (this._frameMetrics && typeof this._frameMetrics.getStats === 'function') {
                frameMetrics = this._frameMetrics.getStats();
            }

            return {
                // Timing
                timing: {
                    startTime: this._startTime,
                    now: now,
                    totalElapsedMs: totalElapsed,
                    totalElapsedSec: (totalElapsed / 1000).toFixed(2)
                },

                // Startup timeline
                timeline: this._events.slice(),

                // VFS stats
                vfs: {
                    loadAttempts: this._vfsLoadAttempts,
                    loadStart: this._vfsLoadStart,
                    loadEnd: this._vfsLoadEnd,
                    loadDurationMs: this._vfsLoadEnd > 0 ? this._vfsLoadEnd - this._vfsLoadStart : 0,
                    bytesLoaded: this._vfsLoadBytes,
                    throughputMBps: this._vfsLoadBytes && this._vfsLoadEnd > this._vfsLoadStart ?
                        (this._vfsLoadBytes / 1048576 / ((this._vfsLoadEnd - this._vfsLoadStart) / 1000)).toFixed(2) : null
                },

                // Shader compilation stats
                shaders: {
                    totalCompilations: this._shaderCount,
                    successfulCompilations: this._shaderCount - this._shaderErrors,
                    failedCompilations: this._shaderErrors,
                    totalCompileTimeMs: this._totalShaderCompileTime,
                    avgCompileTimeMs: this._shaderCount > 0 ?
                        (this._totalShaderCompileTime / this._shaderCount).toFixed(2) : 0,
                    compilations: this._shaderCompilations.slice(-20) // Last 20
                },

                // Network stats
                network: {
                    totalRequests: this._networkLatencySamples.length,
                    latencyStats: networkStats,
                    samples: this._networkLatencySamples.slice(-50) // Last 50
                },

                // Error stats
                errors: {
                    count: this._errors.length,
                    list: this._errors.slice(),
                    byType: this._errors.reduce(function (acc, e) {
                        acc[e.type] = (acc[e.type] || 0) + 1;
                        return acc;
                    }, {})
                },

                // Warning stats
                warnings: {
                    count: this._warnings.length,
                    list: this._warnings.slice(),
                    byType: this._warnings.reduce(function (acc, w) {
                        acc[w.type] = (acc[w.type] || 0) + 1;
                        return acc;
                    }, {})
                },

                // WebGL info
                webgl: {
                    extensions: this._webglExtensions.slice(),
                    errors: this._webglErrors.slice()
                },

                // Memory snapshots
                memory: {
                    snapshots: this._memorySnapshots.slice(),
                    latest: this._memorySnapshots[this._memorySnapshots.length - 1] || null
                },

                // Frame metrics (from performance.js)
                frames: frameMetrics,

                // Summary
                summary: this._getSummaryString()
            };
        },

        /**
         * Generate summary string
         */
        _getSummaryString: function () {
            const elapsed = this._startTime > 0 ?
                ((Date.now() - this._startTime) / 1000).toFixed(1) : 0;

            let summary = `Uptime: ${elapsed}s | Events: ${this._events.length}`;

            if (this._errors.length > 0) {
                summary += ` | Errors: ${this._errors.length}`;
            }

            if (this._warnings.length > 0) {
                summary += ` | Warnings: ${this._warnings.length}`;
            }

            if (this._frameMetrics && this._frameMetrics._fps !== undefined) {
                summary += ` | FPS: ${this._frameMetrics._fps}`;
            }

            return summary;
        },

        /**
         * Log full report to console
         */
        log: function () {
            const report = this.getReport();

            // //console.log('%c=== WebMC Diagnostic Report ===', 'font-weight: bold; color: #4CAF50;');
            // //console.log(`Session Duration: ${report.timing.totalElapsedSec}s`);
            // //console.log(`Timeline Events: ${report.timeline.length}`);

            if (report.errors.count > 0) {
                console.error(`Errors (${report.errors.count}):`, report.errors.list);
            }

            if (report.warnings.count > 0) {
                // //console.warn(`Warnings (${report.warnings.count}):`, report.warnings.list);
            }

            // //console.log('VFS Load:', report.vfs);
            // //console.log('Shader Compilations:', report.shaders);
            // //console.log('Network Stats:', report.network.latencyStats);
            // //console.log('WebGL Extensions:', report.webgl.extensions);

            if (report.memory.latest) {
                // //console.log(`Memory: ${report.memory.latest.usedMB}MB`);
            }

            if (report.frames) {
                // //console.log(`Performance: FPS=${report.frames.fps}, Frame=${report.frames.frameTime.avg}ms`);
            }

            // //console.log('%c================================', 'font-weight: bold; color: #4CAF50;');
        },

        /**
         * Export report as JSON
         */
        export: function () {
            return JSON.stringify(this.getReport(), null, 2);
        },

        /**
         * Clear all collected data
         */
        clear: function () {
            this._events = [];
            this._errors = [];
            this._warnings = [];
            this._shaderCompilations = [];
            this._networkLatencySamples = [];
            this._memorySnapshots = [];
            this._webglErrors = [];
            this._vfsLoadStart = 0;
            this._vfsLoadEnd = 0;
            this._vfsLoadBytes = 0;
            this._shaderCount = 0;
            this._shaderErrors = 0;
            this._totalShaderCompileTime = 0;

            // //console.log('[mc-web/diagnostics] Diagnostic data cleared');
        }
    };

    // Export to global scope
    window.webmcDiagnostics = Diagnostics;

    // Auto-initialize if bootstrap already enabled diagnostics
    if (window.webmcDiagnostics !== undefined && window.webmcDiagnostics !== false) {
        Diagnostics.start();
    }

    // //console.log('[mc-web/diagnostics] Diagnostics module loaded. Access via window.webmcDiagnostics');
})();