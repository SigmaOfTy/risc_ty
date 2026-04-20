
# test_lsu_store_fwd.asm

# 目的：专门验证 LSU 的 store 写入可见性，以及 store->load 紧邻读回一致性。

#

# 约定：

# - base = __dmem_base

# - 结果写到 base + 0x100 开始：

#   [0x100] = 0x7f   -> PASS

#   [0x100] = 0x01   -> FAIL test1 (sh/lhu same addr)

#   [0x100] = 0x02   -> FAIL test2 (coremark-like two counters)

#   [0x100] = 0x03   -> FAIL test3 (sh/lhu with 1 nop)

# - 失败时会把 expected/actual 写到后续地址，便于波形/内存比对。



main:

    la   x10, __dmem_base



    # 清理测试区

    sw   x0, 0(x10)

    sw   x0, 4(x10)

    sw   x0, 8(x10)

    sw   x0, 12(x10)

    sw   x0, 16(x10)



    ################################################################

    # TEST1: 同地址 sh -> 立刻 lhu

    ################################################################

    addi x20, x0, 200



t1_loop:

    lhu  x1, 0(x10)

    addi x1, x1, 1

    sh   x1, 0(x10)

    lhu  x2, 0(x10)

    bne  x1, x2, fail_t1



    addi x20, x20, -1

    bne  x20, x0, t1_loop



    ################################################################

    # TEST2: 模拟 CoreMark 风格：两个 halfword 计数器交替更新

    # 对应模式：

    #   lhu -20; +1; sh -20

    #   lhu -18; +1; sh -18

    #   lhu -18; bgeu compare

    ################################################################

    sh   x0, 2(x10)

    sh   x0, 4(x10)

    addi x21, x0, 200



t2_loop:

    lhu  x3, 4(x10)

    addi x3, x3, 1

    sh   x3, 4(x10)



    lhu  x4, 2(x10)

    addi x4, x4, 1

    sh   x4, 2(x10)



    lhu  x5, 2(x10)

    bne  x4, x5, fail_t2



    addi x21, x21, -1

    bne  x21, x0, t2_loop



    ################################################################

    # TEST3: 同地址 sh -> 1个 nop -> lhu

    # 用于区分“必须 stall/forward 才正确” vs “写入本身失败”

    ################################################################

    sh   x0, 6(x10)

    addi x22, x0, 200



t3_loop:

    lhu  x6, 6(x10)

    addi x6, x6, 1

    sh   x6, 6(x10)

    addi x0, x0, 0     # nop

    lhu  x7, 6(x10)

    bne  x6, x7, fail_t3



    addi x22, x22, -1

    bne  x22, x0, t3_loop



pass:

    addi x31, x0, 0x7f

    sw   x31, 256(x10)

    j    .



fail_t1:

    addi x31, x0, 0x01

    sw   x31, 256(x10)

    sw   x1,  260(x10)   # expected

    sw   x2,  264(x10)   # actual

    j    .



fail_t2:

    addi x31, x0, 0x02

    sw   x31, 256(x10)

    sw   x4,  260(x10)   # expected

    sw   x5,  264(x10)   # actual

    j    .



fail_t3:

    addi x31, x0, 0x03

    sw   x31, 256(x10)

    sw   x6,  260(x10)   # expected

    sw   x7,  264(x10)   # actual

    j    .