cmake_minimum_required(VERSION 3.14.2)
project(native_loader)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(native_loader SHARED main.cpp)

target_compile_options(native_loader PRIVATE -Wall -Werror)

target_link_libraries(native_loader
    android log)
