<template>
  <el-dialog
    :model-value="modelValue"
    title="选择医院"
    width="700px"
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="handleClosed"
  >
    <div class="hospital-selector">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="医院名称">
          <el-input
            v-model="searchName"
            placeholder="输入医院名称搜索"
            clearable
            style="width: 240px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="hospitalList"
        border
        stripe
        style="width: 100%"
        empty-text="暂无医院数据"
        @row-click="handleSelect"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="医院名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="handleSelect(row)">
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { http, HOSPITAL_API } from '@/utils/request'
import { readPage } from '@/utils/page'
import type { PageResult } from '@/types'

interface HospitalVO {
  id: string
  name: string
  address: string
  phone: string
  status: number
  createTime?: string
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  select: [hospital: HospitalVO]
}>()

const loading = ref(false)
const hospitalList = ref<HospitalVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchName = ref('')

const fetchHospitals = async () => {
  loading.value = true
  try {
    const params: Record<string, string | number> = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchName.value.trim()) {
      params.name = searchName.value.trim()
    }
    const response = await http.get<PageResult<HospitalVO>>(HOSPITAL_API.LIST, { params })
    const page = readPage(response)
    hospitalList.value = page.records
    total.value = page.total
  } catch (error) {
    console.error('获取医院列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchHospitals()
}

const handleSizeChange = () => {
  pageNum.value = 1
  fetchHospitals()
}

const handleCurrentChange = () => {
  fetchHospitals()
}

const handleSelect = (row: HospitalVO) => {
  emit('select', row)
  emit('update:modelValue', false)
}

const handleClosed = () => {
  searchName.value = ''
  pageNum.value = 1
}

watch(() => props.modelValue, (val) => {
  if (val) {
    fetchHospitals()
  }
})
</script>

<style lang="scss" scoped>
.hospital-selector {
  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
