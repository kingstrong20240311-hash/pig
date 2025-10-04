package com.pig4cloud.pig.smm.dto;

import com.pig4cloud.pig.smm.enums.JoinResultEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class GroupJoinResultDTO {
	private String username;
	private JoinResultEnum result;
	private String errorMessage;
}
