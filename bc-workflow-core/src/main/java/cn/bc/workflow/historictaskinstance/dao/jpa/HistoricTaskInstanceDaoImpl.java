/**
 *
 */
package cn.bc.workflow.historictaskinstance.dao.jpa;

import cn.bc.workflow.historictaskinstance.dao.HistoricTaskInstanceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务监控Dao的实现
 *
 * @author lbj
 */
@Component
public class HistoricTaskInstanceDaoImpl implements HistoricTaskInstanceDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public List<String> findProcessNames(String account, boolean isDone) {
    String sql = "select c.name_";
    sql += " from act_hi_taskinst a";
    sql += " inner join act_re_procdef c on c.id_=a.proc_def_id_";
    sql += " where a.assignee_= ? ";
    if (isDone)
      sql += " and a.end_time_ is not null ";

    sql += " GROUP BY c.name_";

    return this.jdbcTemplate.queryForList(sql, new Object[]{account}, String.class);
  }

  public List<String> findProcessNames() {
    String sql = "select c.name_";
    sql += " from act_hi_taskinst a";
    sql += " inner join act_re_procdef c on c.id_=a.proc_def_id_";
    sql += " GROUP BY c.name_";

    return this.jdbcTemplate.queryForList(sql, String.class);
  }

  public List<String> findTaskNames(String account, boolean isDone) {
    String sql = "select a.name_";
    sql += " from act_hi_taskinst a";
    sql += " inner join act_re_procdef c on c.id_=a.proc_def_id_";
    sql += " where a.assignee_= ? ";
    if (isDone)
      sql += " and a.end_time_ is not null ";

    sql += " GROUP BY a.name_";

    return this.jdbcTemplate.queryForList(sql, new Object[]{account}, String.class);
  }

  public List<String> findTaskNames() {
    String sql = "select a.name_";
    sql += " from act_hi_taskinst a";
    sql += " GROUP BY a.name_";

    return this.jdbcTemplate.queryForList(sql, String.class);
  }

  public List<String> findTransactors(String processInstanceId,
                                      String[] includeTaskKeys, String[] exclusiveTaskKeys) {
    String sql = "select assignee_ as assignee";
    sql += " FROM act_hi_taskinst";
    sql += " where proc_inst_id_ = ?";
    if (includeTaskKeys == null && exclusiveTaskKeys == null)
      return this.jdbcTemplate.queryForList(sql,
        new Object[]{processInstanceId}, String.class);
    if (exclusiveTaskKeys == null && includeTaskKeys.length > 0) {
      String s = "";
      for (int i = 0; i < includeTaskKeys.length; i++) {
        s += "'" + includeTaskKeys[i] + "'";
        if (i < includeTaskKeys.length - 1)
          s += ",";
      }
      sql += " and task_def_key_ in(" + s + ")";
    }
    if (includeTaskKeys == null && exclusiveTaskKeys.length > 0) {
      String s = "";
      for (int i = 0; i < exclusiveTaskKeys.length; i++) {
        s += "'" + exclusiveTaskKeys[i] + "'";
        if (i < exclusiveTaskKeys.length - 1)
          s += ",";
      }
      sql += " and task_def_key_ not in(" + s + ")";
    }
    return this.jdbcTemplate.queryForList(sql,
      new Object[]{processInstanceId}, String.class);
  }

  @Override
  public List<Map<String, Object>> findHisProcessTaskVarValue(String processInstanceId, String[] taskKey, String[] varName) {
    String defKey = "";
    String name = "";
    for (int i = 0; i < taskKey.length; i++) {
      defKey += ("'" + taskKey[i] + "'");
      if (i < taskKey.length - 1)
        defKey += ",";
    }

    for (int i = 0; i < varName.length; i++) {
      name += ("'" + varName[i] + "'");
      if (i < varName.length - 1)
        name += ",";
    }

    String sql = "select a.name,d.name_,d.text_,t.task_def_key_";
    sql += " from act_hi_detail d";
    sql += " inner join act_hi_taskinst t on t.proc_inst_id_ = d.proc_inst_id_";
    sql += " left join bc_identity_actor a on a.code = t.assignee_";
    sql += " where d.proc_inst_id_ = ?";
    sql += " and t.task_def_key_ in (" + defKey + ")";
    sql += " and t.id_ = d.task_id_";
    sql += " and d.name_ in (" + name + ")";

    return this.jdbcTemplate.queryForList(sql, processInstanceId);
  }

  @Override
  public Date findProcessInstanceStartTime(String processInstanceId) {
    String sql = "select p.start_time_ :: date FROM act_hi_procinst p  where p.proc_inst_id_ = ? ";
    Date startDate = this.jdbcTemplate.queryForObject(sql, new Object[]{processInstanceId}, new RowMapper<Date>() {
      @Override
      public Date mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getDate("start_time_");
      }
    });
    return startDate;
  }

  @Override
  public Date findProcessInstanceTaskStartTime(String processInstanceId,
                                               String taskCode) {

    String sql = "SELECT t.end_time_ :: date "
      + " FROM act_hi_taskinst t "
      + " where t.proc_inst_id_ = ? "
      + " and t.task_def_key_ = ? "
      + " and not exists( "
      + " select 0 "
      + " FROM act_hi_taskinst t1 "
      + " where t1.proc_inst_id_ = ? "
      + " and t1.task_def_key_ = ? "
      + " and t.start_time_ < t1.start_time_ "
      + " ) "
      + " order by t.start_time_ desc ";
    Date startDate = this.jdbcTemplate.queryForObject(sql
      , new Object[]{processInstanceId, taskCode, taskCode, processInstanceId}
      , new RowMapper<Date>() {
        @Override
        public Date mapRow(ResultSet rs, int rowNum) throws SQLException {
          return rs.getDate("end_time_");
        }
      });

    return startDate;
  }

  @Override
  public Date findProcessInstanceTaskEndTime(String processInstanceId,
                                             List<String> taskCodes) {

    String variable = "";  //构建"?"的表达式并进行赋值

    for (int i = 0; i < taskCodes.size(); i++) {
      variable += i == taskCodes.size() - 1 ? " ? " : " ? ,";
    }
    Object[] args = new Object[taskCodes.size() * 2 + 2];
    for (int i = 0; i < taskCodes.size() * 2 + 2; i++) {
      if (i == 0 || i == taskCodes.size() + 1)
        args[i] = processInstanceId;
      else {
        if (i < taskCodes.size() + 1) {
          args[i] = taskCodes.get(i - 1);
        } else {
          args[i] = taskCodes.get((i - 2) % taskCodes.size());
        }
      }
    }
    String sql = "SELECT t.end_time_ :: date "
      + " FROM act_hi_taskinst t "
      + " where t.proc_inst_id_ = ? "
      + " and t.task_def_key_ in ( " + variable + " ) "
      + " and not exists( "
      + " select 0 "
      + " FROM act_hi_taskinst t1 "
      + " where t1.proc_inst_id_ = ? "
      + " and t1.task_def_key_ in (" + variable + ") "
      + " and t.start_time_ < t1.start_time_ "
      + " ) "
      + " order by t.start_time_ desc ";
    Date endDate = this.jdbcTemplate.queryForObject(sql
      , args
      , new RowMapper<Date>() {
        @Override
        public Date mapRow(ResultSet rs, int rowNum) throws SQLException {
          return rs.getDate("end_time_");
        }
      });
    return endDate;
  }
}