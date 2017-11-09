cmake_minimum_required(VERSION 2.8.12)
project(simulation)

add_subdirectory(pybind11)
pybind11_add_module(simulation convolve_py.cc)
