package com.kh.fitguardians.member.controller;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;
import com.kh.fitguardians.member.model.service.MemberServiceImpl;
import com.kh.fitguardians.member.model.vo.BodyInfo;
import com.kh.fitguardians.member.model.vo.Member;
import com.kh.fitguardians.member.model.vo.MemberInfo;

@Controller
public class MemberController {
	
	@Autowired
	private MemberServiceImpl mService = new MemberServiceImpl();
	@Autowired
	private BCryptPasswordEncoder bcryptPasswordEncoder; 
	
    @RequestMapping("traineeDetail.me")
    public ModelAndView memberDetailView(@RequestParam("userId") String userId, ModelAndView mv) {

    	Member m = mService.getTraineeDetails(userId);
    	ArrayList<BodyInfo> bi = mService.getTraineeBodyInfo(userId);
    	MemberInfo mi = mService.getTraineeInfo(m.getUserNo());
    	// 최근 6개 데이터 조회문
    	ArrayList<BodyInfo> recentBi = mService.getRecentInfo(userId);
    	
    	// 가장 최근 1개 데이터 조회문
    	BodyInfo lastBodyInfo = null;
    	
    	for (BodyInfo bodyInfo : bi) {
    	    lastBodyInfo = bodyInfo;
    	}
    	double lastSmm = lastBodyInfo.getSmm();
    	double lastFat = lastBodyInfo.getFat();
    	double lastBmi = lastBodyInfo.getBmi();
    	
    	mv.addObject("m" , m);
    	mv.addObject("bi" , bi);
    	mv.addObject("mi", mi);
    	mv.addObject("lastSmm", String.format("%.1f", lastSmm));
    	mv.addObject("lastFat", String.format("%.1f", lastFat));
    	mv.addObject("lastBmi", String.format("%.1f", lastBmi));
    	mv.addObject("recentBi", recentBi);
    	
    	mv.setViewName("Trainer/traineeDetailInfo");
    	
        return mv;
    }

	@RequestMapping("loginform.me")
	public String loginForm() throws IOException {
		return "common/loginForm";
	}
	
	@RequestMapping("enrollForm.me")
	public String enrollForm() {
		return "common/enrollForm";
	}
	
	@ResponseBody
	@RequestMapping("checkId.me")
	public String memberCheckId(String userId) {
		int result = mService.checkId(userId);
		if(result > 0) {
			return "YYYN";
		}else {
			return "YYYI";
		}
	}
	
	@ResponseBody
	@RequestMapping("auth.email")
	public String ajaxAuthEmail(String email) {
		int randomCode = mService.authEmail(email);
		return randomCode + "";
	}
	
	
	@RequestMapping(value = "enroll.me", produces = "text/html; charset=UTF-8")
	public String memberEnroll(Member m, String memberInfo, HttpServletRequest request) {
		String encPwd = bcryptPasswordEncoder.encode(m.getUserPwd());
		m.setUserPwd(encPwd);
		// 현재 사용불가
		// String profile = "";
		// String gender = m.getGender();
		// if(m.getProfilePic() == null) {
		// 		switch (gender) {
		//		case "F":
		//			System.out.println("여자");
		//			profile = "resources/profilePic/gymW.png";
		//			break;
		//		default:
		//			System.out.println("남자");
		//			profile = "resources/profilePic/gymM.png";
		//			break;
		//		}
		//	}
		// m.setProfilePic(profile);
		
		// 기본 프로필 사진 설정
        String profile = m.getProfilePic() == null ? 
            (m.getGender().equals("F") ? "resources/profilePic/gymW.png" : "resources/profilePic/gymM.png") : 
            m.getProfilePic();
        m.setProfilePic(profile);
		
		// 회원 추가 정보가 있는지 확인
        if (memberInfo != null && !memberInfo.isEmpty()) {
            // 추가 정보가 있으면 추가 정보 저장
            MemberInfo info = new Gson().fromJson(memberInfo, MemberInfo.class);
            
            // 추가 정보중에서 기저질환이 없으면 값을 비게 만들기
            if(info.getDisease().equals("없음")) {
            	info.setDisease("");
            }
            
            int result = mService.insertMemberWithInfo(m, info);
            if (result > 0) {
                request.getSession().setAttribute("alertMsg", "회원가입이 완료되었습니다. 환영합니다!");
                return "Trainee/traineeDashboard";
            }
        } else {
            // 추가 정보가 없으면 기존 방식대로 회원가입 처리
            int result = mService.insertMember(m);
            if (result > 0) {
                request.getSession().setAttribute("alertMsg", "회원가입이 완료되었습니다. 환영합니다!");
                return "Trainee/traineeDashboard";
            }
        }
        request.getSession().setAttribute("errorMsg", "회원가입에 실패했습니다.");
        return "common/loginForm";
		 
        // 사용 노노
		// if(result > 0) { 
		// 		 request.getSession().setAttribute("alertMsg","회원가입되었습니다. 환영합니다!"); 
		// 		 return "Trainee/traineeDashboard"; 
		// }else { 
		//	 	
		//	 	 return "common/loginForm"; 
		// }
	}
	
	@RequestMapping("login.me")
	public String memberLogin(Member m, HttpServletRequest request) {
		Member loginUser = mService.loginMember(m);
		HttpSession session = request.getSession();
		if(loginUser != null) {
			if(bcryptPasswordEncoder.matches(m.getUserPwd(), loginUser.getUserPwd())) {
				session.setAttribute("loginUser", loginUser);
				return "main";
			}else {
				session.setAttribute("errorMsg", "비밀번호가 일치하지 않습니다. 다시 입력해주세요!");
				return "redirect:loginform.me";
			}
			
		}else {
			session.setAttribute("errorMsg", "아이디가 틀렸습니다 다시 입력해주세요!");
			return "redirect:loginform.me";
		}
		
	}
	
	@RequestMapping("logout.me")
	public String memberlogOut(HttpServletRequest request) {
		request.getSession().invalidate();
		return "redirect:/";
	}
	
	@RequestMapping("traineeList.me")
	public ModelAndView traineeList(HttpSession session, ModelAndView mv) {
		// 페이지가 로드되자마자 트레이너의 담당 회원이 조회되야 한다.
		String userId = ((Member)session.getAttribute("loginUser")).getUserId();
		ArrayList<Member> list = mService.getTraineeList(userId);
		//System.out.println("userId :" + userId);
		
		//System.out.println(list);
		mv.addObject("list", list)
		  .setViewName("Trainer/traineeManagement");
		
		return mv;
	}
	
	@ResponseBody
	@RequestMapping("saveBodyInfo.me")
	public String saveBodyInfo(BodyInfo bi){
		
		int result = mService.saveBodyInfo(bi);
		///System.out.println(result);
		return result>0?"success":"error";
		
	}
	
	@ResponseBody
	@RequestMapping("deleteBodyInfo.me")
	public String deleteBodyInfo(int bodyInfoNo) {
		int result = mService.deleteBodyInfo(bodyInfoNo);
		return result >0?"success":"error";
	}
	
	
	
	
}
