package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminReportQueryDTO;
import com.gj.mall.admin.vo.AdminSalesReportVO;

public interface AdminReportService {

    AdminSalesReportVO salesReport(AdminReportQueryDTO query);

    String exportSalesReportCsv(AdminReportQueryDTO query);
}
