// Mei USB sample-rate negotiation adapter. SPDX-License-Identifier: Apache-2.0
#pragma once
namespace neri::usb::control {
enum class SampleRateReadback { Verified, Settle, AcceptUnverified };
SampleRateReadback classifySampleRateReadback(int requestedRate, int reportedRate, int attempt);
int sampleRateSettleDelayMs(int attempt);
}
