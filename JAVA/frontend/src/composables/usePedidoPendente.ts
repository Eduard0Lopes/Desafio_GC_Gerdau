import { computed, ref } from 'vue'
import { api, errorMessage } from '../services/api'
import { toast } from '../services/toast'
import { canManageRequest } from '../services/usuario'
import type { FullCodeResponse } from '../types'

export function usePendingRequest(options: { afterCancel?: () => void | Promise<void> } = {}) {
  const isOpen = ref(false)
  const request = ref<FullCodeResponse | null>(null)
  const saving = ref(false)
  const processing = ref(false)
  const confirmingCancel = ref(false)

  const canManage = computed(() => Boolean(request.value && canManageRequest(request.value)))

  async function open(idOrRequest: string | FullCodeResponse): Promise<void> {
    try {
      request.value = typeof idOrRequest === 'string' ? await api.fullCodes.getById(idOrRequest) : idOrRequest
      isOpen.value = true
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }

  function close(): void {
    isOpen.value = false
    request.value = null
    confirmingCancel.value = false
  }

  async function save(data: { descricao: string; justificativa: string }): Promise<void> {
    if (!request.value || saving.value) return
    saving.value = true
    try {
      request.value = await api.fullCodes.updateRequest(request.value.id, {
        descricao: data.descricao || null,
        justificativa: data.justificativa || null
      })
      toast.success('Pedido atualizado — a governança verá a versão mais nova.')
    } catch (error) {
      toast.error(errorMessage(error))
    } finally {
      saving.value = false
    }
  }

  async function confirmCancelation(): Promise<void> {
    if (!request.value || processing.value) return
    processing.value = true
    confirmingCancel.value = false
    try {
      await api.fullCodes.cancelRequest(request.value.id)
      close()
      toast.success('Pedido cancelado. A composição está livre para um novo envio.')
      await options.afterCancel?.()
    } catch (error) {
      toast.error(errorMessage(error))
    } finally {
      processing.value = false
    }
  }

  return {
    isOpen,
    request,
    canManage,
    saving,
    processing,
    confirmingCancel,
    open,
    close,
    save,
    confirmCancelation
  }
}
