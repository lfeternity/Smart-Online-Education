<!-- 我的消息 -->
<template>
  <div class="messageWrapper content">
    <div class="messageHeader">
      <CardsTitle title="我的消息" />
      <el-button text type="primary" :disabled="loading || messages.length === 0" @click="readAll">
        全部标为已读
      </el-button>
    </div>

    <div class="messageFilters">
      <el-select v-model="params.type" clearable placeholder="全部类型" @change="changeFilter">
        <el-option v-for="item in messageTypes" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-checkbox v-model="unreadOnly" @change="changeFilter">只看未读</el-checkbox>
    </div>

    <div v-loading="loading" class="messageList">
      <article
        v-for="item in messages"
        :key="item.id"
        class="messageItem"
        :class="{ unread: !item.isRead }"
        role="button"
        tabindex="0"
        @click="readOne(item)"
        @keydown.enter="readOne(item)"
      >
        <span class="unreadDot" aria-hidden="true"></span>
        <span class="messageBody">
          <span class="messageMeta">
            <el-tag size="small" effect="plain">{{ typeLabel(item.type) }}</el-tag>
            <time>{{ formatTime(item.pushTime) }}</time>
          </span>
          <strong>{{ item.title || typeLabel(item.type) }}</strong>
          <span class="messageContent">{{ item.content }}</span>
        </span>
        <el-button text type="danger" @click.stop="removeOne(item)">删除</el-button>
      </article>

      <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
    </div>

    <div v-if="total > 0" class="pageination">
      <el-pagination
        v-model:current-page="params.pageNo"
        v-model:page-size="params.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="loadMessages"
        @current-change="loadMessages"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import moment from "moment"
import CardsTitle from "./components/CardsTitle.vue"
import { deleteMessage, getMessages, markAllMessagesRead, markMessageRead } from "@/api/message.js"

const messageTypes = [
  { value: 0, label: "系统通知" },
  { value: 1, label: "笔记通知" },
  { value: 2, label: "问答通知" },
  { value: 3, label: "其他通知" },
  { value: 4, label: "私信" }
]

const params = reactive({ pageNo: 1, pageSize: 10, type: undefined, isRead: undefined })
const unreadOnly = ref(false)
const loading = ref(false)
const total = ref(0)
const messages = ref([])

const typeLabel = type => messageTypes.find(item => item.value === type)?.label || "消息"
const formatTime = time => time ? moment(time).format("YYYY-MM-DD HH:mm") : ""

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await getMessages(params)
    if (res.code !== 200) {
      ElMessage.error(res.msg || "消息列表加载失败")
      return
    }
    messages.value = res.data.list || []
    total.value = Number(res.data.total || 0)
  } catch (error) {
    ElMessage.error("消息列表加载失败")
  } finally {
    loading.value = false
  }
}

const changeFilter = () => {
  params.pageNo = 1
  params.isRead = unreadOnly.value ? false : undefined
  loadMessages()
}

const readOne = async item => {
  if (item.isRead) return
  const res = await markMessageRead(item.id)
  if (res.code === 200) item.isRead = true
}

const readAll = async () => {
  const res = await markAllMessagesRead()
  if (res.code === 200) {
    messages.value.forEach(item => { item.isRead = true })
    if (unreadOnly.value) loadMessages()
    ElMessage.success("已全部标为已读")
  }
}

const removeOne = async item => {
  try {
    await ElMessageBox.confirm("删除后无法恢复，确认删除这条消息吗？", "删除消息", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning"
    })
    const res = await deleteMessage(item.id)
    if (res.code === 200) {
      ElMessage.success("消息已删除")
      loadMessages()
    }
  } catch (error) {
    // 用户取消删除时无需提示。
  }
}

onMounted(loadMessages)
</script>

<style lang="scss" src="./index.scss"></style>
<style lang="scss" scoped src="./style/myMessage.scss"></style>
