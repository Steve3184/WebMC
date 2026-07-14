package top.steve3184.webmc.world;

/**
 * 简化的区块数据类
 */
public class WebChunkSection {

    public static final int SIZE = 16;
    public static final int VOLUME = SIZE * SIZE * SIZE;

    private final BlockType[] blocks;
    private final int chunkX;
    private final int chunkY;
    private final int chunkZ;

    public WebChunkSection(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.blocks = new BlockType[VOLUME];

        // 初始化为空气
        for (int i = 0; i < VOLUME; i++) {
            blocks[i] = BlockType.AIR;
        }
    }

    /**
     * 设置方块
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        if (isInBounds(x, y, z)) {
            blocks[index(x, y, z)] = type;
        }
    }

    /**
     * 获取方块
     */
    public BlockType getBlock(int x, int y, int z) {
        if (isInBounds(x, y, z)) {
            return blocks[index(x, y, z)];
        }
        return BlockType.AIR;
    }

    /**
     * 世界坐标转区块内坐标
     */
    private int index(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }

    private boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }

    public int getChunkX() { return chunkX; }
    public int getChunkY() { return chunkY; }
    public int getChunkZ() { return chunkZ; }

    /**
     * 获取世界坐标对应的方块
     */
    public static int worldToLocal(int worldCoord) {
        int chunk = Math.floorDiv(worldCoord, SIZE);
        return ((worldCoord % SIZE) + SIZE) % SIZE;
    }
}
