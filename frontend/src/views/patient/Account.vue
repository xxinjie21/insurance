<template>
  <div class="account-page">
    <div class="page-header">
      <h2>我的账户</h2>
    </div>

    <el-row :gutter="24">
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="account-card">
            <div class="account-icon">
              <el-icon :size="48" color="#667eea"><Wallet /></el-icon>
            </div>
            <div class="account-info">
              <div class="account-label">账户余额</div>
              <div class="account-value">¥{{ formatMoney(accountInfo.balance) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <div class="account-card">
            <div class="account-icon">
              <el-icon :size="48" color="#52c41a"><Coin /></el-icon>
            </div>
            <div class="account-info">
              <div class="account-label">累计充值</div>
              <div class="account-value">¥{{ formatMoney(accountInfo.totalRecharge) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <div class="account-card">
            <div class="account-icon">
              <el-icon :size="48" color="#faad14"><ShoppingCart /></el-icon>
            </div>
            <div class="account-info">
              <div class="account-label">累计消费</div>
              <div class="account-value">¥{{ formatMoney(accountInfo.totalConsumption) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover">
          <div class="account-card">
            <div class="account-icon">
              <el-icon :size="48" color="#1890ff"><User /></el-icon>
            </div>
            <div class="account-info">
              <div class="account-label">用户姓名</div>
              <div class="account-value">{{ accountInfo.userName || '-' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-divider />

    <div class="quick-actions">
      <el-button type="primary" size="large" icon="Plus" @click="$router.push('/patient/recharge')">
        账户充值
      </el-button>
      <el-button type="success" size="large" icon="Document" @click="toggleRechargeRecords">
        充值记录
      </el-button>
      <el-button type="warning" size="large" icon="ShoppingCart" @click="toggleConsumptionRecords">
        消费记录
      </el-button>
    </div>

    <el-collapse-transition>
      <div v-if="showRechargeRecords" class="records-section">
        <h3>充值记录</h3>
        <el-table :data="rechargeRecords" v-loading="rechargeLoading">
          <el-table-column prop="orderNo" label="订单号" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">
              <span style="color: #52c41a">+¥{{ formatMoney(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              {{ getRechargeTypeName(row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
          <el-table-column prop="createTime" label="时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime || row.payTime) }}
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination" v-if="rechargeTotal > 0">
          <el-pagination
            v-model:current-page="rechargePageNum"
            v-model:page-size="rechargePageSize"
            :total="rechargeTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchRechargeRecords"
            @current-change="fetchRechargeRecords"
          />
        </div>
      </div>
    </el-collapse-transition>

    <el-collapse-transition>
      <div v-if="showConsumptionRecords" class="records-section">
        <h3>消费记录</h3>
        <el-table :data="consumptionRecords" v-loading="consumptionLoading">
          <el-table-column prop="orderNo" label="订单号" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">
              <span style="color: #f5222d">-¥{{ formatMoney(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              {{ row.type === 1 ? '就诊支付' : '其他' }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
          <el-table-column prop="createTime" label="时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination" v-if="consumptionTotal > 0">
          <el-pagination
            v-model:current-page="consumptionPageNum"
            v-model:page-size="consumptionPageSize"
            :total="consumptionTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchConsumptionRecords"
            @current-change="fetchConsumptionRecords"
          />
        </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { http, ACCOUNT_API } from '@/utils/request'
import { formatDateTime, formatMoney } from '@/utils/format'
import { readPage } from '@/utils/page'
import type { ConsumptionRecordVO, RechargeRecordVO, UserAccountVO } from '@/types'

const accountInfo = ref<UserAccountVO>({
  id: '',
  userId: '',
  userName: '',
  balance: 0,
  totalRecharge: 0,
  totalConsumption: 0,
  status: 1,
  createTime: '',
  updateTime: '',
})

const showRechargeRecords = ref(false)
const showConsumptionRecords = ref(false)

const rechargeRecords = ref<RechargeRecordVO[]>([])
const rechargeLoading = ref(false)
const rechargePageNum = ref(1)
const rechargePageSize = ref(10)
const rechargeTotal = ref(0)

const consumptionRecords = ref<ConsumptionRecordVO[]>([])
const consumptionLoading = ref(false)
const consumptionPageNum = ref(1)
const consumptionPageSize = ref(10)
const consumptionTotal = ref(0)

const fetchAccountInfo = async () => {
  try {
    const response = await http.get<UserAccountVO>(ACCOUNT_API.GET)
    accountInfo.value = response.data
  } catch (error) {
    console.error('获取账户信息失败:', error)
  }
}

const fetchRechargeRecords = async () => {
  rechargeLoading.value = true
  try {
    const response = await http.get<RechargeRecordVO[]>(ACCOUNT_API.RECHARGE_LIST, {
      params: {
        pageNum: rechargePageNum.value,
        pageSize: rechargePageSize.value,
      },
    })
    const page = readPage(response)
    rechargeRecords.value = page.records
    rechargeTotal.value = page.total
  } catch (error) {
    console.error('获取充值记录失败:', error)
  } finally {
    rechargeLoading.value = false
  }
}

const fetchConsumptionRecords = async () => {
  consumptionLoading.value = true
  try {
    const response = await http.get<ConsumptionRecordVO[]>(ACCOUNT_API.CONSUMPTION_LIST, {
      params: {
        pageNum: consumptionPageNum.value,
        pageSize: consumptionPageSize.value,
      },
    })
    const page = readPage(response)
    consumptionRecords.value = page.records
    consumptionTotal.value = page.total
  } catch (error) {
    console.error('获取消费记录失败:', error)
  } finally {
    consumptionLoading.value = false
  }
}

const getRechargeTypeName = (type: number) => {
  const types: Record<number, string> = {
    1: '微信',
    2: '支付宝',
    3: '银行卡',
    4: '现金',
  }
  return types[type] || '未知'
}

const toggleRechargeRecords = () => {
  showRechargeRecords.value = !showRechargeRecords.value
  if (showRechargeRecords.value && rechargeRecords.value.length === 0) {
    fetchRechargeRecords()
  }
}

const toggleConsumptionRecords = () => {
  showConsumptionRecords.value = !showConsumptionRecords.value
  if (showConsumptionRecords.value && consumptionRecords.value.length === 0) {
    fetchConsumptionRecords()
  }
}

onMounted(() => {
  fetchAccountInfo()
})
</script>

<style lang="scss" scoped>
.account-page {
  .page-header {
    margin-bottom: 24px;

    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }

  .account-card {
    display: flex;
    align-items: center;
    gap: 16px;

    .account-icon {
      width: 80px;
      height: 80px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .account-info {
      flex: 1;

      .account-label {
        font-size: 14px;
        color: #666;
        margin-bottom: 8px;
      }

      .account-value {
        font-size: 32px;
        font-weight: 600;
        color: #1a1a1a;
      }
    }
  }

  .quick-actions {
    margin: 24px 0;
    display: flex;
    gap: 16px;
  }

  .records-section {
    margin-top: 24px;

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #1a1a1a;
      margin-bottom: 16px;
    }

    .pagination {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
