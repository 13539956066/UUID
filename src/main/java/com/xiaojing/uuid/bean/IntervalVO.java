package com.xiaojing.uuid.bean;

/**
 * Created by xiaojing on 17/4/26.
 */
public class IntervalVO {

  private int dataCenterId;
  private int workerId;
  private long startTime;
  private long endTime;

  public IntervalVO() {
  }

  public IntervalVO(int dataCenterId, int workerId, long startTime, long endTime) {
    this.dataCenterId = dataCenterId;
    this.workerId = workerId;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public int getWorkerId() {
    return workerId;
  }

  public void setWorkerId(int workerId) {
    this.workerId = workerId;
  }

  public int getDataCenterId() {
    return dataCenterId;
  }

  public void setDataCenterId(int dataCenterId) {
    this.dataCenterId = dataCenterId;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

}
