//! The only Java boundary. JVM direct buffers are borrowed synchronously, never retained.
use super::{device::Device, stream::Session};
use jni_sys::{jboolean, jclass, jfloat, jint, jlong, jobject, jstring, JNIEnv};
use std::{
    collections::HashMap,
    ffi::CString,
    panic::{catch_unwind, AssertUnwindSafe},
    ptr,
    sync::{
        atomic::{AtomicU64, Ordering},
        Arc, Mutex, OnceLock,
    },
};
static HANDLES: OnceLock<Mutex<HashMap<i64, Arc<Session>>>> = OnceLock::new();
static LAST: Mutex<String> = Mutex::new(String::new());
static NEXT: AtomicU64 = AtomicU64::new(1);
fn handles() -> &'static Mutex<HashMap<i64, Arc<Session>>> {
    HANDLES.get_or_init(|| Mutex::new(HashMap::new()))
}
fn get(id: i64) -> Option<Arc<Session>> {
    handles().lock().ok()?.get(&id).cloned()
}
fn remember(error: impl Into<String>) {
    if let Ok(mut last) = LAST.lock() {
        *last = error.into();
    }
}
fn boundary<T>(fallback: T, f: impl FnOnce() -> T) -> T {
    match catch_unwind(AssertUnwindSafe(f)) {
        Ok(v) => v,
        Err(_) => {
            remember("usb_native_panicked");
            fallback
        }
    }
}
unsafe fn string(env: *mut JNIEnv, value: &str) -> jstring {
    // All strings here are generated ASCII diagnostics/capability tuples, not arbitrary USB names.
    let Ok(value) = CString::new(value.replace('\0', "?")) else {
        return ptr::null_mut();
    };
    unsafe {
        (**env)
            .NewStringUTF
            .map(|f| f(env, value.as_ptr()))
            .unwrap_or(ptr::null_mut())
    }
}
fn action(handle: jlong, f: impl FnOnce(&Session) -> bool) -> jboolean {
    let Some(s) = get(handle) else { return 0 };
    let Ok(_guard) = s.api.lock() else { return 0 };
    u8::from(f(&s))
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeOutputFormats(
    env: *mut JNIEnv,
    _class: jclass,
    fd: jint,
) -> jstring {
    boundary(ptr::null_mut(), || {
        let mut report = String::new();
        match Device::probe(fd, false) {
            Ok(device) => {
                for c in device.candidates() {
                    if c.ranges.is_empty() {
                        report.push_str(&format!(
                            "{},{},{},0,0,0\n",
                            c.format.bits, c.format.subslot, c.format.channels
                        ));
                    } else {
                        for r in c.ranges {
                            report.push_str(&format!(
                                "{},{},{},{},{},{}\n",
                                c.format.bits,
                                c.format.subslot,
                                c.format.channels,
                                r.min,
                                r.max,
                                r.step
                            ));
                        }
                    }
                }
            }
            Err(e) => remember(e),
        }
        unsafe { string(env, &report) }
    })
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeOpen(
    _env: *mut JNIEnv,
    _class: jclass,
    fd: jint,
    rate: jint,
    channels: jint,
    bits: jint,
    subslot: jint,
) -> jlong {
    boundary(0, || {
        match Session::open(
            fd,
            rate as u32,
            channels as u32,
            bits as u32,
            subslot as u32,
        ) {
            Ok(s) => {
                let id = NEXT.fetch_add(1, Ordering::Relaxed) as i64;
                if id <= 0 {
                    s.close();
                    remember("usb_handle_space_exhausted");
                    return 0;
                }
                match handles().lock() {
                    Ok(mut h) => {
                        h.insert(id, s);
                        remember("none");
                        id
                    }
                    Err(_) => {
                        s.close();
                        remember("usb_registry_poisoned");
                        0
                    }
                }
            }
            Err(e) => {
                remember(e);
                0
            }
        }
    })
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativePreparePlayerPcm(
    _env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
    rate: jint,
    channels: jint,
    encoding: jint,
) -> jboolean {
    boundary(0, || {
        action(handle, |s| s.prepare(rate, channels, encoding))
    })
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeWritePlayerPcm(
    env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
    buffer: jobject,
    offset: jint,
    size: jint,
    volume: jfloat,
) -> jint {
    boundary(-1, || {
        let Some(s) = get(handle) else { return -1 };
        let Ok(_guard) = s.api.lock() else { return -1 };
        if !s.live()
            || !s.prepared.load(Ordering::Acquire)
            || buffer.is_null()
            || offset < 0
            || size < 0
            || !volume.is_finite()
        {
            return -1;
        }
        let (address, capacity) = unsafe {
            let Some(address) = (**env).GetDirectBufferAddress else {
                return -1;
            };
            let Some(capacity) = (**env).GetDirectBufferCapacity else {
                return -1;
            };
            (address(env, buffer), capacity(env, buffer))
        };
        if address.is_null() || capacity < 0 || (offset as i64 + size as i64) > capacity {
            return -1;
        }
        s.core.set_target(volume.clamp(0., 1.));
        // SAFETY: JVM verified direct allocation bounds; borrow ends before JNI returns.
        let input = unsafe {
            std::slice::from_raw_parts(address.cast::<u8>().add(offset as usize), size as usize)
        };
        s.core.write(input).map(|n| n as i32).unwrap_or(-1)
    })
}
macro_rules! bool_native {
    ($name:ident,$body:expr) => {
        #[no_mangle]
        pub unsafe extern "system" fn $name(
            _env: *mut JNIEnv,
            _class: jclass,
            handle: jlong,
        ) -> jboolean {
            boundary(0, || action(handle, $body))
        }
    };
}
bool_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeDrainPlayerPcm,
    |s| s.live() && s.core.drain().unwrap_or(false)
);
bool_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativePlayPlayerPcm,
    |s| s.command(true, false)
);
bool_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativePausePlayerPcm,
    |s| s.command(false, false)
);
bool_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeFlushPlayerPcm,
    |s| s.flush()
);
bool_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeHasHardwareVolume,
    |s| s.device.has_volume()
);
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeConfigurePlayerBufferDuration(
    _env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
    duration: jint,
) -> jboolean {
    boundary(0, || action(handle, |s| s.resize(duration)))
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeSetHardwareVolume(
    _env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
    fraction: jfloat,
) -> jboolean {
    boundary(0, || {
        action(handle, |s| s.live() && s.device.set_volume(fraction))
    })
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeSetPlayerBitPerfect(
    _env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
    enabled: jboolean,
) -> jboolean {
    boundary(0, || {
        action(handle, |s| {
            if !s.live() {
                return false;
            }
            s.core.set_perfect(enabled != 0);
            true
        })
    })
}
macro_rules! count_native {
    ($name:ident,$body:expr) => {
        #[no_mangle]
        pub unsafe extern "system" fn $name(
            _env: *mut JNIEnv,
            _class: jclass,
            handle: jlong,
        ) -> jlong {
            boundary(0, || {
                get(handle).map($body).unwrap_or(0).min(i64::MAX as u64) as i64
            })
        }
    };
}
count_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeGetCompletedAudioFrames,
    |s: Arc<Session>| s.completed.load(Ordering::Acquire)
);
count_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeGetQueuedPlayerFrames,
    |s: Arc<Session>| s.queued_frames()
);
count_native!(
    Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeGetPlayerPcmFreeBytes,
    |s: Arc<Session>| s.core.snapshot().map(|v| v.integers[2]).unwrap_or(0)
);
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeMarkDeviceDetached(
    _env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
) {
    boundary((), || {
        if let Some(s) = get(handle) {
            s.device.online.store(false, Ordering::Release);
        }
    })
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeClose(
    _env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
) {
    boundary((), || {
        let session = handles().lock().ok().and_then(|mut h| h.remove(&handle));
        if let Some(s) = session {
            s.close();
        }
    })
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeRuntimeReport(
    env: *mut JNIEnv,
    _class: jclass,
    handle: jlong,
) -> jstring {
    boundary(ptr::null_mut(), || {
        let report = get(handle).map(|s| s.report()).unwrap_or_else(|| {
            "backend=full-rust terminalFailure=true transportFailed=true error=invalid_handle"
                .into()
        });
        unsafe { string(env, &report) }
    })
}
#[no_mangle]
pub unsafe extern "system" fn Java_com_ljyh_mei_playback_usb_UsbNativeBridge_nativeLastOpenError(
    env: *mut JNIEnv,
    _class: jclass,
) -> jstring {
    boundary(ptr::null_mut(), || {
        let error = LAST
            .lock()
            .map(|s| s.clone())
            .unwrap_or_else(|_| "usb_error_state_poisoned".into());
        unsafe { string(env, &error) }
    })
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn invalid_handles_do_not_touch_foreign_memory() {
        assert!(get(-1).is_none());
        assert_eq!(action(-1, |_| panic!("unreachable")), 0);
        assert_eq!(boundary(7, || panic!("injected JNI panic")), 7);
        assert_eq!(LAST.lock().unwrap().as_str(), "usb_native_panicked");
    }
}
