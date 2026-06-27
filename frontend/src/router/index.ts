import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', requiresAuth: true },
      },
      // 患者相关页面 - 患者(1)和管理员(4)可访问
      {
        path: 'patient/visits',
        name: 'PatientVisits',
        component: () => import('@/views/patient/VisitList.vue'),
        meta: { title: '我的就诊', requiresAuth: true, roles: [1, 4] },
      },
      {
        path: 'patient/settles',
        name: 'PatientSettles',
        component: () => import('@/views/patient/SettleList.vue'),
        meta: { title: '我的结算', requiresAuth: true, roles: [1, 4] },
      },
      {
        path: 'patient/account',
        name: 'PatientAccount',
        component: () => import('@/views/patient/Account.vue'),
        meta: { title: '我的账户', requiresAuth: true, roles: [1, 4] },
      },
      {
        path: 'patient/recharge',
        name: 'Recharge',
        component: () => import('@/views/patient/Recharge.vue'),
        meta: { title: '账户充值', requiresAuth: true, roles: [1, 4] },
      },
      {
        path: 'patient/recharge/list',
        name: 'RechargeList',
        component: () => import('@/views/patient/RechargeList.vue'),
        meta: { title: '充值记录', requiresAuth: true, roles: [1, 4] },
      },
      {
        path: 'patient/consumption/list',
        name: 'ConsumptionList',
        component: () => import('@/views/patient/ConsumptionList.vue'),
        meta: { title: '消费记录', requiresAuth: true, roles: [1, 4] },
      },
      // 医院相关页面 - 医院(2)和管理员(4)可访问
      {
        path: 'hospital/visits',
        name: 'HospitalVisits',
        component: () => import('@/views/hospital/VisitList.vue'),
        meta: { title: '就诊管理', requiresAuth: true, roles: [2, 4] },
      },
      {
        path: 'hospital/visit/add',
        name: 'AddVisit',
        component: () => import('@/views/hospital/AddVisit.vue'),
        meta: { title: '新增就诊', requiresAuth: true, roles: [2, 4] },
      },
      {
        path: 'hospital/visit/:visitId/fees',
        name: 'VisitFees',
        component: () => import('@/views/hospital/VisitFees.vue'),
        meta: { title: '费用管理', requiresAuth: true, roles: [2, 4] },
      },
      {
        path: 'hospital/visit/:visitId/settle',
        name: 'VisitSettle',
        component: () => import('@/views/hospital/VisitSettle.vue'),
        meta: { title: '医保结算', requiresAuth: true, roles: [2, 4] },
      },
      {
        path: 'hospital/settles',
        name: 'HospitalSettles',
        component: () => import('@/views/hospital/SettleList.vue'),
        meta: { title: '结算单管理', requiresAuth: true, roles: [2, 4] },
      },
      {
        path: 'hospital/batches',
        name: 'BatchList',
        component: () => import('@/views/hospital/BatchList.vue'),
        meta: { title: '医保申报', requiresAuth: true, roles: [2, 4] },
      },
      {
        path: 'hospital/batch/:batchId',
        name: 'BatchDetail',
        component: () => import('@/views/hospital/BatchDetail.vue'),
        meta: { title: '批次详情', requiresAuth: true, roles: [2, 4] },
      },
      {
        path: 'hospital/batch-item/list',
        redirect: '/hospital/batches',
      },
      {
        path: 'hospital/patients',
        name: 'HospitalPatients',
        component: () => import('@/views/hospital/HospitalPatientList.vue'),
        meta: { title: '本院患者', requiresAuth: true, roles: [2, 4] },
      },
      // 医院管理页面 - 所有已登录用户可查看列表，但只有医保局(3)和管理员(4)可以注册医院
      {
        path: 'hospital/list',
        name: 'HospitalList',
        component: () => import('@/views/hospital/HospitalList.vue'),
        meta: { title: '医院列表', requiresAuth: true, roles: [1, 2, 3, 4] },
      },
      {
        path: 'hospital/sign',
        name: 'HospitalSign',
        component: () => import('@/views/hospital/HospitalSign.vue'),
        meta: { title: '医院注册', requiresAuth: true, roles: [3, 4] },
      },
      // 医保局相关页面 - 医保局(3)和管理员(4)可访问
      {
        path: 'medical/batches',
        name: 'MedicalBatches',
        component: () => import('@/views/medical/BatchList.vue'),
        meta: { title: '基金拨付', requiresAuth: true, roles: [3, 4] },
      },
      {
        path: 'medical/batch/:batchId',
        name: 'MedicalBatchDetail',
        component: () => import('@/views/hospital/BatchDetail.vue'),
        meta: { title: '批次详情', requiresAuth: true, roles: [3, 4], readonly: true },
      },
      {
        path: 'medical/batch/:batchId/pay',
        redirect: (to) => ({ path: '/medical/batches', query: { batchId: to.params.batchId } }),
      },
    ],
  },
  // 404 兜底路由
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/home',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const token = userStore.getToken()
  const userInfo = userStore.getUserInfo()

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 医保核销系统`
  }

  // 登录/注册页面：单独处理，已登录跳首页，未登录直接放行
  if (to.name === 'Login' || to.name === 'Register') {
    if (token && userInfo) {
      next({ name: 'Home' })
    } else {
      next()
    }
    return
  }

  // 需要登录的页面：未登录跳登录页
  if (to.meta.requiresAuth !== false) {
    if (!token || !userInfo) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    // 检查角色权限
    if (to.meta.roles && Array.isArray(to.meta.roles)) {
      const userRole = userInfo.role
      if (!(to.meta.roles as number[]).includes(userRole)) {
        next({ name: 'Home' })
        return
      }
    }
  }

  next()
})

export default router
