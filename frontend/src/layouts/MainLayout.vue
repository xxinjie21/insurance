<template>
  <div class="main-layout">
    <el-container>
      <el-header>
        <div class="header-content">
          <div class="logo">
            <el-icon :size="28" color="#667eea"><FirstAidKit /></el-icon>
            <span>医保核销系统</span>
          </div>

          <div class="header-right">
            <!-- 管理员选择医院提示 -->
            <div v-if="isAdmin && selectedHospitalId" class="hospital-selector">
              <el-tag type="warning" size="small" closable @close="handleUnselectHospital">
                当前医院: {{ selectedHospitalName }}
              </el-tag>
              <el-button size="small" text type="primary" @click="showHospitalDialog">
                切换
              </el-button>
            </div>
            <div v-if="isAdmin && !selectedHospitalId" class="hospital-selector">
              <el-button size="small" type="warning" @click="showHospitalDialog">
                选择医院
              </el-button>
            </div>

            <div class="user-info" v-if="userInfo">
              <el-tag :type="getRoleType(userInfo.role)" size="small">
                {{ getRoleName(userInfo.role) }}
              </el-tag>
              <span class="username">{{ userInfo.name }}</span>
            </div>

            <el-dropdown @command="handleCommand">
              <el-avatar :size="40" icon="User" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-container>
        <el-aside :width="asideWidth">
          <el-menu
            :default-active="activeMenu"
            background-color="#001529"
            text-color="rgba(255, 255, 255, 0.65)"
            active-text-color="#fff"
            :collapse="isCollapse"
            router
          >
            <template v-for="item in menuItems" :key="item.path">
              <el-menu-item v-if="!item.children" :index="item.path" @click="handleMenuClick(item)">
                <el-icon><component :is="item.icon" /></el-icon>
                <template #title>{{ item.title }}</template>
              </el-menu-item>

              <el-sub-menu v-else :index="item.path">
                <template #title>
                  <el-icon><component :is="item.icon" /></el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in item.children"
                  :key="child.path"
                  :index="child.path"
                  @click="handleMenuClick(child)"
                >
                  <el-icon><component :is="child.icon" /></el-icon>
                  <template #title>{{ child.title }}</template>
                </el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>
        </el-aside>

        <el-main>
          <div class="main-content">
            <router-view />
          </div>
        </el-main>
      </el-container>
    </el-container>

    <!-- 管理员选择医院对话框 -->
    <el-dialog v-model="hospitalDialogVisible" title="选择医院" width="500px" :close-on-click-modal="false">
      <p style="margin-bottom: 12px; color: #666;">请选择要操作的医院，选择后可访问该医院的全部数据</p>
      <el-table
        :data="hospitalList"
        v-loading="hospitalLoading"
        highlight-current-row
        @current-change="handleHospitalSelect"
        style="width: 100%"
        max-height="400"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="医院名称" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination" v-if="hospitalTotal > 0">
        <el-pagination
          v-model:current-page="hospitalPageNum"
          v-model:page-size="hospitalPageSize"
          :total="hospitalTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          @current-change="fetchHospitalList"
        />
      </div>
      <template #footer>
        <el-button type="info" @click="hospitalDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!tempSelectedHospital" @click="confirmSelectHospital">
          确认选择
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { http, USER_API, HOSPITAL_API } from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userInfo = computed(() => userStore.getUserInfo())
const { isAdmin } = storeToRefs(userStore)
const selectedHospitalId = computed(() => userStore.selectedHospitalId)
const selectedHospitalName = computed(() => userStore.selectedHospitalName)
const isCollapse = ref(false)
const asideWidth = computed(() => (isCollapse.value ? '64px' : '200px'))
const activeMenu = computed(() => route.path)

// 医院选择对话框
const hospitalDialogVisible = ref(false)
const hospitalList = ref<any[]>([])
const hospitalLoading = ref(false)
const hospitalPageNum = ref(1)
const hospitalPageSize = ref(10)
const hospitalTotal = ref(0)
const tempSelectedHospital = ref<any>(null)

interface MenuItem {
  path: string
  title: string
  icon: string
  requireHospital?: boolean  // 标记该菜单需要先选择医院
  children?: MenuItem[]
}

// 需要先选择医院的菜单路径
const hospitalRequiredPaths = [
  '/hospital/visits',
  '/hospital/visit/add',
  '/hospital/settles',
  '/hospital/batches',
  '/hospital/patients',
]

// 点击菜单项时的拦截逻辑
const handleMenuClick = (item: MenuItem) => {
  if (isAdmin.value && hospitalRequiredPaths.includes(item.path) && !selectedHospitalId.value) {
    // 管理员访问医院功能但未选择医院，弹出选择框
    ElMessage.warning('请先选择要操作的医院')
    showHospitalDialog()
  }
}

// 显示医院选择对话框
const showHospitalDialog = () => {
  tempSelectedHospital.value = null
  hospitalDialogVisible.value = true
  fetchHospitalList()
}

// 获取医院列表
const fetchHospitalList = async () => {
  hospitalLoading.value = true
  try {
    const response = await http.get(HOSPITAL_API.LIST, {
      params: { pageNum: hospitalPageNum.value, pageSize: hospitalPageSize.value },
    })
    if (response.data) {
      const pageData = response.data as any
      hospitalList.value = pageData.records || []
      hospitalTotal.value = pageData.total || 0
    }
  } catch (error) {
    console.error('获取医院列表失败:', error)
  } finally {
    hospitalLoading.value = false
  }
}

// 表格行选中
const handleHospitalSelect = (row: any) => {
  tempSelectedHospital.value = row
}

// 确认选择医院
const confirmSelectHospital = async () => {
  if (!tempSelectedHospital.value) return
  try {
    await http.post(HOSPITAL_API.SELECT(tempSelectedHospital.value.id))
    userStore.setSelectedHospital(tempSelectedHospital.value.id, tempSelectedHospital.value.name)
    hospitalDialogVisible.value = false
    ElMessage.success(`已选择医院: ${tempSelectedHospital.value.name}`)
  } catch (error) {
    console.error('选择医院失败:', error)
  }
}

// 取消选择医院
const handleUnselectHospital = async () => {
  try {
    await http.post(HOSPITAL_API.UNSELECT)
    userStore.clearSelectedHospital()
    ElMessage.success('已取消选择医院')
  } catch (error) {
    console.error('取消选择失败:', error)
  }
}

// 根据角色动态生成菜单
const menuItems = computed<MenuItem[]>(() => {
  const role = userStore.getRole()

  // 公共菜单
  const commonMenus: MenuItem[] = [
    {
      path: '/home',
      title: '首页',
      icon: 'HomeFilled',
    },
  ]

  // 患者菜单
  if (role === 1) {
    return [
      ...commonMenus,
      {
        path: '/hospital/list',
        title: '医院列表',
        icon: 'OfficeBuilding',
      },
      {
        path: '/patient/visits',
        title: '我的就诊',
        icon: 'FirstAidKit',
      },
      {
        path: '/patient/settles',
        title: '我的结算',
        icon: 'Document',
      },
      {
        path: '/patient/account',
        title: '我的账户',
        icon: 'Wallet',
      },
      {
        path: '/patient/recharge/list',
        title: '充值记录',
        icon: 'TopRight',
      },
      {
        path: '/patient/consumption/list',
        title: '消费记录',
        icon: 'BottomRight',
      },
    ]
  }

  // 医院菜单
  if (role === 2) {
    return [
      ...commonMenus,
      {
        path: '/hospital/list',
        title: '医院列表',
        icon: 'OfficeBuilding',
      },
      {
        path: '/hospital/visits',
        title: '就诊管理',
        icon: 'FirstAidKit',
      },
      {
        path: '/hospital/visit/add',
        title: '新增就诊',
        icon: 'Plus',
      },
      {
        path: '/hospital/settles',
        title: '结算单管理',
        icon: 'DocumentChecked',
      },
      {
        path: '/hospital/batches',
        title: '医保申报',
        icon: 'DocumentCopy',
      },
      {
        path: '/hospital/patients',
        title: '本院患者',
        icon: 'User',
      },
      {
        path: '/registration',
        title: '挂号管理',
        icon: 'Tickets',
      },
      {
        path: '/inpatient',
        title: '住院管理',
        icon: 'HomeFilled',
      },
      {
        path: '/catalog',
        title: '医保目录',
        icon: 'Collection',
      },
    ]
  }

  // 医保局菜单
  if (role === 3) {
    return [
      ...commonMenus,
      {
        path: '/hospital/list',
        title: '医院列表',
        icon: 'OfficeBuilding',
      },
      {
        path: '/hospital/sign',
        title: '医院审批',
        icon: 'CirclePlus',
      },
      {
        path: '/medical/batches',
        title: '基金拨付',
        icon: 'Document',
      },
      {
        path: '/medical/audit',
        title: '医保审核',
        icon: 'Checked',
      },
      {
        path: '/medical/report',
        title: '统计报表',
        icon: 'DataAnalysis',
      },
      {
        path: '/catalog',
        title: '医保目录',
        icon: 'Collection',
      },
    ]
  }

  // 管理员菜单 - 拥有所有功能和操作
  if (role === 4) {
    return [
      ...commonMenus,
      // 患者功能
      {
        path: '/patient',
        title: '患者功能',
        icon: 'User',
        children: [
          {
            path: '/patient/visits',
            title: '就诊查询',
            icon: 'FirstAidKit',
          },
          {
            path: '/patient/settles',
            title: '结算查询',
            icon: 'Document',
          },
          {
            path: '/patient/account',
            title: '账户管理',
            icon: 'Wallet',
          },
          {
            path: '/patient/recharge/list',
            title: '充值记录',
            icon: 'TopRight',
          },
          {
            path: '/patient/consumption/list',
            title: '消费记录',
            icon: 'BottomRight',
          },
        ],
      },
      // 医院功能
      {
        path: '/hospital',
        title: '医院功能',
        icon: 'OfficeBuilding',
        requireHospital: true,
        children: [
          {
            path: '/hospital/visits',
            title: '就诊管理',
            icon: 'FirstAidKit',
            requireHospital: true,
          },
          {
            path: '/hospital/visit/add',
            title: '新增就诊',
            icon: 'Plus',
            requireHospital: true,
          },
          {
            path: '/hospital/settles',
            title: '结算单管理',
            icon: 'DocumentChecked',
            requireHospital: true,
          },
          {
            path: '/hospital/batches',
            title: '医保申报',
            icon: 'DocumentCopy',
            requireHospital: true,
          },
          {
            path: '/hospital/patients',
            title: '本院患者',
            icon: 'UserFilled',
            requireHospital: true,
          },
          {
            path: '/registration',
            title: '挂号管理',
            icon: 'Tickets',
            requireHospital: true,
          },
          {
            path: '/inpatient',
            title: '住院管理',
            icon: 'HomeFilled',
            requireHospital: true,
          },
          {
            path: '/catalog',
            title: '医保目录',
            icon: 'Collection',
            requireHospital: true,
          },
        ],
      },
      // 医保局功能
      {
        path: '/medical',
        title: '医保局功能',
        icon: 'DocumentChecked',
        children: [
          {
            path: '/hospital/list',
            title: '医院列表',
            icon: 'OfficeBuilding',
          },
          {
            path: '/hospital/sign',
            title: '医院审批',
            icon: 'CirclePlus',
          },
          {
            path: '/medical/batches',
            title: '基金拨付',
            icon: 'Document',
          },
          {
            path: '/medical/audit',
            title: '医保审核',
            icon: 'Checked',
          },
          {
            path: '/medical/report',
            title: '统计报表',
            icon: 'DataAnalysis',
          },
        ],
      },
    ]
  }

  return commonMenus
})

const getRoleType = (role: number) => {
  const types: Record<number, 'success' | 'warning' | 'danger' | 'primary'> = {
    1: 'success',   // 患者 - 绿色
    2: 'warning',   // 医院 - 黄色
    3: 'danger',    // 医保局 - 红色
    4: 'primary',   // 管理员 - 蓝色
  }
  return types[role] || 'info'
}

const getRoleName = (role: number) => {
  const names: Record<number, string> = {
    1: '患者',
    2: '医院',
    3: '医保局',
    4: '管理员',
  }
  return names[role] || '未知'
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })

      const token = userStore.getToken()
      if (token) {
        await http.post(USER_API.LOGOUT)
      }

      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('退出失败:', error)
      }
    }
  }
}

const handleResize = () => {
  isCollapse.value = window.innerWidth < 1200
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
  overflow: hidden;
}

.el-container {
  height: 100%;
}

.el-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0;
  display: flex;
  align-items: center;

  .header-content {
    width: 100%;
    padding: 0 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .hospital-selector {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .username {
        font-size: 14px;
        color: #666;
      }
    }
  }
}

.el-aside {
  background: #001529;
  transition: width 0.3s;
  overflow-x: hidden;

  .el-menu {
    border-right: none;
  }
}

.el-main {
  background: #f0f2f5;
  padding: 24px;
  overflow-y: auto;

  .main-content {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    min-height: calc(100vh - 136px);
  }
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
