<!-- 个人设置 -->
<template>
  <div class="mySetWrapper content">
    <CardsTitle class="marg-bt-40" title="个人设置" />
    <TableSwitchBar :data="tabData" @changeTable="checkHandle"></TableSwitchBar>  
    <div v-if="act == 0" class="fx-sb pd-tp-30">
      <div>
        <div class="fx">
          <div class="item fx">
            <span class="lab">昵称：</span> <el-input v-model="user.name" placeholder="请输入内容"></el-input>
          </div>
          <div class="item fx">
            <span class="lab">邮箱：</span> <el-input v-model="user.email" placeholder="请输入邮箱"></el-input>
          </div>
        </div>
        <div class="item fx">
          <span class="lab">性别：</span>
          <el-radio-group class="radioGroup" v-model="user.gender">
            <el-radio :label="0">男</el-radio>
            <el-radio :label="1">女</el-radio>
          </el-radio-group>
        </div>
        <div class="item fx">
          <div class="bt" @click="updateUserInfoHandle">更新信息</div>
        </div>
      </div>
      <div>
        <el-upload
          class="avatar-uploader"
          :action="actions"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :headers="uploadHeaders"
          >
          <img v-if="imageUrl" :src="imageUrl" class="avatar">
          <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          <div class="uploadBut"><span>上传头像</span></div>
        </el-upload>
      </div>
    </div>
    <div v-else class="set pd-tp-30">
      <div class="line fx-sb"><div><span>登录密码</span> 已设置</div><span class="font-bt" @click="openPasswordDialog">修改</span></div>
      <div class="line fx-sb"><div><span>绑定手机</span> {{ maskPhone(userInfo.cellPhone) }}</div><span class="statusText">已绑定</span></div>
      <div class="line fx-sb"><div><span>绑定邮箱</span> {{ userInfo.email || '未绑定' }}</div><span class="font-bt" @click="act = 0">编辑</span></div>
    </div>

    <el-dialog v-model="passwordDialogVisible" title="修改登录密码" width="420px">
      <el-form label-width="88px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">确认修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>

/** 数据导入 **/
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import { updateUserInfo, getUserInfo } from "@/api/user.js";
import { useUserStore } from "@/store"
import proxy from '@/config/proxy';


// 组件导入
import CardsTitle from "./components/CardsTitle.vue";
import TableSwitchBar from "@/components/TableSwitchBar.vue";
import router from "../../router";

const store = useUserStore()
const userInfo = ref(store.getUserInfo)



const env = import.meta.env.MODE || "development"
const actions = proxy[env].host+'/ms/files'
const uploadHeaders = {authorization: store.getToken}
const tabData = [
  {id: 0, name: '基本信息'},
  {id: 1, name: '安全设置'}
]
// 切换基本信息和安全设置
const act = ref(0)
const checkHandle = val => {
  act.value = val
}
// 更新信息的参数
const user = reactive({
  name: userInfo.value.name,
  icon: userInfo.value.icon,
  gender: userInfo.value.gender || 0,
  email: userInfo.value.email || ''
})
// 图片上传
const imageUrl = ref(user.icon)
function handleAvatarSuccess(res, file) {
  if (res.code == 200) {
    imageUrl.value = URL.createObjectURL(file.raw);
    user.icon = res.data.path
  } else {
    ElMessage({
      message: '图片上传出错，请联系管理员',
      type: 'error'
    })
  }
}

const maskPhone = phone => {
  if (!phone || phone.length < 7) return phone || '未绑定'
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

const passwordDialogVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  password: '',
  confirmPassword: ''
})

const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const submitPassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.password) {
    ElMessage.error('请输入原密码和新密码')
    return
  }
  if (passwordForm.password.length < 6) {
    ElMessage.error('新密码不能少于6位')
    return
  }
  if (passwordForm.password !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  passwordSubmitting.value = true
  try {
    const res = await updateUserInfo({
      oldPassword: passwordForm.oldPassword,
      password: passwordForm.password
    })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '密码修改失败')
      return
    }
    passwordDialogVisible.value = false
    ElMessage.success('密码修改成功')
  } catch (error) {
    ElMessage.error('密码修改失败')
  } finally {
    passwordSubmitting.value = false
  }
}

// 提交更新信息
const updateUserInfoHandle = async () => {
  await updateUserInfo(user)
    .then(async (res) => {
      if (res.code == 200) {
        // 从新获取当前登录用户的信息
        const data = await getUserInfo()
        if (data.code == 200) {
            // 记录到store
            store.setUserInfo(data.data)
            userInfo.value = data.data
            router.go(0)
        } 
      } else {
        ElMessage({
          message:res.msg,
          type: 'error'
        });
      }
    })
    .catch(() => {
      ElMessage({
        message: "请求出错！",
        type: 'error'
      });
    });
};
</script>
<style lang="scss" src="./index.scss"> </style>


