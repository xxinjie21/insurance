<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <div class="welcome-content">
        <h1>欢迎使用医保核销系统</h1>
        <p class="subtitle">Medical Insurance Reimbursement System</p>

        <el-row :gutter="24" class="stat-cards">
          <el-col :span="8" v-if="isHospital">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #667eea">
                  <el-icon :size="32"><FirstAidKit /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.pendingVisitCount }}</div>
                  <div class="stat-label">待结算就诊</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8" v-if="isHospital">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #52c41a">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.pendingBatchCount }}</div>
                  <div class="stat-label">待申报批次</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8" v-if="isHospital">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #faad14">
                  <el-icon :size="32"><Wallet /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">¥{{ formatMoney(stats.pendingDisburseAmount) }}</div>
                  <div class="stat-label">待拨付金额</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="12" v-if="isPatient">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #667eea">
                  <el-icon :size="32"><FirstAidKit /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.visitCount }}</div>
                  <div class="stat-label">我的就诊</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="12" v-if="isPatient">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #52c41a">
                  <el-icon :size="32"><Wallet /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">¥{{ formatMoney(stats.balance) }}</div>
                  <div class="stat-label">账户余额</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8" v-if="isMedical">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #667eea">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.pendingReviewBatchCount }}</div>
                  <div class="stat-label">待审核批次</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8" v-if="isMedical">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #52c41a">
                  <el-icon :size="32"><Wallet /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">¥{{ formatMoney(stats.pendingDisburseAmount) }}</div>
                  <div class="stat-label">待拨付金额</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8" v-if="isMedical">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" style="background: #faad14">
                  <el-icon :size="32"><CircleCheck /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ stats.disbursedBatchCount }}</div>
                  <div class="stat-label">已拨付批次</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-divider />

        <div class="quick-actions">
          <h3>快捷操作</h3>
          <el-space :size="16">
            <el-button
              v-if="isPatient"
              type="primary"
              icon="Plus"
              @click="$router.push('/patient/visits')"
            >
              查看就诊
            </el-button>

            <el-button
              v-if="isPatient"
              type="success"
              icon="Wallet"
              @click="$router.push('/patient/account')"
            >
              账户充值
            </el-button>

            <el-button
              v-if="isHospital"
              type="primary"
              icon="Plus"
              @click="$router.push('/hospital/visit/add')"
            >
              新增就诊
            </el-button>

            <el-button
              v-if="isHospital"
              type="success"
              icon="Document"
              @click="$router.push('/hospital/batches')"
            >
              申报批次
            </el-button>

            <el-button
              v-if="isMedical"
              type="primary"
              icon="Document"
              @click="$router.push('/medical/batches')"
            >
              批次审核
            </el-button>

            <el-button
              v-if="isAdmin"
              type="warning"
              icon="OfficeBuilding"
              @click="$router.push('/hospital/list')"
            >
              医院列表
            </el-button>

            <el-button
              v-if="isAdmin"
              type="primary"
              icon="Document"
              @click="$router.push('/medical/batches')"
            >
              批次审核
            </el-button>

            <el-button
              v-if="isAdmin"
              type="success"
              icon="Plus"
              @click="goHospitalPath('/hospital/visit/add')"
            >
              新增就诊
            </el-button>

            <el-button
              v-if="isAdmin"
              type="primary"
              icon="FirstAidKit"
              @click="goHospitalPath('/hospital/visits')"
            >
              就诊管理
            </el-button>
          </el-space>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { http, DASHBOARD_API } from '@/utils/request'
import { formatMoney } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const { isPatient, isHospital, isMedical, isAdmin } = storeToRefs(userStore)

const stats = ref({
  visitCount: 0,
  balance: 0,
  pendingVisitCount: 0,
  pendingBatchCount: 0,
  pendingDisburseAmount: 0,
  pendingReviewBatchCount: 0,
  disbursedBatchCount: 0,
})

const fetchStats = async () => {
  try {
    const response = await http.get(DASHBOARD_API.STATS, {
      params: { role: userStore.roleValue },
    })
    if (response.data) {
      stats.value = { ...stats.value, ...response.data }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

onMounted(() => {
  fetchStats()
})

const goHospitalPath = (path: string) => {
  if (!userStore.selectedHospitalId) {
    ElMessage.warning('请先在顶部选择要操作的医院')
    return
  }
  router.push(path)
}
</script>

<style lang="scss" scoped>
.home-container {
  .welcome-card {
    .welcome-content {
      padding: 20px 0;

      h1 {
        font-size: 32px;
        font-weight: 600;
        color: #1a1a1a;
        margin-bottom: 8px;
      }

      .subtitle {
        font-size: 16px;
        color: #666;
        margin-bottom: 32px;
      }

      .stat-cards {
        margin-bottom: 32px;

        .stat-card {
          display: flex;
          align-items: center;
          gap: 16px;

          .stat-icon {
            width: 64px;
            height: 64px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
          }

          .stat-info {
            flex: 1;

            .stat-value {
              font-size: 28px;
              font-weight: 600;
              color: #1a1a1a;
              margin-bottom: 4px;
            }

            .stat-label {
              font-size: 14px;
              color: #666;
            }
          }
        }
      }

      .quick-actions {
        h3 {
          font-size: 18px;
          font-weight: 600;
          color: #1a1a1a;
          margin-bottom: 16px;
        }
      }
    }
  }
}
</style>
