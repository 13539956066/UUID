### 一、数据库发号器
  每一次都请求数据库，通过数据库的自增ID来获取全局唯一ID    
  对于小系统来说，这是一个简单有效的方案，不过也就不符合讨论情形中的高并发的场景。    
  首先，数据库自增ID需要锁表    
  而且，UUID的生成强依赖于数据库，每次获取UUID都需要经过一次数据库的调用，性能损耗很大。    
  其实，在这种大并发的场景中，数据库的主键都不建议使用数据库的自增ID。因为虽然这个简单，但是如果随便业务发展，需要对原有的数据进行重新分库分表的时候，可能会产生主键冲突，这影响了系统的平滑扩容，容易埋下坑。    

### 二、中间件产生UUID
  常用的中间件，以redis和zookeeper为例，都有产生分布式唯一ID的方案，如redis的getAndIncrement，zookeeper的sequenceId。都是分布式UUID的解决方案。     
  而且redis和zookeeper中间件的性能都很强大，比数据库要好。    
  缺点还是，UUID的生成强依赖于中间件，每次获取UUID都需要一次远程调用。    
  依赖远程调用的缺陷，可以通过一次取批量的方式来解决，据说weibo就是这么做的，从redis中批量取一堆。    
  强依赖于中间件这件事，总感觉是一个不好的设计。虽然现在的中间件可靠性都比较好，甚至可以做到5个9以上，但是主业务流程强依赖于中间件，还是觉得有那么些不爽。比如强依赖数据库这个是可以接受的，但是依赖于zookeeper或redis从设计上看不可取。    


### 三、UUID
`Universally Unique IDentifier(UUID)`，这是一个具有rfc标准的uuid，见[RFC文档](http://www.ietf.org/rfc/rfc4122.txt)    

![](http://images2015.cnblogs.com/blog/1081851/201612/1081851-20161216164113776-668145853.png)


##### 4.1.4 Timestamp 时间戳
  timestamp是一个60位bit的值。    
  对于V1版本的UUID来说，这代表着UTC的时间，也就是从1582年的10月15日的00：00：00.00到现在为止经过的时间，单位是100  nanosecond，对于那些没有可用UTC的系统来说，他们可以用本机时间代替UTC，只要他们在系统中始终保持着这种一致性。但是并不推荐这种做饭，因为本机时间和UTC时间只是一个时区位移的区别而已。    
  对于V3和V5版本的UUID来说，timestamp是一个60bit的值，由一个name来得到的。    
  对于V4版本的UUID来说，timestamp是一个60bit的随机数，或者说伪随机数。    
  timestamp中最重要的就是代表版本的那4位，位于time_hi_and_version字段中的第4到第7位，这是用来区分不同版本的，具体的内容参见4.1.3。    

##### 4.1.5.  Clock Sequence 时钟序列
  对于V1版本的UUID来说，顺序号是用来帮助避免当时钟后退可能带来的冲突，以及node id发生变化时可能引起的冲突。    
  比如，当系统由于断电等原因导致时间倒退时，UUID生成器无法确保是否已经有比当前设置的系统时间更大的UUID已经被生成了，所以始终序列ID需要进行更新，如果知道之前的值的话，更新的操作只需对其进行+1即可，如果不知道的话，应该设置成一个随机数。    
  同样的，如果节点的id变化了的话，比如某一块网卡从一台机器转插到另一台，通过重置序列号的方式也能减少产生冲突的可能。如果之前的序列号是已知的话，那么只需要简单地进行+1即可，当然，这种情况不大可能发生。    
  序列号的初始值必须是随机的，这样才可以减少与系统的相关性，这样可以更好地保护UUID，防止系统间迅速地切换破坏UUID的唯一性。所以，序列号的初始值一定是要与node id无关的。    


##### 4.1.6.  Node 节点
  对于V1版本的UUID来说，node字段是由IEEE 802的MAC地址组成的，通常是本机的地址，对于那些有多个IEEE 802地址的机器来说，任选一个作为node字段即可。对于那些没有IEEE地址的机器来说，可以用一个随机数或者伪随机数来代替。    
  对于V4版本的UUID来说，node字段是由随机数或者伪随机数构成。    


##### 4.4.  Algorithms for Creating a UUID from Truly Random orPseudo-Random Numbers
  v4版本的UUID设计就是通过随机数或者是伪随机数来生成UUID。    

  算法有以下规则：
    1、最重要的两位，第6位和第7位，clock_seq_hi_and_reserved，分别设置成0和1.    
    2、最重要的四位，第12位到第15位，time_hi_and_version，设置成4.1.3描述的内容，0100。    
    3、其他的位设置成随机数或伪随机数即可。    


##### 总结
  从定义中了解了V1和V4这两种比较有代表性的UUID生成规则，实际的生产应用中，V1好像并没有严格的实现。而V4这种基本都是伪随机数的做法，JDK的UUID就是这么干的。    
  这种完全随机的做法，好处是不用再依赖了，但是可读性较差，而且如果使用其作为主键的话，数据库中的索引会经常需要进行改动。    



### 四、SnowFlake
  snowflake算法是twitter所使用的生成UUID的算法。为了满足Twitter每秒上万条消息的请求，每条消息都必须分配一条唯一的id，且这些id还需要根据时间基本有序。    

![](http://images2015.cnblogs.com/blog/1081851/201612/1081851-20161216164125479-1591675346.png)



  如图所示，这里第1位不可用，前41位表示时间，中间10位用来表示工作机器的id，后12位的序列号.    
  其中时间比较好理解，工作机器id则是机器标识，序列号是一个自增序列。有多少位表示在这一个单位时间内，此机器最多可以支持2^12个并发。在进入下一个时间单位后，序列号归0。    

  当然，这些字段的排序和定义也不一定要完全与他一致。比如第一位也可以使用起来，workerid还可以分成其他。    
  要保证根据时间大致有序，所以高位用来保存时间的内容是不可避免了，由于很多操作系统本身只支持毫秒级的时间，所以时间单位使用毫秒级就已经足够了。    
  这三个字段的长度分配分别与如下指标相关：系统设计可用时间、系统所包含的机器数量、系统设计的单机QPS。所以可以根据系统的实际情况，灵活进行调整。    
  worker id这个字段，为了不冲突，可以进行统一分配管理，也可以通过服务注册等方式来进行动态管理。当然第一种分配管理这种把work id写入到代码或者配置中的方式显然不可取，如果是小系统可以进行简单粗暴地redis的getAndIncrement进行处理，反正位数多，不怕浪费。    


  参照代码实现如下


	sequenceMask = ~(-1L << sequenceBits);

	public synchronized long nextId() {
		long currentTimeMillis = System.currentTimeMillis();
		if (currentTimeMillis < lastTimeMillis) {
			throw new RuntimeException(String.format("clock is moving backwards.
			Rejecting requests until %d.", lastTimeMillis));
		}

		if (currentTimeMillis == lastTimeMillis) {
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				for (; currentTimeMillis <= lastTimeMillis; ) {
					currentTimeMillis = System.currentTimeMillis();
				}
			}
		} else {
			sequence = 0;
		}

		lastTimeMillis = currentTimeMillis;
		return ((currentTimeMillis - TWEPOCH) << timeLShift) |
			(dataCenterId << dataCenterLShift) |
			(workerId << workerLShift) |
			sequence;
	}


### 五、实践中的问题
#### workid
如何确定自己的workid一定就是唯一的呢？或者说，处于工作中的所有workid都是不一样的    
使用数据库，没办法回收    
使用zk临时节点，容易出现多个相同的workid同时工作    
#### 时钟错乱    
即使我们已经保证了workid是唯一的，但是时间也是影响id生成的因素之一，如果发生了机器重启后，使用相同的workid，但是时间发生了回退的话，还是有可能会出现产生重复的id。    
#### 无状态 ----> 有状态
使用一个中心节点了管理workid的租期，租期包含workid的值，以及有效的时间。     
使用者发现自己的租期快到的时候，有两种选择，直接关闭，或者选择续租，如果续租成功，则继续使用，等待下一次租期截止的到来。    
如果没有续租，则在租期到之前停止服务，除非再次获取了租期，可以是不同的workid     
这样，中心节点就比较重要了，而且租期本身包含时间信息，所以也不担心客户节点时钟倒退。    
当然，中心节点的稳定性则比较重要。    




### 总体设计
1、worker_id是有限的资源，为了充分利用，使用了租期interval的概念，nterval包含了startTime和endTime，每台机器持有的都是worker_id的一个时间段。  
2、在一个interval的endTime这个时刻过去后，表明此worker_id的这个租期已经失效了。新的租用请求，可以通过获取新的interval来复用此worker_id，但是必须满足新的`this.startTime > prev.endTime`  
3、虽然client机器的时钟不可靠，不能相信，但是相对时间还是准确的，所以我们就对snowflake中的timestamp这个参数进行一个adjust。
`timestamp = client.system.currentTime() - client.rentTimestamp + interval.starTime`  
其中`client.rentTimestamp`是机器去发起租用的那个时刻的`client.system.curentTime()`   
为什么这样做，因为如果不是以这个时间为准的话，如果以workerId的申请者client的时间为准。则很有可能出现的情况是，复用了此workerId的另一台机器，时钟比之前用过此workId的机器慢，从而导致timestamp重复，进而产生重复的UUID。  


4、完整流程图如下  
![](http://images2015.cnblogs.com/blog/1081851/201704/1081851-20170427173411537-93912558.png)




### 租期不宜过长
想象一下，如果租期过长，则会导致这些id暂时都属于不可用状态，所以当机器重启的时候，workerId必须使用新的，从而导致workId增速过快，而workerId的有限的资源。   
极端情况下，遇到服务crash，需要不断重启的情况，则会耗尽workerId。最终因为workerId耗尽，导致服务启动不起来，这是绝对不允许的。
后期会考虑在服务关闭的时候主动发起一个取消租期的请求，当然这个请求也和IP一样，是尽力去取消，取消不了就算了。



### Generator时钟回退
如果机器在正常的运行中出现了回退,我们在内存中保存了lastTime，则检查发现时间回退之后，在时钟追上原有时间之前的那段时间会拒绝服务。   
如果机器程序重启后出现了回退，我们会在程序启动的时候去重新申请id，如果之前使用的id租期还未结束，会使用新的id，这避免了重复id的产生。   
如果之前使用的id的租期已经结束了，则由可能会出现复用原来workerId的情况，但是在我们这里的snowflake算法的时间戳中，我们并不是以本机的时间为准，而是租期的startTime +  ( System.currentTime() - System.租用动作发起时间)。所以这也避免了重复id的产生。

## Rent Server时钟回退
如果中心节点只有一台，这台机器发生了时间回退。则有可能在client节点租用wokerid的时候会找不到可以租用的workerid，从导致client节点的uuid服务无法工作,但是也不会出现重复uuid的情况。正如前文所说的，server服务的可用性比较重要，不能使用单点进行部署。

如果server有多台机器，且时钟不一致，且都在同时提供服务的情况下。  
做一个最坏的假设，B机器比A机器快了一天。我们假定A机器的时间是正常的。  
client_a通过A获取了worker_id为1的租期，时间是从今天9点到10点，而client_b通过B去获取worker_id的时候，B机器时间已经到了明天，所以它认为1这个worker_id是可用的，于是把1分配给了机器client_b。  
这样就出现两台client机器共用同一个id的情况，只不过一个用的是今天的timestamp，另一个用的是明天的timestamp。  
同样的，这里不会产生重复的uuid，但是会破坏uuid的大致有序性。但是换一个思路，大致有序性就是靠机器时间来保证的。如果使用原有的做法，两台不相干的机器，时间不同,worker_id也不同，也会破坏大致有序性。所以这里的破坏大致有序性并不是因为引入了rent server所导致的，而且保证rent server机器的时间一致，比保证多台机器同时有序简单多了，所以说这个也是可以接受的。   

但是这里会有一个问题，那就是A机器的租约到期后，想续租，结果发现续租不上了，因为续租我们使用的是CAS去更新，但是刚才这种情况，续租会失败，这时候client端必须处理这种续租不上时应该先将uuid置为不可用，然后发现新的租约请求。   

综上所述，可以容忍rent server机器时钟不一致的场景.

### Generator初始化与续租
##### 续租的时机
因为网络会有延迟的存在，所以得留一定的buffer，提前进行续租，续租成功后，需要更新租期结束时间。     
续租如果失败了，会发起租用新id的请求，租用新id成功后，需要更新workerId，rentTimestamp，intervalStartTime，intervalEndTime。     
这两个动作都是通过一个后台线程定时去执行的，我们都是选择1s执行一次，buffer选择的时间是5s。   

##### getNextId()
在getNextId()的方法内，如果发现当前时间，距离租用发起时间，到现在的时间间隔已经超过租期了，会拒绝生成uuid，抛出runtime exception。


##### 初始化
初始化的时候，应该先获取workerId再服务，虽然不大应该在构造方法里使用阻塞方式去构造，但是uuid这么关键的东西不能提供服务，启动了服务感觉也没啥大用，所以最后还是选择了在构造方法中传入rent server的信息，在构造方法中使用rpc去获取worker_id等信息。  
在demo代码中做的比较简陋，没有做集群。


### 选择workerId的策略
最初的选择策略是租期已经结束的workerId中选择数值最小的，如果没有租期已经结束的workerId的话,那么就找出所有workerId中数值最大的,然后+1，并新增这个workId，并将其标记为租期已结束。当然，还得考虑id不能超过2 ^ workerIdBits。   
原本的意思是想让workId的顺序规则一点，但是这种情况就是并发出现的时候特别容易冲突,导致租用id失败。所以还是选择了使用随机的策略，在选择租期结束的id和选择一个id新插入的过程中，都使用了随机的策略，成功率有了显著的上升。测试的数据如下：    


使用size为50的线程池去执行10000个请求,每个请求的租期都是1s。    
实际中不会有这么多机器同时去申请worker_id的情况发生,而且我们目前设置的worker_id_bits也只有8位,也就是说在同一个namespace中最多支持256台机器同时工作。所以这个测试只是用于说明选择策略这个问题。


> [ INFO] 2017-04-26 19:09:03.287 [Client.java:99] =======================get intervals end=======================   
> [ INFO] 2017-04-26 19:09:03.293 [Client.java:100] =======================  analysis begin =======================   
> [ INFO] 2017-04-26 19:09:03.299 [Client.java:129] total get interval size=3582   
> [ INFO] 2017-04-26 19:09:03.299 [Client.java:130] error interval size=0   


成功率只有1/3左右  
在将线程池大小调节到10的情况下,数据如下
>[ INFO] 2017-04-26 19:17:57.403 [Client.java:99] =======================get intervals end=======================   
>[ INFO] 2017-04-26 19:17:57.407 [Client.java:100] =======================  analysis begin =======================   
>[ INFO] 2017-04-26 19:17:57.436 [Client.java:129] total get interval size=4831   
>[ INFO] 2017-04-26 19:17:57.436 [Client.java:130] error interval size=0   

使用随机策略后，成功率上升到95%
>[ INFO] 2017-04-26 19:53:42.019 [Client.java:99] =======================get intervals end=======================   
>[ INFO] 2017-04-26 19:53:42.023 [Client.java:100] =======================  analysis begin =======================   
>[ INFO] 2017-04-26 19:53:42.030 [Client.java:129] total get interval size=9555   
>[ INFO] 2017-04-26 19:53:42.030 [Client.java:130] error interval size=0   


### Code
[talk is cheap，code is here](https://github.com/flystar32/UUID)   
这里实现了一个基于thrift协议的rent server demo，client使用的是SnowFlakeIdGen，使用方法见TestUUIDGenerator

##### todo：   
1、服务必须使用集群的方式，这里还没做，应该直接传入servicename，然后根据服务发现去调用，后续再client断加上，而对于服务端，可以直接水平扩容。   
2、TWEPOCH应该在rent server端配置，而非在client端配置，否则关于时间的工作又白做了。   
3、server端的namespace的管理应该写的优雅一点   
4、分层也很混乱，在Dao里杂糅了许多业务逻辑



### 参考文档
snowflake <https://github.com/twitter/snowflake>

江南白衣  <http://calvin1978.blogcn.com/articles/uuid.html>

lanindex.com <http://www.lanindex.com/twitter-snowflake%EF%BC%8C64%E4%BD%8D%E8%87%AA%E5%A2%9Eid%E7%AE%97%E6%B3%95%E8%AF%A6%E8%A7%A3/>
