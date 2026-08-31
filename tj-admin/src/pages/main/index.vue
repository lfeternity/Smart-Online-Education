<!-- 首页 -->
<template>
  <div class="container mainWrapper" style="margin-bottom: -20px">
    <!-- 左侧 -->
    <div class="left bg-wt">
      <!-- 今日数据 -->
      <div class="todaydata bg-wt">
        <p class="todaydataTitle">
          <span>今日数据</span>
          <span class="todaydataTime">
            <a href="###" @click="Refreshtime"
              ><img
                src="@/assets/btn-shx.png"
                alt=""
                style="
                  width: 14px;
                  height: 14px;
                  position: relative;
                  left: -2px;
                  top: 1px;
                "
            /></a>
            <span> 更新时间：{{ nowtime }}</span></span
          >
        </p>
        <div class="todaydatabody">
          <div class="todaydatacard">
            <img src="@/assets/Visits.png" alt="" class="todaydataImg" />
            <div>
              <div class="todaydatatit">
                {{ todaydata.data.visits }}<span class="Company">万</span>
              </div>
              <div class="todaydatainfo">今日访问量</div>
            </div>
          </div>
          <div class="todaydatacard">
            <img
              src="@/assets/todayordermoney.png"
              alt=""
              class="todaydataImg"
            />
            <div>
              <div class="todaydatatit">
                {{ todaydata.data.orderAmount }}<span class="Company">万</span>
              </div>
              <div class="todaydatainfo">今日订单金额</div>
            </div>
          </div>
          <div class="todaydatacard">
            <img src="@/assets/todayorders.png" alt="" class="todaydataImg" />
            <div>
              <div class="todaydatatit">
                {{ todaydata.data.orderNum }}<span class="Company">笔</span>
              </div>
              <div class="todaydatainfo">今日订单笔数</div>
            </div>
          </div>
          <div class="todaydatacard">
            <img
              src="@/assets/todaynewstudent.png"
              alt=""
              class="todaydataImg"
            />
            <div>
              <div class="todaydatatit">
                {{ todaydata.data.stuNewNum }}<span class="Company">人</span>
              </div>
              <div class="todaydatainfo">今日新增学员</div>
            </div>
          </div>
        </div>
      </div>
      <!-- 数据看板（近15天） -->
      <div class="boarddata bg-wt">
        <div class="boarddatahead">
          <div style="margin-top: 8px">数据看板（近15天）</div>
          <div class="tab">
            <el-tabs v-model="actId" class="demo-tabs">
              <el-tab-pane
                v-for="(item, index) in tableBar"
                :key="index + 1"
                :label="item.name"
                :name="index + 1"
              >
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
        <coreChart v-if="actId == 1"></coreChart>
        <flowStatistics v-if="actId == 2"></flowStatistics>
        <userStatistics v-if="actId == 3"></userStatistics>
        <tranSaction v-if="actId == 4"></tranSaction>
      </div>
      <!-- top10榜单 -->
      <div class="Popularcourses bg-wt">
        <TableSwitchBar
          :data="tableTop10"
          @changeTable="changeTable"
        ></TableSwitchBar>
        <popularCourses
          :formData="tableTop10Datas"
        ></popularCourses>
      </div>
    </div>
    <!-- 右侧 -->
    <div class="right bg-wt">
      <!-- 待办事项 -->
       <div class="Todolist bg-wt">
        <p class="Todotitle">待办事项</p>
        <div class="item-card">
          <router-link to="/order/refund">
            <div class="card">
              <div class="tit">6</div>
              <div class="info">退款待审批</div>
            </div>
          </router-link>
          <router-link to="/marketing/index">
            <div class="card">
              <div class="tit">6</div>
              <div class="info">优惠券待发布</div>
            </div>
          </router-link>
        </div>
      </div>
      <!-- 常用功能 -->
      <div class="CommonUse bg-wt">
        <p class="CommonUsetitle">常用功能</p>
        <div class="item-card">
          <router-link to="/user/users">
            <div class="card">
              <div class="CommonUsetit">
                <img
                  src="@/assets/houtairenyuan.png"
                  alt=""
                  class="CommonUseicon"
                />
              </div>
              <div class="CommonUseinfo">新增后台人员</div>
            </div>
          </router-link>
          <router-link to="/user/teacher">
            <div class="card">
              <div class="CommonUsetit">
                <img
                  src="@/assets/courseteacher.png"
                  alt=""
                  class="CommonUseicon"
                />
              </div>
              <div class="CommonUseinfo">新增课程教师</div>
            </div>
          </router-link>
        </div>
        <div class="item-card">
          <router-link to="/curriculum/index">
            <div class="card">
              <div class="CommonUsetit">
                <img
                  src="@/assets/newcourse.png"
                  alt=""
                  class="CommonUseicon"
                />
              </div>
              <div class="CommonUseinfo">新增课程</div>
            </div>
          </router-link>
          <router-link to="/order/index">
            <div class="card">
              <div class="CommonUsetit">
                <img src="@/assets/order.png" alt="" class="CommonUseicon" />
              </div>
              <div class="CommonUseinfo">订单管理</div>
            </div>
          </router-link>
        </div>
      </div>
      <!-- 关键词top10 -->
      <div class="Keyword bg-wt">
        <p class="Keywordtitle">关键词TOP10</p>
        <span
          style="
            position: absolute;
            top: 52px;
            left: 21%;
            font-family: PingFangSC-Regular;
            font-size: 12px;
            color: rgba(51, 41, 41, 0.25);
          "
          >出国留学</span
        >
        <span
          style="
            position: absolute;
            top: 59px;
            right: 10%;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 12px;
            color: rgba(51, 41, 41, 0.67);
          "
          >新媒体运营</span
        >
        <span
          style="
            position: absolute;
            top: 81px;
            left: 9%;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 13px;
            color: #ffb057;
          "
          >企业管理</span
        >
        <span
          style="
            position: absolute;
            top: 72px;
            right: 36%;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 19px;
            color: rgba(255, 115, 79, 0.62);
          "
          >UI设计</span
        >
        <span
          style="
            position: absolute;
            left: 19%;
            bottom: 80px;
            font-family: PingFangSC-S0pxibold;
            font-weight: 600;
            font-size: 25px;
            color: #ff734f;
          "
          >JAVA &nbsp; 零基础</span
        >
        <span
          style="
            position: absolute;
            left: 11%;
            bottom: 62px;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 12px;
            color: rgba(51, 41, 41, 0.67);
          "
          >跨境电商</span
        >
        <span
          style="
            position: absolute;
            left: 40%;
            bottom: 53px;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 12px;
            color: rgba(51, 41, 41, 0.53);
          "
          >投资理财</span
        >
        <span
          style="
            position: absolute;
            right: 6%;
            bottom: 62px;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 12px;
            color: #ffb057;
          "
          >c/c++</span
        >
        <span
          style="
            position: absolute;
            left: 9%;
            bottom: 25px;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 17px;
            color: rgba(255, 115, 79, 0.62);
          "
          >公务员考试</span
        >
        <span
          style="
            bottom: 25px;
            right: 10%;
            position: absolute;
            font-family: PingFangSC-Regular;
            font-weight: 400;
            font-size: 17px;
            color: #332929;
          "
          >教师考试</span
        >
      </div>
      <!-- 消息动态 -->
      <div class="Message bg-wt">
        <p class="Messagetitle">消息动态</p>
        <div class="Messageitem" v-for="(item, index) in messageData" :key="index">
          <div class="Messageitemhead">
            <span class="Messageitemtitle"><img :src="item.icon" alt=""
                class="Messageitemicon" />
              <span class="nickname">{{item.name}}</span> </span><span class="waitgrounding">{{item.title}}</span>
          </div>
          <div class="title">
            <p class="Messageitembody">{{item.desc}}</p>
            <span class="hover" >{{item.desc}}</span >
          </div>
          <p class="Messageitemtime">{{item.time}}</p>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
/** 数据导入 **/
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
// 引入接口api
import { getToday, getTop10 } from '@/api/main'
// 引入组件
import coreChart from './components/coreChart.vue' // 引入核心指标组件
import flowStatistics from './components/flowStatistics.vue' // 引入交易统计组件
import tranSaction from './components/tranSaction.vue' // 引入流量统计组件
import userStatistics from './components/userStatistics.vue' // 引入用户统计组件
import popularCourses from './components/popularCourses.vue' // 引入热门课程组件
import TableSwitchBar from "./components/TableSwitch.vue" // 切换top10表格
import iconImg from  '@/assets/img_touxiang_small@2x.png'
// 数组
const messageData = [
  {
    icon: iconImg,
    name: '白展堂',
    title: '新增课程',
    desc: '软件测试之python自动化测试5(web/app/接口自动化/自动化框架）',
    time: '12分钟前'
  },
  {
    icon: iconImg,
    name: '莫小贝',
    title: '新增课程',
    desc: '套餐-Java后台医院预约挂号小程序 毕设源码+基础课',
    time: '05.16 09:00'
  },
  {
    icon: iconImg,
    name: '白展堂',
    title: '新增优惠券',
    desc: '全场打折、优惠居多、机不可失，快来抢购',
    time: '05.14 09:00'
  },
  {
    icon: iconImg,
    name: '佟湘玉',
    title: '新增题目',
    desc: 'Thread类中能让线程体进行休眠的方法是什么？',
    time: '05.13 09:00'
  },
  {
    icon: iconImg,
    name: '莫小贝',
    title: '新增题目',
    desc: '访问权限修饰符有public、private、 protected和默认修饰符 (没有写任何修饰符)，他们既可以用来修饰类，也可以用来修饰类中的成员，使用private修饰符的成员 可见情况有()',
    time: '05.13 09:00'
  }
]
// 今日数据
const todaydata = reactive({
  data: {}
})
// 当前时间
const nowtime = ref('')
// table切换数据 - 静态数据
const tableBar = [{ id: 1, name: '核心指标' }, { id: 2, name: '交易统计' }, { id: 3, name: '用户统计' }, { id: 4, name: '流量统计' }]
const tableTop10 = [{ id: 1, name: '上周热门课程TOP10' }, { id: 2, name: '上周热销课程TOP10' }]
const tableTop10Data = reactive({
  hot: [],
  hotSales: []
})
// mounted生命周期
onMounted(() => {
  getTodayData()
  getNowTime()
  gettop10Data()
})

// 当前的柱状图table选项
const actId = ref(1)
// 当前的top10排行榜table选项
const topId = ref(1)
const tableTop10Datas = ref([])
// table切换 当前展示信息top10
const changeTable = id => {
  topId.value = id
  tableTop10Datas.value = id == 1 ? tableTop10Data.hot : tableTop10Data.hotSales
}
// 获取今日数据
const getTodayData = async () => {
  const res = await getToday()
  if (res.code === 200) {
    todaydata.data = res.data
  } else {
    ElMessage.error(res.msg)
  }
}
// 获取当前时间
const getNowTime = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  // nowtime的值为当前时间前一个小时的整点
  nowtime.value = `${year}.${month}.${day} ${hour - 1}:00:00`
  if (hour === 0) {
    nowtime.value = `${year}.${month}.${day - 1} 23:00:00`
  } else {
    nowtime.value = `${year}.${month}.${day} ${hour - 1}:00:00`
  }
}
// 点击刷新当前时间
const Refreshtime = () => {
  getNowTime()
}

// 获取数据
const gettop10Data = async () => {
  const res = await getTop10()
  if (res.code === 200) {
    tableTop10Data.hotSales = res.data.hotSales
    tableTop10Data.hot = res.data.hot
    tableTop10Datas.value = res.data.hot
  } else {
    ElMessage.error(res.msg)
  }
}
</script>
<style lang="scss" scoped src="./index.scss"></style>
