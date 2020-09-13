package io.vertx.spi.cluster.zookeeper;

import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Stream.Liu
 */
public class MockZKCluster {

  private RetryPolicy retryPolicy = new ExponentialBackoffRetry(2000, 1, 8000);
  private TestingServer server;
  private Set<ZookeeperClusterManager> clusterManagers = new HashSet<>();

  static {
    System.setProperty("zookeeper.extendedTypesEnabled", "true");
  }

  public MockZKCluster() {
    try {
      server = new TestingServer(new InstanceSpec(null, -1, -1, -1, true, -1, -1, 120), true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JsonObject getDefaultConfig() {
    JsonObject config = new JsonObject();
    config.put("zookeeperHosts", server.getConnectString());
    config.put("sessionTimeout", 5000);
    config.put("connectTimeout", 3000);
    config.put("rootPath", "io.vertx");
    config.put("retry", new JsonObject()
      .put("initialSleepTime", 500)
      .put("maxTimes", 2));
    return config;
  }

  public void stop() {
    try {
      clusterManagers.forEach(clusterManager -> clusterManager.getCuratorFramework().close());
      clusterManagers.clear();
      server.restart();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ClusterManager getClusterManager() {
    CuratorFramework curator = CuratorFrameworkFactory.builder()
      .namespace("io.vertx")
      .sessionTimeoutMs(3000)
      .connectionTimeoutMs(1000)
      .connectString(server.getConnectString())
      .retryPolicy(retryPolicy).build();
    curator.start();
    //there is take up time for zk client thread start up.
    while (curator.getState() != CuratorFrameworkState.STARTED) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    ZookeeperClusterManager zookeeperClusterManager = new ZookeeperClusterManager(retryPolicy, curator);
    clusterManagers.add(zookeeperClusterManager);
    return zookeeperClusterManager;
  }
}
