<template>
  <div class="page">
    <div class="page-header"><h2>医保目录查询</h2></div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="药品目录" name="drug">
        <el-input v-model="keyword" placeholder="搜索名称/编码" clearable @input="search" style="width:300px;margin-bottom:12px"/>
        <el-table :data="drugList" border stripe><el-table-column prop="code" label="编码" width="120"/><el-table-column prop="name" label="名称"/><el-table-column prop="specification" label="规格" width="120"/><el-table-column prop="manufacturer" label="厂家" width="120"/><el-table-column label="类别" width="80"><template #default="{row}">{{ ['','甲类','乙类','自费'][row.category] }}</template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="诊疗目录" name="treatment">
        <el-input v-model="keyword" placeholder="搜索名称/编码" clearable @input="search" style="width:300px;margin-bottom:12px"/>
        <el-table :data="treatmentList" border stripe><el-table-column prop="code" label="编码" width="120"/><el-table-column prop="name" label="项目名称"/><el-table-column prop="projectType" label="项目类别" width="100"/><el-table-column label="类别" width="80"><template #default="{row}">{{ ['','甲类','乙类','自费'][row.category] }}</template></el-table-column><el-table-column label="单价上限" width="100"><template #default="{row}">¥{{ row.unitPriceCap }}</template></el-table-column></el-table>
      </el-tab-pane>
      <el-tab-pane label="耗材目录" name="consumable">
        <el-input v-model="keyword" placeholder="搜索名称/编码" clearable @input="search" style="width:300px;margin-bottom:12px"/>
        <el-table :data="consumableList" border stripe><el-table-column prop="code" label="编码" width="120"/><el-table-column prop="name" label="名称"/><el-table-column label="类别" width="80"><template #default="{row}">{{ ['','甲类','乙类','自费'][row.category] }}</template></el-table-column><el-table-column label="限额" width="100"><template #default="{row}">¥{{ row.limitAmount }}</template></el-table-column></el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { http, CATALOG_API } from '@/utils/request'
const activeTab=ref('drug'),keyword=ref(''),drugList=ref<any[]>([]),treatmentList=ref<any[]>([]),consumableList=ref<any[]>([])
const search=async()=>{try{const k=keyword.value;const p={params:{pageNum:1,pageSize:30,keyword:k}};if(activeTab.value==='drug'){const r=await http.get(CATALOG_API.DRUG_LIST,p);drugList.value=Array.isArray(r.data)?r.data:(r.data as any)?.records||[]}else if(activeTab.value==='treatment'){const r=await http.get(CATALOG_API.TREATMENT_LIST,p);treatmentList.value=Array.isArray(r.data)?r.data:(r.data as any)?.records||[]}else{const r=await http.get(CATALOG_API.CONSUMABLE_LIST,p);consumableList.value=Array.isArray(r.data)?r.data:(r.data as any)?.records||[]}}catch(e){console.error(e)}}
watch(activeTab,()=>{keyword.value='';search()})
search()
</script>
<style scoped>.page{padding:20px}.page-header{margin-bottom:16px}h2{font-size:20px;font-weight:600}</style>
