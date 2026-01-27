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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 密码加密工具类 使用 AES-128-CFB 算法加密密码
 *
 * @author pig4cloud
 */
@Slf4j
public class PasswordEncryptionUtil {

	/**
	 * 加密算法
	 */
	private static final String ALGORITHM = "AES";

	/**
	 * 加密模式/填充方式
	 */
	private static final String TRANSFORMATION = "AES/CFB/NoPadding";

	/**
	 * 使用指定的密钥加密密码
	 * @param password 明文密码
	 * @param encodeKey 加密密钥（同时用作 Key 和 IV）
	 * @return Base64 编码的加密后密码
	 */
	public static String encrypt(String password, String encodeKey) {
		try {
			// 将密钥转换为字节数组
			byte[] keyBytes = encodeKey.getBytes(StandardCharsets.UTF_8);

			// 确保密钥长度为 16 字节（AES-128）
			byte[] key = new byte[16];
			System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));

			// 创建密钥规范
			SecretKeySpec secretKeySpec = new SecretKeySpec(key, ALGORITHM);

			// 创建 IV 参数规范（使用相同的密钥）
			IvParameterSpec ivParameterSpec = new IvParameterSpec(key);

			// 初始化 Cipher
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

			// 加密
			byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

			// Base64 编码
			String result = Base64.getEncoder().encodeToString(encrypted);

			log.debug("密码加密成功，原始长度: {}, 加密后长度: {}", password.length(), result.length());

			return result;
		}
		catch (Exception e) {
			log.error("密码加密失败", e);
			throw new RuntimeException("密码加密失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 使用指定的密钥解密密码（用于测试验证）
	 * @param encryptedPassword Base64 编码的加密密码
	 * @param encodeKey 解密密钥（同时用作 Key 和 IV）
	 * @return 明文密码
	 */
	public static String decrypt(String encryptedPassword, String encodeKey) {
		try {
			// 将密钥转换为字节数组
			byte[] keyBytes = encodeKey.getBytes(StandardCharsets.UTF_8);

			// 确保密钥长度为 16 字节（AES-128）
			byte[] key = new byte[16];
			System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));

			// 创建密钥规范
			SecretKeySpec secretKeySpec = new SecretKeySpec(key, ALGORITHM);

			// 创建 IV 参数规范（使用相同的密钥）
			IvParameterSpec ivParameterSpec = new IvParameterSpec(key);

			// 初始化 Cipher
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

			// Base64 解码
			byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);

			// 解密
			byte[] decrypted = cipher.doFinal(encryptedBytes);

			return new String(decrypted, StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			log.error("密码解密失败", e);
			throw new RuntimeException("密码解密失败: " + e.getMessage(), e);
		}
	}

}
