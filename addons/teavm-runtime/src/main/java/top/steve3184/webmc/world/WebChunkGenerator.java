package top.steve3184.webmc.world;

/**
 * 简化的世界生成器
 */
public class WebChunkGenerator {

    // 地形参数
    private static final int SEA_LEVEL = 62;
    private static final int NOISE_SCALE = 64;
    private static final int AMPLITUDE = 15;

    private final long seed;

    public WebChunkGenerator() {
        this.seed = System.currentTimeMillis();
    }

    public WebChunkGenerator(long seed) {
        this.seed = seed;
    }

    /**
     * 生成区块
     */
    public WebChunkSection generateChunk(int chunkX, int chunkY, int chunkZ) {
        WebChunkSection section = new WebChunkSection(chunkX, chunkY, chunkZ);

        // 只生成玩家周围的区块
        if (chunkY < -4 || chunkY > 4) {
            return section;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // 世界坐标
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                // 简单的噪音地形
                int height = generateHeight(worldX, worldZ);

                // 填充区块
                for (int y = 0; y < 16; y++) {
                    int worldY = chunkY * 16 + y;

                    BlockType block;
                    if (worldY > height) {
                        block = BlockType.AIR;
                    } else if (worldY == height) {
                        // 顶层是草
                        block = BlockType.GRASS;
                    } else if (worldY > height - 4) {
                        // 下面是泥土
                        block = BlockType.DIRT;
                    } else {
                        // 再下面是石头
                        block = BlockType.STONE;
                    }

                    section.setBlock(x, y, z, block);
                }
            }
        }

        return section;
    }

    /**
     * 生成地形高度
     */
    private int generateHeight(int x, int z) {
        // 简化的 Perlin 噪音近似
        double noise = 0;

        // 多层叠加
        noise += Math.sin(x * 0.05) * Math.cos(z * 0.05) * AMPLITUDE;
        noise += Math.sin(x * 0.02 + z * 0.03) * AMPLITUDE * 0.5;

        return SEA_LEVEL + (int) noise;
    }

    /**
     * 获取给定坐标的地面高度
     */
    public int getGroundLevel(int worldX, int worldZ) {
        return generateHeight(worldX, worldZ);
    }

    public long getSeed() {
        return seed;
    }
}
