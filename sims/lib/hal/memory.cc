#include "demu/hal/memory.hh"
#include <cstring>
#include <fstream>
#include <iomanip>
#include <sstream>

namespace demu::hal {

Memory::Memory(size_t size, addr_t base_addr)
    : memory_(size, 0), base_addr_(base_addr) {
  HAL_INFO("Memory initialized: Size={} bytes, BaseAddr=0x{:08x}", size,
           base_addr);
}

// Helpers
bool Memory::load_binary(const std::string &filename, addr_t offset) {
  std::ifstream file(filename, std::ios::binary | std::ios::ate);
  if (!file.is_open()) {
    HAL_ERROR("Failed to open binary file: {}", filename);
    return false;
  }

  std::streamsize size = file.tellg();
  file.seekg(0, std::ios::beg);

  if (offset + size > memory_.size()) {
    HAL_ERROR("Binary file ({}) too large for memory (Size: {}, Available: {})",
              filename, size, memory_.size() - offset);
    return false;
  }

  std::vector<char> buffer(size);
  if (file.read(buffer.data(), size)) {
    for (size_t i = 0; i < (size_t)buffer.size(); i++) {
      memory_[offset + i] = static_cast<byte_t>(buffer[i]);
    }
    HAL_INFO("Successfully loaded binary '{}' ({} bytes) at offset 0x{:08x}",
             filename, size, offset);
    return true;
  }

  HAL_ERROR("Read error while loading binary: {}", filename);
  return false;
}

void Memory::clear() {
  HAL_DEBUG("Memory cleared (zeroed)");
  memset(memory_.data(), 0, memory_.size());
}

void Memory::dump(addr_t start, addr_t length) const {
  HAL_INFO("Memory Dump [0x{:08x} - 0x{:08x}]:", start, start + length);
  for (addr_t addr = start; addr < start + length; addr += 16) {
    std::stringstream ss;
    ss << std::hex << std::setw(8) << std::setfill('0') << addr << ": ";

    for (size_t i = 0; i < 16 && addr + i < start + length; i++) {
      if (i == 8)
        ss << " ";
      ss << std::hex << std::setw(2) << std::setfill('0')
         << (int)read_byte(addr + i) << " ";
    }

    ss << " |";
    for (size_t i = 0; i < 16 && addr + i < start + length; i++) {
      byte_t c = read_byte(addr + i);
      ss << (c >= 32 && c < 127 ? (char)c : '.');
    }
    ss << "|";

    HAL_INFO("{}", ss.str());
  }
}

[[nodiscard]] bool Memory::is_valid_addr(addr_t addr) const noexcept {
  return addr >= base_addr_ &&
         (addr - base_addr_) < static_cast<addr_t>(memory_.size());
}

[[nodiscard]] addr_t Memory::to_offset(addr_t addr) const noexcept {
  return addr - base_addr_;
}

} // namespace demu::hal
