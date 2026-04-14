package com.wsb.social.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏删除 DTO
 */
@Data
public class CollectDeleteDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏 ID
     */
    @JsonAlias({"collectId", "collect_id"})
    @NotNull(message = "收藏ID不能为空")
    private Long collectId;
}
