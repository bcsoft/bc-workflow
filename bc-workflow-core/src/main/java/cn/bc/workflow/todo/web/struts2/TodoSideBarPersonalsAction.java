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
				if(null != map.get("group_id_")){ //判断待办类型是否岗位  0:个人,1:岗位
					todoMap.put("todoType", 1);
				}else{
					todoMap.put("todoType", 0);
				}
				if(null != map.get("due_date_")){ //办理期限是否超时. 办理时间为空视为没有超时
					Date d1 = (Date) map.get("due_date_");
					Date d2 = new Date();
					todoMap.put("isTimeOver", d1.before(d2));
				}else{
					todoMap.put("isTimeOver", false);
				}
				todoMap.put("title", isNullObject(map.get("artName")));//待办事项
				if(null != map.get("create_time_")){
					Date d3 = (Date) map.get("create_time_");
					todoMap.put("createTime", DateUtils.formatDate(d3)); //发送时间
				}
				todoMap.put("description", isNullObject(map.get("description_")));//附加说明
				
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