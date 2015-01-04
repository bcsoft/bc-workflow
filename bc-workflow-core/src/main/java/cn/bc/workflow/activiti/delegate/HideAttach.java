package cn.bc.workflow.activiti.delegate;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

/**
 * 隐藏工作空间的附件区，设置本地变量“hideAttach”等于“true”
 *
 * @author Action
 */
public class HideAttach implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setVariableLocal("hideAttach", true);
    }
}
