<template>
  <div class="add-visit-page">
    <div class="page-header">
      <h2>新增就诊</h2>
    </div>

    <el-card class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入患者身份证号" clearable maxlength="18" />
        </el-form-item>

        <el-form-item label="医院" prop="hospitalId">
          <el-tag v-if="currentHospitalName" type="success">{{ currentHospitalName }}</el-tag>
          <el-tag v-else type="danger">未选择医院</el-tag>
          <div class="form-tip">医院信息从登录/选择信息自动获取</div>
        </el-form-item>

        <el-form-item label="就诊类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button :label="1">门诊</el-radio-button>
            <el-radio-button :label="2">住院</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="诊断结果" prop="diagnosis">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请输入诊断结果"
          />
        </el-form-item>

        <el-form-item>
          <el-space>
            <el-button type="primary" :loading="loading" @click="submit">保存并录入费用</el-button>
            <el-button type="info" @click="router.back()">取消</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { http, VISIT_API } from '@/utils/request'
import type { VisitVO } from '@/types'

interface VisitAddForm {
  idCard: string
  hospitalId: string | null
  type: number
  diagnosis: string
}

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.getUserInfo())
const formRef = ref<FormInstance>()
const loading = ref(false)

// 管理员使用已选择的医院，医院角色使用自身hospitalName
const currentHospitalName = computed(() => {
  if (userInfo.value?.role === 4) {
    return userStore.selectedHospitalName || ''
  }
  // 医院角色直接取登录时返回的医院名称
  return userInfo.value?.hospitalName || ''
})

const form = reactive<VisitAddForm>({
  idCard: '',
  hospitalId: userInfo.value?.hospitalId || null,
  type: 1,
  diagnosis: '',
})

const validateIdCard = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || !value.trim()) {
    callback(new Error('请输入身份证号'))
    return
  }
  // 简单校验：15位或18位
  const idCardPattern = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/
  if (!idCardPattern.test(value.trim())) {
    callback(new Error('请输入有效的18位身份证号'))
    return
  }
  callback()
}

const rules = reactive<FormRules>({
  idCard: [{ validator: validateIdCard, trigger: 'blur' }],
  type: [{ required: true, message: '请选择就诊类型', trigger: 'change' }],
  diagnosis: [{ required: true, message: '请输入诊断结果', trigger: 'blur' }],
})

const submit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      // 只传idCard、type、diagnosis，hospitalId从token获取
      const response = await http.post<VisitVO>(VISIT_API.ADD, {
        idCard: form.idCard.trim(),
        type: form.type,
        diagnosis: form.diagnosis,
      })
      ElMessage.success('就诊记录已创建')
      router.push(`/hospital/visit/${response.data.id}/fees`)
    } catch (error) {
      console.error('新增就诊失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.add-visit-page {
  .page-header {
    margin-bottom: 24px;

    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }

  .form-card {
    max-width: 760px;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
