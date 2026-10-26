# USB transport provenance

USB native transport: NeriPlayer, https://github.com/cwuom/NeriPlayer
Original author: cwuom / ouom. Imported from revision 113699bb.

The imported `usb/exclusive`, `usb/feedback`, `usb/iso`, `usb/pcm`,
`usb/uac1` and `usb/uac2` sources use the NeriPlayer Native Attribution
License reproduced in NERI_LICENSE.md. JNI symbol names and build integration
are adapted for Mei. Mei also adds a read-only playback-format/clock-range probe
to the device selection and session files. Kotlin integration and `usb/control` are Mei implementations;
NeriPlayer Kotlin/Java sources were not copied.

The previously bundled libusb C source and libmei_libusb.so are not used on the
codex/usbfs-rust branch. They were replaced by a Mei Rust implementation of the
Linux/Android usbfs userspace ABI and removed from this source tree. Earlier
revisions retain their original libusb source and LGPL notices in Git history.
NeriPlayer and its author do not endorse or certify Mei.

Rust branch: the PCM codec/pipeline, polyphase sinc resampler and AudioStreaming
Type-I descriptor parsers have been adapted into app/src/main/rust/mei-usb-core.
These ports retain the NeriPlayer native attribution license and attribution above.
The C++ transport sources (`usb/exclusive`, `usb/feedback`, `usb/iso` and the UAC2
feedback/clock helpers) were removed once the Rust port became the only backend;
`usb/pcm`, `usb/uac1` and `usb/uac2` format sources are kept for host parity tests
and remain under the same license.

Rust port: JNI, USB fd/session/interface ownership, UAC clock/rate negotiation,
playback feature controls, isochronous scheduling/completion/cancellation and explicit
feedback have also been ported into that Rust crate. The USB backend no longer compiles
any C++ USB implementation. The same native attribution and no-endorsement terms apply
to these derivative implementations. Cargo now links the JNI shared library directly;
the CMake C anchor has been removed. The new usbfs syscall/URB implementation is Mei code. The NeriPlayer
audio/UAC-derived implementation above still retains its existing attribution.
