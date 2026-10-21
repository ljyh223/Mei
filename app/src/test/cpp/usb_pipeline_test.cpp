#include "usb/pcm/usb_pcm_pipeline.h"
#include <cassert>
#include <cmath>
#include <cstdint>
#include <vector>
using namespace neri::usb;

int main() {
    std::string error;
    // 44.1 -> 48 kHz, 16 -> 24 bit: preserve duration, polarity and channel separation.
    PcmPipeline pipeline;
    assert(pipeline.configure({{48000,2,3,24,6},{44100,2,2},3000,192,4}, &error));
    pipeline.setBitPerfect(true);
    std::vector<uint8_t> input(441 * 4);
    for (int f = 0; f < 441; ++f) {
        writeIntegerPcmValue(input.data() + f * 4,2,16,3276);
        writeIntegerPcmValue(input.data() + f * 4 + 2,2,16,-6553);
    }
    assert(pipeline.write(input.data(), input.size(), &error) == input.size());
    assert(pipeline.drainResampler(&error));
    assert(std::abs(static_cast<int64_t>(pipeline.queuedFrames()) - 480) <= 1);
    std::vector<uint8_t> output(static_cast<size_t>(pipeline.queuedFrames()) * 6);
    assert(pipeline.fill(output.data(), output.size(), true) == output.size());
    assert(std::abs(readIntegerPcmSample(output.data() + 100 * 6,3,24) - 0.1f) < 0.001f);
    assert(std::abs(readIntegerPcmSample(output.data() + 100 * 6 + 3,3,24) + 0.2f) < 0.001f);
    assert(pipeline.snapshot().droppedBytes == 0);

    // Stereo sent to an eight-channel DAC: only the first two channels contain audio.
    assert(pipeline.configure({{48000,8,2,16,16},{48000,2,2},3000,192,4}, &error));
    uint8_t stereo[4] = {0xE8,0x03,0x18,0xFC};
    assert(pipeline.write(stereo, sizeof(stereo), &error) == sizeof(stereo));
    uint8_t surround[16] = {};
    assert(pipeline.fill(surround, sizeof(surround), true) == sizeof(surround));
    assert(readEncodedIntegerPcmSample(surround,2) == 1000);
    assert(readEncodedIntegerPcmSample(surround + 2,2) == -1000);
    for (int channel = 2; channel < 8; ++channel) assert(readEncodedIntegerPcmSample(surround + channel * 2,2) == 0);

    // A fixed reserve lets foreground watermark changes drain an existing background queue losslessly.
    assert(pipeline.configure({{48000,2,2,16,4},{48000,2,2},3000,192,4}, &error));
    std::vector<uint8_t> queued(48000 * 4);
    for (size_t i = 0; i < queued.size(); ++i) queued[i] = static_cast<uint8_t>(i);
    assert(pipeline.write(queued.data(), queued.size(), &error) == queued.size());
    std::vector<uint8_t> recovered(queued.size());
    assert(pipeline.fill(recovered.data(), recovered.size(), true) == recovered.size());
    assert(queued == recovered);
    assert(pipeline.snapshot().droppedBytes == 0);
}
