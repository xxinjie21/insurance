<template>
  <div class="visit-fees-page">
    <div class="page-header">
      <div>
        <h2>费用管理</h2>
        <p>就诊 ID：{{ visitId }}</p>
      </div>
      <el-space>
        <el-button type="info" icon="Refresh" @click="fetchFees">刷新</el-button>
        <el-button type="success" icon="Coin" @click="router.push(`/hospital/visit/${visitId}/settle`)">
          去结算
        </el-button>
      </el-space>
    </div>

    <el-card class="entry-card">
      <template #header>
        <div class="card-header">
          <span>新增费用明细</span>
          <el-button type="primary" link icon="Plus" @click="addRow">添加一行</el-button>
        </div>
      </template>

      <el-table :data="feeRows" style="width: 100%">
        <el-table-column label="项目名称" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.name" placeholder="如：血常规检查" />
          </template>
        </el-table-column>
        <el-table-column label="费用类型" width="130">
          <template #default="{ row }">
            <el-select v-model="row.type">
              <el-option label="甲类" :value="1" />
              <el-option label="乙类" :value="2" />
              <el-option label="自费" :value="3" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0.01" :precision="2" :step="10" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.num" :min="1" :step="1" :precision="0" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            ¥{{ formatMoney(row.price * row.num) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeRow($index)" :disabled="feeRows.length === 1">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="form-footer">
        <span>待提交合计：¥{{ formatMoney(pendingTotal) }}</span>
        <el-button type="primary" :loading="saving" @click="submitFees">批量保存</el-button>
      </div>
    </el-card>

    <el-divider />

    <div class="section-header">
      <h3>已录入费用</h3>
      <strong>合计：¥{{ formatMoney(savedTotal) }}</strong>
    </div>
    <el-table :data="feeList" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="费用 ID" width="90" />
      <el-table-column prop="name" label="项目名称" min-width="180" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ getFeeTypeName(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="单价" width="120">
        <template #default="{ row }">¥{{ formatMoney(row.price) }}</template>
      </el-table-column>
      <el-table-column prop="num" label="数量" width="100" />
      <el-table-column prop="total" label="小计" width="120">
        <template #default="{ row }">¥{{ formatMoney(row.total) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleDeleteFee(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, FEE_API } from '@/utils/request'
import { formatMoney, getFeeTypeName } from '@/utils/format'
import type { FeeVO } from '@/types'

interface FeeFormRow {
  key: number
  visitId: string
  name: string
  type: number
  price: number
  num: number
}

const route = useRoute()
const router = useRouter()
const visitId = route.params.visitId as string
const loading = ref(false)
const saving = ref(false)
const feeList = ref<FeeVO[]>([])

const createRow = (): FeeFormRow => ({
  key: Date.now() + Math.random(),
  visitId,
  name: '',
  type: 1,
  price: 1,
  num: 1,
})

const feeRows = ref<FeeFormRow[]>([createRow()])

const pendingTotal = computed(() =>
  feeRows.value.reduce((sum, row) => sum + Number(row.price || 0) * Number(row.num || 0), 0)
)

const savedTotal = computed(() =>
  feeList.value.reduce((sum, row) => sum + Number(row.total || 0), 0)
)

const addRow = () => {
  feeRows.value.push(createRow())
}

const removeRow = (index: number) => {
  feeRows.value.splice(index, 1)
}

const fetchFees = async () => {
  if (!visitId) return

  loading.value = true
  try {
    const response = await http.get<FeeVO[]>(FEE_API.LIST_BY_VISIT, {
      params: { visitId },
    })
    feeList.value = response.data || []
  } catch (error) {
    console.error('获取费用明细失败:', error)
  } finally {
    loading.value = false
  }
}

const handleDeleteFee = async (row: FeeVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除费用项目"${row.name}"（¥${formatMoney(row.total)}）吗？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await http.delete(FEE_API.DELETE(row.id))
    ElMessage.success('删除成功')
    await fetchFees()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除费用失败:', error)
    }
  }
}

const submitFees = async () => {
  const dtoList = feeRows.value.map((row) => ({
    visitId: row.visitId,
    name: row.name,
    type: row.type,
    price: row.price,
    num: row.num,
  }))
  const invalid = dtoList.some(
    (row) => !row.name.trim() || row.price <= 0 || row.num <= 0 || !row.visitId
  )

  if (invalid) {
    ElMessage.warning('请完整填写费用项目、单价和数量')
    return
  }

  saving.value = true
  try {
    await http.post<FeeVO[]>(FEE_API.BATCH_ADD, dtoList)
    feeRows.value = [createRow()]
    ElMessage.success('费用明细已保存')
    await fetchFees()
  } catch (error) {
    console.error('保存费用明细失败:', error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchFees()
})
</script>

<style lang="scss" scoped>
.visit-fees-page {
  .page-header,
  .section-header,
  .card-header,
  .form-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

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

  .entry-card {
    margin-bottom: 8px;
  }

  .form-footer {
    margin-top: 18px;
    font-size: 15px;
  }

  .section-header {
    margin-bottom: 16px;

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }
}
</style>
