cmake_minimum_required(VERSION 3.22.1)
project(mei_usb_tests LANGUAGES CXX)
enable_testing()
set(USB_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp/usbdriver")
add_executable(mei_usb_tests usb_control_test.cpp
    ${USB_SOURCE}/usb/control/usb_feature_unit.cpp
    ${USB_SOURCE}/usb/control/usb_sample_rate_readback.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_codec.cpp)
target_include_directories(mei_usb_tests PRIVATE ${USB_SOURCE})
target_compile_features(mei_usb_tests PRIVATE cxx_std_17)
target_compile_options(mei_usb_tests PRIVATE -Wall -Wextra -Werror -UNDEBUG -fsanitize=address,undefined)
target_link_options(mei_usb_tests PRIVATE -fsanitize=address,undefined)
add_test(NAME usb_control_and_pcm COMMAND mei_usb_tests)

add_executable(mei_usb_pipeline_tests usb_pipeline_test.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_codec.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_pipeline.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_resampler.cpp)
target_include_directories(mei_usb_pipeline_tests PRIVATE ${USB_SOURCE})
target_compile_features(mei_usb_pipeline_tests PRIVATE cxx_std_17)
target_compile_options(mei_usb_pipeline_tests PRIVATE -Wall -Wextra -Werror -UNDEBUG -fsanitize=address,undefined)
target_link_options(mei_usb_pipeline_tests PRIVATE -fsanitize=address,undefined)
add_test(NAME usb_resampling_channels_and_queue COMMAND mei_usb_pipeline_tests)

# Exercise the real C++ adapter against the Rust core using the same behavioral suite.
include("${CMAKE_CURRENT_SOURCE_DIR}/../../main/rust/mei-usb-core/cmake/MeiUsbRust.cmake")
mei_add_usb_rust()
add_executable(mei_usb_rust_pipeline_tests usb_pipeline_test.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_codec.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_pipeline_rust.cpp)
target_compile_definitions(mei_usb_rust_pipeline_tests PRIVATE MEI_USB_RUST_PCM=1)
target_link_libraries(mei_usb_rust_pipeline_tests PRIVATE mei_usb_rust dl m)

# Directly compare baseline C++ and Rust output in one process, including resampling and gain.
add_executable(mei_usb_rust_parity_tests usb_rust_parity_test.cpp
    ${USB_SOURCE}/usb/uac1/usb_uac1_format.cpp
    ${USB_SOURCE}/usb/uac2/usb_uac2_format.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_codec.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_pipeline.cpp
    ${USB_SOURCE}/usb/pcm/usb_pcm_resampler.cpp)
target_link_libraries(mei_usb_rust_parity_tests PRIVATE mei_usb_rust dl m)
foreach(target mei_usb_rust_pipeline_tests mei_usb_rust_parity_tests)
    target_include_directories(${target} PRIVATE ${USB_SOURCE})
    target_compile_features(${target} PRIVATE cxx_std_17)
    target_compile_options(${target} PRIVATE -Wall -Wextra -Werror -UNDEBUG -fsanitize=address,undefined)
    target_link_options(${target} PRIVATE -fsanitize=address,undefined)
endforeach()
add_test(NAME rust_pipeline_adapter COMMAND mei_usb_rust_pipeline_tests)
add_test(NAME cpp_rust_audio_parity COMMAND mei_usb_rust_parity_tests)
