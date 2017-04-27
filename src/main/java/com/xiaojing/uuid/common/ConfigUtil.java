/*
 * ©2014 北京小桔科技有限公司
 */

package com.xiaojing.uuid.common;


import com.xiaojing.uuid.config.ClassPathSearch;
import com.xiaojing.uuid.config.FilePathSearcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Properties;

/**
 * @author Christ
 * @ClassName: ConfigUtil
 * @date 2015-2-3 上午11:32:54
 */
public class ConfigUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtil.class);


  public static Properties loadProperties(String fileName) throws IOException {
    InputStream inputStream = null;
    Properties properties = new Properties();
    try {
      URL url = getURLByName(fileName);
      inputStream = url.openStream();
      properties.load(inputStream);

      return properties;
    } catch (Exception e) {
      LOGGER.error("load properties file error,filename={},e=", fileName, e);
      throw e;
    } finally {
      close(inputStream);
    }
  }

  public static <T> T loadYaml(String fileName, Class<T> clazz) throws IOException {
    InputStream inputStream = null;
    try {
      Yaml yaml = new Yaml();
      inputStream = getURLByName(fileName).openStream();
      return (T) yaml.load(inputStream);
    } catch (IOException e) {
      LOGGER.error("load yaml file error!,fileName={},e=", fileName, e);
      throw e;
    } finally {
      ConfigUtil.close(inputStream);
    }
  }

  public static LinkedHashMap<String, Object> loadYaml(String fileName) throws IOException {

    InputStream inputStream = null;
    try {
      URL url = getURLByName(fileName);
      inputStream = url.openStream();
      LinkedHashMap<String, Object> configMap = new Yaml().loadAs(inputStream, LinkedHashMap.class);

      if (configMap == null || configMap.isEmpty()) {
        return configMap;
      }
      return configMap;
    } catch (IOException ioe) {
      LOGGER.error("load yaml file error,fileName={},e=", fileName, ioe);
      throw ioe;
    } finally {
      close(inputStream);
    }
  }

  @Deprecated
  public static LinkedHashMap<String, Object> loadYamlByConfigFile(final String yamlConfigFile)
      throws Exception {
    File file = new File(yamlConfigFile);
    if (file.isFile()) {
      FileInputStream fin = null;
      try {
        Yaml yaml = new Yaml();
        fin = new FileInputStream(file);
        LOGGER.debug("loadYamlByConfigFile read config file from config dir,yamlConfigFile="
                     + yamlConfigFile);
        LinkedHashMap<String, Object> configMap = yaml.loadAs(fin, LinkedHashMap.class);

        if (configMap == null || configMap.isEmpty()) {
          return configMap;
        }

        return configMap;
      } catch (Exception e) {
        LOGGER.warn("loadYamlByConfigFile exception", e);
      } finally {
        close(fin);
      }
      return null;
    }

    throw new Exception("file is not exist,yamlConfigFile= " + yamlConfigFile);
  }


  public static URL getURLByName(String fileName) throws IOException {
    //首先从配置文件读取
    URL url = FilePathSearcher.getInstance().getURLByName(fileName);

    //配置文件夹为空时,从classpath读取
    if (null == url) {
      url = ClassPathSearch.getInstance().getURLByName(fileName);
    }
    if (null != url) {
      LOGGER.info("get url by filename success,url={}", url);
      return url;
    }
    //都读取不到流时,抛出异常
    throw new IOException("cannot find file " + fileName);
  }


  public static Properties loadPropertiesByPath(String path) throws IOException {
    File file = new File(path);
    if (file.isFile()) {
      InputStream inputStream = null;
      try {
        inputStream = new FileInputStream(file);
        Properties properties = new Properties();
        properties.load(inputStream);

        return properties;
      } catch (IOException ioe) {
        LOGGER.error("load properties from path fail,path={},e=", path, ioe);
      } finally {
        close(inputStream);
      }
    }
    LOGGER.error("cannot find path {}", path);
    throw new IOException("cannot find " + path);
  }

  public static LinkedHashMap<String, Object> loadYamlByPath(String path) throws IOException {
    File file = new File(path);
    if (file.isFile()) {
      InputStream inputStream = null;
      try {
        inputStream = new FileInputStream(file);
        LinkedHashMap<String, Object>
            configMap =
            new Yaml().loadAs(inputStream, LinkedHashMap.class);

        if (configMap == null || configMap.isEmpty()) {
          return configMap;
        }

        return configMap;
      } catch (IOException ioe) {
        LOGGER.error("load yaml from path fail,path={},e=", path, ioe);
      } finally {
        close(inputStream);
      }
    }
    LOGGER.error("cannot find path {}", path);
    throw new IOException("cannot find " + path);
  }

  public static void close(InputStream is) {
    if (is != null) {
      try {
        is.close();
      } catch (IOException e) {
        LOGGER.error("close input stream exception ", e);
      }
    }
  }
}
