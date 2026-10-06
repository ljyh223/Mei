#include <jni.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <vector>

namespace {
constexpr int kFrames = 1600;
constexpr int kFeatures = 272;
constexpr int kHidden = 150;
constexpr int kConvWidth = 263;
constexpr int kPoolWidth = 131;
constexpr int kProjectionWidth = 262;
constexpr int kGateRows = 4 * kHidden;
constexpr int kModelFloats = 402325;

struct Parameters {
  const float* convolution;
  const float* convolutionBias;
  const float* projection;
  const float* projectionBias;
  const float* inputWeights[2];
  const float* recurrentWeights[2];
  const float* inputBiases[2];
  const float* recurrentBiases[2];
  const float* classifier;
  const float* classifierBias;
};

Parameters ReadParameters(const float* data) {
  const float* cursor = data;
  auto consume = [&cursor](int count) {
    const float* result = cursor;
    cursor += count;
    return result;
  };
  Parameters model{};
  model.convolution = consume(20);
  model.convolutionBias = consume(2);
  model.projection = consume(kHidden * kProjectionWidth);
  model.projectionBias = consume(kHidden);
  for (int layer = 0; layer < 2; ++layer) {
    model.inputWeights[layer] = consume(kGateRows * kHidden);
    model.recurrentWeights[layer] = consume(kGateRows * kHidden);
  }
  for (int layer = 0; layer < 2; ++layer) {
    model.inputBiases[layer] = consume(kGateRows);
    model.recurrentBiases[layer] = consume(kGateRows);
  }
  model.classifier = consume(3 * kHidden);
  model.classifierBias = consume(3);
  return model;
}

float Dot(const float* weights, const float* input, int count) {
  float sum = 0.0f;
  for (int index = 0; index < count; ++index) sum += weights[index] * input[index];
  return sum;
}

float Sigmoid(float value) { return 1.0f / (1.0f + std::exp(-value)); }

struct RecurrentState {
  std::array<float, kHidden> hidden{};
  std::array<float, kHidden> cell{};
};

void AdvanceLayer(const float* input, const float* inputWeights,
                  const float* recurrentWeights, const float* inputBias,
                  const float* recurrentBias, RecurrentState& state,
                  std::array<float, kHidden>& output) {
  std::array<float, kGateRows> gates{};
  for (int row = 0; row < kGateRows; ++row) {
    const int offset = row * kHidden;
    gates[row] = inputBias[row] + recurrentBias[row] +
        Dot(inputWeights + offset, input, kHidden) +
        Dot(recurrentWeights + offset, state.hidden.data(), kHidden);
  }
  // PyTorch LSTM stores its gate rows in input, forget, candidate, output order.
  for (int unit = 0; unit < kHidden; ++unit) {
    const float inputGate = Sigmoid(gates[unit]);
    const float forgetGate = Sigmoid(gates[kHidden + unit]);
    const float candidate = std::tanh(gates[2 * kHidden + unit]);
    const float outputGate = Sigmoid(gates[3 * kHidden + unit]);
    const float nextCell = forgetGate * state.cell[unit] + inputGate * candidate;
    output[unit] = outputGate * std::tanh(nextCell);
    state.cell[unit] = nextCell;
  }
  state.hidden = output;
}

void Predict(const float* weights, const float* features, float* activations) {
  const Parameters model = ReadParameters(weights);
  RecurrentState recurrent[2];
  std::array<float, kProjectionWidth> pooled{};
  std::array<float, kHidden> projected{};
  std::array<float, kHidden> layer0{};
  std::array<float, kHidden> layer1{};
  for (int frame = 0; frame < kFrames; ++frame) {
    const float* feature = features + frame * kFeatures;
    for (int channel = 0; channel < 2; ++channel) {
      std::array<float, kConvWidth> convolved{};
      for (int position = 0; position < kConvWidth; ++position) {
        float value = model.convolutionBias[channel];
        for (int kernel = 0; kernel < 10; ++kernel) {
          value += model.convolution[channel * 10 + kernel] * feature[position + kernel];
        }
        convolved[position] = std::max(0.0f, value);
      }
      for (int position = 0; position < kPoolWidth; ++position) {
        pooled[channel * kPoolWidth + position] =
            std::max(convolved[2 * position], convolved[2 * position + 1]);
      }
    }
    for (int unit = 0; unit < kHidden; ++unit) {
      projected[unit] = model.projectionBias[unit] +
          Dot(model.projection + unit * kProjectionWidth, pooled.data(), kProjectionWidth);
    }
    AdvanceLayer(projected.data(), model.inputWeights[0], model.recurrentWeights[0],
                 model.inputBiases[0], model.recurrentBiases[0], recurrent[0], layer0);
    AdvanceLayer(layer0.data(), model.inputWeights[1], model.recurrentWeights[1],
                 model.inputBiases[1], model.recurrentBiases[1], recurrent[1], layer1);
    std::array<float, 3> logits{};
    for (int output = 0; output < 3; ++output) {
      logits[output] = model.classifierBias[output] +
          Dot(model.classifier + output * kHidden, layer1.data(), kHidden);
    }
    const float highest = *std::max_element(logits.begin(), logits.end());
    float total = 0.0f;
    for (float& value : logits) {
      value = std::exp(value - highest);
      total += value;
    }
    activations[2 * frame] = logits[0] / total;
    activations[2 * frame + 1] = logits[1] / total;
  }
}
}  // namespace

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_ljyh_mei_playback_transition_BeatNetRuntime_predict(
    JNIEnv* environment, jobject, jobject modelBuffer, jfloatArray featureArray) {
  if (modelBuffer == nullptr || featureArray == nullptr ||
      environment->GetDirectBufferCapacity(modelBuffer) != kModelFloats * sizeof(float) ||
      environment->GetArrayLength(featureArray) != kFrames * kFeatures) return nullptr;
  const auto* model = static_cast<const float*>(environment->GetDirectBufferAddress(modelBuffer));
  if (model == nullptr) return nullptr;
  std::vector<float> features(kFrames * kFeatures);
  environment->GetFloatArrayRegion(featureArray, 0, features.size(), features.data());
  if (environment->ExceptionCheck()) return nullptr;
  std::vector<float> activations(2 * kFrames);
  Predict(model, features.data(), activations.data());
  jfloatArray result = environment->NewFloatArray(activations.size());
  if (result != nullptr) {
    environment->SetFloatArrayRegion(result, 0, activations.size(), activations.data());
  }
  return result;
}
