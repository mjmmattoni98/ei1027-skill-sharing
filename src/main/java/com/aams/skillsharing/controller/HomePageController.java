package com.aams.skillsharing.controller;

import com.aams.skillsharing.dao.*;
import com.aams.skillsharing.model.InternalUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/homePage")
public class HomePageController extends RoleController{

    private CollaborationDao collaborationDao;
    private RequestDao requestDao;

    private OfferDao offerDao;

    @Autowired
    public void setCollaborationDao(CollaborationDao collaborationDao) {
        this.collaborationDao = collaborationDao;
    }

    @Autowired
    public void setRequestDao(RequestDao requestDao) {
        this.requestDao = requestDao;
    }

    @Autowired
    public void setOfferDao(OfferDao offerDao) {
        this.offerDao = offerDao;
    }

    @RequestMapping("list")
    public String listHomePageStudent(HttpSession session, Model model){
        if(session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");
        String username = user.getUsername();
        model.addAttribute("collaborations", collaborationDao.getCollaborationsStudent(username));
        model.addAttribute("offers", offerDao.getOffersStudent(username));
        model.addAttribute("requests", requestDao.getRequestsStudent(username));
        model.addAttribute("student", username);
        return "homePage/list";
    }

}