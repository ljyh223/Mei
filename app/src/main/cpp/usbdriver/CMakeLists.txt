# USB audio/session algorithms retain NeriPlayer attribution; see NOTICE.md.
# Cargo links libmei_usb.so directly. USB builds contain no C/C++ source or libusb.
include("${CMAKE_CURRENT_SOURCE_DIR}/../../rust/mei-usb-core/cmake/MeiUsbRust.cmake")
mei_add_usb_rust(ON)
