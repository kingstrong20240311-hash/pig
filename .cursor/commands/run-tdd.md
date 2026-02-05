选择测试模块{module}: vault/order
运行{module}测试
运行命令:
vault: mvn -pl pig-vault/pig-vault-biz -am clean test 
order: mvn -pl pig-order/pig-order-biz -am clean test (等4分钟)
有错误修复错误,修复之后再运行一遍测试.
没错误就结束