<template>
  <div class="visit-list-page">
    <div class="page-header">
      <div>
        <h2>就诊管理</h2>
        <p>查看本院就诊记录，并进入费用录入或医保结算流程</p>
      </div>
      <el-space>
        <el-button type="info" icon="Refresh" @click="fetchData">刷新</el-button>
        <el-button type="primary" icon="Plus" @click="router.push('/hospital/visit/add')">
          新增就诊
        </el-button>
      </el-space>
    </div>

    <div class="search-form">
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
      empty-text="请输入患者姓名搜索并选择患者后查询"
    >
      <el-table-column prop="id" label="就诊 ID" min-width="90" align="center" />
      <el-table-column prop="userName" label="患者姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatIdCard(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="hospitalName" label="医院名称" min-width="140" show-overflow-tooltip />
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
      <el-table-column label="操作" min-width="220" align="center">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="router.push(`/hospital/visit/${row.id}/fees`)">
            费用
          </el-button>
          <el-button type="success" link size="small" @click="router.push(`/hospital/visit/${row.id}/settle`)">
            结算
          </el-button>
          <el-button type="info" link size="small" @click="openDetail(row)">详情</el-button>
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

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

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

    <el-dialog v-model="detailVisible" title="就诊详情" width="560px">
      <el-descriptions v-if="selectedVisit" :column="1" border>
        <el-descriptions-item label="就诊 ID">{{ selectedVisit.id }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ selectedVisit.userName }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ formatIdCard(selectedVisit) }}</el-descriptions-item>
        <el-descriptions-item label="医院名称">{{ selectedVisit.hospitalName }}</el-descriptions-item>
        <el-descriptions-item label="就诊类型">{{ getVisitTypeName(selectedVisit.type) }}</el-descriptions-item>
        <el-descriptions-item label="诊断结果">{{ selectedVisit.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getVisitStatusName(selectedVisit.status) }}</el-descriptions-item>
        <el-descriptions-item label="就诊时间">
          {{ formatDateTime(selectedVisit.createTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, VISIT_API, USER_API } from '@/utils/request'
import { formatDateTime, formatIdCard, getVisitTypeName, getVisitStatusName, getVisitStatusType } from '@/utils/format'
import { readPage } from '@/utils/page'
import type { PageResult, VisitVO } from '@/types'

const router = useRouter()
const loading = ref(false)
const visitList = ref<VisitVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const selectedVisit = ref<VisitVO | null>(null)

const searchingPatient = ref(false)
const patientDialogVisible = ref(false)
const patientList = ref<Array<{ id: string; name: string; idCard: string }>>([])
const selectedPatient = ref<{ id: string; name: string; idCard: string } | null>(null)

const searchForm = reactive({
  patientName: '',
  timeRange: null as [string, string] | null,
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

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (selectedPatient.value) {
      params.userId = selectedPatient.value.id
    }
    if (searchForm.timeRange?.length === 2) {
      params.startTime = searchForm.timeRange[0]
      params.endTime = searchForm.timeRange[1]
    }
    const response = await http.get<PageResult<VisitVO>>(VISIT_API.HOSPITAL_LIST, { params })
    const page = readPage(response)
    visitList.value = page.records
    total.value = page.total
  } catch (error) {
    console.error('获取本院就诊列表失败:', error)
  } finally {
    loading.value = false
  }
}

const openDetail = (row: VisitVO) => {
  selectedVisit.value = row
  detailVisible.value = true
}

const handleDelete = async (row: VisitVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除该就诊记录（ID: ${row.id}）吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await http.delete(VISIT_API.DELETE(row.id))
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.patientName = ''
  searchForm.timeRange = null
  selectedPatient.value = null
  pageNum.value = 1
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.visit-list-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: flex-start;
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

  .search-form {
    margin-bottom: 24px;
    padding: 20px;
    background-color: #f5f7fa;
    border-radius: 8px;

    .patient-selector {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .pagination {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
