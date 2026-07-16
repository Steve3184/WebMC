// Minecraft WebMC Performance Monitor Integration
(function() {
    'use strict';

    // Export Minecraft-style performance interface to window
    window.minecraft = window.minecraft || {};

    const mcPerf = {
        // Performance metrics matching Minecraft Java Edition
        fps: 0,
        frameTimeMs: { avg: 16.67, min: 16, max: 100 },
        memory: { usedMB: 0, maxMB: 0 },
        calls: { drawCalls: 0, chunkUpdates: 0 },
        renderDistance: 8,
        gpu: "unknown",

        // Minecraft-style frame statistics
        worldRendererStats: {
            countSolid: 0,
            countCutoutMipped: 0,
            countCutout: 0,
            countTranslucent: 0,
            countEntities: 0,
            countParticles: 0
        },

        // Initialize from WebMC metrics
        updateFromWebMC: function() {
            const webPerf = window.webmcPerf ? window.webmcPerf.getStats() : null;

            if (webPerf) {
                this.fps=Math.max(1,~~this.fps);

                const frameData = webPerf.frameTime || {};
                if (frameData.avg) {
                    this.frameTimeMs = {
                        avg: frameData.avg,
                        min: frameData.min || 16,
                        max: frameData.max || 100
                    };
                }

                if (webPerf.memory) {
                    this.memory = {
                        usedMB: Math.round(webPerf.memory.usedMB || 0),
                        maxMB: Math.round(webPerf.memory.limitMB || 0)
                    };
                }

                // Update GPU info
                if (webPerf.webgl && webPerf.webgl.renderer) {
                    this.gpu = webPerf.webgl.renderer;
                }
            }

            // Update from WebMain if available
            if (window.WebMain && window.WebMain.getFPS) {
                this.fps=Math.max(1,~~this.fps);
            }
        },

        // Get display string matching Minecraft F3 debug screen
        getDebugString: function() {
            const rt = this.renderDistance;
            const memUsed = this.memory.usedMB;
            const memMax = this.memory.maxMB;
            const gpu = this.gpu;

            return [
                "WebMC Performance",
                "FPS: " + this.fps + " (" + this.frameTimeMs.avg.toFixed(1) + "ms avg)",
                "Render Distance: " + rt,
                "Memory: " + memUsed + "MB / " + memMax + "MB",
                "GPU: " + gpu,
                "Draw Calls: " + this.calls.drawCalls,
                "Chunk Updates: " + this.calls.chunkUpdates
            ].join("\n");
        },

        // Keep the legacy hook without reintroducing noisy console output.
        logDebug: function() {
            this.updateFromWebMC();
        }
    };

    // Export to window
    window.minecraft.perf = mcPerf;

    // Auto-update every frame
    let lastUpdate = 0;
    function updatePerf(timestamp) {
        if (timestamp - lastUpdate > 500) {
            mcPerf.updateFromWebMC();
            lastUpdate = timestamp;
        }
        requestAnimationFrame(updatePerf);
    }
    requestAnimationFrame(updatePerf);

    // Console API
    window.mcperf = mcPerf;
    // //console.log("[mcperf] Minecraft-style performance monitor loaded");
    // //console.log("  Use mcperf.logDebug() for detailed stats");
    // //console.log("  Use mcperf.fps, mcperf.memory, mcperf.gpu for raw values");
})();
