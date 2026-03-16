package com.wsb.community.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 群组更新DTO
 */
@Data
public class GroupUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 群组ID
     */
    @NotNull(message = "群组ID不能为空")
    private Long groupId;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 群组描述
     */
    private String remark;
}