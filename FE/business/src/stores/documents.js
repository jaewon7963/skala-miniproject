import { defineStore } from 'pinia'
import { ref } from 'vue'
import { documentApi } from '@/api'
import { DOC_PERIOD, DOC_SORT } from '@/constants/enums'

/** 라이브러리 목록 상태 (DASH-01 ~ DASH-04) */
export const useDocumentStore = defineStore('documents', () => {
  const items = ref([])
  const tags = ref([])
  const total = ref(0)
  const counts = ref({})
  const loading = ref(false)
  const error = ref('')

  const query = ref({
    q: '',
    status: 'ALL',
    period: DOC_PERIOD.D30,
    sort: DOC_SORT.UPDATED_DESC,
    tag: null,
    page: 1,
    size: 20,
  })

  async function fetchList(patch = {}) {
    Object.assign(query.value, patch)
    loading.value = true
    error.value = ''
    try {
      const data = await documentApi.list({ ...query.value })
      items.value = data.items
      total.value = data.total
      counts.value = data.counts ?? {}
    } catch (e) {
      error.value = e.message
      items.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  async function fetchTags() {
    try {
      tags.value = await documentApi.tags()
    } catch {
      tags.value = []
    }
  }

  function resetQuery() {
    query.value = {
      q: '',
      status: 'ALL',
      period: DOC_PERIOD.D30,
      sort: DOC_SORT.UPDATED_DESC,
      tag: null,
      page: 1,
      size: 20,
    }
  }

  const rename = async (id, name) => {
    await documentApi.rename(id, name)
    await fetchList()
  }

  const remove = async (id) => {
    await documentApi.remove(id)
    await fetchList()
  }

  return {
    items,
    tags,
    total,
    counts,
    loading,
    error,
    query,
    fetchList,
    fetchTags,
    resetQuery,
    rename,
    remove,
  }
})
