package com.xxj.insurance.controller;

import com.xxj.insurance.common.annotation.OperationLog;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.ICatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医保目录查询 Controller — 药品/诊疗/耗材
 *
 * @author xxj
 * @since 2026-07-18
 */
@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final ICatalogService catalogService;

    @OperationLog("查询药品目录")
    @GetMapping("/drug/list")
    @Permission({Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
    public Result drugList(PageDTO pageDTO,
                           @RequestParam(required = false) String keyword) {
        return catalogService.drugList(pageDTO, keyword);
    }

    @OperationLog("查询诊疗目录")
    @GetMapping("/treatment/list")
    @Permission({Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
    public Result treatmentList(PageDTO pageDTO,
                                @RequestParam(required = false) String keyword) {
        return catalogService.treatmentList(pageDTO, keyword);
    }

    @OperationLog("查询耗材目录")
    @GetMapping("/consumable/list")
    @Permission({Role.HOSPITAL, Role.MEDICAL, Role.ADMIN})
    public Result consumableList(PageDTO pageDTO,
                                 @RequestParam(required = false) String keyword) {
        return catalogService.consumableList(pageDTO, keyword);
    }
}
