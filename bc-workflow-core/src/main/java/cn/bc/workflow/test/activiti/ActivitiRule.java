package cn.bc.workflow.test.activiti;

import org.junit.runners.model.FrameworkMethod;
import org.springframework.test.annotation.Rollback;

public class ActivitiRule extends org.activiti.engine.test.ActivitiRule {
	@Override
	public void finished(FrameworkMethod method) {
		Rollback rollbackAnnotation = method.getAnnotation(Rollback.class);
		if (rollbackAnnotation == null || rollbackAnnotation.value())
			super.finished(method);
	}
}
