#pragma once

#include <cstdint>
#include <map>
#include <string>

namespace demu::dbg {

enum class BreakType { PC, CYCLE, INSTRET };
enum class WatchType { READ, WRITE, READWRITE };

struct Breakpoint {
  uint32_t id;
  BreakType type;
  uint64_t value;
  bool enabled;
};

struct Watchpoint {
  uint32_t id;
  uint32_t address;
  uint32_t size;
  WatchType type;
  bool enabled;
};

class BreakpointManager {
public:
  uint32_t add_breakpoint(BreakType type, uint64_t value);
  uint32_t add_watchpoint(uint32_t addr, uint32_t size, WatchType type);

  bool remove(uint32_t id);
  bool enable(uint32_t id);
  bool disable(uint32_t id);

  bool check_breakpoint(uint64_t pc, uint64_t cycle, uint64_t instret) const;
  bool check_watchpoint_read(uint32_t addr, uint32_t size) const;
  bool check_watchpoint_write(uint32_t addr, uint32_t size) const;

  std::string list() const;

private:
  uint32_t next_id_{1};
  std::map<uint32_t, Breakpoint> breakpoints_;
  std::map<uint32_t, Watchpoint> watchpoints_;

  bool ranges_overlap(uint32_t a_start, uint32_t a_size, uint32_t b_start,
                      uint32_t b_size) const;
};

} // namespace demu::dbg
