<template>
  <div class="medical-batch-page">
    <div class="page-header">
      <div>
        <h2>基金拨付</h2>
        <p>可按创建时间筛选批次；不选日期时展示全部，按创建时间倒序</p>
      </div>
    </div>

    <el-card class="query-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="创建时间">
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
        <el-form-item label="医院">
          <el-tag
            v-if="selectedHospitalName"
            closable
            type="warning"
            @close="clearHospital"
          >
            {{ selectedHospitalName }}
          </el-tag>
          <el-button type="primary" icon="Search" @click="hospitalDialogVisible = true">
            {{ selectedHospitalName ? '更换医院' : '选择医院' }}
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" :loading="loading" @click="handleSearch">
            查询批次
          </el-button>
          <el-button type="info" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <el-table
        v-loading="loading"
        :data="batchList"
        border
        stripe
        highlight-current-row
        style="width: 100%"
        empty-text="该时间段内暂无批次"
        @current-change="handleSelect"
      >
        <el-table-column width="55" align="center">
          <template #default="{ row }">
            <el-radio v-model="selectedBatchId" :value="row.id">&nbsp;</el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="hospitalName" label="医院" min-width="140" show-overflow-tooltip />
        <el-table-column prop="settleCnt" label="结算单数" min-width="100" align="center" />
        <el-table-column label="总金额" min-width="110" align="right">
          <template #default="{ row }">
            ¥{{ formatMoney(row.totalAmt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getBatchStatusType(row.status)">
              {{ getBatchStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goBatchDetail(row)">
              批次详情
            </el-button>
            <el-button type="info" link size="small" @click="selectAndQueryPay(row)">
              查询拨付
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="success"
              link
              size="small"
              @click="selectAndPay(row)"
            >
              拨付
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              size="small"
              @click="openRejectDialog(row)"
            >
              拒绝拨付
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

    <el-card v-if="payInfo" class="result-card">
      <template #header>
        拨付信息
        <span v-if="selectedBatch" class="batch-no-hint">（批次号：{{ selectedBatch.batchNo }}）</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="批次号">{{ selectedBatch?.batchNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="医院">{{ selectedBatch?.hospitalName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="拨付金额">¥{{ formatMoney(payInfo.amount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getPayStatusType(payInfo.status)">
            {{ getPayStatusName(payInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="拨付/处理时间">
          {{ formatDateTime(payInfo.payTime) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="payInfo.status === 2 && payInfo.rejectReason" label="拒绝理由" :span="2">
          {{ payInfo.rejectReason }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="拒绝拨付" width="520px" @closed="resetRejectForm">
      <p v-if="rejectTarget" class="reject-hint">
        批次号：{{ rejectTarget.batchNo }}（{{ rejectTarget.hospitalName }}）
      </p>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="拒绝理由" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写拒绝拨付的原因，医院端可见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="info" @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <HospitalSelector v-model="hospitalDialogVisible" @select="onHospitalSelected" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, BATCH_API, PAY_API } from '@/utils/request'
import {
  formatDateTime,
  formatMoney,
  getBatchStatusName,
  getBatchStatusType,
  getPayStatusName,
  getPayStatusType,
} from '@/utils/format'
import { buildPageQuery } from '@/utils/listQuery'
import { readPage } from '@/utils/page'
import type { BatchVO, PageResult, PayVO } from '@/types'
import HospitalSelector from './HospitalSelector.vue'

const router = useRouter()
const loading = ref(false)
const paying = ref(false)
const rejecting = ref(false)
const rejectDialogVisible = ref(false)
const rejectTarget = ref<BatchVO | null>(null)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({ reason: '' })
const rejectRules: FormRules = {
  reason: [
    { required: true, message: '请填写拒绝理由', trigger: 'blur' },
    { min: 2, max: 500, message: '长度在 2 到 500 个字符', trigger: 'blur' },
  ],
}
const batchList = ref<BatchVO[]>([])
const total = ref(0)
const selectedBatchId = ref<string | null>(null)
const selectedBatch = ref<BatchVO | null>(null)
const payInfo = ref<PayVO | null>(null)

const searchForm = reactive({
  timeRange: null as [string, string] | null,
})

const hospitalDialogVisible = ref(false)
const selectedHospitalId = ref<string | null>(null)
const selectedHospitalName = ref('')

const onHospitalSelected = (hospital: { id: string; name: string }) => {
  selectedHospitalId.value = hospital.id
  selectedHospitalName.value = hospital.name
}

const clearHospital = () => {
  selectedHospitalId.value = null
  selectedHospitalName.value = ''
}

const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

const fetchBatchList = async () => {
  loading.value = true
  payInfo.value = null
  try {
    const extra: Record<string, string | number> = {}
    if (selectedHospitalId.value) {
      extra.hospitalId = selectedHospitalId.value
    }
    const response = await http.get<PageResult<BatchVO>>(BATCH_API.MEDICAL_LIST, {
      params: buildPageQuery(pageParams.pageNum, pageParams.pageSize, searchForm.timeRange, extra),
    })
    const page = readPage(response)
    batchList.value = page.records
    total.value = page.total
  } catch (error) {
    console.error('获取批次列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageParams.pageNum = 1
  selectedBatchId.value = null
  selectedBatch.value = null
  fetchBatchList()
}

const handleReset = () => {
  searchForm.timeRange = null
  clearHospital()
  pageParams.pageNum = 1
  selectedBatchId.value = null
  selectedBatch.value = null
  payInfo.value = null
  fetchBatchList()
}

onMounted(() => {
  fetchBatchList()
})

const handleSizeChange = (size: number) => {
  pageParams.pageSize = size
  pageParams.pageNum = 1
  fetchBatchList()
}

const handleCurrentChange = (page: number) => {
  pageParams.pageNum = page
  fetchBatchList()
}

const handleSelect = (row: BatchVO | null) => {
  selectedBatch.value = row
  selectedBatchId.value = row ? row.id : null
}

const selectBatch = (row: BatchVO) => {
  selectedBatch.value = row
  selectedBatchId.value = row.id
}

const selectAndQueryPay = async (row: BatchVO) => {
  selectBatch(row)
  try {
    const response = await http.get<PayVO>(PAY_API.BY_BATCH(row.id))
    payInfo.value = response.data
  } catch (error) {
    console.error('查询拨付信息失败:', error)
    payInfo.value = null
  }
}

const goBatchDetail = (row: BatchVO) => {
  router.push(`/medical/batch/${row.id}`)
}

const openRejectDialog = (row: BatchVO) => {
  rejectTarget.value = row
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

const resetRejectForm = () => {
  rejectTarget.value = null
  rejectForm.reason = ''
  rejectFormRef.value?.clearValidate()
}

const submitReject = async () => {
  if (!rejectTarget.value) return
  const valid = await rejectFormRef.value?.validate().catch(() => false)
  if (!valid) return

  rejecting.value = true
  try {
    const response = await http.post<PayVO>(PAY_API.REJECT_BATCH(rejectTarget.value.id), {
      reason: rejectForm.reason.trim(),
    })
    payInfo.value = response.data
    selectedBatch.value = rejectTarget.value
    selectedBatchId.value = rejectTarget.value.id
    ElMessage.success('已拒绝拨付')
    rejectDialogVisible.value = false
    fetchBatchList()
  } catch (error) {
    console.error('拒绝拨付失败:', error)
  } finally {
    rejecting.value = false
  }
}

const selectAndPay = async (row: BatchVO) => {
  if (row.status !== 1) {
    ElMessage.warning('仅已申报状态的批次可拨付')
    return
  }

  selectBatch(row)
  try {
    await ElMessageBox.confirm(
      `确认对批次 ${row.batchNo} 执行基金拨付吗？`,
      '基金拨付',
      { confirmButtonText: '确认拨付', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  paying.value = true
  try {
    const response = await http.post<PayVO>(PAY_API.PAY_BATCH(row.id))
    payInfo.value = response.data
    ElMessage.success('拨付成功')
    fetchBatchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拨付失败:', error)
    }
  } finally {
    paying.value = false
  }
}
</script>

<style lang="scss" scoped>
.medical-batch-page {
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

  .query-card,
  .list-card,
  .result-card {
    margin-bottom: 18px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .batch-no-hint {
    margin-left: 8px;
    font-size: 14px;
    font-weight: normal;
    color: #666;
  }

  .reject-hint {
    margin-bottom: 12px;
    color: #666;
    font-size: 14px;
  }
}
</style>
