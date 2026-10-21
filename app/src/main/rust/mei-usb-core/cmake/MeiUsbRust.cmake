# One independent Cargo output directory per CMake ABI/build tree; no network during builds.
function(mei_add_usb_rust)
    find_program(MEI_CARGO_EXECUTABLE cargo HINTS "$ENV{HOME}/.cargo/bin" REQUIRED)
    get_filename_component(MEI_USB_RUST_ROOT "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/.." ABSOLUTE)
    set(MEI_RUST_TARGET_ARGS)
    if(MEI_USB_FULL_RUST)
        list(APPEND MEI_RUST_TARGET_ARGS --features full-usb)
    endif()
    set(MEI_RUST_TARGET_DIR "${CMAKE_CURRENT_BINARY_DIR}/rust-target")
    if(ANDROID)
        if(ANDROID_ABI STREQUAL "arm64-v8a")
            set(MEI_RUST_TARGET aarch64-linux-android)
        elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
            set(MEI_RUST_TARGET armv7-linux-androideabi)
        elseif(ANDROID_ABI STREQUAL "x86")
            set(MEI_RUST_TARGET i686-linux-android)
        elseif(ANDROID_ABI STREQUAL "x86_64")
            set(MEI_RUST_TARGET x86_64-linux-android)
        else()
            message(FATAL_ERROR "Unsupported Rust USB ABI: ${ANDROID_ABI}")
        endif()
        list(APPEND MEI_RUST_TARGET_ARGS --target "${MEI_RUST_TARGET}")
        set(MEI_RUST_LIBRARY "${MEI_RUST_TARGET_DIR}/${MEI_RUST_TARGET}/release/libmei_usb_core.a")
    else()
        set(MEI_RUST_LIBRARY "${MEI_RUST_TARGET_DIR}/release/libmei_usb_core.a")
    endif()
    file(GLOB_RECURSE MEI_RUST_SOURCES CONFIGURE_DEPENDS "${MEI_USB_RUST_ROOT}/src/*.rs")
    add_custom_command(OUTPUT "${MEI_RUST_LIBRARY}"
        COMMAND "${CMAKE_COMMAND}" -E env "CARGO_TARGET_DIR=${MEI_RUST_TARGET_DIR}"
            "RUSTFLAGS=-C relocation-model=pic"
            "${MEI_CARGO_EXECUTABLE}" build --manifest-path "${MEI_USB_RUST_ROOT}/Cargo.toml"
            --release --offline --locked ${MEI_RUST_TARGET_ARGS}
        DEPENDS ${MEI_RUST_SOURCES} "${MEI_USB_RUST_ROOT}/Cargo.toml" "${MEI_USB_RUST_ROOT}/Cargo.lock"
        COMMENT "Building Rust USB backend (${ANDROID_ABI})" VERBATIM)
    add_custom_target(mei_usb_rust_build DEPENDS "${MEI_RUST_LIBRARY}")
    add_library(mei_usb_rust STATIC IMPORTED GLOBAL)
    set_target_properties(mei_usb_rust PROPERTIES IMPORTED_LOCATION "${MEI_RUST_LIBRARY}"
        INTERFACE_INCLUDE_DIRECTORIES "${MEI_USB_RUST_ROOT}/include")
    add_dependencies(mei_usb_rust mei_usb_rust_build)
endfunction()
