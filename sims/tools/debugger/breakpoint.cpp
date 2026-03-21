#include "breakpoint.hh"
#include <fmt/format.h>

namespace demu::dbg {

uint32_t BreakpointManager::add_breakpoint(BreakType type, uint64_t value) {
  uint32_t id = next_id_++;
  breakpoints_[id] = {id, type, value, true};
  return id;
}

uint32_t BreakpointManager::add_watchpoint(uint32_t addr, uint32_t size,
                                           WatchType type) {
  uint32_t id = next_id_++;
  watchpoints_[id] = {id, addr, size, type, true};
  return id;
}

bool BreakpointManager::remove(uint32_t id) {
  if (breakpoints_.erase(id))
    return true;
  if (watchpoints_.erase(id))
    return true;
  return false;
}

bool BreakpointManager::enable(uint32_t id) {
  auto bit = breakpoints_.find(id);
  if (bit != breakpoints_.end()) {
    bit->second.enabled = true;
    return true;
  }
  auto wit = watchpoints_.find(id);
  if (wit != watchpoints_.end()) {
    wit->second.enabled = true;
    return true;
  }
  return false;
}

bool BreakpointManager::disable(uint32_t id) {
  auto bit = breakpoints_.find(id);
  if (bit != breakpoints_.end()) {
    bit->second.enabled = false;
    return true;
  }
  auto wit = watchpoints_.find(id);
  if (wit != watchpoints_.end()) {
    wit->second.enabled = false;
    return true;
  }
  return false;
}

bool BreakpointManager::check_breakpoint(uint64_t pc, uint64_t cycle,
                                         uint64_t instret) const {
  for (const auto &[id, bp] : breakpoints_) {
    if (!bp.enabled)
      continue;
    switch (bp.type) {
    case BreakType::PC:
      if (pc == bp.value)
        return true;
      break;
    case BreakType::CYCLE:
      if (cycle >= bp.value)
        return true;
      break;
    case BreakType::INSTRET:
      if (instret >= bp.value)
        return true;
      break;
    }
  }
  return false;
}

bool BreakpointManager::check_watchpoint_read(uint32_t addr,
                                              uint32_t size) const {
  for (const auto &[id, wp] : watchpoints_) {
    if (!wp.enabled)
      continue;
    if (wp.type == WatchType::WRITE)
      continue;
    if (ranges_overlap(addr, size, wp.address, wp.size))
      return true;
  }
  return false;
}

bool BreakpointManager::check_watchpoint_write(uint32_t addr,
                                               uint32_t size) const {
  for (const auto &[id, wp] : watchpoints_) {
    if (!wp.enabled)
      continue;
    if (wp.type == WatchType::READ)
      continue;
    if (ranges_overlap(addr, size, wp.address, wp.size))
      return true;
  }
  return false;
}

bool BreakpointManager::ranges_overlap(uint32_t a_start, uint32_t a_size,
                                       uint32_t b_start,
                                       uint32_t b_size) const {
  return a_start < (b_start + b_size) && b_start < (a_start + a_size);
}

std::string BreakpointManager::list() const {
  if (breakpoints_.empty() && watchpoints_.empty())
    return "No breakpoints or watchpoints set.\n";

  std::string result;

  for (const auto &[id, bp] : breakpoints_) {
    const char *type_str = "???";
    switch (bp.type) {
    case BreakType::PC:
      type_str = "PC";
      break;
    case BreakType::CYCLE:
      type_str = "CYCLE";
      break;
    case BreakType::INSTRET:
      type_str = "INSTRET";
      break;
    }
    result += fmt::format("  #{:<4d} {:>7s} = 0x{:x}  [{}]\n", id, type_str,
                          bp.value, bp.enabled ? "enabled" : "disabled");
  }

  for (const auto &[id, wp] : watchpoints_) {
    const char *type_str = "???";
    switch (wp.type) {
    case WatchType::READ:
      type_str = "r";
      break;
    case WatchType::WRITE:
      type_str = "w";
      break;
    case WatchType::READWRITE:
      type_str = "rw";
      break;
    }
    result += fmt::format("  #{:<4d} WATCH 0x{:08x} size={} type={}  [{}]\n",
                          id, wp.address, wp.size, type_str,
                          wp.enabled ? "enabled" : "disabled");
  }

  return result;
}

} // namespace demu::dbg
