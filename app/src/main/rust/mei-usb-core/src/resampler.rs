#![forbid(unsafe_code)]
//! Kaiser-windowed polyphase sinc, ported from the attributed NeriPlayer native resampler.
//! Integer phase accumulation and retained filter history make output independent of write chunking.
use std::sync::Arc;
const PHASES: usize = 128;
#[derive(Clone)]
struct Kernel {
    half: usize,
    coefficients: Vec<f32>,
    deltas: Vec<f32>,
}
#[derive(Clone)]
pub struct Resampler {
    kernel: Arc<Kernel>,
    input_rate: u64,
    output_rate: u64,
    channels: usize,
    fifo: Vec<f32>,
    base: usize,
    fraction: u64,
}
fn bessel(x: f64) -> f64 {
    let mut sum = 1.0;
    let mut term = 1.0;
    for k in 1..64 {
        let f = x * 0.5 / k as f64;
        term *= f * f;
        sum += term;
        if term < sum * 1e-17 {
            break;
        }
    }
    sum
}
impl Resampler {
    pub fn new(input_rate: usize, output_rate: usize, channels: usize) -> Self {
        let cutoff = 0.5 * 0.96 * (output_rate as f64 / input_rate as f64).min(1.0);
        let half = ((32.0 / (2.0 * cutoff)).ceil() as usize).clamp(32, 320);
        let taps = half * 2;
        let mut coefficients = vec![0.0; (PHASES + 1) * taps];
        let mut row = vec![0.0; taps];
        for phase in 0..=PHASES {
            let mut total = 0.0;
            for (tap, value) in row.iter_mut().enumerate() {
                let t = tap as f64 - half as f64 + 1.0 - phase as f64 / PHASES as f64;
                let angle = std::f64::consts::PI * 2.0 * cutoff * t;
                let sinc = if angle.abs() < 1e-12 {
                    1.0
                } else {
                    angle.sin() / angle
                };
                let ratio = 1.0 - (t / half as f64).powi(2);
                let window = if ratio <= 0.0 {
                    0.0
                } else {
                    bessel(9.0 * ratio.sqrt()) / bessel(9.0)
                };
                *value = 2.0 * cutoff * sinc * window;
                total += *value;
            }
            for tap in 0..taps {
                coefficients[phase * taps + tap] = (row[tap] / total) as f32;
            }
        }
        let mut deltas = vec![0.0; PHASES * taps];
        for i in 0..deltas.len() {
            deltas[i] = coefficients[i + taps] - coefficients[i];
        }
        let mut result = Self {
            kernel: Arc::new(Kernel {
                half,
                coefficients,
                deltas,
            }),
            input_rate: input_rate as u64,
            output_rate: output_rate as u64,
            channels,
            fifo: Vec::new(),
            base: 0,
            fraction: 0,
        };
        result.reset();
        result
    }
    pub fn reset(&mut self) {
        self.fraction = 0;
        self.base = self.kernel.half - 1;
        self.fifo.clear();
        self.fifo.resize(self.base * self.channels, 0.0);
    }
    pub fn acceptable_input(&self, available: usize, max_output: usize) -> usize {
        let next = (max_output as u64 * self.input_rate + self.fraction) / self.output_rate;
        (next as usize + self.kernel.half + self.base)
            .saturating_sub(self.fifo.len() / self.channels)
            .min(available)
    }
    pub fn process(&mut self, input: &[f32], output: &mut Vec<f32>) {
        self.fifo.extend_from_slice(input);
        let half = self.kernel.half;
        let taps = half * 2;
        let mut weights = [0.0f32; 640];
        while self.base + half < self.fifo.len() / self.channels {
            let position = self.fraction as f64 * PHASES as f64 / self.output_rate as f64;
            let phase = (position as usize).min(PHASES - 1);
            let blend = (position - phase as f64) as f32;
            for (tap, weight) in weights[..taps].iter_mut().enumerate() {
                let index = phase * taps + tap;
                *weight = self.kernel.coefficients[index] + blend * self.kernel.deltas[index];
            }
            let first = (self.base + 1 - half) * self.channels;
            for channel in 0..self.channels {
                let mut sum = 0.0f32;
                for (tap, weight) in weights[..taps].iter().enumerate() {
                    sum += self.fifo[first + channel + tap * self.channels] * weight;
                }
                output.push(sum);
            }
            self.fraction += self.input_rate;
            self.base += (self.fraction / self.output_rate) as usize;
            self.fraction %= self.output_rate;
        }
        let drop = self
            .base
            .saturating_sub(half - 1)
            .min(self.fifo.len() / self.channels);
        if drop > 0 {
            self.fifo.drain(..drop * self.channels);
            self.base -= drop;
        }
    }
    pub fn drain(&mut self, output: &mut Vec<f32>) {
        let silence = vec![0.0; self.kernel.half * self.channels];
        self.process(&silence, output);
        self.reset();
    }
}
