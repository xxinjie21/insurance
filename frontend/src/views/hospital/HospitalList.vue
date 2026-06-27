<template>
  <div class="hospital-list-page">
    <div class="page-header">
      <div>
        <h2>医院列表</h2>
        <p>查看所有已注册的医院信息</p>
      </div>
      <el-space>
        <el-button type="info" icon="Refresh" @click="fetchData">刷新</el-button>
      </el-space>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="hospitalList"
        border
        stripe
        style="width: 100%"
        empty-text="暂无医院数据"
      >
        <el-table-column prop="id" label="医院ID" min-width="90" align="center" />
        <el-table-column prop="name" label="医院名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" min-width="130" />
        <el-table-column label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getHospitalStatusType(row.status)">
              {{ getHospitalStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
            <template v-if="canManageHospital">
              <template v-if="row.status === 0">
                <el-button type="success" link size="small" @click="approveHospital(row)">通过</el-button>
                <el-button type="danger" link size="small" @click="rejectHospital(row)">拒绝</el-button>
              </template>
              <el-button v-if="row.status === 1" type="warning" link size="small" @click="disableHospital(row)">停用</el-button>
              <el-button v-if="row.status === 2" type="success" link size="small" @click="enableHospital(row)">启用</el-button>
            </template>
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

    <el-dialog v-model="detailVisible" title="医院详情" width="560px">
      <el-descriptions v-if="selectedHospital" :column="1" border>
        <el-descriptions-item label="医院ID">{{ selectedHospital.id }}</el-descriptions-item>
        <el-descriptions-item label="医院名称">{{ selectedHospital.name }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ selectedHospital.address }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ selectedHospital.phone }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getHospitalStatusType(selectedHospital.status)">
            {{ getHospitalStatusName(selectedHospital.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ formatDateTime(selectedHospital.createTime) }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="info" @click="detailVisible = false">关闭</el-button>
        <template v-if="canManageHospital && selectedHospital">
          <template v-if="selectedHospital.status === 0">
            <el-button type="success" @click="approveHospital(selectedHospital); detailVisible = false">通过</el-button>
            <el-button type="danger" @click="rejectHospital(selectedHospital); detailVisible = false">拒绝</el-button>
          </template>
          <el-button v-if="selectedHospital.status === 1" type="warning" @click="disableHospital(selectedHospital); detailVisible = false">停用</el-button>
          <el-button v-if="selectedHospital.status === 2" type="success" @click="enableHospital(selectedHospital); detailVisible = false">启用</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, HOSPITAL_API } from '@/utils/request'
import { formatDateTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import type { HospitalVO, PageResult } from '@/types'

const userStore = useUserStore()

// 判断是否为医保局或管理员（可以注册医院和启用/停用）
const { isMedical, isAdmin } = storeToRefs(userStore)
const canManageHospital = computed(() => isMedical.value || isAdmin.value)
const loading = ref(false)
const hospitalList = ref<HospitalVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const selectedHospital = ref<HospitalVO | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const response = await http.get<PageResult<HospitalVO>>(HOSPITAL_API.LIST, {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    })
    hospitalList.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch (error: any) {
    console.error('获取医院列表失败:', error)
    // 错误消息已在拦截器中显示，这里不再重复显示
  } finally {
    loading.value = false
  }
}

const openDetail = (row: HospitalVO) => {
  selectedHospital.value = row
  detailVisible.value = true
}

const getHospitalStatusType = (status?: number) => {
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  if (status === 3) return 'danger'
  return 'info'
}

const getHospitalStatusName = (status?: number) => {
  if (status === 1) return '已启用'
  if (status === 2) return '已停用'
  if (status === 3) return '已拒绝'
  return '待审批'
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

const enableHospital = async (row: HospitalVO) => {
  try {
    await ElMessageBox.confirm(`确定要启用医院 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await http.post(HOSPITAL_API.ENABLE(row.id))
    ElMessage.success('启用成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('启用失败:', error)
    }
  }
}

const disableHospital = async (row: HospitalVO) => {
  try {
    await ElMessageBox.confirm(`确定要停用医院 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await http.post(HOSPITAL_API.DISABLE(row.id))
    ElMessage.success('停用成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('停用失败:', error)
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.hospital-list-page {
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
