package com.aliyun.openservices.ots.samples;

import com.aliyun.openservices.ClientException;
import com.aliyun.openservices.ServiceException;
import com.aliyun.openservices.ots.OTSClient;
import com.aliyun.openservices.ots.OTSErrorCode;
import com.aliyun.openservices.ots.model.ColumnValue;
import com.aliyun.openservices.ots.model.PrimaryKeyType;
import com.aliyun.openservices.ots.model.PrimaryKeyValue;
import com.aliyun.openservices.ots.model.Row;
import com.aliyun.openservices.ots.model.RowPutChange;
import com.aliyun.openservices.ots.model.SingleRowQueryCriteria;
import com.aliyun.openservices.ots.model.TableMeta;

/**
 * 该示例代码包含了如何创建、删除OTS表；
 * 如何向表中插入一条数据；
 * 以及如何从表中根据指定条件查询一条数据。
 *
 */
public class OTSSingleDataSample {

    private static final String COLUMN_GID_NAME = "gid";
    private static final String COLUMN_UID_NAME = "gid";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_ADDRESS_NAME = "address";
    private static final String COLUMN_AGE_NAME = "age";
    private static final String COLUMN_MOBILE_NAME = "mobile";

    public static void main(String args[]) {
        final String accessId = "<your access id>";
        final String accessKey = "<your access key>";
        OTSClient client = new OTSClient(accessId, accessKey); 
        final String tableName = "sampleTable";

        try{
            // 创建表
            createTable(client, tableName);

            // 注意：创建表只是提交请求，OTS创建表需要一段时间。
            // 这里简单地等待30秒，请根据您的实际逻辑修改。
            Thread.sleep(30000);

            // 插入一条数据。
            putData(client, tableName);
            // 再取回来看看。
            getRow(client, tableName);

        }catch(ServiceException e){
            System.err.println("操作失败，详情：" + e.getMessage());
            // 可以根据错误代码做出处理， OTS的ErrorCode定义在OTSErrorCode中。
            if (OTSErrorCode.QUOTA_EXHAUSTED.equals(e.getErrorCode())){
                System.err.println("超出存储配额。");
            }
            // Request ID和Host ID可以用于有问题时联系客服诊断异常。
            System.err.println("Request ID:" + e.getRequestId());
            System.err.println("Host ID:" + e.getHostId());
        }catch(ClientException e){
            // 可能是网络不好或者是返回结果有问题
            System.err.println("请求失败，详情：" + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        finally{
            // 不留垃圾。
            try {
                deleteTable(client, tableName);
            } catch (ServiceException e) {
                System.err.println("删除表格失败，原因：" + e.getMessage());
                e.printStackTrace();
            } catch (ClientException e) {
                System.err.println("删除表格请求失败，原因：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void createTable(OTSClient client, String tableName)
            throws ServiceException, ClientException{
        TableMeta tableMeta = new TableMeta(tableName);
        tableMeta.addPrimaryKey(COLUMN_GID_NAME, PrimaryKeyType.INTEGER);
        tableMeta.addPrimaryKey(COLUMN_UID_NAME, PrimaryKeyType.INTEGER);

        client.createTable(tableMeta);

        System.out.println("表已创建");
    }

    private static void putData(OTSClient client, String tableName)
            throws ServiceException, ClientException{

        // 使用RowPutChange通过OTSClient#putData接口插入一行数据；
        // 如果是删除一行数据或一行中指定的列的数据，
        // 请使用RowDeleteChange类型通过OTSClient#deleteData接口操作。
        RowPutChange rowChange = new RowPutChange();
        rowChange.addPrimaryKey(COLUMN_GID_NAME, PrimaryKeyValue.fromLong(1));
        rowChange.addPrimaryKey(COLUMN_UID_NAME, PrimaryKeyValue.fromLong(101));
        rowChange.addAttributeColumn(COLUMN_NAME_NAME, ColumnValue.fromString("张三"));
        rowChange.addAttributeColumn(COLUMN_MOBILE_NAME, ColumnValue.fromString("111111111"));
        rowChange.addAttributeColumn(COLUMN_ADDRESS_NAME, ColumnValue.fromString("中国某地"));
        rowChange.addAttributeColumn(COLUMN_AGE_NAME, ColumnValue.fromLong(20));

        client.putData(tableName, rowChange, null);

        System.out.println("成功插入数据");
    }

    private static void getRow(OTSClient client, String tableName)
            throws ServiceException, ClientException{

        SingleRowQueryCriteria criteria = new SingleRowQueryCriteria(tableName);
        criteria.addPrimaryKey(COLUMN_GID_NAME, PrimaryKeyValue.fromLong(1));
        criteria.addPrimaryKey(COLUMN_UID_NAME, PrimaryKeyValue.fromLong(101));
        criteria.addColumnNames(new String[] {
                COLUMN_NAME_NAME,
                COLUMN_ADDRESS_NAME,
                COLUMN_AGE_NAME
        });

        Row row = client.getRow(criteria, null);

        System.out.println(row.getColumns().get(COLUMN_NAME_NAME) + "的信息：");
        System.out.println("地址" + row.getColumns().get(COLUMN_ADDRESS_NAME));
        boolean adult = row.getColumns().get(COLUMN_AGE_NAME).toLong() >= 18;
        System.out.println("是否成年？" + (adult ? "是" : "否"));
    }

    private static void deleteTable(OTSClient client, String tableName)
            throws ServiceException, ClientException{

        client.deleteTable(tableName);

        System.out.println("表已删除");
    }
}
