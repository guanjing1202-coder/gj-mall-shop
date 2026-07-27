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
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final PmsBrandMapper mapper;
    private final PmsSpuMapper spuMapper;

    @Override
    public PageResult<PmsBrand> page(String keyword, Long pageNum, Long pageSize) {
        String actualKeyword = StrUtil.trim(keyword);
        Page<PmsBrand> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        IPage<PmsBrand> result = mapper.selectPage(page,
                Wrappers.<PmsBrand>lambdaQuery()
                        .like(StrUtil.isNotBlank(actualKeyword), PmsBrand::getName, actualKeyword)
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
        validateSaveDTO(dto);
        PmsBrand entity = new PmsBrand();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(null);
        entity.setName(StrUtil.trim(dto.getName()));
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getShowStatus() == null) entity.setShowStatus(1);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(BrandDTO dto) {
        validateUpdateDTO(dto);
        PmsBrand exists = mapper.selectById(dto.getId());
        if (exists == null) throw new BizException(ResultCode.DATA_NOT_FOUND);
        PmsBrand entity = new PmsBrand();
        BeanUtil.copyProperties(dto, entity);
        entity.setName(StrUtil.trim(dto.getName()));
        mapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        if (id == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少品牌ID");
        PmsBrand exists = mapper.selectById(id);
        if (exists == null) {
            return;
        }
        Long productCount = spuMapper.selectCount(Wrappers.<PmsSpu>lambdaQuery()
                .eq(PmsSpu::getBrandId, id));
        if (productCount != null && productCount > 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "品牌下存在商品，不能删除");
        }
        mapper.deleteById(id);
    }

    private void validateSaveDTO(BrandDTO dto) {
        validateDTO(dto);
    }

    private void validateUpdateDTO(BrandDTO dto) {
        validateDTO(dto);
        if (dto.getId() == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少品牌ID");
    }

    private void validateDTO(BrandDTO dto) {
        if (dto == null) throw new BizException(ResultCode.PARAM_MISSING, "品牌信息不能为空");
        if (StrUtil.isBlank(dto.getName())) throw new BizException(ResultCode.PARAM_MISSING, "品牌名不能为空");
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
