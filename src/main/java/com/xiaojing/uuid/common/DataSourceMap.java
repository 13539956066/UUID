package com.xiaojing.uuid.common;


import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

/**
 * Created by xiaojing on 17/4/25.
 */
public class DataSourceMap {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMap.class);
  public static final Map<String, DataSource> dataSourceMap = new HashMap<>();

  static {
    dataSourceMap.put("dataSource",getDataSource("db.properties"));
  }



  private static DataSource getDataSource(String fileName) {

    LOGGER.info("init datasource file {}", fileName);

    DataSource dataSource = null;

    try {
      dataSource = BasicDataSourceFactory.createDataSource(ConfigUtil.loadProperties(fileName));
    } catch (Exception e) {

      LOGGER.error("init datasource file. e:", e);
    }
    return dataSource;
  }


  public static Connection getConnection() throws SQLException {
    DataSource dataSource = dataSourceMap.get("dataSource");
    return dataSource.getConnection();
  }
}
