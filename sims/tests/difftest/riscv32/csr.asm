// RUN: %bare_asm
// RUN: %difftest -c 200 -SLT

.section .text.entry, "ax"
.globl main

main:
    li    x31, 0

    li    x1, 0x12
    csrrw x2, 0x300, x1
    bne   x2, x0, fail
    csrr  x3, 0x300
    bne   x3, x1, fail

    li    x4, 0x05
    csrrs x5, 0x300, x4
    bne   x5, x1, fail
    csrr  x6, 0x300
    li    t0, 0x17
    bne   x6, t0, fail

    li    x4, 0x03
    csrrc x7, 0x300, x4
    li    t0, 0x17
    bne   x7, t0, fail
    csrr  x8, 0x300
    li    t0, 0x14
    bne   x8, t0, fail

    csrrwi x9, 0x300, 1
    li     t0, 0x14
    bne    x9, t0, fail
    csrr   x10, 0x300
    li     t0, 1
    bne    x10, t0, fail

    csrrsi x11, 0x300, 2
    li     t0, 1
    bne    x11, t0, fail
    csrr   x12, 0x300
    li     t0, 3
    bne    x12, t0, fail

    csrrci x13, 0x300, 1
    li     t0, 3
    bne    x13, t0, fail
    csrr   x14, 0x300
    li     t0, 2
    bne    x14, t0, fail

    csrr   x15, 0xB00

    la     x16, after_mret
    csrw   0x341, x16
    mret
    j      fail

after_mret:
    j      done

fail:
    li     x31, 1

done:
    j      .
