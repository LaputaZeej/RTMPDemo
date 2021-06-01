
cmake_minimum_required(VERSION 3.10.2)


include_directories(${CMAKE_SOURCE_DIR}/librtmp)
add_subdirectory(librtmp)
add_library(
        native-lib
        SHARED
        native-lib.cpp
        JavaCallOnStateChangedCallback.cpp
        JavaCallHelper.cpp
        VideoChannel.cpp
        AudioChannel.cpp
)

#或者使用SROUCE一次性导入
#AUX_SOURCE_DIRECTORY(. SOURCE)
#add_library(
#        native-lib
#        SHARED
#        ${SOURCE}
#        )


include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)
#CXXFLAGS
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")

#推流
target_link_libraries(
        native-lib
        x264
         faac
        rtmp
        log)


# 图像处理操作
include_directories(${CMAKE_SOURCE_DIR}/libyuv/include)
add_subdirectory(libyuv)
add_library(
        ImageUtils
        SHARED
        ImageUtils.cpp)

target_link_libraries(
        ImageUtils
        yuv)
