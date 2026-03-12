package com.aliyun.openservices.ots.samples;

import java.util.ArrayList;
import java.util.List;

import com.aliyun.openservices.ClientException;
import com.aliyun.openservices.ServiceException;
import com.aliyun.openservices.ots.OTSClient;
import com.aliyun.openservices.ots.OTSErrorCode;
import com.aliyun.openservices.ots.OTSException;
import com.aliyun.openservices.ots.model.ColumnValue;
import com.aliyun.openservices.ots.model.PartitionKeyValue;
import com.aliyun.openservices.ots.model.PrimaryKeyType;
import com.aliyun.openservices.ots.model.PrimaryKeyValue;
import com.aliyun.openservices.ots.model.RangeRowQueryCriteria;
import com.aliyun.openservices.ots.model.Row;
import com.aliyun.openservices.ots.model.RowChange;
import com.aliyun.openservices.ots.model.RowPutChange;
import com.aliyun.openservices.ots.model.TableMeta;

public class OTSMultiDataSample {
    private static final String COLUMN_GID_NAME = "gid";
    private static final String COLUMN_UID_NAME = "uid";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_AGE_NAME = "age";

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

            // 插入多行数据。
            putMultiData(client, tableName);
            // 再取回来看看。
            getMultiData(client, tableName);
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
        tableMeta.addPrimaryKey(COLUMN_GID_NAME, PrimaryKeyType.INTEGER); // group id
        tableMeta.addPrimaryKey(COLUMN_UID_NAME, PrimaryKeyType.INTEGER); // user id

        client.createTable(tableMeta);

        System.out.println("表已创建");
    }

    private static void deleteTable(OTSClient client, String tableName)
            throws ServiceException, ClientException{

        client.deleteTable(tableName);

        System.out.println("表已删除");
    }

    private static void putMultiData(OTSClient client, String tableName)
            throws OTSException, ClientException{
        // 把多个RowPutChange对象放入到List中，通过OTSClient#batchModifyData
        // 接口来一次性插入多行数据。
        // 如果要批量删除数据，请使用RowDeleteChange和OTSClient#batchModifyData接口。
        List<RowChange> rowChanges = new ArrayList<RowChange>();
        int bid = 1;
        final int rowCount = 5;
        for(int i = 0; i < rowCount; ++i){
            RowPutChange rowPutChange = new RowPutChange();
            rowPutChange.addPrimaryKey(COLUMN_GID_NAME, PrimaryKeyValue.fromLong(bid));
            rowPutChange.addPrimaryKey(COLUMN_UID_NAME, PrimaryKeyValue.fromLong(i));
            rowPutChange.addAttributeColumn(COLUMN_NAME_NAME, ColumnValue.fromString("小" + Integer.toString(i + 1)));
            rowPutChange.addAttributeColumn(COLUMN_AGE_NAME, ColumnValue.fromLong(21 - i));

            rowChanges.add(rowPutChange);
        }

        // 批量操作必须在事务（Transaction）中进行，因此先开始一个事务
        // RowChange的List中所有数据的第一个主键的值必须与这里指定的PartitionKeyValue相同。
        String transactionId =
                client.startTransaction(tableName, PartitionKeyValue.fromLong(bid));

        client.batchModifyData(tableName, rowChanges, transactionId);

        // 提交事务以使数据操作生效
        client.commitTransaction(transactionId);

        System.out.println(String.format("成功插入%d行数据。", rowCount));
    }

    private static void getMultiData(OTSClient client, String tableName)
            throws OTSException, ClientException{
        // 演示一下如何按主键范围查找，这里查找uid从2-4（左开右闭）的数据
        RangeRowQueryCriteria criteria = new RangeRowQueryCriteria(tableName);
        criteria.addPrimaryKey(COLUMN_GID_NAME, PrimaryKeyValue.fromLong(1));
        criteria.setRange(COLUMN_UID_NAME, PrimaryKeyValue.fromLong(2), PrimaryKeyValue.fromLong(4));

        List<Row> rows = client.getRowsByRange(criteria, null);
        for(Row row : rows){
            boolean adult = row.getColumns().get(COLUMN_AGE_NAME).toLong() >= 18;
            System.out.print(row.getColumns().get(COLUMN_NAME_NAME) + "是否成年：");
            System.out.println(adult ? "是" : "否");
        }
    }
}
