cmake_minimum_required(VERSION 3.18.1)
project("j2me_native")

# Add SDL2 submodule
set(SDL_SHARED OFF CACHE BOOL "Build a shared version of the library" FORCE)
set(SDL_STATIC ON CACHE BOOL "Build a static version of the library" FORCE)
add_subdirectory(sdl2)

# Default Android log library for debug printing
find_library(log-lib log)

# Create a shared library (.so) from our C++ code
add_library(j2me_native SHARED
    bridge/android_jni_bridge.cpp
)

# Link the libraries
target_include_directories(j2me_native PRIVATE sdl2/include)
target_link_libraries(j2me_native PRIVATE ${log-lib} android SDL2-static)
