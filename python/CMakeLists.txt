cmake_minimum_required(VERSION 2.8.12)
project(simulation)

# verilator library
set(VERILATOR_DIRECTORY /usr/local/share/verilator)
set(VERILATOR_SRCS  ${VERILATOR_DIRECTORY}/include/verilated.cpp
                    ${VERILATOR_DIRECTORY}/include/verilated_save.cpp
                    ${VERILATOR_DIRECTORY}/include/verilated_vcd_c.cpp
                    ${VERILATOR_DIRECTORY}/include/verilated_vpi.cpp)

include_directories(${VERILATOR_DIRECTORY}/include ${VERILATOR_DIRECTORY}/include/vltstd)
add_library(verilator STATIC ${VERILATOR_SRCS})

# verilated code
set(VERILATED_DIRECTORY obj_dir)
set(VERILOG_DIRECTORY ${PROJECT_SOURCE_DIR}/../build)
set(VERILOG_SRCS ${VERILOG_DIRECTORY}/simple_convolution.v)

add_custom_command(
    OUTPUT generated/Vsimple_convolution.cpp
    COMMAND ${CMAKE_COMMAND} -E make_directory generated
    COMMAND verilator ${VERILATOR_ARGS}
        -Mdir generated
        --cc ${VERILOG_SRCS}
    COMMAND ${CMAKE_COMMAND} -E remove generated/*.mk
    COMMAND ${CMAKE_COMMAND} -E remove generated/*.d
    COMMAND ${CMAKE_COMMAND} -E remove generated/*.dat
    MAIN_DEPENDENCY ${VERILOG_SRCS}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    COMMENT "Generating C++ code for ${VERILOG_SRCS}"
    )
include_directories(${PROJECT_SOURCE_DIR}/generated)

set(MODULE_SRCS
    ${PROJECT_SOURCE_DIR}/convolve_py.cc
    ${PROJECT_SOURCE_DIR}/generated/Vsimple_convolution.cpp
    ${PROJECT_SOURCE_DIR}/generated/Vsimple_convolution__Syms.cpp)

add_subdirectory(pybind11)

add_library(simulation MODULE ${MODULE_SRCS})
target_link_libraries(simulation PRIVATE pybind11::module verilator)
set_target_properties(simulation PROPERTIES PREFIX "${PYTHON_MODULE_PREFIX}"
                                         SUFFIX "${PYTHON_MODULE_EXTENSION}")
