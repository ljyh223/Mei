# USB transport provenance

USB native transport: NeriPlayer, https://github.com/cwuom/NeriPlayer
Original author: cwuom / ouom. Imported from revision 113699bb.

The imported `usb/exclusive`, `usb/feedback`, `usb/iso`, `usb/pcm`,
`usb/uac1` and `usb/uac2` sources use the NeriPlayer Native Attribution
License reproduced in NERI_LICENSE.md. JNI symbol names and build integration
are adapted for Mei. Kotlin integration and `usb/control` are Mei implementations;
NeriPlayer Kotlin/Java sources were not copied.

`third_party/libusb` keeps its original LGPL-2.1-or-later notices and source;
it is built as the separate shared library libmei_libusb.so. The original
source and license files are provided alongside the build definition.
NeriPlayer and its author do not endorse or certify Mei.
