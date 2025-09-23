/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pig4cloud.pig.smm.api.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 群组表
 * </p>
 *
 * @author lengleng
 * @since 2025-09-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "群组")
public class SmmGroup extends SmmChannel {

	private static final long serialVersionUID = 1L;

	/**
	 * 群组链接
	 */
	@Schema(description = "群组链接")
	private String link;

}
