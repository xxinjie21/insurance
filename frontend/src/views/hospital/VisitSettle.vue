<template>
  <div class="visit-settle-page">
    <div class="page-header">
      <div>
        <h2>医保结算</h2>
        <p>就诊 ID：{{ visitId }}</p>
      </div>
      <el-space>
        <el-button type="primary" icon="Document" @click="fetchSettle">查询结算单</el-button>
        <el-button type="primary" icon="Check" :loading="settling" @click="calculate">
          执行结算
        </el-button>
      </el-space>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="settle ? 14 : 24">
        <el-card>
          <template #header>费用明细</template>
          <el-table :data="feeList" v-loading="feeLoading" style="width: 100%">
            <el-table-column prop="name" label="项目名称" min-width="180" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">{{ getFeeTypeName(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="price" label="单价" width="110">
              <template #default="{ row }">¥{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="num" label="数量" width="90" />
            <el-table-column prop="total" label="小计" width="120">
              <template #default="{ row }">¥{{ formatMoney(row.total) }}</template>
            </el-table-column>
          </el-table>
          <div class="fee-total">费用合计：¥{{ formatMoney(feeTotal) }}</div>
        </el-card>
      </el-col>

      <el-col v-if="settle" :xs="24" :lg="10">
        <el-card class="settle-card">
          <template #header>结算结果</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="结算单 ID">{{ settle.id }}</el-descriptions-item>
            <el-descriptions-item label="总费用">¥{{ formatMoney(settle.total) }}</el-descriptions-item>
            <el-descriptions-item label="医保报销">¥{{ formatMoney(settle.reimburse) }}</el-descriptions-item>
            <el-descriptions-item label="患者自付">¥{{ formatMoney(settle.selfPay) }}</el-descriptions-item>
            <el-descriptions-item label="申报状态">{{ getSettleStatusName(settle.status) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(settle.createTime) }}</el-descriptions-item>
          </el-descriptions>
          <div class="actions">
            <el-button type="success" @click="router.push('/hospital/batches')">
              加入申报批次
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!settle" description="尚未加载结算单，可先查询或执行结算" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, FEE_API, SETTLE_API } from '@/utils/request'
import {
  formatDateTime,
  formatMoney,
  getFeeTypeName,
  getSettleStatusName,
} from '@/utils/format'
import type { FeeVO, SettleVO } from '@/types'

const route = useRoute()
const router = useRouter()
const visitId = route.params.visitId as string
const feeLoading = ref(false)
const settling = ref(false)
const feeList = ref<FeeVO[]>([])
const settle = ref<SettleVO | null>(null)

const feeTotal = computed(() => feeList.value.reduce((sum, row) => sum + Number(row.total || 0), 0))

const fetchFees = async () => {
  feeLoading.value = true
  try {
    const response = await http.get<FeeVO[]>(FEE_API.LIST_BY_VISIT, {
      params: { visitId },
    })
    feeList.value = response.data || []
  } catch (error) {
    console.error('获取费用明细失败:', error)
  } finally {
    feeLoading.value = false
  }
}

const fetchSettle = async () => {
  try {
    const response = await http.get<SettleVO>(SETTLE_API.DETAIL(visitId))
    settle.value = response.data
  } catch (error) {
    console.error('获取结算详情失败:', error)
  }
}

const calculate = async () => {
  try {
    await ElMessageBox.confirm('确认对该就诊记录执行医保结算吗？', '医保结算', {
      confirmButtonText: '确认结算',
      cancelButtonText: '取消',
      type: 'warning',
    })

    settling.value = true
    const response = await http.post<SettleVO>(SETTLE_API.CALCULATE(visitId))
    settle.value = response.data
    ElMessage.success('结算成功')
    fetchFees()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('执行结算失败:', error)
    }
  } finally {
    settling.value = false
  }
}

onMounted(() => {
  fetchFees()
})
</script>

<style lang="scss" scoped>
.visit-settle-page {
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

  .fee-total {
    text-align: right;
    margin-top: 16px;
    font-weight: 600;
  }

  .settle-card {
    .actions {
      margin-top: 18px;
      text-align: right;
    }
  }
}
</style>
