<template>
  <div class="register-container">
    <div class="register-box">
      <el-tabs v-model="activeTab" class="register-tabs">
        <el-tab-pane label="用户注册" name="user">
          <div class="register-header">
            <h1>用户注册</h1>
            <p>创建您的账号</p>
          </div>

          <el-form
            ref="formRef"
            :model="registerForm"
            :rules="rules"
            class="register-form"
            size="large"
          >
        <el-form-item prop="name">
          <el-input
            v-model="registerForm.name"
            placeholder="请输入姓名"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="role">
          <el-select
            v-model="registerForm.role"
            placeholder="请选择角色"
            style="width: 100%"
            @change="handleRoleChange"
          >
            <el-option label="患者" :value="1" />
            <el-option label="医院" :value="2" />
            <el-option label="医保局" :value="3" />
            <el-option label="管理员" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item prop="idCard">
          <el-input
            v-model="registerForm.idCard"
            placeholder="请输入身份证号"
            clearable
          />
        </el-form-item>

        <el-form-item
          v-if="registerForm.role === 2"
          prop="hospitalPhone"
        >
          <el-input
            v-model="registerForm.hospitalPhone"
            placeholder="请输入医院注册手机号"
            clearable
          />
        </el-form-item>

        <el-form-item
          v-if="registerForm.role === 2"
          prop="hospitalPassword"
        >
          <el-input
            v-model="registerForm.hospitalPassword"
            type="password"
            placeholder="请输入医院注册密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            size="large"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </el-tab-pane>

    <el-tab-pane label="医院注册" name="hospital">
      <div class="register-header">
        <h1>医院注册</h1>
        <p>提交申请后等待医保局审批</p>
      </div>

      <el-form
        ref="hospitalFormRef"
        :model="hospitalForm"
        :rules="hospitalRules"
        class="register-form"
        size="large"
      >
        <el-form-item prop="name">
          <el-input
            v-model="hospitalForm.name"
            placeholder="请输入医院名称"
            clearable
          />
        </el-form-item>

        <el-form-item prop="address">
          <el-input
            v-model="hospitalForm.address"
            placeholder="请输入医院地址"
            clearable
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="hospitalForm.phone"
            placeholder="请输入联系电话"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="hospitalForm.password"
            type="password"
            placeholder="请输入登录密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="hospitalForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="hospitalLoading"
            style="width: 100%"
            size="large"
            @click="handleHospitalRegister"
          >
            提交申请
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </el-tab-pane>
  </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { http, USER_API, HOSPITAL_API } from '@/utils/request'
import type { UserRegisterVO } from '@/types'

const activeTab = ref('user')

interface RegisterDTO {
  phone: string
  name: string
  password: string
  role: number
  idCard?: string
  hospitalPhone?: string
  hospitalPassword?: string
}

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const registerForm = reactive<RegisterDTO & { confirmPassword: string }>({
  phone: '',
  name: '',
  password: '',
  confirmPassword: '',
  role: 1,
  idCard: '',
  hospitalPhone: '',
  hospitalPassword: '',
})

const validateIdCard = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('身份证号不能为空'))
    return
  }
  const idCardRegex = /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/
  if (!idCardRegex.test(value)) {
    callback(new Error('身份证号格式不正确'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateHospitalPhone = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
) => {
  if (registerForm.role !== 2) {
    callback()
    return
  }

  if (!value) {
    callback(new Error('请输入医院注册手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const validateHospitalPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
) => {
  if (registerForm.role !== 2) {
    callback()
    return
  }

  if (!value) {
    callback(new Error('请输入医院注册密码'))
  } else {
    callback()
  }
}

const rules = reactive<FormRules>({
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  idCard: [{ validator: validateIdCard, trigger: 'blur' }],
  hospitalPhone: [{ validator: validateHospitalPhone, trigger: 'blur' }],
  hospitalPassword: [{ validator: validateHospitalPassword, trigger: 'blur' }],
})

const handleRoleChange = () => {
  registerForm.hospitalPhone = ''
  registerForm.hospitalPassword = ''
}

// 医院注册相关
const hospitalFormRef = ref<FormInstance>()
const hospitalLoading = ref(false)

interface HospitalSignDTO {
  name: string
  address: string
  phone: string
  password: string
}

const hospitalForm = reactive<HospitalSignDTO & { confirmPassword: string }>({
  name: '',
  address: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const validateHospRegPhone = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入联系电话'))
    return
  }
  const phoneRegex = /^1[3-9]\d{9}$/
  const telRegex = /^0\d{2,3}-?\d{7,8}$/
  if (!phoneRegex.test(value) && !telRegex.test(value)) {
    callback(new Error('电话格式不正确'))
  } else {
    callback()
  }
}

const validateHospRegConfirm = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== hospitalForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const hospitalRules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入医院名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  address: [
    { required: true, message: '请输入医院地址', trigger: 'blur' },
    { min: 5, max: 200, message: '地址长度在 5 到 200 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, validator: validateHospRegPhone, trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateHospRegConfirm, trigger: 'blur' },
  ],
})

const handleHospitalRegister = async () => {
  if (!hospitalFormRef.value) return

  await hospitalFormRef.value.validate(async (valid) => {
    if (!valid) return

    hospitalLoading.value = true
    try {
      await http.post(HOSPITAL_API.SIGN, {
        name: hospitalForm.name,
        address: hospitalForm.address,
        phone: hospitalForm.phone,
        password: hospitalForm.password,
      })
      ElMessage.success('提交成功，等待医保局审批')
      hospitalFormRef.value?.resetFields()
    } catch (error) {
      console.error('医院注册失败:', error)
    } finally {
      hospitalLoading.value = false
    }
  })
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const registerData: RegisterDTO = {
      phone: registerForm.phone,
      name: registerForm.name,
      password: registerForm.password,
      role: registerForm.role,
      idCard: registerForm.idCard,
      hospitalPhone: registerForm.hospitalPhone,
      hospitalPassword: registerForm.hospitalPassword,
    }
    // 医院角色才传 hospitalPhone 和 hospitalPassword
    if (registerData.role !== 2) {
      delete registerData.hospitalPhone
      delete registerData.hospitalPassword
    }

    loading.value = true
    try {
      await http.post<UserRegisterVO>(USER_API.REGISTER, registerData)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #b3e5fc 0%, #4fc3f7 100%);
  padding: 20px;
}

.register-box {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.register-header {
  text-align: center;
  margin-bottom: 40px;

  h1 {
    font-size: 28px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #666;
  }
}

.register-form {
  .el-form-item {
    margin-bottom: 24px;
  }
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;

  a {
    color: #42a5f5;
    text-decoration: none;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
