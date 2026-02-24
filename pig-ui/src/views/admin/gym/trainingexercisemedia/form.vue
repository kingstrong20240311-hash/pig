<template>
	<el-dialog
		:close-on-click-modal="false"
		:title="form.id ? $t('common.editBtn') : $t('common.addBtn')"
		width="600"
		draggable
		v-model="visible"
	>
		<el-form :model="form" :rules="dataRules" label-width="120px" ref="dataFormRef" v-loading="loading">
			<el-form-item :label="t('trainingexercisemedia.sessionId')" prop="sessionId">
				<el-input
					:placeholder="t('trainingexercisemedia.inputSessionIdTip')"
					v-model.number="form.sessionId"
					type="number"
				/>
			</el-form-item>
			<el-form-item :label="t('trainingexercisemedia.exerciseRecordId')" prop="exerciseRecordId">
				<el-input
					:placeholder="t('trainingexercisemedia.inputExerciseRecordIdTip')"
					v-model.number="form.exerciseRecordId"
					type="number"
				/>
			</el-form-item>
			<el-form-item :label="t('trainingexercisemedia.detailUrl')" prop="detailUrl">
				<el-input
					:placeholder="t('trainingexercisemedia.inputDetailUrlTip')"
					v-model="form.detailUrl"
					maxlength="500"
					show-word-limit
				/>
			</el-form-item>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">{{ $t('common.cancelButtonText') }}</el-button>
				<el-button @click="onSubmit" type="primary" :disabled="loading">{{ $t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="TrainingExerciseMediaDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj } from '/@/api/admin/trainingexercisemedia';
import { useI18n } from 'vue-i18n';

const emit = defineEmits(['refresh']);

const { t } = useI18n();

// 定义变量内容
const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

// 提交表单数据
const form = reactive({
	id: '',
	sessionId: null as number | null,
	exerciseRecordId: null as number | null,
	detailUrl: '',
});

// 定义校验规则
const dataRules = reactive({
	sessionId: [{ required: true, message: '课程ID不能为空', trigger: 'blur' }],
	exerciseRecordId: [{ required: true, message: '动作记录ID不能为空', trigger: 'blur' }],
	detailUrl: [{ required: true, message: '素材链接不能为空', trigger: 'blur' }],
});

// 打开弹窗
const openDialog = (id: string) => {
	visible.value = true;
	form.id = '';

	// 重置表单数据
	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	// 获取训练动作素材信息
	if (id) {
		form.id = id;
		getTrainingExerciseMediaData(id);
	}
};

// 提交
const onSubmit = async () => {
	const valid = await dataFormRef.value.validate().catch(() => {});
	if (!valid) return false;

	try {
		loading.value = true;
		form.id ? await putObj(form) : await addObj(form);
		useMessage().success(t(form.id ? 'common.editSuccessText' : 'common.addSuccessText'));
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

// 初始化表单数据
const getTrainingExerciseMediaData = (id: string) => {
	getObj(id).then((res: any) => {
		Object.assign(form, res.data);
	});
};

// 暴露变量
defineExpose({
	openDialog,
});
</script>
