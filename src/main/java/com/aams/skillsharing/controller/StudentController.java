package com.aams.skillsharing.controller;

import com.aams.skillsharing.dao.*;
import com.aams.skillsharing.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/student")
public class StudentController extends RoleController {
    private StudentDao studentDao;
    private CollaborationDao collaborationDao;
    private OfferDao offerDao;
    private RequestDao requestDao;
    private EmailDao emailDao;
    private static final StudentValidator validator = new StudentValidator();
    private static final StudentUpdateValidator updateValidator = new StudentUpdateValidator();

    @Autowired
    public void setStudentDao(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    @Autowired
    public void setCollaborationDao(CollaborationDao collaborationDao) {
        this.collaborationDao = collaborationDao;
    }

    @Autowired
    public void setOfferDao(OfferDao offerDao) {
        this.offerDao = offerDao;
    }

    @Autowired
    public void setRequestDao(RequestDao requestDao) {
        this.requestDao = requestDao;
    }

    @Autowired
    public void setEmailDao(EmailDao emailDao) {
        this.emailDao = emailDao;
    }

    @RequestMapping("/paged_list")
    public String listStudentsPaged(Model model, @RequestParam("page") Optional<Integer> page) {
        List<Student> students = studentDao.getStudents();
        Collections.sort(students);

        List<List<Student>> studentsPaged = new ArrayList<>();
        int start = 0;
        int pageLength = 10;
        int end = pageLength - 1;
        while (end < students.size()) {
            studentsPaged.add(new ArrayList<>(students.subList(start, end)));
            start += pageLength;
            end += pageLength;
        }

        studentsPaged.add(new ArrayList<>(students.subList(start, students.size())));
        model.addAttribute("studentsPaged", studentsPaged);

        int totalPages = studentsPaged.size();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        int currentPage = page.orElse(0);
        model.addAttribute("selectedPage", currentPage);
        return "student/paged_list";
    }

    @RequestMapping("/list")
    public String listStudents(HttpSession session, Model model) {
        InternalUser user = checkSession(session, SKP_ROLE);
        if (user == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        model.addAttribute("students", studentDao.getStudents());
        return "student/list";
    }

    @RequestMapping("/profile")
    public String studentProfile(HttpSession session, Model model){
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        model.addAttribute("student", studentDao.getStudent(user.getUsername()));
        return "student/profile";
    }

    @RequestMapping("/statistics/{username}")
    public String studentStatistics(HttpSession session, Model model, @PathVariable String username){
        InternalUser user = checkSession(session, SKP_ROLE);
        if (user == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        Student student = studentDao.getStudent(username);
        model.addAttribute("student", username);
        model.addAttribute("balance_hours", student.getBalanceHours());

        List<Offer> offers = offerDao.getOffersStudent(username);
        model.addAttribute("offers", offers);

        List<Request> requests = requestDao.getRequestsStudent(username);
        model.addAttribute("requests", requests);

        List<Collaboration> collaborations = collaborationDao.getCollaborationsStudent(username);
        model.addAttribute("collaborations", collaborations);
        double averageAssessment = collaborations.stream().mapToInt(Collaboration::getAssessment).average().orElse(0);
        model.addAttribute("average_assessment", averageAssessment);

        return "student/statistics";
    }

    @RequestMapping(value = "/add")
    public String addStudent(Model model) {
        Student student = new Student();
        model.addAttribute("student", student);
        return "student/add";
    }

    @PostMapping(value = "/add")
    public String processAddStudent(@ModelAttribute("student") Student student, BindingResult bindingResult) {

        validator.validate(student, bindingResult);
        if (bindingResult.hasErrors()) return "student/add";

        try{
            studentDao.addStudent(student);
        }
        catch (DuplicateKeyException e){
            throw new SkillSharingException("It already exist the username\n" + e.getMessage(),
                    "PKDuplicate", "student/add");
        }
        catch (DataAccessException e){
            throw new SkillSharingException("Error accessing the database\n" + e.getMessage(),
                    "ErrorAccessingDatabase", "/");
        }
        return "redirect:/student/profile";
    }

    @GetMapping(value = "/update/{username}")
    public String updateStudent(Model model, @PathVariable String username) {
        model.addAttribute("student", studentDao.getStudent(username));
        return "student/update";
    }

    @PostMapping(value = "/update")
    public String processUpdateSubmit(@ModelAttribute("student") Student student,
                                      BindingResult bindingResult) {
        updateValidator.validate(student, bindingResult);
        if (bindingResult.hasErrors()) return "student/update";
        studentDao.updateStudent(student);
        return "redirect:/student/profile";
    }

    @RequestMapping(value = "/block/{username}")
    public String processBlockStudent(HttpSession session, Model model, @PathVariable String username) {
        InternalUser user = checkSession(session, SKP_ROLE);
        if (user == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        Student student = studentDao.getStudent(username);
        student.setBlocked(true);
        studentDao.updateStudent(student);

        Email email = new Email();
        email.setReceiver(student.getEmail());
        email.setSubject("Block account");
        email.setBody("Your account has been blocked by the administrator");
        email.setSendDate(LocalDate.now());
        email.setSender("skill.sharing@gmail.com");
        emailDao.addEmail(email);

        return "redirect:../list/";
    }
}
