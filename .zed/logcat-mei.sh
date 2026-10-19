#!/usr/bin/env bash

pid=$(adb shell pidof -s com.ljyh.mei | tr -d '\r')
if [[ ! $pid =~ ^[0-9]+$ ]]; then
    echo 'Mei 未运行，或 Android 设备未连接。' >&2
    exit 1
fi

exec adb logcat -v color --pid="$pid" "$@"
