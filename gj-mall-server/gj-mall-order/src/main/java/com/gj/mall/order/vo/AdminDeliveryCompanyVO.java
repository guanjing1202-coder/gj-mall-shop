package com.gj.mall.order.vo;

import com.gj.mall.order.entity.OmsDeliveryCompany;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminDeliveryCompanyVO {
    private Long id;
    private String code;
    private String name;
    private String contactPhone;
    private Integer sort;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminDeliveryCompanyVO from(OmsDeliveryCompany company) {
        AdminDeliveryCompanyVO vo = new AdminDeliveryCompanyVO();
        vo.setId(company.getId());
        vo.setCode(company.getCode());
        vo.setName(company.getName());
        vo.setContactPhone(company.getContactPhone());
        vo.setSort(company.getSort());
        vo.setStatus(company.getStatus());
        vo.setStatusDesc(company.getStatus() != null && company.getStatus() == 1 ? "启用" : "停用");
        vo.setCreateTime(company.getCreateTime());
        vo.setUpdateTime(company.getUpdateTime());
        return vo;
    }
}
