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