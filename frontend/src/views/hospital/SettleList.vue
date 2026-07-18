<template>
  <div class="settle-list-page">
    <div class="page-header">
      <div>
        <h2>结算单管理</h2>
        <p>查看本院所有医保结算单，可按患者和结算时间筛选</p>
      </div>
      <el-button type="info" icon="Refresh" @click="fetchSettleList">刷新</el-button>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="患者">
          <div class="patient-selector">
            <el-input
              v-model="searchForm.patientName"
              placeholder="输入姓名搜索患者"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearchPatient"
            />
            <el-button type="primary" @click="handleSearchPatient" :loading="searchingPatient">
              搜索
            </el-button>
            <el-tag
              v-if="selectedPatient"
              closable
              type="warning"
              @close="handleClearPatient"
              style="margin-left: 8px"
            >
              {{ selectedPatient.name }} ({{ selectedPatient.idCard }})
            </el-tag>
          </div>
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
    </el-card>

    <el-card>
      <el-table
        v-loading="loading"
        :data="settleList"
        border
        stripe
        style="width: 100%"
        empty-text="请输入患者姓名搜索并选择患者后查询"
      >
        <el-table-column prop="id" label="结算单号" min-width="100" align="center" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatIdCard(row) }}
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
        <el-table-column label="操作" min-width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="warning"
              link
              size="small"
              @click="openBatchDialog(row)"
            >
              加入批次
            </el-button>
            <el-button
              v-if="row.status !== 0"
              type="success"
              link
              size="small"
              @click="viewBatchInfo(row)"
            >
              查看批次
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageParams.pageNum"
          v-model:page-size="pageParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 选择患者对话框 -->
    <el-dialog v-model="patientDialogVisible" title="选择患者" width="500px">
      <el-table
        :data="patientList"
        border
        highlight-current-row
        @row-click="handleSelectPatient"
        style="width: 100%"
        empty-text="未找到匹配患者"
      >
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="idCard" label="身份证号" min-width="160" />
      </el-table>
      <template #footer>
        <el-button @click="patientDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 结算单详情对话框 -->
    <el-dialog v-model="detailVisible" title="结算单详情" width="560px">
      <el-descriptions v-if="selectedSettle" :column="1" border>
        <el-descriptions-item label="结算单号">{{ selectedSettle.id }}</el-descriptions-item>
        <el-descriptions-item label="就诊 ID">{{ selectedSettle.visitId }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ selectedSettle.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ formatIdCard(selectedSettle) }}</el-descriptions-item>
        <el-descriptions-item label="总费用">¥{{ formatMoney(selectedSettle.total) }}</el-descriptions-item>
        <el-descriptions-item label="报销金额">¥{{ formatMoney(selectedSettle.reimburse) }}</el-descriptions-item>
        <el-descriptions-item label="自付金额">¥{{ formatMoney(selectedSettle.selfPay) }}</el-descriptions-item>
        <el-descriptions-item v-if="selectedSettle.poolingPay != null" label="统筹支付">¥{{ formatMoney(selectedSettle.poolingPay) }}</el-descriptions-item>
        <el-descriptions-item v-if="selectedSettle.catastrophicPay" label="大病支付">¥{{ formatMoney(selectedSettle.catastrophicPay) }}</el-descriptions-item>
        <el-descriptions-item v-if="selectedSettle.assistancePay" label="医疗救助">¥{{ formatMoney(selectedSettle.assistancePay) }}</el-descriptions-item>
        <el-descriptions-item v-if="selectedSettle.accountPay != null" label="个账支付">¥{{ formatMoney(selectedSettle.accountPay) }}</el-descriptions-item>
        <el-descriptions-item v-if="selectedSettle.cashPay != null" label="现金支付">¥{{ formatMoney(selectedSettle.cashPay) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getSettleStatusType(selectedSettle.status)">
            {{ getSettleStatusName(selectedSettle.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="结算时间">
          {{ formatDateTime(selectedSettle.createTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 查看批次对话框 -->
    <el-dialog v-model="batchInfoVisible" title="所属批次" width="500px">
      <el-empty v-if="!batchInfo" description="未加入任何批次" />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="批次号">{{ batchInfo.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="批次状态">
          <el-tag :type="getBatchStatusType(batchInfo.batchStatus)">
            {{ getBatchStatusName(batchInfo.batchStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="批次创建时间">
          {{ formatDateTime(batchInfo.batchCreateTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 加入批次对话框 -->
    <el-dialog v-model="batchDialogVisible" title="选择申报批次" width="600px" @close="handleBatchDialogClose">
      <div v-if="currentSettle" class="batch-dialog-info">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="结算单号">{{ currentSettle.id }}</el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{ currentSettle.patientName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ formatIdCard(currentSettle) }}</el-descriptions-item>
          <el-descriptions-item label="报销金额">¥{{ formatMoney(currentSettle.reimburse) }}</el-descriptions-item>
          <el-descriptions-item label="自付金额">¥{{ formatMoney(currentSettle.selfPay) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div style="margin-top: 16px;">
        <div v-if="loadingBatches" style="text-align: center; padding: 20px;">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <p>加载批次中...</p>
        </div>
        <el-empty v-else-if="pendingBatches.length === 0" description="暂无可用的待申报批次，请先创建批次" />
        <el-table
          v-else
          ref="batchTableRef"
          :data="pendingBatches"
          border
          highlight-current-row
          @current-change="handleBatchSelect"
          style="width: 100%"
        >
          <el-table-column width="55">
            <template #default="{ row }">
              <el-radio v-model="selectedBatchId" :value="row.id">&nbsp;</el-radio>
            </template>
          </el-table-column>
          <el-table-column prop="batchNo" label="批次号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="settleCnt" label="结算单数" width="90" align="center" />
          <el-table-column label="总金额" width="120" align="right">
            <template #default="{ row }">
              ¥{{ formatMoney(row.totalAmt) }}
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button type="info" @click="batchDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="addingToBatch"
          :disabled="!selectedBatchId"
          @click="confirmAddToBatch"
        >
          确认加入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { http, SETTLE_API, BATCH_API, BATCH_ITEM_API, USER_API } from '@/utils/request'
import { formatMoney, formatDateTime, formatIdCard, getSettleStatusName, getSettleStatusType, getBatchStatusName, getBatchStatusType } from '@/utils/format'
import { buildPageQuery } from '@/utils/listQuery'
import { readPage } from '@/utils/page'
import type { PageResult } from '@/types'
import type { SettleVO, BatchVO, BatchItemVO } from '@/types'

const loading = ref(false)
const settleList = ref<SettleVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const selectedSettle = ref<SettleVO | null>(null)
const batchInfoVisible = ref(false)
const batchInfo = ref<BatchItemVO | null>(null)

// 患者搜索相关
const searchingPatient = ref(false)
const patientDialogVisible = ref(false)
const patientList = ref<Array<{ id: string; name: string; idCard: string }>>([])
const selectedPatient = ref<{ id: string; name: string; idCard: string } | null>(null)

// 加入批次相关
const batchDialogVisible = ref(false)
const currentSettle = ref<SettleVO | null>(null)
const pendingBatches = ref<BatchVO[]>([])
const loadingBatches = ref(false)
const selectedBatchId = ref<string | null>(null)
const addingToBatch = ref(false)

const searchForm = reactive({
  patientName: '',
  timeRange: null as [string, string] | null,
})

const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

const handleSearchPatient = async () => {
  const name = searchForm.patientName.trim()
  if (!name) {
    ElMessage.warning('请输入患者姓名')
    return
  }
  searchingPatient.value = true
  try {
    const response = await http.get(USER_API.SEARCH, { params: { name } })
    patientList.value = (response.data as any[]) || []
    if (patientList.value.length === 0) {
      ElMessage.warning('未找到匹配患者')
    } else if (patientList.value.length === 1) {
      selectedPatient.value = patientList.value[0]
      ElMessage.success(`已选择：${selectedPatient.value.name}`)
    } else {
      patientDialogVisible.value = true
    }
  } catch (error) {
    console.error('搜索患者失败:', error)
  } finally {
    searchingPatient.value = false
  }
}

const handleSelectPatient = (row: { id: string; name: string; idCard: string }) => {
  selectedPatient.value = row
  patientDialogVisible.value = false
  ElMessage.success(`已选择：${row.name}`)
}

const handleClearPatient = () => {
  selectedPatient.value = null
  searchForm.patientName = ''
}

const fetchSettleList = async () => {
  loading.value = true
  try {
    const extra: Record<string, string | number> = {}
    if (selectedPatient.value) {
      extra.userId = selectedPatient.value.id
    }
    const response = await http.get<PageResult<SettleVO>>(SETTLE_API.HOSPITAL_LIST, {
      params: buildPageQuery(pageParams.pageNum, pageParams.pageSize, searchForm.timeRange,
        Object.keys(extra).length > 0 ? extra : undefined),
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
  pageParams.pageNum = 1
  fetchSettleList()
}

const handleReset = () => {
  searchForm.patientName = ''
  searchForm.timeRange = null
  selectedPatient.value = null
  pageParams.pageNum = 1
  fetchSettleList()
}

const handleSizeChange = (size: number) => {
  pageParams.pageSize = size
  pageParams.pageNum = 1
  fetchSettleList()
}

const handleCurrentChange = (page: number) => {
  pageParams.pageNum = page
  fetchSettleList()
}

const openDetail = (row: SettleVO) => {
  selectedSettle.value = row
  detailVisible.value = true
}

const viewBatchInfo = async (row: SettleVO) => {
  batchInfo.value = null
  batchInfoVisible.value = true
  try {
    const response = await http.get<BatchItemVO>(BATCH_ITEM_API.BY_SETTLE(row.id))
    batchInfo.value = response.data || null
  } catch (error) {
    console.error('查询批次信息失败:', error)
  }
}

// 加入批次相关方法
const fetchPendingBatches = async () => {
  loadingBatches.value = true
  try {
    const response = await http.get<BatchVO[]>(BATCH_API.PENDING_LIST)
    const data = response.data
    pendingBatches.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取待申报批次列表失败:', error)
    pendingBatches.value = []
  } finally {
    loadingBatches.value = false
  }
}

const openBatchDialog = (row: SettleVO) => {
  currentSettle.value = row
  selectedBatchId.value = null
  batchDialogVisible.value = true
  fetchPendingBatches()
}

const handleBatchSelect = (row: BatchVO | null) => {
  selectedBatchId.value = row ? row.id : null
}

const confirmAddToBatch = async () => {
  if (!selectedBatchId.value || !currentSettle.value) {
    ElMessage.warning('请选择要加入的批次')
    return
  }

  addingToBatch.value = true
  try {
    await http.post(BATCH_API.ADD_SETTLE(selectedBatchId.value, currentSettle.value.id))
    ElMessage.success('已成功加入批次')
    batchDialogVisible.value = false
    fetchSettleList()
  } catch (error: any) {
    console.error('加入批次失败:', error)
  } finally {
    addingToBatch.value = false
  }
}

const handleBatchDialogClose = () => {
  currentSettle.value = null
  selectedBatchId.value = null
  pendingBatches.value = []
}

onMounted(() => {
  fetchSettleList()
})
</script>

<style lang="scss" scoped>
.settle-list-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
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

    .patient-selector {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .batch-dialog-info {
    margin-bottom: 12px;
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
