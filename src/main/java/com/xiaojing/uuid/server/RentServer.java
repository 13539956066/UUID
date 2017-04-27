package com.xiaojing.uuid.server;

import com.xiaojing.uuid.bean.IntervalVO;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by xiaojing on 17/4/24.
 */
public class RentServer {



  /**
   * 对于两台不同的发号主机,可能存在着时间不一致的情况下,需要容忍这种情况的存在
   * 所以需要对两个不同的租户,在更换时容忍这种容错的措施(其实也没必要,我们使用的其实都是租期票据上的时间,不可能有id冲突)
   */
  public static final long revise = 5 * 60 * 1000 ;


  /**
   * 首次租用,得传入租用时间
   * 返回值包含,租用到的id,以及以中心侧为准的startTime & endTime
   * @param duration 租期,单位ms
   * @return Interval 包含id startTime endTime
   */
  public IntervalVO rentInterval(int dataCenterId, long duration){
    long start = System.currentTimeMillis();
    IntervalVO interval = getAvailableInterval(dataCenterId);
    return rentNewId(interval,start,duration);
  }

  /**
   * 继续租用此id,需要传入原本的id,以及需要续租的时间
   * @param interval 原有租期的信息
   * @param duration 需要续的长度
   * @return 返回最新的租期信息
   */
  public IntervalVO renewal(IntervalVO interval, long duration){

    interval = renewalInterval(interval, duration);
    return interval;
  }

  /**
   * 获取一个目前可用的id
   * 有两种情况: 1此id从未被使用过,2此id的租期已结束
   * 为了统一逻辑,统一使用2的逻辑,从头开始找,当找不到有用的已存在id时,进行id自增操作.
   * @return
   */
  private IntervalVO getAvailableInterval(int dataCenterId){

    /**
     * 1  从已经有的id中,查找,endTime小于当前时间的并返回
     * 2  如果返回有内容,则使用select for update   \\   或者是CAS去更新
     * 3  select for update必须使用同一个Connection,则是否意味着getAndUpdate这个操作,其实应该包装在一个数据库操作内?
     * 4  如果包含在数据库操作内的话,那意味着是不是和实现绑定的太死了
     *
     * todo 这个get操作是单独的get,还是直接将get 和 update放在一起?
     *
     */

    long current = System.currentTimeMillis();

    List<IntervalVO> list = new LinkedList<>();// = "select * from interval where end_time < current order by id";
    if(list.size() > 0){
      return list.get(0);
    }else {
      //list = "select * from interval order by id";
      int workId = list.get(0).getWorkerId()+1;
      long startTime = 0;
      //" insert into interval (id,start_time,end_time) values(id,startTime,current)"
      return new IntervalVO(dataCenterId,workId,startTime,current);
    }
  }


  /**
   * 传入租期,开始租用
   * @param interVal
   * @param start
   * @param duration
   * @return
   */
  public IntervalVO rentNewId(IntervalVO interVal,long start,long duration){

    if(start + duration <= interVal.getEndTime() ){
      throw new RuntimeException("");
    }

//    Connection connection;
//    getAvailableId();
//    selectForUpdate();//todo select for update被其他人持有时,表现是等待?如果是这样,依旧没有解决这个问题
//    update();//todo update必须使用CAS的方式,如果是这样,那么select for update感觉也没必要了,没有必要使用select for update,则意味着不需要同一个connection
//    select();
    return new IntervalVO();
  }

  public boolean updateWorkId(int id,long start,long duration){
    //todo update the storage
    //update CAS
    return true;
  }

  public IntervalVO renewalInterval(IntervalVO interval,long duration){

    long endTime = interval.getEndTime() + duration;
    boolean success = false;
    //"update interval set end_time = :entTime where id = :id and end_time = :originEnd" todo CAS更新

    if(success){
      return new IntervalVO(interval.getDataCenterId(),interval.getWorkerId(),interval.getStartTime(),endTime);
    }else {
      throw new RuntimeException();
    }
  }

}
