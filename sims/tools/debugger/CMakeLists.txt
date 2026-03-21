find_library(READLINE_LIB readline REQUIRED)

add_executable(demu_${ISA_TARGET}_dbg)
target_sources(demu_${ISA_TARGET}_dbg PRIVATE
  main.cpp
  debugger.cpp
  breakpoint.cpp
)
target_link_libraries(demu_${ISA_TARGET}_dbg PRIVATE
  demu
  ${READLINE_LIB}
  Threads::Threads
)

set_target_properties(demu_${ISA_TARGET}_dbg PROPERTIES
  OUTPUT_NAME demu_${ISA_TARGET}_dbg
  RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)

install(TARGETS demu_${ISA_TARGET}_dbg DESTINATION bin)
