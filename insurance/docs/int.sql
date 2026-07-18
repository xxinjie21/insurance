-- 医保核销系统 建表SQL(MySQL)
-- 包含完整表结构、字段注释、外键约束、测试数据
-- ----------------------------
-- 1. 创建数据库（不存在则创建）
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `medical_insurance`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 使用该数据库
USE `medical_insurance`;

-- 1. 医院表
DROP TABLE IF EXISTS `hospital`;
CREATE TABLE `hospital` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，医院ID(自增，非空，唯一标识医院主体)',
                            `name` VARCHAR(64) NOT NULL COMMENT '医院名称(非空，不可重复)',
                            `address` VARCHAR(255) DEFAULT NULL COMMENT '医院地址',
                            `phone` VARCHAR(20) DEFAULT NULL COMMENT '医院电话',
                            `password` VARCHAR(255) DEFAULT NULL COMMENT '医院密码(BCrypt加密)',
                            `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审批 1-已启用 2-已停用 3-已拒绝',
                            `level` TINYINT DEFAULT NULL COMMENT '医院等级：1-三甲 2-三乙 3-二甲 4-二乙 5-一级 6-社区',
                            `agreement_expire` DATE DEFAULT NULL COMMENT '定点协议有效期',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院表';

-- 医院表测试数据
INSERT INTO `hospital` (`name`, `address`, `phone`, `status`, `level`, `create_time`) VALUES
                                                             ('南昌市第一人民医院', '南昌市东湖区象山北路128号', '0791-88888888', 1, 1, NOW()),
                                                             ('江西省中医院', '南昌市八一大道445号', '0791-66666666', 1, 2, NOW()),
                                                             ('青山湖社区医院', '南昌市青山湖区南京东路235号', '0791-99999999', 1, 6, NOW());

-- 2. 用户表 (患者/医院/医保局/管理员)
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，用户 ID(自增，非空，唯一标识系统内所有用户)',
                        `password` VARCHAR(64) NOT NULL COMMENT '登录密码(BCrypt加密)',
                        `name` VARCHAR(32) NOT NULL COMMENT '姓名',
                        `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                        `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号 (仅患者使用，唯一)',
                        `hospital_id` BIGINT DEFAULT NULL COMMENT '所属医院 ID(仅医院账号使用)',
                        `role` TINYINT NOT NULL DEFAULT 1 COMMENT '角色：1-患者 2-医院 3-医保局 4-管理员',
                        `insurance_type` TINYINT DEFAULT NULL COMMENT '参保类型：1-职工 2-居民(仅患者)',
                        `insurance_no` VARCHAR(32) DEFAULT NULL COMMENT '医保个人编号(仅患者)',
                        `insurance_city` VARCHAR(32) DEFAULT NULL COMMENT '参保地(仅患者)',
                        `personal_account_balance` DECIMAL(12,2) DEFAULT 0.00 COMMENT '医保个人账户余额(仅职工参保患者)',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_id_card` (`id_card`),
                        KEY `fk_hospital_id` (`hospital_id`),
                        CONSTRAINT `fk_user_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表：患者/医院/医保局/管理员';

-- 用户表测试数据 (密码 123456 MD5 加密)
INSERT INTO `user` (`password`, `name`, `phone`, `id_card`, `hospital_id`, `role`, `insurance_type`, `insurance_no`, `insurance_city`, `personal_account_balance`, `create_time`) VALUES
                                                                                             ('e10adc3949ba59abbe56e057f20f883e', '张三', '13800001111', '360104199001011234', NULL, 1, 1, '360104199001011234', '南昌市', 5000.00, NOW()),
                                                                                             ('e10adc3949ba59abbe56e057f20f883e', '李四', '13800002222', '360102198505055678', NULL, 1, 2, '360102198505055678', '南昌市', 0.00, NOW()),
                                                                                             ('e10adc3949ba59abbe56e057f20f883e', '南昌市一院管理员', '13800003333', '360103198001010011', 1, 2, NULL, NULL, NULL, 0.00, NOW()),
                                                                                             ('e10adc3949ba59abbe56e057f20f883e', '省中医院管理员', '13800004444', '360103198001010022', 2, 2, NULL, NULL, NULL, 0.00, NOW()),
                                                                                             ('e10adc3949ba59abbe56e057f20f883e', '医保局审核员', '13800005555', '360103198001010033', NULL, 3, NULL, NULL, NULL, 0.00, NOW()),
                                                                                             ('e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800006666', '360103198001010044', NULL, 4, NULL, NULL, NULL, 0.00, NOW());

-- 3. 就诊表
DROP TABLE IF EXISTS `visit`;
CREATE TABLE `visit` (
                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，就诊ID',
                         `user_id` BIGINT NOT NULL COMMENT '患者ID',
                         `hospital_id` BIGINT NOT NULL COMMENT '就诊医院ID',
                         `type` TINYINT DEFAULT 1 COMMENT '就诊类型：1-门诊 2-住院',
                         `dept` VARCHAR(32) DEFAULT NULL COMMENT '就诊科室',
                         `doctor_name` VARCHAR(32) DEFAULT NULL COMMENT '接诊医生姓名',
                         `diagnosis` VARCHAR(255) NOT NULL COMMENT '诊断结果',
                         `status` TINYINT DEFAULT 0 COMMENT '状态：0-就诊中 1-已结算',
                         `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '就诊时间',
                         PRIMARY KEY (`id`),
                         KEY `fk_user_id` (`user_id`),
                         KEY `fk_visit_hospital` (`hospital_id`),
                         CONSTRAINT `fk_visit_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
                         CONSTRAINT `fk_visit_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就诊表';

-- 就诊表测试数据
INSERT INTO `visit` (`user_id`, `hospital_id`, `type`, `dept`, `doctor_name`, `diagnosis`, `status`, `create_time`) VALUES
                                                                                                 (1, 1, 1, '呼吸内科', '王医生', '上呼吸道感染', 1, NOW()),
                                                                                                 (2, 2, 1, '心内科', '陈医生', '高血压1级', 1, NOW()),
                                                                                                 (1, 3, 2, '消化内科', '刘医生', '急性肠胃炎', 0, NOW());

-- 4. 费用明细表
DROP TABLE IF EXISTS `fee`;
CREATE TABLE `fee` (
                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，费用ID',
                       `visit_id` BIGINT NOT NULL COMMENT '就诊ID',
                       `name` VARCHAR(64) NOT NULL COMMENT '项目名称(药品/检查)',
                       `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
                       `num` INT NOT NULL DEFAULT 1 COMMENT '数量',
                       `total` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
                       `type` TINYINT NOT NULL COMMENT '费用类别：1-甲类 2-乙类 3-自费',
                       `insurance_code` VARCHAR(32) DEFAULT NULL COMMENT '医保项目编码',
                       `specification` VARCHAR(64) DEFAULT NULL COMMENT '药品规格',
                       `usage_method` VARCHAR(64) DEFAULT NULL COMMENT '用法用量',
                       `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
                       PRIMARY KEY (`id`),
                       KEY `fk_visit_id` (`visit_id`),
                       CONSTRAINT `fk_fee_visit` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用明细表';

-- 费用明细测试数据
INSERT INTO `fee` (`visit_id`, `name`, `price`, `num`, `total`, `type`, `insurance_code`, `specification`, `usage_method`, `create_time`) VALUES
                                                                                           (1, '阿莫西林胶囊', 15.50, 2, 31.00, 1, 'XL01AA01', '0.5g*24粒', '口服 每日3次 每次2粒', NOW()),
                                                                                           (1, '血常规检查', 25.00, 1, 25.00, 1, 'ZL250101', NULL, NULL, NOW()),
                                                                                           (2, '硝苯地平缓释片', 28.80, 1, 28.80, 2, 'XL02BB01', '30mg*7片', '口服 每日1次 每次1片', NOW()),
                                                                                           (2, '心电图检查', 40.00, 1, 40.00, 2, 'ZL250301', NULL, NULL, NOW());

-- 5. 结算表
DROP TABLE IF EXISTS `settle`;
CREATE TABLE `settle` (
                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，结算ID',
                          `visit_id` BIGINT NOT NULL COMMENT '就诊ID',
                          `hospital_id` BIGINT NOT NULL COMMENT '医院ID',
                          `total` DECIMAL(10,2) NOT NULL COMMENT '总费用',
                          `reimburse` DECIMAL(10,2) NOT NULL COMMENT '医保报销金额',
                          `self_pay` DECIMAL(10,2) NOT NULL COMMENT '个人自付金额',
                          `pooling_pay` DECIMAL(10,2) DEFAULT 0.00 COMMENT '统筹支付金额',
                          `account_pay` DECIMAL(10,2) DEFAULT 0.00 COMMENT '个人账户支付金额',
                          `cash_pay` DECIMAL(10,2) DEFAULT 0.00 COMMENT '个人现金支付金额',
                          `status` TINYINT DEFAULT 0 COMMENT '状态：0-待申报 1-已申报 2-已自付 3-已拨付',
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '结算时间',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_visit_id` (`visit_id`),
                          KEY `fk_settle_hospital` (`hospital_id`),
                          CONSTRAINT `fk_settle_visit` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`id`),
                          CONSTRAINT `fk_settle_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算表';

-- 结算测试数据
INSERT INTO `settle` (`visit_id`, `hospital_id`, `total`, `reimburse`, `self_pay`, `pooling_pay`, `account_pay`, `cash_pay`, `status`, `create_time`) VALUES
                                                                                                                (1, 1, 56.00, 56.00, 0.00, 56.00, 0.00, 0.00, 0, NOW()),
                                                                                                                (2, 2, 68.80, 55.04, 13.76, 55.04, 0.00, 13.76, 0, NOW());

-- 6. 申报批次表
DROP TABLE IF EXISTS `batch`;
CREATE TABLE `batch` (
                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，批次 ID',
                         `hospital_id` BIGINT NOT NULL COMMENT '申报医院 ID',
                         `batch_no` VARCHAR(32) NOT NULL COMMENT '批次号 (唯一)',
                         `settle_cnt` INT NOT NULL COMMENT '结算单总笔数',
                         `total_amt` DECIMAL(12,2) NOT NULL COMMENT '申报总金额',
                         `status` TINYINT DEFAULT 0 COMMENT '状态：0-待申报 1-已申报 2-已完成 3-拨付拒绝',
                         `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申报时间',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_batch_no` (`batch_no`),
                         KEY `fk_batch_hospital` (`hospital_id`),
                         CONSTRAINT `fk_batch_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报批次表';

-- 申报批次测试数据
INSERT INTO `batch` (`hospital_id`, `batch_no`, `settle_cnt`, `total_amt`, `status`, `create_time`) VALUES
                                                                                                        (1, '202604001', 1, 56.00, 0, NOW()),
                                                                                                        (2, '202604002', 1, 55.04, 0, NOW());

-- 7. 申报明细表
DROP TABLE IF EXISTS `batch_item`;
CREATE TABLE `batch_item` (
                              `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，明细ID',
                              `batch_id` BIGINT NOT NULL COMMENT '所属批次ID',
                              `settle_id` BIGINT NOT NULL COMMENT '关联结算单ID',
                              `audit` TINYINT DEFAULT 0 COMMENT '审核结果：0-通过 1-扣款',
                              `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              PRIMARY KEY (`id`),
                              KEY `fk_batch_id` (`batch_id`),
                              KEY `fk_settle_id` (`settle_id`),
                              CONSTRAINT `fk_batch_item_batch` FOREIGN KEY (`batch_id`) REFERENCES `batch` (`id`),
                              CONSTRAINT `fk_batch_item_settle` FOREIGN KEY (`settle_id`) REFERENCES `settle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报明细表';

-- 申报明细测试数据
INSERT INTO `batch_item` (`batch_id`, `settle_id`, `audit`, `create_time`) VALUES
                                                                (1, 1, 0, NOW()),
                                                                (2, 2, 0, NOW());

-- 8. 基金拨付表
DROP TABLE IF EXISTS `pay`;
CREATE TABLE `pay` (
                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，拨付ID',
                       `batch_id` BIGINT NOT NULL COMMENT '对应批次ID',
                       `hospital_id` BIGINT NOT NULL COMMENT '收款医院ID',
                       `amount` DECIMAL(12,2) NOT NULL COMMENT '拨付金额',
                       `status` TINYINT DEFAULT 0 COMMENT '状态：0-待支付 1-已支付',
                       `pay_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '支付时间',
                       `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝拨付理由',
                       `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       PRIMARY KEY (`id`),
                       UNIQUE KEY `uk_batch_id` (`batch_id`),
                       KEY `fk_pay_hospital` (`hospital_id`),
                       CONSTRAINT `fk_pay_batch` FOREIGN KEY (`batch_id`) REFERENCES `batch` (`id`),
                       CONSTRAINT `fk_pay_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金拨付表';

-- 拨付测试数据
INSERT INTO `pay` (`batch_id`, `hospital_id`, `amount`, `status`, `pay_time`, `reject_reason`) VALUES
                                                                                  (1, 1, 56.00, 0, NOW(), NULL),
                                                                                  (2, 2, 55.04, 0, NOW(), NULL);

-- 更新就诊表已结算状态
UPDATE `visit` SET `status` = 1 WHERE `id` IN (1,2);

-- ----------------------------
-- 9. 患者账户表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `user_account`;
CREATE TABLE `user_account` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，账户 ID',
                                `user_id` BIGINT NOT NULL COMMENT '用户 ID(唯一)',
                                `balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
                                `total_recharge` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值金额',
                                `total_consumption` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
                                `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-冻结 1-正常',
                                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_user_id` (`user_id`),
                                CONSTRAINT `fk_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者账户表';

-- ----------------------------
-- 10. 充值记录表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `recharge_record`;
CREATE TABLE `recharge_record` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，充值记录 ID',
                                   `user_id` BIGINT NOT NULL COMMENT '用户 ID',
                                   `order_no` VARCHAR(32) NOT NULL COMMENT '充值订单号 (唯一)',
                                   `amount` DECIMAL(12,2) NOT NULL COMMENT '充值金额',
                                   `type` TINYINT NOT NULL DEFAULT 1 COMMENT '充值类型：1-微信 2-支付宝 3-银行卡 4-现金',
                                   `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付 1-支付成功 2-支付失败 3-已退款',
                                   `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
                                   `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
                                   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_order_no` (`order_no`),
                                   KEY `fk_recharge_user` (`user_id`),
                                   CONSTRAINT `fk_recharge_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值记录表';

-- ----------------------------
-- 11. 消费记录表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `consumption_record`;
CREATE TABLE `consumption_record` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，消费记录 ID',
                                      `user_id` BIGINT NOT NULL COMMENT '用户 ID',
                                      `visit_id` BIGINT DEFAULT NULL COMMENT '就诊 ID(可选)',
                                      `settle_id` BIGINT DEFAULT NULL COMMENT '结算 ID(可选)',
                                      `order_no` VARCHAR(32) NOT NULL COMMENT '消费订单号 (唯一)',
                                      `amount` DECIMAL(12,2) NOT NULL COMMENT '消费金额',
                                      `type` TINYINT NOT NULL DEFAULT 1 COMMENT '消费类型：1-就诊支付 2-退款 3-调整',
                                      `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-取消 1-成功',
                                      `balance_before` DECIMAL(12,2) NOT NULL COMMENT '消费前余额',
                                      `balance_after` DECIMAL(12,2) NOT NULL COMMENT '消费后余额',
                                      `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
                                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_order_no` (`order_no`),
                                      KEY `fk_consumption_user` (`user_id`),
                                      KEY `fk_consumption_visit` (`visit_id`),
                                      KEY `fk_consumption_settle` (`settle_id`),
                                      CONSTRAINT `fk_consumption_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
                                      CONSTRAINT `fk_consumption_visit` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`id`),
                                      CONSTRAINT `fk_consumption_settle` FOREIGN KEY (`settle_id`) REFERENCES `settle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费记录表';

-- 初始化患者账户数据（为已有患者创建账户）
INSERT INTO `user_account` (`user_id`, `balance`, `total_recharge`, `total_consumption`, `status`)
SELECT id, 0.00, 0.00, 0.00, 1 FROM `user` WHERE `role` = 1;

-- ----------------------------
-- 12. 报销规则表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `reimburse_rule`;
CREATE TABLE `reimburse_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `insurance_type` TINYINT NOT NULL COMMENT '参保类型：1-职工 2-居民',
    `hospital_level` TINYINT NOT NULL COMMENT '医院等级：1-三甲 2-三乙 3-二甲 4-二乙 5-一级 6-社区',
    `visit_type` TINYINT NOT NULL COMMENT '就诊类型：1-门诊 2-住院',
    `deductible` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '起付线',
    `reimburse_ratio` DECIMAL(4,3) NOT NULL COMMENT '统筹报销比例(0~1)',
    `annual_cap` DECIMAL(12,2) NOT NULL COMMENT '年度封顶线',
    `category_b_self_ratio` DECIMAL(4,3) NOT NULL DEFAULT 0.200 COMMENT '乙类先自付比例(0~1)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保报销规则表';

-- 报销规则种子数据（南昌市为例）
INSERT INTO `reimburse_rule` (`insurance_type`, `hospital_level`, `visit_type`, `deductible`, `reimburse_ratio`, `annual_cap`, `category_b_self_ratio`) VALUES
-- 职工医保 门诊
(1, 1, 1, 300.00, 0.550, 2000.00, 0.200),   -- 三甲门诊：起付300 报销55% 封顶2000
(1, 2, 1, 300.00, 0.600, 2000.00, 0.200),   -- 三乙门诊
(1, 3, 1, 200.00, 0.650, 2000.00, 0.200),   -- 二甲门诊
(1, 4, 1, 200.00, 0.650, 2000.00, 0.200),   -- 二乙门诊
(1, 5, 1, 100.00, 0.700, 2000.00, 0.200),   -- 一级门诊
(1, 6, 1, 0.00,   0.750, 2000.00, 0.200),   -- 社区门诊：0起付 报销75%
-- 职工医保 住院
(1, 1, 2, 800.00, 0.850, 300000.00, 0.200),  -- 三甲住院：起付800 报销85% 封顶30万
(1, 2, 2, 800.00, 0.850, 300000.00, 0.200),
(1, 3, 2, 500.00, 0.880, 300000.00, 0.200),
(1, 4, 2, 500.00, 0.880, 300000.00, 0.200),
(1, 5, 2, 300.00, 0.900, 300000.00, 0.200),
(1, 6, 2, 200.00, 0.920, 300000.00, 0.200),
-- 居民医保 门诊
(2, 1, 1, 200.00, 0.500, 800.00,  0.200),   -- 三甲门诊：起付200 报销50% 封顶800
(2, 2, 1, 200.00, 0.500, 800.00,  0.200),
(2, 3, 1, 100.00, 0.550, 800.00,  0.200),
(2, 4, 1, 100.00, 0.550, 800.00,  0.200),
(2, 5, 1, 0.00,   0.600, 800.00,  0.200),
(2, 6, 1, 0.00,   0.650, 800.00,  0.200),
-- 居民医保 住院
(2, 1, 2, 600.00, 0.750, 150000.00, 0.200),  -- 三甲住院：起付600 报销75% 封顶15万
(2, 2, 2, 600.00, 0.750, 150000.00, 0.200),
(2, 3, 2, 400.00, 0.800, 150000.00, 0.200),
(2, 4, 2, 400.00, 0.800, 150000.00, 0.200),
(2, 5, 2, 200.00, 0.850, 150000.00, 0.200),
(2, 6, 2, 100.00, 0.880, 150000.00, 0.200);

-- ----------------------------
-- 13. 年度累计表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `year_accumulate`;
CREATE TABLE `year_accumulate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '参保人ID',
    `year` INT NOT NULL COMMENT '年份',
    `deductible_used` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '本年度已累计起付线金额',
    `pooling_total` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '本年度统筹支付累计',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_year` (`user_id`, `year`),
    CONSTRAINT `fk_accumulate_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参保人年度累计表';

-- ----------------------------
-- 14. 药品目录表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `drug_catalog`;
CREATE TABLE `drug_catalog` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(32) NOT NULL COMMENT '医保药品编码',
    `name` VARCHAR(128) NOT NULL COMMENT '药品通用名',
    `specification` VARCHAR(64) DEFAULT NULL COMMENT '规格',
    `manufacturer` VARCHAR(128) DEFAULT NULL COMMENT '生产厂家',
    `category` TINYINT NOT NULL COMMENT '甲乙分类：1-甲类 2-乙类 3-自费',
    `self_pay_ratio` DECIMAL(4,3) DEFAULT 0.000 COMMENT '乙类自付比例(仅乙类有效)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注（限制使用范围等）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品目录表';

-- 药品目录种子数据
INSERT INTO `drug_catalog` (`code`, `name`, `specification`, `manufacturer`, `category`, `self_pay_ratio`, `remark`) VALUES
('XL01AA01', '阿莫西林胶囊', '0.5g*24粒', '华北制药', 1, 0.000, NULL),
('XL01AA02', '头孢克洛胶囊', '0.25g*12粒', '广州白云山', 1, 0.000, NULL),
('XL02BB01', '硝苯地平缓释片', '30mg*7片', '拜耳医药', 2, 0.200, NULL),
('XL02BB02', '氨氯地平片', '5mg*7片', '辉瑞制药', 2, 0.200, NULL),
('XL02BB03', '厄贝沙坦片', '150mg*7片', '赛诺菲', 2, 0.200, NULL),
('XL03CC01', '阿托伐他汀钙片', '20mg*7片', '辉瑞制药', 2, 0.250, '限高胆固醇血症'),
('XL03CC02', '瑞舒伐他汀钙片', '10mg*7片', '阿斯利康', 2, 0.250, '限高胆固醇血症'),
('XL04DD01', '奥司他韦胶囊', '75mg*10粒', '罗氏制药', 2, 0.200, '限流感重症'),
('XL05EE01', '布洛芬缓释胶囊', '0.3g*20粒', '中美史克', 1, 0.000, NULL),
('XL05EE02', '对乙酰氨基酚片', '0.5g*12片', '强生制药', 1, 0.000, NULL),
('XL06FF01', '胰岛素注射液', '400IU/10ml', '诺和诺德', 1, 0.000, '限糖尿病患者'),
('XL07GG01', '甲钴胺片', '0.5mg*20片', '卫材药业', 2, 0.100, NULL);

-- ----------------------------
-- 15. 诊疗项目目录表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `treatment_catalog`;
CREATE TABLE `treatment_catalog` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(32) NOT NULL COMMENT '诊疗项目编码',
    `name` VARCHAR(128) NOT NULL COMMENT '项目名称',
    `project_type` VARCHAR(32) DEFAULT NULL COMMENT '项目类别：检查/检验/手术/治疗/护理',
    `category` TINYINT NOT NULL COMMENT '甲乙分类：1-甲类 2-乙类 3-自费',
    `unit_price_cap` DECIMAL(10,2) DEFAULT NULL COMMENT '单价上限',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_t` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊疗项目目录表';

-- 诊疗项目种子数据
INSERT INTO `treatment_catalog` (`code`, `name`, `project_type`, `category`, `unit_price_cap`, `remark`) VALUES
('ZL250101', '血常规检查', '检验', 1, 25.00, NULL),
('ZL250102', '尿常规检查', '检验', 1, 15.00, NULL),
('ZL250103', '肝功能全套', '检验', 1, 80.00, NULL),
('ZL250104', '肾功能检查', '检验', 1, 60.00, NULL),
('ZL250201', '胸部X线摄影', '检查', 1, 50.00, NULL),
('ZL250301', '心电图检查', '检查', 1, 40.00, NULL),
('ZL250302', '动态心电图', '检查', 2, 200.00, NULL),
('ZL250401', '腹部B超', '检查', 1, 120.00, NULL),
('ZL250501', '胃镜检查', '检查', 2, 300.00, NULL),
('ZL250502', '肠镜检查', '检查', 2, 400.00, NULL),
('ZL250601', '普通门诊诊查费', '诊查', 1, 15.00, NULL),
('ZL250602', '专家门诊诊查费', '诊查', 2, 30.00, '限副主任医师以上'),
('ZL250701', '一级护理', '护理', 1, 20.00, NULL),
('ZL250702', '二级护理', '护理', 1, 12.00, NULL),
('ZL250801', '清创缝合（小）', '手术', 1, 80.00, NULL),
('ZL250802', '清创缝合（中）', '手术', 1, 150.00, NULL);

-- ----------------------------
-- 16. 耗材目录表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `consumable_catalog`;
CREATE TABLE `consumable_catalog` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(32) NOT NULL COMMENT '耗材编码',
    `name` VARCHAR(128) NOT NULL COMMENT '耗材名称',
    `specification` VARCHAR(64) DEFAULT NULL COMMENT '规格',
    `category` TINYINT NOT NULL COMMENT '甲乙分类：1-甲类 2-乙类 3-自费',
    `limit_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '限额（超出部分自费）',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_c` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='耗材目录表';

-- 耗材目录种子数据
INSERT INTO `consumable_catalog` (`code`, `name`, `specification`, `category`, `limit_amount`, `remark`) VALUES
('HC00101', '一次性输液器', '带针', 1, 2.00, NULL),
('HC00102', '一次性注射器', '5ml', 1, 1.00, NULL),
('HC00103', '无菌纱布块', '8cm*10cm', 1, 0.50, NULL),
('HC00201', '血糖试纸', '50条/盒', 2, 100.00, '限糖尿病患者'),
('HC00202', '一次性导尿管', '双腔16Fr', 2, 15.00, NULL),
('HC00301', '人工髋关节', '陶瓷', 3, NULL, '高值耗材，按比例报销');

-- 修改 fee 表：增加 fee_date 支持每日费用清单
ALTER TABLE `fee` ADD COLUMN `fee_date` DATE DEFAULT NULL COMMENT '费用日期(住院每日清单)';

-- ----------------------------
-- 17. 门诊挂号表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `registration`;
CREATE TABLE `registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '患者ID',
    `hospital_id` BIGINT NOT NULL COMMENT '医院ID',
    `dept` VARCHAR(32) DEFAULT NULL COMMENT '挂号科室',
    `doctor_name` VARCHAR(32) DEFAULT NULL COMMENT '医生姓名',
    `reg_type` TINYINT NOT NULL DEFAULT 1 COMMENT '挂号类型：1-普通门诊 2-专家门诊',
    `reg_fee` DECIMAL(10,2) NOT NULL COMMENT '挂号费(医事服务费)',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-已挂号 1-已就诊 2-已取消',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '挂号时间',
    PRIMARY KEY (`id`),
    KEY `fk_reg_user` (`user_id`),
    KEY `fk_reg_hospital` (`hospital_id`),
    CONSTRAINT `fk_reg_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_reg_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门诊挂号表';

-- 挂号种子数据
INSERT INTO `registration` (`user_id`, `hospital_id`, `dept`, `doctor_name`, `reg_type`, `reg_fee`, `status`, `create_time`) VALUES
(1, 1, '呼吸内科', '王医生', 1, 15.00, 0, NOW()),
(2, 2, '心内科', '陈医生', 2, 30.00, 0, NOW());

-- ----------------------------
-- 18. 住院记录表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `inpatient`;
CREATE TABLE `inpatient` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `visit_id` BIGINT NOT NULL COMMENT '关联就诊ID',
    `user_id` BIGINT NOT NULL COMMENT '患者ID',
    `hospital_id` BIGINT NOT NULL COMMENT '医院ID',
    `inpatient_no` VARCHAR(32) NOT NULL COMMENT '住院号(唯一)',
    `bed_no` VARCHAR(16) DEFAULT NULL COMMENT '床位号',
    `admission_time` DATETIME NOT NULL COMMENT '入院时间',
    `discharge_time` DATETIME DEFAULT NULL COMMENT '出院时间',
    `deposit_total` DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计缴纳押金',
    `total_fee` DECIMAL(12,2) DEFAULT 0.00 COMMENT '住院总费用',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-住院中 1-已出院 2-出院结算中',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inpatient_no` (`inpatient_no`),
    UNIQUE KEY `uk_visit_id` (`visit_id`),
    KEY `fk_ip_user` (`user_id`),
    KEY `fk_ip_hospital` (`hospital_id`),
    CONSTRAINT `fk_ip_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_ip_visit` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`id`),
    CONSTRAINT `fk_ip_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='住院记录表';

-- 住院种子数据（visitId=3 是住院类型）
INSERT INTO `inpatient` (`visit_id`, `user_id`, `hospital_id`, `inpatient_no`, `bed_no`, `admission_time`, `deposit_total`, `total_fee`, `status`, `create_time`) VALUES
(3, 1, 3, 'IP202607180001', '301-01', NOW(), 2000.00, 0.00, 0, NOW());

-- ----------------------------
-- 19. 住院押金记录表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `inpatient_deposit`;
CREATE TABLE `inpatient_deposit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `inpatient_id` BIGINT NOT NULL COMMENT '住院记录ID',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1-缴纳押金 2-退押金',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `fk_dep_inpatient` (`inpatient_id`),
    CONSTRAINT `fk_dep_inpatient` FOREIGN KEY (`inpatient_id`) REFERENCES `inpatient` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='住院押金记录表';

-- 押金种子数据
INSERT INTO `inpatient_deposit` (`inpatient_id`, `amount`, `type`, `remark`, `create_time`) VALUES
(1, 2000.00, 1, '入院押金', NOW());

-- 修改 batch_item 表：增加调减金额字段
ALTER TABLE `batch_item` ADD COLUMN `adjust_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '审核调减金额';

-- ----------------------------
-- 20. 审核规则表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `audit_rule`;
CREATE TABLE `audit_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型：DIAG_DRUG_MATCH-诊断药品匹配 / DUPLICATE_DRUG-重复用药 / AGE_RESTRICT-年龄限制 / SEX_RESTRICT-性别限制',
    `description` VARCHAR(255) NOT NULL COMMENT '规则描述',
    `param_key` VARCHAR(128) DEFAULT NULL COMMENT '参数键（如诊断名/药品名）',
    `param_value` VARCHAR(512) DEFAULT NULL COMMENT '参数值（JSON或具体值）',
    `severity` TINYINT NOT NULL DEFAULT 1 COMMENT '严重程度：1-预警 2-扣款',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核规则表';

-- 审核规则种子数据
INSERT INTO `audit_rule` (`rule_type`, `description`, `param_key`, `param_value`, `severity`, `enabled`) VALUES
('DIAG_DRUG_MATCH', '高血压：硝苯地平/氨氯地平/厄贝沙坦适用', '高血压', '硝苯地平,氨氯地平,厄贝沙坦', 2, 1),
('DIAG_DRUG_MATCH', '上呼吸道感染：阿莫西林/头孢克洛/布洛芬/对乙酰氨基酚适用', '上呼吸道感染', '阿莫西林,头孢克洛,布洛芬,对乙酰氨基酚', 2, 1),
('DIAG_DRUG_MATCH', '高胆固醇血症：阿托伐他汀/瑞舒伐他汀适用', '高胆固醇血症', '阿托伐他汀,瑞舒伐他汀', 2, 1),
('DIAG_DRUG_MATCH', '糖尿病：胰岛素适用', '糖尿病', '胰岛素', 2, 1),
('DUPLICATE_DRUG', '阿莫西林与头孢克洛同为抗生素，不建议重复使用', '阿莫西林', '头孢克洛', 1, 1),
('DUPLICATE_DRUG', '硝苯地平与氨氯地平同为钙通道阻滞剂，不建议重复使用', '硝苯地平', '氨氯地平', 1, 1),
('AGE_RESTRICT', '阿托伐他汀限18岁以上使用', '阿托伐他汀', '18', 2, 1),
('AGE_RESTRICT', '瑞舒伐他汀限18岁以上使用', '瑞舒伐他汀', '18', 2, 1);

-- fee 表关联处方
ALTER TABLE `fee` ADD COLUMN `prescription_id` BIGINT DEFAULT NULL COMMENT '关联处方ID';

-- ----------------------------
-- 21. 医生表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `hospital_id` BIGINT NOT NULL COMMENT '所属医院ID',
    `name` VARCHAR(32) NOT NULL COMMENT '医生姓名',
    `dept` VARCHAR(32) NOT NULL COMMENT '科室',
    `title` VARCHAR(32) DEFAULT NULL COMMENT '职称：主任医师/副主任医师/主治医师/住院医师',
    `insurance_code` VARCHAR(32) DEFAULT NULL COMMENT '医保医师编码',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-停用 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_insurance_code` (`insurance_code`),
    KEY `fk_doc_hospital` (`hospital_id`),
    CONSTRAINT `fk_doc_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生表';

-- 医生种子数据
INSERT INTO `doctor` (`hospital_id`, `name`, `dept`, `title`, `insurance_code`, `status`) VALUES
(1, '王建华', '呼吸内科', '主任医师', 'DOC36010001', 1),
(1, '赵明', '消化内科', '副主任医师', 'DOC36010002', 1),
(2, '陈志强', '心内科', '主任医师', 'DOC36010003', 1),
(2, '孙丽', '内分泌科', '主治医师', 'DOC36010004', 1),
(3, '刘伟', '消化内科', '主治医师', 'DOC36010005', 1),
(3, '周芳', '全科', '副主任医师', 'DOC36010006', 1);

-- ----------------------------
-- 22. 处方表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `visit_id` BIGINT NOT NULL COMMENT '就诊ID',
    `doctor_id` BIGINT NOT NULL COMMENT '开方医生ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审核 1-审核通过 2-审核拒绝 3-已发药',
    `pharmacist_id` BIGINT DEFAULT NULL COMMENT '审核药师(暂用doctor表ID)',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝理由',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_rx_visit` (`visit_id`),
    KEY `fk_rx_doctor` (`doctor_id`),
    CONSTRAINT `fk_rx_visit` FOREIGN KEY (`visit_id`) REFERENCES `visit` (`id`),
    CONSTRAINT `fk_rx_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方表';

-- ----------------------------
-- 23. 异地就医备案表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `remote_medical_filing`;
CREATE TABLE `remote_medical_filing` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '参保人ID',
    `insured_city` VARCHAR(32) NOT NULL COMMENT '参保地',
    `treatment_city` VARCHAR(32) NOT NULL COMMENT '就医地',
    `treatment_hospital_id` BIGINT DEFAULT NULL COMMENT '就医医院ID(不指定则就医地所有定点均可)',
    `filing_status` TINYINT DEFAULT 1 COMMENT '备案状态：1-已备案 2-已过期 3-已取消',
    `start_date` DATE NOT NULL COMMENT '生效日期',
    `end_date` DATE DEFAULT NULL COMMENT '失效日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_rmf_user` (`user_id`),
    KEY `fk_rmf_hospital` (`treatment_hospital_id`),
    CONSTRAINT `fk_rmf_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_rmf_hospital` FOREIGN KEY (`treatment_hospital_id`) REFERENCES `hospital` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异地就医备案表';

-- 异地备案种子数据（张三在南昌参保，备案到南昌市一院）
INSERT INTO `remote_medical_filing` (`user_id`, `insured_city`, `treatment_city`, `treatment_hospital_id`, `filing_status`, `start_date`, `end_date`, `create_time`) VALUES
(1, '南昌市', '南昌市', 1, 1, '2026-01-01', '2026-12-31', NOW());

-- ----------------------------
-- 24. 慢特病认定表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `chronic_disease_cert`;
CREATE TABLE `chronic_disease_cert` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '参保人ID',
    `disease_type` VARCHAR(64) NOT NULL COMMENT '病种：高血压/糖尿病/冠心病/慢阻肺等',
    `cert_status` TINYINT DEFAULT 1 COMMENT '状态：1-有效 2-过期 3-撤销',
    `annual_cap` DECIMAL(12,2) NOT NULL COMMENT '年度限额',
    `reimburse_ratio` DECIMAL(4,3) NOT NULL COMMENT '报销比例',
    `start_date` DATE NOT NULL,
    `end_date` DATE DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_cdc_user` (`user_id`),
    CONSTRAINT `fk_cdc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门诊慢特病认定表';

-- 慢特病种子数据（张三有高血压慢病认定）
INSERT INTO `chronic_disease_cert` (`user_id`, `disease_type`, `cert_status`, `annual_cap`, `reimburse_ratio`, `start_date`, `end_date`) VALUES
(1, '高血压', 1, 5000.00, 0.800, '2026-01-01', '2026-12-31');

-- user 表增加医疗救助标记
ALTER TABLE `user` ADD COLUMN `medical_assistance` TINYINT DEFAULT 0 COMMENT '医疗救助标记：0-否 1-是(低保/特困)';
ALTER TABLE `settle` ADD COLUMN `catastrophic_pay` DECIMAL(10,2) DEFAULT 0.00 COMMENT '大病保险支付金额';
ALTER TABLE `settle` ADD COLUMN `assistance_pay` DECIMAL(10,2) DEFAULT 0.00 COMMENT '医疗救助支付金额';

-- ----------------------------
-- 25. 退款表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `refund`;
CREATE TABLE `refund` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `settle_id` BIGINT NOT NULL COMMENT '结算单ID',
    `visit_id` BIGINT NOT NULL COMMENT '就诊ID',
    `user_id` BIGINT NOT NULL COMMENT '患者ID',
    `total_refund` DECIMAL(10,2) NOT NULL COMMENT '退款总额',
    `pooling_refund` DECIMAL(10,2) DEFAULT 0.00 COMMENT '统筹退还金额',
    `account_refund` DECIMAL(10,2) DEFAULT 0.00 COMMENT '个账退还金额',
    `cash_refund` DECIMAL(10,2) DEFAULT 0.00 COMMENT '现金退还金额',
    `reason` VARCHAR(500) NOT NULL COMMENT '退款原因',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审批 1-已通过 2-已拒绝 3-已完成退款',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝理由',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_refund_settle` (`settle_id`),
    KEY `fk_refund_user` (`user_id`),
    CONSTRAINT `fk_refund_settle` FOREIGN KEY (`settle_id`) REFERENCES `settle` (`id`),
    CONSTRAINT `fk_refund_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';

-- ----------------------------
-- 26. 操作日志表（新增）
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
    `operation` VARCHAR(128) NOT NULL COMMENT '操作描述',
    `method` VARCHAR(256) DEFAULT NULL COMMENT '请求方法',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `result` VARCHAR(512) DEFAULT NULL COMMENT '执行结果摘要',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '耗时(毫秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_op_user` (`user_id`),
    KEY `idx_op_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';