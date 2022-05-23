package com.aams.skillsharing.controller;

import com.aams.skillsharing.dao.EmailDao;
import com.aams.skillsharing.dao.StudentDao;
import com.aams.skillsharing.model.Email;
import com.aams.skillsharing.model.InternalUser;
import com.aams.skillsharing.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/email")
public class EmailController extends RoleController{
    private EmailDao emailDAO;
    private StudentDao studentDao;

    @Autowired
    public void setStudentDao(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    @Autowired
    public void setEmailDAO(EmailDao e) {
        this.emailDAO = e;
    }

    @RequestMapping("/list/{receiver}")
    public String listEmails(HttpSession session, Model model, @PathVariable String receiver) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Student student = studentDao.getStudent(user.getUsername());
        if (!student.getEmail().equals(receiver)) {
            throw new SkillSharingException("You can only see your own emails",
                    "AccesDenied", "../" + user.getUrlMainPage());
        }

        model.addAttribute("emails", emailDAO.getEmails(receiver));
        return "email/list";
    }

    @RequestMapping(value = "/delete/{id}")
    public String processDeleteSkill(HttpSession session, Model model, @PathVariable int id) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Student student = studentDao.getStudent(user.getUsername());
        Email email = emailDAO.getEmail(id);
        if (!student.getEmail().equals(email.getReceiver()))
            throw new SkillSharingException("You cannot delete emails of other students",
                    "AccesDenied", "../" + user.getUrlMainPage());

        emailDAO.deleteEmail(id);
        return "redirect:../list/";
    }
}
