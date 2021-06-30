
cmake_minimum_required(VERSION 3.10.2)


include_directories(${CMAKE_SOURCE_DIR}/librtmp)
add_subdirectory(librtmp)
add_library(
        native-lib
        SHARED
        native-lib.cpp
        H264.cpp
        h264_decode_sps.h
)

target_link_libraries(
        native-lib
        rtmp
        log)
