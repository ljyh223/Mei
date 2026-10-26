# Cargo owns USB code generation and linking; no C/C++/ASM anchor is compiled.
function(mei_add_usb_rust)
    find_program(MEI_CARGO_EXECUTABLE cargo HINTS "$ENV{HOME}/.cargo/bin" REQUIRED)
    get_filename_component(MEI_USB_RUST_ROOT "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/.." ABSOLUTE)
    set(MEI_RUST_TARGET_ARGS)
    set(MEI_RUST_ENV "CARGO_TARGET_DIR=${CMAKE_CURRENT_BINARY_DIR}/rust-target")
    set(MEI_RUST_TARGET_DIR "${CMAKE_CURRENT_BINARY_DIR}/rust-target")
    set(MEI_RUST_FLAGS "-C relocation-model=pic")
    if(ARGC GREATER 0 AND ARGV0)
        list(APPEND MEI_RUST_TARGET_ARGS --features full-usb)
    endif()
    if(ANDROID)
        if(ANDROID_ABI STREQUAL "arm64-v8a")
            set(MEI_RUST_TARGET aarch64-linux-android)
            set(MEI_NDK_TRIPLE aarch64-linux-android)
        elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
            set(MEI_RUST_TARGET armv7-linux-androideabi)
            set(MEI_NDK_TRIPLE armv7a-linux-androideabi)
        elseif(ANDROID_ABI STREQUAL "x86")
            set(MEI_RUST_TARGET i686-linux-android)
            set(MEI_NDK_TRIPLE i686-linux-android)
        elseif(ANDROID_ABI STREQUAL "x86_64")
            set(MEI_RUST_TARGET x86_64-linux-android)
            set(MEI_NDK_TRIPLE x86_64-linux-android)
        else()
            message(FATAL_ERROR "Unsupported Rust USB ABI: ${ANDROID_ABI}")
        endif()
        get_filename_component(MEI_NDK_BIN "${CMAKE_C_COMPILER}" DIRECTORY)
        set(MEI_RUST_LINKER "${MEI_NDK_BIN}/${MEI_NDK_TRIPLE}${ANDROID_PLATFORM_LEVEL}-clang")
        if(NOT EXISTS "${MEI_RUST_LINKER}")
            message(FATAL_ERROR "Android Rust linker missing: ${MEI_RUST_LINKER}")
        endif()
        string(TOUPPER "${MEI_RUST_TARGET}" MEI_RUST_ENV_TARGET)
        string(REPLACE "-" "_" MEI_RUST_ENV_TARGET "${MEI_RUST_ENV_TARGET}")
        list(APPEND MEI_RUST_ENV "CARGO_TARGET_${MEI_RUST_ENV_TARGET}_LINKER=${MEI_RUST_LINKER}")
        string(APPEND MEI_RUST_FLAGS " -C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-soname,libmei_usb.so")
        list(APPEND MEI_RUST_TARGET_ARGS --target "${MEI_RUST_TARGET}")
        set(MEI_RUST_LIBRARY "${MEI_RUST_TARGET_DIR}/${MEI_RUST_TARGET}/release/libmei_usb_core.so")
        # AGP commonly uses RelWithDebInfo for release native builds.
        if(CMAKE_BUILD_TYPE STREQUAL "Debug")
            set(MEI_RUST_VARIANT debug)
        else()
            set(MEI_RUST_VARIANT release)
        endif()
        get_filename_component(MEI_APP_ROOT "${PROJECT_SOURCE_DIR}/../../.." ABSOLUTE)
        set(MEI_USB_JNI_DIR "${MEI_APP_ROOT}/build/generated/usbRustJni/${MEI_RUST_VARIANT}/${ANDROID_ABI}")
        set(MEI_USB_OUTPUT "${MEI_USB_JNI_DIR}/libmei_usb.so")
    else()
        set(MEI_RUST_LIBRARY "${MEI_RUST_TARGET_DIR}/release/libmei_usb_core.a")
        set(MEI_USB_OUTPUT "${MEI_RUST_LIBRARY}")
    endif()
    list(APPEND MEI_RUST_ENV "RUSTFLAGS=${MEI_RUST_FLAGS}")
    file(GLOB_RECURSE MEI_RUST_SOURCES CONFIGURE_DEPENDS "${MEI_USB_RUST_ROOT}/src/*.rs")
    if(ANDROID)
        add_custom_command(OUTPUT "${MEI_USB_OUTPUT}"
            COMMAND "${CMAKE_COMMAND}" -E env ${MEI_RUST_ENV}
                "${MEI_CARGO_EXECUTABLE}" build --manifest-path "${MEI_USB_RUST_ROOT}/Cargo.toml"
                --lib --release --offline --locked ${MEI_RUST_TARGET_ARGS}
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${MEI_USB_JNI_DIR}"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different "${MEI_RUST_LIBRARY}" "${MEI_USB_OUTPUT}"
            DEPENDS ${MEI_RUST_SOURCES} "${MEI_USB_RUST_ROOT}/Cargo.toml" "${MEI_USB_RUST_ROOT}/Cargo.lock"
            COMMENT "Building Rust usbfs JNI library (${ANDROID_ABI})" VERBATIM)
    else()
        add_custom_command(OUTPUT "${MEI_USB_OUTPUT}"
            COMMAND "${CMAKE_COMMAND}" -E env ${MEI_RUST_ENV}
                "${MEI_CARGO_EXECUTABLE}" build --manifest-path "${MEI_USB_RUST_ROOT}/Cargo.toml"
                --lib --release --offline --locked ${MEI_RUST_TARGET_ARGS}
            DEPENDS ${MEI_RUST_SOURCES} "${MEI_USB_RUST_ROOT}/Cargo.toml" "${MEI_USB_RUST_ROOT}/Cargo.lock"
            COMMENT "Building Rust PCM parity library" VERBATIM)
    endif()
    add_custom_target(mei_usb_rust_build ALL DEPENDS "${MEI_USB_OUTPUT}")
    if(ANDROID)
        # AGP builds native library targets explicitly. This dependency also builds
        # Cargo's JNI output before Gradle merges the generated jniLibs directory.
        add_dependencies(mei_beatnet mei_usb_rust_build)
    else()
        add_library(mei_usb_rust STATIC IMPORTED GLOBAL)
        set_target_properties(mei_usb_rust PROPERTIES IMPORTED_LOCATION "${MEI_RUST_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${MEI_USB_RUST_ROOT}/include")
        add_dependencies(mei_usb_rust mei_usb_rust_build)
    endif()
endfunction()
