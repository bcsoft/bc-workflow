package cn.bc.workflow.copy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**'
 * 复制流程数据
 * Created by dragon on 2015/1/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class CopyProcessTest {
	private static final Logger logger = LoggerFactory.getLogger(CopyProcessTest.class);
	@Autowired
	@Qualifier("jdbcTemplate")
	private JdbcTemplate jdbcTemplate1; // 第1数据库

	@Autowired
	@Qualifier("jdbcTemplate2")
	private JdbcTemplate jdbcTemplate2;	// 第2数据库

	// 复制流程实例数据
	@Test
	public void copyInstance(){
		String sql = "select name from bc_identity_actor where code = 'dragon'";
		String name = this.jdbcTemplate2.queryForObject(sql, String.class);
		logger.debug("name={}", name);
	}
}
