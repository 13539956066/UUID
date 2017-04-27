package com.xiaojing.uuid.server;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Created by xiaojing on 17/4/26.
 */
public class ContextConfig {
  private static final ContextConfig INSTANCE = new ContextConfig();

  private final Injector injector;


  private ContextConfig() {

    injector = Guice.createInjector();
  }


  public static ContextConfig getInstance() {
    return INSTANCE;
  }

  public Injector getInjector() {
    return injector;
  }

}
