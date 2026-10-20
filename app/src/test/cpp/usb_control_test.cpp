#include "usb/control/usb_feature_unit.h"
#include "usb/control/usb_sample_rate_readback.h"
#include "usb/pcm/usb_pcm_codec.h"
#include <cassert>
#include <cmath>
#include <cstdint>
#include <vector>
using namespace neri::usb::control;
int main() {
    // UAC1 speaker and microphone paths share an AudioControl interface.
    const uint8_t descriptors[] = {
        10,0x24,6,2,1,1,3,3,3,0,
        9,0x24,3,3,1,3,0,2,0,
        8,0x24,6,5,4,1,3,0,
        9,0x24,3,6,1,1,0,5,0
    };
    auto controls = findPlaybackFeatureUnitControls(descriptors, sizeof(descriptors), 1);
    assert(controls.size() == 6);
    for (auto c : controls) assert(c.unitId == 2);
    const uint8_t uac2[] = {10,0x24,6,4,1,15,0,0,0,0, 12,0x24,3,3,1,3,0,4,0,0,0,0};
    assert(findPlaybackFeatureUnitControls(uac2, sizeof(uac2), 2).size() == 2);
    for (size_t length = 0; length < sizeof(descriptors); ++length) {
        findPlaybackFeatureUnitControls(descriptors, length, 1);
    }
    const uint8_t malformed[] = {0,0x24,6};
    assert(findPlaybackFeatureUnitControls(malformed, sizeof(malformed), 1).empty());
    const uint8_t cycle[] = {8,0x24,6,2,2,1,3,0, 9,0x24,3,3,1,3,0,2,0};
    assert(findPlaybackFeatureUnitControls(cycle, sizeof(cycle), 1).size() == 2);
    assert(quantizeVolumeToResolution(-101, -1000, 0, 100) == -200);
    assert(quantizeVolumeToResolution(100, -1000, 0, 100) == 0);
    assert(unityVolumeWithinRange(-8000, 1000) == 0);
    assert(hardwareVolumeForFraction(0, -8000, 0) == -8000);
    assert(hardwareVolumeForFraction(1, -8000, 0) == 0);
    assert(hardwareVolumeForFraction(0.5f, -8000, 0) < 0);
    assert(muteRestoreValue(true, 1) == 1);
    assert(muteRestoreValue(false, 1) == 0);
    uint8_t range[] = {1,0,0,0xe0,0,0,0,1};
    int16_t low = 0, high = 0; uint16_t step = 0;
    assert(decodeUac2VolumeRange(range, sizeof(range), &low, &high, &step));
    assert(low == -8192 && high == 0 && step == 256);
    assert(!decodeUac2VolumeRange(range, 7, &low, &high, &step));
    std::vector<FeatureUnitVolume> volumes = {{{2,1,2},-8000,0,1},{{2,0,2},-8000,0,1},{{5,0,2},-8000,0,1}};
    const auto selected = selectHardwareVolumeControls(volumes);
    assert(selected.size() == 1 && selected[0].control.unitId == 2 && selected[0].control.channel == 0);
    assert(classifySampleRateReadback(48000,48000,0) == SampleRateReadback::Verified);
    assert(classifySampleRateReadback(48000,44100,0) == SampleRateReadback::Settle);
    assert(classifySampleRateReadback(48000,44100,3) == SampleRateReadback::AcceptUnverified);
    const int64_t values[] = {-8388608,-4194305,-1,0,1,4194305,8388607};
    for (const auto value : values) {
        uint8_t packed[3] = {}, container[4] = {};
        neri::usb::writeIntegerPcmValue(packed,3,24,value);
        assert(neri::usb::readEncodedIntegerPcmSample(packed,21) == value);
        neri::usb::writeIntegerPcmValue(container,4,24,value);
        const float decoded = neri::usb::readIntegerPcmSample(container,4,24);
        assert(decoded == static_cast<float>(value) / 8388608.0f);
    }
    for (int value = -32768; value <= 32767; ++value) {
        uint8_t sample[2] = {};
        neri::usb::writeIntegerPcmValue(sample,2,16,value);
        assert(neri::usb::readEncodedIntegerPcmSample(sample,2) == value);
    }
}
