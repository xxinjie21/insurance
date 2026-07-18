<template>
  <div class="page">
    <div class="page-header"><h2>医保审核</h2></div>
    <el-form :inline="true"><el-form-item label="结算单ID"><el-input v-model="settleId" placeholder="输入结算单ID" /></el-form-item><el-form-item><el-button type="primary" @click="handleAudit">审核</el-button></el-form-item></el-form>
    <el-card v-if="findings.length" style="margin-top:16px"><template #header>审核结果（{{ findings.length }}条问题）</template>
      <el-table :data="findings" border stripe>
        <el-table-column prop="ruleDescription" label="问题描述" min-width="300" show-overflow-tooltip />
        <el-table-column label="严重程度" width="100"><template #default="{row}"><el-tag :type="row.severity===2?'danger':'warning'">{{ row.severity===2?'扣款':'预警' }}</el-tag></template></el-table-column>
        <el-table-column prop="feeName" label="关联费用" width="140" />
        <el-table-column label="建议调减" width="120"><template #default="{row}">¥{{ row.suggestDeductAmount }}</template></el-table-column>
      </el-table>
    </el-card>
    <el-empty v-else-if="audited" description="该结算单未发现问题" />
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { http, AUDIT_API } from '@/utils/request'
const settleId=ref(''),findings=ref<any[]>([]),audited=ref(false)
const handleAudit=async()=>{try{const r=await http.get(AUDIT_API.SETTLE(settleId.value));findings.value=r.data||[];audited.value=true}catch(e){console.error(e)}}
</script>
<style scoped>.page{padding:20px}.page-header{margin-bottom:16px}h2{font-size:20px;font-weight:600}</style>
