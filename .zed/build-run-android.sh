#!/usr/bin/env bash
set -e

device_list=$(adb devices -l)
devices=()
while read -r serial state details; do
    if [[ $state != device ]]; then
        continue
    fi

    if [[ $details =~ (^|[[:space:]])model:([^[:space:]]+) ]]; then
        devices+=("$serial (${BASH_REMATCH[2]})")
    else
        devices+=("$serial")
    fi
done <<< "$device_list"

if (( ${#devices[@]} == 0 )); then
    printf '没有可用的 ADB 设备。请连接设备并确认已授权 USB 或无线调试。\n%s\n' "$device_list" >&2
    exit 1
fi

target=${devices[0]}
if (( ${#devices[@]} > 1 )); then
    target=''
    PS3='请选择要运行 Mei 的设备编号：'
    select target in "${devices[@]}"; do
        if [[ -n $target ]]; then
            break
        fi
        printf '无效的设备编号，请重试。\n' >&2
    done
    if [[ -z $target ]]; then
        printf '已取消设备选择。\n' >&2
        exit 1
    fi
fi

serial=${target%% *}
printf '目标设备：%s\n' "$target"
ANDROID_SERIAL="$serial" ./gradlew :app:installDebug --console=plain
exec adb -s "$serial" shell am start -W \
    -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
    -n com.ljyh.mei/.MainActivity
