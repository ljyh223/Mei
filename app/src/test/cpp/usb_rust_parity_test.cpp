#include "mei_usb_pcm.h"
#include "usb/pcm/usb_pcm_pipeline.h"
#include "usb/uac1/usb_uac1_format.h"
#include "usb/uac2/usb_uac2_format.h"
#include <algorithm>
#include <cassert>
#include <cmath>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>
using namespace neri::usb;
static void compare(int inputRate, int outputRate, int encoding, int bits, int bytes, int inputChannels,
                    int outputChannels, bool perfect, float gain) {
    PcmPipeline reference;
    std::unique_ptr<MeiPcmCore, decltype(&mei_pcm_free)> rust(mei_pcm_new(), mei_pcm_free);
    assert(rust && mei_usb_rust_core_version() == 1);
    const int frameBytes = outputChannels * bytes;
    const MeiPcmConfig c{outputRate,    outputChannels, bytes, bits, frameBytes, inputRate,
                         inputChannels, encoding,       3000,  192,  4};
    assert(mei_pcm_configure(rust.get(), &c) == 1);
    std::string error;
    assert(reference.configure({{outputRate, outputChannels, bytes, bits, frameBytes},
                                {inputRate, inputChannels, encoding},
                                3000,
                                192,
                                4},
                               &error));
    reference.setBitPerfect(perfect);
    mei_pcm_set_perfect(rust.get(), perfect ? 1 : 0);
    reference.setTargetGain(gain);
    mei_pcm_set_gain(rust.get(), gain);
    const int width = bytesPerSampleForEncoding(encoding);
    const int frames = 4410;
    std::vector<uint8_t> input(static_cast<size_t>(frames * inputChannels * width));
    for (int f = 0; f < frames; ++f)
        for (int channel = 0; channel < inputChannels; ++channel) {
            const float value = static_cast<float>(std::sin(f * 0.1 + channel * 0.7) * 0.7);
            auto *sample = input.data() + (f * inputChannels + channel) * width;
            if (encoding == 4) {
                union {
                    float f;
                    uint32_t bits;
                } raw{};
                raw.f = value;
                for (int b = 0; b < 4; ++b)
                    sample[b] = static_cast<uint8_t>(raw.bits >> (b * 8));
            } else {
                writeIntegerPcmSample(sample, width, width * 8, value);
                if (isBigEndianIntegerPcmEncoding(encoding))
                    std::reverse(sample, sample + width);
            }
        }
    // Irregular source chunks exercise resampler history and PCM frame alignment.
    for (size_t offset = 0; offset < input.size();) {
        const size_t chunk = std::min(input.size() - offset, static_cast<size_t>(37 * inputChannels * width));
        const auto written = reference.write(input.data() + offset, chunk, &error);
        assert(written == chunk);
        assert(mei_pcm_write(rust.get(), input.data() + offset, chunk) == written);
        offset += written;
    }
    assert(reference.drainResampler(&error));
    assert(mei_pcm_drain(rust.get()) == 1);
    assert(reference.queuedFrames() == mei_pcm_queued(rust.get()));
    const size_t count = reference.queuedFrames() * static_cast<size_t>(frameBytes);
    std::vector<uint8_t> expected(count), actual(count);
    assert(reference.fill(expected.data(), count, true) == count);
    assert(mei_pcm_fill(rust.get(), actual.data(), count, 1) == count);
    if (inputRate == outputRate && perfect && encoding != 4)
        assert(actual == expected);
    else
        for (size_t offset = 0; offset < count; offset += static_cast<size_t>(bytes)) {
            const float a = readIntegerPcmSample(actual.data() + offset, bytes, bits);
            const float e = readIntegerPcmSample(expected.data() + offset, bytes, bits);
            assert(std::abs(a - e) < 0.000002f);
        }
    const auto snapshot = reference.snapshot();
    MeiPcmSnapshot state{};
    assert(mei_pcm_snapshot(rust.get(), &state) == 1);
    assert(state.integers[4] == static_cast<uint64_t>(snapshot.inputBytes));
    assert(state.integers[5] == static_cast<uint64_t>(snapshot.outputBytes));
    assert(state.integers[6] == static_cast<uint64_t>(snapshot.droppedBytes));
    assert(state.integers[0] == 0);
}
static void compareDescriptor(const std::vector<uint8_t> &data) {
    for (int version : {1, 2}) {
        MeiUacFormat parsed{};
        std::string error;
        const bool success = mei_usb_parse_format(data.data(), data.size(), version, &parsed) != 0;
        if (version == 1) {
            neri::usb::uac1::TypeIFormat format;
            assert(success == neri::usb::uac1::parseTypeIFormat(data.data(), static_cast<int>(data.size()),
                                                                &format, &error));
            if (!success)
                continue;
            assert(parsed.format == format.formatTag &&
                   parsed.channels == static_cast<uint32_t>(format.channels));
            assert(parsed.subslot == static_cast<uint32_t>(format.subslotBytes) &&
                   parsed.bits == static_cast<uint32_t>(format.bitsPerSample));
            assert(parsed.rate_kind == static_cast<uint32_t>(format.sampleRateKind));
            assert(parsed.minimum == static_cast<uint32_t>(format.minimumSampleRate) &&
                   parsed.maximum == static_cast<uint32_t>(format.maximumSampleRate));
            assert(parsed.rate_count == format.discreteSampleRates.size());
            for (size_t i = 0; i < parsed.rate_count; ++i)
                assert(parsed.rates[i] == static_cast<uint32_t>(format.discreteSampleRates[i]));
        } else {
            neri::usb::uac2::TypeIFormat format;
            assert(success == neri::usb::uac2::parseTypeIFormat(data.data(), static_cast<int>(data.size()),
                                                                &format, &error));
            if (!success)
                continue;
            assert(parsed.format == format.formats &&
                   parsed.channels == static_cast<uint32_t>(format.channels));
            assert(parsed.subslot == static_cast<uint32_t>(format.subslotBytes) &&
                   parsed.bits == static_cast<uint32_t>(format.bitsPerSample));
            assert(parsed.terminal == static_cast<uint32_t>(format.terminalLink) &&
                   parsed.format_type == format.formatType);
        }
    }
}
static void descriptors() {
    const std::vector<std::vector<uint8_t>> seeds = {
        {7, 0x24, 1, 1, 1, 1, 0, 14, 0x24, 2, 1, 2, 3, 24, 2, 0x44, 0xac, 0, 0x80, 0xbb, 0},
        {7, 0x24, 1, 1, 1, 1, 0, 14, 0x24, 2, 1, 2, 2, 16, 0, 0x80, 0x3e, 0, 0, 0x77, 1},
        {16, 0x24, 1, 1, 0, 1, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 6, 0x24, 2, 1, 4, 24}};
    for (const auto &seed : seeds) {
        compareDescriptor(seed);
        for (size_t size = 0; size < seed.size(); ++size)
            compareDescriptor({seed.begin(), seed.begin() + size});
        for (size_t index = 0; index < seed.size(); ++index)
            for (int value = 0; value < 256; ++value) {
                auto changed = seed;
                changed[index] = static_cast<uint8_t>(value);
                compareDescriptor(changed);
            }
    }
}
int main() {
    descriptors();
    for (int encoding : {2, 21, 22, 4, 0x10000000, 0x50000000, 0x60000000})
        for (int bits : {16, 24, 32})
            compare(44100, 44100, encoding, bits, bits == 16 ? 2 : 4, 2, 2, true, 1.0f);
    for (int rate : {8000, 48000, 88200, 96000, 192000})
        compare(44100, rate, 2, 24, 3, 2, 2, true, 1.0f);
    for (int channels : {1, 2, 8})
        compare(48000, 48000, 2, 24, 4, 1, channels, true, 1.0f);
    compare(48000, 48000, 2, 16, 2, 2, 1, true, 1.0f);
    compare(48000, 48000, 2, 16, 2, 2, 2, false, 0.5f);
    compare(44100, 48000, 4, 24, 3, 2, 2, false, 0.3f);
}
