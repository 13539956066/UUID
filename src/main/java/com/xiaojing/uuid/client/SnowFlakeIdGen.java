package com.xiaojing.uuid.client;

import com.xiaojing.uuid.bean.Interval;
import com.xiaojing.uuid.bean.UUID;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Java version of noeqd(https://github.com/bmizerany/noeqd)
 */
public class SnowFlakeIdGen implements IdGen {

  private final static Logger LOGGER = LoggerFactory.getLogger(SnowFlakeIdGen.class);
  //2017/01/01 00:00:00 UTC
  private final static long TWEPOCH = 1483228800000L;

  private final static long GAP = 5 * 1000;//提前5s续租
  private final static int RETRY = 3;

  private final String ip;
  private final int port;
  private final String namespace;
  private final long duration;
  private final int dataCenterBits;
  private final int workerBits;
  private final int sequenceBits;

  private final long workerLShift;
  private final long dataCenterLShift;
  private final long sequenceMask;
  private final long timeLShift;

  private final int dataCenterId;
  private int workerId;
  private long sequence = 0;
  private long lastTimeMillis = -1L;

  private long intervalStartTime;
  private long intervalEndTime;
  private long rentTimeStamp;
  private long rentTime;

  private volatile long newRentTimestamp;
  private volatile Interval newInterval;

  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  /**
   * SnowFlakeIdGen can be used to generate id, the id is 64 bits and combined in four parts with format:
   * time bits|data center bits|worker bits|sequential bits,
   * time bits determine how long we can keep time bits increasing counting from TWEPOCH(2017/01/01 00:00:00 UTC)
   * time bits should not be less than 40
   *
   */
  public SnowFlakeIdGen(String ip, int port, String namespace, long duration) throws Exception{


    this.ip = ip;
    this.port = port;
    this.namespace = namespace;
    this.duration = duration;
    this.rentTimeStamp = System.currentTimeMillis();

    Interval interval = getInterval(ip, port,namespace, duration);


    this.dataCenterId = interval.getDataCenterId();
    this.workerId = interval.getWorkerId();
    this.dataCenterBits = interval.getDataCenterBits();
    this.workerBits = interval.getWorkerBits();
    this.sequenceBits = interval.getSequenceBits();
    this.intervalStartTime = interval.getStartTime();
    this.intervalEndTime = interval.getEndTime();
    this.rentTime = duration;

    //time bits should not be less than 40 to make sure time bit keep increasing in 34 years counting from TWEPOCH
    if (dataCenterBits + workerBits + sequenceBits >= 24) {
      throw new IllegalArgumentException(
          "sum of data center bits, worker bits, sequence bits should be less than 34");
    }

    //sanity check for data center id
    if (dataCenterId < 0 || dataCenterId >= (1 << dataCenterBits)) {
      throw new IllegalArgumentException(
          String.format("data center Id should be between 0 and %d", 1 << interval.getDataCenterBits()));
    }

    //sanity check for worker id
    if (workerId < 0 || workerId >= (1 << workerBits)) {
      throw new IllegalArgumentException(
          String.format("worker Id should be between 0 and %d", 1 << interval.getWorkerBits()));
    }



    sequenceMask = ~(-1L << sequenceBits);
    workerLShift = sequenceBits;
    dataCenterLShift = sequenceBits + workerBits;
    timeLShift = sequenceBits + workerBits + dataCenterBits;
    renewal();
  }

  /**
   * generate an id, RuntimeException if clock is moving backwards
   *
   * @return id
   */
  public synchronized long nextId() {
    long currentTimeMillis = getAdjustTime();

    if (currentTimeMillis < lastTimeMillis) {
      throw new RuntimeException(String.format("clock is moving backwards. Rejecting requests until %d.", lastTimeMillis));
    }

    if(currentTimeMillis > this.intervalEndTime){
      throw new RuntimeException(String.format("interval has ended, stop gen uuid,end time %s", intervalEndTime));
    }

    if (currentTimeMillis == lastTimeMillis) {
      sequence = (sequence + 1) & sequenceMask;
      if (sequence == 0) {
        for (; currentTimeMillis <= lastTimeMillis; ) {
          currentTimeMillis = getAdjustTime();
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

  private long getAdjustTime(){
    return System.currentTimeMillis() - rentTimeStamp + intervalStartTime;
  }

  private Interval getInterval(String ip, int port, String namespace, long duration) throws Exception{
    int retry = RETRY;
    while(retry > 0) {
      TSocket socket = null;
      try {
        socket = new TSocket(ip, port);
        TBinaryProtocol protocol = new TBinaryProtocol(socket);
        UUID.Client client = new UUID.Client(protocol);
        socket.open();
        return client.rentInterval(namespace, duration);
      } catch (Exception e) {
        LOGGER.error("get a interval fail,ip={},port{},namespace={},e=", ip, port, namespace, e);
        retry--;
      } finally {
        if(null != socket){
          socket.close();
        }
      }
    }
    throw new Exception("get interval fail after "+ RETRY +" times!");
  }

  private Interval renewalInterval(String ip, int port, long duration) throws Exception{
    int retry = RETRY;
    while(retry > 0) {
      TSocket socket = null;
      try {
        socket = new TSocket(ip, port);
        TBinaryProtocol protocol = new TBinaryProtocol(socket);
        UUID.Client client = new UUID.Client(protocol);
        socket.open();
        Interval interval = new Interval(this.dataCenterId, this.workerId,
                                         this.intervalStartTime, this.intervalEndTime,
                                         this.dataCenterBits, this.workerBits, this.sequenceBits);
        return client.renewal(interval, duration);
      } catch (Exception e) {
        LOGGER.error("renewal interval fail,ip={},port={},e=", ip, port, e);
        retry--;
      } finally {
        if(null != socket){
          socket.close();
        }
      }
    }
    throw new Exception("renewal interval fail after "+ RETRY +" times!");
  }

  private void renewal(){
    executorService.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        long elapsed = System.currentTimeMillis() - rentTimeStamp;
        if (rentTime - elapsed < GAP) {//距离结束时间已经小于GAP,则提前续租
          try {
            Interval interval = renewalInterval(SnowFlakeIdGen.this.ip, SnowFlakeIdGen.this.port, SnowFlakeIdGen.this.duration);
            SnowFlakeIdGen.this.intervalEndTime = interval.getEndTime();
            rentTime = intervalEndTime - SnowFlakeIdGen.this.rentTimeStamp;
          } catch (Exception e) {
            LOGGER.error("renewal interval error,rent a new workerId,e=", e);
            rentNewInterval();//续租失败,则重新发起租期 todo IOException时需要重试,逻辑异常才重租
          }
        }
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  private void rentNewInterval(){
    try {
      this.newRentTimestamp = System.currentTimeMillis();
      this.newInterval = getInterval(ip, port, namespace, duration);
      LOGGER.info("new interval={}", this.newInterval);
      updateWorkerId();
    }catch (Exception e){
      LOGGER.error("rent new interval fail,ip={},port={},e=", ip, port, e);
    }
  }

  /**
   * 更新的过程中,不能生产UUID,所以此方法需要使用synchronized
   * 因为此方法与唯一的使用场景都是在renewal内,所以没有必要对rentTime这几个变量设为volatile
   */
  private synchronized void updateWorkerId(){
    this.rentTimeStamp = newRentTimestamp;
    this.workerId = this.newInterval.getWorkerId();
    this.intervalStartTime = this.newInterval.getStartTime();
    this.intervalEndTime = this.newInterval.getEndTime();
    this.rentTime = intervalEndTime - rentTimeStamp;
  }
}
