package com.wsb.community.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 群组成员操作DTO
 */
@Data
public class GroupUserOperateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    /**
     * 用户ID列表
     */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;

    /**
     * 操作类型：add-拉人进群，minus-踢人出群
     */
    @NotNull(message = "操作类型不能为空")
    private String type;
}