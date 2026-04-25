package com.wsb.common.mybatis.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * MP自动填充
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

  private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Shanghai");

  private LocalDateTime now() {
    return LocalDateTime.now(APP_ZONE_ID);
  }

  @Override
  public void insertFill(MetaObject metaObject) {
    this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now());
    this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now());
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    this.setFieldValByName("updateTime", now(), metaObject);
  }

  /**
   * 获取当前用户ID
   */
  private String getUserId() {
    try {
      Object loginId = StpUtil.getLoginIdDefaultNull();
      return loginId != null ? loginId.toString() : null;
    } catch (Exception e) {
      log.warn("自动填充获取用户ID失败: {}", e.getMessage());
      return null;
    }
  }
}
