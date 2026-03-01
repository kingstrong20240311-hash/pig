<template>
	<el-dialog
		:close-on-click-modal="false"
		:title="form.id ? $t('common.editBtn') : $t('common.addBtn')"
		width="700"
		draggable
		v-model="visible"
	>
		<el-form :model="form" :rules="dataRules" label-width="130px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.deptId')" prop="deptId">
						<el-tree-select
							:data="deptData"
							:props="{ value: 'id', label: 'name', children: 'children' }"
							placeholder="请选择门店/场馆"
							check-strictly
							clearable
							class="w100"
							v-model="form.deptId"
							@change="onDeptChange"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.coachId')" prop="coachId">
						<el-select
							v-model="form.coachId"
							placeholder="请选择教练"
							clearable
							filterable
							class="w100"
							:disabled="!form.deptId"
							:loading="coachLoading"
						>
							<el-option
								v-for="item in coachOptions"
								:key="item.userId"
								:label="item.name"
								:value="item.userId"
							/>
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.name')" prop="name">
						<el-input
							:placeholder="t('member.inputNameTip')"
							v-model="form.name"
							maxlength="50"
							show-word-limit
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.mobile')" prop="mobile">
						<el-input
							:placeholder="t('member.inputMobileTip')"
							v-model="form.mobile"
							maxlength="20"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.gender')" prop="gender">
						<el-radio-group v-model="form.gender">
							<el-radio value="男">男</el-radio>
							<el-radio value="女">女</el-radio>
						</el-radio-group>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.birthday')" prop="birthday">
						<el-date-picker
							:placeholder="t('member.inputBirthdayTip')"
							v-model="form.birthday"
							type="date"
							value-format="YYYY-MM-DD"
							style="width: 100%"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.heightCm')" prop="heightCm">
						<el-input
							:placeholder="t('member.inputHeightCmTip')"
							v-model.number="form.heightCm"
							type="number"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.weightKg')" prop="weightKg">
						<el-input
							:placeholder="t('member.inputWeightKgTip')"
							v-model.number="form.weightKg"
							type="number"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.fmsScore')" prop="fmsScore">
						<el-input
							:placeholder="t('member.inputFmsScoreTip')"
							v-model.number="form.fmsScore"
							type="number"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.lastFmsScore')" prop="lastFmsScore">
						<el-input
							:placeholder="t('member.inputLastFmsScoreTip')"
							v-model.number="form.lastFmsScore"
							type="number"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('member.enabled')" prop="enabled">
						<el-switch v-model="form.enabled" :active-text="t('member.enabledActive')" :inactive-text="t('member.enabledInactive')" />
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('member.avatarUrl')" prop="avatarUrl">
						<el-input
							:placeholder="t('member.avatarUrl')"
							v-model="form.avatarUrl"
							maxlength="500"
							show-word-limit
						/>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('member.injuryHistory')" prop="injuryHistory">
						<el-input
							:placeholder="t('member.inputInjuryHistoryTip')"
							v-model="form.injuryHistory"
							type="textarea"
							:rows="2"
							maxlength="1000"
							show-word-limit
						/>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('member.medicalNotes')" prop="medicalNotes">
						<el-input
							:placeholder="t('member.inputMedicalNotesTip')"
							v-model="form.medicalNotes"
							type="textarea"
							:rows="2"
							maxlength="1000"
							show-word-limit
						/>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('member.goalNotes')" prop="goalNotes">
						<el-input
							:placeholder="t('member.inputGoalNotesTip')"
							v-model="form.goalNotes"
							type="textarea"
							:rows="2"
							maxlength="500"
							show-word-limit
						/>
					</el-form-item>
				</el-col>
			</el-row>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">{{ $t('common.cancelButtonText') }}</el-button>
				<el-button @click="onSubmit" type="primary" :disabled="loading">{{ $t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="MemberDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj } from '/@/api/admin/member';
import { deptTree } from '/@/api/admin/dept';
import { getObjDetails as getPostDetails } from '/@/api/admin/post';
import { pageList as userPageList } from '/@/api/admin/user';
import { useI18n } from 'vue-i18n';

const emit = defineEmits(['refresh']);

const { t } = useI18n();

// 定义变量内容
const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);
const deptData = ref<any[]>([]);
const coachOptions = ref<any[]>([]);
const coachLoading = ref(false);
const coachPostId = ref<number | null>(null);

// 提交表单数据
const form = reactive({
	id: '',
	deptId: null as number | null,
	coachId: null as number | null,
	name: '',
	avatarUrl: '',
	mobile: '',
	gender: '',
	birthday: '',
	heightCm: null as number | null,
	weightKg: null as number | null,
	injuryHistory: '',
	medicalNotes: '',
	goalNotes: '',
	enabled: true,
	fmsScore: null as number | null,
	lastFmsScore: null as number | null,
});

// 定义校验规则
const dataRules = reactive({
	deptId: [{ required: true, message: '门店/场馆不能为空', trigger: 'change' }],
	coachId: [{ required: true, message: '教练不能为空', trigger: 'change' }],
	name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
	mobile: [{ required: true, message: '手机号不能为空', trigger: 'blur' }],
	gender: [{ required: true, message: '性别不能为空', trigger: 'change' }],
});

// 打开弹窗
const openDialog = async (id: string) => {
	visible.value = true;
	form.id = '';
	coachOptions.value = [];

	// 重置表单数据
	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	await getDeptData();
	await getCoachPostId();

	// 获取会员信息
	if (id) {
		form.id = id;
		await getMemberData(id);
		await loadCoachesByDept(form.deptId);
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
const getMemberData = async (id: string) => {
	const { data } = await getObj(id);
	Object.assign(form, data);
};

// 初始化部门
const getDeptData = async () => {
	const { data } = await deptTree();
	deptData.value = data || [];
};

// 获取教练岗位ID（postCode=COACH）
const getCoachPostId = async () => {
	if (coachPostId.value) {
		return coachPostId.value;
	}
	const { data } = await getPostDetails({ postCode: 'COACH' });
	coachPostId.value = data?.postId ?? null;
	return coachPostId.value;
};

// 根据场馆加载教练
const loadCoachesByDept = async (deptId: number | null) => {
	coachOptions.value = [];
	if (!deptId) {
		return;
	}

	const postId = await getCoachPostId();
	if (!postId) {
		return;
	}

	try {
		coachLoading.value = true;
		const { data } = await userPageList({ current: 1, size: 500, deptId });
		const records = data?.records ?? [];
		coachOptions.value = records
			.filter((user: any) => Array.isArray(user.postList) && user.postList.some((post: any) => post.postId === postId || post.postCode === 'COACH'))
			.map((user: any) => ({
				userId: user.userId,
				name: user.name || user.username,
			}));
	} finally {
		coachLoading.value = false;
	}
};

// 切换场馆后重置教练
const onDeptChange = async (value: number | null) => {
	form.coachId = null;
	await loadCoachesByDept(value);
};

// 暴露变量
defineExpose({
	openDialog,
});
</script>
