package cn.bc.workflow.historictaskinstance.web.struts2;

import cn.bc.workflow.historictaskinstance.service.HistoricTaskInstanceService;
import com.google.gson.JsonObject;
import org.activiti.engine.HistoryService;
import org.activiti.engine.TaskService;
import org.commontemplate.util.Assert;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * 流程历史任务Action
 *
 * @author dragon
 *
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class HistoricTaskInstanceAction {
    public static final String JSON = "json";

    private HistoricTaskInstanceService historicTaskInstanceService;

    public String json;// json
    public String id;// 任务、流程实例等的id，视具体情况而定
    public String key;// 任务、流程的编码，视具体情况而定
    public String ver;// 任务、流程的版本号，视具体情况而定
    public String params;// 参数 格式为 字符串“流程Code1:流程变量名1,流程Code2:流程变量名2”

    @Autowired
    public void setHistoricTaskInstanceService(HistoricTaskInstanceService historicTaskInstanceService) {
        this.historicTaskInstanceService = historicTaskInstanceService;
    }

    /**
     * 查找历史流程任务变量值，变量为本地变量
     *
     * @return
     */
    public String findHisProcessTaskVarValue() {
        Assert.assertNotEmpty(id);      // id为流程实例id
        Assert.assertNotEmpty(params);

        // params 格式为 字符串“key:value,key:value,key:value”
        String[] keyAndVal = params.split(",");
        String[] keys = new String[keyAndVal.length];
        String[] vals = new String[keyAndVal.length];
        for (int i = 0; i < keyAndVal.length; i++) {
            keys[i] = keyAndVal[i].split(":")[0];
            vals[i] = keyAndVal[i].split(":")[1];
        }

        List<Map<String, Object>> listValues = this.historicTaskInstanceService.findHisProcessTaskVarValue(id, keys, vals);

        JSONArray jsonArray = new JSONArray(listValues);
        this.json = jsonArray.toString();

        return JSON;
    }
}
