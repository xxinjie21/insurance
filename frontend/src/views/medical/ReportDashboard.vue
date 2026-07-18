<template>
  <div class="page">
    <div class="page-header"><h2>统计报表</h2></div>
    <el-form :inline="true"><el-form-item label="年份"><el-input-number v-model="year" :min="2020" :max="2030" /></el-form-item><el-form-item label="月份"><el-input-number v-model="month" :min="1" :max="12" /></el-form-item><el-form-item><el-button type="primary" @click="loadAll">查询</el-button></el-form-item></el-form>
    <el-row :gutter="16">
      <el-col :span="12"><el-card><template #header>基金收支</template><el-descriptions :column="1" border v-if="fund"><el-descriptions-item label="基金支出">¥{{ fund.totalFundPay }}</el-descriptions-item><el-descriptions-item label="统筹支出">¥{{ fund.totalPooling }}</el-descriptions-item><el-descriptions-item label="大病支出">¥{{ fund.totalCatastrophic }}</el-descriptions-item><el-descriptions-item label="救助支出">¥{{ fund.totalAssistance }}</el-descriptions-item><el-descriptions-item label="结算单数">{{ fund.settleCount }}</el-descriptions-item></el-descriptions></el-card></el-col>
      <el-col :span="12"><el-card><template #header>费用构成</template><el-descriptions :column="1" border v-if="fee"><el-descriptions-item label="药品费">¥{{ fee.drugFee }}</el-descriptions-item><el-descriptions-item label="检查费">¥{{ fee.checkFee }}</el-descriptions-item><el-descriptions-item label="治疗费">¥{{ fee.treatmentFee }}</el-descriptions-item><el-descriptions-item label="耗材费">¥{{ fee.consumableFee }}</el-descriptions-item></el-descriptions></el-card></el-col>
    </el-row>
    <el-card v-if="visit" style="margin-top:16px"><template #header>就诊统计</template><el-descriptions :column="3" border><el-descriptions-item label="门诊人次">{{ visit.outpatientCount }}</el-descriptions-item><el-descriptions-item label="住院人次">{{ visit.inpatientCount }}</el-descriptions-item><el-descriptions-item label="总费用">¥{{ visit.totalFee }}</el-descriptions-item><el-descriptions-item label="总报销">¥{{ visit.totalReimburse }}</el-descriptions-item><el-descriptions-item label="人均费用">¥{{ visit.avgFeePerVisit }}</el-descriptions-item><el-descriptions-item label="报销比例">{{ visit.reimburseRatio }}</el-descriptions-item></el-descriptions></el-card>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { http, REPORT_API } from '@/utils/request'
const year=ref(2026),month=ref<number|undefined>(undefined),fund=ref<any>(null),fee=ref<any>(null),visit=ref<any>(null)
const loadAll=async()=>{const p={params:{year:year.value,month:month.value}};try{[fund.value,fee.value,visit.value]=await Promise.all([http.get(REPORT_API.FUND,p),http.get(REPORT_API.FEE_COMPOSITION,p),http.get(REPORT_API.VISIT_STATS,p)]);[fund.value,fee.value,visit.value]=[fund.value.data,fee.value.data,visit.value.data]}catch(e){console.error(e)}}
loadAll()
</script>
<style scoped>.page{padding:20px}.page-header{margin-bottom:16px}h2{font-size:20px;font-weight:600}.el-card{margin-bottom:16px}</style>
