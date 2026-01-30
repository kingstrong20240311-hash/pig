1. 重启服务：pig-gateway, pig-auth, pig-upms-biz, pig-order-biz, pig-vault-biz
2. 以环境变量：PIG_GATEWAY_URL=127.0.0.1:9999，运行pig-e2e-test中的verify target
3. 检测verify运行结果，有错误的话，先分析错误，不改代码。