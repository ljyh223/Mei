#!/usr/bin/env python3
"""Reproduce NetEase Cloud Music Android 9.6.05 deviceId (4th field of the EAPI device blob).

Reversed from libnetdev-1.0.0.so (arm64-v8a) in
网易云音乐_9.6.05.apk / sha256 616d08a792d03cbac5cd29af7d1a54ede52a18de69a4bce3af9dde83fc8a781d.

Chain
-----
l4.e()  ->  m(ctx) + "\t" + o(ctx) + "\t" + d(ctx) + "\t" + f(ctx)
l4.f(ctx)  ->  NEDeviceID.getLocalID(ctx)                     (native getDeviceId)
             -> CMobJsonHandler::GetDeviceId
             -> CGenerateUUID::getAntiRubbishUUID()
             -> CUtil::calUUIDStringMd5( getRubbishUUIDRaw() + (u8)jarReflectMul )
l4.e() then Base64(..., NO_WRAP) + URLEncoder.encode

Field 4 is cropped by l4.f():
    trimmed.length() <= 20 -> unchanged
    21..24                   -> trimmed.substring(0, 20)
    >= 25                    -> trimmed.substring(8, 24)   <-- 32-hex ids land here (16 chars)
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import urllib.parse

# ---------------------------------------------------------------------------
# Constants recovered from libnetdev-1.0.0.so
# ---------------------------------------------------------------------------

# 0x1a20..0x1ab0 in CGenerateUUID::getRubbishUUIDRaw -- the "rubbish" MD5
# blacklist compared against MD5(raw_candidate).
RUBBISH_MD5_BLACKLIST = frozenset(
    {
        "1ec0e4c8f9c73052573c278cba6e9b79",
        "e2e0e74b9ac0e9ecb4b44c991ec4def2",
        "f56d7360e5f9df234621fd695a751e81",
        "66e16ed20f860f7530a34a18cffd42d0",
        "a4181dd9a8478db3133341d3be2b5a65",
        "2d7254d41cc34f59477e5a4b561cd1e0",
        "0222fcde3ad88c4b639fffb6b7a5fead",
        "dc8925cae120d0cda6fcef276f90e58b",
        "6a0ba127f3e09088fbabe8a218739c59",
        "4a7ede2c6ad5f72be9b86fb52c545ee6",
        "23685ac20e9b689a99603b77281e9915",
        "88c041265cf2e58776a569670e024d51",
        "67261c409b6faacfac5f1ca7cb5d6a29",
        "bd62e30ee863f6e3a63f8d2628af7000",
    }
)

# CGenerateUUID::checkDeviceId -- accepted iff length == 32 and all hex digits.
CACHED_DEVICE_ID_LEN = 32

# CReflectInfo::getReflectMul() == com/netease/is/deviceid/poly/a.o
#                                      + com/netease/is/deviceid/poly/a.p
#                                      - 2
# Both static ints are JNI-injected; poly/a.<clinit> is `return-void`, so on a
# stock device with the JNI hooks intact they default to 0 -> -2.
DEFAULT_JAR_REFLECT_MUL = -2

# CUtil::calUUIDStringMd5 always emits 32 lowercase hex chars ("%02x" x 16).
LOCAL_ID_HEX_LEN = 32


# ---------------------------------------------------------------------------
# Helpers mirroring the native/std::string behaviour
# ---------------------------------------------------------------------------

def md5_hex(data: bytes) -> str:
    """CUtil::calUUIDStringMd5 / calRawStringMd5 -> 32 lowercase hex chars."""
    return hashlib.md5(data).hexdigest()


def check_device_id(value: str) -> bool:
    """CGenerateUUID::checkDeviceId: exactly 32 chars, every one a hex digit."""
    if len(value) != CACHED_DEVICE_ID_LEN:
        return False
    return all(c in "0123456789abcdefABCDEF" for c in value)


def crop_local_id(local_id: str) -> str:
    """com.netease.cloudmusic.utils.l4.f(Context) trimming applied in Java."""
    trimmed = local_id.strip()
    if len(trimmed) <= 24:
        return trimmed[:20] if len(trimmed) > 20 else trimmed
    return trimmed[8:24]


# ---------------------------------------------------------------------------
# Native side
# ---------------------------------------------------------------------------

def get_rubbish_uuid_raw(
    wifi_mac: str | None = None,
    android_id: str | None = None,
    is_simulator: bool = False,
) -> str:
    """CGenerateUUID::getRubbishUUIDRaw.

    Preference order (from the arm64 disassembly):
      1. CWifiMacInfo::getWifiMacByNetlink()  -- needs root, usually ""
      2. the buffered/cached value at <.apk dir>/.CMDatsetRequireRJSBXPN.dat
      3. CReflectInfo::getReflectAndroidID*() when on an emulator or when the
         candidate MD5 hits RUBBISH_MD5_BLACKLIST.
    """
    candidate = (wifi_mac or "").strip()

    if len(candidate) <= 5:
        # Netlink MAC unavailable -> fall through to the cached/SD value, and
        # finally to the Android ID on a simulator or a blacklisted candidate.
        candidate = (android_id or "").strip()
    elif md5_hex(candidate.encode()) in RUBBISH_MD5_BLACKLIST:
        candidate = (android_id or "").strip()

    if is_simulator:
        candidate = (android_id or "").strip()

    return candidate


def get_anti_rubbish_uuid(
    cached: str | None = None,
    sdcard_cached: str | None = None,
    sdk_ver: int = 30,
    wifi_mac: str | None = None,
    android_id: str | None = None,
    is_simulator: bool = False,
    jar_reflect_mul: int = DEFAULT_JAR_REFLECT_MUL,
) -> str:
    """CGenerateUUID::getAntiRubbishUUID -- the 32-hex local device ID."""
    # 1. in-process buffer
    device_id = (cached or "").strip()

    # 2. SD-card copy, only consulted on Android 8.1+ (ro.build.version.sdk > 27)
    if not check_device_id(device_id) and sdk_ver > 27:
        device_id = (sdcard_cached or "").strip()

    if check_device_id(device_id):
        return device_id

    # 3. fresh generation
    raw = get_rubbish_uuid_raw(
        wifi_mac=wifi_mac,
        android_id=android_id,
        is_simulator=is_simulator,
    )
    # CUtil::calUUIDStringMd5(raw || (uint8)jar_reflect_mul)
    device_id = md5_hex(raw.encode() + bytes([jar_reflect_mul & 0xFF]))
    return device_id


def build_device_blob(
    imei: str | None,
    wifi_mac: str | None,
    android_id: str | None,
    local_id: str | None,
) -> str:
    """com.netease.cloudmusic.utils.l4.e() -- the value the app actually sends."""
    fields = [
        imei if imei is not None else "null",
        wifi_mac if wifi_mac else "02:00:00:00:00:00",
        android_id if android_id is not None else "null",
        crop_local_id(local_id) if local_id is not None else "null",
    ]
    raw = "\t".join(fields)
    encoded = base64.b64encode(raw.encode()).decode()
    return urllib.parse.quote(encoded, safe="")


def decode_device_blob(blob: str) -> list[str]:
    """Inverse of build_device_blob -- handy for auditing fixtures."""
    return base64.b64decode(urllib.parse.unquote(blob)).decode().split("\t")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--imei", default=None, help='IMEI, omit or "null" when unavailable')
    ap.add_argument("--wifi-mac", default=None, help="Wi-Fi MAC address (aa:bb:cc:dd:ee:ff)")
    ap.add_argument("--android-id", default=None, help="Settings.Secure.ANDROID_ID")
    ap.add_argument("--local-id", default=None,
                    help="full 32-hex local ID (recomputed if omitted)")
    ap.add_argument("--cached", default=None, help="existing 32-hex cached deviceId")
    ap.add_argument("--sdcard-cached", default=None, help="existing SD-card deviceId")
    ap.add_argument("--sdk-ver", type=int, default=30, help="ro.build.version.sdk")
    ap.add_argument("--simulator", action="store_true", help="emulator detected")
    ap.add_argument("--jar-reflect-mul", type=int, default=DEFAULT_JAR_REFLECT_MUL,
                    help="poly/a.o + poly/a.p - 2 (default -2)")
    ap.add_argument("--decode", default=None, help="decode an existing device blob and exit")
    args = ap.parse_args()

    if args.decode:
        for i, field in enumerate(decode_device_blob(args.decode), 1):
            print(f"field{i}: {field}")
        return

    local_id = args.local_id or get_anti_rubbish_uuid(
        cached=args.cached,
        sdcard_cached=args.sdcard_cached,
        sdk_ver=args.sdk_ver,
        wifi_mac=args.wifi_mac,
        android_id=args.android_id,
        is_simulator=args.simulator,
        jar_reflect_mul=args.jar_reflect_mul,
    )

    print(f"localId (full) : {local_id}")
    print(f"localId (field4): {crop_local_id(local_id)}")
    print(f"deviceId blob  : {build_device_blob(args.imei, args.wifi_mac, args.android_id, local_id)}")


if __name__ == "__main__":
    main()
