import os

import lit.formats

config.name = 'Demu-Difftest'
config.test_format = lit.formats.ShTest(not bool(os.environ.get('LIT_USE_INTERNAL_SHELL')))
config.suffixes = ['.c', '.S', '.asm']
config.excludes = ['CMakeLists.txt', 'env']

config.available_features.add(config.arch)
config.available_features.add(config.cpu)

TOOLCHAINS = {
    "riscv32": {
        "cc": "riscv64-unknown-elf-gcc",
        "cflags": f"-march={config.cpu}_zicsr -mabi=ilp32 -mcmodel=medany -static -nostartfiles -nostdlib",
        "linker": f"{config.demu_src_root}/runtime/bare-metal/riscv32/linker.ld",
        "start": f"{config.demu_src_root}/runtime/bare-metal/riscv32/start.S"
    },
}

if config.arch not in TOOLCHAINS:
    lit_config.fatal(f"Unsupported architecture for difftest: {config.arch}")

tc = TOOLCHAINS[config.arch]

bare_c = f"{tc['cc']} {tc['cflags']} -T {tc['linker']} {tc['start']} %s -o %t.elf"
bare_asm = f"{tc['cc']} {tc['cflags']} -T {tc['linker']} -x assembler-with-cpp %s -o %t.elf"

config.substitutions.append(('%bare_c', bare_c))
config.substitutions.append(('%bare_asm', bare_asm))

difftest_cmd = f"{config.demu_exe} -R gdb %t.elf -L5"
config.substitutions.append(('%difftest', difftest_cmd))

for arch_dir in TOOLCHAINS.keys():
    if arch_dir != config.arch:
        config.excludes.append(arch_dir)
