#pragma once
#include <stddef.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
struct MeiUacFormat {
    uint32_t format, terminal, format_type, channels, subslot, bits, rate_kind, minimum, maximum, rate_count;
    uint32_t rates[82];
};
int32_t mei_usb_parse_format(const uint8_t *, size_t, uint32_t, struct MeiUacFormat *);
struct MeiPcmCore;
struct MeiPcmConfig {
    int32_t output_rate, output_channels, output_bytes, output_bits, frame_bytes;
    int32_t input_rate, input_channels, input_encoding, ring_ms, transfer_bytes, transfer_count;
};
struct MeiPcmSnapshot {
    uint64_t integers[17];
    float floats[8];
};
struct MeiPcmCore *mei_pcm_new(void);
void mei_pcm_free(struct MeiPcmCore *);
int32_t mei_pcm_configure(const struct MeiPcmCore *, const struct MeiPcmConfig *);
int32_t mei_pcm_resize(const struct MeiPcmCore *, int32_t, int32_t, int32_t);
size_t mei_pcm_write(const struct MeiPcmCore *, const uint8_t *, size_t);
size_t mei_pcm_fill(const struct MeiPcmCore *, uint8_t *, size_t, int32_t);
int32_t mei_pcm_drain(const struct MeiPcmCore *);
int32_t mei_pcm_clear(const struct MeiPcmCore *);
int32_t mei_pcm_reset_counters(const struct MeiPcmCore *);
void mei_pcm_add_dropped(const struct MeiPcmCore *, uint64_t);
void mei_pcm_set_gain(const struct MeiPcmCore *, float);
void mei_pcm_set_perfect(const struct MeiPcmCore *, int32_t);
int32_t mei_pcm_is_perfect(const struct MeiPcmCore *);
void mei_pcm_arm_ramp(const struct MeiPcmCore *);
void mei_pcm_apply_ramp(const struct MeiPcmCore *, uint8_t *, size_t);
size_t mei_pcm_queued(const struct MeiPcmCore *);
int32_t mei_pcm_snapshot(const struct MeiPcmCore *, struct MeiPcmSnapshot *);
uint32_t mei_usb_rust_core_version(void);
#ifdef __cplusplus
}
static_assert(sizeof(MeiUacFormat) == 368);
static_assert(sizeof(MeiPcmConfig) == 44);
static_assert(sizeof(MeiPcmSnapshot) == 168);
#endif
