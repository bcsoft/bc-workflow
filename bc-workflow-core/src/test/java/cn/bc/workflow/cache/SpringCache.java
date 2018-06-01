package cn.bc.workflow.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Spring 缓存测试
 */
@Component
public class SpringCache {
  private static final Logger logger = LoggerFactory.getLogger(SpringCache.class);

  // 不缓存 null 结果
  @Cacheable(value = "test", unless = "#result == null")
  public String cache1(String id) {
    logger.debug("cache1 id={}", id);
    return id;
  }

  public String noCache(String id) {
    logger.debug("noCache id={}", id);
    return this.cache1(id);
  }
}