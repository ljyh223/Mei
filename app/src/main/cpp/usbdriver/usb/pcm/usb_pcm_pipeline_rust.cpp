// Mei Rust adapter; native transport provenance remains in NOTICE.md.
#ifdef MEI_USB_RUST_PCM
#include "mei_usb_pcm.h"
#include "usb_pcm_pipeline.h"
#include <cstring>
#include <limits>
#include <new>
namespace neri::usb {
namespace {
const MeiPcmCore *core(const void *handle) { return static_cast<const MeiPcmCore *>(handle); }
bool result(bool success, std::string *error) {
    if (error)
        *error = success ? "" : "rust_pcm_operation_failed";
    return success;
}
} // namespace
PcmPipeline::PcmPipeline() : rustCore_(mei_pcm_new()) {
    if (!rustCore_)
        throw std::bad_alloc();
}
PcmPipeline::~PcmPipeline() { mei_pcm_free(static_cast<MeiPcmCore *>(rustCore_)); }
bool PcmPipeline::configure(const PcmPipelineConfig &c, std::string *error) {
    const MeiPcmConfig config{c.output.sampleRate,    c.output.channelCount, c.output.subslotBytes,
                              c.output.bitsPerSample, c.output.frameBytes,   c.input.sampleRate,
                              c.input.channelCount,   c.input.encoding,      c.ringDurationMs,
                              c.transferBytes,        c.transferCount};
    return result(mei_pcm_configure(core(rustCore_), &config) != 0, error);
}
bool PcmPipeline::resizeRingDuration(int ms, int bytes, int count, std::string *error) {
    return result(mei_pcm_resize(core(rustCore_), ms, bytes, count) != 0, error);
}
size_t PcmPipeline::write(const uint8_t *data, size_t bytes, std::string *error) {
    const size_t written = mei_pcm_write(core(rustCore_), data, bytes);
    result(written != std::numeric_limits<size_t>::max(), error);
    return written; // SIZE_MAX becomes -1 at the existing JNI write boundary.
}
bool PcmPipeline::drainResampler(std::string *error) {
    const int state = mei_pcm_drain(core(rustCore_));
    result(state >= 0, error);
    return state == 1;
}
size_t PcmPipeline::fill(uint8_t *data, size_t bytes, bool enabled) {
    const size_t count = mei_pcm_fill(core(rustCore_), data, bytes, enabled ? 1 : 0);
    if (count == std::numeric_limits<size_t>::max()) {
        if (data)
            std::memset(data, 0, bytes);
        return 0;
    }
    return count;
}
void PcmPipeline::clear() { mei_pcm_clear(core(rustCore_)); }
void PcmPipeline::resetCounters() { mei_pcm_reset_counters(core(rustCore_)); }
void PcmPipeline::addDroppedFrames(int64_t frames) {
    if (frames > 0)
        mei_pcm_add_dropped(core(rustCore_), static_cast<uint64_t>(frames));
}
void PcmPipeline::setTargetGain(float gain) { mei_pcm_set_gain(core(rustCore_), gain); }
void PcmPipeline::setBitPerfect(bool enabled) { mei_pcm_set_perfect(core(rustCore_), enabled ? 1 : 0); }
bool PcmPipeline::bitPerfect() const { return mei_pcm_is_perfect(core(rustCore_)) != 0; }
void PcmPipeline::armTransportStartRamp() { mei_pcm_arm_ramp(core(rustCore_)); }
void PcmPipeline::applyTransportStartRamp(uint8_t *data, size_t bytes) {
    mei_pcm_apply_ramp(core(rustCore_), data, bytes);
}
size_t PcmPipeline::queuedFrames() const { return mei_pcm_queued(core(rustCore_)); }
PcmPipelineSnapshot PcmPipeline::snapshot() const {
    MeiPcmSnapshot value{};
    mei_pcm_snapshot(core(rustCore_), &value);
    const auto &i = value.integers;
    const auto &f = value.floats;
    return {static_cast<size_t>(i[0]),
            static_cast<size_t>(i[1]),
            static_cast<size_t>(i[2]),
            static_cast<size_t>(i[3]),
            static_cast<int64_t>(i[4]),
            static_cast<int64_t>(i[5]),
            static_cast<int64_t>(i[6]),
            static_cast<int64_t>(i[7]),
            static_cast<int64_t>(i[8]),
            static_cast<int64_t>(i[9]),
            static_cast<int64_t>(i[10]),
            static_cast<int64_t>(i[11]),
            static_cast<int64_t>(i[12]),
            static_cast<int64_t>(i[13]),
            static_cast<int64_t>(i[14]),
            static_cast<int64_t>(i[15]),
            static_cast<int64_t>(i[16]),
            f[0],
            f[1],
            f[2],
            f[3],
            f[4],
            f[5],
            f[6],
            f[7]};
}
} // namespace neri::usb
#endif
