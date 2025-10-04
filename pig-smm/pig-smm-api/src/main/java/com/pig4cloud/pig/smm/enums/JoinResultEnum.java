package com.pig4cloud.pig.smm.enums;

public enum JoinResultEnum {
	INIT(0), SUCCESS(1), FAIL(2);

	public Integer result;

	JoinResultEnum(int result) {
		this.result = result;
	}

	public Integer getResult() {
		return result;
	}

	public static JoinResultEnum fromResult(int result) {
		for (JoinResultEnum e : JoinResultEnum.values()) {
			if (e.getResult().equals(result)) {
				return e;
			}
		}
		throw new IllegalArgumentException("Invalid join result: " + result);
	}
}
