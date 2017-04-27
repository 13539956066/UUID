package com.xiaojing.uuid.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.xiaojing.uuid.bean.IntervalVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by xiaojing on 17/4/25.
 */
public class IntervalDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(IntervalDao.class);

  @Inject
  private Random random;

  @Inject
  private DbUtil dbUtil;

  public IntervalVO getAvailableInterval(Connection connection, int dataCenterId,int workerBits) throws SQLException {

    IntervalVO available = getAvailable(connection, dataCenterId);
    if (null != available) {
      return available;
    }

    List<IntervalVO> list = queryMaxId(connection, dataCenterId);

    List<Integer> usedList = Lists.newArrayList();
    for(IntervalVO intervalVO : list){
      usedList.add(intervalVO.getWorkerId());
    }

    List<Integer> availableIdList = Lists.newArrayList();
    int max = 1<< workerBits ;
    for(int i=0;i < max; i++){
      availableIdList.add(i);
    }

    availableIdList.removeAll(usedList);
    int index = random.nextInt(availableIdList.size());

    int workerId = availableIdList.get(index);

    return insertNewInterval(connection, dataCenterId, workerId);
  }


  public IntervalVO rentNewId(Connection connection, IntervalVO interval, long duration) throws SQLException {
    long start = System.currentTimeMillis();
    long endTime = start + duration;
    if (endTime <= interval.getEndTime()) {
      throw new RuntimeException("illegal end time");
    }

    String sql = "update uuid.interval set start_time=:startTime, end_time=:endTime "
                 + "where data_center_id=:dataCenterId and worker_id = :workerId and end_time=:originEnd";

    Map<String, Object> params = Maps.newHashMap();
    params.put("startTime", start);
    params.put("endTime", endTime);
    params.put("dataCenterId", interval.getDataCenterId());
    params.put("workerId", interval.getWorkerId());
    params.put("originEnd", interval.getEndTime());
    int result = dbUtil.update(connection, sql, params);

    if (result > 0) {
      return new IntervalVO(interval.getDataCenterId(), interval.getWorkerId(), start, endTime);
    } else {
      throw new RuntimeException("sql exception");//todo 上游处理这种重试
    }

  }

  public IntervalVO renewalInterval(Connection connection, IntervalVO interval, long duration) throws SQLException {

    int dataCenterId = interval.getDataCenterId();
    int workerId = interval.getWorkerId();
    long startTime = interval.getStartTime();

    long endTime = interval.getEndTime() + duration;

    String sql = "update uuid.interval set end_time=:endTime where "
                 + "data_center_id=:dataCenterId and worker_id = :workerId and end_time=:originEnd";

    Map<String, Object> params = Maps.newHashMap();
    params.put("endTime", endTime);
    params.put("dataCenterId", dataCenterId);
    params.put("workerId", workerId);
    params.put("originEnd", interval.getEndTime());
    int result = dbUtil.update(connection, sql, params);

    if (result > 0) {
      return new IntervalVO(dataCenterId, workerId, startTime, endTime);
    } else {
      throw new RuntimeException("sql exception");//todo 上游处理这种重试
    }

  }


  private IntervalVO getAvailable(Connection connection, int dataCenterId) throws SQLException{
    long current = System.currentTimeMillis();

    String queryAvailable = "select * from uuid.interval "
                            + "where data_center_id = :dataCenterId and end_time < :current "
                            + "order by worker_id";

    Map<String, Object> params = Maps.newHashMap();
    params.put("dataCenterId", dataCenterId);
    params.put("current", current);

    List<IntervalVO> list = dbUtil.queryList(connection, queryAvailable, IntervalVO.class, params);
    if (list.size() > 0) {
      int index = random.nextInt(list.size());
      LOGGER.info("index={}",index);
      return list.get(index);
    } else {
      return null;
    }
  }


  private List<IntervalVO> queryMaxId(Connection connection, int dataCenterId) throws SQLException{
    String queryMaxId = "select * from uuid.interval "
                        + "where data_center_id=:dataCenterId order by worker_id desc";
    Map<String, Object> params = Maps.newHashMap();
    params.put("dataCenterId", dataCenterId);
    return dbUtil.queryList(connection, queryMaxId, IntervalVO.class, params);

  }

  private IntervalVO insertNewInterval(Connection connection, int dataCenterId, int workerId) throws SQLException {

    long current = System.currentTimeMillis();

    String insertSql = "insert into uuid.interval (data_center_id,worker_id,start_time,end_time) "
                       + "values (:dateCenterId,:workerId,:startTime,:endTime)";
    Map<String, Object> params = Maps.newHashMap();
    params.put("dateCenterId", dataCenterId);
    params.put("workerId", workerId);
    params.put("startTime", 0);
    params.put("endTime", current);

    int result = dbUtil.insert(connection, insertSql, params);
    if (result < 1) {
      throw new RuntimeException("db exception");//todo 上游重试,重新获取
    }

    return new IntervalVO(dataCenterId, workerId, 0, current);
  }


}
