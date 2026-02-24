<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item :label="$t('trainingsession.memberId')" prop="memberId">
						<el-input
							:placeholder="$t('trainingsession.inputMemberIdTip')"
							style="max-width: 180px"
							v-model="state.queryForm.memberId"
						/>
					</el-form-item>
					<el-form-item :label="$t('trainingsession.coachId')" prop="coachId">
						<el-input
							:placeholder="$t('trainingsession.inputCoachIdTip')"
							style="max-width: 180px"
							v-model="state.queryForm.coachId"
						/>
					</el-form-item>
					<el-form-item :label="$t('trainingsession.status')" prop="status">
						<el-select :placeholder="$t('trainingsession.inputStatusTip')" v-model="state.queryForm.status">
							<el-option label="已预约" value="SCHEDULED"></el-option>
							<el-option label="已完成" value="COMPLETED"></el-option>
							<el-option label="已取消" value="CANCELED"></el-option>
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-button @click="getDataList" icon="search" type="primary">
							{{ $t('common.queryBtn') }}
						</el-button>
						<el-button @click="resetQuery" icon="Refresh">{{ $t('common.resetBtn') }}</el-button>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row>
				<div class="mb8" style="width: 100%">
					<el-button
						v-auth="'gym_trainingsession_add'"
						@click="formDialogRef.openDialog()"
						class="ml10"
						icon="folder-add"
						type="primary"
					>
						{{ $t('common.addBtn') }}
					</el-button>

					<el-button
						plain
						v-auth="'gym_trainingsession_del'"
						:disabled="multiple"
						@click="handleDelete(selectObjs)"
						class="ml10"
						icon="Delete"
						type="primary"
					>
						{{ $t('common.delBtn') }}
					</el-button>

					<right-toolbar
						:export="'gym_trainingsession_export'"
						@exportExcel="exportExcel"
						@queryTable="getDataList"
						class="ml10"
						style="float: right; margin-right: 20px"
						v-model:showSearch="showSearch"
					></right-toolbar>
				</div>
			</el-row>
			<el-table
				:data="state.dataList"
				@selection-change="handleSelectionChange"
				style="width: 100%"
				v-loading="state.loading"
				border
				:cell-style="tableStyle.cellStyle"
				:header-cell-style="tableStyle.headerCellStyle"
			>
				<el-table-column align="center" type="selection" width="40" />
				<el-table-column :label="t('trainingsession.index')" type="index" width="60" />
				<el-table-column :label="t('trainingsession.memberId')" prop="memberId" show-overflow-tooltip />
				<el-table-column :label="t('trainingsession.coachId')" prop="coachId" show-overflow-tooltip />
				<el-table-column :label="t('trainingsession.lessonPlanId')" prop="lessonPlanId" show-overflow-tooltip />
				<el-table-column :label="t('trainingsession.scheduledAt')" prop="scheduledAt" show-overflow-tooltip />
				<el-table-column :label="t('trainingsession.completedAt')" prop="completedAt" show-overflow-tooltip />
				<el-table-column :label="t('trainingsession.status')" prop="status" show-overflow-tooltip>
					<template #default="scope">
						<el-tag v-if="scope.row.status === 'SCHEDULED'" type="info">已预约</el-tag>
						<el-tag v-else-if="scope.row.status === 'COMPLETED'" type="success">已完成</el-tag>
						<el-tag v-else-if="scope.row.status === 'CANCELED'" type="danger">已取消</el-tag>
						<el-tag v-else type="warning">{{ scope.row.status }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('trainingsession.createTime')" prop="createTime" show-overflow-tooltip />
				<el-table-column :label="$t('common.action')" width="150">
					<template #default="scope">
						<el-button icon="edit-pen" @click="formDialogRef.openDialog(scope.row.id)" text type="primary">
							{{ $t('common.editBtn') }}
						</el-button>
						<el-button
							icon="delete"
							v-auth="'gym_trainingsession_del'"
							@click="handleDelete([scope.row.id])"
							text
							type="primary"
							style="margin-left: 12px"
						>
							{{ $t('common.delBtn') }}
						</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>

		<!-- 编辑、新增  -->
		<form-dialog @refresh="getDataList()" ref="formDialogRef" />
	</div>
</template>

<script lang="ts" name="gymTrainingSession" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { delObj, fetchList } from '/@/api/admin/trainingsession';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

// 引入组件
const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const { t } = useI18n();

// 定义变量内容
const formDialogRef = ref();
// 搜索变量
const queryRef = ref();
const showSearch = ref(true);
// 多选变量
const selectObjs = ref([]) as any;
const multiple = ref(true);

const state: BasicTableProps = reactive<BasicTableProps>({
	queryForm: {},
	pageList: fetchList,
	descs: ['create_time'],
});

//  table hook
const { getDataList, currentChangeHandle, sizeChangeHandle, downBlobFile, tableStyle } = useTable(state);

// 清空搜索条件
const resetQuery = () => {
	queryRef.value.resetFields();
	getDataList();
};

// 导出excel
const exportExcel = () => {
	downBlobFile('/admin/gym/trainingsession/export', state.queryForm, 'trainingsession.xlsx');
};

// 多选事件
const handleSelectionChange = (objs: { id: number }[]) => {
	selectObjs.value = objs.map(({ id }) => id);
	multiple.value = !objs.length;
};

// 删除操作
const handleDelete = async (ids: number[]) => {
	try {
		await useMessageBox().confirm(t('common.delConfirmText'));
	} catch {
		return;
	}

	try {
		await delObj(ids);
		getDataList();
		useMessage().success(t('common.delSuccessText'));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};
</script>
