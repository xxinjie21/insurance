<template>
  <div class="settle-list-page">
    <div class="page-header">
      <h2>我的结算</h2>
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
        <el-form-item label="结算时间">
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
      :data="settleList"
      border
      stripe
      style="width: 100%"
      empty-text="暂无结算单"
    >
      <el-table-column prop="id" label="结算单号" min-width="100" align="center" />
      <el-table-column prop="visitId" label="就诊 ID" min-width="90" align="center" />
      <el-table-column prop="hospitalName" label="医院" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.hospitalName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="总费用" min-width="110" align="right">
        <template #default="{ row }">
          <span class="amount-total">¥{{ formatMoney(row.total) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="报销金额" min-width="110" align="right">
        <template #default="{ row }">
          <span class="amount-reimburse">¥{{ formatMoney(row.reimburse) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="自付金额" min-width="110" align="right">
        <template #default="{ row }">
          <span class="amount-self-pay">¥{{ formatMoney(row.selfPay) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getSettleStatusType(row.status)">
            {{ getSettleStatusName(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结算时间" min-width="170" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="140" align="center">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewFees(row)">
            查看费用明细
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

    <el-dialog v-model="feeDialogVisible" title="费用明细" width="700px">
      <el-table v-loading="feeLoading" :data="feeItems" border stripe style="width: 100%">
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
      <template #footer>
        <el-button @click="feeDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { http, SETTLE_API, HOSPITAL_API, FEE_API } from '@/utils/request'
import {
  formatDateTime,
  formatMoney,
  getSettleStatusName,
  getSettleStatusType,
  getFeeTypeName,
  getFeeTypeTag,
} from '@/utils/format'
import { readPage } from '@/utils/page'
import type { PageResult, SettleVO, FeeVO } from '@/types'

const loading = ref(false)
const settleList = ref<SettleVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const feeDialogVisible = ref(false)
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
    const response = await http.get<PageResult<SettleVO>>(SETTLE_API.MY_LIST, {
      params: buildParams(),
    })
    const page = readPage(response)
    settleList.value = page.records
    total.value = page.total
  } catch (error) {
    console.error('获取结算单列表失败:', error)
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

const handleViewFees = async (row: SettleVO) => {
  if (!row.visitId) return
  feeLoading.value = true
  feeDialogVisible.value = true
  try {
    const response = await http.get<FeeVO[]>(FEE_API.LIST_BY_VISIT, {
      params: { visitId: row.visitId },
    })
    feeItems.value = response.data || []
  } catch (error) {
    console.error('获取费用明细失败:', error)
  } finally {
    feeLoading.value = false
  }
}

onMounted(() => {
  fetchHospitals()
  fetchData()
})
</script>

<style lang="scss" scoped>
.settle-list-page {
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

  .amount-total {
    font-weight: 600;
  }

  .amount-reimburse {
    color: #52c41a;
  }

  .amount-self-pay {
    color: #f5222d;
  }
}
</style>
