<template>
  <div class="consumption-list-page">
    <div class="page-header">
      <h2>消费记录</h2>
      <p>可按时间筛选；不选日期时展示全部记录，按时间倒序</p>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="消费时间">
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
    </el-card>

    <el-table
      v-loading="loading"
      :data="consumptionList"
      border
      stripe
      style="width: 100%"
      empty-text="暂无消费记录"
    >
      <el-table-column prop="id" label="ID" min-width="80" align="center" />
      <el-table-column prop="orderNo" label="消费单号" min-width="160" show-overflow-tooltip />
      <el-table-column label="就诊 ID" min-width="90" align="center">
        <template #default="{ row }">
          {{ row.visitId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="消费金额" min-width="110" align="right">
        <template #default="{ row }">
          <span class="amount-out">-¥{{ formatMoney(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="消费类型" min-width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getConsumptionTypeTag(row.type)">
            {{ getConsumptionTypeName(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '成功' : '处理中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="消费前余额" min-width="110" align="right">
        <template #default="{ row }">
          ¥{{ formatMoney(row.balanceBefore) }}
        </template>
      </el-table-column>
      <el-table-column label="消费后余额" min-width="110" align="right">
        <template #default="{ row }">
          ¥{{ formatMoney(row.balanceAfter) }}
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.remark || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="消费时间" min-width="170" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="120" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.visitId"
            type="primary"
            link
            size="small"
            @click="handleViewFee(row)"
          >
            查看费用
          </el-button>
          <span v-else class="no-action">-</span>
        </template>
      </el-table-column>
    </el-table>

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

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { http, ACCOUNT_API, FEE_API } from '@/utils/request'
import { formatDateTime, formatMoney, getFeeTypeName, getFeeTypeTag } from '@/utils/format'
import { buildPageQuery } from '@/utils/listQuery'
import { readPage } from '@/utils/page'
import type { PageResult, ConsumptionRecordVO, FeeVO } from '@/types'

const loading = ref(false)
const consumptionList = ref<ConsumptionRecordVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const feeDialogVisible = ref(false)
const feeLoading = ref(false)
const feeItems = ref<FeeVO[]>([])

const searchForm = reactive({
  timeRange: null as [string, string] | null,
})

const fetchData = async () => {
  loading.value = true
  try {
    const response = await http.get<PageResult<ConsumptionRecordVO>>(ACCOUNT_API.CONSUMPTION_LIST, {
      params: buildPageQuery(pageNum.value, pageSize.value, searchForm.timeRange),
    })
    const page = readPage(response)
    consumptionList.value = page.records
    total.value = page.total
  } catch (error) {
    console.error('获取消费记录失败:', error)
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
  pageNum.value = 1
  fetchData()
}

const handleSizeChange = () => {
  pageNum.value = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const getConsumptionTypeName = (type: number) => {
  const types: Record<number, string> = {
    1: '就诊支付',
    2: '其他',
  }
  return types[type] || '未知'
}

const getConsumptionTypeTag = (type: number) => {
  const types: Record<number, string> = {
    1: 'primary',
    2: 'info',
  }
  return types[type] || ''
}

const handleViewFee = async (row: ConsumptionRecordVO) => {
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
  fetchData()
})
</script>

<style lang="scss" scoped>
.consumption-list-page {
  .page-header {
    margin-bottom: 24px;

    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a1a;
    }

    p {
      margin-top: 6px;
      color: #666;
      font-size: 14px;
    }
  }

  .search-card {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
  }

  .amount-out {
    color: #f5222d;
    font-weight: 600;
  }
}
</style>
