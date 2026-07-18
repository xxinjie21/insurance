<template>
  <div class="page">
    <div class="page-header"><h2>住院管理</h2></div>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="inpatientNo" label="住院号" width="160" />
      <el-table-column prop="bedNo" label="床位" width="100" />
      <el-table-column prop="admissionTime" label="入院时间" width="170" />
      <el-table-column label="押金" width="120"><template #default="{row}">¥{{ row.depositTotal }}</template></el-table-column>
      <el-table-column label="总费用" width="120"><template #default="{row}">¥{{ row.totalFee }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.status===0?'warning':row.status===1?'success':'info'">{{ ['住院中','已出院','结算中'][row.status] }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button v-if="row.status===0" link type="primary" size="small" @click="openDeposit(row)">押金</el-button>
          <el-button v-if="row.status===0" link type="warning" size="small" @click="handleDischarge(row)">出院</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="depositVisible" title="缴纳押金" width="350px">
      <el-form :model="depositForm"><el-form-item label="金额"><el-input-number v-model="depositForm.amount" :min="0.01" /></el-form-item></el-form>
      <template #footer><el-button @click="depositVisible=false">取消</el-button><el-button type="primary" @click="handleDeposit">确认</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, INPATIENT_API } from '@/utils/request'
const loading=ref(false),list=ref<any[]>([]),depositVisible=ref(false),depositForm=ref({inpatientId:0,amount:0})
const fetch=async()=>{loading.value=true;try{const r=await http.get(INPATIENT_API.HOSPITAL_LIST,{params:{pageNum:1,pageSize:50}});list.value=Array.isArray(r.data)?r.data:(r.data as any)?.records||[]}finally{loading.value=false}}
const openDeposit=(row:any)=>{depositForm.value.inpatientId=row.id;depositForm.value.amount=0;depositVisible.value=true}
const handleDeposit=async()=>{try{await http.post(INPATIENT_API.DEPOSIT,depositForm.value);ElMessage.success('押金缴纳成功');depositVisible.value=false;fetch()}catch(e){console.error(e)}}
const handleDischarge=async(row:any)=>{try{await ElMessageBox.confirm('确认出院结算？');await http.post(INPATIENT_API.DISCHARGE(row.id));ElMessage.success('出院结算完成');fetch()}catch(e){console.error(e)}}
onMounted(fetch)
</script>
<style scoped>.page{padding:20px}.page-header{margin-bottom:16px}h2{font-size:20px;font-weight:600}</style>
