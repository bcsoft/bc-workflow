/**
 * 
 */
package cn.bc.workflow.todo.web.struts2;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.util.DateUtils;


/**
 * 待办边栏Action
 * 
 * @author wis
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoSideBarPersonalsAction extends TodoPersonalsAction {
	private static final long serialVersionUID = 1L;

	public List<Map<String, Object>> todoList;
	
	@Override
	public String execute() throws Exception {
		List<Map<String, Object>> list = this.findList();
		
		if(null != list && list.size() > 0){
			Map<String,Object> todoMap = null;
			todoList = new ArrayList<Map<String,Object>>();
			
			for(Map<String, Object> map: list){
				todoMap = new HashMap<String, Object>();
				todoMap.put("taskId", isNullObject(map.get("id_"))); // 任务id
				todoMap.put("procInstId", isNullObject(map.get("procInstId"))); // 任务id
				if(null != map.get("dueDate")){ //办理期限是否超时. 办理时间为空视为没有超时
					Date d1 = (Date) map.get("dueDate");
					Date d2 = new Date();
					todoMap.put("isTimeOver", d1.before(d2));
					todoMap.put("dueDate", DateUtils.formatDateTime2Minute((Date)map.get("dueDate")));
				}else{
					todoMap.put("isTimeOver", false);
					todoMap.put("dueDate", null);
				}
				todoMap.put("title", isNullObject(map.get("taskName")));//待办事项
				todoMap.put("processSubject", map.get("subject"));//流程标题
				if(null != map.get("createTime")){
					Date d3 = (Date) map.get("createTime");
					todoMap.put("createTime", DateUtils.formatDateTime(d3)); //发送时间
				}
				todoMap.put("description", isNullObject(map.get("desc")));//附加说明
				todoMap.put("category",isNullObject(map.get("processName")));//所属分类
				todoMap.put("assignee", isNullObject(map.get("assignee"))); //任务处理人
//				if(null != map.get("group_id_")){ //判断所属分类,如果是所属岗位的任务:0,个人任务:1
//					todoMap.put("todoType", 0);
//				}else{
//					todoMap.put("todoType", 1);
//				}
				todoList.add(todoMap);
			}
		}
		
		//待办期限是否超时
		return SUCCESS;
	}
	
	public String isNullObject(Object obj) {
		if (null != obj) {
			return obj.toString();
		} else {
			return "";
		}
	}
}