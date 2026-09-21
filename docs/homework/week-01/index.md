# Week 01：开发环境与个人仓库

## 环境检查

> 下面粘贴每条命令的真实输出（对应的截图放在 `screenshots/` 目录下）。

### java --version

```
openjdk 17.0.20 2026-07-21
OpenJDK Runtime Environment (build 17.0.20+8-1-26.04-Ubuntu)
OpenJDK 64-Bit Server VM (build 17.0.20+8-1-26.04-Ubuntu, mixed mode, sharing)
```

### mvn --version

```
Apache Maven 3.9.12
Maven home: /usr/share/maven
Java version: 17.0.20, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "6.18.33.2-microsoft-standard-wsl2", arch: "amd64", family: "unix"
```

### git --version

```
git version 2.53.0
```

### docker version

```
Client:
 Version:           29.8.0
 API version:       1.56
 Go version:        go1.26.8
 Git commit:        88096ef
 Built:             Thu Sep  3 21:49:51 2026
 OS/Arch:           linux/amd64
 Context:           default

Server: Docker Desktop 4.91.0 (239619)
 Engine:
  Version:          29.8.0
  API version:      1.56 (minimum version 1.40)
  Go version:       go1.26.8
  Git commit:       3ce5872
  Built:            Thu Sep  3 21:51:20 2026
  OS/Arch:          linux/amd64
  Experimental:     false
 containerd:
  Version:          v2.3.4
  GitCommit:        db8809540e1a7a9da5d518876894933ff55692ab
 runc:
  Version:          1.4.3
  GitCommit:        v1.4.3-0-gbb14dabe
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0
```

### docker compose version

```
Docker Compose version v5.5.1
```



## 概念回答

**什么是微服务架构？**

​		微服务架构说白了就是不把所有功能写在一个大项目里，而是拆成好几个小项目，每个小项目只管一小块事情（比如一个专门处理用户、一个专门处理订单），它们各自能单独启动、单独改代码、单独重新部署，谁也不依赖谁的代码，只通过网络接口（比如发 HTTP 请求）互相打交道。跟以前写单体项目时直接调用一个函数完全不一样。

**微服务和单体架构的主要区别是什么？**

​		单体架构就是我们平时最开始写的那种——所有代码打包成一个项目一起跑，写起来简单，但项目一大就容易牵一发动全身，改个小功能可能得把整个项目重新测一遍、重新部署一遍。而微服务是把这些功能拆成一个个独立的服务，哪个服务出问题只用动它自己，也可以单独给某个服务加机器扩容，不用整个系统一起扩。但代价也很明显，这次我自己在搭环境的时候就深有体会：光是要把 Docker、WSL 这些东西配置好、让服务之间能互相通信，就已经比单体项目复杂不少了，更别说以后还要考虑服务之间数据不一致怎么办这些问题。

**为什么本课程先实现单体系统，再逐步拆分为微服务？**

​		我猜是因为如果一上来就直接上微服务，光是注册中心、网关这些基础设施怎么搭、服务之间怎么调用，就已经够让人头疼的了（这次单纯装个 Docker 环境都折腾了挺久），业务逻辑本身反而顾不上。不如先写成单体，把功能实现对、逻辑理清楚，之后再看哪些模块之间联系不紧密、可以拆出去，这样每一步拆分都是有理由的，不是为了拆而拆。

**为什么作业需要提供可重复运行的测试或验证脚本？**

​		因为截图和文字说明只能证明"这一次我跑成功了"，但换个人的电脑、或者过段时间代码改了之后还能不能跑通，光靠这些证明不了。有一份能重复跑的测试脚本，其他人拿到手就能直接验证功能是不是真的没问题，自己以后改代码改到一半，也能跑一下脚本看看有没有把原来好使的功能弄坏，微服务这种好几个服务互相调来调去的项目，这个尤其重要，不然出了问题都不知道是哪个服务的锅。

## 问题记录

​		本次未遇到明显问题。