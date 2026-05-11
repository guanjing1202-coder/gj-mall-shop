package com.gj.mall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.dto.BrandDTO;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final PmsBrandMapper mapper;

    @Override
    public PageResult<PmsBrand> page(String keyword, Long pageNum, Long pageSize) {
        Page<PmsBrand> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        IPage<PmsBrand> result = mapper.selectPage(page,
                Wrappers.<PmsBrand>lambdaQuery()
                        .like(StrUtil.isNotBlank(keyword), PmsBrand::getName, keyword)
                        .orderByAsc(PmsBrand::getSort)
                        .orderByDesc(PmsBrand::getCreateTime));
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public List<PmsBrand> listAll() {
        return mapper.selectList(Wrappers.<PmsBrand>lambdaQuery()
                .eq(PmsBrand::getShowStatus, 1)
                .orderByAsc(PmsBrand::getSort));
    }

    @Override
    public Long save(BrandDTO dto) {
        PmsBrand entity = new PmsBrand();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(null);
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getShowStatus() == null) entity.setShowStatus(1);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(BrandDTO dto) {
        if (dto.getId() == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少品牌ID");
        PmsBrand exists = mapper.selectById(dto.getId());
        if (exists == null) throw new BizException(ResultCode.DATA_NOT_FOUND);
        PmsBrand entity = new PmsBrand();
        BeanUtil.copyProperties(dto, entity);
        mapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
