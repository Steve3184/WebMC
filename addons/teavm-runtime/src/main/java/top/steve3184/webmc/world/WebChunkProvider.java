package top.steve3184.webmc.world;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块提供者 - 管理区块加载和卸载
 */
public class WebChunkProvider {

    private final WebChunkGenerator generator;
    private final Map<String, WebChunkSection> chunks;
    private final int renderDistance;

    public WebChunkProvider(WebChunkGenerator generator) {
        this.generator = generator;
        this.chunks = new HashMap<>();
        this.renderDistance = 4; // 4 区块渲染距离
    }

    /**
     * 获取区块
     */
    public WebChunkSection getChunk(int chunkX, int chunkY, int chunkZ) {
        String key = chunkKey(chunkX, chunkY, chunkZ);

        if (!chunks.containsKey(key)) {
            // 生成新区块
            WebChunkSection chunk = generator.generateChunk(chunkX, chunkY, chunkZ);
            chunks.put(key, chunk);
        }

        return chunks.get(key);
    }

    /**
     * 加载玩家周围的区块
     */
    public void loadChunksAround(float playerX, float playerY, float playerZ) {
        int playerChunkX = Math.floorDiv((int) playerX, 16);
        int playerChunkZ = Math.floorDiv((int) playerZ, 16);

        // 卸载远处的区块
        String[] keysToRemove = chunks.keySet().toArray(new String[0]);
        for (String key : keysToRemove) {
            String[] parts = key.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[2]);

            int dist = Math.max(Math.abs(cx - playerChunkX), Math.abs(cz - playerChunkZ));
            if (dist > renderDistance + 2) {
                chunks.remove(key);
            }
        }

        // 加载新区块
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    getChunk(playerChunkX + dx, dy, playerChunkZ + dz);
                }
            }
        }
    }

    /**
     * 获取区块键
     */
    private String chunkKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    /**
     * 获取加载的区块数量
     */
    public int getLoadedChunkCount() {
        return chunks.size();
    }
}
