## 分布式旅游预定系统
#### test: 该部分是用Java编写实现的测试用例
* data: 运行生成的数据文件夹
* results: 测试案例的 log
* Connector.java: 清除数据、启动组件、获取WC的引用、安全退出
* RunTest.java: 测试主程序
* Makefile
* 其它Java程序: 测试用例

#### 测试：
窗口一（运行Register）：
```
cd SourceCode/lockmgr
make clean
make 
cd ../transaction
make clean
make all
make runregistry &
```

窗口二（测试）：
```
cd SourceCode/test
mkdir data
mkdir results

make clean
make all
make test 
```

# 注意：
在测试结束后，在窗口一 ctrl+c 中止进程后，需要执行下述命令完全关闭 `Register：kill $(lsof -t -i:3345)`
若要关闭所有Java程序：`kill $(lsof -c java)`
