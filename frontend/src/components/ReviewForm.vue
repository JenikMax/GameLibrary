<template>
  <div class="review-form">
    <h4 class="mb-2">{{ review ? t('review.edit') : t('review.write') }}</h4>

    <div class="field">
      <label>{{ t('review.gameplay') }}</label>
      <div class="flex align-items-center gap-2">
        <Rating v-model="form.gameplayScore" :stars="10" :cancel="true" />
        <span v-if="form.gameplayScore" class="text-sm font-semibold">{{ form.gameplayScore }}/10</span>
      </div>
    </div>

    <div class="field">
      <label>{{ t('review.graphics') }}</label>
      <div class="flex align-items-center gap-2">
        <Rating v-model="form.graphicsScore" :stars="10" :cancel="true" />
        <span v-if="form.graphicsScore" class="text-sm font-semibold">{{ form.graphicsScore }}/10</span>
      </div>
    </div>

    <div class="field">
      <label>{{ t('review.story') }}</label>
      <div class="flex align-items-center gap-2">
        <Rating v-model="form.storyScore" :stars="10" :cancel="true" />
        <span v-if="form.storyScore" class="text-sm font-semibold">{{ form.storyScore }}/10</span>
      </div>
    </div>

    <div class="field">
      <label>{{ t('review.music') }}</label>
      <div class="flex align-items-center gap-2">
        <Rating v-model="form.musicScore" :stars="10" :cancel="true" />
        <span v-if="form.musicScore" class="text-sm font-semibold">{{ form.musicScore }}/10</span>
      </div>
    </div>

    <div class="field">
      <label for="review-text">{{ t('review.text') }}</label>
      <Textarea
        id="review-text"
        v-model="form.text"
        :placeholder="t('review.text_placeholder')"
        rows="4"
        class="w-full"
        autoResize
      />
    </div>

    <div class="field">
      <label for="review-pros">{{ t('review.pros') }}</label>
      <Textarea
        id="review-pros"
        v-model="form.pros"
        :placeholder="t('review.pros_placeholder')"
        rows="2"
        class="w-full"
        autoResize
      />
    </div>

    <div class="field">
      <label for="review-cons">{{ t('review.cons') }}</label>
      <Textarea
        id="review-cons"
        v-model="form.cons"
        :placeholder="t('review.cons_placeholder')"
        rows="2"
        class="w-full"
        autoResize
      />
    </div>

    <div class="flex gap-2 justify-content-end">
      <Button v-if="review" :label="t('common.cancel')" severity="secondary" text @click="$emit('cancel')" />
      <Button :label="review ? t('common.save') : t('review.submit')" @click="handleSubmit" :loading="submitting" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useI18n } from '../composables/useI18n'
import Rating from 'primevue/rating'
import Textarea from 'primevue/textarea'
import Button from 'primevue/button'

const { t } = useI18n()

const props = defineProps({
  review: { type: Object, default: null }
})

const emit = defineEmits(['submit', 'cancel'])

const submitting = ref(false)
const form = reactive({
  text: props.review?.text || '',
  pros: props.review?.pros || '',
  cons: props.review?.cons || '',
  gameplayScore: props.review?.gameplayScore || 0,
  graphicsScore: props.review?.graphicsScore || 0,
  storyScore: props.review?.storyScore || 0,
  musicScore: props.review?.musicScore || 0
})

function handleSubmit() {
  submitting.value = true
  emit('submit', {
    text: form.text || '',
    pros: form.pros || '',
    cons: form.cons || '',
    gameplayScore: form.gameplayScore || null,
    graphicsScore: form.graphicsScore || null,
    storyScore: form.storyScore || null,
    musicScore: form.musicScore || null
  })
  setTimeout(() => { submitting.value = false }, 2000)
}
</script>

<style scoped>
.review-form {
  background: var(--p-surface-100);
  border-radius: 8px;
  padding: 1rem;
}
.app-dark .review-form {
  background: var(--p-surface-800);
}
.review-form .field {
  margin-bottom: 0.75rem;
}
.review-form label {
  display: block;
  font-weight: 600;
  font-size: 0.85rem;
  margin-bottom: 0.25rem;
}
</style>
