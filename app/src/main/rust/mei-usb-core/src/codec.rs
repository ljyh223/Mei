#![forbid(unsafe_code)]
//! PCM codecs. Integer widening never passes through a float.
pub fn width(encoding: i32) -> Option<usize> {
    match encoding {
        3 => Some(1),
        2 | 0x10000000 => Some(2),
        21 | 0x50000000 => Some(3),
        4 | 22 | 0x60000000 => Some(4),
        _ => None,
    }
}
pub fn integer_bits(encoding: i32) -> Option<u32> {
    match encoding {
        2 | 0x10000000 => Some(16),
        21 | 0x50000000 => Some(24),
        22 | 0x60000000 => Some(32),
        _ => None,
    }
}
pub fn integer(input: &[u8], encoding: i32) -> i64 {
    let bytes = width(encoding).unwrap_or(0);
    let big = matches!(encoding, 0x10000000 | 0x50000000 | 0x60000000);
    let mut raw = 0u32;
    for (i, &value) in input.iter().take(bytes).enumerate() {
        let shift = if big { bytes - 1 - i } else { i } * 8;
        raw |= (value as u32) << shift;
    }
    let shift = 32 - bytes as u32 * 8;
    ((raw << shift) as i32 >> shift) as i64
}
pub fn decode(input: &[u8], encoding: i32) -> f32 {
    match encoding {
        3 => (input[0] as f32 - 128.0) / 128.0,
        4 => finite(f32::from_le_bytes(input[..4].try_into().unwrap())),
        _ => integer_bits(encoding).map_or(0.0, |bits| {
            (integer(input, encoding) as f64 / (1u64 << (bits - 1)) as f64) as f32
        }),
    }
}
pub fn finite(value: f32) -> f32 {
    if value.is_finite() {
        value.clamp(-1.0, 1.0)
    } else {
        0.0
    }
}
pub fn gain(value: f32) -> f32 {
    if value.is_finite() {
        value.clamp(0.0, 1.0)
    } else {
        0.0
    }
}
pub fn encode_integer(output: &mut [u8], bits: u32, value: i64) {
    let scale = 1i64 << (bits - 1);
    let clipped = value.clamp(-scale, scale - 1);
    let encoded = (clipped as u32) << (output.len() as u32 * 8 - bits);
    for (index, byte) in output.iter_mut().enumerate() {
        *byte = (encoded >> (index * 8)) as u8;
    }
}
pub fn encode(output: &mut [u8], bits: u32, value: f32) {
    let value = (finite(value) as f64 * (1u64 << (bits - 1)) as f64).round() as i64;
    encode_integer(output, bits, value);
}
pub fn decode_output(input: &[u8], bits: u32) -> f32 {
    let mut raw = 0u32;
    for (index, &byte) in input.iter().enumerate() {
        raw |= (byte as u32) << (index * 8);
    }
    raw >>= input.len() as u32 * 8 - bits;
    let shift = 32 - bits;
    let signed = ((raw << shift) as i32 >> shift) as i64;
    (signed as f64 / (1u64 << (bits - 1)) as f64) as f32
}
