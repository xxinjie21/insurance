package com.xxj.insurance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.ConsumableCatalog;
import com.xxj.insurance.domain.po.DrugCatalog;
import com.xxj.insurance.domain.po.TreatmentCatalog;
import com.xxj.insurance.mapper.ConsumableCatalogMapper;
import com.xxj.insurance.mapper.DrugCatalogMapper;
import com.xxj.insurance.mapper.TreatmentCatalogMapper;
import com.xxj.insurance.service.ICatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 医保目录服务实现
 *
 * @author xxj
 * @since 2026-07-18
 */
@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements ICatalogService {

    private final DrugCatalogMapper drugCatalogMapper;
    private final TreatmentCatalogMapper treatmentCatalogMapper;
    private final ConsumableCatalogMapper consumableCatalogMapper;

    // ---- 药品目录 ----

    @Override
    public Result drugList(PageDTO pageDTO, String keyword) {
        pageDTO = normalizePage(pageDTO);
        Page<DrugCatalog> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<DrugCatalog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(DrugCatalog::getName, keyword)
                             .or().like(DrugCatalog::getCode, keyword)
                             .or().like(DrugCatalog::getSpecification, keyword));
        }
        wrapper.orderByAsc(DrugCatalog::getCode);
        Page<DrugCatalog> result = drugCatalogMapper.selectPage(page, wrapper);
        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public DrugCatalog getDrugById(Long id) {
        return drugCatalogMapper.selectById(id);
    }

    // ---- 诊疗目录 ----

    @Override
    public Result treatmentList(PageDTO pageDTO, String keyword) {
        pageDTO = normalizePage(pageDTO);
        Page<TreatmentCatalog> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<TreatmentCatalog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(TreatmentCatalog::getName, keyword)
                             .or().like(TreatmentCatalog::getCode, keyword));
        }
        wrapper.orderByAsc(TreatmentCatalog::getCode);
        Page<TreatmentCatalog> result = treatmentCatalogMapper.selectPage(page, wrapper);
        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public TreatmentCatalog getTreatmentById(Long id) {
        return treatmentCatalogMapper.selectById(id);
    }

    // ---- 耗材目录 ----

    @Override
    public Result consumableList(PageDTO pageDTO, String keyword) {
        pageDTO = normalizePage(pageDTO);
        Page<ConsumableCatalog> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        LambdaQueryWrapper<ConsumableCatalog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ConsumableCatalog::getName, keyword)
                             .or().like(ConsumableCatalog::getCode, keyword));
        }
        wrapper.orderByAsc(ConsumableCatalog::getCode);
        Page<ConsumableCatalog> result = consumableCatalogMapper.selectPage(page, wrapper);
        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public ConsumableCatalog getConsumableById(Long id) {
        return consumableCatalogMapper.selectById(id);
    }

    private PageDTO normalizePage(PageDTO pageDTO) {
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            return new PageDTO(1, 20);
        }
        return pageDTO;
    }
}
