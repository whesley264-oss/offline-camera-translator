cmake_minimum_required(VERSION 3.22.1)
project("translation_engine")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -Wall -std=c99")

add_library(
        translation_engine
        SHARED
        translation_engine.c
)

find_library(log-lib log)
target_link_libraries(translation_engine ${log-lib})