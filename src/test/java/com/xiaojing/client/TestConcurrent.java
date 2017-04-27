package com.xiaojing.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.xiaojing.uuid.bean.Interval;
import com.xiaojing.uuid.bean.UUID;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiaojing on 17/4/27.
 *
 * 执行此测试的时候,需要将Server端对workerIdBits的限制放开,2^8测不出效果
 */
public class TestConcurrent {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestConcurrent.class);

  @Test
  public void testConcurrent() throws Exception{
    concurrent(10000,10, 120);
    concurrent(10000,50, 360);
  }

  private void concurrent(int total,int threadSize,long waitTime) throws Exception {

    Map<Integer,List<Interval>> map = Maps.newConcurrentMap();
    ExecutorService executorService = Executors.newFixedThreadPool(threadSize);


    //执行10000次请求
    for(int i=0;i<total;i++){
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          TSocket socket = null;
          try{
            socket = new TSocket("127.0.0.1",9999);

            TBinaryProtocol protocol = new TBinaryProtocol(socket);
            UUID.Client client = new UUID.Client(protocol);

            socket.open();
            Interval interval = client.rentInterval("test",1000L);//租期都是只有1s,便于测试

            //将获取到的interval放在map中,最后进行分析,看租期是否有重合
            List<Interval> list = map.get(interval.getWorkerId());
            if(null == list){
              list = Lists.newArrayList();
              map.put(interval.getWorkerId(),list);
            }
            list.add(interval);
          }catch (Exception e){
//            LOGGER.error("get interval error,e=",e);
          }finally {
            if(null != socket) {
              socket.close();
            }
          }
        }
      });
    }


    //主线程睡眠一段时间,等待10000个请求执行完毕
    TimeUnit.SECONDS.sleep(waitTime);
    int count = 0;
    int errorCount = 0;
    LOGGER.info("=======================get intervals end=======================");
    LOGGER.info("=======================  analysis begin =======================");

    //最后分析,看租期是否有冲突的
    for(List<Interval> list : map.values()){

      //插入到list过程中可能有乱序,根据开始时间排序后在比较
      Collections.sort(list, new Comparator<Interval>() {
        @Override
        public int compare(Interval o1, Interval o2) {
          if (o1.getStartTime() < o2.getStartTime()) {
            return -1;
          } else {
            return 1;
          }
        }
      });

      for(int i=1;i<list.size();i++){
        count++;
        long prevEnd = list.get(i -1).getEndTime();
        long start = list.get(i).getStartTime();
        if(start <= prevEnd){
          errorCount++;
          LOGGER.error("interval error!prev={},next={}",list.get(i-1),list.get(i));
        }
      }
    }
    LOGGER.info("total get interval size={}", count);
    LOGGER.info("error interval size={}", errorCount);
  }
}
