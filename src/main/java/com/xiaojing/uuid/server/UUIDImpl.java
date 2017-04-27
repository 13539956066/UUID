package com.xiaojing.uuid.server;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.xiaojing.uuid.bean.Interval;
import com.xiaojing.uuid.bean.IntervalVO;
import com.xiaojing.uuid.bean.UUID;
import com.xiaojing.uuid.common.DataSourceMap;
import com.xiaojing.uuid.dao.IntervalDao;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.Connection;
import java.util.Map;

/**
 * Created by xiaojing on 17/4/25.
 */
public class UUIDImpl implements UUID.Iface {

  private static final Logger LOGGER = LoggerFactory.getLogger(UUIDImpl.class);


  @Inject
  IntervalDao intervalDao;


  private static final Map<String, Integer> NAME_SPACE_MAP = Maps.<String, Integer>newHashMap();
  private static final Map<String, Integer> DATA_CENTER_BIT_MAP = Maps.<String, Integer>newHashMap();
  private static final Map<String, Integer> WORKER_BIT_MAP = Maps.<String, Integer>newHashMap();
  private static final Map<String, Integer> SEQUENCE_BIT_MAP = Maps.<String, Integer>newHashMap();

  static {
    NAME_SPACE_MAP.put("test", 0);
    DATA_CENTER_BIT_MAP.put("test", 0);
    WORKER_BIT_MAP.put("test", 8);
    SEQUENCE_BIT_MAP.put("test", 10);
  }


  @Override
  public Interval rentInterval(String nameSpace, long duration) throws TException {
    Integer value = NAME_SPACE_MAP.get(nameSpace);
    if (null == value) {
      throw new TException("illegal name space");
    }
    int dataCenterId = value;

    int retry = 3;
    while (retry > 0) {
      Connection connection = null;
      try {
        connection = DataSourceMap.getConnection();
        IntervalVO intervalVO = intervalDao.getAvailableInterval(connection, dataCenterId, WORKER_BIT_MAP.get(nameSpace));
        intervalVO = intervalDao.rentNewId(connection, intervalVO, duration);
        Interval interval =  convertToInterval(intervalVO);
        interval.setDataCenterBits(DATA_CENTER_BIT_MAP.get(nameSpace));
        interval.setWorkerBits(WORKER_BIT_MAP.get(nameSpace));
        interval.setSequenceBits(SEQUENCE_BIT_MAP.get(nameSpace));
        return interval;
      } catch (Exception e) {
        LOGGER.warn("rent interval fail,e=", e);
        retry--;
      } finally {
        closeConnection(connection);
      }
    }
    throw new TException("fail");
  }


  @Override
  public Interval renewal(Interval interval, long duration) throws TException {

    int retry = 3;
    while (retry > 0) {
      Connection connection = null;
      try {
        connection = DataSourceMap.getConnection();
        IntervalVO intervalVO = convertToVo(interval);
        intervalVO = intervalDao.renewalInterval(connection, intervalVO, duration);
        return convertToInterval(intervalVO);
      } catch (Exception e) {
        LOGGER.warn("rent interval fail,e=", e);
        retry--;
      } finally {
        closeConnection(connection);
      }
    }
    throw new TException("fail");
  }

  @Override
  @Deprecated
  public Interval cancel(Interval interval) throws TException {
    return null;
  }


  private void closeConnection(Connection connection) {
    if (null != connection) {
      try {
        connection.close();
      } catch (Exception ignoreException) {

      }
    }
  }

  public static Interval convertToInterval(IntervalVO intervalVo,Interval interval) {

    int dataCenterId = intervalVo.getDataCenterId();
    int workerId = intervalVo.getWorkerId();
    long startTime = intervalVo.getStartTime();
    long endTime = intervalVo.getEndTime();
    int dataCenterBits = interval.getDataCenterBits();
    int workerIdBits = interval.getWorkerBits();
    int sequenceBits = interval.getSequenceBits();
    return new Interval(dataCenterId, workerId, startTime, endTime, dataCenterBits, workerIdBits, sequenceBits);
  }
  public static Interval convertToInterval(IntervalVO intervalVo) {

    int dataCenterId = intervalVo.getDataCenterId();
    int workerId = intervalVo.getWorkerId();
    long startTime = intervalVo.getStartTime();
    long endTime = intervalVo.getEndTime();
    return new Interval(dataCenterId, workerId, startTime, endTime, 0, 0, 0);
  }

  public static IntervalVO convertToVo(Interval interval) {
    int dataCenterId = interval.getDataCenterId();
    int workerId = interval.getWorkerId();
    long startTime = interval.getStartTime();
    long endTime = interval.getEndTime();
    return new IntervalVO(dataCenterId, workerId, startTime, endTime);
  }

}
