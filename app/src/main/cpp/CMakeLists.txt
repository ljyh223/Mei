cmake_minimum_required(VERSION 3.22.1)
project(mei_beatnet LANGUAGES CXX)

add_library(mei_beatnet SHARED beatnet_inference.cpp)
target_compile_features(mei_beatnet PRIVATE cxx_std_17)
target_compile_options(mei_beatnet PRIVATE -O3 -fno-math-errno)
