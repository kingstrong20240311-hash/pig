<template>
	<el-dialog
		:close-on-click-modal="false"
		:title="form.id ? $t('common.editBtn') : $t('common.addBtn')"
		width="860"
		draggable
		v-model="visible"
	>
		<el-form :model="form" :rules="dataRules" label-width="140px" ref="dataFormRef" v-loading="loading">
			<!-- 基本信息 -->
			<el-divider content-position="left">基本信息</el-divider>
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.memberId')" prop="memberId">
						<el-input :placeholder="t('inbodyTest.inputMemberIdTip')" v-model.number="form.memberId" type="number" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.coachId')" prop="coachId">
						<el-input :placeholder="t('inbodyTest.inputCoachIdTip')" v-model.number="form.coachId" type="number" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.testDate')" prop="testDate">
						<el-date-picker
							v-model="form.testDate"
							type="datetime"
							value-format="x"
							:placeholder="t('inbodyTest.testDate')"
							style="width: 100%"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.heightCm')" prop="heightCm">
						<el-input v-model.number="form.heightCm" type="number" :placeholder="t('inbodyTest.heightCm')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.weightKg')" prop="weightKg">
						<el-input :placeholder="t('inbodyTest.inputWeightKgTip')" v-model.number="form.weightKg" type="number" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.bmi')" prop="bmi">
						<el-input v-model.number="form.bmi" type="number" :placeholder="t('inbodyTest.bmi')" />
					</el-form-item>
				</el-col>
			</el-row>

			<!-- 体成分 -->
			<el-divider content-position="left">体成分</el-divider>
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.bodyFatPercentage')" prop="bodyFatPercentage">
						<el-input v-model.number="form.bodyFatPercentage" type="number" :placeholder="t('inbodyTest.bodyFatPercentage')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.bodyFatMassKg')" prop="bodyFatMassKg">
						<el-input v-model.number="form.bodyFatMassKg" type="number" :placeholder="t('inbodyTest.bodyFatMassKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.skeletalMuscleMassKg')" prop="skeletalMuscleMassKg">
						<el-input v-model.number="form.skeletalMuscleMassKg" type="number" :placeholder="t('inbodyTest.skeletalMuscleMassKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.leanBodyMassKg')" prop="leanBodyMassKg">
						<el-input v-model.number="form.leanBodyMassKg" type="number" :placeholder="t('inbodyTest.leanBodyMassKg')" />
					</el-form-item>
				</el-col>
			</el-row>

			<!-- 水分分析 -->
			<el-divider content-position="left">水分分析</el-divider>
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.totalBodyWaterKg')" prop="totalBodyWaterKg">
						<el-input v-model.number="form.totalBodyWaterKg" type="number" :placeholder="t('inbodyTest.totalBodyWaterKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.intracellularWaterKg')" prop="intracellularWaterKg">
						<el-input v-model.number="form.intracellularWaterKg" type="number" :placeholder="t('inbodyTest.intracellularWaterKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.extracellularWaterKg')" prop="extracellularWaterKg">
						<el-input v-model.number="form.extracellularWaterKg" type="number" :placeholder="t('inbodyTest.extracellularWaterKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.ecwTbwRatio')" prop="ecwTbwRatio">
						<el-input v-model.number="form.ecwTbwRatio" type="number" :placeholder="t('inbodyTest.ecwTbwRatio')" />
					</el-form-item>
				</el-col>
			</el-row>

			<!-- 营养成分 -->
			<el-divider content-position="left">营养成分</el-divider>
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.proteinKg')" prop="proteinKg">
						<el-input v-model.number="form.proteinKg" type="number" :placeholder="t('inbodyTest.proteinKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.mineralsKg')" prop="mineralsKg">
						<el-input v-model.number="form.mineralsKg" type="number" :placeholder="t('inbodyTest.mineralsKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.boneMineralContentKg')" prop="boneMineralContentKg">
						<el-input v-model.number="form.boneMineralContentKg" type="number" :placeholder="t('inbodyTest.boneMineralContentKg')" />
					</el-form-item>
				</el-col>
			</el-row>

			<!-- 代谢与体型 -->
			<el-divider content-position="left">代谢与体型</el-divider>
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.basalMetabolicRateKcal')" prop="basalMetabolicRateKcal">
						<el-input v-model.number="form.basalMetabolicRateKcal" type="number" :placeholder="t('inbodyTest.basalMetabolicRateKcal')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.visceralFatLevel')" prop="visceralFatLevel">
						<el-input v-model.number="form.visceralFatLevel" type="number" :placeholder="t('inbodyTest.visceralFatLevel')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.waistHipRatio')" prop="waistHipRatio">
						<el-input v-model.number="form.waistHipRatio" type="number" :placeholder="t('inbodyTest.waistHipRatio')" />
					</el-form-item>
				</el-col>
			</el-row>

			<!-- 节段去脂体重 -->
			<el-divider content-position="left">节段去脂体重</el-divider>
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.leanMassRightArmKg')" prop="leanMassRightArmKg">
						<el-input v-model.number="form.leanMassRightArmKg" type="number" :placeholder="t('inbodyTest.leanMassRightArmKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.leanMassLeftArmKg')" prop="leanMassLeftArmKg">
						<el-input v-model.number="form.leanMassLeftArmKg" type="number" :placeholder="t('inbodyTest.leanMassLeftArmKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.leanMassTrunkKg')" prop="leanMassTrunkKg">
						<el-input v-model.number="form.leanMassTrunkKg" type="number" :placeholder="t('inbodyTest.leanMassTrunkKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.leanMassRightLegKg')" prop="leanMassRightLegKg">
						<el-input v-model.number="form.leanMassRightLegKg" type="number" :placeholder="t('inbodyTest.leanMassRightLegKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.leanMassLeftLegKg')" prop="leanMassLeftLegKg">
						<el-input v-model.number="form.leanMassLeftLegKg" type="number" :placeholder="t('inbodyTest.leanMassLeftLegKg')" />
					</el-form-item>
				</el-col>
			</el-row>

			<!-- 节段体脂 -->
			<el-divider content-position="left">节段体脂</el-divider>
			<el-row :gutter="16">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.fatMassRightArmKg')" prop="fatMassRightArmKg">
						<el-input v-model.number="form.fatMassRightArmKg" type="number" :placeholder="t('inbodyTest.fatMassRightArmKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.fatMassLeftArmKg')" prop="fatMassLeftArmKg">
						<el-input v-model.number="form.fatMassLeftArmKg" type="number" :placeholder="t('inbodyTest.fatMassLeftArmKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.fatMassTrunkKg')" prop="fatMassTrunkKg">
						<el-input v-model.number="form.fatMassTrunkKg" type="number" :placeholder="t('inbodyTest.fatMassTrunkKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.fatMassRightLegKg')" prop="fatMassRightLegKg">
						<el-input v-model.number="form.fatMassRightLegKg" type="number" :placeholder="t('inbodyTest.fatMassRightLegKg')" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('inbodyTest.fatMassLeftLegKg')" prop="fatMassLeftLegKg">
						<el-input v-model.number="form.fatMassLeftLegKg" type="number" :placeholder="t('inbodyTest.fatMassLeftLegKg')" />
					</el-form-item>
				</el-col>
			</el-row>

			<!-- 备注 -->
			<el-row :gutter="16">
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('inbodyTest.remark')" prop="remark">
						<el-input
							:placeholder="t('inbodyTest.inputRemarkTip')"
							v-model="form.remark"
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

<script lang="ts" name="InbodyTestDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj } from '/@/api/admin/inbodyTest';
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
	testDate: null as number | null,
	heightCm: null as number | null,
	weightKg: null as number | null,
	bmi: null as number | null,
	bodyFatPercentage: null as number | null,
	bodyFatMassKg: null as number | null,
	skeletalMuscleMassKg: null as number | null,
	leanBodyMassKg: null as number | null,
	totalBodyWaterKg: null as number | null,
	intracellularWaterKg: null as number | null,
	extracellularWaterKg: null as number | null,
	ecwTbwRatio: null as number | null,
	proteinKg: null as number | null,
	mineralsKg: null as number | null,
	boneMineralContentKg: null as number | null,
	basalMetabolicRateKcal: null as number | null,
	visceralFatLevel: null as number | null,
	waistHipRatio: null as number | null,
	leanMassRightArmKg: null as number | null,
	leanMassLeftArmKg: null as number | null,
	leanMassTrunkKg: null as number | null,
	leanMassRightLegKg: null as number | null,
	leanMassLeftLegKg: null as number | null,
	fatMassRightArmKg: null as number | null,
	fatMassLeftArmKg: null as number | null,
	fatMassTrunkKg: null as number | null,
	fatMassRightLegKg: null as number | null,
	fatMassLeftLegKg: null as number | null,
	remark: '',
});

// 定义校验规则
const dataRules = reactive({
	memberId: [{ required: true, message: '会员ID不能为空', trigger: 'blur' }],
	weightKg: [{ required: true, message: '体重不能为空', trigger: 'blur' }],
	testDate: [{ required: true, message: '测试日期不能为空', trigger: 'change' }],
});

// 打开弹窗
const openDialog = async (id: string) => {
	visible.value = true;
	form.id = '';

	// 重置表单数据
	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	// 获取记录信息
	if (id) {
		form.id = id;
		const { data } = await getObj(id);
		Object.assign(form, data);
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

// 暴露变量
defineExpose({
	openDialog,
});
</script>
