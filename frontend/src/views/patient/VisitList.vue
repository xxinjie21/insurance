<template>
  <div class="visit-list-page">
    <div class="page-header">
      <h2>我的就诊</h2>
    </div>

    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="医院">
          <el-select v-model="searchForm.hospitalId" placeholder="全部医院" clearable style="width: 200px">
            <el-option
              v-for="h in hospitalOptions"
              :key="h.id"
              :label="h.name"
              :value="h.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊时间">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button type="info" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table
      v-loading="loading"
      :data="visitList"
      border
      stripe
      style="width: 100%"
      empty-text="暂无就诊记录"
    >
      <el-table-column prop="id" label="就诊 ID" min-width="90" align="center" />
      <el-table-column prop="hospitalName" label="医院" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.hospitalName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="类型" min-width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'success' : 'warning'">
            {{ getVisitTypeName(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="diagnosis" label="诊断结果" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" min-width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getVisitStatusType(row.status)">
            {{ getVisitStatusName(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="就诊时间" min-width="170" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="160" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            type="primary"
            link
            size="small"
            @click="handlePay(row)"
          >
            余额支付
          </el-button>
          <el-button type="info" link size="small" @click="handleViewDetail(row)">
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

    <el-dialog v-model="detailVisible" title="就诊详情" width="700px">
      <el-descriptions v-if="selectedVisit" :column="1" border>
        <el-descriptions-item label="就诊 ID">{{ selectedVisit.id }}</el-descriptions-item>
        <el-descriptions-item label="医院名称">{{ selectedVisit.hospitalName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就诊类型">{{ getVisitTypeName(selectedVisit.type) }}</el-descriptions-item>
        <el-descriptions-item label="诊断结果">{{ selectedVisit.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="就诊状态">{{ getVisitStatusName(selectedVisit.status) }}</el-descriptions-item>
        <el-descriptions-item label="就诊时间">
          {{ formatDateTime(selectedVisit.createTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider v-if="selectedVisit" />

      <div v-if="selectedVisit">
        <h4 style="margin-bottom: 12px;">医疗项目明细</h4>
        <el-table v-loading="feeLoading" :data="feeItems" border stripe style="width: 100%" empty-text="暂无费用记录">
          <el-table-column prop="name" label="项目名称" min-width="160" />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getFeeTypeTag(row.type)">{{ getFeeTypeName(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="110" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="num" label="数量" width="80" align="center" />
          <el-table-column label="总价" width="110" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.total) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, VISIT_API, ACCOUNT_API, FEE_API, HOSPITAL_API } from '@/utils/request'
import { formatDateTime, formatMoney, getVisitTypeName, getVisitStatusName, getVisitStatusType, getFeeTypeName, getFeeTypeTag } from '@/utils/format'
import { readPage } from '@/utils/page'
import type { PageResult, VisitVO, FeeVO } from '@/types'

const loading = ref(false)
const visitList = ref<VisitVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const selectedVisit = ref<VisitVO | null>(null)
const feeLoading = ref(false)
const feeItems = ref<FeeVO[]>([])

const hospitalOptions = ref<Array<{ id: string; name: string }>>([])

const searchForm = reactive({
  timeRange: null as [string, string] | null,
  hospitalId: null as string | null,
})

const fetchHospitals = async () => {
  try {
    const response = await http.get(HOSPITAL_API.LIST, {
      params: { pageNum: 1, pageSize: 100 },
    })
    const page = readPage(response)
    hospitalOptions.value = page.records.map((h: any) => ({ id: h.id, name: h.name }))
  } catch (error) {
    console.error('获取医院列表失败:', error)
  }
}

const buildParams = () => {
  const params: Record<string, string | number> = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  }
  if (searchForm.timeRange?.length === 2) {
    params.startTime = searchForm.timeRange[0]
    params.endTime = searchForm.timeRange[1]
  }
  if (searchForm.hospitalId) {
    params.hospitalId = searchForm.hospitalId
  }
  return params
}

const fetchData = async () => {
  loading.value = true
  try {
    const response = await http.get<PageResult<VisitVO>>(VISIT_API.MY_LIST, {
      params: buildParams(),
    })
    const page = readPage(response)
    visitList.value = page.records
    total.value = page.total
  } catch (error) {
    console.error('获取就诊列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.timeRange = null
  searchForm.hospitalId = null
  pageNum.value = 1
  fetchData()
}

const handlePay = async (row: VisitVO) => {
  try {
    await ElMessageBox.confirm('确认使用账户余额支付该就诊自付部分吗？', '余额支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await http.post(ACCOUNT_API.PAY, { visitId: row.id })
    ElMessage.success('支付成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('支付失败:', error)
    }
  }
}

const handleViewDetail = async (row: VisitVO) => {
  selectedVisit.value = row
  detailVisible.value = true
  feeItems.value = []
  if (row.id) {
    feeLoading.value = true
    try {
      const response = await http.get<FeeVO[]>(FEE_API.LIST_BY_VISIT, {
        params: { visitId: row.id },
      })
      feeItems.value = response.data || []
    } catch (error) {
      console.error('获取费用明细失败:', error)
    } finally {
      feeLoading.value = false
    }
  }
}

onMounted(() => {
  fetchHospitals()
  fetchData()
})
</script>

<style lang="scss" scoped>
.visit-list-page {
  .page-header {
    margin-bottom: 24px;

    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }

  .search-form {
    margin-bottom: 24px;
    padding: 20px;
    background-color: #f5f7fa;
    border-radius: 8px;
  }

  .pagination {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
