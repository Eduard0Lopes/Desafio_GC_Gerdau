export function debounce<A extends unknown[]>(callback: (...args: A) => void, delay: number): ((...args: A) => void) & { cancel: () => void } {
  let timer: ReturnType<typeof setTimeout> | null = null
  const run = (...args: A): void => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => callback(...args), delay)
  }
  run.cancel = (): void => {
    if (timer) clearTimeout(timer)
    timer = null
  }
  return run
}

export function wait(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
