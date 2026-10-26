// Verify the Rust usbfs UAPI declarations against installed kernel/NDK headers.
// Test-only C++; no C/C++ is linked into the Android USB library.
#include <cstddef>
#include <cstdint>
#include <linux/usbdevice_fs.h>
#include <sys/eventfd.h>
#include <sys/ioctl.h>
#include <poll.h>
static constexpr bool wide = sizeof(void*) == 8;
static_assert(sizeof(usbdevfs_iso_packet_desc) == 12);
static_assert(sizeof(usbdevfs_urb) == (wide ? 56 : 44));
static_assert(offsetof(usbdevfs_urb, iso_frame_desc) == sizeof(usbdevfs_urb));
static_assert(offsetof(usbdevfs_urb, buffer) == (wide ? 16 : 12));
static_assert(offsetof(usbdevfs_urb, usercontext) == (wide ? 48 : 40));
static_assert(sizeof(usbdevfs_ctrltransfer) == (wide ? 24 : 16));
static_assert(sizeof(usbdevfs_ioctl) == (wide ? 16 : 12));
static_assert(USBDEVFS_CONTROL == (wide ? 0xc0185500U : 0xc0105500U));
static_assert(USBDEVFS_SUBMITURB == (wide ? 0x8038550aU : 0x802c550aU));
static_assert(USBDEVFS_REAPURBNDELAY == (wide ? 0x4008550dU : 0x4004550dU));
static_assert(USBDEVFS_SETINTERFACE == 0x80085504U);
static_assert(USBDEVFS_GETDRIVER == 0x41045508U);
static_assert(USBDEVFS_DISCARDURB == 0x550bU);
static_assert(USBDEVFS_CLAIMINTERFACE == 0x8004550fU);
static_assert(USBDEVFS_RELEASEINTERFACE == 0x80045510U);
static_assert(USBDEVFS_GET_CAPABILITIES == 0x8004551aU);
static_assert(USBDEVFS_DISCONNECT_CLAIM == 0x8108551bU);
static_assert(USBDEVFS_GET_SPEED == 0x551fU);
static_assert(USBDEVFS_URB_ISO_ASAP == 2);
static_assert(USBDEVFS_CAP_REAP_AFTER_DISCONNECT == 16);
static_assert(EFD_CLOEXEC == 0x80000 && EFD_NONBLOCK == 0x800);
#if defined(__ANDROID__)
static_assert(sizeof(nfds_t) == 4);
int (*const check_ioctl_signature)(int, int, ...) = &ioctl;
#else
static_assert(sizeof(nfds_t) == sizeof(void*));
int (*const check_ioctl_signature)(int, unsigned long, ...) = &ioctl;
#endif
int main() { (void)check_ioctl_signature; return 0; }
