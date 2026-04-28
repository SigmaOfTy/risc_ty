ROOT_DIR := ../../..
RUNTIME_DIR := $(ROOT_DIR)/sims/runtime/bare-metal/riscv32
BUILD_DIR := build
SRC_DIR := src

CROSS_COMPILE ?= riscv64-unknown-elf-
CC := $(CROSS_COMPILE)gcc

OPT ?= -O2
EXECS ?= 7
TOTAL_DATA_SIZE ?= 2000
ITERATIONS ?= 1

TARGET := $(BUILD_DIR)/coremark-rv32im-execs$(EXECS)-size$(TOTAL_DATA_SIZE)-$(patsubst -%,%,$(OPT)).elf

SRCS := \
	$(RUNTIME_DIR)/start.S \
	$(SRC_DIR)/core_list_join.c \
	$(SRC_DIR)/core_main.c \
	$(SRC_DIR)/core_matrix.c \
	$(SRC_DIR)/core_state.c \
	$(SRC_DIR)/core_util.c \
	$(SRC_DIR)/core_portme.c \
	$(SRC_DIR)/ee_printf.c

CFLAGS := \
	$(OPT) \
	-march=rv32im_zicsr \
	-mabi=ilp32 \
	-mcmodel=medany \
	-static \
	-nostartfiles \
	-nostdlib \
	-ffreestanding \
	-fno-builtin \
	-DPERFORMANCE_RUN=1 \
	-DITERATIONS=$(ITERATIONS) \
	-DEXECS=$(EXECS) \
	-DTOTAL_DATA_SIZE=$(TOTAL_DATA_SIZE) \
	-DFLAGS_STR='"rv32im baremetal coremark"' \
	-I$(SRC_DIR)

LDFLAGS := -T $(RUNTIME_DIR)/linker.ld

.PHONY: all clean

all: $(TARGET)

$(TARGET): $(SRCS) $(SRC_DIR)/coremark.h $(SRC_DIR)/core_portme.h | $(BUILD_DIR)
	$(CC) $(CFLAGS) $(LDFLAGS) $(SRCS) -o $@

$(BUILD_DIR):
	mkdir -p $@

clean:
	rm -rf $(BUILD_DIR)
