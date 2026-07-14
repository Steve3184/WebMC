package top.steve3184.webmc.world;

/**
 * 方块类型枚举
 */
public enum BlockType {
    AIR(0),
    GRASS(1),
    DIRT(2),
    STONE(3),
    WATER(4),
    SAND(5),
    WOOD(6),
    LEAVES(7);

    private final int id;

    BlockType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static BlockType fromId(int id) {
        for (BlockType type : values()) {
            if (type.id == id) return type;
        }
        return AIR;
    }

    /**
     * 方块是否透明
     */
    public boolean isTransparent() {
        return this == AIR || this == WATER;
    }
}
