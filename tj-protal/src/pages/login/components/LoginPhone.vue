<!-- 登录页面 - 手机号 -->
<template>
  <div class="loginPhone">
    <el-form
      ref="formRef"
      :model="fromData"
      :rules="rules"
      label-width="0px"
      class="demo-dynamic"
    >
      <el-form-item prop="cellPhone" label="">
        <el-input v-model="fromData.cellPhone" placeholder="请输入手机号" maxlength="11" />
      </el-form-item>
      <el-form-item prop="code" label="">
        <div class="code-row">
          <el-input v-model="fromData.code" placeholder="请输入验证码" maxlength="4" />
          <button type="button" class="bt bt-grey code-button" :disabled="countdown > 0 || sending" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </button>
        </div>
      </el-form-item>
      <el-form-item class="marg-b-10">
        <div class="fx-sb">
            <div>
                <el-checkbox v-model="fromData.rememberMe" label="7天免登录" size="large" />
            </div>
            <div>找回密码</div>
        </div>
      </el-form-item>
      <el-form-item class="marg-bt-15">
        <button type="button" class="bt login-button" :disabled="submitting" @click="submitForm(formRef)">
          {{ submitting ? '登录中...' : '登 录' }}
        </button>
      </el-form-item>
    </el-form>
    <div class="font-bt text-center"  @click="goRegister">
        去注册
    </div>
  </div>
</template>
<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import { useRouter } from 'vue-router';
import { getUserInfo, phoneLogins, verifycode } from "@/api/user";
import { useUserStore } from '@/store';
import { ElMessage } from "element-plus";

const emit = defineEmits(['goHandle'])
const router = useRouter();
const store = useUserStore();
// 登录数据初始化
const formRef = ref();
const fromData = reactive({
  cellPhone: "",
  code: "",
  rememberMe: false
});
const submitting = ref(false);
const sending = ref(false);
const countdown = ref(0);
let countdownTimer;
const phonePattern = /^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\d{8}$/;
// 效验规则
const rules = reactive({
  cellPhone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: phonePattern, message: "请输入正确的手机号", trigger: "blur" },
  ],
  code: [
    { required: true, message: "请输入短信验证码", trigger: "blur" },
    { pattern: /^\d{4,6}$/, message: "验证码格式错误", trigger: "blur" },
  ],
});
const sendCode = async () => {
  if (sending.value || countdown.value > 0) return;
  if (!phonePattern.test(fromData.cellPhone)) {
    ElMessage({ message: '请输入正确的手机号', type: 'error' });
    return;
  }
  sending.value = true;
  try {
    const res = await verifycode({ cellPhone: fromData.cellPhone });
    if (res.code !== 200) {
      ElMessage({ message: res.msg || '验证码发送失败', type: 'error' });
      return;
    }
    ElMessage({ message: '验证码发送成功', type: 'success' });
    countdown.value = 60;
    countdownTimer = setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0) {
        clearInterval(countdownTimer);
        countdownTimer = undefined;
      }
    }, 1000);
  } catch (error) {
    ElMessage({ message: error?.message || '验证码发送失败', type: 'error' });
  } finally {
    sending.value = false;
  }
};
// 数据提交
const submitForm = (formEl) => {
  if (!formEl) return;
  formEl.validate(async (valid) => {
    if (!valid || submitting.value) return;
    submitting.value = true;
    try {
      const res = await phoneLogins({
        cellPhone: fromData.cellPhone,
        password: fromData.code,
        rememberMe: fromData.rememberMe,
      });
      if (res.code !== 200) {
        ElMessage({ message: res.msg || '登录失败', type: 'error' });
        return;
      }
      await store.setToken(res.data);
      const userResponse = await getUserInfo();
      if (userResponse.code !== 200) {
        ElMessage({ message: userResponse.msg || '获取用户信息失败', type: 'error' });
        return;
      }
      await store.setUserInfo(userResponse.data);
      await router.push('/main/index');
    } catch (error) {
      ElMessage({ message: error?.message || '登录失败', type: 'error' });
    } finally {
      submitting.value = false;
    }
  });
};

// 去注册
const goRegister = () => {
  emit('goHandle', 'register')
}
onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});
</script>
<style lang="scss" scoped>
.loginPhone {
    margin-top: 40px;
    .code-row {
      display: flex;
      gap: 10px;
      width: 100%;
    }
    .code-button {
      flex: 0 0 96px;
      padding: 0 8px;
      border: 0;
    }
    .login-button {
      width: 100%;
      border: 0;
    }
}
</style>

