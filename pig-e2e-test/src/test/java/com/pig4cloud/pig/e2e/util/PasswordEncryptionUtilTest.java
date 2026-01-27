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

package com.pig4cloud.pig.e2e.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码加密工具类测试
 *
 * @author pig4cloud
 */
@Slf4j
class PasswordEncryptionUtilTest {

	/**
	 * 默认加密密钥
	 */
	private static final String DEFAULT_ENCODE_KEY = "thanks,pig4cloud";

	/**
	 * 测试默认密钥加密 "123456" 是否输出 "YehdBPev"
	 */
	@Test
	void testEncryptWithDefaultKey() {
		String password = "123456";
		String expected = "YehdBPev";

		String encrypted = PasswordEncryptionUtil.encrypt(password, DEFAULT_ENCODE_KEY);

		log.info("密码加密测试：password={}, encrypted={}", password, encrypted);

		assertEquals(expected, encrypted, "使用默认密钥加密 '123456' 应该输出 'YehdBPev'");
	}

	/**
	 * 测试加密解密一致性
	 */
	@Test
	void testEncryptDecrypt() {
		String originalPassword = "testPassword123!";
		String encodeKey = DEFAULT_ENCODE_KEY;

		// 加密
		String encrypted = PasswordEncryptionUtil.encrypt(originalPassword, encodeKey);
		assertNotNull(encrypted, "加密结果不应为空");
		assertNotEquals(originalPassword, encrypted, "加密后的密码应与原密码不同");

		log.info("加密测试：原密码={}, 加密后={}", originalPassword, encrypted);

		// 解密
		String decrypted = PasswordEncryptionUtil.decrypt(encrypted, encodeKey);
		assertEquals(originalPassword, decrypted, "解密后的密码应与原密码一致");

		log.info("解密测试：解密后={}", decrypted);
	}

	/**
	 * 测试不同密码加密结果不同
	 */
	@Test
	void testDifferentPasswordsDifferentResults() {
		String password1 = "password1";
		String password2 = "password2";
		String encodeKey = DEFAULT_ENCODE_KEY;

		String encrypted1 = PasswordEncryptionUtil.encrypt(password1, encodeKey);
		String encrypted2 = PasswordEncryptionUtil.encrypt(password2, encodeKey);

		assertNotEquals(encrypted1, encrypted2, "不同密码的加密结果应该不同");

		log.info("不同密码加密测试：password1={}, encrypted1={}", password1, encrypted1);
		log.info("不同密码加密测试：password2={}, encrypted2={}", password2, encrypted2);
	}

	/**
	 * 测试不同密钥加密结果不同
	 */
	@Test
	void testDifferentKeysDifferentResults() {
		String password = "testPassword";
		String key1 = DEFAULT_ENCODE_KEY;
		String key2 = "different-key-12";

		String encrypted1 = PasswordEncryptionUtil.encrypt(password, key1);
		String encrypted2 = PasswordEncryptionUtil.encrypt(password, key2);

		assertNotEquals(encrypted1, encrypted2, "相同密码使用不同密钥的加密结果应该不同");

		log.info("不同密钥加密测试：key1={}, encrypted1={}", key1, encrypted1);
		log.info("不同密钥加密测试：key2={}, encrypted2={}", key2, encrypted2);
	}

	/**
	 * 测试空密码加密
	 */
	@Test
	void testEmptyPassword() {
		String password = "";
		String encodeKey = DEFAULT_ENCODE_KEY;

		String encrypted = PasswordEncryptionUtil.encrypt(password, encodeKey);
		assertNotNull(encrypted, "空密码加密结果不应为空");

		String decrypted = PasswordEncryptionUtil.decrypt(encrypted, encodeKey);
		assertEquals(password, decrypted, "空密码解密后应与原密码一致");

		log.info("空密码加密测试：encrypted={}", encrypted);
	}

	/**
	 * 测试特殊字符密码加密
	 */
	@Test
	void testSpecialCharactersPassword() {
		String password = "p@ssw0rd!@#$%^&*()";
		String encodeKey = DEFAULT_ENCODE_KEY;

		String encrypted = PasswordEncryptionUtil.encrypt(password, encodeKey);
		assertNotNull(encrypted, "特殊字符密码加密结果不应为空");

		String decrypted = PasswordEncryptionUtil.decrypt(encrypted, encodeKey);
		assertEquals(password, decrypted, "特殊字符密码解密后应与原密码一致");

		log.info("特殊字符密码加密测试：password={}, encrypted={}", password, encrypted);
	}

	/**
	 * 测试中文密码加密
	 */
	@Test
	void testChinesePassword() {
		String password = "中文密码123";
		String encodeKey = DEFAULT_ENCODE_KEY;

		String encrypted = PasswordEncryptionUtil.encrypt(password, encodeKey);
		assertNotNull(encrypted, "中文密码加密结果不应为空");

		String decrypted = PasswordEncryptionUtil.decrypt(encrypted, encodeKey);
		assertEquals(password, decrypted, "中文密码解密后应与原密码一致");

		log.info("中文密码加密测试：password={}, encrypted={}", password, encrypted);
	}

}
