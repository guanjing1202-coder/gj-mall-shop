package com.gj.mall.admin.service;

import com.gj.mall.admin.vo.DashboardOverviewVO;
import com.gj.mall.admin.vo.DashboardBusinessVO;

public interface AdminDashboardService {

    DashboardOverviewVO overview();

    DashboardBusinessVO business();
}
