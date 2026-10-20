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
