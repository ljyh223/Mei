//! Checked parsing of untrusted USB AudioStreaming Type-I descriptors.
#![forbid(unsafe_code)]
#[repr(C)]
#[derive(Clone)]
pub struct Format {
    pub format: u32,
    pub terminal: u32,
    pub format_type: u32,
    pub channels: u32,
    pub subslot: u32,
    pub bits: u32,
    pub rate_kind: u32,
    pub minimum: u32,
    pub maximum: u32,
    pub rate_count: u32,
    pub rates: [u32; 82],
}
impl Default for Format {
    fn default() -> Self {
        Self {
            format: 0,
            terminal: 0,
            format_type: 0,
            channels: 0,
            subslot: 0,
            bits: 0,
            rate_kind: 0,
            minimum: 0,
            maximum: 0,
            rate_count: 0,
            rates: [0; 82],
        }
    }
}
fn le24(bytes: &[u8]) -> u32 {
    bytes[0] as u32 | (bytes[1] as u32) << 8 | (bytes[2] as u32) << 16
}
pub fn parse(data: &[u8], version: u32) -> Option<Format> {
    if version != 1 && version != 2 {
        return None;
    }
    let mut parsed = Format::default();
    let mut general = false;
    let mut format = false;
    let mut remaining = data;
    while !remaining.is_empty() {
        let header = remaining.get(..2)?;
        let length = header[0] as usize;
        if length < 2 {
            return None;
        }
        let descriptor = remaining.get(..length)?;
        remaining = &remaining[length..];
        if descriptor[1] != 0x24 {
            continue;
        }
        let subtype = *descriptor.get(2)?;
        match (version, subtype) {
            (1, 1) => {
                let d = descriptor.get(..7)?;
                parsed.format = u16::from_le_bytes([d[5], d[6]]) as u32;
                general = true;
            }
            (2, 1) => {
                let d = descriptor.get(..16)?;
                parsed.terminal = d[3] as u32;
                parsed.format_type = d[5] as u32;
                parsed.format = u32::from_le_bytes(d[6..10].try_into().ok()?);
                parsed.channels = d[10] as u32;
                if parsed.terminal == 0 || parsed.channels == 0 {
                    return None;
                }
                general = true;
            }
            (1, 2) => {
                let d = descriptor.get(..8)?;
                if d[3] != 1 {
                    return None;
                }
                parsed.channels = d[4] as u32;
                parsed.subslot = d[5] as u32;
                parsed.bits = d[6] as u32;
                let count = d[7] as usize;
                if count == 0 {
                    let d = descriptor.get(..14)?;
                    parsed.rate_kind = 2;
                    parsed.minimum = le24(&d[8..11]);
                    parsed.maximum = le24(&d[11..14]);
                    if parsed.minimum == 0 || parsed.maximum < parsed.minimum {
                        return None;
                    }
                } else {
                    if count > parsed.rates.len() {
                        return None;
                    }
                    let d = descriptor.get(..8 + count * 3)?;
                    for (index, rate) in d[8..].chunks_exact(3).enumerate() {
                        parsed.rates[index] = le24(rate);
                        if parsed.rates[index] == 0 {
                            return None;
                        }
                    }
                    parsed.rates[..count].sort_unstable();
                    let mut unique = 0;
                    for index in 0..count {
                        if unique == 0 || parsed.rates[index] != parsed.rates[unique - 1] {
                            parsed.rates[unique] = parsed.rates[index];
                            unique += 1;
                        }
                    }
                    parsed.rate_kind = 1;
                    parsed.rate_count = unique as u32;
                }
                if parsed.channels == 0
                    || parsed.subslot == 0
                    || parsed.bits == 0
                    || parsed.bits > parsed.subslot * 8
                {
                    return None;
                }
                format = true;
            }
            (2, 2) => {
                let d = descriptor.get(..6)?;
                if d[3] != 1 {
                    return None;
                }
                parsed.subslot = d[4] as u32;
                parsed.bits = d[5] as u32;
                if parsed.subslot == 0 || parsed.bits == 0 || parsed.bits > parsed.subslot * 8 {
                    return None;
                }
                format = true;
            }
            _ => {}
        }
    }
    (general && format).then_some(parsed)
}
#[cfg(test)]
mod tests {
    use super::*;
    const UAC1: &[u8] = &[
        7, 0x24, 1, 1, 1, 1, 0, 14, 0x24, 2, 1, 2, 3, 24, 2, 0x44, 0xac, 0, 0x80, 0xbb, 0,
    ];
    const UAC2: &[u8] = &[
        16, 0x24, 1, 1, 0, 1, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 6, 0x24, 2, 1, 4, 24,
    ];
    #[test]
    fn valid_streaming_formats_preserve_pcm_fields() {
        let one = parse(UAC1, 1).unwrap();
        assert_eq!((one.channels, one.bits, one.subslot), (2, 24, 3));
        assert_eq!(&one.rates[..2], &[44100, 48000]);
        let two = parse(UAC2, 2).unwrap();
        assert_eq!(
            (two.terminal, two.channels, two.bits, two.subslot),
            (1, 2, 24, 4)
        );
    }
    #[test]
    fn all_truncated_prefixes_are_rejected() {
        for (data, version) in [(UAC1, 1), (UAC2, 2)] {
            for length in 0..data.len() {
                assert!(parse(&data[..length], version).is_none());
            }
        }
    }
    #[test]
    fn malformed_lengths_and_unknown_versions_are_rejected() {
        for (data, version) in [(UAC1, 1), (UAC2, 2)] {
            for value in [0, 1, 255] {
                let mut corrupt = data.to_vec();
                corrupt[0] = value;
                assert!(parse(&corrupt, version).is_none());
            }
        }
        assert!(parse(UAC1, 3).is_none());
        assert_eq!(std::mem::size_of::<Format>(), 368);
    }
    #[test]
    fn arbitrary_descriptor_bytes_do_not_panic() {
        let mut state = 0x7138u32;
        for size in 0..512 {
            let data: Vec<u8> = (0..size)
                .map(|_| {
                    state = state.wrapping_mul(1664525).wrapping_add(1013904223);
                    (state >> 24) as u8
                })
                .collect();
            let _ = parse(&data, 1);
            let _ = parse(&data, 2);
        }
    }
}
