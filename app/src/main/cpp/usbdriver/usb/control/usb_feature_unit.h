// Mei USB Audio Class control adapter. SPDX-License-Identifier: Apache-2.0
#pragma once
#include <cstddef>
#include <cstdint>
#include <vector>
namespace neri::usb::control {
inline constexpr uint8_t kFeatureUnitMuteSelector = 1;
inline constexpr uint8_t kFeatureUnitVolumeSelector = 2;
struct FeatureUnitControl { int unitId = 0; int channel = 0; uint8_t selector = 0; };
struct FeatureUnitVolume { FeatureUnitControl control; int16_t minimum = 0; int16_t maximum = 0; uint16_t resolution = 1; };
std::vector<FeatureUnitControl> findPlaybackFeatureUnitControls(const uint8_t*, size_t, int);
int16_t unityVolumeWithinRange(int16_t, int16_t);
uint8_t muteRestoreValue(bool, uint8_t);
int16_t quantizeVolumeToResolution(int, int16_t, int16_t, uint16_t);
std::vector<FeatureUnitVolume> selectHardwareVolumeControls(const std::vector<FeatureUnitVolume>&);
int16_t hardwareVolumeForFraction(float, int16_t, int16_t, uint16_t = 1);
bool decodeUac2VolumeRange(const uint8_t*, size_t, int16_t*, int16_t*, uint16_t*);
int16_t decodeLittleEndianInt16(const uint8_t*);
void encodeLittleEndianInt16(int16_t, uint8_t*);
}
