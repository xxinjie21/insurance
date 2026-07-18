<template>
  <div class="page">
    <div class="page-header"><h2>门诊挂号管理</h2><el-button type="primary" @click="addVisible=true">新增挂号</el-button></div>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="挂号ID" width="80" />
      <el-table-column prop="dept" label="科室" width="120" />
      <el-table-column prop="doctorName" label="医生" width="100" />
      <el-table-column label="挂号费" width="100"><template #default="{row}">¥{{ row.regFee }}</template></el-table-column>
      <el-table-column label="类型" width="100"><template #default="{row}">{{ row.regType===2?'专家':'普通' }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.status===0?'':row.status===1?'success':'info'">{{ ['已挂号','已就诊','已取消'][row.status] }}</el-tag></template></el-table-column>
      <el-table-column prop="createTime" label="挂号时间" width="170" />
    </el-table>
    <el-dialog v-model="addVisible" title="新增挂号" width="400px">
      <el-form :model="form">
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="科室"><el-input v-model="form.dept" /></el-form-item>
        <el-form-item label="医生"><el-input v-model="form.doctorName" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.regType"><el-option :value="1" label="普通(15元)"/><el-option :value="2" label="专家(30元)"/></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="addVisible=false">取消</el-button><el-button type="primary" @click="handleAdd">确认</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http, REGISTRATION_API } from '@/utils/request'
const loading=ref(false),list=ref<any[]>([]),addVisible=ref(false)
const form=ref({idCard:'',dept:'',doctorName:'',regType:1,hospitalId:0})
const fetch=async()=>{loading.value=true;try{const r=await http.get(REGISTRATION_API.HOSPITAL_LIST,{params:{pageNum:1,pageSize:50}});list.value=Array.isArray(r.data)?r.data:(r.data as any)?.records||[]}finally{loading.value=false}}
const handleAdd=async()=>{try{await http.post(REGISTRATION_API.ADD,form.value);ElMessage.success('挂号成功');addVisible.value=false;fetch()}catch(e){console.error(e)}}
onMounted(fetch)
</script>
<style scoped>.page{padding:20px}.page-header{display:flex;justify-content:space-between;margin-bottom:16px}h2{font-size:20px;font-weight:600}</style>
