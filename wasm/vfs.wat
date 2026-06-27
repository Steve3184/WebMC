(module
  (memory (export "memory") 256 32768)

  (global $entry_count (mut i32) (i32.const 0))
  (global $path_strings_offset (mut i32) (i32.const 0))
  (global $blob_offset (mut i32) (i32.const 0))

  (func $read_u16_le (param $off i32) (result i32)
    (i32.or
      (i32.and (i32.load8_u (local.get $off)) (i32.const 0xFF))
      (i32.shl (i32.and (i32.load8_u (i32.add (local.get $off) (i32.const 1))) (i32.const 0xFF)) (i32.const 8))
    )
  )

  (func $read_u32_le (param $off i32) (result i32)
    (i32.or
      (i32.or
        (i32.and (i32.load8_u (local.get $off)) (i32.const 0xFF))
        (i32.shl (i32.and (i32.load8_u (i32.add (local.get $off) (i32.const 1))) (i32.const 0xFF)) (i32.const 8))
      )
      (i32.or
        (i32.shl (i32.and (i32.load8_u (i32.add (local.get $off) (i32.const 2))) (i32.const 0xFF)) (i32.const 16))
        (i32.shl (i32.and (i32.load8_u (i32.add (local.get $off) (i32.const 3))) (i32.const 0xFF)) (i32.const 24))
      )
    )
  )

  (func $to_lower (param $c i32) (result i32)
    (if (result i32) (i32.and (i32.ge_u (local.get $c) (i32.const 65)) (i32.le_u (local.get $c) (i32.const 90)))
      (then (i32.add (local.get $c) (i32.const 32)))
      (else (local.get $c))
    )
  )

  (func $hash_path (param $path_off i32) (param $path_len i32) (result i32)
    (local $h i32)
    (local $i i32)
    (local $c i32)
    (local.set $h (i32.const 5381))
    (local.set $i (i32.const 0))
    (block $break
      (loop $loop
        (br_if $break (i32.ge_u (local.get $i) (local.get $path_len)))
        (local.set $c (i32.load8_u (i32.add (local.get $path_off) (local.get $i))))
        (local.set $c (call $to_lower (local.get $c)))
        (local.set $h (i32.add (i32.shl (local.get $h) (i32.const 5)) (local.get $h)))
        (local.set $h (i32.add (local.get $h) (local.get $c)))
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        (br $loop)
      )
    )
    (i32.and (local.get $h) (i32.const 0x1FFF))
  )

  (func (export "vfs_parse") (param $data_off i32) (param $data_len i32) (result i32)
    (local $pos i32)
    (local $count i32)
    (local $i i32)
    (local $path_len i32)
    (local $data_len2 i32)
    (local $path_off i32)
    (local $entry_base i32)
    (local $hash_val i32)
    (local $h_off i32)
    (local $has_slash i32)
    (local $j i32)
    (local $c i32)

    ;; Check magic "MCVF"
    (if (i32.ne (i32.load8_u (local.get $data_off)) (i32.const 77))
      (then (return (i32.const -1)))
    )
    (if (i32.ne (i32.load8_u (i32.add (local.get $data_off) (i32.const 1))) (i32.const 67))
      (then (return (i32.const -1)))
    )
    (if (i32.ne (i32.load8_u (i32.add (local.get $data_off) (i32.const 2))) (i32.const 86))
      (then (return (i32.const -1)))
    )
    (if (i32.ne (i32.load8_u (i32.add (local.get $data_off) (i32.const 3))) (i32.const 70))
      (then (return (i32.const -1)))
    )

    ;; Check version = 1
    (if (i32.ne (call $read_u32_le (i32.add (local.get $data_off) (i32.const 4))) (i32.const 1))
      (then (return (i32.const -3)))
    )

    ;; Read count
    (local.set $count (call $read_u32_le (i32.add (local.get $data_off) (i32.const 8))))
    (if (i32.gt_u (local.get $count) (i32.const 25000))
      (then (return (i32.const -4)))
    )

    ;; Store blob_offset for later data access
    (global.set $blob_offset (local.get $data_off))

    ;; Clear hash table (8192 entries * 4 bytes = 32768 bytes, starting at offset 65536)
    ;; Entries array starts at offset 65536 + 32768 = 98304
    ;; Each entry: path_offset(4) + path_len(2pad4) + data_offset(4) + data_len(4) + next(4) = 20 bytes
    ;; Path strings start at 98304 + 25000*20 = 598304

    ;; Clear hash table
    (local.set $i (i32.const 0))
    (block $clr_brk
      (loop $clr_loop
        (br_if $clr_brk (i32.ge_u (local.get $i) (i32.const 8192)))
        (i32.store (i32.add (i32.const 65536) (i32.shl (local.get $i) (i32.const 2))) (i32.const 0))
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        (br $clr_loop)
      )
    )

    ;; Parse entries
    (local.set $pos (i32.add (local.get $data_off) (i32.const 12)))
    (local.set $i (i32.const 0))
    (global.set $path_strings_offset (i32.const 598304))
    (global.set $entry_count (i32.const 0))

    (block $parse_brk
      (loop $parse_loop
        (br_if $parse_brk (i32.ge_u (local.get $i) (local.get $count)))

        ;; Read pathLen
        (local.set $path_len (call $read_u16_le (local.get $pos)))
        (local.set $pos (i32.add (local.get $pos) (i32.const 2)))

        ;; Copy and normalize path to path_strings area
        (local.set $path_off (local.get $pos))
        (local.set $has_slash (i32.eqz (i32.ne (i32.load8_u (local.get $path_off)) (i32.const 47))))

        ;; Write normalized path
        (if (i32.eqz (local.get $has_slash))
          (then
            (i32.store8 (global.get $path_strings_offset) (i32.const 47))
            (global.set $path_strings_offset (i32.add (global.get $path_strings_offset) (i32.const 1)))
          )
        )

        ;; Copy path bytes with to_lower
        (local.set $j (i32.const 0))
        (block $cp_brk
          (loop $cp_loop
            (br_if $cp_brk (i32.ge_u (local.get $j) (local.get $path_len)))
            (local.set $c (call $to_lower (i32.load8_u (i32.add (local.get $path_off) (local.get $j)))))
            (i32.store8 (global.get $path_strings_offset) (local.get $c))
            (global.set $path_strings_offset (i32.add (global.get $path_strings_offset) (i32.const 1)))
            (local.set $j (i32.add (local.get $j) (i32.const 1)))
            (br $cp_loop)
          )
        )

        (local.set $pos (i32.add (local.get $pos) (local.get $path_len)))

        ;; Read dataLen
        (local.set $data_len2 (call $read_u32_le (local.get $pos)))
        (local.set $pos (i32.add (local.get $pos) (i32.const 4)))

        ;; Store entry: entry_base = 98304 + global.entry_count * 20
        (local.set $entry_base (i32.add (i32.const 98304) (i32.mul (global.get $entry_count) (i32.const 20))))

        ;; path_offset in path_strings (relative to memory start)
        ;; We store the start of path_strings for this entry
        ;; Actually we need to store the offset within path_strings area
        ;; For simplicity, store absolute memory offset

        ;; Reconstruct path start: we need to recalculate
        ;; path_start = current path_strings_offset - (path_len + (has_slash ? 0 : 1))
        ;; But we already advanced path_strings_offset. Let's compute it differently.

        ;; Actually, let's store entry data:
        ;; entry.path_offset = path_strings start for this entry
        ;; entry.path_len = path_len + (has_slash ? 0 : 1)
        ;; entry.data_offset = pos (absolute in memory)
        ;; entry.data_len = data_len2
        ;; entry.next = 0

        ;; We need the path start offset. Let's compute it:
        ;; path_start = path_strings_offset - total_path_len
        ;; where total_path_len = path_len + (has_slash ? 0 : 1)

        (i32.store (local.get $entry_base)
          (i32.sub (global.get $path_strings_offset)
            (i32.add (local.get $path_len)
              (if (result i32) (i32.eqz (local.get $has_slash))
                (then (i32.const 1))
                (else (i32.const 0))
              )
            )
          )
        )

        ;; path_len (with leading slash added)
        (i32.store (i32.add (local.get $entry_base) (i32.const 4))
          (i32.add (local.get $path_len)
            (if (result i32) (i32.eqz (local.get $has_slash))
              (then (i32.const 1))
              (else (i32.const 0))
            )
          )
        )

        ;; data_offset
        (i32.store (i32.add (local.get $entry_base) (i32.const 8)) (local.get $pos))

        ;; data_len
        (i32.store (i32.add (local.get $entry_base) (i32.const 12)) (local.get $data_len2))

        ;; Hash and link
        (local.set $hash_val
          (call $hash_path
            (i32.load (local.get $entry_base))
            (i32.load (i32.add (local.get $entry_base) (i32.const 4)))
          )
        )
        (local.set $h_off (i32.add (i32.const 65536) (i32.shl (local.get $hash_val) (i32.const 2))))

        ;; entry.next = hash_table[hash_val]
        (i32.store (i32.add (local.get $entry_base) (i32.const 16)) (i32.load (local.get $h_off)))

        ;; hash_table[hash_val] = entry_count + 1 (1-based)
        (i32.store (local.get $h_off) (i32.add (global.get $entry_count) (i32.const 1)))

        (global.set $entry_count (i32.add (global.get $entry_count) (i32.const 1)))

        (local.set $pos (i32.add (local.get $pos) (local.get $data_len2)))
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        (br $parse_loop)
      )
    )

    (global.get $entry_count)
  )

  (func (export "vfs_find") (param $path_off i32) (param $path_len i32) (result i32)
    (local $hash_val i32)
    (local $bucket i32)
    (local $idx i32)
    (local $entry_base i32)
    (local $ep_off i32)
    (local $ep_len i32)
    (local $j i32)
    (local $match i32)

    (local.set $hash_val (call $hash_path (local.get $path_off) (local.get $path_len)))
    (local.set $bucket (i32.load (i32.add (i32.const 65536) (i32.shl (local.get $hash_val) (i32.const 2)))))

    (block $find_brk
      (loop $find_loop
        (br_if $find_brk (i32.eqz (local.get $bucket)))

        (local.set $idx (i32.sub (local.get $bucket) (i32.const 1)))
        (local.set $entry_base (i32.add (i32.const 98304) (i32.mul (local.get $idx) (i32.const 20))))

        (local.set $ep_off (i32.load (local.get $entry_base)))
        (local.set $ep_len (i32.load (i32.add (local.get $entry_base) (i32.const 4))))

        (if (i32.eq (local.get $ep_len) (local.get $path_len))
          (then
            ;; Compare bytes with to_lower on input path
            (local.set $match (i32.const 1))
            (local.set $j (i32.const 0))
            (block $cmp_brk
              (loop $cmp_loop
                (br_if $cmp_brk (i32.ge_u (local.get $j) (local.get $path_len)))
                (if (i32.ne
                      (call $to_lower (i32.load8_u (i32.add (local.get $path_off) (local.get $j))))
                      (i32.load8_u (i32.add (local.get $ep_off) (local.get $j)))
                    )
                  (then
                    (local.set $match (i32.const 0))
                    (br $cmp_brk)
                  )
                )
                (local.set $j (i32.add (local.get $j) (i32.const 1)))
                (br $cmp_loop)
              )
            )
            (if (local.get $match) (then (return (local.get $idx))))
          )
        )

        (local.set $bucket (i32.load (i32.add (local.get $entry_base) (i32.const 16))))
        (br $find_loop)
      )
    )

    (i32.const -1)
  )

  (func (export "vfs_get_data_offset") (param $idx i32) (result i32)
    (if (result i32) (i32.lt_s (local.get $idx) (i32.const 0))
      (then (i32.const 0))
      (else
        (if (result i32) (i32.ge_u (local.get $idx) (global.get $entry_count))
          (then (i32.const 0))
          (else (i32.load (i32.add (i32.add (i32.const 98304) (i32.mul (local.get $idx) (i32.const 20))) (i32.const 8))))
        )
      )
    )
  )

  (func (export "vfs_get_data_len") (param $idx i32) (result i32)
    (if (result i32) (i32.lt_s (local.get $idx) (i32.const 0))
      (then (i32.const 0))
      (else
        (if (result i32) (i32.ge_u (local.get $idx) (global.get $entry_count))
          (then (i32.const 0))
          (else (i32.load (i32.add (i32.add (i32.const 98304) (i32.mul (local.get $idx) (i32.const 20))) (i32.const 12))))
        )
      )
    )
  )

  (func (export "vfs_get_entry_count") (result i32)
    (global.get $entry_count)
  )
)
