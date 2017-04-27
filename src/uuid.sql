use uuid;

CREATE TABLE if NOT EXISTS uuid.interval (
 id INT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
 data_center_id INT NOT NULL DEFAULT 0 COMMENT '数据中心id',
 worker_id INT NOT NULL DEFAULT 0 COMMENT '工作节点id',
 start_time BIGINT(20) NOT NULL DEFAULT 0 COMMENT '租期开始时间',
 end_time BIGINT(20) NOT NULL DEFAULT 0 COMMENT '租期结束时间',
 PRIMARY KEY(id),
 UNIQUE KEY idx_data_center_worker (data_center_id, worker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='节点租期表';