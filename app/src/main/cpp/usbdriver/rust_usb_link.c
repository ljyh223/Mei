/* Link anchor and ABI assertions only. All USB/JNI executable code is Rust.
 * Layouts must agree with src/usb/sys.rs on every Android ABI. */
#include <stddef.h>
#include <stdint.h>
#include <sys/time.h>
#include "third_party/libusb/libusb.h"
#define WIDE (UINTPTR_MAX > UINT32_MAX)
_Static_assert(sizeof(struct libusb_endpoint_descriptor) == (WIDE ? 32 : 20), "Rust endpoint ABI");
_Static_assert(sizeof(struct libusb_interface_descriptor) == (WIDE ? 40 : 24), "Rust alternate ABI");
_Static_assert(sizeof(struct libusb_config_descriptor) == (WIDE ? 40 : 24), "Rust configuration ABI");
_Static_assert(sizeof(struct libusb_transfer) == (WIDE ? 64 : 40), "Rust transfer ABI");
_Static_assert(offsetof(struct libusb_transfer, iso_packet_desc) == (WIDE ? 60 : 40), "Rust packet offset");
_Static_assert(sizeof(struct libusb_iso_packet_descriptor) == 12, "Rust packet ABI");
_Static_assert(sizeof(struct timeval) == 2 * sizeof(long), "Rust event timeval ABI");
const unsigned int mei_usb_rust_link_anchor = 1;
