/**  
 * @Title: RecruitController.java
 * @Package com.xzit.ar.portal.controller.recruit
 * @Description: TODO
 * @author Mr.Black
 * @date 2016年1月17日 下午1:54:22
 * @version V1.0  
 */
package com.xzit.ar.portal.controller.recruit;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.xzit.ar.common.base.BaseController;
import com.xzit.ar.common.exception.ServiceException;
import com.xzit.ar.common.init.context.ARContext;
import com.xzit.ar.common.page.Page;
import com.xzit.ar.common.po.recruit.Recruit;
import com.xzit.ar.common.po.recruit.RecruitUnit;
import com.xzit.ar.common.util.CommonUtil;
import com.xzit.ar.portal.service.my.ResumeService;
import com.xzit.ar.portal.service.recruit.RecruitService;
import com.xzit.ar.portal.service.recruit.UnitService;

/**
 * @ClassName: RecruitController
 * @Description: TODO RecruitController
 * @author Mr.Black
 * @date 2016年1月17日 下午1:54:22
 */
@Controller
@RequestMapping("/recruit")
public class RecruitController extends BaseController {

	@Resource
	private RecruitService recruitService;

	@Resource
	private UnitService unitService;

	@Resource
	private ResumeService resumeService;

	@RequestMapping(value = "", method = { RequestMethod.GET })
	public String indexRecruit(Model model) throws ServiceException {
		// 构造 page 对象
		Page<Map<String, Object>> page = new Page<>(getPageIndex(), getPageSize());
		// 条件查询
		recruitService.queryRecruit(page, getQueryStr());
		// 传向页面
		model.addAttribute("page", page);
		setQueryStr();

		return "recruit/recruit-index";
	}

	/**
	 * @throws ServiceException
	 * @Title: detailRecruit
	 * @Description: TODO detailRecruit
	 */
	@RequestMapping(value = "/detailRecruit", method = { RequestMethod.GET })
	public String detailRecruit(Model model, @RequestParam("recruitId") Integer recruitId) throws ServiceException {
		// 根据 id 查询招聘信息
		Map<String, Object> recruit = recruitService.getDetailRecruit(recruitId);
		List<Map<String, Object>> resumeList = resumeService.loadResumesToPost(getCurrentUserId());
		List<Map<String, Object>> postRecords = resumeService.postResumeRecord(recruitId);
		List<Map<String, Object>> otherRecruits = recruitService.loadOtherRecruits((Integer) recruit.get("unitId"),
				recruitId);

		// 解析 福利待遇
		String benefit = (String) recruit.get("benefit");

		// 传递参数
		if (CommonUtil.isNotEmpty(benefit)) {
			String[] benefits = benefit.split(",");
			recruit.remove("benefit");
			model.addAttribute("benefits", benefits);
		}
		model.addAttribute("resumeList", resumeList);
		model.addAttribute("postRecords", postRecords);
		recruit.put("userId", getCurrentUserId());
		model.addAttribute("recruit", recruit);
		model.addAttribute("otherRecruits", otherRecruits);

		return "recruit/recruit-detail";
	}

	@RequestMapping("/addRecruit")
	public String addRecruit(Model model) throws ServiceException {
		// 判断当前用户创建公司情况
		List<RecruitUnit> unitList = unitService.getUnitsByUserId(getCurrentUserId());
		if (CommonUtil.isEmpty(unitList)) {
			// 创建新的
			return "forward:/unit/addUnit.action";
		}
		// 加载招聘常量
		model.addAttribute("positionSalary", ARContext.positionSalary);
		model.addAttribute("positionBenefit", ARContext.positionBenefit);
		model.addAttribute("positionProf", ARContext.positionProf);
		super.setFormValid();
		setOperateRemarks("公司信息为个人公司信息，若要修改，请到 我的帐户 >> 我的招聘 中修改公司信息");

		return "recruit/recruit-add";
	}

	@RequestMapping("/addRecruitSubmit")
	public String addRecruitSubmit(Model model, RedirectAttributes attr, Recruit recruit) throws ServiceException {
		// 判断当前用户创建公司情况
		List<RecruitUnit> unitList = unitService.getUnitsByUserId(getCurrentUserId());
		if (!validForm()) {
			setMessage(model, "请不要重复发布！");
			return "recruit/recruit-success";
		} else if (CommonUtil.isEmpty(unitList)) {
			// 创建新的
			return "redirect:/unit/addUnit.action";
		} else {
			// 参数设定
			recruit.setCreateTime(new Date());
			recruit.setResumes(0);
			recruit.setIsTop("0");
			recruit.setUserId(getCurrentUserId());
			recruit.setUnitId(unitList.get(0).getUnitId());
			recruit.setState("D");
			recruit.setStateTime(new Date());
			// 存储招聘信息
			recruitService.createRecruit(recruit);
		}

		return "redirect:recruitSuccess.action";
	}

	/**
	 * @Title: recruitSuccess
	 * @Description: TODO 招聘发布成功跳转
	 */
	@RequestMapping("/recruitSuccess")
	public String recruitSuccess(Model model) {
		setMessage(model, "提交成功！");
		setOperateRemarks(model, "若要修改招聘信息，请到 个人中心 >> 我的招聘");
		return "recruit/recruit-success";
	}

}