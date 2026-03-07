find_package(Threads REQUIRED)
find_package(spdlog CONFIG REQUIRED)
find_package(verilator HINTS $ENV{VERILATOR_ROOT})
find_package(Protobuf REQUIRED)
find_package(absl REQUIRED)

if(NOT verilator_FOUND)
  message(FATAL_ERROR "Verilator not found. Please install Verilator or set VERILATOR_ROOT")
else()
  set(VERILATOR_ARGS
    -Wall
    -Wno-WIDTH
    -Wno-UNUSED
    -Wno-UNOPTFLAT
    -Wno-DECLFILENAME
    -Wno-PINCONNECTEMPTY
  )

  if(${ISA} STREQUAL "rv32i")
    set(__ISA_RV32I__ TRUE CACHE INTERNAL "rv32i is available")
    add_compile_definitions(__ISA_RV32I__)
  else()
    message(FATAL_ERROR "Unsupported ISA: ${ISA}. Supported ISAs: rv32i")
  endif()

  if(ENABLE_TRACE)
    list(APPEND VERILATOR_ARGS --trace)
    add_definitions(-DENABLE_TRACE)
  endif()

  if(ENABLE_COVERAGE)
    list(APPEND VERILATOR_ARGS --coverage)
  endif()

  if(ENABLE_SYSTEM)
    add_compile_definitions(ENABLE_SYSTEM)
  endif()

endif()

get_filename_component(PROTO_ROOT
  "${CMAKE_CURRENT_SOURCE_DIR}/../proto"
  ABSOLUTE
)
set(PROTO_FILE "${PROTO_ROOT}/risc_config.proto")
set(PROTO_OUT  "${CMAKE_BINARY_DIR}/generated")

if(NOT EXISTS "${PROTO_FILE}")
  message(FATAL_ERROR
    "[proto] File not found: ${PROTO_FILE}\n"
    "Run 'sbt run' first to generate proto/risc_config.proto.")
endif()

file(MAKE_DIRECTORY "${PROTO_OUT}")

add_custom_command(
  OUTPUT
    "${PROTO_OUT}/risc_config.pb.h"
    "${PROTO_OUT}/risc_config.pb.cc"
  COMMAND
    ${Protobuf_PROTOC_EXECUTABLE}
    --cpp_out=${PROTO_OUT}
    --proto_path=${PROTO_ROOT}
    ${PROTO_FILE}
  DEPENDS "${PROTO_FILE}"
  COMMENT "[protoc] Generating risc_config.pb.h / risc_config.pb.cc"
)

add_custom_target(gen_proto DEPENDS
  "${PROTO_OUT}/risc_config.pb.h"
  "${PROTO_OUT}/risc_config.pb.cc"
)

add_subdirectory(${CMAKE_SOURCE_DIR}/third-party/json)
