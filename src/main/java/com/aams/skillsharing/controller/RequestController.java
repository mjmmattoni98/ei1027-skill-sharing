package com.aams.skillsharing.controller;

import com.aams.skillsharing.dao.RequestDao;
import com.aams.skillsharing.dao.SkillDao;
import com.aams.skillsharing.model.InternalUser;
import com.aams.skillsharing.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/request")
public class RequestController extends RoleController{
    private RequestDao requestDao;
    private SkillDao skillDao;
    private static final RequestValidator validator = new RequestValidator();

    @Autowired
    public void setRequestDao(RequestDao requestDao) {
        this.requestDao = requestDao;
    }

    @Autowired
    public void setSkillDao(SkillDao skillDao) {
        this.skillDao = skillDao;
    }

    @RequestMapping("/list")
    public String listRequests(Model model) {
        List<Request> requests = requestDao.getRequests();
        requests.removeIf(request -> request.getFinishDate() != null &&
                request.getFinishDate().compareTo(LocalDate.now()) < 0);

        model.addAttribute("requests", requests);
        return "request/list";
    }

    @RequestMapping("/list/student/{username}")
    public String listRequestsStudent(HttpSession session, Model model, @PathVariable String username) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        List<Request> requests = requestDao.getRequestsStudent(username);
        requests.removeIf(request -> request.getFinishDate() != null &&
                request.getFinishDate().compareTo(LocalDate.now()) < 0);

        model.addAttribute("requests", requests);
        model.addAttribute("student", username);
        return "request/list";
    }

    @RequestMapping("/list/skill/{name}")
    public String listRequestsSkill(Model model, @PathVariable String name) {
        List<Request> requests = requestDao.getRequestsSkill(name);
        requests.removeIf(request -> request.getFinishDate() != null &&
                request.getFinishDate().compareTo(LocalDate.now()) < 0);

        model.addAttribute("requests", requests);
        model.addAttribute("skill", name);
        return "request/list";
    }

    @RequestMapping(value = "/add/{name}")
    public String addRequest(HttpSession session, Model model, @PathVariable String name) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Request request = new Request();
        request.setUsername(user.getUsername());
        request.setName(name);
        model.addAttribute("request", request);
        return "request/add";
    }

    @RequestMapping(value = "/add")
    public String addRequest(HttpSession session, Model model) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Request request = new Request();
        request.setUsername(user.getUsername());
        model.addAttribute("request", request);
        model.addAttribute("skills", skillDao.getAvailableSkills());
        return "request/add";
    }

    @PostMapping(value = "/add")
    public String processAddRequest(@ModelAttribute("request") Request request,
                                    BindingResult bindingResult) {
        validator.validate(request, bindingResult);
        if (bindingResult.hasErrors()) {
            return "request/add";
        }
        try {
            requestDao.addRequest(request);
        }
        catch (DataAccessException e){
            throw new SkillSharingException("Error accessing the database\n" + e.getMessage(),
                    "ErrorAccessingDatabase", "/");
        }
        return "redirect:list/";
    }

    @GetMapping(value = "/update/{id}")
    public String updateRequest(HttpSession session, Model model, @PathVariable int id) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Request request = requestDao.getRequest(id);
        if (!request.getUsername().equals(user.getUsername()))
            throw new SkillSharingException("You are not allowed to update this request", "NotAllowed", "/");
        model.addAttribute("request", request);
        return "request/update";
    }

    @PostMapping(value = "/update")
    public String processUpdateSubmit(@ModelAttribute("request") Request request,
                                      BindingResult bindingResult) {
        validator.validate(request, bindingResult);
        if (bindingResult.hasErrors()) return "request/update";
        requestDao.updateRequest(request);
        return "redirect:list/";
    }

    @RequestMapping(value = "/cancel/{id}")
    public String processCancelRequest(HttpSession session, Model model, @PathVariable int id) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Request request = requestDao.getRequest(id);
        if (!request.getUsername().equals(user.getUsername())) {
            throw new SkillSharingException("You cannot cancel requests of other students",
                    "AccesDenied", "../" + user.getUrlMainPage());
        }

        request.setFinishDate(LocalDate.now());
        requestDao.updateRequest(request);
        return "redirect:../list/";
    }
}
