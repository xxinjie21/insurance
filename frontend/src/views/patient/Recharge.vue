<template>
  <div class="recharge-page">
    <div class="page-header">
      <h2>账户充值</h2>
    </div>

    <el-card class="recharge-card">
      <el-form
        ref="formRef"
        :model="rechargeForm"
        :rules="rules"
        label-width="100px"
        class="recharge-form"
      >
        <el-form-item label="充值金额" prop="amount">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="0.01"
            :max="999999.99"
            :precision="2"
            :step="100"
            style="width: 100%"
            placeholder="请输入充值金额"
          />
          <div class="form-tip">最小充值金额：¥0.01，最大充值金额：¥999,999.99</div>
        </el-form-item>

        <el-form-item label="充值方式" prop="type">
          <el-select v-model="rechargeForm.type" placeholder="请选择充值方式" style="width: 100%">
            <el-option label="微信支付" :value="1" />
            <el-option label="支付宝" :value="2" />
            <el-option label="银行卡" :value="3" />
            <el-option label="现金" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="rechargeForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-space>
            <el-button type="primary" :loading="loading" @click="handleSubmit">
              立即充值
            </el-button>
            <el-button type="info" @click="$router.back()">取消</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { http, ACCOUNT_API } from '@/utils/request'

interface RechargeDTO {
  amount: number
  type: number
  remark?: string
}

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const rechargeForm = reactive<RechargeDTO>({
  amount: 100,
  type: 1,
  remark: '',
})

const rules = reactive<FormRules>({
  amount: [
    { required: true, message: '请输入充值金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '充值金额必须大于 0', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择充值方式', trigger: 'change' }],
})

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await http.post(ACCOUNT_API.RECHARGE, rechargeForm)
      ElMessage.success('充值成功')
      router.push('/patient/account')
    } catch (error) {
      console.error('充值失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.recharge-page {
  .page-header {
    margin-bottom: 24px;

    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }

  .recharge-card {
    max-width: 600px;
  }

  .recharge-form {
    .form-tip {
      margin-top: 8px;
      font-size: 12px;
      color: #999;
    }
  }
}
</style>
