package cn.bc.workflow.cache;

import cn.bc.core.util.SpringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-test.xml")
public class SpringCacheTest {
	private static final Logger logger = LoggerFactory.getLogger(SpringCacheTest.class);
	@Autowired
	private SpringCache service;

	@Test
	public void cache1() {
		debugCacheManager();

		service.cache1("1");
		service.cache1("1");
		service.cache1("2");
		service.cache1("2");
		// null 值不缓存
		service.cache1(null);
		service.cache1(null);

		debugCacheManager();
	}

	private void debugCacheManager() {
		CacheManager cacheManager = SpringUtils.getBean(SimpleCacheManager.class);
		Map cacheMap;
		logger.info(cacheManager.getClass().toString());
		logger.info("	" + cacheManager.getCacheNames().toString());
		for (String name : cacheManager.getCacheNames()) {
			cacheMap = (ConcurrentMap) cacheManager.getCache(name).getNativeCache();
			logger.info("	" + name + ": " + cacheMap);
		}
	}

	@Test
	public void cache2() {
		service.cache1("1");
		service.noCache("1");
		service.cache1("1");
	}
}