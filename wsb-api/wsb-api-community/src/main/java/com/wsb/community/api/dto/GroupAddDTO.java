package com.wsb.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 群组新增DTO
 */
@Data
public class GroupAddDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 群组名称
     */
    @NotBlank(message = "群组名称不能为空")
    private String groupName;

    /**
     * 群成员用户ID列表
     */
    @NotBlank(message = "群成员不能为空")
    private List<Long> userIds;

    /**
     * 群组描述
     */
    private String remark;
}