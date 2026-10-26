//! Owned, checked configuration descriptors read over a granted device fd.
#![forbid(unsafe_code)]
use std::collections::HashSet;
#[derive(Clone, Debug)]
pub struct Endpoint {
    pub address: u8,
    pub attributes: u8,
    pub interval: u8,
    pub capacity: usize,
    pub extra: Vec<u8>,
    pub sync_address: u8,
}
#[derive(Clone, Debug)]
pub struct Alt {
    pub number: u8,
    pub alternate: u8,
    pub class: u8,
    pub subclass: u8,
    pub protocol: u8,
    pub extra: Vec<u8>,
    pub endpoints: Vec<Endpoint>,
}
#[derive(Debug)]
pub struct Configuration {
    pub value: u8,
    pub extra: Vec<u8>,
    pub alts: Vec<Alt>,
}
pub fn parse(data: &[u8], speed: i32) -> Result<Configuration, &'static str> {
    if data.len() < 9
        || data[0] != 9
        || data[1] != 2
        || u16::from_le_bytes([data[2], data[3]]) as usize != data.len()
        || data[5] == 0
    {
        return Err("invalid_usb_configuration");
    }
    let mut result = Configuration {
        value: data[5],
        extra: vec![],
        alts: vec![],
    };
    let mut seen = HashSet::new();
    let mut endpoints = 0;
    let mut offset = 9;
    while offset < data.len() {
        let n = *data.get(offset).ok_or("truncated_usb_descriptor")? as usize;
        if n < 2 || offset + n > data.len() {
            return Err("invalid_usb_descriptor_length");
        }
        let d = &data[offset..offset + n];
        offset += n;
        match d[1] {
            4 => {
                if n < 9 || !seen.insert((d[2], d[3])) {
                    return Err("invalid_usb_alternate");
                }
                if result
                    .alts
                    .last()
                    .is_some_and(|a| a.endpoints.len() != endpoints)
                {
                    return Err("usb_endpoint_count_mismatch");
                }
                endpoints = d[4] as usize;
                result.alts.push(Alt {
                    number: d[2],
                    alternate: d[3],
                    class: d[5],
                    subclass: d[6],
                    protocol: d[7],
                    extra: vec![],
                    endpoints: vec![],
                });
            }
            5 => {
                if n < 7 {
                    return Err("truncated_usb_endpoint");
                }
                let a = result
                    .alts
                    .last_mut()
                    .ok_or("usb_endpoint_without_interface")?;
                if d[2] & 15 == 0 || a.endpoints.iter().any(|e| e.address == d[2]) {
                    return Err("invalid_usb_endpoint_address");
                }
                let raw = u16::from_le_bytes([d[4], d[5]]);
                let transactions = (raw >> 11) & 3;
                if transactions == 3 {
                    return Err("invalid_usb_packet_multiplier");
                }
                let capacity = (raw & 0x7ff) as usize
                    * if speed == 3 && matches!(d[3] & 3, 1 | 3) {
                        1 + transactions as usize
                    } else {
                        1
                    };
                if capacity == 0 {
                    return Err("zero_usb_packet_capacity");
                }
                a.endpoints.push(Endpoint {
                    address: d[2],
                    attributes: d[3],
                    interval: d[6],
                    capacity,
                    extra: vec![],
                    sync_address: d.get(8).copied().unwrap_or(0),
                });
            }
            11 => result.extra.extend_from_slice(d), // IAD may occur between any two interfaces.
            0x30 | 0x31 => {
                let ep = result
                    .alts
                    .last_mut()
                    .and_then(|a| a.endpoints.last_mut())
                    .ok_or("usb_companion_without_endpoint")?;
                let capacity = if d[1] == 0x30 && n >= 6 {
                    u16::from_le_bytes([d[4], d[5]]) as usize
                } else if d[1] == 0x31 && n >= 8 {
                    u32::from_le_bytes(d[4..8].try_into().map_err(|_| "invalid_usb_companion")?)
                        as usize
                } else {
                    return Err("truncated_usb_companion");
                };
                if speed >= 5 && ep.attributes & 3 == 1 {
                    if capacity == 0 || capacity > 49152 {
                        return Err("invalid_usb_interval_capacity");
                    }
                    ep.capacity = capacity;
                }
                ep.extra.extend_from_slice(d);
            }
            _ => {
                if let Some(a) = result.alts.last_mut() {
                    if d[1] != 0x24 {
                        if let Some(ep) = a.endpoints.last_mut() {
                            ep.extra.extend_from_slice(d);
                            continue;
                        }
                    }
                    a.extra.extend_from_slice(d);
                } else {
                    result.extra.extend_from_slice(d);
                }
            }
        }
    }
    if result
        .alts
        .last()
        .is_some_and(|a| a.endpoints.len() != endpoints)
    {
        return Err("usb_endpoint_count_mismatch");
    }
    let interfaces: HashSet<u8> = result.alts.iter().map(|a| a.number).collect();
    if interfaces.len() != data[4] as usize {
        return Err("usb_interface_count_mismatch");
    }
    Ok(result)
}
#[cfg(test)]
mod tests {
    use super::*;
    fn config(body: &[u8], count: u8) -> Vec<u8> {
        let len = (body.len() + 9) as u16;
        let mut d = vec![9, 2, len as u8, (len >> 8) as u8, count, 1, 0, 0x80, 50];
        d.extend_from_slice(body);
        d
    }
    #[test]
    fn uac_extras_and_feedback_endpoints_stay_with_their_alternate() {
        let d = config(
            &[
                8, 11, 0, 2, 1, 0, 0, 0, 9, 4, 0, 0, 0, 1, 1, 0x20, 0, 9, 0x24, 1, 0, 2, 0, 0, 0,
                0, 9, 4, 1, 1, 2, 1, 2, 0x20, 0, 6, 0x24, 2, 1, 3, 24, 9, 5, 1, 5, 0x26, 1, 1, 0,
                0x81, 7, 0x25, 1, 0, 0, 0, 0, 7, 5, 0x81, 0x11, 4, 0, 1,
            ],
            2,
        );
        let c = parse(&d, 3).unwrap();
        assert_eq!(c.extra.len(), 8);
        let a = &c.alts[1];
        assert_eq!(a.extra.len(), 6);
        assert_eq!(a.endpoints.len(), 2);
        assert_eq!(a.endpoints[0].extra.len(), 7);
        assert_eq!(a.endpoints[0].capacity, 294);
        assert_eq!(a.endpoints[0].sync_address, 0x81);
        for n in 0..d.len() {
            assert!(parse(&d[..n], 3).is_err());
        }
    }
    #[test]
    fn superspeed_bytes_per_interval_overrides_legacy_max_packet() {
        let d = config(
            &[
                9, 4, 0, 1, 1, 1, 2, 0x20, 0, 7, 5, 1, 5, 0, 4, 1, 6, 0x30, 0, 1, 0, 12,
            ],
            1,
        );
        assert_eq!(parse(&d, 5).unwrap().alts[0].endpoints[0].capacity, 3072);
    }
    #[test]
    fn malformed_counts_lengths_and_duplicate_alternates_fail() {
        let d = config(&[9, 4, 0, 0, 1, 1, 2, 0, 0], 1);
        assert!(parse(&d, 2).is_err());
        let d = config(&[9, 4, 0, 0, 0, 1, 2, 0, 0, 9, 4, 0, 0, 0, 1, 2, 0, 0], 1);
        assert!(parse(&d, 2).is_err());
        for n in 0..512 {
            let _ = parse(&vec![255; n], 3);
        }
    }
}
