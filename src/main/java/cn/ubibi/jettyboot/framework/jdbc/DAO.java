package cn.ubibi.jettyboot.framework.jdbc;

import cn.ubibi.jettyboot.framework.commons.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class DAO<T> {

    private Class<T> clazz;
    private String tableName;
    private String schemaName;
    private DBAccess dbAccess;


    public DAO(Class<T> clazz, String tableName, IConnectionFactory connectionFactory) {
        this.clazz = clazz;
        this.tableName = tableName;
        this.schemaName = "";
        this.dbAccess = new DBAccess(connectionFactory);
    }

    public DAO(Class<T> clazz, String tableName, Connection connection) {
        this.clazz = clazz;
        this.tableName = tableName;
        this.schemaName = "";
        this.dbAccess = new DBAccess(connection);
    }

    private String schemaTableName() {
        if (schemaName == null || schemaName.isEmpty()) {
            return tableName;
        }
        return schemaName + "." + tableName;
    }

    private DAO() {
    }

    public Object clone() {

        try {
            //获取的是子类的Class
            Object o = this.getClass().newInstance();
            BeanUtils.copyField(o,this);
            return o;

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }


    public DAO<T> use(String tableName) {
        DAO dao = (DAO)this.clone();
        dao.tableName = tableName;
        return dao;
    }

    public DAO<T> useSchema(String schemaName) {
        DAO dao = (DAO)this.clone();
        dao.schemaName = schemaName;
        return dao;
    }

    public DAO<T> use(Connection connection) {
        DAO dao = (DAO)this.clone();
        dao.dbAccess = new DBAccess(connection);
        return dao;
    }

    public DAO<T> use(IConnectionFactory connSource) {
        DAO dao = (DAO)this.clone();
        dao.dbAccess = new DBAccess(connSource);
        return dao;
    }


    public T findById(Object id) throws Exception {
        String sql = "select * from " + schemaTableName() + " where id = ?";
        return dbAccess.queryObject(clazz, sql, id);
    }

    public List<T> findAll() throws Exception {
        return findByWhere("");
    }

    public List<T> findByWhere(WhereSqlAndArgs whereSqlAndArgs) throws Exception {
        return findByWhere(whereSqlAndArgs.whereSql, whereSqlAndArgs.whereArgs);
    }


    public List<T> findByWhere(String whereSql, Object... args) throws Exception {
        String sql = "select * from " + schemaTableName() + " " + whereSql;
        return dbAccess.query(clazz, sql, args);
    }


    public PageData<T> findPage(int pageNo, int pageSize) throws Exception {
        return findPage(pageNo, pageSize, "", "");
    }

    /**
     *
     * 分页查询
     *
     * @param pageNo    页号从零开始
     * @param pageSize  每夜多少条数据
     * @param whereSql  条件
     * @param orderBy 排序条件
     * @param whereArgs 条件参数
     * @throws Exception 可能的异常
     * @return 返回Page对象
     */
    public PageData<T> findPage(int pageNo, int pageSize, String whereSql, String orderBy, Object... whereArgs) throws Exception {

        if (pageNo < 0) {
            pageNo = 0;
        }

        if (pageSize < 0) {
            pageSize = 1;
        }

        if (whereSql == null) {
            whereSql = "";
        }
        if (orderBy == null) {
            orderBy = "";
        }

        int beginIndex = pageNo * pageSize;

        long totalCount = this.countByWhereSql(whereSql,whereArgs);

        //totalCount 为0的时候可以不查询
        List<T> dataList;
        if(totalCount > 0){
            String sqlList = "select * from " + schemaTableName() + " " + whereSql + " " + orderBy + " limit  " + beginIndex + "," + pageSize;
            dataList = dbAccess.query(clazz, sqlList, whereArgs);
        }else {
            dataList = new ArrayList<>();
        }



        PageData<T> result = new PageData(dataList, totalCount, pageNo, pageSize);
        return result;
    }


    /**
     * 统计整个表的大小
     * @return 数量
     */
    public Long countAll(){
        return countByWhereSql("");
    }


    /**
     * 统计数量多少
     * @param whereSql 条件
     * @param whereArgs 条件参数
     * @return 数量
     */
    public Long countByWhereSql( String whereSql, Object... whereArgs){
        String sqlCount = "select count(0) from " + schemaTableName() + " " + whereSql;
        Object totalCount = dbAccess.queryValue(sqlCount, whereArgs);
        Long totalCountLong = 0L;
        if (totalCount instanceof Long) {
            totalCountLong = (Long) totalCount;
        } else {
            totalCountLong = new StringWrapper(totalCount.toString()).toLong();
        }
        return totalCountLong;
    }

    /**
     * 根据Id删除
     *
     * @param id bean id
     */
    public void deleteById(Object id) {
        deleteByWhereSql("where id=?", id);
    }

    /**
     *  删除
     *
     * @param whereSql 条件
     * @param whereArgs 参数
     */
    public void deleteByWhereSql(String whereSql, Object... whereArgs) {
        String sql = "delete from " + schemaTableName() + " " + whereSql;
        dbAccess.update(sql, whereArgs);
    }


    public void updateById(Map<String, Object> newValues, Object id) {
        updateByWhereSql(newValues, "where id = ? ", id);
    }


    public void updateByWhereSql(Map<String, Object> newValues, String whereSql, Object... whereArgs) {
        if (newValues != null && !newValues.isEmpty()) {

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

            dbAccess.update(sql, values);
        }
    }


    /**
     * 在调用此方法时,把它放在同一个事务里，效率会更好
     *
     * @param objectList
     */
    public void insertObjectList(List<Map<String, Object>> objectList) {
        if (objectList != null && !objectList.isEmpty()) {
            for (Map<String, Object> obj : objectList) {
                insertObject(obj);
            }
        }
    }


    public void insertObject(Map<String, Object> newValues) {
        if (newValues != null && !newValues.isEmpty()) {

            List[] keysValues = CollectionUtils.listKeyValues(newValues);
            List<String> keys = keysValues[0];
            List<Object> values = keysValues[1];

            List<String> keys2 = CollectionUtils.eachWrap(keys, "`", "`");
            List<String> valuesQuota = CollectionUtils.repeatList("?", values.size());

            String filedSql = StringUtils.join(keys2, ",");
            String valuesSql = StringUtils.join(valuesQuota, ",");

            String sql = "insert into " + schemaTableName() + "(" + filedSql + ") values (" + valuesSql + ")";

            dbAccess.update(sql, values);
        }
    }


    public void saveOrUpdateById(Map<String, Object> newValues, Object id) throws Exception {
        saveOrUpdate(newValues, "where id = ?", id);
    }


    public void saveOrUpdate(Map<String, Object> newValues, String whereSql, Object... whereArgs) throws Exception {
        List<T> findResult = findByWhere(whereSql, whereArgs);
        if (findResult.isEmpty()) {
            insertObject(newValues);
        } else {
            updateByWhereSql(newValues, whereSql, whereArgs);
        }
    }


    protected WhereSqlAndArgs toWhereSqlAndArgs(Map<String, Object> condition) {
        List[] keysValues = CollectionUtils.listKeyValues(condition);
        List<String> keys = keysValues[0];
        List<Object> values = keysValues[1];
        List<String> whereFields = CollectionUtils.eachWrap(keys, "`", "` = ?");
        String whereSql = "where " + StringUtils.join(whereFields, " and ");
        return new WhereSqlAndArgs(whereSql, values);
    }

    protected static class WhereSqlAndArgs {
        public String whereSql;
        public List<Object> whereArgs;

        public WhereSqlAndArgs(String whereSql, List<Object> whereArgs) {
            this.whereSql = whereSql;
            this.whereArgs = whereArgs;
        }
    }
}
