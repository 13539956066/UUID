## 租期,租期包含workId startTime endTime的信息
在snowflake生产id的timeStamp参数中,并不是以本机的时间为准的,本机的时间只是单纯地用作时间差△,而非timestamp
timestamp = startTime + △
其中△ = System.currentTime() - rentTime.  rentTime为系统发起租用请求的时间.

为什么这样做,因为如果不是以这个时间为准的话,如果已workId申请者的时间为准,则很有可能发生复用此workId的另一台机器,时钟比之前用过此workId的机器慢,从而导致重复uuid的情况发生



## 租期不宜过长
想象一下,如果租期过长,则会导致这些id暂时都属于不可用状态,当机器重启的时候,workId必须使用新的,从而导致workId增速过快,而workId的有限的资源.
极端情况下,遇到服务crash,需要不断重启的情况,则会耗尽workId.最终因为workId耗尽,导致服务启动不起来,这是绝对不允许的.



## workId机器时钟回退
如果机器在正常的运行中出现了回退,我们在内存中保存了lastTime,则检查发现时间回退之后,在那段时间会拒绝服务
如果机器程序重启间出现了回退,我们会在程序启动的时候去重新申请id,如果当前的id还未结束,会使用新的id
如果id已经结束了,snowflake的时间戳,我们并不是以本机的时间为准,而是租期的startTime +  ( System.currentTime() - System.租用动作发起时间).

## rent server时间回退
如果中心节点只有一台,发生时间回退,有可能会导致找不到可用的租期,服务启动不起来,而不会出现重复uuid的情况,这一点来说,是可以接受的,现实中关闭NTP基本已满足
如果两台机器,时钟不一致,且都在同时提供服务的情况下,做一个最坏的假设,B机器比A机器快了一天.则可能出现两台client机器共用同一个id的情况,只不过一个用的是今天的timestamp,另一个用的是明天的timestamp.
同样的,不会产生重复的uuid,但是会破坏uuid的大致有序性.但是换一个思路,大致有序性就是靠时间来保证的.原有的做法,两台不相干的机器,时间不同时,id也不同,本身也会破坏大致有序性.保证id rent server机器的时间有序
比保证多台机器同时有序简单多了,所以说这个是可以接受的.
可以容忍id rent server机器时钟不一致的场景.

## client
既然不应该在构造方法里使用阻塞方式去构造,那应该放在哪比较好


## 获取有效的workerid的策略
之前都是找最小的有用的,如果都没有的话,那么就找出最大的,然后+1
但是这种情况就是并发时候容易冲突,导致调用失败
使用size为50的线程池去执行10000个请求,每个请求的租期都是1s


> [ INFO] 2017-04-26 19:09:03.287 [Client.java:99] =======================get intervals end=======================
> [ INFO] 2017-04-26 19:09:03.293 [Client.java:100] =======================  analysis begin =======================
> [ INFO] 2017-04-26 19:09:03.299 [Client.java:129] total get interval size=3582
> [ INFO] 2017-04-26 19:09:03.299 [Client.java:130] error interval size=0


成功率只有1/3左右
实际中不会有这么多机器同时去申请worker_id的情况发生,而且我们目前设置的worker_id_bits也只有8位,也就是说在同一个namespace中最多支持256台机器同时工作

在将线程池大小调节到10的情况下,数据如下
[ INFO] 2017-04-26 19:17:57.403 [Client.java:99] =======================get intervals end=======================
[ INFO] 2017-04-26 19:17:57.407 [Client.java:100] =======================  analysis begin =======================
[ INFO] 2017-04-26 19:17:57.436 [Client.java:129] total get interval size=4831
[ INFO] 2017-04-26 19:17:57.436 [Client.java:130] error interval size=0


使用随机策略后
[ INFO] 2017-04-26 19:53:42.019 [Client.java:99] =======================get intervals end=======================
[ INFO] 2017-04-26 19:53:42.023 [Client.java:100] =======================  analysis begin =======================
[ INFO] 2017-04-26 19:53:42.030 [Client.java:129] total get interval size=9555
[ INFO] 2017-04-26 19:53:42.030 [Client.java:130] error interval size=0
