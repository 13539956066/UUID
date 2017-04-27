package com.xiaojing.client;

import com.xiaojing.uuid.client.SnowFlakeIdGen;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by xiaojing on 17/4/27.
 */
public class TestUUIDGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestUUIDGenerator.class);

  @Test
  public void testUUIDGen(){
    try{

      //租期的长度设定为15s
      SnowFlakeIdGen snowFlakeIdGen = new SnowFlakeIdGen("127.0.0.1", 9999, "test", 1000 * 15L);

      while (true){
        try{
          TimeUnit.SECONDS.sleep(2);
          LOGGER.info("uuid={}", snowFlakeIdGen.nextId());
        }catch (Exception e){
          LOGGER.error("e=", e);
        }
      }
    }catch (Exception e){
      LOGGER.error("init error");
    }

  }
}
