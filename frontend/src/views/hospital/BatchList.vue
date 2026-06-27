<template>
  <div class="batch-list-page">
    <div class="page-header">
      <div>
        <h2>医保申报</h2>
        <p>查询批次、维护明细并提交申报；可按创建时间筛选</p>
      </div>
      <el-button type="primary" icon="Plus" :loading="creating" @click="createBatch">
        创建批次
      </el-button>
    </div>

    <!-- 创建结果 -->
    <el-card v-if="createdBatch" class="result-card">
      <template #header>创建结果</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="批次号">{{ createdBatch.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="结算单数量">{{ createdBatch.settleCnt }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ formatMoney(createdBatch.totalAmt) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getBatchStatusName(createdBatch.status) }}</el-descriptions-item>
      </el-descriptions>
      <div class="actions">
        <el-button type="primary" @click="router.push(`/hospital/batch/${createdBatch.id}`)">
          进入批次详情
        </el-button>
        <el-button type="info" @click="createdBatch = null">关闭</el-button>
      </div>
    </el-card>

    <el-card class="search-card">
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
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button type="info" icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table
        v-loading="loading"
        :data="batchList"
        border
        stripe
        style="width: 100%"
        empty-text="暂无批次数据"
      >
        <el-table-column prop="batchNo" label="批次号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="settleCnt" label="结算单数" min-width="100" align="center" />
        <el-table-column label="总金额" min-width="110" align="right">
          <template #default="{ row }">
            <span class="amount-total">¥{{ formatMoney(row.totalAmt) }}</span>
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
        <el-table-column label="操作" min-width="280" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="router.push(`/hospital/batch/${row.id}`)">
              详情/申报
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="warning"
              link
              size="small"
              @click="handleDeclare(row)"
            >
              快速申报
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              size="small"
              @click="handleWithdraw(row)"
            >
              撤回申报
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, BATCH_API } from '@/utils/request'
import { formatDateTime, formatMoney, getBatchStatusName, getBatchStatusType } from '@/utils/format'
import { buildPageQuery } from '@/utils/listQuery'
import { readPage } from '@/utils/page'
import type { BatchVO, PageResult } from '@/types'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const createdBatch = ref<BatchVO | null>(null)
const batchList = ref<BatchVO[]>([])
const total = ref(0)

const searchForm = reactive({
  timeRange: null as [string, string] | null,
})

const pageParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

const fetchBatchList = async () => {
  loading.value = true
  try {
    const response = await http.get<PageResult<BatchVO>>(BATCH_API.HOSPITAL_LIST, {
      params: buildPageQuery(pageParams.pageNum, pageParams.pageSize, searchForm.timeRange),
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
  fetchBatchList()
}

const handleReset = () => {
  searchForm.timeRange = null
  pageParams.pageNum = 1
  fetchBatchList()
}

// 分页变化
const handleSizeChange = (size: number) => {
  pageParams.pageSize = size
  pageParams.pageNum = 1
  fetchBatchList()
}

const handleCurrentChange = (page: number) => {
  pageParams.pageNum = page
  fetchBatchList()
}

const createBatch = async () => {
  creating.value = true
  try {
    const response = await http.post<BatchVO>(BATCH_API.CREATE)
    createdBatch.value = response.data
    ElMessage.success('批次创建成功')
    fetchBatchList()
  } catch (error) {
    console.error('创建批次失败:', error)
  } finally {
    creating.value = false
  }
}

const handleDeclare = async (row: BatchVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要申报批次 ${row.batchNo} 吗？医保局拨付前可撤回。`,
      '申报确认',
      { confirmButtonText: '确定申报', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  try {
    await http.post(BATCH_API.DECLARE(row.id))
    ElMessage.success('申报成功')
    fetchBatchList()
  } catch (error: any) {
    console.error('申报失败:', error)
  }
}

const handleWithdraw = async (row: BatchVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤回批次 ${row.batchNo} 的申报吗？撤回后可继续编辑并重新申报。`,
      '撤回确认',
      { confirmButtonText: '确定撤回', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await http.post(BATCH_API.WITHDRAW(row.id))
    ElMessage.success('已撤回申报')
    fetchBatchList()
  } catch (error: any) {
    console.error('撤回失败:', error)
  }
}

const handleDelete = async (row: BatchVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除批次 ${row.batchNo} 吗？批次中的结算单将恢复为未申报状态。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await http.delete(BATCH_API.DELETE(row.id))
    ElMessage.success('已删除批次')
    fetchBatchList()
  } catch (error: any) {
    console.error('删除失败:', error)
  }
}

onMounted(() => {
  fetchBatchList()
})
</script>

<style lang="scss" scoped>
.batch-list-page {
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
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .amount-total {
    font-weight: 600;
  }

  .result-card {
    margin-bottom: 20px;

    .actions {
      margin-top: 18px;
      display: flex;
      gap: 12px;
    }
  }
}
</style>
