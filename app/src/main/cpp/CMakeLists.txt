# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

include_directories(include)

#${CMAKE_SOURCE_DIR}=D:/Android/UT/demo/FFmpegPlayer/app/src/main/cpp/
set(DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

project("native-lib")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
file(GLOB source ${CMAKE_SOURCE_DIR}/include/libcustom/*.cpp)


#1
add_library(native-lib SHARED ${source} native-lib.cpp)
#2
add_library(avcodec SHARED IMPORTED)

set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${DIR}/libavcodec.so)
#3
add_library(avfilter SHARED IMPORTED)

set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION ${DIR}/libavfilter.so)
#4
add_library(avformat SHARED IMPORTED)

set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${DIR}/libavformat.so)
#5
add_library(avutil SHARED IMPORTED)

set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${DIR}/libavutil.so)

#6
add_library(postproc SHARED IMPORTED)

set_target_properties(postproc PROPERTIES IMPORTED_LOCATION ${DIR}/libpostproc.so)
#7
add_library(swresample SHARED IMPORTED)

set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${DIR}/libswresample.so)
#8
add_library(swscale SHARED IMPORTED)

set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${DIR}/libswscale.so)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in_queue the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(log-lib log)
#find_library(OpenGLES3 GLESv3)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in_queue this
# build script, prebuilt third-party libraries, or system libraries.
#不加log,不能打印android日志
target_link_libraries(
        native-lib
        avcodec
        avfilter
        avformat
        avutil
        postproc
        swresample
        swscale
        OpenSLES
        log
        android
        ${log-lib}
)

#set_property(TARGET native-lib PROPERTY IMPORTED_NO_SONAME 1)




