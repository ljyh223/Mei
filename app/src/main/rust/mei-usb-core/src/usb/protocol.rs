//! Checked UAC controls, topology, feedback and USB service-interval arithmetic.
#![forbid(unsafe_code)]
use std::collections::{HashMap, HashSet};
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct RateRange {
    pub min: u32,
    pub max: u32,
    pub step: u32,
}
impl RateRange {
    pub fn accepts(self, rate: u32) -> bool {
        rate >= self.min
            && rate <= self.max
            && (self.step == 0 || (rate - self.min) % self.step == 0)
    }
}
pub fn ranges(data: &[u8]) -> Option<Vec<RateRange>> {
    let count = u16::from_le_bytes(data.get(..2)?.try_into().ok()?) as usize;
    if count == 0 || count > 256 || data.len() != 2 + count * 12 {
        return None;
    }
    data[2..]
        .chunks_exact(12)
        .map(|d| {
            let min = u32::from_le_bytes(d[..4].try_into().ok()?);
            let max = u32::from_le_bytes(d[4..8].try_into().ok()?);
            let step = u32::from_le_bytes(d[8..12].try_into().ok()?);
            (min >= 8000 && max <= 768000 && max >= min && step <= max).then_some(RateRange {
                min,
                max,
                step,
            })
        })
        .collect()
}
pub fn descriptors(data: &[u8]) -> Option<Vec<&[u8]>> {
    let mut result = Vec::new();
    let mut tail = data;
    while !tail.is_empty() {
        let n = *tail.first()? as usize;
        if n < 2 {
            return None;
        }
        result.push(tail.get(..n)?);
        tail = &tail[n..];
    }
    Some(result)
}
#[derive(Clone, Debug)]
pub enum Clock {
    Source { id: u8, writable: bool },
    Selector { id: u8, pins: Vec<u8> },
    Multiplier(u8),
}
pub fn clocks(data: &[u8]) -> HashMap<u8, Clock> {
    descriptors(data)
        .unwrap_or_default()
        .into_iter()
        .filter_map(|d| {
            if d.get(1) != Some(&0x24) || d.len() < 4 {
                return None;
            }
            let id = d[3];
            let clock = match d[2] {
                0x0a if d.len() >= 8 => Clock::Source {
                    id,
                    writable: d[5] & 3 == 3,
                },
                0x0b if d.len() >= 7 && d.len() >= 7 + d[4] as usize => Clock::Selector {
                    id,
                    pins: d[5..5 + d[4] as usize].to_vec(),
                },
                0x0c if d.len() >= 7 => Clock::Multiplier(d[4]),
                _ => return None,
            };
            Some((id, clock))
        })
        .collect()
}
pub fn terminal_clock(data: &[u8], terminal: u8) -> Option<u8> {
    descriptors(data)?.into_iter().find_map(|d| {
        if d.get(1) != Some(&0x24) || d.get(3) != Some(&terminal) {
            return None;
        }
        match d.get(2) {
            Some(2) if d.len() >= 17 => Some(d[7]),
            Some(3) if d.len() >= 12 => Some(d[8]),
            _ => None,
        }
    })
}
pub fn resolve_clock(
    graph: &HashMap<u8, Clock>,
    start: u8,
    mut selector: impl FnMut(u8) -> Option<u8>,
    mut multiplier: impl FnMut(u8) -> Option<(u16, u16)>,
) -> Option<(u8, bool)> {
    let mut current = start;
    let mut seen = HashSet::new();
    while seen.insert(current) {
        match graph.get(&current)? {
            Clock::Source { id, writable } => return Some((*id, *writable)),
            Clock::Selector { id, pins } => {
                current = *pins.get(selector(*id)?.checked_sub(1)? as usize)?;
            }
            Clock::Multiplier(source) => {
                let (num, den) = multiplier(current)?;
                // Non-unity multipliers require transforming the clock range and its resolution.
                if num == 0 || num != den {
                    return None;
                }
                current = *source;
            }
        }
    }
    None
}
#[derive(Clone, Debug)]
pub struct Feature {
    pub unit: u8,
    pub channel: u8,
    pub selector: u8,
}
pub fn playback_features(data: &[u8], version: u8) -> Vec<Feature> {
    let ds = descriptors(data).unwrap_or_default();
    let by_id: HashMap<u8, &[u8]> = ds
        .iter()
        .filter(|d| d.get(1) == Some(&0x24) && d.len() >= 4)
        .map(|d| (d[3], *d))
        .collect();
    let mut pending: Vec<u8> = ds
        .iter()
        .filter(|d| d.get(1) == Some(&0x24) && d.get(2) == Some(&3) && d.len() >= 9 && d[5] != 1)
        .map(|d| d[7])
        .collect();
    let mut seen = HashSet::new();
    let mut result = Vec::new();
    while let Some(id) = pending.pop() {
        if !seen.insert(id) {
            continue;
        }
        let Some(d) = by_id.get(&id) else {
            continue;
        };
        match d[2] {
            6 if d.len() >= 7 => {
                pending.push(d[4]);
                let (start, width) = if version == 2 {
                    (5, 4)
                } else {
                    (6, d[5] as usize)
                };
                if width == 0 || width > 4 || d.len() <= start + width {
                    continue;
                }
                for (channel, control) in d[start..d.len() - 1]
                    .chunks_exact(width)
                    .enumerate()
                    .take(9)
                {
                    let bits = control
                        .iter()
                        .enumerate()
                        .fold(0u32, |v, (i, b)| v | ((*b as u32) << (i * 8)));
                    for selector in 1..=2 {
                        let writable = if version == 2 {
                            (bits >> ((selector - 1) * 2)) & 3 == 3
                        } else {
                            bits & (1 << (selector - 1)) != 0
                        };
                        if writable {
                            result.push(Feature {
                                unit: id,
                                channel: channel as u8,
                                selector,
                            });
                        }
                    }
                }
            }
            3 if d.len() >= 8 => pending.push(d[7]),
            4 | 5 if d.len() >= 5 && d.len() >= 5 + d[4] as usize => {
                pending.extend_from_slice(&d[5..5 + d[4] as usize])
            }
            _ => {}
        }
    }
    // Prefer a unit's master volume; retain channel volumes only if no master exists.
    let masters: HashSet<(u8, u8)> = result
        .iter()
        .filter(|f| f.channel == 0)
        .map(|f| (f.unit, f.selector))
        .collect();
    result.retain(|f| f.channel == 0 || !masters.contains(&(f.unit, f.selector)));
    result
}
pub fn intervals(speed: i32, interval: u8) -> u32 {
    let base = if speed >= 3 { 8000u32 } else { 1000 };
    (base >> interval.clamp(1, 16).saturating_sub(1)).max(1)
}
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct TransferPlan {
    pub packets: usize,
    pub slots: usize,
}
/// Keep the same bounded USB scheduling window as the C++ baseline so language
/// comparisons do not accidentally compare different in-flight audio latencies.
pub fn transfer_plan(ips: u32, feedback: bool) -> TransferPlan {
    let ips = ips.max(1);
    if feedback {
        return TransferPlan {
            packets: (ips as usize * 8 / 1000).clamp(8, 128),
            slots: 16,
        };
    }
    let packets = if ips <= 1000 {
        32
    } else {
        (32 * (ips as usize / 1000))
            .clamp(32, 128)
            .min((ips as usize * 8 / 1000).clamp(32, 128))
    };
    let duration = if ips > 1000 { 160 } else { 320 };
    let slots = (ips as usize * duration)
        .div_ceil(1000)
        .div_ceil(packets)
        .clamp(8, 16);
    TransferPlan { packets, slots }
}
#[derive(Clone, Debug)]
pub struct Scheduler {
    rate: f64,
    phase: f64,
    intervals: u32,
    max_frames: usize,
}
impl Scheduler {
    pub fn new(rate: u32, intervals: u32, max_frames: usize) -> Option<Self> {
        (intervals > 0 && rate.div_ceil(intervals) as usize <= max_frames).then_some(Self {
            rate: rate as f64,
            phase: 0.,
            intervals,
            max_frames,
        })
    }
    pub fn set_rate(&mut self, rate: f64) -> bool {
        if !rate.is_finite()
            || rate <= 0.
            || (rate / self.intervals as f64).ceil() as usize > self.max_frames
        {
            return false;
        }
        self.rate = rate;
        true
    }
    pub fn next(&mut self) -> usize {
        self.phase += self.rate / self.intervals as f64;
        let frames = self.phase.floor() as usize;
        self.phase -= frames as f64;
        frames.min(self.max_frames)
    }
}
pub fn feedback(data: &[u8], high_speed: bool, nominal: u32) -> Option<f64> {
    let raw = match data.len() {
        3 => u32::from_le_bytes([data[0], data[1], data[2], 0]),
        4 => u32::from_le_bytes(data.try_into().ok()?),
        _ => return None,
    } as f64;
    // Prefer UAC's specified representation, then known 3/4-byte device variants.
    let expected = if data.len() == 3 {
        raw / 16384. * 1000.
    } else {
        raw / 65536. * if high_speed { 8000. } else { 1000. }
    };
    [
        expected,
        raw / 65536. * 1000.,
        raw / 16384. * 1000.,
        raw / 65536. * 8000.,
    ]
    .into_iter()
    .find(|r| r.is_finite() && (*r / nominal as f64 - 1.).abs() < 0.01)
}
pub fn volume_value(fraction: f32, min: i16, max: i16, step: i16) -> i16 {
    let max = max.min(0).max(min);
    let wanted = if fraction <= 0. {
        min as f32
    } else {
        40. * fraction.clamp(0., 1.).log10() * 256.
    };
    let value = (wanted.floor() as i32).clamp(min as i32, max as i32);
    let step = (step as i32).max(1);
    (min as i32 + (value - min as i32) / step * step) as i16
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn scheduling_windows_match_existing_transport() {
        assert_eq!(
            transfer_plan(1000, false),
            TransferPlan {
                packets: 32,
                slots: 10
            }
        );
        assert_eq!(
            transfer_plan(8000, false),
            TransferPlan {
                packets: 64,
                slots: 16
            }
        );
        assert_eq!(
            transfer_plan(1000, true),
            TransferPlan {
                packets: 8,
                slots: 16
            }
        );
        assert_eq!(
            transfer_plan(8000, true),
            TransferPlan {
                packets: 64,
                slots: 16
            }
        );
    }
    #[test]
    fn malformed_range_table_and_resolution_are_rejected() {
        let mut data = vec![1, 0];
        data.extend_from_slice(&44100u32.to_le_bytes());
        data.extend_from_slice(&192000u32.to_le_bytes());
        data.extend_from_slice(&100u32.to_le_bytes());
        let r = ranges(&data).unwrap();
        assert!(r[0].accepts(48000));
        assert!(!r[0].accepts(48001));
        for n in 0..data.len() {
            assert!(ranges(&data[..n]).is_none());
        }
        data[6..10].copy_from_slice(&8000u32.to_le_bytes());
        assert!(ranges(&data).is_none());
    }
    #[test]
    fn feature_controls_exclude_capture_and_prefer_master() {
        let playback = [9, 0x24, 6, 2, 1, 1, 3, 3, 0, 9, 0x24, 3, 3, 1, 3, 0, 2, 0];
        let result = playback_features(&playback, 1);
        assert_eq!(result.len(), 2);
        assert!(result.iter().all(|f| f.unit == 2 && f.channel == 0));
        let mut capture = playback;
        capture[14] = 1;
        assert!(playback_features(&capture, 1).is_empty());
        for n in 0..playback.len() {
            let _ = playback_features(&playback[..n], 1);
        }
    }
    #[test]
    fn feedback_frequency_changes_preserve_fractional_phase() {
        let mut scheduler = Scheduler::new(44100, 1000, 100).unwrap();
        assert_eq!(scheduler.next(), 44);
        assert!(scheduler.set_rate(44200.));
        let count: usize = (0..10000).map(|_| scheduler.next()).sum();
        assert!((count as i64 - 442000).abs() <= 1);
    }
    #[test]
    fn rate_resolution_is_enforced() {
        let r = RateRange {
            min: 44100,
            max: 176400,
            step: 44100,
        };
        assert!(r.accepts(88200));
        assert!(!r.accepts(48000));
        assert!(!r.accepts(192000));
        assert!(ranges(&[0, 0]).is_none());
        for n in 0..64 {
            let _ = ranges(&vec![255; n]);
            let _ = descriptors(&vec![255; n]);
        }
    }
    #[test]
    fn selectors_and_cycles_fail_closed() {
        let mut g = HashMap::new();
        g.insert(
            1,
            Clock::Selector {
                id: 1,
                pins: vec![2, 3],
            },
        );
        g.insert(
            2,
            Clock::Source {
                id: 2,
                writable: true,
            },
        );
        g.insert(3, Clock::Multiplier(1));
        assert_eq!(
            resolve_clock(&g, 1, |_| Some(1), |_| Some((1, 1))),
            Some((2, true))
        );
        assert_eq!(resolve_clock(&g, 1, |_| Some(0), |_| Some((1, 1))), None);
        assert_eq!(resolve_clock(&g, 1, |_| Some(2), |_| Some((1, 1))), None);
        assert_eq!(resolve_clock(&g, 3, |_| Some(1), |_| Some((2, 1))), None);
    }
    #[test]
    fn fractional_packets_preserve_long_term_frame_count() {
        for speed in [2, 3] {
            for rate in [44100, 48000, 96000, 176400, 192000, 384000] {
                let ips = intervals(speed, 1);
                let mut s = Scheduler::new(rate, ips, 1000).unwrap();
                let total: usize = (0..ips * 10).map(|_| s.next()).sum();
                assert!((total as i64 - rate as i64 * 10).abs() <= 1);
                assert!(!s.set_rate(f64::NAN));
            }
        }
        assert!(Scheduler::new(192000, 1000, 100).is_none());
    }
    #[test]
    fn feedback_scaling_and_corruption() {
        let fs = ((48f64 * 16384.) as u32).to_le_bytes();
        assert_eq!(feedback(&fs[..3], false, 48000), Some(48000.));
        let hs = (6u32 * 65536).to_le_bytes();
        assert_eq!(feedback(&hs, true, 48000), Some(48000.));
        assert_eq!(feedback(&[255; 4], true, 48000), None);
        assert_eq!(feedback(&[0; 3], false, 48000), None);
    }
    #[test]
    fn volume_never_exceeds_unity_and_quantizes_from_minimum() {
        for f in [0., 0.01, 0.1, 0.5, 1., 2.] {
            let v = volume_value(f, -25000, 1200, 128);
            assert!(v <= 0);
            assert_eq!((v as i32 + 25000) % 128, 0);
        }
    }
}
