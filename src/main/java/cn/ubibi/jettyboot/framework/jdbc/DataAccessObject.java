package cn.ubibi.jettyboot.framework.jdbc;

import cn.ubibi.jettyboot.framework.commons.*;
import cn.ubibi.jettyboot.framework.commons.ifs.CharFilter;
import cn.ubibi.jettyboot.framework.commons.model.Page;
import cn.ubibi.jettyboot.framework.jdbc.model.DataModifyListener;
import cn.ubibi.jettyboot.framework.jdbc.model.SingleConnectionFactory;
import cn.ubibi.jettyboot.framework.jdbc.model.UpdateResult;
import cn.ubibi.jettyboot.framework.jdbc.utils.ResultSetParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.*;


/**
 * 1.如果想动态选择DB，可以在ConnectionFactory中实现
 * 2.如果想动态选择schemaName,可以在子类中的schemaTableName方法实现.
 * <p>
 * 固定表名
 *
 * @param <T> ORM的类名
 */
public class DataAccessObject<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessObject.class);

    protected Class<T> clazz;
    protected String tableName;
    protected String selectFields = "*";
    protected String schemaName = "";
    protected DataAccess dataAccess;


    /**
     * 创建一个数据访问对象
     *
     * @param clazz                 ORM的类
     * @param tableName             表名
     * @param connectionFactoryName 如果想动态选择DB，可以在ConnectionFactory中实现
     */
    public DataAccessObject(Class<T> clazz, String tableName, String connectionFactoryName) {
        this.clazz = clazz;
        this.tableName = tableName;
        ConnectionFactory connectionFactory = FrameworkConfig.getInstance().getConnectionFactory(connectionFactoryName);
        this.dataAccess = new DataAccess(connectionFactory);
    }


    /**
     * 创建一个数据访问对象
     *
     * @param clazz     ORM的类
     * @param tableName 表名
     */
    public DataAccessObject(Class<T> clazz, String tableName) {
        this(clazz, tableName, FrameworkConfig.DEFAULT_CONNECTION_FACTORY_NAME);
    }


    /**
     * 创建一个数据访问对象
     *
     * @param clazz             ORM的类
     * @param tableName         表名
     * @param connectionFactory 如果想动态选择DB，可以在ConnectionFactory中实现
     */
    public DataAccessObject(Class<T> clazz, String tableName, ConnectionFactory connectionFactory) {
        this.clazz = clazz;
        this.tableName = tableName;
        this.dataAccess = new DataAccess(connectionFactory);
    }

    @Deprecated
    public DataAccessObject(Class<T> clazz, String tableName, Connection connection) {
        this.clazz = clazz;
        this.tableName = tableName;
        this.dataAccess = new DataAccess(new SingleConnectionFactory(connection));
    }

    public DataAccess getDataAccess() {
        return this.dataAccess;
    }


    /**
     * protected方法方便子类扩展
     * 如果想动态改变schemaName可以在子类中的schemaTableName方法实现
     *
     * @return select from 后面的表名
     */
    protected String schemaTableName() {
        if (schemaName == null || schemaName.isEmpty()) {
            return tableName;
        }
        return schemaName + "." + tableName;
    }


    public DataAccessObject() {
    }


    public void setDataModifyListener(DataModifyListener dataModifyListener) {
        this.dataAccess.setDataModifyListener(dataModifyListener);
    }

    /**
     * 请在子类的构造方法中调用
     *
     * @param resultSetParser
     */
    protected void setResultSetParser(ResultSetParser<T> resultSetParser) {
        this.dataAccess.setResultSetParser(resultSetParser);
    }


    //克隆的是一个子类对象
    //子类对象必须实现一个无参构造方法
    public Object clone() {
        try {
            //获取的是子类的Class
            Object o = this.getClass().newInstance();
            BeanUtils.copyField(o, this);
            return o;
        } catch (IllegalAccessException e) {
            LOGGER.error("", e);
        } catch (InstantiationException e) {
            LOGGER.error("", e);
        }
        return null;
    }


    public T findById(Object id) throws Exception {
        return findOneByWhere("where `id` = ?", id);
    }

    public T findOneByWhere(String whereSql, Object... args) throws Exception {
        whereSql = whereSql + " limit 0,1";
        List<T> list = findByWhere(whereSql, args);
        return CollectionUtils.getFirstElement(list);
    }

    public List<T> findAll() throws Exception {
        return findByWhere("");
    }


    public List<T> findByIdList(List idList) throws Exception {
        return findByIdList("id", idList, new DefaultIdCharFilter());
    }

    public List<T> findByIdList(String idFieldName, List idList) throws Exception {
        return findByIdList(idFieldName, idList, new DefaultIdCharFilter());
    }

    public List<T> findByIdList(List idList, CharFilter idCharFilter) throws Exception {
        return findByIdList("id", idList, idCharFilter);
    }

    public List<T> findByIdList(String idFieldName, List idList, CharFilter idCharFilter) throws Exception {

        //移除null
        idList = CollectionUtils.removeNull(idList);

        //移除重复的
        idList = CollectionUtils.uniqueList(idList);

        //过滤出合法的Id类型。避免SQL注入的问题。
        idList = CollectionUtils.filterOnlyLegalItems(idList, idCharFilter);

        if (CollectionUtils.isEmpty(idList)) {
            return new ArrayList<>();
        }

        StringParser stringParser = new StringParser() {
            @Override
            public String valueOf(Object o) {
                if (o instanceof String) {
                    return "'" + o.toString() + "'";
                }
                return String.valueOf(o);
            }
        };

        String idString = StringUtils.join(idList, ",", stringParser);
        String sql = "select " + selectFields + " from " + schemaTableName() + " where `" + idFieldName + "` in (" + idString + ")";
        List<Map<String, ?>> mapList = dataAccess.queryTemp(sql);
        return BeanUtils.mapListToBeanList(clazz, mapList);
    }


    public List<T> findByWhere(WhereSqlAndArgs whereSqlAndArgs) throws Exception {
        return findByWhere(whereSqlAndArgs.whereSql, whereSqlAndArgs.whereArgs);
    }

    public List<T> findByExample(T example) throws Exception {
        Map<String, Object> exampleMap = BeanUtils.beanToMap(example, true, true);
        return findByExample(exampleMap);
    }


    public List<T> findByExample(Map<String, Object> example) throws Exception {
        return findByWhere(toWhereSqlAndArgs(example));
    }

    public List<T> findByWhere(String whereSql, Object... args) throws Exception {
        String sql = "select " + selectFields + " from " + schemaTableName() + " " + whereSql;
        return dataAccess.query(clazz, sql, args);
    }

    public Page<T> findPageByExample(int pageNo, int pageSize, Map<String, Object> example) throws Exception {
        return findPageByExample(pageNo, pageSize, example, "");
    }

    public Page<T> findPageByExample(int pageNo, int pageSize, T example) throws Exception {
        Map<String, Object> exampleMap = BeanUtils.beanToMap(example, true, true);
        return findPageByExample(pageNo, pageSize, exampleMap, "");
    }

    public Page<T> findPageByExample(int pageNo, int pageSize, T example, String orderBy) throws Exception {
        Map<String, Object> exampleMap = BeanUtils.beanToMap(example, true, true);
        return findPageByExample(pageNo, pageSize, exampleMap, orderBy);
    }

    public Page<T> findPageByExample(int pageNo, int pageSize, Map<String, Object> example, String orderBy) throws Exception {
        WhereSqlAndArgs mm = toWhereSqlAndArgs(example);
        return findPage(pageNo, pageSize, mm.whereSql, orderBy, mm.whereArgs);
    }


    public Page<T> findPage(int pageNo, int pageSize) throws Exception {
        return findPage(pageNo, pageSize, "", "");
    }


    public Page<T> findPage(int pageNo, int pageSize, String whereSql, String orderBy, List<Object> whereArgs) throws Exception {
        Object[] whereArgArray = whereArgs.toArray(new Object[whereArgs.size()]);
        return findPage(pageNo, pageSize, whereSql, orderBy, whereArgArray);
    }

    /**
     * 分页查询
     *
     * @param pageNo    页号从零开始
     * @param pageSize  每夜多少条数据
     * @param whereSql  条件
     * @param orderBy   排序条件
     * @param whereArgs 条件参数
     * @return 返回Page对象
     * @throws Exception 可能的异常
     */
    public Page<T> findPage(int pageNo, int pageSize, String whereSql, String orderBy, Object... whereArgs) throws Exception {

        if (pageNo < 0) {
            pageNo = 0;
        }

        if (pageSize < 0) {
            pageSize = 30;
        }

        if (pageSize > FrameworkConfig.getInstance().getJbdcMaxPageRowSize()) {
            pageSize = FrameworkConfig.getInstance().getJbdcMaxPageRowSize();
        }


        if (whereSql == null) {
            whereSql = "";
        }
        if (orderBy == null) {
            orderBy = "";
        }

        int beginIndex = pageNo * pageSize;

        long totalCount = this.countByWhereSql(whereSql, whereArgs);

        //totalCount 为0的时候可以不查询
        List<T> dataList;
        if (totalCount > 0) {
            String sqlList = "select " + selectFields + " from " + schemaTableName() + " " + whereSql + " " + orderBy + " limit  " + beginIndex + "," + pageSize;
            dataList = dataAccess.query(clazz, sqlList, whereArgs);
        } else {
            dataList = new ArrayList<>();
        }


        Page<T> result = new Page(dataList, totalCount, pageNo, pageSize);
        return result;
    }


    /**
     * 统计整个表的大小
     *
     * @return 数量
     */
    public Long countAll() throws Exception {
        return countByWhereSql("");
    }


    /**
     * 判断对象是否存在
     *
     * @param example 查询条件
     * @return
     */
    public boolean exists(Map<String, Object> example) throws Exception {
        Long x = countByExample(example);
        return (x != null && x > 0);
    }


    public Long countByExample(Map<String, Object> example) throws Exception {
        WhereSqlAndArgs whereSqlArgs = toWhereSqlAndArgs(example);
        return countByWhereSql(whereSqlArgs.whereSql, whereSqlArgs.whereArgs);
    }


    /**
     * 统计数量多少
     *
     * @param whereSql  条件
     * @param whereArgs 条件参数
     * @return 数量
     */
    public Long countByWhereSql(String whereSql, Object... whereArgs) throws Exception {
        String sqlCount = "select count(0) from " + schemaTableName() + " " + whereSql;
        Object totalCount = dataAccess.queryValue(sqlCount, whereArgs);
        return (Long) CastBasicTypeUtils.toBasicTypeOf(totalCount, Long.class);
    }

    /**
     * 统计数量多少
     *
     * @param whereSql  条件
     * @param whereArgs 条件参数
     * @return 数量
     */
    public Long countByWhereSql(String whereSql, List<Object> whereArgs) throws Exception {
        Object[] whereArgArray = whereArgs.toArray(new Object[whereArgs.size()]);
        return countByWhereSql(whereSql, whereArgArray);
    }


    /**
     * 根据Id删除
     *
     * @param id bean id
     */
    public UpdateResult deleteById(Object id) throws Exception {
        return deleteByWhereSql("where `id`=?", id);
    }


    /**
     * 根据条件删除
     *
     * @param entity 查询条件,不包括null值，并自动驼峰转下划线
     * @return 操作结果
     */
    public UpdateResult deleteByExample(T entity) throws Exception {
        Map<String, Object> example = BeanUtils.beanToMap(entity, true, true);
        return deleteByExample(example);
    }


    /**
     * 根据条件删除
     *
     * @param example 查询条件
     * @return 操作结果
     */
    public UpdateResult deleteByExample(Map<String, Object> example) throws Exception {
        WhereSqlAndArgs mm = toWhereSqlAndArgs(example);
        return deleteByWhereSql(mm.whereSql, mm.whereArgs);
    }


    /**
     * 删除
     *
     * @param whereSql  条件
     * @param whereArgs 参数
     */
    public UpdateResult deleteByWhereSql(String whereSql, Object... whereArgs) throws Exception {
        String sql = "delete from " + schemaTableName() + " " + whereSql;
        return dataAccess.update(sql, whereArgs);
    }


    /**
     * 删除
     *
     * @param whereSql  条件
     * @param whereArgs 参数
     */
    public UpdateResult deleteByWhereSql(String whereSql, List<Object> whereArgs) throws Exception {
        Object[] whereArgArray = whereArgs.toArray(new Object[whereArgs.size()]);
        return deleteByWhereSql(whereSql, whereArgArray);
    }



    public UpdateResult updateById(T entity, Object id) throws Exception {
        Map<String, Object> newValues = BeanUtils.beanToMap(entity, true, true);
        return updateByWhereSql(newValues, "where `id` = ? ", id);
    }


    public UpdateResult updateById(Map<String, Object> newValues, Object id) throws Exception {
        return updateByWhereSql(newValues, "where `id` = ? ", id);
    }


    public UpdateResult updateByWhereSql(Map<String, Object> newValues, String whereSql, Object... whereArgs) throws Exception {
        if (!CollectionUtils.isEmpty(newValues)) {

            List[] keysValues = CollectionUtils.listKeyValues(newValues);
            List<String> keys = keysValues[0];
            List<Object> values = keysValues[1];
            List<String> keys2 = CollectionUtils.eachWrap(keys, "`", "`=?");
            String setSql = StringUtils.join(keys2, ",");


            String sql = "update " + schemaTableName() + " set " + setSql + " " + whereSql;
            if (whereArgs != null && whereArgs.length > 0) {
                List<Object> whereArgsList = Arrays.asList(whereArgs);
                values.addAll(whereArgsList);
            }

            return dataAccess.update(sql, values);
        }
        return new UpdateResult("params is empty");
    }


    public UpdateResult insertObject(T entity) throws Exception {
        return insertObject(entity, true, true);
    }


    /**
     * 保存对象
     *
     * @param entity         要保存的对象
     * @param isUnderlineKey 生成SQL时，自动将驼峰字段名转换为下划线
     * @param isIgnoreNull   忽略值为null的字段
     * @return
     * @throws Exception
     */
    public UpdateResult insertObject(T entity, boolean isUnderlineKey, boolean isIgnoreNull) throws Exception {
        Map<String, Object> map = BeanUtils.beanToMap(entity, isUnderlineKey, isIgnoreNull);
        return insertObject(map);
    }


    public UpdateResult insertObject(Map<String, Object> newValues) throws Exception {
        if (!CollectionUtils.isEmpty(newValues)) {

            List[] keysValues = CollectionUtils.listKeyValues(newValues);
            List<String> keys = keysValues[0];
            List<Object> values = keysValues[1];

            List<String> keys2 = CollectionUtils.eachWrap(keys, "`", "`");
            List<String> valuesQuota = CollectionUtils.repeatList("?", values.size());

            String filedSql = StringUtils.join(keys2, ",");
            String valuesSql = StringUtils.join(valuesQuota, ",");

            String sql = "insert into " + schemaTableName() + "(" + filedSql + ") values (" + valuesSql + ")";
            return dataAccess.update(sql, values);
        }

        return new UpdateResult("params is empty");
    }


    /**
     * 批量插入，使用循环调用的方式。
     * 优点：允许中间出现差错。
     * 缺点：效率低
     * 在调用此方法时,把它放在同一个事务里，效率会更好。
     *
     * @param objectList 循环插入的对象列表
     * @return 插入结果的集合
     */
    public List<UpdateResult> batchInsertUsingRepeat(List<Map<String, Object>> objectList) throws ConnectException {
        List<UpdateResult> results = new ArrayList<>();

        if (!CollectionUtils.isEmpty(objectList)) {

            for (Map<String, Object> obj : objectList) {

                UpdateResult result;
                try {
                    result = insertObject(obj);
                } catch (ConnectException ce) {
                    throw ce;
                } catch (Exception e) {
                    result = new UpdateResult(e.toString());
                }
                results.add(result);
            }
        }

        return results;
    }


    /**
     * 批量插入，拼接成一个大SQL。
     * 优点：高效
     * 缺点：中间出现一个异常会导致整批插入失败。
     *
     * @param objectList 需要插入大对象
     * @return Update Result
     */
    public UpdateResult batchInsertUsingLargeSQL(List<Map<String, Object>> objectList) throws Exception {

        objectList = CollectionUtils.removeEmptyMap(objectList);

        if (!CollectionUtils.isEmpty(objectList)) {

            Set<String> fieldKeys = CollectionUtils.getAllMapKeys(objectList);

            List<String> fieldKeysList = new ArrayList<>(fieldKeys);
            List<String> fieldKeysWList = CollectionUtils.eachWrap(fieldKeysList, "`", "`");
            String filedSql = StringUtils.join(fieldKeysWList, ",");


            List<String> valuesQuota = CollectionUtils.repeatList("?", fieldKeysList.size());
            String valuesSql = "(" + StringUtils.join(valuesQuota, ",") + ")"; // (?,?,?)


            List<String> allValuesSqlList = new ArrayList<>();
            List<Object> allValues = new ArrayList<>();
            for (Map<String, Object> object : objectList) {
                allValuesSqlList.add(valuesSql);
                for (String key : fieldKeysList) {
                    Object value = object.get(key);
                    allValues.add(value);
                }
            }

            String allValuesSql = StringUtils.join(allValuesSqlList, ",");
            String sql = "insert into " + schemaTableName() + " (" + filedSql + ") values " + allValuesSql;

            return dataAccess.update(sql, allValues);
        }


        return new UpdateResult("params is empty");
    }


    public UpdateResult saveOrUpdateById(Map<String, Object> newValues, Object id) throws Exception {
        return saveOrUpdate(newValues, "where `id` = ?", id);
    }


    public UpdateResult saveOrUpdate(Map<String, Object> newValues, String whereSql, Object... whereArgs) throws Exception {
        List<T> findResult = findByWhere(whereSql, whereArgs);
        if (findResult.isEmpty()) {
            return insertObject(newValues);
        } else {
            return updateByWhereSql(newValues, whereSql, whereArgs);
        }
    }


    protected WhereSqlAndArgs toWhereSqlAndArgs(Map<String, Object> example) {
        if (CollectionUtils.isEmpty(example)) {
            return new WhereSqlAndArgs("", new ArrayList<>());
        }

        List[] keysValues = CollectionUtils.listKeyValues(example);
        List<String> keys = keysValues[0];
        List<Object> values = keysValues[1];
        List<String> whereFields = CollectionUtils.eachWrap(keys, "`", "` = ?");
        String whereSql = "where " + StringUtils.join(whereFields, " and ");
        return new WhereSqlAndArgs(whereSql, values);
    }


    public T findOneByField(String fieldName, Object value) throws Exception {
        return this.findOneByWhere(toFieldWhereSql(fieldName), value);
    }

    public List<T> findListByField(String fieldName, Object value) throws Exception {
        return this.findByWhere(toFieldWhereSql(fieldName), value);
    }

    public UpdateResult deleteByField(String fieldName, Object value) throws Exception {
        return this.deleteByWhereSql(toFieldWhereSql(fieldName), value);
    }

    public UpdateResult updateByField(Map<String, Object> newValues,String fieldName, Object value) throws Exception {
        return this.updateByWhereSql(newValues,toFieldWhereSql(fieldName),value);
    }

    public Long countByField(String fieldName, Object value) throws Exception {
        return this.countByWhereSql(toFieldWhereSql(fieldName), value);
    }

    private String toFieldWhereSql(String fieldName) throws Exception {
        fieldName = fieldName.trim();
        if (fieldName.isEmpty()){
            throw new Exception("fieldName can not be empty");
        }
        return "where `" + fieldName + "` = ?";
    }

    protected static class WhereSqlAndArgs {
        public String whereSql;
        public Object[] whereArgs;

        public WhereSqlAndArgs(String whereSql, List<Object> whereArgs) {
            this.whereSql = whereSql;
            this.whereArgs = whereArgs.toArray();
        }
    }

    private static class DefaultIdCharFilter implements CharFilter {


        private static final char[] WHITE_LIST = {'-', '_', '~', '.'};

        /**
         * 判断是否是合法的ID允许出现的字符
         *
         * @param cc 字符
         * @return 是否合法
         */
        public boolean isOK(char cc) {

            if (cc >= 'A' && cc <= 'Z') {
                return true;
            }
            if (cc >= 'a' && cc <= 'z') {
                return true;
            }
            if (cc >= '0' && cc <= '9') {
                return true;
            }

            for (int i = 0; i < WHITE_LIST.length; i++) {
                if (cc == WHITE_LIST[i]) {
                    return true;
                }
            }

            return false;
        }
    }

}
