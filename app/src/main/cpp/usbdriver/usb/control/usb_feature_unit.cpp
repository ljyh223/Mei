// Mei USB Audio Class control adapter. SPDX-License-Identifier: Apache-2.0
#include "usb/control/usb_feature_unit.h"
#include <algorithm>
#include <cmath>
#include <map>
#include <set>
namespace neri::usb::control {
int16_t decodeLittleEndianInt16(const uint8_t* p) { return static_cast<int16_t>(p[0] | (p[1] << 8)); }
void encodeLittleEndianInt16(int16_t value, uint8_t* p) { p[0] = value & 255; p[1] = (static_cast<uint16_t>(value) >> 8) & 255; }
uint8_t muteRestoreValue(bool known, uint8_t original) { return known ? original : 0; }
int16_t unityVolumeWithinRange(int16_t low, int16_t high) { return low <= high ? std::clamp(0, int(low), int(high)) : 0; }
int16_t quantizeVolumeToResolution(int value, int16_t low, int16_t high, uint16_t step) {
    if (low > high) return 0;
    int v = std::clamp(value, int(low), int(high));
    if (step > 0 && step <= 32767 && step <= high - low) v = low + (v - low) / step * step;
    return static_cast<int16_t>(v);
}
int16_t hardwareVolumeForFraction(float fraction, int16_t low, int16_t high, uint16_t step) {
    const float f = std::isfinite(fraction) ? std::clamp(fraction, 0.0f, 1.0f) : 0.0f;
    const int v = f <= 0 ? low : static_cast<int>(std::floor(40.0 * std::log10(f) * 256.0));
    return quantizeVolumeToResolution(std::min(v, 0), low, high, step);
}
bool decodeUac2VolumeRange(const uint8_t* p, size_t size, int16_t* low, int16_t* high, uint16_t* step) {
    if (!p || !low || !high || !step || size < 8 || (p[0] | (p[1] << 8)) == 0) return false;
    *low = decodeLittleEndianInt16(p + 2); *high = decodeLittleEndianInt16(p + 4);
    *step = static_cast<uint16_t>(decodeLittleEndianInt16(p + 6));
    return *low <= *high;
}
std::vector<FeatureUnitVolume> selectHardwareVolumeControls(const std::vector<FeatureUnitVolume>& values) {
    std::vector<FeatureUnitVolume> selected;
    for (const auto& value : values) {
        if (value.minimum >= value.maximum) continue;
        if (selected.empty() || selected.front().control.unitId == value.control.unitId) selected.push_back(value);
    }
    auto master = std::find_if(selected.begin(), selected.end(), [](const auto& v) { return v.control.channel == 0; });
    return master == selected.end() ? selected : std::vector<FeatureUnitVolume>{*master};
}
std::vector<FeatureUnitControl> findPlaybackFeatureUnitControls(const uint8_t* bytes, size_t size, int version) {
    struct Node { std::vector<int> inputs; std::vector<FeatureUnitControl> controls; };
    std::map<int, Node> nodes;
    std::vector<int> outputs;
    if (!bytes || (version != 1 && version != 2)) return {};
    for (size_t offset = 0; offset + 3 <= size;) {
        const uint8_t* p = bytes + offset;
        const size_t length = p[0];
        if (length < 3 || length > size - offset) break;
        offset += length;
        if (p[1] != 0x24 || length < 5) continue;
        const int subtype = p[2], id = p[3];
        if (subtype == 3 && length >= 9) {
            const unsigned terminal = p[4] | (p[5] << 8);
            nodes[id].inputs.push_back(p[7]);
            if ((terminal & 0xff00) != 0x0100) outputs.push_back(id);
        } else if (subtype == 6 && length >= (version == 1 ? 8u : 10u)) {
            auto& node = nodes[id];
            node.inputs.push_back(p[4]);
            const size_t width = version == 1 ? p[5] : 4;
            const size_t first = version == 1 ? 6 : 5;
            if (width == 0 || width > 4) continue;
            int channel = 0;
            for (size_t i = first; i + width < length; i += width, ++channel) {
                uint32_t mask = 0;
                for (size_t j = 0; j < width; ++j) mask |= uint32_t(p[i + j]) << (j * 8);
                for (uint8_t selector : {kFeatureUnitMuteSelector, kFeatureUnitVolumeSelector}) {
                    const bool writable = version == 1 ? (mask & (1u << (selector - 1))) != 0 :
                        ((mask >> (2 * (selector - 1))) & 3u) == 3u;
                    if (writable) node.controls.push_back({id, channel, selector});
                }
            }
        } else if ((subtype == 4 || subtype == 5) && length >= 6 && p[4] <= length - 5) {
            for (size_t i = 0; i < p[4]; ++i) nodes[id].inputs.push_back(p[5 + i]);
        }
    }
    std::vector<FeatureUnitControl> result;
    std::set<int> visited;
    for (size_t i = 0; i < outputs.size(); ++i) {
        const int id = outputs[i];
        if (!visited.insert(id).second) continue;
        const auto found = nodes.find(id);
        if (found == nodes.end()) continue;
        result.insert(result.end(), found->second.controls.begin(), found->second.controls.end());
        outputs.insert(outputs.end(), found->second.inputs.begin(), found->second.inputs.end());
    }
    return result;
}
}
