<template>
  <div class="hospital-patient-list-page">
    <div class="page-header">
      <div>
        <h2>本院患者列表</h2>
        <p>查看在本院就诊过的患者信息</p>
      </div>
      <el-button type="info" icon="Refresh" @click="fetchData">刷新</el-button>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="patientList"
        border
        stripe
        style="width: 100%"
        empty-text="暂无患者数据"
      >
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="注册时间" min-width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="80" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
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
    </el-card>

    <el-dialog v-model="detailVisible" title="患者详情" width="560px">
      <el-descriptions v-if="selectedPatient" :column="1" border>
        <el-descriptions-item label="姓名">{{ selectedPatient.name }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ selectedPatient.idCard }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ selectedPatient.phone }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ formatDateTime(selectedPatient.createTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http, HOSPITAL_API } from '@/utils/request'
import { formatDateTime } from '@/utils/format'
import type { PageResult } from '@/types'

interface PatientVO {
  name: string
  idCard: string
  phone: string
  createTime?: string
}

const loading = ref(false)
const patientList = ref<PatientVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const selectedPatient = ref<PatientVO | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const response = await http.get<PageResult<PatientVO>>(HOSPITAL_API.PATIENT_LIST, {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    })
    patientList.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch (error) {
    console.error('获取患者列表失败:', error)
  } finally {
    loading.value = false
  }
}

const openDetail = (row: PatientVO) => {
  selectedPatient.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.hospital-patient-list-page {
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

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
