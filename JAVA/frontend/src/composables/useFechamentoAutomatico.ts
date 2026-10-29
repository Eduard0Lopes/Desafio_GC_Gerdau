import { onUnmounted, ref, watch, type Ref } from 'vue'

export function useAutoClose(
  source: Ref<unknown>,
  onClose: () => void,
  canClose: () => boolean = () => true
) {
  const timeLeft = ref(100)
  let clock: ReturnType<typeof setInterval> | null = null

  watch(source, (novo) => {
    if (clock) clearInterval(clock)
    clock = null
    timeLeft.value = 100
    if (!novo) return
    clock = setInterval(() => {
      timeLeft.value -= 2
      if (timeLeft.value <= 0) {
        if (clock) clearInterval(clock)
        clock = null
        if (canClose()) onClose()
      }
    }, 100)
  })

  onUnmounted(() => {
    if (clock) clearInterval(clock)
  })

  return { timeLeft }
}
