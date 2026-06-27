<template>
  <div class="recharge-list-page">
    <div class="page-header">
      <h2>充值记录</h2>
      <p>可按时间筛选；不选日期时展示全部记录，按时间倒序</p>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="充值时间">
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

    <el-table
      v-loading="loading"
      :data="rechargeList"
      border
      stripe
      style="width: 100%"
      empty-text="暂无充值记录"
    >
      <el-table-column prop="id" label="ID" min-width="80" align="center" />
      <el-table-column prop="orderNo" label="充值单号" min-width="160" show-overflow-tooltip />
      <el-table-column label="充值金额" min-width="110" align="right">
        <template #default="{ row }">
          <span class="amount-in">+¥{{ formatMoney(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" min-width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getRechargeTypeTag(row.type)">
            {{ getRechargeTypeName(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '成功' : '待支付' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.remark || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="支付时间" min-width="170" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.payTime) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { http, ACCOUNT_API } from '@/utils/request'
import { formatDateTime, formatMoney } from '@/utils/format'
import { buildPageQuery } from '@/utils/listQuery'
import { readPage } from '@/utils/page'
import type { PageResult, RechargeRecordVO } from '@/types'

const loading = ref(false)
const rechargeList = ref<RechargeRecordVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  timeRange: null as [string, string] | null,
})

const fetchData = async () => {
  loading.value = true
  try {
    const response = await http.get<PageResult<RechargeRecordVO>>(ACCOUNT_API.RECHARGE_LIST, {
      params: buildPageQuery(pageNum.value, pageSize.value, searchForm.timeRange),
    })
    const page = readPage(response)
    rechargeList.value = page.records
    total.value = page.total
  } catch (error) {
    console.error('获取充值记录失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.timeRange = null
  pageNum.value = 1
  fetchData()
}

const handleSizeChange = () => {
  pageNum.value = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
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

const getRechargeTypeTag = (type: number) => {
  const types: Record<number, string> = {
    1: 'success',
    2: 'primary',
    3: 'warning',
    4: 'info',
  }
  return types[type] || ''
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.recharge-list-page {
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

  .search-card {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
  }

  .amount-in {
    color: #52c41a;
    font-weight: 600;
  }
}
</style>
