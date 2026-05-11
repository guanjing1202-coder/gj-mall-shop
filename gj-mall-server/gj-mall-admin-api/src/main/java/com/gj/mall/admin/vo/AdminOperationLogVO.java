package com.gj.mall.admin.vo;

import com.gj.mall.admin.entity.SysOperationLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台操作日志")
public class AdminOperationLogVO {
    private Long id;
    private Long adminId;
    private String username;
    private String module;
    private String operation;
    private String requestMethod;
    private String requestUri;
    private String requestParams;
    private String ip;
    private Integer status;
    private String statusDesc;
    private String errorMessage;
    private Long costTime;
    private LocalDateTime createTime;

    public static AdminOperationLogVO from(SysOperationLog log) {
        AdminOperationLogVO vo = new AdminOperationLogVO();
        vo.setId(log.getId());
        vo.setAdminId(log.getAdminId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setOperation(log.getOperation());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestUri(log.getRequestUri());
        vo.setRequestParams(log.getRequestParams());
        vo.setIp(log.getIp());
        vo.setStatus(log.getStatus());
        vo.setStatusDesc(log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败");
        vo.setErrorMessage(log.getErrorMessage());
        vo.setCostTime(log.getCostTime());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
