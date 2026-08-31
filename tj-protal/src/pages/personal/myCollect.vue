<!-- 个人中心 - 我的收藏 -->
<template>
  <div class="personalCards myCollect" v-loading="loading">
    <CardsTitle title="我的收藏">
      <span class="count">共 {{ courses.length }} 门课程</span>
    </CardsTitle>

    <div v-if="courses.length" class="courseGrid">
      <article
        v-for="course in courses"
        :key="course.id"
        class="courseCard"
        @click="goDetails(course.id)"
      >
        <div class="cover">
          <img :src="course.coverUrl" :alt="course.name" />
          <el-tooltip content="取消收藏" placement="top">
            <el-button
              class="uncollect"
              circle
              :loading="removingId === course.id"
              aria-label="取消收藏"
              @click.stop="removeCollection(course)"
            >
              <el-icon v-if="removingId !== course.id"><StarFilled /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        <div class="info">
          <h3>{{ course.name }}</h3>
          <p>共 {{ course.sectionNum || 0 }} 节</p>
          <strong v-if="Number(course.price)">￥{{ (Number(course.price) / 100).toFixed(2) }}</strong>
          <strong v-else>免费</strong>
        </div>
      </article>
    </div>
    <div v-else-if="!loading" class="emptyState">
      <Empty desc="还没有收藏课程" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { StarFilled } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { getLikedBizIds, putLiked } from "@/api/classDetails.js";
import { getCourseSimpleInfoList } from "@/api/class.js";
import Empty from "@/components/Empty.vue";
import CardsTitle from "./components/CardsTitle.vue";

const router = useRouter();
const courses = ref([]);
const loading = ref(false);
const removingId = ref(null);

onMounted(loadCollections);

async function loadCollections() {
  loading.value = true;
  try {
    const likedResponse = await getLikedBizIds("COURSE");
    if (likedResponse.code !== 200) {
      throw new Error(likedResponse.msg || "收藏列表加载失败");
    }
    const ids = likedResponse.data || [];
    if (!ids.length) {
      courses.value = [];
      return;
    }
    const courseResponse = await getCourseSimpleInfoList(ids);
    if (courseResponse.code !== 200) {
      throw new Error(courseResponse.msg || "课程信息加载失败");
    }
    courses.value = courseResponse.data || [];
  } catch (error) {
    ElMessage.error(error.message || "收藏列表请求出错");
  } finally {
    loading.value = false;
  }
}

async function removeCollection(course) {
  removingId.value = course.id;
  try {
    const response = await putLiked({
      bizId: course.id,
      bizType: "COURSE",
      liked: false,
    });
    if (response.code !== 200) {
      throw new Error(response.msg || "取消收藏失败");
    }
    courses.value = courses.value.filter(item => item.id !== course.id);
    ElMessage.success("已取消收藏");
  } catch (error) {
    ElMessage.error(error.message || "取消收藏请求出错");
  } finally {
    removingId.value = null;
  }
}

function goDetails(id) {
  router.push({ path: "/details", query: { id } });
}
</script>

<style lang="scss" scoped>
.myCollect {
  min-height: 520px;
}

.count {
  color: var(--color-font2);
  font-size: 14px;
  font-weight: 400;
}

.courseGrid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.courseCard {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #eeeeee;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(25, 35, 43, 0.12);
    transform: translateY(-2px);
  }
}

.cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f5f6f7;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.uncollect {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 34px;
  height: 34px;
  border: 0;
  color: var(--color-main);
  box-shadow: 0 2px 8px rgba(25, 35, 43, 0.18);
}

.info {
  padding: 14px 16px 16px;

  h3 {
    min-height: 44px;
    overflow: hidden;
    margin: 0 0 8px;
    color: #19232b;
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
  }

  p {
    margin: 0 0 8px;
    color: var(--color-font2);
    font-size: 12px;
  }

  strong {
    color: var(--color-main);
    font-size: 16px;
    font-weight: 500;
  }
}

.emptyState {
  height: 410px;
}

@media (max-width: 900px) {
  .courseGrid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
