//! C ABI contract: an opaque Core is created once and freed once after all calls finish.
//! C++ shared session ownership guarantees its lifetime, including cancelled USB callbacks.
//! Byte ranges must be valid for the call and input/output must not alias. Calls are synchronous;
//! Rust never retains a borrowed C++/JNI buffer. Core's locks allow concurrent producer/consumer calls.
//! Panics are contained here and reported as failure; poisoned state is never silently recovered.
#![allow(clippy::missing_safety_doc)]
use crate::pipeline::{Config, Core, Snapshot};
use std::{
    panic::{catch_unwind, AssertUnwindSafe},
    slice,
};
fn contained<T: Copy>(fallback: T, operation: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(operation)).unwrap_or(fallback)
}
unsafe fn core<'a>(raw: *const Core) -> Option<&'a Core> {
    // SAFETY: lifetime and alignment are guaranteed by the owning C++ PcmPipeline.
    unsafe { raw.as_ref() }
}
unsafe fn input<'a>(raw: *const u8, size: usize) -> Option<&'a [u8]> {
    if size == 0 {
        return Some(&[]);
    }
    if raw.is_null() || size > isize::MAX as usize {
        return None;
    }
    // SAFETY: caller supplies a readable region for this synchronous call; no pointer is retained.
    Some(unsafe { slice::from_raw_parts(raw, size) })
}
unsafe fn output<'a>(raw: *mut u8, size: usize) -> Option<&'a mut [u8]> {
    if size == 0 {
        return Some(&mut []);
    }
    if raw.is_null() || size > isize::MAX as usize {
        return None;
    }
    // SAFETY: caller grants exclusive mutable access to this region for the duration of the call.
    Some(unsafe { slice::from_raw_parts_mut(raw, size) })
}
#[no_mangle]
pub extern "C" fn mei_pcm_new() -> *mut Core {
    contained(std::ptr::null_mut(), || {
        Box::into_raw(Box::new(Core::default()))
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_free(raw: *mut Core) {
    if !raw.is_null() {
        let _ = contained(false, || {
            unsafe {
                drop(Box::from_raw(raw));
            }
            true
        });
    }
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_configure(raw: *const Core, config: *const Config) -> i32 {
    contained(0, || {
        match (unsafe { core(raw) }, unsafe { config.as_ref() }) {
            (Some(c), Some(config)) => i32::from(c.configure(*config).is_ok()),
            _ => 0,
        }
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_resize(raw: *const Core, ms: i32, bytes: i32, count: i32) -> i32 {
    contained(0, || {
        unsafe { core(raw) }.map_or(0, |c| i32::from(c.resize(ms, bytes, count).is_ok()))
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_write(raw: *const Core, data: *const u8, bytes: usize) -> usize {
    contained(usize::MAX, || {
        match (unsafe { core(raw) }, unsafe { input(data, bytes) }) {
            (Some(c), Some(data)) => c.write(data).unwrap_or(usize::MAX),
            _ => usize::MAX,
        }
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_fill(
    raw: *const Core,
    data: *mut u8,
    bytes: usize,
    playing: i32,
) -> usize {
    contained(usize::MAX, || {
        match (unsafe { core(raw) }, unsafe { output(data, bytes) }) {
            (Some(c), Some(data)) => c.fill(data, playing != 0).unwrap_or(usize::MAX),
            _ => usize::MAX,
        }
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_drain(raw: *const Core) -> i32 {
    contained(-1, || {
        unsafe { core(raw) }.map_or(-1, |c| c.drain().map(i32::from).unwrap_or(-1))
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_clear(raw: *const Core) -> i32 {
    contained(0, || {
        unsafe { core(raw) }.map_or(0, |c| i32::from(c.clear().is_ok()))
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_reset_counters(raw: *const Core) -> i32 {
    contained(0, || {
        unsafe { core(raw) }.map_or(0, |c| i32::from(c.reset_counters().is_ok()))
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_add_dropped(raw: *const Core, frames: u64) {
    contained((), || {
        if let Some(c) = unsafe { core(raw) } {
            let _ = c.add_dropped(frames);
        }
    });
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_set_gain(raw: *const Core, gain: f32) {
    contained((), || {
        if let Some(c) = unsafe { core(raw) } {
            c.set_target(gain);
        }
    });
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_set_perfect(raw: *const Core, enabled: i32) {
    contained((), || {
        if let Some(c) = unsafe { core(raw) } {
            c.set_perfect(enabled != 0);
        }
    });
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_is_perfect(raw: *const Core) -> i32 {
    contained(0, || {
        unsafe { core(raw) }.map_or(0, |c| i32::from(c.perfect()))
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_arm_ramp(raw: *const Core) {
    contained((), || {
        if let Some(c) = unsafe { core(raw) } {
            let _ = c.arm_ramp();
        }
    });
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_apply_ramp(raw: *const Core, data: *mut u8, bytes: usize) {
    contained((), || {
        if let (Some(c), Some(data)) = (unsafe { core(raw) }, unsafe { output(data, bytes) }) {
            let _ = c.start_ramp(data);
        }
    });
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_queued(raw: *const Core) -> usize {
    contained(0, || {
        unsafe { core(raw) }
            .and_then(|c| c.queued().ok())
            .unwrap_or(0)
    })
}
#[no_mangle]
pub unsafe extern "C" fn mei_pcm_snapshot(raw: *const Core, target: *mut Snapshot) -> i32 {
    contained(0, || {
        let Some(snapshot) = unsafe { core(raw) }.and_then(|c| c.snapshot().ok()) else {
            return 0;
        };
        let Some(target) = (unsafe { target.as_mut() }) else {
            return 0;
        };
        *target = snapshot;
        1
    })
}
#[no_mangle]
pub extern "C" fn mei_usb_rust_core_version() -> u32 {
    1
}

#[no_mangle]
pub unsafe extern "C" fn mei_usb_parse_format(
    data: *const u8,
    bytes: usize,
    version: u32,
    target: *mut crate::descriptor::Format,
) -> i32 {
    contained(0, || {
        let Some(data) = (unsafe { input(data, bytes) }) else {
            return 0;
        };
        let Some(parsed) = crate::descriptor::parse(data, version) else {
            return 0;
        };
        let Some(target) = (unsafe { target.as_mut() }) else {
            return 0;
        };
        *target = parsed;
        1
    })
}

#[cfg(test)]
mod tests {
    #[test]
    fn panic_is_contained_before_returning_to_foreign_code() {
        assert_eq!(
            super::contained(-1, || panic!("injected boundary panic")),
            -1
        );
    }
}
