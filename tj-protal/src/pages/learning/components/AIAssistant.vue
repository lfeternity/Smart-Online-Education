<template>
  <section :class="['ai-assistant', { global: mode === 'global' }]">
    <header class="ai-head">
      <div><strong>AI 助教</strong><small>{{ mode === 'global' ? '学习规划与课程检索' : '基于当前课程上下文' }}</small></div>
      <div class="head-actions">
        <el-tooltip content="新建对话" placement="bottom"><button class="icon-button" type="button" aria-label="新建对话" @click="newConversation"><Plus /></button></el-tooltip>
        <el-tooltip content="对话记录" placement="bottom"><button :class="['icon-button', { active: historyOpen }]" type="button" aria-label="对话记录" @click="toggleHistory"><Clock /></button></el-tooltip>
        <el-tooltip v-if="streaming" content="停止生成" placement="bottom"><button class="icon-button stop-button" type="button" aria-label="停止生成" @click="stop"><VideoPause /></button></el-tooltip>
      </div>
    </header>
    <aside v-if="historyOpen" class="conversation-history">
      <div class="history-head"><strong>对话记录</strong><button class="icon-button" type="button" aria-label="关闭对话记录" @click="historyOpen = false"><Close /></button></div>
      <div class="history-list">
        <div v-if="historyLoading" class="history-state">正在加载...</div>
        <div v-else-if="!conversations.length" class="history-state">暂无对话记录</div>
        <div v-for="item in conversations" :key="item.id" :class="['history-item', { current: item.id === conversationId }]">
          <button class="history-select" type="button" @click="selectConversation(item)"><span>{{ item.title || '新的学习对话' }}</span><small>{{ formatTime(item.updateTime || item.createTime) }}</small></button>
          <el-tooltip content="删除对话" placement="left"><button class="history-delete" type="button" aria-label="删除对话" @click="removeConversation(item)"><Delete /></button></el-tooltip>
        </div>
      </div>
    </aside>
    <div ref="listRef" class="ai-messages">
      <div v-if="!privacyAccepted" class="privacy-notice"><strong>使用前请确认</strong><p>你的提问和 AI 回答会保存到个人会话，用于连续对话和反馈处理。请勿提交密码、支付信息等敏感数据。</p><div><button type="button" @click="acceptPrivacy">同意并继续</button></div></div>
      <div v-else-if="messagesLoading" class="ai-empty">正在加载对话...</div>
      <div v-else-if="!messages.length" class="ai-empty">{{ mode === 'global' ? '可以问我课程推荐、学习规划，或先选择要讨论的课程。' : '可以问我课程内容、学习进度或本周学习安排。' }}</div>
      <article v-for="item in messages" :key="item.id" :class="['ai-message', item.role]">
        <div class="bubble" v-html="renderText(item.content)"></div>
        <div v-if="item.citations?.length" class="citations"><button v-for="citation in item.citations" :key="citation.chunkId" type="button" @click="jumpCitation(citation)">[{{ citation.index }}] {{ citation.title }}</button></div>
        <div v-if="item.action" class="action-box"><div>{{ item.action.summary }}</div><div class="action-actions"><button type="button" @click="confirm(item.action)">确认执行</button><button type="button" @click="cancel(item.action)">取消</button></div></div>
        <div v-if="item.role === 'assistant' && item.done" class="feedback"><button type="button" @click="regenerate(item)">重新生成</button><button type="button" @click="feedback(item, 'UP')">有帮助</button><button type="button" @click="feedback(item, 'DOWN')">需改进</button></div>
      </article>
      <div v-if="error" class="ai-error">{{ error }} <button v-if="lastQuestion" type="button" @click="retry">重试</button><button type="button" @click="error = ''">关闭</button></div>
    </div>
    <form v-if="privacyAccepted" class="ai-composer" @submit.prevent="send"><textarea v-model="draft" :disabled="streaming" maxlength="4000" rows="2" :placeholder="mode === 'global' ? '输入课程或学习规划问题' : '输入你想了解的课程问题'"></textarea><button type="submit" :disabled="!draft.trim() || streaming">发送</button></form>
  </section>
</template>
<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Clock, Close, Delete, Plus, VideoPause } from "@element-plus/icons-vue";
import { cancelAgentAction, confirmAgentAction, createConversation, deleteConversation, getConversationMessages, listConversations, streamAgentMessage, submitAgentFeedback } from "@/api/agent.js";
const props = defineProps({ context: { type: Object, default: () => ({}) }, mode: { type: String, default: "learning" } });
const messages = ref([]); const draft = ref(""); const streaming = ref(false); const error = ref(""); const listRef = ref(null); const lastQuestion = ref("");
const conversations = ref([]); const historyOpen = ref(false); const historyLoading = ref(false); const messagesLoading = ref(false);
const privacyAccepted = ref(localStorage.getItem("agentPrivacyAccepted") === "true");
const storageKey = () => props.mode === "global" ? "agentConversationId:global" : `agentConversationId:course:${props.context?.courseId || "unknown"}`;
const expectedScene = () => props.mode === "global" ? "GLOBAL_ASSISTANT" : "LEARNING_ASSISTANT";
const conversationId = ref(sessionStorage.getItem(storageKey()) || ""); let cancelStream = null; let loadVersion = 0;
const renderText = value => String(value || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/\n/g, "<br>");
const scrollBottom = () => nextTick(() => { if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight; });
const responseData = response => response?.data;
async function ensureConversation() { if (conversationId.value) return conversationId.value; const response = await createConversation({ scene: expectedScene(), title: props.mode === "global" ? "全局 AI 学习助理" : "学习页 AI 助教" }); conversationId.value = responseData(response)?.id; if (!conversationId.value) throw new Error("AI 会话创建失败"); sessionStorage.setItem(storageKey(), conversationId.value); refreshConversations(true); return conversationId.value; }
async function refreshConversations(silent = false) {
  historyLoading.value = true;
  try { const response = await listConversations(); conversations.value = (responseData(response) || []).filter(item => item.scene === expectedScene()); return true; }
  catch { if (!silent) ElMessage.warning("对话记录加载失败"); return false; }
  finally { historyLoading.value = false; }
}
async function initialize() {
  if (!privacyAccepted.value) return;
  if (!await refreshConversations(true)) return;
  const storedId = sessionStorage.getItem(storageKey()) || "";
  if (storedId && conversations.value.some(item => item.id === storedId)) await selectConversation({ id: storedId }, false);
  else { conversationId.value = ""; sessionStorage.removeItem(storageKey()); }
}
async function toggleHistory() { historyOpen.value = !historyOpen.value; if (historyOpen.value) await refreshConversations(); }
async function selectConversation(item, closeHistory = true) {
  if (!item?.id || messagesLoading.value) return;
  stop(); const version = ++loadVersion; messagesLoading.value = true; error.value = "";
  try {
    const response = await getConversationMessages(item.id); if (version !== loadVersion) return;
    messages.value = (responseData(response) || []).map((message, messageIndex) => ({
      id: message.id || `history-${messageIndex}`, serverId: message.id, role: String(message.role || "assistant").toLowerCase(),
      content: message.content || "", done: String(message.role).toUpperCase() === "ASSISTANT",
      citations: (message.citations || []).map((citation, citationIndex) => ({ ...citation, index: citation.index || citationIndex + 1 }))
    }));
    conversationId.value = item.id; sessionStorage.setItem(storageKey(), item.id); if (closeHistory) historyOpen.value = false; scrollBottom();
  } catch { if (version === loadVersion) ElMessage.error("对话内容加载失败"); }
  finally { if (version === loadVersion) messagesLoading.value = false; }
}
function newConversation() { stop(); loadVersion++; messagesLoading.value = false; messages.value = []; conversationId.value = ""; sessionStorage.removeItem(storageKey()); historyOpen.value = false; error.value = ""; }
async function removeConversation(item) {
  try { await ElMessageBox.confirm("删除后将无法恢复这段对话。", "删除对话", { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" }); }
  catch { return; }
  try { await deleteConversation(item.id); if (item.id === conversationId.value) newConversation(); await refreshConversations(true); ElMessage.success("对话已删除"); }
  catch { ElMessage.error("对话删除失败"); }
}
function formatTime(value) { const date = new Date(value); if (Number.isNaN(date.getTime())) return ""; const now = new Date(); if (date.toDateString() === now.toDateString()) return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }); return date.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" }); }
async function send() { const text = draft.value.trim(); draft.value = ""; await sendText(text); }
async function sendText(text) {
  if (!text || streaming.value || !privacyAccepted.value) return; error.value = ""; lastQuestion.value = text;
  const user = { id: `u-${Date.now()}`, role: "user", content: text }; const assistant = { id: `a-${Date.now()}`, role: "assistant", content: "", citations: [] }; messages.value.push(user, assistant); streaming.value = true; scrollBottom();
  try { const id = await ensureConversation(); cancelStream = streamAgentMessage(id, { message: text, context: props.context }, {
    content_delta: data => { assistant.content += data.delta || ""; scrollBottom(); }, content_replace: data => { assistant.content = data.content || ""; scrollBottom(); }, citation: data => assistant.citations.push(data),
    metadata: data => { assistant.serverId = data.messageId; },
    tool_confirmation_required: data => { assistant.action = { id: data.actionId, summary: data.summary, type: data.actionType }; }, completed: () => { assistant.done = true; },
    error: data => { error.value = data?.message || "AI 助教暂时不可用"; }, end: () => { streaming.value = false; assistant.done = true; cancelStream = null; refreshConversations(true); scrollBottom(); },
  }); } catch (e) { error.value = e.message; streaming.value = false; }
}
function acceptPrivacy() { localStorage.setItem("agentPrivacyAccepted", "true"); privacyAccepted.value = true; initialize(); }
function retry() { const text = lastQuestion.value; error.value = ""; sendText(text); }
function regenerate(item) { if (streaming.value) return; const index = messages.value.indexOf(item); const previous = messages.value.slice(0, index).reverse().find(value => value.role === "user"); if (previous) sendText(previous.content); }
function stop() { cancelStream?.(); cancelStream = null; streaming.value = false; }
async function confirm(action) { try { await confirmAgentAction(action.id); action.status = "CONFIRMED"; ElMessage.success("操作已执行"); } catch { ElMessage.error("确认失败，请重试"); } }
async function cancel(action) { try { await cancelAgentAction(action.id); action.status = "CANCELLED"; } catch { ElMessage.error("取消失败，请重试"); } }
function jumpCitation(citation) { if (citation.sectionId) window.dispatchEvent(new CustomEvent("agent-jump-section", { detail: citation })); }
async function feedback(item, rating) { item.feedback = rating; if (!item.serverId) return; try { await submitAgentFeedback(item.serverId, { rating }); } catch { ElMessage.warning("反馈暂时未保存"); } }
onMounted(initialize);
onBeforeUnmount(() => cancelStream?.());
watch(() => props.context?.courseId, () => {
  if (props.mode !== "learning") return;
  stop(); loadVersion++; messagesLoading.value = false; messages.value = []; error.value = ""; historyOpen.value = false;
  conversationId.value = sessionStorage.getItem(storageKey()) || ""; initialize();
});
</script>
<style scoped lang="scss">
.ai-assistant { position: relative; height: 100%; display: flex; flex-direction: column; overflow: hidden; color: #d9e2ec; }.ai-head { min-height: 49px; display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 4px 0 9px; border-bottom: 1px solid #3a434c; }.ai-head strong { display: block; font-size: 16px; }.ai-head small { display: block; color: #8795a3; font-size: 11px; }.head-actions { display: flex; flex: 0 0 auto; gap: 3px; }.icon-button { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; border-radius: 4px; }.icon-button svg { width: 16px; height: 16px; }.icon-button:hover, .icon-button.active { color: #74b8ff; background: #2a3540; }.stop-button { color: #ffb86c; }button { border: 0; cursor: pointer; color: inherit; background: transparent; }.conversation-history { position: absolute; z-index: 5; inset: 53px 0 0; display: flex; flex-direction: column; background: #1b2127; }.history-head { min-height: 44px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #343e47; }.history-head strong { font-size: 14px; }.history-list { flex: 1; min-height: 0; overflow: auto; padding: 8px 0; }.history-state { padding: 28px 8px; color: #8795a3; text-align: center; font-size: 12px; }.history-item { min-height: 56px; display: flex; align-items: center; gap: 3px; padding: 4px; border-radius: 5px; }.history-item:hover, .history-item.current { background: #29333d; }.history-select { flex: 1; min-width: 0; padding: 5px 7px; text-align: left; }.history-select span, .history-select small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.history-select span { font-size: 13px; }.history-select small { margin-top: 4px; color: #8795a3; font-size: 11px; }.history-delete { width: 30px; height: 30px; flex: 0 0 30px; display: inline-flex; align-items: center; justify-content: center; color: #8795a3; border-radius: 4px; }.history-delete:hover { color: #f56c6c; background: #3b2a2c; }.history-delete svg { width: 15px; height: 15px; }.ai-messages { flex: 1; min-height: 0; overflow: auto; padding: 14px 2px; }.ai-empty { color: #8f9ca9; line-height: 1.7; font-size: 13px; }.ai-message { margin-bottom: 14px; }.ai-message.user { text-align: right; }.bubble { display: inline-block; max-width: 92%; padding: 9px 11px; border-radius: 5px; background: #29333d; text-align: left; line-height: 1.6; font-size: 13px; word-break: break-word; }.user .bubble { background: #1766b3; }.citations { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 6px; }.citations button { color: #74b8ff; font-size: 11px; text-align: left; }.action-box { margin-top: 8px; padding: 9px; border-left: 3px solid #e6a23c; background: #353027; font-size: 12px; }.action-actions { display: flex; gap: 12px; margin-top: 8px; }.action-actions button { color: #74b8ff; }.feedback { margin-top: 5px; color: #7f8b98; font-size: 11px; display: flex; gap: 9px; }.ai-error { color: #f56c6c; font-size: 12px; }.ai-composer { display: flex; gap: 7px; padding-top: 9px; border-top: 1px solid #3a434c; }.ai-composer textarea { flex: 1; resize: none; border: 1px solid #46515c; border-radius: 4px; padding: 7px; color: #e6edf3; background: #202830; font: inherit; outline: none; }.ai-composer button { align-self: flex-end; padding: 7px 12px; border-radius: 4px; background: #2080f7; color: white; }.ai-composer button:disabled { opacity: .45; cursor: not-allowed; }
.privacy-notice { padding: 12px; border-left: 3px solid #2080f7; color: #b7c2cc; font-size: 12px; line-height: 1.6; }.privacy-notice p { margin: 6px 0 10px; }.privacy-notice button { color: #74b8ff; }.global { color: #25313b; }.global .ai-head { border-color: #dce2e8; }.global .ai-head small, .global .ai-empty, .global .history-state, .global .history-select small { color: #687682; }.global .icon-button:hover, .global .icon-button.active { background: #eef1f4; }.global .conversation-history { background: white; }.global .history-head { border-color: #dce2e8; }.global .history-item:hover, .global .history-item.current { background: #eef1f4; }.global .history-delete:hover { background: #fff0f0; }.global .bubble { color: #25313b; background: #eef1f4; }.global .user .bubble { color: white; background: #1766b3; }.global .ai-composer { border-color: #dce2e8; }.global .ai-composer textarea { color: #25313b; background: white; border-color: #cbd3da; }.global .action-box { background: #fff7e8; }.global .privacy-notice { color: #53616c; }
</style>
