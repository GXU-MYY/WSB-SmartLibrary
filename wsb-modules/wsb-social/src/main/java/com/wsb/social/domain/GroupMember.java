package com.wsb.social.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@TableName("t_group_user") // 显式映射数据库表名
public class GroupMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID (如果有关联表主键)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 群组 ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;
}