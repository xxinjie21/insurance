<template>
  <div class="hospital-approve-page">
    <div class="page-header">
      <div>
        <h2>医院审批</h2>
        <p>审批医院的注册申请</p>
      </div>
      <el-button type="info" icon="Refresh" @click="fetchData">刷新</el-button>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="pendingList"
        border
        stripe
        style="width: 100%"
        empty-text="暂无待审批的医院"
      >
        <el-table-column prop="id" label="医院ID" min-width="80" align="center" />
        <el-table-column prop="name" label="医院名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" min-width="130" />
        <el-table-column label="申请时间" min-width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="approveHospital(row)">通过</el-button>
            <el-button type="danger" size="small" @click="rejectHospital(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, HOSPITAL_API } from '@/utils/request'
import { formatDateTime } from '@/utils/format'
import type { HospitalVO, PageResult } from '@/types'

const loading = ref(false)
const pendingList = ref<HospitalVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const response = await http.get<PageResult<HospitalVO>>(HOSPITAL_API.LIST, {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    })
    const records = response.data?.records || []
    pendingList.value = records.filter((h: HospitalVO) => h.status === 0)
    total.value = pendingList.value.length
  } catch (error: any) {
    console.error('获取医院列表失败:', error)
  } finally {
    loading.value = false
  }
}

const approveHospital = async (row: HospitalVO) => {
  try {
    await ElMessageBox.confirm(`确定要通过医院 "${row.name}" 的注册申请吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await http.post(HOSPITAL_API.APPROVE(row.id))
    ElMessage.success('审批通过')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('审批失败:', error)
    }
  }
}

const rejectHospital = async (row: HospitalVO) => {
  try {
    await ElMessageBox.confirm(`确定要拒绝医院 "${row.name}" 的注册申请吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await http.post(HOSPITAL_API.REJECT(row.id))
    ElMessage.success('已拒绝')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('拒绝失败:', error)
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.hospital-approve-page {
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
