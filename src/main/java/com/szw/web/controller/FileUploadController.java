package com.szw.web.controller;

import com.szw.web.constant.CommonConstant;
import com.szw.web.util.DateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传的Controller
 * Created by lf on 2017/7/17.
 */
@Controller
public class FileUploadController extends BaseController {

	@Value("${file.upload.path}")
	private String uploadPath;

	/**
	 * 文件上传具体实现方法（单文件上传）
	 * @param file
	 * @return
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
		if (file.isEmpty()) {
			return error("文件为空");
		}
		String a=request.getParameter("a");
		// 获取文件名
		String fileName = file.getOriginalFilename();
		// 获取文件的后缀名
		String suffixName = fileName.substring(fileName.lastIndexOf("."));
		// 解决中文问题，liunx下中文路径，图片显示问题
		fileName = UUID.randomUUID() + suffixName;
		String relativeUrl = DateUtil.format(new Date(),"yyyy-MM-dd") + CommonConstant.FILE_SEPARATOR + fileName;
		File dest = new File(uploadPath+relativeUrl);
		// 检测是否存在目录
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		try {
			file.transferTo(dest);
			Map<String,String> map = new HashMap<String,String>();
			map.put("filePath",relativeUrl);
			return success(map);
		} catch (Exception e) {
			e.printStackTrace();
			return error("上传失败");
		}
	}

	/**
	 * 文件上传具体实现方法（单文件上传）
	 * @param file
	 * @return
	 */
	@RequestMapping(value = "/uploadNew", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> uploadNew(@RequestParam("file1") MultipartFile file, HttpServletRequest request) {
		if (file.isEmpty()) {
			return error("文件为空");
		}
		// 获取文件名
		String fileName = file.getOriginalFilename();
		// 获取文件的后缀名
		String suffixName = fileName.substring(fileName.lastIndexOf("."));
		// 解决中文问题，liunx下中文路径，图片显示问题
		fileName = UUID.randomUUID() + suffixName;
		String relativeUrl = DateUtil.format(new Date(),"yyyy-MM-dd") + CommonConstant.FILE_SEPARATOR + fileName;
		File dest = new File(uploadPath+relativeUrl);
		// 检测是否存在目录
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		try {
			file.transferTo(dest);
			Map<String,String> map = new HashMap<String,String>();
			map.put("filePath", relativeUrl);
			return success(map);
		} catch (Exception e) {
			e.printStackTrace();
			return error("上传失败");
		}
	}

}