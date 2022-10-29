package com.meaningfarm.mall.product;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;
import net.sf.json.JSONArray;

@Controller
@RequestMapping("/product/*")
public class ProductController {

	Logger logger = LoggerFactory.getLogger(ProductController.class);
	
	@Autowired
	private ProductService productService;
	
//	@GetMapping("/list")
//	public void productList() {
//		logger.info("게시판 목록");
//	}
	// product 상품 목록 페이지 띄우기
	@GetMapping("/productlisttest")
	public String productList(Model model) {
		logger.info("ProductController productList");
		model.addAttribute("productSelectAll", productService.productList());
		logger.info(productService.productList().toString());
		return "product/productlisttest";
	}
	// product 상품 등록 버튼 누를 때
	@PostMapping("/productinsert")
	public String productInsert(ProductVO productVO) {
		logger.info("글 작성 버튼 누름 ProductVO " + productVO);
		productService.productInsert(productVO);
		return "redirect:/product/productlisttest";
	}
	// product 상품 상세 창으로 가는 거
	@GetMapping("/productdetail")
	public String productDetail(ProductVO productVO, Model model) {
		logger.info("ProductController productDetail");
		model.addAttribute("productSelectOne", productService.productDetail(productVO.getProduct_no()));
		
		List<CategoryTypeVO> CTList = null;
		CTList = productService.CTList();
		model.addAttribute("selectCategoryType", JSONArray.fromObject(CTList));
		
		List<CategoryLocalVO> CLList = null;
		CLList = productService.CLList();
		model.addAttribute("selectCategoryLocal", JSONArray.fromObject(CLList));
		return "product/productdetail";
	}
	// product 상품 수정
	@PostMapping("/productdetail")
	public String productUpdate(ProductVO productVO) {
		logger.info("ProductController productUpdate " + productVO);
		productService.productUpdate(productVO);
		return "redirect:/product/productlisttest";
	}
	// product 상품 삭제
	@PostMapping("/productdelete")
	public String productDelete(ProductVO productVO) {
		logger.info("ProductController productDelete " + productVO);
		productService.productDelete(productVO.getProduct_no());
		return "redirect:/product/productlisttest";
	}
	
	
	
	
	@GetMapping(value={"/productinsert"})
	public void categoryList(Model model) throws Exception {
		logger.info("ProductController CTList");
		List<CategoryTypeVO> CTList = null;
		CTList = productService.CTList();
		model.addAttribute("selectCategoryType", JSONArray.fromObject(CTList));
		
		List<CategoryLocalVO> CLList = null;
		CLList = productService.CLList();
		model.addAttribute("selectCategoryLocal", JSONArray.fromObject(CLList));
	}
	
//	@GetMapping(value="/productfileList", produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
//	public ResponseEntity<List<ProductFileVO>> productfileList(int product_no) {
//		logger.info("ProductController productfileList");
//		return new ResponseEntity<List<ProductFileVO>>(productService.productfileList(product_no), HttpStatus.OK);
//	}
	
	@PostMapping(value="/productfileinsert", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<ProductFileVO>> productfileInsert(@RequestParam(value="uploadImg") MultipartFile[] uploadImg) {
		logger.info("ProductController productfileInsert");
		System.out.println(uploadImg.length);
		for(int i=0;i<uploadImg.length;i++) {
			logger.info("파일 이름 : " + uploadImg[i].getOriginalFilename());
			logger.info("파일 타입 : " + uploadImg[i].getContentType());
			logger.info("파일 크기 : " + uploadImg[i].getSize());
		}
		
		for(MultipartFile multipartImg : uploadImg) {
			File checkImg = new File(multipartImg.getOriginalFilename());
			String imgType = null;
			try {
				imgType = Files.probeContentType(checkImg.toPath());
				logger.info("MIME TYPE " + imgType);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(!imgType.startsWith("image")) {
				List<ProductFileVO> list = null;
				return new ResponseEntity<>(list, HttpStatus.BAD_REQUEST);
			} 
			
		} // end of for
		
		String uploadFolder = "C:\\meaningFarm\\meaningFarm\\src\\main\\webapp\\resources\\image\\";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // 오늘 날짜 형식에 맞게 변환
		Date date = new Date();
		String dateStr = sdf.format(date);
		String datePath = dateStr.replace("-", File.separator);
		
		File uploadPath = new File(uploadFolder, datePath); // (부모 경로, 자식 경로)
		if(uploadPath.exists() == false) { // 대상이 존재하지 않으면
			uploadPath.mkdirs(); // 새 폴더들(mkdir's') 생성
		}
		
		List<ProductFileVO> PFList = new ArrayList();
		
		for(MultipartFile multipartImg : uploadImg) {
			System.out.println("PFList " + PFList);
			System.out.println(PFList.size());
			ProductFileVO productfileVO = new ProductFileVO();
			
			String uploadImgName = multipartImg.getOriginalFilename(); // 파일 이름. getOriginalFilename: 파일의 원래 이름 가져옴
			productfileVO.setProductfile_path(uploadPath.toString()); // 파일 위치 vo에 
			String uuid = UUID.randomUUID().toString();
			uploadImgName = uuid + "_" + uploadImgName; // uuid 적용 파일 후 이름. uuid 쓰는 이유: 동일한 이름의 파일 덮어쓰기 막기 위해
			productfileVO.setProductfile_name(uploadImgName); // 파일이 이름 vo에 넣어 줌.
			productfileVO.setProductfile_sname("s_"+uploadImgName);
			File saveImg = new File(uploadPath, uploadImgName); // 파일 위치, 파일 이름을 합친 File 객체
			// 파일 저장
			try {
				multipartImg.transferTo(saveImg); // 파일 저장 코드
				File thumbnailImg = new File(uploadPath, "s_" + uploadImgName); // 경로, 이름. 썸네일은 이름 앞에 s_ 추가
				/*
				 * 방법 1
				 * BufferedImage bo_image = ImageIO.read(saveImg);
				 * double ratio = 3; // 비율
				 * int width = (int)(bo_image.getWidth() / ratio); // 너비
				 * int height = (int)(bo_image.getHeight() / ratio); // 높이
				 * BufferedImage bt_image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
				 * Graphics2D graphic = bt_image.createGraphics();
				 * graphic.drawImage(bo_image, 0, 0, 300, 300, null);
				 * ImageIO.write(bt_image, "jpg", thumbnailImg);
				 */
				// 방법 2
				Thumbnails.of(saveImg).size(160, 160).toFile(thumbnailImg); // 썸네일 생성 코드.
			} catch (Exception e) {
				e.printStackTrace();
			}
			int i = 0;
			System.out.println(++i);
//			productService.productfileInsert(productfileVO);
			PFList.add(productfileVO);
			System.out.println("PFList " + PFList);
			System.out.println(PFList.size());
		} // for
		
		ResponseEntity<List<ProductFileVO>> result = new ResponseEntity<List<ProductFileVO>>(PFList, HttpStatus.OK);

		return result;
	}
	
//	@PostMapping("/productfileinsert")
//	public void productfileInsert(@RequestParam(value="uploadFile") MultipartFile[] uploadFile) {
//		logger.info("ProductController productfileInsert");
//		for(int i=0;i<uploadFile.length;i++) {
//			logger.info("ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ");
//			logger.info("파일 이름 : " + uploadFile[i].getOriginalFilename());
//			logger.info("파일 타입 : " + uploadFile[i].getContentType());
//			logger.info("파일 크기 : " + uploadFile[i].getSize());
//		}
//	}
	
	
	@GetMapping("/productfiledetail") // 파일 하나만... 아마도
	public ResponseEntity<byte[]> productfileDetail(@RequestParam(name="imgName") String imgName) {
		logger.info("ProductController imagedetail " + imgName);
		File file = new File("C:\\meaningFarm\\meaningFarm\\src\\main\\webapp\\resources\\image\\" + imgName);
		ResponseEntity<byte[]> result = null;
		try {
			HttpHeaders header = new HttpHeaders();
			header.add("Content-type", Files.probeContentType(file.toPath()));
			result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), header, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	@PostMapping("/productfiledelete")
	public ResponseEntity<String> productfileDelete(@RequestParam(name="imgName") String imgName, ProductFileVO productfileVO) {
		
		logger.info("ProductController imageDelete " + imgName);
		File file = null;
		try {
			// 썸네일 파일 삭제
			file = new File("C:\\meaningFarm\\meaningFarm\\src\\main\\webapp\\resources\\image\\" + URLDecoder.decode(imgName, "UTF-8"));
			file.delete();
			// 원본 파일 삭제
			String originImgName = file.getAbsolutePath().replaceFirst("s_", "");
			logger.info("originImgName " + originImgName);
			file = new File(originImgName);
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>("fail", HttpStatus.NOT_IMPLEMENTED);
		}
		productService.productfileDelete(productfileVO.getProductfile_no());
		return new ResponseEntity<String>("success", HttpStatus.OK);
	}
	
//	@PostMapping("productfileinsert")
//	public String productfileInsert(ProductFileVO productfileVO, RedirectAttributes rttr) throws Exception {
//		logger.info("ProductController productfileInsert " + productfileVO);
//		productService.productfileInsert(productfileVO);
//		return "";
//	}
}
