#include <stdint.h>

#define MAX_ENTRIES 25000
#define HASH_SIZE 8192
#define SCRATCH_OFF 0x10000
#define HASH_OFF 0x18000
#define ENTRY_OFF 0x20000
#define ENTRY_STRIDE 16

uint32_t g_count = 0;
uint32_t g_break_reason = 0;
uint32_t g_last_pos = 0;
uint32_t g_last_path_cur = 0;

static uint32_t u32le(uint8_t* p) {
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

static uint16_t u16le(uint8_t* p) {
    return (uint16_t)p[0] | ((uint16_t)p[1] << 8);
}

static uint8_t lc(uint8_t c) {
    return (c >= 'A' && c <= 'Z') ? (c + 32) : c;
}

int32_t vfs_parse(uint32_t data_off, uint32_t size) {
    g_count = 0;
    g_break_reason = 0;
    g_last_pos = 0;
    g_last_path_cur = 0;

    if (size < 12) return -1;

    uint8_t* data = (uint8_t*)data_off;

    if (data[0] != 'M' || data[1] != 'C' || data[2] != 'V' || data[3] != 'F') return -2;

    uint32_t ver = u32le(data + 4);
    if (ver != 1) return -3;

    uint32_t count = u32le(data + 8);
    if (count > MAX_ENTRIES) return -4;

    uint8_t* mem = (uint8_t*)0;

    for (uint32_t i = 0; i < HASH_SIZE; i++) {
        *((uint32_t*)(mem + HASH_OFF + i * 4)) = 0;
    }

    uint32_t path_base = ENTRY_OFF + MAX_ENTRIES * ENTRY_STRIDE;
    uint32_t path_cur = path_base;

    uint32_t pos = 12;

    for (uint32_t i = 0; i < count && pos < size; i++) {
        if (pos + 2 > size) { g_break_reason = 1; break; }
        uint32_t pl = u16le(data + pos);
        pos += 2;
        if (pos + pl > size || pos + 4 > size) { g_break_reason = 2; break; }

        uint8_t slash = data[pos] == '/' ? 1 : 0;
        uint32_t epl = pl + (slash ? 0 : 1);

        if (path_cur + epl >= 0x80000000) { g_break_reason = 3; g_last_pos = path_cur; break; }

        uint32_t po = path_cur;

        if (!slash) mem[po++] = '/';
        for (uint32_t j = 0; j < pl; j++) mem[po + j] = lc(data[pos + j]);
        path_cur = po + pl;
        pos += pl;

        if (pos + 4 > size) { g_break_reason = 4; break; }
        uint32_t dl = u32le(data + pos);
        pos += 4;
        if (pos + dl > size) { g_break_reason = 5; break; }

        uint32_t eo = ENTRY_OFF + g_count * ENTRY_STRIDE;
        *((uint32_t*)(mem + eo + 0)) = slash ? po : (po - 1);
        *((uint32_t*)(mem + eo + 4)) = epl;
        *((uint32_t*)(mem + eo + 8)) = data_off + pos;
        *((uint32_t*)(mem + eo + 12)) = dl;

        uint32_t h = 5381;
        uint32_t hb = slash ? po : (po - 1);
        for (uint32_t k = 0; k < epl; k++) h = ((h << 5) + h) + lc(mem[hb + k]);
        h = h & (HASH_SIZE - 1);
        uint32_t hbucket = HASH_OFF + h * 4;
        *((uint32_t*)(mem + eo + 16)) = *((uint32_t*)(mem + hbucket));
        *((uint32_t*)(mem + hbucket)) = g_count + 1;

        g_count++;
        g_last_path_cur = path_cur;
        pos += dl;
    }

    g_last_pos = pos;

    if (pos >= size) g_break_reason = 0;

    return (int32_t)g_count;
}

int32_t vfs_find(uint32_t path_off, uint16_t path_len) {
    uint8_t* mem = (uint8_t*)0;
    uint32_t so = SCRATCH_OFF;
    uint32_t sl = 0;
    uint8_t* path = (uint8_t*)path_off;

    if (path_len > 0 && path[0] != '/') {
        mem[so++] = '/';
    }

    for (uint32_t i = 0; i < path_len; i++) {
        mem[so + sl] = lc(path[i]);
        sl++;
    }

    uint32_t h = 5381;
    for (uint32_t i = 0; i < sl; i++) h = ((h << 5) + h) + lc(mem[so + i]);
    h = h & (HASH_SIZE - 1);
    uint32_t b = *((uint32_t*)(mem + HASH_OFF + h * 4));

    while (b != 0) {
        uint32_t idx = b - 1;
        uint32_t eo = ENTRY_OFF + idx * ENTRY_STRIDE;
        uint32_t ep_off = *((uint32_t*)(mem + eo + 0));
        uint32_t ep_len = *((uint32_t*)(mem + eo + 4));

        if (ep_len == sl) {
            uint8_t ok = 1;
            for (uint32_t i = 0; i < sl; i++) {
                if (mem[ep_off + i] != mem[so + i]) { ok = 0; break; }
            }
            if (ok) return (int32_t)idx;
        }
        b = *((uint32_t*)(mem + eo + 16));
    }
    return -1;
}

uint32_t vfs_get_data_off(int32_t idx) {
    uint8_t* mem = (uint8_t*)0;
    if (idx < 0 || (uint32_t)idx >= g_count) return 0;
    return *((uint32_t*)(mem + ENTRY_OFF + (uint32_t)idx * ENTRY_STRIDE + 8));
}

uint32_t vfs_get_data_len(int32_t idx) {
    uint8_t* mem = (uint8_t*)0;
    if (idx < 0 || (uint32_t)idx >= g_count) return 0;
    return *((uint32_t*)(mem + ENTRY_OFF + (uint32_t)idx * ENTRY_STRIDE + 12));
}

uint32_t vfs_count(void) {
    return g_count;
}

uint32_t vfs_debug_info(uint32_t idx) {
    if (idx == 0) return g_count;
    if (idx == 1) return g_break_reason;
    if (idx == 2) return g_last_pos;
    if (idx == 3) return g_last_path_cur;
    if (idx == 4) return ENTRY_OFF + MAX_ENTRIES * ENTRY_STRIDE;
    return 0;
}

uint32_t vfs_get_debug2(uint32_t entry_idx, uint32_t field_idx) {
    uint8_t* mem = (uint8_t*)0;
    uint32_t eo = ENTRY_OFF + entry_idx * ENTRY_STRIDE;
    if (field_idx == 0) return *((uint32_t*)(mem + eo + 0));
    if (field_idx == 1) return *((uint32_t*)(mem + eo + 4));
    if (field_idx == 2) return *((uint32_t*)(mem + eo + 8));
    if (field_idx == 3) return *((uint32_t*)(mem + eo + 12));
    if (field_idx == 4) return *((uint32_t*)(mem + eo + 16));
    return 0;
}

uint32_t vfs_get_path(uint32_t idx) {
    uint8_t* mem = (uint8_t*)0;
    uint32_t eo = ENTRY_OFF + idx * ENTRY_STRIDE;
    uint32_t ep_off = *((uint32_t*)(mem + eo + 0));
    uint32_t ep_len = *((uint32_t*)(mem + eo + 4));
    return (ep_off << 16) | (ep_len & 0xFFFF);
}
