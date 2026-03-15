# --------------------------------------------------------
# Host:                         127.0.0.1
# Server version:               5.1.42-community
# Server OS:                    Win64
# HeidiSQL version:             6.0.0.3603
# Date/time:                    2012-08-07 17:07:48
# --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

# Dumping structure for procedure stock_zjs.clearTableData
DROP PROCEDURE IF EXISTS `clearTableData`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `clearTableData`()
BEGIN
declare i int;
declare cNum int;
declare tName varchar(50);
declare sqll varchar(500);
   
set i=0;
set cNum=99;
REPEAT
	
	set tName = concat("t_c_ext_index_",i);
	set @sqll = concat("delete from ",tName);
	PREPARE create_stmt from @sqll;
   EXECUTE create_stmt;
   set i = i+1;
UNTIL i > cNum
END REPEAT;

END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.createExtIndex
DROP PROCEDURE IF EXISTS `createExtIndex`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `createExtIndex`()
    COMMENT '公司编码'
BEGIN
declare i int;
declare cNum int;
declare tName varchar(50);
declare sqll varchar(500);
   
set i=0;
set cNum=99;
REPEAT	
	set tName = concat("t_c_ext_index_",i);
	/*set @sqll = concat("CREATE TABLE ",tName ,"(`ID` BIGINT(10) NOT NULL AUTO_INCREMENT COMMENT '自增长键',`COMPANY_CODE` VARCHAR(20) NULL DEFAULT NULL COMMENT '公司编码',`INDEX_CODE` VARCHAR(20) NULL DEFAULT NULL COMMENT '公司指标编码',`VALUE` DOUBLE(20,4) NULL DEFAULT NULL COMMENT '值',`TIME` DATETIME NULL DEFAULT NULL COMMENT '时间',`UPDATE_TIME` DATETIME NULL DEFAULT NULL COMMENT '生成时间',`INDEX_NAME` VARCHAR(50) NULL DEFAULT NULL COMMENT '指标名',PRIMARY KEY (`ID`),UNIQUE INDEX `ext_unique_key` (`COMPANY_CODE`, `TIME`, `INDEX_CODE`))");*/
	set @sqll = concat("CREATE TABLE ",tName ,"(`COMPANY_CODE` VARCHAR(20) NULL DEFAULT NULL COMMENT '公司编码',`INDEX_CODE` VARCHAR(20) NULL DEFAULT NULL COMMENT '公司指标编码',`VALUE` DOUBLE(20,4) NULL DEFAULT NULL COMMENT '值',`TIME` DATETIME NULL DEFAULT NULL COMMENT '时间',`UPDATE_TIME` DATETIME NULL DEFAULT NULL COMMENT '生成时间',`INDEX_NAME` VARCHAR(50) NULL DEFAULT NULL COMMENT '指标名',UNIQUE INDEX `ext_unique_key` (`COMPANY_CODE`, `TIME`, `INDEX_CODE`)) ENGINE=myisam");
	PREPARE create_stmt from @sqll;
   EXECUTE create_stmt;
   set i = i+1;
UNTIL i > cNum
END REPEAT;


END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.create_data
DROP PROCEDURE IF EXISTS `create_data`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `create_data`(IN `companyCode` VARCHAR(50))
BEGIN
DECLARE cName VARCHAR(50);
DECLARE i INT;
DECLARE cNum INT; 
DECLARE stop_flag INT DEFAULT 0;
DECLARE tIndexCode BIGINT;
DECLARE icode BIGINT;
declare code INT;




SET i=0;
SET cNum = 100;
REPEAT
set code = (2200+i*(10+rand()));
INSERT INTO `t_asset` ( `COMPANY_CODE`, `LIVE_ASSET`, `CURR_FUNDS`, `FINANCE_ASSET`, 
`THEORY_ACCEPT_BILL`, `THEORY_ACCEPT_ACCOUNTS`, `ADVANCE_PAY_FUND`, `THEORY_ACCEPT_INTEREST`, 
`THEORY_ACCEPT_DIVIDEND`, `OTHER_ACCEPT_FUNDS`, `STOCK`, `PER_ASSET_WITHIN_ONE_YEAR`, 
`OTHER_LIVE_ASSET`, `LIVE_ASSET_SUM`, `NON_LIVE_ASSET`, `SELL_FINANCE_ASSET`, `HOLD_EXPIRE_INVET`, 
`LONG_TERM_FUNDS`, `LONG_TERM_INTEREST_INVEST`, `INVEST_ESTATE`, `PERMANENT_ASSET`, `UNDER_CON_ENGINEER`, 
`ENGINEER_MATERIAL`, `PERMANENT_ASSET_CLEAR`, `PRODUCT_BEING_ASSET`, `OIL_GAS_ASSET`, `INVISIBLE_ASSET`, 
`DEVELOP_EXPENSE`, `PRESTIGE`, `LONG_TERM_UNAPPLIED_EXPENSE`, `DEFER_INCOME_TAX_ASSET`, `OTHER_PERMANMENT_ASSET`, 
`PERMANMENT_ASSET_SUM`, `ASSET_SUM`, `INDEBTED_OWER_CODE`, `TIME`, `UPDATE_TIME`) 
VALUES ( companyCode, (412400+(rand()+100)*(i+100)+i*(10+rand())), (41240.00+(rand()+100)*(i+100)+i*(10+rand())), (100.00+(rand()+100)*(i+100)+i*(10+rand())), (60.00+(rand()+100)*(i+100)+i*(10+rand())), (5140.00+(rand()+100)*(i+100)+i*150), (33100.00+(rand()+100)*(i+100)+i*(10+rand())), 
(8150.00+(rand()+100)*(i+100)+i*520), (4900.0+(rand()+100)*(i+100)+i*(10+rand())), (20110.00+(rand()+100)*(i+100)+i*(10+rand())), (1630220.00+(rand()+100)*(i+100)+i*150), (0.00+(rand()+100)*(i+100)+i*150), (7340.00+(rand()+100)*(i+100)+i*(10+rand())), (5110.00+(rand()+100)*(i+100)+i*(10+rand())), 
(670.00+(rand()+100)*(i+100)+i*510), (307160.0+(rand()+100)*(i+100)+i*(10+rand())), (27141330.0+(rand()+100)*(i+100)+i*(10+rand())), (2102320.0+(rand()+100)*(i+100)+i*(10+rand())), (350.00+(rand()+100)*(i+100)+i*150), (93430.00+(rand()+100)*(i+100)+i*(10+rand())), (42430.00+(rand()+100)*(i+100)+i*(10+rand())), 
(220.00+(rand()+100)*(i+100)+i*530), (220.00+(rand()+100)*(i+100)+i*150), (0650.0+(rand()+100)*(i+100)+i*(10+rand())), (50.00+(rand()+100)*(i+100)+i*(10+rand())), (180.00+(rand()+100)*(i+100)+i*(10+rand())), (11080.0+(rand()+100)*(i+100)+i*150), (880.00+(rand()+100)*(i+100)+i*150), 
(220.0+(rand()+100)*(i+100)+i*(10+rand())), (230.00+(rand()+100)*(i+100)+i*(10+rand())), (230.00+(rand()+100)*(i+100)+i*150), (180.0+(rand()+100)*(i+100)+i*(10+rand())), (26510.00+(rand()+100)*(i+100)+i*150), (7160.00+(rand()+100)*(i+100)+i*150), 
code, DATE_ADD('1990-12-30', INTERVAL (3*i) MONTH), now());

INSERT INTO 
`t_indebted_owner` (`COMPANY_CODE`,`FLOW_INDEBTED`, `SHORT_TERM_LOAN`, `FINANCE_INDEBTED`, `THEORY_PAY_BILL`, `THEORY_PAY_ACCOUNTS`, `ADVANCE_ACCEPT_FUND`, 
`THEORY_STAFF_SALARY`, `THEORY_TAX`, `THEORY_PAY_INTEREST`, `THEORY_PAY_DIVIDEND`, `OTHER_PAY_FUNDS`, `PER_INDEBTED_WITHEN_ONE_YEAR`, `OTHER_LIVE_INDEBTED`, 
`PERMANMENT_INDEBETED`, `LONG_TERM_LOAN`, `THEORY_BOND`, `LONG_TERM_PAY_BILL`, `SPECIAL_PAY_BILL`, `PREDICT_INDEBTED`, `DEFER_TAX_INDEBTED`, `OTHER_PERMANMENT_INDEBETED`, 
`PERMANMENT_INDEBETED_SUM`, `INDEBETED_SUM`, `OWNER_RIGHTS`, `REAL_ACCEPT_CAPITAL`, `CAPITAL_SURPLUS`, `INVENTORY_PART`, `PROFIT_SURPLUS`, `UNDISTRIBUTED_PROFIT`, `OWNER_RIGHTS_SUM`, 
`INDEBTED_OWER_CODE`,`TIME`, `UPDATE_TIME`) 
VALUES (companyCode, (220.0+(rand()+10)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), 
(220.0+(rand()+100)*(i+100)+i*(10+rand())), (20.0+(rand()+100)*(i+10)+i*(10+rand())), (220.0+(rand()+10)*(i+100)+i*(10+rand())), (220.0+(rand()+10)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), 
(220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), 
(220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+10)+i*(10+rand())), (220.0+(rand()+10)*(i+100)+i*(10+rand())), (220.0+(rand()+10)*(i+100)+i*(10+rand())), 
(20.0+(rand()+10)*(i+10)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), 
(220.0+(rand()+100)*(i+10)+i*(10+rand())), (20.0+(rand()+10)*(i+100)+i*(10+rand())), (220.0+(rand()+100)*(i+10)+i*(10+rand())), (220.0+(rand()+10)*(i+100)+i*(10+rand())), 
(220.0+(rand()+100)*(i+10)+i*(10+rand())), (20.0+(rand()+100)*(i+10)+i*(10+rand())), (220.0+(rand()+100)*(i+100)+i*(10+rand())), 
(220.0+(rand()+100)*(i+10)+i*(10+rand())), (220.0+(rand()+100)*(i+10)+i*(10+rand())), code, DATE_ADD('1990-12-30', INTERVAL (3*i) MONTH), now());
set i=i+1;
UNTIL i > cNum
END REPEAT;
END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.create_dictionary
DROP PROCEDURE IF EXISTS `create_dictionary`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `create_dictionary`(IN `tableName` VARCHAR(50), IN `indexType` INT, IN `sct` vARCHAR(50))
BEGIN
DECLARE cName VARCHAR(50);
DECLARE cCName VARCHAR(50);
DECLARE i INT;
DECLARE cNum INT; 
DECLARE stop_flag INT DEFAULT 0;
DECLARE tIndexCode BIGINT;
DECLARE icode BIGINT;
DECLARE cCur CURSOR FOR  SELECT COLUMN_NAME ,COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = tableName AND TABLE_SCHEMA = 'stock';
DECLARE CONTINUE HANDLER FOR NOT FOUND SET stop_flag = 1;

OPEN cCur;
SELECT COUNT(*) INTO cNum FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = tableName AND TABLE_SCHEMA = 'stock' ;

SET i=0;

REPEAT
FETCH cCur INTO cName,cCName;
IF cName !='id'
	THEN
	
	SELECT INDEX_CODE INTO tIndexCode FROM T_DATA_DIICTIONARY WHERE TABLE_NAME = tableName AND COLUMN_NAME = cName;
	
	IF tIndexCode is null || tIndexCode = 99999
		THEN
		SELECT MAX(INDEX_CODE) INTO icode FROM T_DATA_DIICTIONARY;

		IF icode is null
			THEN set icode = 0;
			ELSE SET icode = icode + 1;
			END IF;
		INSERT INTO T_DATA_DIICTIONARY (INDEX_CODE,TABLE_NAME,COLUMN_NAME,TYPE,COLUMN_CHI_NAME,SYSTEM_CHILD_TABLE_CODE) VALUES (icode,tableName,cName,indexType,cCName,sct);
		END IF;
	set tIndexCode = 99999;
	set i = i+1;
END IF;
UNTIL i > cNum
END REPEAT;
CLOSE cCur;
END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.create_tsc_company_sub
DROP PROCEDURE IF EXISTS `create_tsc_company_sub`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `create_tsc_company_sub`()
    COMMENT '公司与报表体系的关系表'
BEGIN
DECLARE industryCode VARCHAR(50);
DECLARE tsc VARCHAR(50);


set tsc='ts_00003';
INSERT INTO  t_table_system_company_sub (TABLE_SYSTEM_CODE,COMPANY_CODE,COMPANY_NAME)  select * from (SELECT tsc,COMPANY_CODE,COMPANY_NAME_CHI FROM T_COMPANY where INDUSTRY_CODE != '100010' and INDUSTRY_CODE != '100012') as tb;






END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.data_init
DROP PROCEDURE IF EXISTS `data_init`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `data_init`()
BEGIN
CALL `create_data`('11230');
CALL `create_data`('01230');
CALL `create_data`('600839');
CALL `create_data`('601919');
CALL `create_data`('300253');
END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.import2NormalDb
DROP PROCEDURE IF EXISTS `import2NormalDb`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `import2NormalDb`()
    COMMENT '把测试库的数据导入正式表中'
BEGIN

INSERT INTO `tb_asset_0290` 
(`ob_object_id`, `company_code`, `ob_orgname_0290`, `ob_declaredate_0290`, `ob_enddate_0290`, `time`, `f002v_0290`, `f003v_0290`, `f004v_0290`, 
`f005v_0290`, `f006n_0290`, `f007n_0290`, `f008n_0290`, `f009n_0290`, `f010n_0290`, `f011n_0290`, `f012n_0290`, `f013n_0290`, `f014n_0290`, `f015n_0290`,
 `f016n_0290`, `f017n_0290`, `f018n_0290`, `f019n_0290`, `f020n_0290`, `f021n_0290`, `f022n_0290`, `f023n_0290`, `f024n_0290`, `f025n_0290`, `f026n_0290`, `f027n_0290`, 
 `f028n_0290`, `f029n_0290`, `f030n_0290`, `f031n_0290`, `f032n_0290`, `f033n_0290`, `f034n_0290`, `f035n_0290`, `f036n_0290`, `f037n_0290`, `f038n_0290`, `f039n_0290`, 
 `f040n_0290`, `f041n_0290`, `f042n_0290`, `f043n_0290`, `f044n_0290`, `f045n_0290`, `f046n_0290`, `f047n_0290`, `f048n_0290`, `f049n_0290`, `f050n_0290`, `f051n_0290`, 
 `f052n_0290`, `f053n_0290`, `f054n_0290`, `f055n_0290`, `f056n_0290`, `f057n_0290`, `f058n_0290`, `f059n_0290`, `f060n_0290`, `f061n_0290`, `f062n_0290`, `f063n_0290`, 
 `f064n_0290`, `f066n_0290`, `f065n_0290`, `f067n_0290`, `f068n_0290`, `f069n_0290`, `f070n_0290`, `f071n_0290`, `ob_memo_0290`, `ob_rectime_0290`, `ob_modtime_0290`, 
 `ob_isvalid_0290`) 
 select * from tb_company_asset_0290;
 

 INSERT INTO `tb_cash_0292` (`ob_object_id`, `company_code`, `ob_orgname_0292`, `ob_declaredate_0292`, `ob_startdate_0292`, `ob_enddate_0292`, `time`, `f002v_0292`, 
 `f003v_0292`, `f004v_0292`, `f005v_0292`, `f006n_0292`, `f007n_0292`, `f008n_0292`, `f009n_0292`, `f010n_0292`, `f011n_0292`, `f012n_0292`, `f013n_0292`, `f014n_0292`, `f015n_0292`, 
 `f016n_0292`, `f017n_0292`, `f018n_0292`, `f019n_0292`, `f020n_0292`, `f021n_0292`, `f022n_0292`, `f023n_0292`, `f024n_0292`, `f025n_0292`, `f026n_0292`, `f027n_0292`, `f028n_0292`, 
 `f029n_0292`, `f030n_0292`, `f031n_0292`, `f032n_0292`, `f033n_0292`, `f034n_0292`, `f035n_0292`, `f036n_0292`, `f037n_0292`, `f038n_0292`, `f039n_0292`, `f040n_0292`, `f041n_0292`, 
 `f042n_0292`, `f043n_0292`, `f044n_0292`, `f045n_0292`, `f046n_0292`, `f047n_0292`, `f048n_0292`, `f049n_0292`, `f050n_0292`, `f051n_0292`, `f052n_0292`, `f053n_0292`, `f054n_0292`, 
 `f055n_0292`, `f056n_0292`, `f057n_0292`, `f058n_0292`, `f059n_0292`, `f060n_0292`, `f061n_0292`, `f062n_0292`, `f063n_0292`, `f064n_0292`, `f065n_0292`, `f066n_0292`, `f067n_0292`, 
 `f068n_0292`, `f069n_0292`, `f070n_0292`, `f071n_0292`, `ob_memo_0292`, `ob_rectime_0292`, `ob_modtime_0292`, `ob_isvalid_0292`) 
  select * from tb_company_cash_0292 ;
  
  INSERT INTO `tb_profile_0291` (`ob_object_id`, `company_code`, `ob_orgname_0291`, `ob_declaredate_0291`, `ob_startdate_0291`, `ob_enddate_0291`, 
  `time`, `f002v_0291`, `f003v_0291`, `f004v_0291`, `f005v_0291`, `f006n_0291`, `f007n_0291`, `f008n_0291`, `f009n_0291`, `f010n_0291`, `f011n_0291`, `f012n_0291`, 
  `f013n_0291`, `f014n_0291`, `f015n_0291`, `f016n_0291`, `f017n_0291`, `f018n_0291`, `f019n_0291`, `f020n_0291`, `f021n_0291`, `f022n_0291`, `f023n_0291`, `f024n_0291`, 
  `f025n_0291`, `f026n_0291`, `f027n_0291`, `f028n_0291`, `f029n_0291`, `f030n_0291`, `f031n_0291`, `f032n_0291`, `ob_memo_0291`, `ob_rectime_0291`, `ob_modtime_0291`, 
  `ob_isvalid_0291`)
   select * from tb_company_profile_0291 ;
  
END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.init_dictionary
DROP PROCEDURE IF EXISTS `init_dictionary`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `init_dictionary`()
BEGIN

CALL `create_dictionary`('t_asset_wind_a',0,'sct_0001');
CALL `create_dictionary`('t_cash_flow_wind_a',2,'sct_0002');
CALL `create_dictionary`('t_profile_wind_a',4,'sct_0003');
CALL `create_dictionary`('t_c_base_financial_index_wind_a',5,'sct_0004');


CALL `create_dictionary`('t_asset_wind_a_bk',0,'sct_0005');
CALL `create_dictionary`('t_cash_flow_wind_a_bk',2,'sct_0006');
CALL `create_dictionary`('t_profile_wind_a_bk',4,'sct_0007');
CALL `create_dictionary`('t_c_base_financial_index_wind_a_bk',5,'sct_0008');


update t_data_diictionary set type=-1 where column_name='COMPANY_NAME';
update t_data_diictionary set type=-1 where column_name='COMPANY_CODE';
update t_data_diictionary set type=-1 where column_name='UPDATE_TIME';
update t_data_diictionary set type=-1 where column_name='time';

END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.screenDataBuild
DROP PROCEDURE IF EXISTS `screenDataBuild`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `screenDataBuild`(IN `myColumn` VARCHAR(50))
BEGIN
DECLARE no_more_departments int;
DECLARE i INT DEFAULT 0;
DECLARE CIndexCode VARCHAR(50);
DECLARE CIndexName VARCHAR(50);
DECLARE CIndexCode2 VARCHAR(50);
DECLARE isColumnExist INT DEFAULT 0;

declare ind_cur cursor for select c_index_code,name from t_c_financial_index_rule where c_index_code > COALESCE(myColumn,'0') order by c_index_code  ;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET no_more_departments=1;
select count(name) from t_c_financial_index_rule;

drop table if exists tmp_table21;
CREATE TEMPORARY TABLE tmp_table21(    
`COMPANY_CODE` VARCHAR(50) NULL DEFAULT NULL COMMENT '公司编码',
`TIME` DATETIME NULL DEFAULT NULL COMMENT '时间',
`UPDATE_TIME` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
`value` DOUBLE NULL DEFAULT NULL COMMENT '货币资金',
UNIQUE INDEX `tmpkey` (`COMPANY_CODE`, `TIME`)
)	
COLLATE='utf8_general_ci'
ENGINE=MyISAM
ROW_FORMAT=DEFAULT;

open  ind_cur;
REPEAT

		FETCH NEXT FROM ind_cur INTO CIndexCode, CIndexName;
		select CIndexCode, CIndexName;
		
		select count(*) into isColumnExist from information_schema.columns 
		where table_name = 't_c_base_financial_index'
		and column_name = concat('Index_',CIndexCode);
		if isColumnExist =0 then
			SET @stmt = concat('ALTER TABLE `t_c_base_financial_index`  ADD COLUMN `Index_',CIndexCode,'` DOUBLE NULL DEFAULT NULL COMMENT \'',CIndexName ,'\';');  
			PREPARE s1 FROM @stmt;
			EXECUTE s1;  
		end if;
		call screenDataBuildByColumn(CIndexCode);

   
UNTIL no_more_departments
END REPEAT;
CLOSE ind_cur;
END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.screenDataBuildBaseByColumn
DROP PROCEDURE IF EXISTS `screenDataBuildBaseByColumn`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `screenDataBuildBaseByColumn`(IN `myColumn` VARCHAR(50))
    COMMENT '唐斌奇 将临时表数据经过判断后insert或update写入t_c_base_financial_index表'
BEGIN
DECLARE sql2 varchar(500);
DECLARE S_COMPANY_CODE varchar(50);
DECLARE S_TIME date;
DECLARE S_UPDATE_TIME date;
DECLARE S_VALUE double;
DECLARE no_more_departments int;

DECLARE ordernumbers CURSOR  FOR  select `COMPANY_CODE`,`TIME`,`UPDATE_TIME`,`VALUE` from tmp_table21;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET no_more_departments=1;  
SET no_more_departments=0; 
OPEN ordernumbers;
REPEAT   
	FETCH ordernumbers INTO S_COMPANY_CODE,S_TIME,S_UPDATE_TIME,S_VALUE; 	
	
  	if exists(SELECT company_code FROM t_c_base_financial_index WHERE company_code = S_COMPANY_CODE and `TIME` = S_TIME) then
		set sql2 = concat('update t_c_base_financial_index set company_code = \'',S_COMPANY_CODE,'\',`time`=\'',S_TIME,
		'\',update_time=\'',S_UPDATE_TIME,'\',`Index_',myColumn,'`=',S_VALUE,' where company_code =\'',S_COMPANY_CODE,'\' and `TIME` = \''
		,S_TIME ,'\';');
	else
		set sql2 = concat('insert into t_c_base_financial_index (company_code,`time`,update_time,`Index_',myColumn
		,'`)values(\'',S_COMPANY_CODE,'\',\'',S_TIME,'\',\'',S_UPDATE_TIME,'\',',S_VALUE,');');
	end if;
	
	set @sql2 = sql2;	
	PREPARE create_stmt from @sql2;
   EXECUTE create_stmt;	
UNTIL no_more_departments
END REPEAT; 
CLOSE ordernumbers; 

END//
DELIMITER ;


# Dumping structure for procedure stock_zjs.screenDataBuildByColumn
DROP PROCEDURE IF EXISTS `screenDataBuildByColumn`;
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `screenDataBuildByColumn`(IN `myColumn` VARCHAR(50))
    COMMENT '唐斌奇 按照指标Code来处理'
BEGIN
declare i int;
declare cNum int;
declare sqll varchar(500);
declare tmpCounts int;

declare SCompany varchar(50);
declare STime date;
declare SUpdateTime date;
declare SValue double;
   
set i=0;
set cNum=99;
REPEAT
	set @sqll = concat("replace into tmp_table21 (company_code,`time`,update_time,`value`)select company_code,`time`,update_time,value from t_c_ext_index_",i," where index_code = ",myColumn);
	PREPARE create_stmt from @sqll;
   EXECUTE create_stmt;
  
	-- 当单表的数据太多时，超过1000就处理
	select count(`COMPANY_CODE`) into tmpCounts from tmp_table21;
	if tmpCounts > 1000 then
	select i, myColumn ,count(`COMPANY_CODE`) from tmp_table21;
	call screenDataBuildBaseByColumn(myColumn);
	delete from tmp_table21;
	end if;
	
   set i = i+1;
UNTIL i >= cNum
END REPEAT;

	
	select count(`COMPANY_CODE`) into tmpCounts from tmp_table21;
	select myColumn ,count(`COMPANY_CODE`) from tmp_table21;
	if tmpCounts > 0 then	
	call screenDataBuildBaseByColumn(myColumn);
	delete from tmp_table21;
	end if;


END//
DELIMITER ;


# Dumping structure for table stock_zjs.tb_asset_0290
DROP TABLE IF EXISTS `tb_asset_0290`;
CREATE TABLE IF NOT EXISTS `tb_asset_0290` (
  `ob_object_id` bigint(20) DEFAULT NULL COMMENT '记录ID',
  `company_code` varchar(11) NOT NULL COMMENT '机构ID',
  `ob_orgname_0290` varchar(100) NOT NULL COMMENT '机构名称',
  `ob_declaredate_0290` datetime DEFAULT NULL COMMENT '公告日期',
  `ob_enddate_0290` datetime DEFAULT NULL COMMENT '截止日期',
  `time` datetime DEFAULT NULL COMMENT '报告年度',
  `f002v_0290` varchar(12) DEFAULT NULL COMMENT '合并类型编码',
  `f003v_0290` varchar(60) DEFAULT NULL COMMENT '合并类型',
  `f004v_0290` varchar(12) DEFAULT NULL COMMENT '报表来源编码',
  `f005v_0290` varchar(60) DEFAULT NULL COMMENT '报表来源',
  `f006n_0290` double(18,2) DEFAULT NULL COMMENT '货币资金',
  `f007n_0290` double(18,2) DEFAULT NULL COMMENT '交易性金融资产',
  `f008n_0290` double(18,2) DEFAULT NULL COMMENT '应收票据',
  `f009n_0290` double(18,2) DEFAULT NULL COMMENT '应收账款',
  `f010n_0290` double(18,2) DEFAULT NULL COMMENT '预付款项',
  `f011n_0290` double(18,2) DEFAULT NULL COMMENT '其他应收款',
  `f012n_0290` double(18,2) DEFAULT NULL COMMENT '应收关联公司款',
  `f013n_0290` double(18,2) DEFAULT NULL COMMENT '应收利息',
  `f014n_0290` double(18,2) DEFAULT NULL COMMENT '应收股利',
  `f015n_0290` double(18,2) DEFAULT NULL COMMENT '存货',
  `f016n_0290` double(18,2) DEFAULT NULL COMMENT '其中：消耗性生物资产',
  `f017n_0290` double(18,2) DEFAULT NULL COMMENT '一年内到期的非流动资产',
  `f018n_0290` double(18,2) DEFAULT NULL COMMENT '其他流动资产',
  `f019n_0290` double(18,2) DEFAULT NULL COMMENT '流动资产合计',
  `f020n_0290` double(18,2) DEFAULT NULL COMMENT '可供出售金融资产',
  `f021n_0290` double(18,2) DEFAULT NULL COMMENT '持有至到期投资',
  `f022n_0290` double(18,2) DEFAULT NULL COMMENT '长期应收款',
  `f023n_0290` double(18,2) DEFAULT NULL COMMENT '长期股权投资',
  `f024n_0290` double(18,2) DEFAULT NULL COMMENT '投资性房地产',
  `f025n_0290` double(18,2) DEFAULT NULL COMMENT '固定资产',
  `f026n_0290` double(18,2) DEFAULT NULL COMMENT '在建工程',
  `f027n_0290` double(18,2) DEFAULT NULL COMMENT '工程物资',
  `f028n_0290` double(18,2) DEFAULT NULL COMMENT '固定资产清理',
  `f029n_0290` double(18,2) DEFAULT NULL COMMENT '生产性生物资产',
  `f030n_0290` double(18,2) DEFAULT NULL COMMENT '油气资产',
  `f031n_0290` double(18,2) DEFAULT NULL COMMENT '无形资产',
  `f032n_0290` double(18,2) DEFAULT NULL COMMENT '开发支出',
  `f033n_0290` double(18,2) DEFAULT NULL COMMENT '商誉',
  `f034n_0290` double(18,2) DEFAULT NULL COMMENT '长期待摊费用',
  `f035n_0290` double(18,2) DEFAULT NULL COMMENT '递延所得税资产',
  `f036n_0290` double(18,2) DEFAULT NULL COMMENT '其他非流动资产',
  `f037n_0290` double(18,2) DEFAULT NULL COMMENT '非流动资产合计',
  `f038n_0290` double(18,2) DEFAULT NULL COMMENT '资产总计',
  `f039n_0290` double(18,2) DEFAULT NULL COMMENT '短期借款',
  `f040n_0290` double(18,2) DEFAULT NULL COMMENT '交易性金融负债',
  `f041n_0290` double(18,2) DEFAULT NULL COMMENT '应付票据',
  `f042n_0290` double(18,2) DEFAULT NULL COMMENT '应付账款',
  `f043n_0290` double(18,2) DEFAULT NULL COMMENT '预收款项',
  `f044n_0290` double(18,2) DEFAULT NULL COMMENT '应付职工薪酬',
  `f045n_0290` double(18,2) DEFAULT NULL COMMENT '应交税费',
  `f046n_0290` double(18,2) DEFAULT NULL COMMENT '应付利息',
  `f047n_0290` double(18,2) DEFAULT NULL COMMENT '应付股利',
  `f048n_0290` double(18,2) DEFAULT NULL COMMENT '其他应付款',
  `f049n_0290` double(18,2) DEFAULT NULL COMMENT '应付关联公司款',
  `f050n_0290` double(18,2) DEFAULT NULL COMMENT '一年内到期的非流动负债',
  `f051n_0290` double(18,2) DEFAULT NULL COMMENT '其他流动负债',
  `f052n_0290` double(18,2) DEFAULT NULL COMMENT '流动负债合计',
  `f053n_0290` double(18,2) DEFAULT NULL COMMENT '长期借款',
  `f054n_0290` double(18,2) DEFAULT NULL COMMENT '应付债券',
  `f055n_0290` double(18,2) DEFAULT NULL COMMENT '长期应付款',
  `f056n_0290` double(18,2) DEFAULT NULL COMMENT '专项应付款',
  `f057n_0290` double(18,2) DEFAULT NULL COMMENT '预计负债',
  `f058n_0290` double(18,2) DEFAULT NULL COMMENT '递延所得税负债',
  `f059n_0290` double(18,2) DEFAULT NULL COMMENT '其他非流动负债',
  `f060n_0290` double(18,2) DEFAULT NULL COMMENT '非流动负债合计',
  `f061n_0290` double(18,2) DEFAULT NULL COMMENT '负债合计',
  `f062n_0290` double(18,2) DEFAULT NULL COMMENT '实收资本（或股本）',
  `f063n_0290` double(18,2) DEFAULT NULL COMMENT '资本公积',
  `f064n_0290` double(18,2) DEFAULT NULL COMMENT '盈余公积',
  `f066n_0290` double(18,2) DEFAULT NULL COMMENT '减：库存股',
  `f065n_0290` double(18,2) DEFAULT NULL COMMENT '未分配利润',
  `f067n_0290` double(18,2) DEFAULT NULL COMMENT '少数股东权益',
  `f068n_0290` double(18,2) DEFAULT NULL COMMENT '外币报表折算价差',
  `f069n_0290` double(18,2) DEFAULT NULL COMMENT '非正常经营项目收益调整',
  `f070n_0290` double(18,2) DEFAULT NULL COMMENT '所有者权益（或股东权益）合计',
  `f071n_0290` double(18,2) DEFAULT NULL COMMENT '负债和所有者（或股东权益）合计',
  `ob_memo_0290` varchar(500) DEFAULT NULL COMMENT '备注',
  `ob_rectime_0290` datetime DEFAULT NULL COMMENT '录入时间',
  `ob_modtime_0290` datetime DEFAULT NULL COMMENT '修改时间',
  `ob_isvalid_0290` char(1) DEFAULT NULL COMMENT '是否有效',
  UNIQUE KEY `company_code` (`company_code`,`time`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='2007通用资产负债表--正式库';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.tb_cash_0292
DROP TABLE IF EXISTS `tb_cash_0292`;
CREATE TABLE IF NOT EXISTS `tb_cash_0292` (
  `ob_object_id` bigint(20) DEFAULT NULL COMMENT '记录ID',
  `company_code` varchar(11) NOT NULL COMMENT '机构ID',
  `ob_orgname_0292` varchar(100) NOT NULL COMMENT '机构名称',
  `ob_declaredate_0292` datetime DEFAULT NULL COMMENT '公告日期',
  `ob_startdate_0292` datetime DEFAULT NULL COMMENT '开始日期',
  `ob_enddate_0292` datetime DEFAULT NULL COMMENT '截止日期',
  `time` datetime DEFAULT NULL COMMENT '报告年度',
  `f002v_0292` varchar(12) DEFAULT NULL COMMENT '合并类型编码',
  `f003v_0292` varchar(60) DEFAULT NULL COMMENT '合并类型',
  `f004v_0292` varchar(12) DEFAULT NULL COMMENT '报表来源编码',
  `f005v_0292` varchar(60) DEFAULT NULL COMMENT '报表来源',
  `f006n_0292` double(18,2) DEFAULT NULL COMMENT '销售商品、提供劳务收到的现金',
  `f007n_0292` double(18,2) DEFAULT NULL COMMENT '收到的税费返还',
  `f008n_0292` double(18,2) DEFAULT NULL COMMENT '收到其他与经营活动有关的现金',
  `f009n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动现金流入小计',
  `f010n_0292` double(18,2) DEFAULT NULL COMMENT '购买商品、接受劳务支付的现金',
  `f011n_0292` double(18,2) DEFAULT NULL COMMENT '支付给职工以及为职工支付的现金',
  `f012n_0292` double(18,2) DEFAULT NULL COMMENT '支付的各项税费',
  `f013n_0292` double(18,2) DEFAULT NULL COMMENT '支付其他与经营活动有关的现金',
  `f014n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动现金流出小计',
  `f015n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动产生的现金流量净额',
  `f016n_0292` double(18,2) DEFAULT NULL COMMENT '收回投资收到的现金',
  `f017n_0292` double(18,2) DEFAULT NULL COMMENT '取得投资收益收到的现金',
  `f018n_0292` double(18,2) DEFAULT NULL COMMENT '处置固定资产、无形资产和其他长期资产收回的现金净额',
  `f019n_0292` double(18,2) DEFAULT NULL COMMENT '处置子公司及其他营业单位收到的现金净额',
  `f020n_0292` double(18,2) DEFAULT NULL COMMENT '收到其他与投资活动有关的现金',
  `f021n_0292` double(18,2) DEFAULT NULL COMMENT '投资活动现金流入小计',
  `f022n_0292` double(18,2) DEFAULT NULL COMMENT '购建固定资产、无形资产和其他长期资产支付的现金',
  `f023n_0292` double(18,2) DEFAULT NULL COMMENT '投资支付的现金',
  `f024n_0292` double(18,2) DEFAULT NULL COMMENT '取得子公司及其他营业单位支付的现金净额',
  `f025n_0292` double(18,2) DEFAULT NULL COMMENT '支付其他与投资活动有关的现金',
  `f026n_0292` double(18,2) DEFAULT NULL COMMENT '投资活动现金流出小计',
  `f027n_0292` double(18,2) DEFAULT NULL COMMENT '投资活动产生的现金流量净额',
  `f028n_0292` double(18,2) DEFAULT NULL COMMENT '吸收投资收到的现金',
  `f029n_0292` double(18,2) DEFAULT NULL COMMENT '取得借款收到的现金',
  `f030n_0292` double(18,2) DEFAULT NULL COMMENT '收到其他与筹资活动有关的现金',
  `f031n_0292` double(18,2) DEFAULT NULL COMMENT '筹资活动现金流入小计',
  `f032n_0292` double(18,2) DEFAULT NULL COMMENT '偿还债务支付的现金',
  `f033n_0292` double(18,2) DEFAULT NULL COMMENT '分配股利、利润或偿付利息支付的现金',
  `f034n_0292` double(18,2) DEFAULT NULL COMMENT '支付其他与筹资活动有关的现金',
  `f035n_0292` double(18,2) DEFAULT NULL COMMENT '筹资活动现金流出小计',
  `f036n_0292` double(18,2) DEFAULT NULL COMMENT '筹资活动产生的现金流量净额',
  `f037n_0292` double(18,2) DEFAULT NULL COMMENT '四、汇率变动对现金的影响',
  `f038n_0292` double(18,2) DEFAULT NULL COMMENT '四(2)、其他原因对现金的影响',
  `f039n_0292` double(18,2) DEFAULT NULL COMMENT '五、现金及现金等价物净增加额',
  `f040n_0292` double(18,2) DEFAULT NULL COMMENT '期初现金及现金等价物余额',
  `f041n_0292` double(18,2) DEFAULT NULL COMMENT '期末现金及现金等价物余额',
  `f042n_0292` double(18,2) DEFAULT NULL COMMENT '附注：',
  `f043n_0292` double(18,2) DEFAULT NULL COMMENT '1、将净利润调节为经营活动现金流量：',
  `f044n_0292` double(18,2) DEFAULT NULL COMMENT '净利润',
  `f045n_0292` double(18,2) DEFAULT NULL COMMENT '加：资产减值准备',
  `f046n_0292` double(18,2) DEFAULT NULL COMMENT '固定资产折旧、油气资产折耗、生产性生物资产折旧',
  `f047n_0292` double(18,2) DEFAULT NULL COMMENT '无形资产摊销',
  `f048n_0292` double(18,2) DEFAULT NULL COMMENT '长期待摊费用摊销',
  `f049n_0292` double(18,2) DEFAULT NULL COMMENT '处置固定资产、无形资产和其他长期资产的损失',
  `f050n_0292` double(18,2) DEFAULT NULL COMMENT '固定资产报废损失',
  `f051n_0292` double(18,2) DEFAULT NULL COMMENT '公允价值变动损失',
  `f052n_0292` double(18,2) DEFAULT NULL COMMENT '财务费用',
  `f053n_0292` double(18,2) DEFAULT NULL COMMENT '投资损失',
  `f054n_0292` double(18,2) DEFAULT NULL COMMENT '递延所得税资产减少',
  `f055n_0292` double(18,2) DEFAULT NULL COMMENT '递延所得税负债增加',
  `f056n_0292` double(18,2) DEFAULT NULL COMMENT '存货的减少',
  `f057n_0292` double(18,2) DEFAULT NULL COMMENT '经营性应收项目的减少',
  `f058n_0292` double(18,2) DEFAULT NULL COMMENT '经营性应付项目的增加',
  `f059n_0292` double(18,2) DEFAULT NULL COMMENT '其他',
  `f060n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动产生的现金流量净额2',
  `f061n_0292` double(18,2) DEFAULT NULL COMMENT '2、不涉及现金收支的重大投资和筹资活动：',
  `f062n_0292` double(18,2) DEFAULT NULL COMMENT '债务转为资本',
  `f063n_0292` double(18,2) DEFAULT NULL COMMENT '一年内到期的可转换公司债券',
  `f064n_0292` double(18,2) DEFAULT NULL COMMENT '融资租入固定资产',
  `f065n_0292` double(18,2) DEFAULT NULL COMMENT '3、现金及现金等价物净变动情况：',
  `f066n_0292` double(18,2) DEFAULT NULL COMMENT '现金的期末余额',
  `f067n_0292` double(18,2) DEFAULT NULL COMMENT '减：现金的期初余额',
  `f068n_0292` double(18,2) DEFAULT NULL COMMENT '加：现金等价物的期末余额',
  `f069n_0292` double(18,2) DEFAULT NULL COMMENT '减：现金等价物的期初余额',
  `f070n_0292` double(18,2) DEFAULT NULL COMMENT '加：其他原因对现金的影响2',
  `f071n_0292` double(18,2) DEFAULT NULL COMMENT '现金及现金等价物净增加额',
  `ob_memo_0292` varchar(500) DEFAULT NULL COMMENT '备注',
  `ob_rectime_0292` datetime DEFAULT NULL COMMENT '录入时间',
  `ob_modtime_0292` datetime DEFAULT NULL COMMENT '修改时间',
  `ob_isvalid_0292` char(1) DEFAULT NULL COMMENT '是否有效',
  UNIQUE KEY `company_code` (`company_code`,`time`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='通用现金流量表及补充资料2007版--正式库';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.tb_company_asset_0290
DROP TABLE IF EXISTS `tb_company_asset_0290`;
CREATE TABLE IF NOT EXISTS `tb_company_asset_0290` (
  `ob_object_id` bigint(20) DEFAULT NULL COMMENT '记录ID',
  `ob_orgid_0290` varchar(11) NOT NULL COMMENT '机构ID',
  `ob_orgname_0290` varchar(100) NOT NULL COMMENT '机构名称',
  `ob_declaredate_0290` datetime DEFAULT NULL COMMENT '公告日期',
  `ob_enddate_0290` datetime DEFAULT NULL COMMENT '截止日期',
  `f001d_0290` datetime DEFAULT NULL COMMENT '报告年度',
  `f002v_0290` varchar(12) DEFAULT NULL COMMENT '合并类型编码',
  `f003v_0290` varchar(60) DEFAULT NULL COMMENT '合并类型',
  `f004v_0290` varchar(12) DEFAULT NULL COMMENT '报表来源编码',
  `f005v_0290` varchar(60) DEFAULT NULL COMMENT '报表来源',
  `f006n_0290` double(18,2) DEFAULT NULL COMMENT '货币资金',
  `f007n_0290` double(18,2) DEFAULT NULL COMMENT '交易性金融资产',
  `f008n_0290` double(18,2) DEFAULT NULL COMMENT '应收票据',
  `f009n_0290` double(18,2) DEFAULT NULL COMMENT '应收账款',
  `f010n_0290` double(18,2) DEFAULT NULL COMMENT '预付款项',
  `f011n_0290` double(18,2) DEFAULT NULL COMMENT '其他应收款',
  `f012n_0290` double(18,2) DEFAULT NULL COMMENT '应收关联公司款',
  `f013n_0290` double(18,2) DEFAULT NULL COMMENT '应收利息',
  `f014n_0290` double(18,2) DEFAULT NULL COMMENT '应收股利',
  `f015n_0290` double(18,2) DEFAULT NULL COMMENT '存货',
  `f016n_0290` double(18,2) DEFAULT NULL COMMENT '其中：消耗性生物资产',
  `f017n_0290` double(18,2) DEFAULT NULL COMMENT '一年内到期的非流动资产',
  `f018n_0290` double(18,2) DEFAULT NULL COMMENT '其他流动资产',
  `f019n_0290` double(18,2) DEFAULT NULL COMMENT '流动资产合计',
  `f020n_0290` double(18,2) DEFAULT NULL COMMENT '可供出售金融资产',
  `f021n_0290` double(18,2) DEFAULT NULL COMMENT '持有至到期投资',
  `f022n_0290` double(18,2) DEFAULT NULL COMMENT '长期应收款',
  `f023n_0290` double(18,2) DEFAULT NULL COMMENT '长期股权投资',
  `f024n_0290` double(18,2) DEFAULT NULL COMMENT '投资性房地产',
  `f025n_0290` double(18,2) DEFAULT NULL COMMENT '固定资产',
  `f026n_0290` double(18,2) DEFAULT NULL COMMENT '在建工程',
  `f027n_0290` double(18,2) DEFAULT NULL COMMENT '工程物资',
  `f028n_0290` double(18,2) DEFAULT NULL COMMENT '固定资产清理',
  `f029n_0290` double(18,2) DEFAULT NULL COMMENT '生产性生物资产',
  `f030n_0290` double(18,2) DEFAULT NULL COMMENT '油气资产',
  `f031n_0290` double(18,2) DEFAULT NULL COMMENT '无形资产',
  `f032n_0290` double(18,2) DEFAULT NULL COMMENT '开发支出',
  `f033n_0290` double(18,2) DEFAULT NULL COMMENT '商誉',
  `f034n_0290` double(18,2) DEFAULT NULL COMMENT '长期待摊费用',
  `f035n_0290` double(18,2) DEFAULT NULL COMMENT '递延所得税资产',
  `f036n_0290` double(18,2) DEFAULT NULL COMMENT '其他非流动资产',
  `f037n_0290` double(18,2) DEFAULT NULL COMMENT '非流动资产合计',
  `f038n_0290` double(18,2) DEFAULT NULL COMMENT '资产总计',
  `f039n_0290` double(18,2) DEFAULT NULL COMMENT '短期借款',
  `f040n_0290` double(18,2) DEFAULT NULL COMMENT '交易性金融负债',
  `f041n_0290` double(18,2) DEFAULT NULL COMMENT '应付票据',
  `f042n_0290` double(18,2) DEFAULT NULL COMMENT '应付账款',
  `f043n_0290` double(18,2) DEFAULT NULL COMMENT '预收款项',
  `f044n_0290` double(18,2) DEFAULT NULL COMMENT '应付职工薪酬',
  `f045n_0290` double(18,2) DEFAULT NULL COMMENT '应交税费',
  `f046n_0290` double(18,2) DEFAULT NULL COMMENT '应付利息',
  `f047n_0290` double(18,2) DEFAULT NULL COMMENT '应付股利',
  `f048n_0290` double(18,2) DEFAULT NULL COMMENT '其他应付款',
  `f049n_0290` double(18,2) DEFAULT NULL COMMENT '应付关联公司款',
  `f050n_0290` double(18,2) DEFAULT NULL COMMENT '一年内到期的非流动负债',
  `f051n_0290` double(18,2) DEFAULT NULL COMMENT '其他流动负债',
  `f052n_0290` double(18,2) DEFAULT NULL COMMENT '流动负债合计',
  `f053n_0290` double(18,2) DEFAULT NULL COMMENT '长期借款',
  `f054n_0290` double(18,2) DEFAULT NULL COMMENT '应付债券',
  `f055n_0290` double(18,2) DEFAULT NULL COMMENT '长期应付款',
  `f056n_0290` double(18,2) DEFAULT NULL COMMENT '专项应付款',
  `f057n_0290` double(18,2) DEFAULT NULL COMMENT '预计负债',
  `f058n_0290` double(18,2) DEFAULT NULL COMMENT '递延所得税负债',
  `f059n_0290` double(18,2) DEFAULT NULL COMMENT '其他非流动负债',
  `f060n_0290` double(18,2) DEFAULT NULL COMMENT '非流动负债合计',
  `f061n_0290` double(18,2) DEFAULT NULL COMMENT '负债合计',
  `f062n_0290` double(18,2) DEFAULT NULL COMMENT '实收资本（或股本）',
  `f063n_0290` double(18,2) DEFAULT NULL COMMENT '资本公积',
  `f064n_0290` double(18,2) DEFAULT NULL COMMENT '盈余公积',
  `f066n_0290` double(18,2) DEFAULT NULL COMMENT '减：库存股',
  `f065n_0290` double(18,2) DEFAULT NULL COMMENT '未分配利润',
  `f067n_0290` double(18,2) DEFAULT NULL COMMENT '少数股东权益',
  `f068n_0290` double(18,2) DEFAULT NULL COMMENT '外币报表折算价差',
  `f069n_0290` double(18,2) DEFAULT NULL COMMENT '非正常经营项目收益调整',
  `f070n_0290` double(18,2) DEFAULT NULL COMMENT '所有者权益（或股东权益）合计',
  `f071n_0290` double(18,2) DEFAULT NULL COMMENT '负债和所有者（或股东权益）合计',
  `ob_memo_0290` varchar(500) DEFAULT NULL COMMENT '备注',
  `ob_rectime_0290` datetime DEFAULT NULL COMMENT '录入时间',
  `ob_modtime_0290` datetime DEFAULT NULL COMMENT '修改时间',
  `ob_isvalid_0290` char(1) DEFAULT NULL COMMENT '是否有效',
  UNIQUE KEY `ob_orgid_0290` (`ob_orgid_0290`,`f001d_0290`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='2007通用资产负债表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.tb_company_cash_0292
DROP TABLE IF EXISTS `tb_company_cash_0292`;
CREATE TABLE IF NOT EXISTS `tb_company_cash_0292` (
  `ob_object_id` bigint(20) DEFAULT NULL COMMENT '记录ID',
  `ob_orgid_0292` varchar(11) NOT NULL COMMENT '机构ID',
  `ob_orgname_0292` varchar(100) NOT NULL COMMENT '机构名称',
  `ob_declaredate_0292` datetime DEFAULT NULL COMMENT '公告日期',
  `ob_startdate_0292` datetime DEFAULT NULL COMMENT '开始日期',
  `ob_enddate_0292` datetime DEFAULT NULL COMMENT '截止日期',
  `f001d_0292` datetime DEFAULT NULL COMMENT '报告年度',
  `f002v_0292` varchar(12) DEFAULT NULL COMMENT '合并类型编码',
  `f003v_0292` varchar(60) DEFAULT NULL COMMENT '合并类型',
  `f004v_0292` varchar(12) DEFAULT NULL COMMENT '报表来源编码',
  `f005v_0292` varchar(60) DEFAULT NULL COMMENT '报表来源',
  `f006n_0292` double(18,2) DEFAULT NULL COMMENT '销售商品、提供劳务收到的现金',
  `f007n_0292` double(18,2) DEFAULT NULL COMMENT '收到的税费返还',
  `f008n_0292` double(18,2) DEFAULT NULL COMMENT '收到其他与经营活动有关的现金',
  `f009n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动现金流入小计',
  `f010n_0292` double(18,2) DEFAULT NULL COMMENT '购买商品、接受劳务支付的现金',
  `f011n_0292` double(18,2) DEFAULT NULL COMMENT '支付给职工以及为职工支付的现金',
  `f012n_0292` double(18,2) DEFAULT NULL COMMENT '支付的各项税费',
  `f013n_0292` double(18,2) DEFAULT NULL COMMENT '支付其他与经营活动有关的现金',
  `f014n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动现金流出小计',
  `f015n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动产生的现金流量净额',
  `f016n_0292` double(18,2) DEFAULT NULL COMMENT '收回投资收到的现金',
  `f017n_0292` double(18,2) DEFAULT NULL COMMENT '取得投资收益收到的现金',
  `f018n_0292` double(18,2) DEFAULT NULL COMMENT '处置固定资产、无形资产和其他长期资产收回的现金净额',
  `f019n_0292` double(18,2) DEFAULT NULL COMMENT '处置子公司及其他营业单位收到的现金净额',
  `f020n_0292` double(18,2) DEFAULT NULL COMMENT '收到其他与投资活动有关的现金',
  `f021n_0292` double(18,2) DEFAULT NULL COMMENT '投资活动现金流入小计',
  `f022n_0292` double(18,2) DEFAULT NULL COMMENT '购建固定资产、无形资产和其他长期资产支付的现金',
  `f023n_0292` double(18,2) DEFAULT NULL COMMENT '投资支付的现金',
  `f024n_0292` double(18,2) DEFAULT NULL COMMENT '取得子公司及其他营业单位支付的现金净额',
  `f025n_0292` double(18,2) DEFAULT NULL COMMENT '支付其他与投资活动有关的现金',
  `f026n_0292` double(18,2) DEFAULT NULL COMMENT '投资活动现金流出小计',
  `f027n_0292` double(18,2) DEFAULT NULL COMMENT '投资活动产生的现金流量净额',
  `f028n_0292` double(18,2) DEFAULT NULL COMMENT '吸收投资收到的现金',
  `f029n_0292` double(18,2) DEFAULT NULL COMMENT '取得借款收到的现金',
  `f030n_0292` double(18,2) DEFAULT NULL COMMENT '收到其他与筹资活动有关的现金',
  `f031n_0292` double(18,2) DEFAULT NULL COMMENT '筹资活动现金流入小计',
  `f032n_0292` double(18,2) DEFAULT NULL COMMENT '偿还债务支付的现金',
  `f033n_0292` double(18,2) DEFAULT NULL COMMENT '分配股利、利润或偿付利息支付的现金',
  `f034n_0292` double(18,2) DEFAULT NULL COMMENT '支付其他与筹资活动有关的现金',
  `f035n_0292` double(18,2) DEFAULT NULL COMMENT '筹资活动现金流出小计',
  `f036n_0292` double(18,2) DEFAULT NULL COMMENT '筹资活动产生的现金流量净额',
  `f037n_0292` double(18,2) DEFAULT NULL COMMENT '四、汇率变动对现金的影响',
  `f038n_0292` double(18,2) DEFAULT NULL COMMENT '四(2)、其他原因对现金的影响',
  `f039n_0292` double(18,2) DEFAULT NULL COMMENT '五、现金及现金等价物净增加额',
  `f040n_0292` double(18,2) DEFAULT NULL COMMENT '期初现金及现金等价物余额',
  `f041n_0292` double(18,2) DEFAULT NULL COMMENT '期末现金及现金等价物余额',
  `f042n_0292` double(18,2) DEFAULT NULL COMMENT '附注：',
  `f043n_0292` double(18,2) DEFAULT NULL COMMENT '1、将净利润调节为经营活动现金流量：',
  `f044n_0292` double(18,2) DEFAULT NULL COMMENT '净利润',
  `f045n_0292` double(18,2) DEFAULT NULL COMMENT '加：资产减值准备',
  `f046n_0292` double(18,2) DEFAULT NULL COMMENT '固定资产折旧、油气资产折耗、生产性生物资产折旧',
  `f047n_0292` double(18,2) DEFAULT NULL COMMENT '无形资产摊销',
  `f048n_0292` double(18,2) DEFAULT NULL COMMENT '长期待摊费用摊销',
  `f049n_0292` double(18,2) DEFAULT NULL COMMENT '处置固定资产、无形资产和其他长期资产的损失',
  `f050n_0292` double(18,2) DEFAULT NULL COMMENT '固定资产报废损失',
  `f051n_0292` double(18,2) DEFAULT NULL COMMENT '公允价值变动损失',
  `f052n_0292` double(18,2) DEFAULT NULL COMMENT '财务费用',
  `f053n_0292` double(18,2) DEFAULT NULL COMMENT '投资损失',
  `f054n_0292` double(18,2) DEFAULT NULL COMMENT '递延所得税资产减少',
  `f055n_0292` double(18,2) DEFAULT NULL COMMENT '递延所得税负债增加',
  `f056n_0292` double(18,2) DEFAULT NULL COMMENT '存货的减少',
  `f057n_0292` double(18,2) DEFAULT NULL COMMENT '经营性应收项目的减少',
  `f058n_0292` double(18,2) DEFAULT NULL COMMENT '经营性应付项目的增加',
  `f059n_0292` double(18,2) DEFAULT NULL COMMENT '其他',
  `f060n_0292` double(18,2) DEFAULT NULL COMMENT '经营活动产生的现金流量净额2',
  `f061n_0292` double(18,2) DEFAULT NULL COMMENT '2、不涉及现金收支的重大投资和筹资活动：',
  `f062n_0292` double(18,2) DEFAULT NULL COMMENT '债务转为资本',
  `f063n_0292` double(18,2) DEFAULT NULL COMMENT '一年内到期的可转换公司债券',
  `f064n_0292` double(18,2) DEFAULT NULL COMMENT '融资租入固定资产',
  `f065n_0292` double(18,2) DEFAULT NULL COMMENT '3、现金及现金等价物净变动情况：',
  `f066n_0292` double(18,2) DEFAULT NULL COMMENT '现金的期末余额',
  `f067n_0292` double(18,2) DEFAULT NULL COMMENT '减：现金的期初余额',
  `f068n_0292` double(18,2) DEFAULT NULL COMMENT '加：现金等价物的期末余额',
  `f069n_0292` double(18,2) DEFAULT NULL COMMENT '减：现金等价物的期初余额',
  `f070n_0292` double(18,2) DEFAULT NULL COMMENT '加：其他原因对现金的影响2',
  `f071n_0292` double(18,2) DEFAULT NULL COMMENT '现金及现金等价物净增加额',
  `ob_memo_0292` varchar(500) DEFAULT NULL COMMENT '备注',
  `ob_rectime_0292` datetime DEFAULT NULL COMMENT '录入时间',
  `ob_modtime_0292` datetime DEFAULT NULL COMMENT '修改时间',
  `ob_isvalid_0292` char(1) DEFAULT NULL COMMENT '是否有效',
  UNIQUE KEY `ob_orgid_0292` (`ob_orgid_0292`,`f001d_0292`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='通用现金流量表及补充资料2007版--测试库';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.tb_company_profile_0291
DROP TABLE IF EXISTS `tb_company_profile_0291`;
CREATE TABLE IF NOT EXISTS `tb_company_profile_0291` (
  `ob_object_id` bigint(20) DEFAULT NULL COMMENT '记录ID',
  `ob_orgid_0291` varchar(11) NOT NULL COMMENT '机构ID',
  `ob_orgname_0291` varchar(100) NOT NULL COMMENT '机构名称',
  `ob_declaredate_0291` datetime DEFAULT NULL COMMENT '公告日期',
  `ob_startdate_0291` datetime DEFAULT NULL COMMENT '开始日期',
  `ob_enddate_0291` datetime DEFAULT NULL COMMENT '截止日期',
  `f001d_0291` datetime DEFAULT NULL COMMENT '报告年度',
  `f002v_0291` varchar(12) DEFAULT NULL COMMENT '合并类型编码',
  `f003v_0291` varchar(60) DEFAULT NULL COMMENT '合并类型',
  `f004v_0291` varchar(12) DEFAULT NULL COMMENT '报表来源编码',
  `f005v_0291` varchar(60) DEFAULT NULL COMMENT '报表来源',
  `f006n_0291` double(18,2) DEFAULT NULL COMMENT '一、营业收入',
  `f007n_0291` double(18,2) DEFAULT NULL COMMENT '减：营业成本',
  `f008n_0291` double(18,2) DEFAULT NULL COMMENT '营业税金及附加',
  `f009n_0291` double(18,2) DEFAULT NULL COMMENT '销售费用',
  `f010n_0291` double(18,2) DEFAULT NULL COMMENT '管理费用',
  `f011n_0291` double(18,2) DEFAULT NULL COMMENT '堪探费用',
  `f012n_0291` double(18,2) DEFAULT NULL COMMENT '财务费用',
  `f013n_0291` double(18,2) DEFAULT NULL COMMENT '资产减值损失',
  `f014n_0291` double(18,2) DEFAULT NULL COMMENT '加：公允价值变动净收益',
  `f015n_0291` double(18,2) DEFAULT NULL COMMENT '投资收益',
  `f016n_0291` double(18,2) DEFAULT NULL COMMENT '其中：对联营企业和合营企业的投资收益',
  `f017n_0291` double(18,2) DEFAULT NULL COMMENT '影响营业利润的其他科目',
  `f018n_0291` double(18,2) DEFAULT NULL COMMENT '二、营业利润',
  `f019n_0291` double(18,2) DEFAULT NULL COMMENT '加：补贴收入',
  `f020n_0291` double(18,2) DEFAULT NULL COMMENT '营业外收入',
  `f021n_0291` double(18,2) DEFAULT NULL COMMENT '减：营业外支出',
  `f022n_0291` double(18,2) DEFAULT NULL COMMENT '其中：非流动资产处置净损失',
  `f023n_0291` double(18,2) DEFAULT NULL COMMENT '加：影响利润总额的其他科目',
  `f024n_0291` double(18,2) DEFAULT NULL COMMENT '三、利润总额',
  `f025n_0291` double(18,2) DEFAULT NULL COMMENT '减：所得税',
  `f026n_0291` double(18,2) DEFAULT NULL COMMENT '加：影响净利润的其他科目',
  `f027n_0291` double(18,2) DEFAULT NULL COMMENT '四、净利润',
  `f028n_0291` double(18,2) DEFAULT NULL COMMENT '归属于母公司所有者的净利润',
  `f029n_0291` double(18,2) DEFAULT NULL COMMENT '少数股东损益',
  `f030n_0291` double(14,6) DEFAULT NULL COMMENT '五、每股收益：',
  `f031n_0291` double(14,6) DEFAULT NULL COMMENT '（一）基本每股收益',
  `f032n_0291` double(14,6) DEFAULT NULL COMMENT '（二）稀释每股收益',
  `ob_memo_0291` varchar(500) DEFAULT NULL COMMENT '备注',
  `ob_rectime_0291` datetime DEFAULT NULL COMMENT '录入时间',
  `ob_modtime_0291` datetime DEFAULT NULL COMMENT '修改时间',
  `ob_isvalid_0291` char(1) DEFAULT NULL COMMENT '是否有效',
  UNIQUE KEY `ob_orgid_0291` (`ob_orgid_0291`,`f001d_0291`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='通用利润表2007版 --测试库';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.tb_profile_0291
DROP TABLE IF EXISTS `tb_profile_0291`;
CREATE TABLE IF NOT EXISTS `tb_profile_0291` (
  `ob_object_id` bigint(20) DEFAULT NULL COMMENT '记录ID',
  `company_code` varchar(11) NOT NULL COMMENT '机构ID',
  `ob_orgname_0291` varchar(100) NOT NULL COMMENT '机构名称',
  `ob_declaredate_0291` datetime DEFAULT NULL COMMENT '公告日期',
  `ob_startdate_0291` datetime DEFAULT NULL COMMENT '开始日期',
  `ob_enddate_0291` datetime DEFAULT NULL COMMENT '截止日期',
  `time` datetime DEFAULT NULL COMMENT '报告年度',
  `f002v_0291` varchar(12) DEFAULT NULL COMMENT '合并类型编码',
  `f003v_0291` varchar(60) DEFAULT NULL COMMENT '合并类型',
  `f004v_0291` varchar(12) DEFAULT NULL COMMENT '报表来源编码',
  `f005v_0291` varchar(60) DEFAULT NULL COMMENT '报表来源',
  `f006n_0291` double(18,2) DEFAULT NULL COMMENT '一、营业收入',
  `f007n_0291` double(18,2) DEFAULT NULL COMMENT '减：营业成本',
  `f008n_0291` double(18,2) DEFAULT NULL COMMENT '营业税金及附加',
  `f009n_0291` double(18,2) DEFAULT NULL COMMENT '销售费用',
  `f010n_0291` double(18,2) DEFAULT NULL COMMENT '管理费用',
  `f011n_0291` double(18,2) DEFAULT NULL COMMENT '堪探费用',
  `f012n_0291` double(18,2) DEFAULT NULL COMMENT '财务费用',
  `f013n_0291` double(18,2) DEFAULT NULL COMMENT '资产减值损失',
  `f014n_0291` double(18,2) DEFAULT NULL COMMENT '加：公允价值变动净收益',
  `f015n_0291` double(18,2) DEFAULT NULL COMMENT '投资收益',
  `f016n_0291` double(18,2) DEFAULT NULL COMMENT '其中：对联营企业和合营企业的投资收益',
  `f017n_0291` double(18,2) DEFAULT NULL COMMENT '影响营业利润的其他科目',
  `f018n_0291` double(18,2) DEFAULT NULL COMMENT '二、营业利润',
  `f019n_0291` double(18,2) DEFAULT NULL COMMENT '加：补贴收入',
  `f020n_0291` double(18,2) DEFAULT NULL COMMENT '营业外收入',
  `f021n_0291` double(18,2) DEFAULT NULL COMMENT '减：营业外支出',
  `f022n_0291` double(18,2) DEFAULT NULL COMMENT '其中：非流动资产处置净损失',
  `f023n_0291` double(18,2) DEFAULT NULL COMMENT '加：影响利润总额的其他科目',
  `f024n_0291` double(18,2) DEFAULT NULL COMMENT '三、利润总额',
  `f025n_0291` double(18,2) DEFAULT NULL COMMENT '减：所得税',
  `f026n_0291` double(18,2) DEFAULT NULL COMMENT '加：影响净利润的其他科目',
  `f027n_0291` double(18,2) DEFAULT NULL COMMENT '四、净利润',
  `f028n_0291` double(18,2) DEFAULT NULL COMMENT '归属于母公司所有者的净利润',
  `f029n_0291` double(18,2) DEFAULT NULL COMMENT '少数股东损益',
  `f030n_0291` double(14,6) DEFAULT NULL COMMENT '五、每股收益：',
  `f031n_0291` double(14,6) DEFAULT NULL COMMENT '（一）基本每股收益',
  `f032n_0291` double(14,6) DEFAULT NULL COMMENT '（二）稀释每股收益',
  `ob_memo_0291` varchar(500) DEFAULT NULL COMMENT '备注',
  `ob_rectime_0291` datetime DEFAULT NULL COMMENT '录入时间',
  `ob_modtime_0291` datetime DEFAULT NULL COMMENT '修改时间',
  `ob_isvalid_0291` char(1) DEFAULT NULL COMMENT '是否有效',
  UNIQUE KEY `company_code` (`company_code`,`time`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='通用利润表2007版 --正式库';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.tb_public_0007
DROP TABLE IF EXISTS `tb_public_0007`;
CREATE TABLE IF NOT EXISTS `tb_public_0007` (
  `ob_secid_0007` varchar(11) NOT NULL COMMENT '证券主体ID',
  `ob_seccode_0007` varchar(10) NOT NULL COMMENT '证券代码',
  `ob_secname_0007` varchar(40) DEFAULT NULL COMMENT '证券简称',
  `f001v_0007` varchar(20) DEFAULT NULL COMMENT '拼音简称',
  `f016v_0007` varchar(100) DEFAULT NULL COMMENT '证券全称',
  `f002v_0007` varchar(12) DEFAULT NULL COMMENT '证券类别编码',
  `f003v_0007` varchar(60) DEFAULT NULL COMMENT '证券类别',
  `f005v_0007` varchar(12) DEFAULT NULL COMMENT '交易市场编码',
  `f006v_0007` varchar(60) DEFAULT NULL COMMENT '交易市场',
  `f007d_0007` datetime DEFAULT NULL COMMENT '上市日期',
  `f008d_0007` datetime DEFAULT NULL COMMENT '终止上市日期',
  `f009n_0007` double(18,2) DEFAULT NULL COMMENT '初始上市数量',
  `f012v_0007` varchar(12) DEFAULT NULL COMMENT '代码属性编码',
  `f013v_0007` varchar(60) DEFAULT NULL COMMENT '代码属性',
  `f014v_0007` varchar(11) DEFAULT NULL COMMENT '发行机构ID',
  `f015v_0007` varchar(100) DEFAULT NULL COMMENT '发行机构名称',
  `f017v_0007` varchar(12) DEFAULT NULL COMMENT '上市状态编码',
  `f018v_0007` varchar(60) DEFAULT NULL COMMENT '上市状态',
  `ob_memo_0007` varchar(255) DEFAULT NULL COMMENT '备注',
  `ob_modtime_0007` datetime DEFAULT NULL COMMENT '修改时间',
  `ob_rectime_0007` datetime DEFAULT NULL COMMENT '录入时间',
  `ob_isvalid_0007` char(1) DEFAULT NULL COMMENT '是否有效',
  `ob_object_id` bigint(20) DEFAULT NULL COMMENT '记录ID',
  KEY `ob_secid_0007` (`ob_secid_0007`,`ob_seccode_0007`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='证券信息表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_company
DROP TABLE IF EXISTS `t_company`;
CREATE TABLE IF NOT EXISTS `t_company` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `COMPANY_CODE` varchar(20) NOT NULL COMMENT '公司代码',
  `STOCK_CODE` varchar(20) NOT NULL COMMENT '股票代码',
  `INDUSTRY_CODE` varchar(20) NOT NULL COMMENT '行业名称',
  `INDUSTRY_CODE_YFZX` varchar(20) NOT NULL COMMENT '行业名称_yfzx',
  `COMPANY_NAME_CHI` varchar(100) DEFAULT NULL COMMENT '法定名称',
  `COMPANY_NAME_ENG` varchar(100) DEFAULT NULL COMMENT '英文名称',
  `ESTABLISH_TIME` date DEFAULT NULL COMMENT '成立日期',
  `LEGAL_MAN` varchar(20) DEFAULT NULL COMMENT '法人代表',
  `COM_TELEPHONE` varchar(20) DEFAULT NULL COMMENT '公司电话',
  `COM_FAX` varchar(20) DEFAULT NULL COMMENT '公司传真',
  `COM_MAIL` varchar(30) DEFAULT NULL COMMENT '公司电子邮箱',
  `COM_NET` varchar(30) DEFAULT NULL COMMENT '公司网址',
  `COM_PLATE` varchar(100) DEFAULT NULL COMMENT '所属板块',
  `COM_OPERATE` tinytext COMMENT '经营范围',
  `COM_INTRODUCE` text COMMENT '公司简介',
  `COM_SUM_INDUSTRY` bigint(20) DEFAULT NULL COMMENT '同行业公司数',
  `ACCOUNT_REGION` varchar(200) DEFAULT '12-30|12-30' COMMENT '会计区间',
  `TAGS` tinytext COMMENT '公司标签',
  PRIMARY KEY (`ID`,`COMPANY_CODE`),
  KEY `Index_COMPANY_CODE` (`COMPANY_CODE`),
  KEY `FK_Reference_1` (`INDUSTRY_CODE`),
  CONSTRAINT `FK_t_company_t_industry` FOREIGN KEY (`INDUSTRY_CODE`) REFERENCES `t_industry` (`INDUSTRY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='公司';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_company_attacheed
DROP TABLE IF EXISTS `t_company_attacheed`;
CREATE TABLE IF NOT EXISTS `t_company_attacheed` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `COMPANY_CODE` varchar(20) NOT NULL COMMENT '公司名称',
  `MARKET_TIME` date DEFAULT NULL COMMENT '上市日期',
  `REGISTER_CAPTIAL` double(20,5) DEFAULT NULL COMMENT '注册资本(万元)',
  `CLERK` varchar(20) DEFAULT NULL COMMENT '董事会秘书',
  `STAFF` bigint(20) DEFAULT NULL COMMENT '职工总数',
  `CLERK_TELEPHONE` varchar(20) DEFAULT NULL COMMENT '董秘电话',
  `CLERK_FAX` varchar(20) DEFAULT NULL COMMENT '董秘传真',
  `CLERK_MAIL` varchar(30) DEFAULT NULL COMMENT '董秘电子邮箱',
  `COM_REGISTER_ADD` varchar(100) DEFAULT NULL COMMENT '注册地址',
  `COM_OFFICE_ADD` varchar(100) DEFAULT NULL COMMENT '办公地址',
  PRIMARY KEY (`ID`,`COMPANY_CODE`),
  KEY `Index_COMPANY_CODE` (`COMPANY_CODE`),
  CONSTRAINT `FK_Reference_27` FOREIGN KEY (`COMPANY_CODE`) REFERENCES `t_company` (`COMPANY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='公司信息扩展表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_currency
DROP TABLE IF EXISTS `t_currency`;
CREATE TABLE IF NOT EXISTS `t_currency` (
  `curreney_code` varchar(20) NOT NULL COMMENT '编码',
  `name` varchar(20) NOT NULL COMMENT '货币名',
  `toRmbRule` varchar(50) NOT NULL COMMENT '到人民币的转换规则',
  `rmbToThisRule` varchar(50) NOT NULL COMMENT '人民币到此货币的转换规则',
  `rate` double(10,4) NOT NULL COMMENT '汇率',
  PRIMARY KEY (`curreney_code`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='货币';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_base_financial_index
DROP TABLE IF EXISTS `t_c_base_financial_index`;
CREATE TABLE IF NOT EXISTS `t_c_base_financial_index` (
  `COMPANY_CODE` varchar(50) DEFAULT NULL COMMENT '公司编码',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  `Index_1867` double(20,4) DEFAULT NULL COMMENT '货币资金',
  `Index_1868` double DEFAULT NULL COMMENT '流动资产',
  `Index_1869` double DEFAULT NULL COMMENT '长期资产',
  `Index_1870` double DEFAULT NULL COMMENT '短期借款',
  `Index_1871` double DEFAULT NULL COMMENT '流动负债',
  `Index_1872` double DEFAULT NULL COMMENT '长期资金来源',
  `Index_1873` double DEFAULT NULL COMMENT '偿债能力',
  `Index_1874` double DEFAULT NULL COMMENT '议价能力',
  `Index_1875` double DEFAULT NULL COMMENT '长期资金来源流足性',
  `Index_1876` double DEFAULT NULL COMMENT '有息负债',
  `Index_1878` double DEFAULT NULL COMMENT '速动资产',
  `Index_1879` double DEFAULT NULL COMMENT '流动比率',
  `Index_1880` double DEFAULT NULL COMMENT '速动比率',
  `Index_1881` double DEFAULT NULL COMMENT '资产负债率',
  `Index_1882` double DEFAULT NULL COMMENT '有息负债率',
  `Index_1883` double DEFAULT NULL COMMENT '季度经营活动现金净流量',
  `Index_1885` double DEFAULT NULL COMMENT '半年度经营活动现金净流量',
  `Index_1886` double DEFAULT NULL COMMENT '年化经营活动现金净流量',
  `Index_1887` double DEFAULT NULL COMMENT '季度收到其他与经营活动有关的现金',
  `Index_1888` double DEFAULT NULL COMMENT '半年收到其他与经营活动有关的现金',
  `Index_1889` double DEFAULT NULL COMMENT '年化收到其他与经营活动有关的现金',
  `Index_1891` double DEFAULT NULL COMMENT '季度支付其他与经常活动有关的现金',
  `Index_1892` double DEFAULT NULL COMMENT '半年支付其他与经常活动有关的现金',
  `Index_1893` double DEFAULT NULL COMMENT '年化支付其他与经营活动有关的现金',
  `Index_1894` double DEFAULT NULL COMMENT '季度购建固定资产、无形资产和其他长期资产支付的现金',
  `Index_1895` double DEFAULT NULL COMMENT '半年购建固定资产、无形资产和其他长期资产支付的现金',
  `Index_1896` double(20,4) DEFAULT NULL COMMENT '年化购建固定资产、无形资产和其他长期资产支付的现金',
  `Index_1897` double(20,4) DEFAULT NULL COMMENT '季度自由现金流',
  `Index_1898` double(20,4) DEFAULT NULL COMMENT '半年自由现金流',
  `Index_1899` double(20,4) DEFAULT NULL COMMENT '年化自由现金流',
  `Index_1900` double(20,4) DEFAULT NULL COMMENT '季度营业收入',
  `Index_1901` double(20,4) DEFAULT NULL COMMENT '半年营业收入',
  `Index_1902` double(20,4) DEFAULT NULL COMMENT '年化营业收入',
  `Index_1903` double(20,4) DEFAULT NULL COMMENT '季度营业成本',
  `Index_1904` double(20,4) DEFAULT NULL COMMENT '半年营业成本',
  `Index_1905` double(20,4) DEFAULT NULL COMMENT '年化营业成本',
  `Index_1906` double(20,4) DEFAULT NULL COMMENT '季度营业税金及附加',
  `Index_1907` double(20,4) DEFAULT NULL COMMENT '半年营业税金及附加',
  `Index_1908` double(20,4) DEFAULT NULL COMMENT '年化营业税金及附加',
  `Index_1909` double(20,4) DEFAULT NULL COMMENT '季度毛利',
  `Index_1910` double(20,4) DEFAULT NULL COMMENT '半年毛利',
  `Index_1911` double(20,4) DEFAULT NULL COMMENT '年化毛利',
  `Index_1912` double(20,4) DEFAULT NULL COMMENT '季度毛利率',
  `Index_1913` double(20,4) DEFAULT NULL COMMENT '半年毛利率',
  `Index_1914` double(20,4) DEFAULT NULL COMMENT '年化毛利率',
  `Index_1915` double(20,4) DEFAULT NULL COMMENT '季度管理费用',
  `Index_1916` double(20,4) DEFAULT NULL COMMENT '半年管理 费用',
  `Index_1917` double(20,4) DEFAULT NULL COMMENT '年化管理 费用',
  `Index_1918` double(20,4) DEFAULT NULL COMMENT '季度销售费用',
  `Index_1919` double(20,4) DEFAULT NULL COMMENT '半年销售费用',
  `Index_1920` double(20,4) DEFAULT NULL COMMENT '年化销售费用',
  `Index_1921` double(20,4) DEFAULT NULL COMMENT '季度堪探费用',
  `Index_1922` double(20,4) DEFAULT NULL COMMENT '半年堪探费用',
  `Index_1923` double DEFAULT NULL COMMENT '年化堪探费用',
  `Index_1924` double DEFAULT NULL COMMENT '季度财务费用',
  `Index_1925` double DEFAULT NULL COMMENT '半年财务费用',
  `Index_1926` double DEFAULT NULL COMMENT '年化财务费用',
  `Index_1927` double DEFAULT NULL COMMENT '季度营业利润(未调整)',
  `Index_1928` double DEFAULT NULL COMMENT '半年营业利润(未调整)',
  `Index_1929` double DEFAULT NULL COMMENT '年化营业利润(未调整)',
  `Index_1930` double DEFAULT NULL COMMENT '季度经营利润率(未调整)',
  `Index_1931` double DEFAULT NULL COMMENT '半年经营利润率(未调整)',
  `Index_1932` double DEFAULT NULL COMMENT '年化经营利润率(未调整)',
  `Index_1933` double DEFAULT NULL COMMENT '季度利润总额(未调整)',
  `Index_1934` double DEFAULT NULL COMMENT '半年利润总额(未调整)',
  `Index_1935` double DEFAULT NULL COMMENT '年化利润总额(未调整)',
  `Index_1936` double DEFAULT NULL COMMENT '季度净利润(未调整)',
  `Index_1937` double DEFAULT NULL COMMENT '半年净利润(未调整)',
  `Index_1938` double DEFAULT NULL COMMENT '年化净利润(未调整)',
  `Index_1939` double DEFAULT NULL COMMENT '季度净利润率(未调整)',
  `Index_1940` double DEFAULT NULL COMMENT '半年净利润率(未调整)',
  `Index_1941` double DEFAULT NULL COMMENT '年化净利润率(未调整)',
  `Index_1942` double DEFAULT NULL COMMENT '季度归属于股东的净利润(未调整)',
  `Index_1943` double DEFAULT NULL COMMENT '半年归属于股东的净利润(未调整)',
  `Index_1944` double DEFAULT NULL COMMENT '年化归属于股东的净利润(未调整)',
  `Index_1945` double DEFAULT NULL COMMENT '季度非经常性收益',
  `Index_1946` double DEFAULT NULL COMMENT '半年非经常性收益',
  `Index_1947` double DEFAULT NULL COMMENT '年化非经常性收益',
  `Index_1948` double DEFAULT NULL COMMENT '季度非经常性收益占利润总额(未调整)的比例',
  `Index_1949` double DEFAULT NULL COMMENT '半年非经常性收益占利润总额(未调整)的比例',
  `Index_1950` double DEFAULT NULL COMMENT '年化非经常性收益占利润总额(未调整)的比例',
  `Index_1951` double DEFAULT NULL COMMENT '企业所得税税负率',
  `Index_1952` double DEFAULT NULL COMMENT '季度营业利润(经调整)',
  `Index_1953` double DEFAULT NULL COMMENT '半年营业利润(经调整)',
  `Index_1954` double DEFAULT NULL COMMENT '年化营业利润(经调整)',
  `Index_1955` double DEFAULT NULL COMMENT '季度利润总额(经调整)',
  `Index_1956` double DEFAULT NULL COMMENT '半年利润总额(经调整)',
  `Index_1957` double DEFAULT NULL COMMENT '年化利润总额(经调整)',
  `Index_1958` double DEFAULT NULL COMMENT '季度净利润(经调整)',
  `Index_1959` double DEFAULT NULL COMMENT '半年净利润(经调整)',
  `Index_1960` double DEFAULT NULL COMMENT '年化净利润(经调整)',
  `Index_1961` double DEFAULT NULL COMMENT '母公司权益占比',
  `Index_1962` double DEFAULT NULL COMMENT '季度归属于股东的净利润(经调整)',
  `Index_1963` double DEFAULT NULL COMMENT '半年归属于股东的净利润(经调整)',
  `Index_1964` double DEFAULT NULL COMMENT '年化归属于股东的净利润(经调整)',
  `Index_1965` double DEFAULT NULL COMMENT '季度净利润率(经调整)',
  `Index_1966` double DEFAULT NULL COMMENT '半年净利润率(经调整)',
  `Index_1967` double DEFAULT NULL COMMENT '年化净利润率(经调整)',
  `Index_1968` double DEFAULT NULL COMMENT '应收账款及票据',
  `Index_1969` double DEFAULT NULL COMMENT '应付账款及票据',
  `Index_1970` double DEFAULT NULL COMMENT '无形资产及商誉',
  `Index_1971` double DEFAULT NULL COMMENT '应收账款及票据占总资产比',
  `Index_1972` double DEFAULT NULL COMMENT '预付账款占总资产比',
  `Index_1973` double DEFAULT NULL COMMENT '存货占总资产比',
  `Index_1974` double DEFAULT NULL COMMENT '其他应收款占总资产比',
  `Index_1975` double DEFAULT NULL COMMENT '预收账款占总资产比',
  `Index_1976` double DEFAULT NULL COMMENT '应付账款及票据占总资产比',
  `Index_1977` double DEFAULT NULL COMMENT '固定资产占总资产比',
  `Index_1978` double DEFAULT NULL COMMENT '流动资产占总资产比',
  `Index_1979` double DEFAULT NULL COMMENT '无形资产及商誉占总资产比',
  `Index_1980` double DEFAULT NULL COMMENT '平均应收账款及票据(两点)',
  `Index_1981` double DEFAULT NULL COMMENT '平均应收账款及票据(三点)',
  `Index_1982` double DEFAULT NULL COMMENT '平均应收账款及票据(五点)',
  `Index_1983` double DEFAULT NULL COMMENT '平均存货(两点)',
  `Index_1984` double DEFAULT NULL COMMENT '平均存货(三点)',
  `Index_1985` double DEFAULT NULL COMMENT '平均存货(五点)',
  `Index_1986` double DEFAULT NULL COMMENT '平均预付账款(两点)',
  `Index_1987` double DEFAULT NULL COMMENT '平均预付账款(三点)',
  `Index_1988` double DEFAULT NULL COMMENT '平均预付账款(五点)',
  `Index_1989` double DEFAULT NULL COMMENT '平均应付账款及票据(两点)',
  `Index_1990` double DEFAULT NULL COMMENT '平均应付账款及票据(三点)',
  `Index_1991` double DEFAULT NULL COMMENT '平均应付账款及票据(五点)',
  `Index_1992` double DEFAULT NULL COMMENT '平均预收账款(两点)',
  `Index_1993` double DEFAULT NULL COMMENT '平均预收账款(三点)',
  `Index_1994` double DEFAULT NULL COMMENT '平均预收账款(五点)',
  `Index_1995` double DEFAULT NULL COMMENT '固定资产',
  `Index_1996` double DEFAULT NULL COMMENT '平均固定资产(两点)',
  `Index_1997` double DEFAULT NULL COMMENT '平均固定资产(三点)',
  `Index_1998` double DEFAULT NULL COMMENT '平均固定资产(五点)',
  `Index_1999` double DEFAULT NULL COMMENT '平均流动资产(两点)',
  `Index_2000` double DEFAULT NULL COMMENT '平均流动资产(三点)',
  `Index_2001` double DEFAULT NULL COMMENT '平均流动资产(五点)',
  `Index_2002` double DEFAULT NULL COMMENT '平均净资产(两点)',
  `Index_2003` double DEFAULT NULL COMMENT '平均净资产(三点)',
  `Index_2004` double DEFAULT NULL COMMENT '平均净资产(五点)',
  `Index_2005` double DEFAULT NULL COMMENT '平均总资产(两点)',
  `Index_2006` double DEFAULT NULL COMMENT '平均总资产(三点)',
  `Index_2007` double DEFAULT NULL COMMENT '平均总资产(五点)',
  `Index_2008` double DEFAULT NULL COMMENT '平均有息负债(两点)',
  `Index_2009` double DEFAULT NULL COMMENT '平均有息负债(三点)',
  `Index_2010` double DEFAULT NULL COMMENT '平均有息负债(五点)',
  `Index_2011` double DEFAULT NULL COMMENT '季度资产收益率(未调整)',
  `Index_2012` double DEFAULT NULL COMMENT '季度资产收益率(经调整)',
  `Index_2013` double DEFAULT NULL COMMENT '半年资产收益率(未调整)',
  `Index_2014` double DEFAULT NULL COMMENT '半年资产收益率(经调整)',
  `Index_2015` double DEFAULT NULL COMMENT '年化资产收益率(未调整)',
  `Index_2016` double DEFAULT NULL COMMENT '年化资产收益率(经调整)',
  `Index_2017` double DEFAULT NULL COMMENT '季度净资产收益率(未调整)',
  `Index_2018` double DEFAULT NULL COMMENT '季度净资产收益率(经调整)',
  `Index_2019` double DEFAULT NULL COMMENT '半年净资产收益率(未调整)',
  `Index_2020` double DEFAULT NULL COMMENT '半年净资产收益率(经调整)',
  `Index_2021` double DEFAULT NULL COMMENT '年化净资产收益率(未调整)',
  `Index_2022` double DEFAULT NULL COMMENT '年化净资产收益率(经调整)',
  `Index_2023` double DEFAULT NULL COMMENT '季度EBIT(未调整)',
  `Index_2024` double DEFAULT NULL COMMENT '半年EBIT(未调整)',
  `Index_2025` double DEFAULT NULL COMMENT '年化EBIT(未调整)',
  `Index_2026` double DEFAULT NULL COMMENT '季度EBIT(经调整)',
  `Index_2027` double DEFAULT NULL COMMENT '半年EBIT(经调整)',
  `Index_2028` double DEFAULT NULL COMMENT '年化EBIT(经调整)',
  `Index_2029` double DEFAULT NULL COMMENT '季度折旧及摊销',
  `Index_2030` double DEFAULT NULL COMMENT '半年折旧及摊销',
  `Index_2031` double DEFAULT NULL COMMENT '年化折旧及摊销',
  `Index_2032` double DEFAULT NULL COMMENT '季度EBITDA(未调整)',
  `Index_2033` double DEFAULT NULL COMMENT '半年EBITDA(未调整)',
  `Index_2034` double DEFAULT NULL COMMENT '年化EBITDA(未调整)',
  `Index_2035` double DEFAULT NULL COMMENT '季度EBITDA(经调整)',
  `Index_2036` double DEFAULT NULL COMMENT '半年EBITDA(经调整)',
  `Index_2037` double DEFAULT NULL COMMENT '年化EBITDA(经调整)',
  `Index_2038` double DEFAULT NULL COMMENT '季度投入资本收益率(ROIC)(未调整)',
  `Index_2039` double DEFAULT NULL COMMENT '半年投入资本收益率(ROIC)(未调整)',
  `Index_2040` double DEFAULT NULL COMMENT '年化投入资本收益率(ROIC)(未调整)',
  `Index_2041` double DEFAULT NULL COMMENT '季度投入资本收益率(ROIC)(经调整)',
  `Index_2042` double DEFAULT NULL COMMENT '半年投入资本收益率(ROIC)(经调整)',
  `Index_2043` double DEFAULT NULL COMMENT '年化投入资本收益率(ROIC)(经调整)',
  `Index_2044` double DEFAULT NULL COMMENT '应收账款周转率(季度)',
  `Index_2045` double DEFAULT NULL COMMENT '应收账款周转天数(季度)',
  `Index_2046` double DEFAULT NULL COMMENT '应收账款周转率(半年)',
  `Index_2047` double DEFAULT NULL COMMENT '应收账款周转天数(半年)',
  `Index_2048` double DEFAULT NULL COMMENT '应收账款周转率(年化)',
  `Index_2049` double DEFAULT NULL COMMENT '应收账款周转天数(年化)',
  `Index_2050` double DEFAULT NULL COMMENT '存货周转率(季度)',
  `Index_2051` double DEFAULT NULL COMMENT '存货周转天数(季度)',
  `Index_2052` double DEFAULT NULL COMMENT '存货周转率(半年)',
  `Index_2053` double DEFAULT NULL COMMENT '存货周转天数(半年)',
  `Index_2054` double DEFAULT NULL COMMENT '存货周转率(年化)',
  `Index_2055` double DEFAULT NULL COMMENT '存货周转天数(年化)',
  `Index_2056` double DEFAULT NULL COMMENT '预付账款周转率(季度)',
  `Index_2057` double DEFAULT NULL COMMENT '预付账款周转天数(季度)',
  `Index_2058` double DEFAULT NULL COMMENT '预付账款周转率(半年)',
  `Index_2059` double DEFAULT NULL COMMENT '预付账款周转天数(半年)',
  `Index_2060` double DEFAULT NULL COMMENT '预付账款周转率(年化)',
  `Index_2061` double DEFAULT NULL COMMENT '预付账款周转天数(年化)',
  `Index_2062` double DEFAULT NULL COMMENT '应付账款周转率(季度)',
  `Index_2063` double DEFAULT NULL COMMENT '应付账款周转天数(季度)',
  `Index_2064` double DEFAULT NULL COMMENT '应付账款周转率(半年)',
  `Index_2065` double DEFAULT NULL COMMENT '应付账款周转天数(半年)',
  `Index_2066` double DEFAULT NULL COMMENT '应付账款周转率(年化)',
  `Index_2067` double DEFAULT NULL COMMENT '应付账款周转天数(年化)',
  `Index_2068` double DEFAULT NULL COMMENT '预收账款周转率(季度)',
  `Index_2069` double DEFAULT NULL COMMENT '预收账款周转天数(季度)',
  `Index_2070` double DEFAULT NULL COMMENT '预收账款周转率(半年)',
  `Index_2071` double DEFAULT NULL COMMENT '预收账款周转天数(半年)',
  `Index_2072` double DEFAULT NULL COMMENT '预收账款周转率(年化)',
  `Index_2073` double DEFAULT NULL COMMENT '预收账款周转天数(年化)',
  `Index_2074` double DEFAULT NULL COMMENT '固定资产周转率(季度)',
  `Index_2075` double DEFAULT NULL COMMENT '固定资产周转率(半年)',
  `Index_2076` double DEFAULT NULL COMMENT '固定资产周转率(年化)',
  `Index_2077` double DEFAULT NULL COMMENT '流动资产周转率(季度)',
  `Index_2078` double DEFAULT NULL COMMENT '流动资产周转率(半年)',
  `Index_2079` double DEFAULT NULL COMMENT '流动资产周转率(年化)',
  `Index_2080` double DEFAULT NULL COMMENT '总资产周转率(季度)',
  `Index_2081` double DEFAULT NULL COMMENT '总资产周转率(半年)',
  `Index_2082` double DEFAULT NULL COMMENT '总资产周转率(年化)',
  `Index_2083` double DEFAULT NULL COMMENT '营业转换周期(季度)',
  `Index_2084` double DEFAULT NULL COMMENT '营业转换周期(半年)',
  `Index_2085` double DEFAULT NULL COMMENT '营业转换周期(年化)',
  `Index_2086` double DEFAULT NULL COMMENT '现金转换周期(季度)',
  `Index_2087` double DEFAULT NULL COMMENT '现金转换周期(半年)',
  `Index_2088` double DEFAULT NULL COMMENT '现金转换周期(年化)',
  `Index_2089` double DEFAULT NULL COMMENT 'CCC日均资金成本（季度）',
  `Index_2090` double DEFAULT NULL COMMENT 'CCC日均资金成本（半年）',
  `Index_2091` double DEFAULT NULL COMMENT 'CCC日均资金成本（年化）',
  `Index_2092` double DEFAULT NULL COMMENT '管理费用占收入比(季度)',
  `Index_2093` double DEFAULT NULL COMMENT '管理费用占收入比(半年)',
  `Index_2094` double DEFAULT NULL COMMENT '管理费用占收入比(年化)',
  `Index_2095` double DEFAULT NULL COMMENT '销售费用占收入比(季度)',
  `Index_2096` double DEFAULT NULL COMMENT '销售费用占收入比(半年)',
  `Index_2097` double DEFAULT NULL COMMENT '销售费用占收入比(年化)',
  `Index_2098` double DEFAULT NULL COMMENT '财务费用占收入比(季度)',
  `Index_2099` double DEFAULT NULL COMMENT '财务费用占收入比(半年)',
  `Index_2100` double DEFAULT NULL COMMENT '财务费用占收入比(年化)',
  `Index_2101` double DEFAULT NULL COMMENT '堪探费用占收入比(季度)',
  `Index_2102` double DEFAULT NULL COMMENT '堪探费用占收入比(半年)',
  `Index_2103` double DEFAULT NULL COMMENT '堪探费用占收入比(年化)',
  `Index_2104` double DEFAULT NULL COMMENT '营业成本占收入比(季度)',
  `Index_2105` double DEFAULT NULL COMMENT '营业成本占收入比(半年)',
  `Index_2106` double DEFAULT NULL COMMENT '营业成本占收入比(年化)',
  `Index_2107` double DEFAULT NULL COMMENT '每股收益(季度)(未调整)',
  `Index_2108` double DEFAULT NULL COMMENT '每股收益(半年)(未调整)',
  `Index_2109` double DEFAULT NULL COMMENT '每股收益(年化)(未调整)',
  `Index_2110` double DEFAULT NULL COMMENT '每股收益(季度)(经调整)',
  `Index_2111` double DEFAULT NULL COMMENT '每股收益(半年)(经调整)',
  `Index_2112` double DEFAULT NULL COMMENT '每股收益(年化)(经调整)',
  `Index_2113` double DEFAULT NULL COMMENT '每股净资产',
  `Index_2114` double DEFAULT NULL COMMENT '每股有形净资产',
  `Index_2115` double DEFAULT NULL COMMENT '每股经营活动现金净流量(季度)',
  `Index_2116` double DEFAULT NULL COMMENT '每股经营活动现金净流量(半年)',
  `Index_2117` double DEFAULT NULL COMMENT '每股经营活动现金净流量(年化)',
  `Index_2118` double DEFAULT NULL COMMENT '每股自由现金流(季度)',
  `Index_2119` double DEFAULT NULL COMMENT '每股自由现金流(半年)',
  `Index_2120` double DEFAULT NULL COMMENT '每股自由现金流(年化)',
  `Index_2121` double DEFAULT NULL COMMENT '经济附加值(未调整)',
  `Index_2122` double DEFAULT NULL COMMENT '经济附加值(经调整)',
  UNIQUE KEY `uniquekey` (`COMPANY_CODE`,`TIME`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='公司的基础财务指标';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_0
DROP TABLE IF EXISTS `t_c_ext_index_0`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_0` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_1
DROP TABLE IF EXISTS `t_c_ext_index_1`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_1` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_10
DROP TABLE IF EXISTS `t_c_ext_index_10`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_10` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_11
DROP TABLE IF EXISTS `t_c_ext_index_11`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_11` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_12
DROP TABLE IF EXISTS `t_c_ext_index_12`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_12` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_13
DROP TABLE IF EXISTS `t_c_ext_index_13`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_13` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_14
DROP TABLE IF EXISTS `t_c_ext_index_14`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_14` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_15
DROP TABLE IF EXISTS `t_c_ext_index_15`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_15` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_16
DROP TABLE IF EXISTS `t_c_ext_index_16`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_16` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_17
DROP TABLE IF EXISTS `t_c_ext_index_17`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_17` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_18
DROP TABLE IF EXISTS `t_c_ext_index_18`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_18` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_19
DROP TABLE IF EXISTS `t_c_ext_index_19`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_19` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_2
DROP TABLE IF EXISTS `t_c_ext_index_2`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_2` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_20
DROP TABLE IF EXISTS `t_c_ext_index_20`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_20` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_21
DROP TABLE IF EXISTS `t_c_ext_index_21`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_21` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_22
DROP TABLE IF EXISTS `t_c_ext_index_22`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_22` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_23
DROP TABLE IF EXISTS `t_c_ext_index_23`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_23` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_24
DROP TABLE IF EXISTS `t_c_ext_index_24`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_24` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_25
DROP TABLE IF EXISTS `t_c_ext_index_25`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_25` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_26
DROP TABLE IF EXISTS `t_c_ext_index_26`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_26` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_27
DROP TABLE IF EXISTS `t_c_ext_index_27`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_27` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_28
DROP TABLE IF EXISTS `t_c_ext_index_28`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_28` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_29
DROP TABLE IF EXISTS `t_c_ext_index_29`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_29` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_3
DROP TABLE IF EXISTS `t_c_ext_index_3`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_3` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_30
DROP TABLE IF EXISTS `t_c_ext_index_30`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_30` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_31
DROP TABLE IF EXISTS `t_c_ext_index_31`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_31` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_32
DROP TABLE IF EXISTS `t_c_ext_index_32`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_32` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_33
DROP TABLE IF EXISTS `t_c_ext_index_33`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_33` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_34
DROP TABLE IF EXISTS `t_c_ext_index_34`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_34` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_35
DROP TABLE IF EXISTS `t_c_ext_index_35`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_35` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_36
DROP TABLE IF EXISTS `t_c_ext_index_36`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_36` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_37
DROP TABLE IF EXISTS `t_c_ext_index_37`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_37` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_38
DROP TABLE IF EXISTS `t_c_ext_index_38`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_38` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_39
DROP TABLE IF EXISTS `t_c_ext_index_39`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_39` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_4
DROP TABLE IF EXISTS `t_c_ext_index_4`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_4` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_40
DROP TABLE IF EXISTS `t_c_ext_index_40`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_40` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_41
DROP TABLE IF EXISTS `t_c_ext_index_41`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_41` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_42
DROP TABLE IF EXISTS `t_c_ext_index_42`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_42` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_43
DROP TABLE IF EXISTS `t_c_ext_index_43`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_43` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_44
DROP TABLE IF EXISTS `t_c_ext_index_44`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_44` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_45
DROP TABLE IF EXISTS `t_c_ext_index_45`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_45` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_46
DROP TABLE IF EXISTS `t_c_ext_index_46`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_46` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_47
DROP TABLE IF EXISTS `t_c_ext_index_47`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_47` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_48
DROP TABLE IF EXISTS `t_c_ext_index_48`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_48` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_49
DROP TABLE IF EXISTS `t_c_ext_index_49`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_49` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_5
DROP TABLE IF EXISTS `t_c_ext_index_5`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_5` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_50
DROP TABLE IF EXISTS `t_c_ext_index_50`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_50` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_51
DROP TABLE IF EXISTS `t_c_ext_index_51`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_51` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_52
DROP TABLE IF EXISTS `t_c_ext_index_52`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_52` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_53
DROP TABLE IF EXISTS `t_c_ext_index_53`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_53` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_54
DROP TABLE IF EXISTS `t_c_ext_index_54`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_54` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_55
DROP TABLE IF EXISTS `t_c_ext_index_55`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_55` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_56
DROP TABLE IF EXISTS `t_c_ext_index_56`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_56` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_57
DROP TABLE IF EXISTS `t_c_ext_index_57`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_57` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_58
DROP TABLE IF EXISTS `t_c_ext_index_58`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_58` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_59
DROP TABLE IF EXISTS `t_c_ext_index_59`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_59` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_6
DROP TABLE IF EXISTS `t_c_ext_index_6`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_6` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_60
DROP TABLE IF EXISTS `t_c_ext_index_60`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_60` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_61
DROP TABLE IF EXISTS `t_c_ext_index_61`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_61` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_62
DROP TABLE IF EXISTS `t_c_ext_index_62`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_62` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_63
DROP TABLE IF EXISTS `t_c_ext_index_63`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_63` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_64
DROP TABLE IF EXISTS `t_c_ext_index_64`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_64` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_65
DROP TABLE IF EXISTS `t_c_ext_index_65`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_65` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_66
DROP TABLE IF EXISTS `t_c_ext_index_66`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_66` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_67
DROP TABLE IF EXISTS `t_c_ext_index_67`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_67` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_68
DROP TABLE IF EXISTS `t_c_ext_index_68`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_68` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_69
DROP TABLE IF EXISTS `t_c_ext_index_69`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_69` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_7
DROP TABLE IF EXISTS `t_c_ext_index_7`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_7` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_70
DROP TABLE IF EXISTS `t_c_ext_index_70`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_70` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_71
DROP TABLE IF EXISTS `t_c_ext_index_71`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_71` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_72
DROP TABLE IF EXISTS `t_c_ext_index_72`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_72` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_73
DROP TABLE IF EXISTS `t_c_ext_index_73`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_73` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_74
DROP TABLE IF EXISTS `t_c_ext_index_74`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_74` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_75
DROP TABLE IF EXISTS `t_c_ext_index_75`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_75` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_76
DROP TABLE IF EXISTS `t_c_ext_index_76`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_76` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_77
DROP TABLE IF EXISTS `t_c_ext_index_77`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_77` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_78
DROP TABLE IF EXISTS `t_c_ext_index_78`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_78` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_79
DROP TABLE IF EXISTS `t_c_ext_index_79`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_79` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_8
DROP TABLE IF EXISTS `t_c_ext_index_8`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_8` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_80
DROP TABLE IF EXISTS `t_c_ext_index_80`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_80` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_81
DROP TABLE IF EXISTS `t_c_ext_index_81`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_81` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_82
DROP TABLE IF EXISTS `t_c_ext_index_82`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_82` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_83
DROP TABLE IF EXISTS `t_c_ext_index_83`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_83` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_84
DROP TABLE IF EXISTS `t_c_ext_index_84`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_84` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_85
DROP TABLE IF EXISTS `t_c_ext_index_85`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_85` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_86
DROP TABLE IF EXISTS `t_c_ext_index_86`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_86` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_87
DROP TABLE IF EXISTS `t_c_ext_index_87`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_87` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_88
DROP TABLE IF EXISTS `t_c_ext_index_88`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_88` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_89
DROP TABLE IF EXISTS `t_c_ext_index_89`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_89` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_9
DROP TABLE IF EXISTS `t_c_ext_index_9`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_9` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_90
DROP TABLE IF EXISTS `t_c_ext_index_90`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_90` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_91
DROP TABLE IF EXISTS `t_c_ext_index_91`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_91` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_92
DROP TABLE IF EXISTS `t_c_ext_index_92`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_92` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_93
DROP TABLE IF EXISTS `t_c_ext_index_93`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_93` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_94
DROP TABLE IF EXISTS `t_c_ext_index_94`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_94` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_95
DROP TABLE IF EXISTS `t_c_ext_index_95`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_95` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_96
DROP TABLE IF EXISTS `t_c_ext_index_96`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_96` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_97
DROP TABLE IF EXISTS `t_c_ext_index_97`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_97` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_98
DROP TABLE IF EXISTS `t_c_ext_index_98`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_98` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_ext_index_99
DROP TABLE IF EXISTS `t_c_ext_index_99`;
CREATE TABLE IF NOT EXISTS `t_c_ext_index_99` (
  `COMPANY_CODE` varchar(20) DEFAULT NULL COMMENT '公司编码',
  `INDEX_CODE` varchar(20) DEFAULT NULL COMMENT '公司指标编码',
  `VALUE` double(20,4) DEFAULT NULL COMMENT '值',
  `TIME` datetime DEFAULT NULL COMMENT '时间',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '生成时间',
  `INDEX_NAME` varchar(50) DEFAULT NULL COMMENT '指标名',
  UNIQUE KEY `ext_unique_key` (`COMPANY_CODE`,`TIME`,`INDEX_CODE`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_c_financial_index_rule
DROP TABLE IF EXISTS `t_c_financial_index_rule`;
CREATE TABLE IF NOT EXISTS `t_c_financial_index_rule` (
  `ID` bigint(8) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(50) NOT NULL COMMENT '公司财务指标名',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '指标更新时间',
  `C_INDEX_CODE` varchar(50) NOT NULL COMMENT '公司指标编码',
  `COMPANY_CODE` varchar(50) DEFAULT NULL COMMENT '公司编码 :1--100为所有公司的公用指标',
  `RULE` varchar(500) NOT NULL COMMENT '指标规则',
  `TYPE` int(11) NOT NULL COMMENT '指标类型\\\\\\\\\\\\\\\\r\\\\\\\\\\\\\\\\n            0:基本指示规则,不带时间变量的\\\\\\\\\\\\\\\\r\\\\\\\\\\\\\\\\n            1:带时间变量的指标规则       2:规则模板  3:中间表达式   ',
  `COMMENTS` varchar(500) NOT NULL COMMENT '备注',
  `ACROSS_TYPE` int(10) NOT NULL DEFAULT '0' COMMENT '跨期类型:0:不跨期,1:跨期',
  `Q1_RULE` varchar(500) DEFAULT '' COMMENT 'Q1公式',
  `Q1_RULE_COMMENTS` varchar(500) DEFAULT '' COMMENT 'Q1公式注解',
  `Q2_RULE` varchar(500) DEFAULT '' COMMENT 'Q2公式',
  `Q2_RULE_COMMENTS` varchar(500) DEFAULT '' COMMENT 'Q2公式注解',
  `Q3_RULE` varchar(500) DEFAULT '' COMMENT 'Q3公式',
  `Q3_RULE_COMMENTS` varchar(500) DEFAULT '' COMMENT 'Q3公式注解',
  `Q4_RULE` varchar(500) DEFAULT '' COMMENT 'Q4公式',
  `Q4_RULE_COMMENTS` varchar(500) DEFAULT '' COMMENT 'Q4公式注解',
  `TABLE_SYSTEM_CODE` varchar(50) NOT NULL COMMENT '报表体系编码',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `C_INDEX_CODE` (`C_INDEX_CODE`),
  UNIQUE KEY `NAME` (`NAME`,`TABLE_SYSTEM_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='公司财务(company)指标规则库';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_data_diictionary
DROP TABLE IF EXISTS `t_data_diictionary`;
CREATE TABLE IF NOT EXISTS `t_data_diictionary` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `TABLE_NAME` varchar(50) DEFAULT NULL COMMENT '表名',
  `SYSTEM_CHILD_TABLE_CODE` varchar(50) NOT NULL COMMENT '报表体系中的子表编码',
  `COLUMN_NAME` varchar(50) DEFAULT NULL COMMENT '列名',
  `INDEX_CODE` varchar(50) NOT NULL COMMENT '指标编码',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '新定义的:-2不显示的指标;-1:非指标项;0:资产表指标;1:负债表指标;2:现多流量表指标;3:现金流量附表指标;4:利润表指标;5:基本指标表指标;6扩展表指标; 7:公司自定义指标 8:行业自定义指标 9:用户自定义指标;-9:测试指标',
  `COLUMN_CHI_NAME` varchar(200) NOT NULL DEFAULT '0' COMMENT '列的中文名',
  `unit` int(4) NOT NULL DEFAULT '-1' COMMENT '单位：-1：无单位,0:%百分比,1:千,2:万,3:百万,4:千万,5:亿',
  `tags` varchar(50) DEFAULT NULL COMMENT '指标分类,多个分类用'';''分隔',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `INDEX_CODE` (`INDEX_CODE`),
  KEY `FK_t_data_diictionary_t_match_info` (`SYSTEM_CHILD_TABLE_CODE`),
  CONSTRAINT `FK_t_data_diictionary_t_match_info` FOREIGN KEY (`SYSTEM_CHILD_TABLE_CODE`) REFERENCES `t_match_info` (`system_child_table_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据字典';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_data_source
DROP TABLE IF EXISTS `t_data_source`;
CREATE TABLE IF NOT EXISTS `t_data_source` (
  `data_source_code` varchar(50) NOT NULL COMMENT '数据源编码',
  `data_source_name` varchar(50) NOT NULL COMMENT '数据源名字',
  PRIMARY KEY (`data_source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据源表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_index_category
DROP TABLE IF EXISTS `t_index_category`;
CREATE TABLE IF NOT EXISTS `t_index_category` (
  `index_category_code` int(10) NOT NULL COMMENT '指标分类编码',
  `name` varchar(80) NOT NULL COMMENT '指标分类名称',
  `parent_code` varchar(150) NOT NULL COMMENT '父分类编码',
  PRIMARY KEY (`index_category_code`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='指标分类表，顶级父分类编码为：-1,起始值：10000';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_industry
DROP TABLE IF EXISTS `t_industry`;
CREATE TABLE IF NOT EXISTS `t_industry` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `INDUSTRY_CODE` varchar(20) NOT NULL COMMENT '行业代码',
  `NAME` varchar(50) DEFAULT NULL COMMENT '行业名称',
  `PARENT_CODE` varchar(20) DEFAULT NULL COMMENT '父id',
  PRIMARY KEY (`ID`,`INDUSTRY_CODE`),
  KEY `Index_INDUSTRY_CODE` (`INDUSTRY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='行业';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_industry_sw
DROP TABLE IF EXISTS `t_industry_sw`;
CREATE TABLE IF NOT EXISTS `t_industry_sw` (
  `Ind_Code` char(8) DEFAULT NULL COMMENT '行业编码',
  `Ind_Name` varchar(50) DEFAULT NULL COMMENT '行业名称',
  KEY `Index 1` (`Ind_Code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='行业 来自申银万国';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_i_base_financial_index
DROP TABLE IF EXISTS `t_i_base_financial_index`;
CREATE TABLE IF NOT EXISTS `t_i_base_financial_index` (
  `id` bigint(10) NOT NULL AUTO_INCREMENT,
  `INDUSTRY_CODE` varchar(20) DEFAULT NULL COMMENT '行业代码',
  PRIMARY KEY (`id`),
  KEY `Index_INDUSTRY_CODE` (`INDUSTRY_CODE`),
  CONSTRAINT `FK_Reference_9` FOREIGN KEY (`INDUSTRY_CODE`) REFERENCES `t_industry` (`INDUSTRY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='行业的基础财务指标';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_match_info
DROP TABLE IF EXISTS `t_match_info`;
CREATE TABLE IF NOT EXISTS `t_match_info` (
  `data_source_code` varchar(50) NOT NULL COMMENT '数据源编码',
  `data_source_name` varchar(50) NOT NULL COMMENT '数据源名',
  `industry_code` varchar(50) DEFAULT '' COMMENT '行业编码',
  `table_system_code` varchar(50) NOT NULL COMMENT '选择的报表体系编码',
  `system_child_table_type` varchar(50) NOT NULL COMMENT '报表体系中的子表类型 0:资产表;1:负债表;2:现多流量表;3:现金流量附表;4:利润表:5:基本指标表6:扩展指标表',
  `system_child_table_name` varchar(50) NOT NULL COMMENT '报表体系中的子表的表名',
  `system_child_table_code` varchar(50) NOT NULL COMMENT '报表体系中的子表的编码',
  PRIMARY KEY (`system_child_table_code`),
  KEY `FK_t_match_info_t_table_system` (`table_system_code`),
  KEY `FK_t_match_info_t_data_source` (`data_source_code`),
  CONSTRAINT `FK_t_match_info_t_data_source` FOREIGN KEY (`data_source_code`) REFERENCES `t_data_source` (`data_source_code`),
  CONSTRAINT `FK_t_match_info_t_table_system` FOREIGN KEY (`table_system_code`) REFERENCES `t_table_system` (`table_system_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='适配信息表;每加一套报表体系,均需要增加此体系的t_ext_index表的适配信息';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_ranking
DROP TABLE IF EXISTS `t_ranking`;
CREATE TABLE IF NOT EXISTS `t_ranking` (
  `ranking_code` int(10) NOT NULL AUTO_INCREMENT COMMENT '榜单编码',
  `ranking_name` varchar(50) NOT NULL COMMENT '榜单名',
  `table_system_code` varchar(50) NOT NULL COMMENT '报表体系编码',
  `ranking_rule` varchar(500) NOT NULL COMMENT '榜单规则',
  `ranking_comments` varchar(500) NOT NULL COMMENT '榜单规则的注释',
  `uptime` datetime NOT NULL COMMENT '榜单更新时间',
  PRIMARY KEY (`ranking_code`),
  UNIQUE KEY `ranking_name` (`ranking_name`,`table_system_code`),
  KEY `FK_t_ranking_t_table_system` (`table_system_code`),
  CONSTRAINT `FK_t_ranking_t_table_system` FOREIGN KEY (`table_system_code`) REFERENCES `t_table_system` (`table_system_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='榜单表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_ranking_result
DROP TABLE IF EXISTS `t_ranking_result`;
CREATE TABLE IF NOT EXISTS `t_ranking_result` (
  `rank_code` int(10) NOT NULL COMMENT '榜单编码',
  `table_system_code` varchar(50) NOT NULL COMMENT '榜单编码',
  `rank_companys` text NOT NULL COMMENT '入榜公司',
  `rank_period` datetime NOT NULL COMMENT '期',
  `uptime` datetime NOT NULL COMMENT '更新时间',
  UNIQUE KEY `table_system_code` (`table_system_code`,`rank_period`),
  UNIQUE KEY `rank_code` (`rank_code`,`rank_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='榜单计算结果表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_reprot_template
DROP TABLE IF EXISTS `t_reprot_template`;
CREATE TABLE IF NOT EXISTS `t_reprot_template` (
  `id` bigint(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL COMMENT '模板名',
  `tContents` blob COMMENT '模板路径',
  `createTime` datetime DEFAULT NULL COMMENT '创建时间',
  `updateTime` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='报表模板';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_stock_exchange
DROP TABLE IF EXISTS `t_stock_exchange`;
CREATE TABLE IF NOT EXISTS `t_stock_exchange` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `STOCK_EXCHANGE_CODE` int(11) NOT NULL COMMENT '交易所代码',
  `NAME` int(11) DEFAULT NULL COMMENT '交易所名称',
  PRIMARY KEY (`ID`,`STOCK_EXCHANGE_CODE`),
  KEY `Index_STOCK_EXCHANGE_CODE` (`STOCK_EXCHANGE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='证券交易所';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_stock_plate
DROP TABLE IF EXISTS `t_stock_plate`;
CREATE TABLE IF NOT EXISTS `t_stock_plate` (
  `id` bigint(10) NOT NULL AUTO_INCREMENT,
  `plateCode` varchar(20) NOT NULL COMMENT '板块代码',
  `name` varchar(50) DEFAULT NULL COMMENT '板块名称',
  PRIMARY KEY (`id`,`plateCode`),
  KEY `Index_plateCode` (`plateCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='股票板块';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_system_user
DROP TABLE IF EXISTS `t_system_user`;
CREATE TABLE IF NOT EXISTS `t_system_user` (
  `id` bigint(10) NOT NULL AUTO_INCREMENT,
  `userCode` varchar(50) DEFAULT NULL,
  `userName` varchar(50) DEFAULT NULL,
  `password` varchar(50) DEFAULT NULL,
  `groupId` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_tablename_classname
DROP TABLE IF EXISTS `t_tablename_classname`;
CREATE TABLE IF NOT EXISTS `t_tablename_classname` (
  `className` varchar(100) DEFAULT NULL COMMENT '实体小写类名',
  `tableName` varchar(100) DEFAULT NULL COMMENT '表名',
  `resultMapName` varchar(500) DEFAULT NULL COMMENT '名称空间.映般id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_table_system
DROP TABLE IF EXISTS `t_table_system`;
CREATE TABLE IF NOT EXISTS `t_table_system` (
  `table_system_code` varchar(50) NOT NULL COMMENT '报表体系编码',
  `table_system_name` varchar(50) NOT NULL COMMENT '报表体系名',
  PRIMARY KEY (`table_system_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='报表体系';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_table_system_company_sub
DROP TABLE IF EXISTS `t_table_system_company_sub`;
CREATE TABLE IF NOT EXISTS `t_table_system_company_sub` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT COMMENT '序号',
  `COMPANY_CODE` varchar(50) NOT NULL COMMENT '公司代码',
  `COMPANY_NAME` varchar(50) NOT NULL COMMENT '公司名称',
  `TABLE_SYSTEM_CODE` varchar(50) NOT NULL COMMENT '报体代码',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='报表体系与公司对应关系表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_user
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE IF NOT EXISTS `t_user` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `USER_CODE` varchar(20) DEFAULT NULL COMMENT '用户代码',
  `TYPE` int(11) DEFAULT NULL COMMENT '用户类型\r\n            1.个人\r\n            2.公司',
  `RANK` int(11) DEFAULT NULL COMMENT '用户等级\r\n            0：普通',
  `USER_NAME` varchar(20) DEFAULT NULL COMMENT '用户姓名',
  `SEX` varchar(2) DEFAULT NULL COMMENT '性别',
  `BIRTH_DATA` date DEFAULT NULL COMMENT '出生日期',
  `AREA` varchar(50) DEFAULT NULL COMMENT '所属地区',
  `SELE_STOCK` varchar(10) DEFAULT NULL COMMENT '自选股',
  PRIMARY KEY (`ID`),
  KEY `Index_USER_CODE` (`USER_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_user_attacheed
DROP TABLE IF EXISTS `t_user_attacheed`;
CREATE TABLE IF NOT EXISTS `t_user_attacheed` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `USER_CODE` bigint(10) NOT NULL COMMENT '用户代码',
  PRIMARY KEY (`ID`,`USER_CODE`),
  KEY `Index_USER_CODE` (`USER_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户信息扩展表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_warn_rule
DROP TABLE IF EXISTS `t_warn_rule`;
CREATE TABLE IF NOT EXISTS `t_warn_rule` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `WARN_RULE_CODE` varchar(20) DEFAULT NULL COMMENT '警告规则代码',
  `RULE` varchar(50) DEFAULT NULL COMMENT '规则',
  `TYPE` int(11) DEFAULT NULL COMMENT '规则类型\r\n            0:标准\r\n            1:自定义',
  PRIMARY KEY (`ID`),
  KEY `Index_WARN_RULE_CODE` (`WARN_RULE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='告警规则管理表';

# Data exporting was unselected.


# Dumping structure for table stock_zjs.t_warn_rule_relation
DROP TABLE IF EXISTS `t_warn_rule_relation`;
CREATE TABLE IF NOT EXISTS `t_warn_rule_relation` (
  `ID` bigint(10) NOT NULL AUTO_INCREMENT,
  `WARN_RULE_CODE` varchar(20) DEFAULT NULL COMMENT '警告规则代码',
  `USER_CODE` varchar(20) DEFAULT NULL COMMENT '用户代码',
  PRIMARY KEY (`ID`),
  KEY `Index_WARN_RULE_CODE` (`WARN_RULE_CODE`),
  KEY `FK_Reference_20` (`USER_CODE`),
  CONSTRAINT `FK_Reference_20` FOREIGN KEY (`USER_CODE`) REFERENCES `t_user` (`USER_CODE`),
  CONSTRAINT `FK_Reference_21` FOREIGN KEY (`WARN_RULE_CODE`) REFERENCES `t_warn_rule` (`WARN_RULE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='告警关系表';

# Data exporting was unselected.
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
