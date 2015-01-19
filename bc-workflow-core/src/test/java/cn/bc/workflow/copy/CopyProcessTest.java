package cn.bc.workflow.copy;

import cn.bc.core.exception.CoreException;
import cn.bc.docs.domain.Attach;
import cn.bc.spider.Result;
import cn.bc.spider.callable.JsonCallable;
import cn.bc.spider.callable.StreamCallable;
import cn.bc.spider.http.TaskExecutor;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import org.json.JSONObject;
import org.junit.Assert;
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
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

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

	// 请求的分组名称
	String httpGroup = "copyProcess";
	// 复制源的url
	String fromUrl = "http://192.168.0.7:8081/bcsystem";

	// 复制流程实例数据
	@Test
	public void copyInstance() throws Exception {
		// 流程实例附件保存的根路径
		String COPY_TO_ROOT = Attach.DATA_REAL_PATH + "/" + FlowAttach.DATA_SUB_PATH;
		// 流程实例ID
		String instanceId = "3887597";

		// 获取流程实例的流程附件路径
		// 3887597 司机留用审批（陈勇辉2015-01-21)
		String sql = "select id as id, path_ as path from bc_wf_attach where pid = '" + instanceId + "'";
		List<Map<String, Object>> attachs = this.jdbcTemplate2.queryForList(sql);
		logger.debug("attachs={}", attachs);
		String path;
		Integer id;
		InputStream in;
		File toFile, toDir;

		// 登录系统
		if(attachs != null && !attachs.isEmpty()) this.loginSourceSystem();

		// 复制流程实例附件
		for(Map<String, Object> attach : attachs){
			id = (Integer) attach.get("id");
			path = COPY_TO_ROOT + "/" + attach.get("path");
			toFile = new File(path);
			logger.debug("toFile={}", toFile);

			// 创建保存到的目录
			toDir = toFile.getParentFile();
			if(!toDir.exists()) toDir.mkdirs();

			// 获取附件流
			in = getFlowAttach(fromUrl, id);

			// 复制流
			FileCopyUtils.copy(in, new FileOutputStream(toFile));
		}
	}

	// 系统登录
	private void loginSourceSystem() throws Exception {
		JsonCallable c = new JsonCallable();
		c.setMethod("post");
		c.setGroup(httpGroup);
		String url = fromUrl + "/doLogin";
		c.setUrl(url);
		String userCode = "dragon";// 登陆帐号
		String password = "21218cca77804d2ba1922c33e0151105";// 登录密码的MD5值
		c.addFormData("name", userCode);
		c.addFormData("password", password);
		logger.debug("login system with userCode={}, password={}", userCode, password);

		Result<JSONObject> result = TaskExecutor.get(c);
		Assert.assertTrue(result.isSuccess());
		JSONObject r = result.getData();
		logger.debug("login response={}", r.toString());
		if(!r.getBoolean("success")){
			throw new CoreException("登录失败，请设置正确的账号、密码！");
		}
	}

	private InputStream getFlowAttach(String fromUrl, Integer id) {
		StreamCallable c = new StreamCallable();
		c.setMethod("get");
		c.setGroup(httpGroup);
		String url = fromUrl + "/bc-workflow/flowattachfile/inline?id=" + id;
		c.setUrl(url);
		logger.debug("get attach stream from {}", url);

		Result<InputStream> result = TaskExecutor.get(c);
		Assert.assertTrue(result.isSuccess());
		return result.getData();
	}
}
