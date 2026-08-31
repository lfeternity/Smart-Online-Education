<!-- 问答详情 - 同课程相关问题 -->
<template>
  <aside class="relatedQuestions">
    <div class="but">
      <span class="bt bt-round" @click="publishQuestion">发新问题</span>
    </div>
    <div class="tit">相关问题</div>
    <div v-if="questions.length">
      <p v-for="question in questions" :key="question.id" @click="openQuestion(question.id)">
        {{ question.title }}
      </p>
    </div>
    <div v-else-if="!loading" class="empty">暂无相关问题</div>
  </aside>
</template>

<script setup>
import { ref, watch } from "vue";
import { useRouter } from "vue-router";
import { getAskList } from "@/api/classDetails.js";

const props = defineProps({
  id: {
    type: [String, Number],
    default: "",
  },
  currentId: {
    type: [String, Number],
    default: "",
  },
  title: {
    type: String,
    default: "",
  },
});

const router = useRouter();
const questions = ref([]);
const loading = ref(false);

watch(
  () => [props.id, props.currentId],
  ([courseId]) => loadQuestions(courseId),
  { immediate: true }
);

async function loadQuestions(courseId) {
  if (!courseId) {
    questions.value = [];
    return;
  }
  loading.value = true;
  try {
    const response = await getAskList({ courseId, pageNo: 1, pageSize: 5 });
    if (response.code === 200) {
      questions.value = (response.data?.list || [])
        .filter(question => String(question.id) !== String(props.currentId))
        .slice(0, 4);
    } else {
      questions.value = [];
    }
  } catch (error) {
    questions.value = [];
  } finally {
    loading.value = false;
  }
}

function publishQuestion() {
  router.push({ path: "/ask", query: { id: props.id, title: props.title } });
}

function openQuestion(id) {
  router.push({
    path: "/askDetails",
    query: { id, detailsId: props.id, name: props.title },
  });
}
</script>

<style lang="scss" scoped>
.relatedQuestions {
  width: 344px;
  height: fit-content;
  padding: 30px;
  border-radius: 8px;
  background: #ffffff;
  font-size: 14px;
  line-height: 24px;

  .but {
    margin-bottom: 30px;
    padding: 0 50px 30px;
    border-bottom: 1px solid var(--color-background1);
  }

  p {
    margin: 20px 0;
    cursor: pointer;

    &:hover {
      color: var(--color-main);
    }
  }

  .tit {
    font-size: 20px;
    font-weight: 600;
  }

  .empty {
    padding: 30px 0 10px;
    color: var(--color-font2);
    text-align: center;
  }
}
</style>
