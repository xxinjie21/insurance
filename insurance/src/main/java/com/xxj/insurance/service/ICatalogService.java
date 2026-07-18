package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.ConsumableCatalog;
import com.xxj.insurance.domain.po.DrugCatalog;
import com.xxj.insurance.domain.po.TreatmentCatalog;

/**
 * 医保目录服务接口 — 药品/诊疗/耗材统一查询
 *
 * @author xxj
 * @since 2026-07-18
 */
public interface ICatalogService {

    // ---- 药品目录 ----
    Result drugList(PageDTO pageDTO, String keyword);
    DrugCatalog getDrugById(Long id);

    // ---- 诊疗目录 ----
    Result treatmentList(PageDTO pageDTO, String keyword);
    TreatmentCatalog getTreatmentById(Long id);

    // ---- 耗材目录 ----
    Result consumableList(PageDTO pageDTO, String keyword);
    ConsumableCatalog getConsumableById(Long id);
}
