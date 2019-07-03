#kill `pidof /android-sdk/emulator/qemu/linux-x86_64/qemu-system-x86_64-headless`

adb devices | grep "emulator-" | while read -r emulator device; do
  adb -s $emulator emu kill
done

#adb kill-server
#adb start-server
