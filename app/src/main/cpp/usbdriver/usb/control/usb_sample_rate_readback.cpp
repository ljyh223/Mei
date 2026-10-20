// Mei USB sample-rate negotiation adapter. SPDX-License-Identifier: Apache-2.0
#include "usb/control/usb_sample_rate_readback.h"
#include <algorithm>
namespace neri::usb::control {
SampleRateReadback classifySampleRateReadback(int requested, int actual, int attempt) {
    if (actual == requested && requested > 0) return SampleRateReadback::Verified;
    if (actual > 0 && attempt < 3) return SampleRateReadback::Settle;
    return SampleRateReadback::AcceptUnverified;
}
int sampleRateSettleDelayMs(int attempt) { return 10 * (std::clamp(attempt, 0, 3) + 1); }
}
