use crate::{
    codec, ffi,
    pipeline::{Config, Core, Snapshot},
};
use std::sync::{
    atomic::{AtomicBool, Ordering},
    Arc,
};
fn config() -> Config {
    Config {
        output_rate: 48000,
        output_channels: 2,
        output_bytes: 2,
        output_bits: 16,
        frame_bytes: 4,
        input_rate: 48000,
        input_channels: 2,
        input_encoding: 2,
        ring_ms: 100,
        transfer_bytes: 16,
        transfer_count: 1,
    }
}
fn configured(c: Config) -> Core {
    let core = Core::default();
    core.configure(c).unwrap();
    core.set_perfect(true);
    core
}
fn drain_bytes(core: &Core) -> Vec<u8> {
    let snapshot = core.snapshot().unwrap();
    let mut bytes = vec![0; snapshot.integers[0] as usize];
    assert_eq!(core.fill(&mut bytes, true).unwrap(), bytes.len());
    bytes
}
#[test]
fn invalid_configuration_fails_without_replacing_active_queue() {
    let core = configured(config());
    core.write(&[1, 2, 3, 4]).unwrap();
    assert!(core.configure(Config::default()).is_err());
    assert_eq!(drain_bytes(&core), [1, 2, 3, 4]);
    for c in [
        Config {
            output_bits: 33,
            ..config()
        },
        Config {
            output_channels: 9,
            ..config()
        },
        Config {
            frame_bytes: 3,
            ..config()
        },
        Config {
            input_encoding: 999,
            ..config()
        },
    ] {
        assert!(core.configure(c).is_err());
    }
}
#[test]
fn integer_16_bit_unity_is_exact_for_every_value() {
    let core = configured(Config {
        ring_ms: 3000,
        ..config()
    });
    let mut input = Vec::new();
    for value in i16::MIN..=i16::MAX {
        input.extend_from_slice(&value.to_le_bytes());
        input.extend_from_slice(&value.to_le_bytes());
    }
    assert_eq!(core.write(&input).unwrap(), input.len());
    assert_eq!(drain_bytes(&core), input);
}
#[test]
fn packed_24_to_padded_24_preserves_every_source_bit() {
    let core = configured(Config {
        output_bytes: 4,
        output_bits: 24,
        frame_bytes: 8,
        input_encoding: 21,
        ..config()
    });
    let input = [0, 0, 0x80, 0xff, 0xff, 0x7f, 1, 0, 0, 0xff, 0xff, 0xff];
    assert_eq!(core.write(&input).unwrap(), input.len());
    assert_eq!(
        drain_bytes(&core),
        [0, 0, 0, 0x80, 0, 0xff, 0xff, 0x7f, 0, 1, 0, 0, 0, 0xff, 0xff, 0xff]
    );
}
#[test]
fn endian_conversion_preserves_low_integer_32_bits() {
    let core = configured(Config {
        output_bytes: 4,
        output_bits: 32,
        frame_bytes: 8,
        input_encoding: 0x60000000,
        ..config()
    });
    let values = [i32::MIN, -1, 1, 16_777_217, i32::MAX, -16_777_217];
    let input: Vec<_> = values.iter().flat_map(|v| v.to_be_bytes()).collect();
    core.write(&input).unwrap();
    assert_eq!(
        drain_bytes(&core),
        values
            .iter()
            .flat_map(|v| v.to_le_bytes())
            .collect::<Vec<_>>()
    );
}
#[test]
fn nonfinite_float_samples_are_silent_and_out_of_range_is_clamped() {
    let core = configured(Config {
        input_encoding: 4,
        ..config()
    });
    let input: Vec<_> = [f32::NAN, f32::INFINITY, f32::NEG_INFINITY, 2.0]
        .iter()
        .flat_map(|v| v.to_le_bytes())
        .collect();
    core.write(&input).unwrap();
    assert_eq!(drain_bytes(&core), [0, 0, 0, 0, 0, 0, 0xff, 0x7f]);
}
#[test]
fn mono_to_multichannel_populates_only_front_left_and_right() {
    let core = configured(Config {
        output_channels: 8,
        frame_bytes: 16,
        input_channels: 1,
        ..config()
    });
    core.write(&1000i16.to_le_bytes()).unwrap();
    let bytes = drain_bytes(&core);
    assert_eq!(&bytes[..4], &[0xe8, 3, 0xe8, 3]);
    assert!(bytes[4..].iter().all(|v| *v == 0));
}
#[test]
fn stereo_to_mono_averages_both_channels() {
    let core = configured(Config {
        output_channels: 1,
        frame_bytes: 2,
        ..config()
    });
    let input: Vec<_> = [1000i16, 3000]
        .iter()
        .flat_map(|v| v.to_le_bytes())
        .collect();
    core.write(&input).unwrap();
    assert_eq!(drain_bytes(&core), 2000i16.to_le_bytes());
}
#[test]
fn backpressure_accepts_only_complete_source_frames() {
    let core = configured(config());
    let capacity = core.snapshot().unwrap().integers[1] as usize;
    let input = vec![1; capacity + 100];
    assert_eq!(core.write(&input).unwrap(), capacity);
    assert_eq!(core.write(&[1, 2, 3, 4]).unwrap(), 0);
    assert!(core.write(&[1]).is_err());
    let mut released = [0; 4];
    core.fill(&mut released, true).unwrap();
    assert_eq!(core.write(&[5, 6, 7, 8]).unwrap(), 4);
    assert_eq!(core.snapshot().unwrap().integers[6], 0);
}
#[test]
fn pause_keeps_queue_and_does_not_report_real_audio_consumed() {
    let core = configured(config());
    core.write(&[1, 2, 3, 4]).unwrap();
    let mut output = [99; 16];
    assert_eq!(core.fill(&mut output, false).unwrap(), 0);
    assert_eq!(output, [0; 16]);
    assert_eq!(core.queued().unwrap(), 1);
    assert_eq!(drain_bytes(&core), [1, 2, 3, 4]);
}
#[test]
fn shrinking_never_discards_accepted_background_pcm() {
    let core = configured(Config {
        ring_ms: 1500,
        ..config()
    });
    let input = vec![1; 48000 * 4];
    core.write(&input).unwrap();
    core.resize(100, 16, 1).unwrap();
    assert_eq!(core.snapshot().unwrap().integers[6], 0);
    assert_eq!(drain_bytes(&core), input);
    core.resize(100, 16, 1).unwrap();
    assert_eq!(core.snapshot().unwrap().integers[1], 19200);
}
fn resampled(chunks: usize, rate: i32) -> Vec<u8> {
    let core = configured(Config {
        input_rate: 44100,
        output_rate: rate,
        ring_ms: 3000,
        ..config()
    });
    let input: Vec<_> = (0..4410)
        .flat_map(|f| {
            let sample = ((f as f64 * 0.1).sin() * 10000.0) as i16;
            [sample, -sample].into_iter().flat_map(i16::to_le_bytes)
        })
        .collect();
    for bytes in input.chunks(chunks * 4) {
        assert_eq!(core.write(bytes).unwrap(), bytes.len());
    }
    assert!(core.drain().unwrap());
    let bytes = drain_bytes(&core);
    assert!((bytes.len() as i64 / 4 - rate as i64 / 10).abs() <= 1);
    assert!(core.drain().unwrap());
    assert_eq!(core.queued().unwrap(), 0);
    bytes
}
#[test]
fn resampling_preserves_duration_and_is_independent_of_chunk_boundaries() {
    for rate in [8000, 48000, 88200, 96000] {
        assert_eq!(resampled(4410, rate), resampled(17, rate));
    }
}
#[test]
fn downsampling_suppresses_out_of_band_signal() {
    let core = configured(Config {
        input_rate: 48000,
        output_rate: 8000,
        ring_ms: 3000,
        ..config()
    });
    let input: Vec<_> = (0..4800)
        .flat_map(|f| {
            let value = if f % 2 == 0 { 20000i16 } else { -20000 };
            [value, value].into_iter().flat_map(i16::to_le_bytes)
        })
        .collect();
    core.write(&input).unwrap();
    core.drain().unwrap();
    let bytes = drain_bytes(&core);
    let samples: Vec<f32> = bytes
        .chunks_exact(2)
        .skip(100)
        .take(1200)
        .map(|s| codec::decode(s, 2))
        .collect();
    let rms = (samples.iter().map(|v| v * v).sum::<f32>() / samples.len() as f32).sqrt();
    assert!(rms < 0.001, "alias rms={rms}");
}
#[test]
fn seek_clears_resampler_history_and_counters() {
    let core = configured(Config {
        input_rate: 44100,
        ..config()
    });
    core.write(&vec![1; 400]).unwrap();
    core.clear().unwrap();
    core.reset_counters().unwrap();
    assert_eq!(core.queued().unwrap(), 0);
    core.drain().unwrap();
    assert_eq!(core.queued().unwrap(), 0);
    assert_eq!(core.snapshot().unwrap().integers[4], 0);
}
#[test]
fn perfect_mode_mutes_without_ramps_and_preserves_unity_data() {
    let core = configured(config());
    core.arm_ramp().unwrap();
    core.write(&[1, 2, 3, 4]).unwrap();
    let mut output = drain_bytes(&core);
    core.start_ramp(&mut output).unwrap();
    assert_eq!(output, [1, 2, 3, 4]);
    core.set_target(0.0);
    core.write(&[1, 2, 3, 4]).unwrap();
    assert_eq!(drain_bytes(&core), [0, 0, 0, 0]);
}
#[test]
fn software_gain_ramps_to_target_and_sanitizes_nonfinite_gain() {
    let core = configured(config());
    core.set_perfect(false);
    let input: Vec<_> = (0..4800)
        .flat_map(|_| [12000i16, 12000].into_iter().flat_map(i16::to_le_bytes))
        .collect();
    core.write(&input).unwrap();
    core.set_target(0.0);
    let output = drain_bytes(&core);
    assert!(codec::decode(&output[..2], 2) > 0.3);
    assert!(output[4000 * 4..].iter().all(|b| *b == 0));
    core.set_target(f32::NAN);
    assert_eq!(core.target(), 0.0);
}
#[test]
fn concurrent_producer_consumer_delivers_exactly_accepted_bytes() {
    let core = Arc::new(configured(config()));
    let done = Arc::new(AtomicBool::new(false));
    let input: Vec<u8> = (0..65536).map(|v| (v % 251) as u8).collect();
    let expected = input.clone();
    let writer = core.clone();
    let writer_done = done.clone();
    let producer = std::thread::spawn(move || {
        let mut offset = 0;
        while offset < input.len() {
            let end = (offset + 256).min(input.len());
            offset += writer.write(&input[offset..end]).unwrap();
            std::thread::yield_now();
        }
        writer_done.store(true, Ordering::Release);
    });
    let mut received = Vec::new();
    while !done.load(Ordering::Acquire) || core.queued().unwrap() > 0 {
        let mut block = [0; 128];
        let count = core.fill(&mut block, true).unwrap();
        received.extend_from_slice(&block[..count]);
        std::thread::yield_now();
    }
    producer.join().unwrap();
    assert_eq!(received, expected);
    assert_eq!(core.snapshot().unwrap().integers[6], 0);
}
#[test]
fn ffi_rejects_null_regions_and_abi_layout_is_fixed() {
    assert_eq!(std::mem::size_of::<Config>(), 44);
    assert_eq!(std::mem::size_of::<Snapshot>(), 168);
    assert_eq!(ffi::mei_usb_rust_core_version(), 1);
    unsafe {
        assert_eq!(
            ffi::mei_pcm_write(std::ptr::null(), std::ptr::null(), 4),
            usize::MAX
        );
        let core = ffi::mei_pcm_new();
        assert!(!core.is_null());
        assert_eq!(ffi::mei_pcm_configure(core, &config()), 1);
        assert_eq!(ffi::mei_pcm_write(core, std::ptr::null(), 4), usize::MAX);
        assert_eq!(ffi::mei_pcm_write(core, std::ptr::null(), 0), 0);
        assert_eq!(
            ffi::mei_pcm_fill(core, std::ptr::null_mut(), 4, 1),
            usize::MAX
        );
        assert_eq!(ffi::mei_pcm_snapshot(core, std::ptr::null_mut()), 0);
        ffi::mei_pcm_free(core);
    }
}
