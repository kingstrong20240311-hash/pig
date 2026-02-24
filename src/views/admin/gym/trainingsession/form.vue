<template>
	<el-dialog
		:close-on-click-modal="false"
		:title="form.id ? $t('common.editBtn') : $t('common.addBtn')"
		width="600"
		draggable
		v-model="visible"
	>
		<el-form :model="form" :rules="dataRules" label-width="120px" ref="dataFormRef" v-loading="loading">
			<el-form-item :label="t('trainingsession.memberId')" prop="memberId">
				<el-input :placeholder="t('trainingsession.inputMemberIdTip')" v-model.number="form.memberId" type="number" />
			</el-form-item>
			<el-form-item :label="t('trainingsession.coachId')" prop="coachId">
				<el-input :placeholder="t('trainingsession.inputCoachIdTip')" v-model.number="form.coachId" type="number" />
			</el-form-item>
			<el-form-item :label="t('trainingsession.lessonPlanId')" prop="lessonPlanId">
				<el-input
					:placeholder="t('trainingsession.inputLessonPlanIdTip')"
					v-model.number="form.lessonPlanId"
					type="number"
				/>
			</el-form-item>
			<el-form-item :label="t('trainingsession.scheduledAt')" prop="scheduledAt">
				<el-date-picker
					v-model="form.scheduledAt"
					type="datetime"
					:placeholder="t('trainingsession.inputScheduledAtTip')"
					style="width: 100%"
					value-format="YYYY-MM-DD HH:mm:ss"
				/>
			</el-form-item>
			<el-form-item :label="t('trainingsession.completedAt')" prop="completedAt">
				<el-date-picker
					v-model="form.completedAt"
					type="datetime"
					:placeholder="t('trainingsession.inputCompletedAtTip')"
					style="width: 100%"
					value-format="YYYY-MM-DD HH:mm:ss"
				/>
			</el-form-item>
			<el-form-item :label="t('trainingsession.status')" prop="status">
				<el-select :placeholder="t('trainingsession.inputStatusTip')" v-model="form.status" style="width: 100%">
					<el-option label="已预约" value="SCHEDULED"></el-option>
					<el-option label="已完成" value="COMPLETED"></el-option>
					<el-option label="已取消" value="CANCELED"></el-option>
				</el-select>
			</el-form-item>
			<el-form-item :label="t('trainingsession.cancelReason')" prop="cancelReason">
				<el-input
					:placeholder="t('trainingsession.inputCancelReasonTip')"
					v-model="form.cancelReason"
					type="textarea"
					:rows="3"
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

<script lang="ts" name="TrainingSessionDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj } from '/@/api/admin/trainingsession';
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
	memberId: null as number | null,
	coachId: null as number | null,
	lessonPlanId: null as number | null,
	scheduledAt: '',
	completedAt: '',
	status: 'SCHEDULED',
	cancelReason: '',
});

// 定义校验规则
const dataRules = reactive({
	memberId: [{ required: true, message: '会员ID不能为空', trigger: 'blur' }],
	coachId: [{ required: true, message: '教练ID不能为空', trigger: 'blur' }],
	scheduledAt: [{ required: true, message: '预约时间不能为空', trigger: 'blur' }],
	status: [{ required: true, message: '课程状态不能为空', trigger: 'blur' }],
});

// 打开弹窗
const openDialog = (id: string) => {
	visible.value = true;
	form.id = '';

	// 重置表单数据
	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	// 获取训练课程信息
	if (id) {
		form.id = id;
		getTrainingSessionData(id);
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
const getTrainingSessionData = (id: string) => {
	getObj(id).then((res: any) => {
		Object.assign(form, res.data);
	});
};

// 暴露变量
defineExpose({
	openDialog,
});
</script>
