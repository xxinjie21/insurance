<template>
  <div class="batch-detail-page">
    <div class="page-header">
      <div>
        <h2>批次详情</h2>
        <p v-if="batch">批次号：{{ batch.batchNo }}</p>
      </div>
      <el-space>
        <el-button type="info" v-if="readonly" @click="router.back()">返回</el-button>
        <el-button type="info" icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button
          v-if="!readonly && batch && batch.status === 0"
          type="warning"
          @click="handleDeclare"
        >
          申报批次
        </el-button>
        <el-button
          v-if="!readonly && batch && batch.status === 1"
          type="danger"
          @click="handleWithdraw"
        >
          撤回申报
        </el-button>
      </el-space>
    </div>

    <el-alert
      v-if="batch?.status === 3 && batch.rejectReason"
      type="error"
      :closable="false"
      show-icon
      class="reject-alert"
      title="医保局已拒绝拨付"
      :description="batch.rejectReason"
    />

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      class="load-error-alert"
      title="加载失败"
    >
      <p>{{ loadError }}</p>
      <el-button type="primary" link @click="router.back()">返回上一页</el-button>
    </el-alert>

    <el-card v-loading="loading">
      <template #header>批次信息</template>
      <el-descriptions v-if="batch" :column="2" border>
        <el-descriptions-item label="批次号">{{ batch.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="医院名称">{{ batch.hospitalName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getBatchStatusType(batch.status)">
            {{ getBatchStatusName(batch.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="结算单数量">{{ batch.settleCnt }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ formatMoney(batch.totalAmt) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(batch.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无批次信息" />
    </el-card>

    <!-- 添加已结算就诊单（仅待申报状态显示） -->
    <el-card v-if="!readonly && batch && batch.status === 0" class="add-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>添加已结算就诊单</span>
          <el-button type="primary" size="small" :loading="adding" :disabled="selectedSettleIds.length === 0" @click="addSettles">
            添加选中 ({{ selectedSettleIds.length }})
          </el-button>
        </div>
      </template>

      <el-table
        ref="availableTableRef"
        v-loading="loadingAvailable"
        :data="availableSettles"
        border
        stripe
        max-height="360"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="结算单 ID" width="100" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" />
        <el-table-column label="身份证号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatIdCard(row) }}
          </template>
        </el-table-column>
        <el-table-column label="就诊类型" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.visitType === 1 ? 'success' : 'warning'" size="small">
              {{ getVisitTypeName(row.visitType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diagnosis" label="诊断" min-width="160" show-overflow-tooltip />
        <el-table-column label="总费用" width="100" align="right">
          <template #default="{ row }">
            ¥{{ formatMoney(row.total) }}
          </template>
        </el-table-column>
        <el-table-column label="报销金额" min-width="100" align="right">
          <template #default="{ row }">
            <span style="color: #67c23a">¥{{ formatMoney(row.reimburse) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="自付金额" min-width="100" align="right">
          <template #default="{ row }">
            <span style="color: #f56c6c">¥{{ formatMoney(row.selfPay) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结算时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loadingAvailable && availableSettles.length === 0" description="暂无可添加的已结算就诊单" />
    </el-card>

    <!-- 本批次结算单明细 -->
    <el-card class="detail-card">
      <template #header>
        <div class="detail-card-header">
          <span>本批次结算单（共 {{ itemTotal }} 条）</span>
        </div>
      </template>

      <el-form :inline="true" :model="itemSearchForm" class="item-search-form">
        <el-form-item label="加入时间">
          <el-date-picker
            v-model="itemSearchForm.timeRange"
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
          <el-button type="primary" icon="Search" @click="handleItemSearch">查询</el-button>
          <el-button type="info" icon="Refresh" @click="handleItemReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loadingItems"
        :data="itemList"
        style="width: 100%"
        border
        stripe
        empty-text="该批次尚未添加结算单"
      >
        <el-table-column prop="settleId" label="结算单号" min-width="100" align="center" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" show-overflow-tooltip />
        <el-table-column label="身份证号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatIdCard(row) }}
          </template>
        </el-table-column>
        <el-table-column label="总费用" min-width="100" align="right">
          <template #default="{ row }">
            ¥{{ formatMoney(row.settleTotal) }}
          </template>
        </el-table-column>
        <el-table-column label="报销金额" min-width="100" align="right">
          <template #default="{ row }">
            <span style="color: #67c23a">¥{{ formatMoney(row.settleReimburse) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="自付金额" min-width="100" align="right">
          <template #default="{ row }">
            <span style="color: #f56c6c">¥{{ formatMoney(row.settleSelfPay) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.audit === 0 ? 'success' : 'warning'" size="small">
              {{ row.audit === 0 ? '通过' : '扣款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结算状态" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getSettleStatusType(row.settleStatus)" size="small">
              {{ getSettleStatusName(row.settleStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="加入批次时间" min-width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="itemPageNum"
          v-model:page-size="itemPageSize"
          :total="itemTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleItemSizeChange"
          @current-change="handleItemPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, BATCH_API, BATCH_ITEM_API, SETTLE_API } from '@/utils/request'
import { buildPageQuery } from '@/utils/listQuery'
import { readPage } from '@/utils/page'
import {
  formatDateTime,
  formatIdCard,
  formatMoney,
  getBatchStatusName,
  getBatchStatusType,
  getSettleStatusName,
  getSettleStatusType,
  getVisitTypeName,
} from '@/utils/format'
import type { BatchItemVO, BatchVO, PageResult, SettleVO } from '@/types'

const route = useRoute()
const router = useRouter()
const readonly = computed(() => route.meta.readonly === true)
const batchId = route.params.batchId as string
const loading = ref(false)
const adding = ref(false)
const loadingAvailable = ref(false)
const batch = ref<BatchVO | null>(null)
const loadError = ref('')
const availableSettles = ref<SettleVO[]>([])
const selectedSettleIds = ref<string[]>([])

const loadingItems = ref(false)
const itemList = ref<BatchItemVO[]>([])
const itemPageNum = ref(1)
const itemPageSize = ref(10)
const itemTotal = ref(0)
const itemSearchForm = reactive({
  timeRange: null as [string, string] | null,
})

const fetchDetail = async () => {
  if (!batchId) {
    loadError.value = '批次号无效，请从列表重新进入'
    batch.value = null
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    const response = await http.get<BatchVO>(BATCH_API.DETAIL(batchId))
    batch.value = response.data
    if (!batch.value) {
      loadError.value = '未获取到批次数据'
    }
  } catch (error: unknown) {
    batch.value = null
    const err = error as { message?: string }
    loadError.value = err?.message || '获取批次详情失败，请检查权限或稍后重试'
    console.error('获取批次详情失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchBatchItems = async () => {
  if (!batchId || loadError.value) {
    return
  }

  loadingItems.value = true
  try {
    const response = await http.get<PageResult<BatchItemVO>>(BATCH_ITEM_API.BATCH_PAGE(batchId), {
      params: buildPageQuery(itemPageNum.value, itemPageSize.value, itemSearchForm.timeRange),
    })
    const page = readPage(response)
    itemList.value = page.records
    itemTotal.value = page.total
  } catch (error) {
    console.error('获取批次结算单列表失败:', error)
    itemList.value = []
    itemTotal.value = 0
  } finally {
    loadingItems.value = false
  }
}

const handleItemSearch = () => {
  itemPageNum.value = 1
  fetchBatchItems()
}

const handleItemReset = () => {
  itemSearchForm.timeRange = null
  itemPageNum.value = 1
  fetchBatchItems()
}

const handleItemSizeChange = () => {
  itemPageNum.value = 1
  fetchBatchItems()
}

const handleItemPageChange = () => {
  fetchBatchItems()
}

const refreshAll = async () => {
  await fetchDetail()
  if (!loadError.value) {
    await fetchBatchItems()
  }
}

const fetchAvailableSettles = async () => {
  loadingAvailable.value = true
  try {
    const response = await http.get<SettleVO[]>(SETTLE_API.AVAILABLE_FOR_BATCH)
    const data = response.data
    availableSettles.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取可添加结算单失败:', error)
  } finally {
    loadingAvailable.value = false
  }
}

const handleSelectionChange = (selection: SettleVO[]) => {
  selectedSettleIds.value = selection.map(item => item.id)
}

const addSettles = async () => {
  if (selectedSettleIds.value.length === 0) {
    ElMessage.warning('请选择要添加的结算单')
    return
  }

  adding.value = true
  let successCount = 0
  let failCount = 0

  for (const settleId of selectedSettleIds.value) {
    try {
      await http.post(BATCH_API.ADD_SETTLE(batchId, settleId))
      successCount++
    } catch (error: any) {
      failCount++
      console.error(`添加结算单 ${settleId} 失败:`, error)
    }
  }

  adding.value = false

  if (successCount > 0) {
    ElMessage.success(`成功添加 ${successCount} 条结算单${failCount > 0 ? `，${failCount} 条失败` : ''}`)
    // 刷新批次详情和可用列表
    await refreshAll()
    fetchAvailableSettles()
    selectedSettleIds.value = []
  } else {
    ElMessage.error('添加失败，请重试')
  }
}



const handleWithdraw = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要撤回该批次的申报吗？撤回后可继续添加结算单并重新申报。',
      '撤回确认',
      { confirmButtonText: '确定撤回', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await http.post(BATCH_API.WITHDRAW(batchId))
    ElMessage.success('已撤回申报')
    await refreshAll()
    if (!readonly.value) {
      fetchAvailableSettles()
    }
  } catch (error: any) {
    console.error('撤回失败:', error)
  }
}

const handleDeclare = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要申报此批次吗？医保局拨付前可撤回。`,
      '申报确认',
      { confirmButtonText: '确定申报', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  try {
    await http.post(BATCH_API.DECLARE(batchId))
    ElMessage.success('申报成功')
    await refreshAll()
  } catch (error: any) {
    console.error('申报失败:', error)
  }
}

onMounted(() => {
  refreshAll()
  if (!readonly.value) {
    fetchAvailableSettles()
  }
})
</script>

<style lang="scss" scoped>
.batch-detail-page {
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

  .reject-alert,
  .load-error-alert {
    margin-bottom: 16px;
  }

  .add-card {
    margin: 20px 0;
  }

  .detail-card {
    margin-top: 20px;

    .detail-card-header {
      font-weight: 600;
    }

    .item-search-form {
      margin-bottom: 16px;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
