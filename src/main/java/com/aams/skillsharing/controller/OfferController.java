package com.aams.skillsharing.controller;

import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.aams.skillsharing.dao.CollaborationDao;
import com.aams.skillsharing.dao.OfferDao;
import com.aams.skillsharing.dao.RequestDao;
import com.aams.skillsharing.dao.SkillDao;
import com.aams.skillsharing.model.InternalUser;
import com.aams.skillsharing.model.Offer;
import com.aams.skillsharing.model.Request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/offer")
public class OfferController extends RoleController{
    private OfferDao offerDao;
    private SkillDao skillDao;
    private RequestDao requestDao;
    private CollaborationDao collaborationDao;
    private static final OfferValidator validator = new OfferValidator();

    @Autowired
    public void setOfferDao(OfferDao offerDao) {
        this.offerDao = offerDao;
    }

    @Autowired
    public void setRequestDao(RequestDao requestDao) {
        this.requestDao = requestDao;
    }

    @Autowired
    public void setCollaborationDao(CollaborationDao collaborationDao) {
        this.collaborationDao = collaborationDao;
    }

    @Autowired
    public void setSkillDao(SkillDao skillDao) {
        this.skillDao = skillDao;
    }

    @RequestMapping("/list")
    public String listOffers(Model model) {
        List<Offer> offers = offerDao.getOffers();

        model.addAttribute("offers", offers);
        return "offer/list";
    }

    @RequestMapping("/list/collaborate/{id}")
    public String listOffersToCollaborate(HttpSession session, Model model, @PathVariable int id) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        Request request = requestDao.getRequest(id);
        List<Offer> offers = offerDao.getOffersSkill(request.getName());
        // Remove my offers and the offers that are already collaborating with the request
        offers.removeIf(offer -> offer.getUsername().equals(request.getUsername()) ||
                        collaborationDao.getCollaboration(offer.getId(), request.getId()) != null);


        model.addAttribute("request", request.getId());
        model.addAttribute("offers", offers);
        return "offer/list";
    }

    @RequestMapping("/list/student/{username}")
    public String listOffersStudent(HttpSession session, Model model, @PathVariable String username) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        if (!user.getUsername().equals(username))
            throw new SkillSharingException("You cannot list offers of other students",
                    "AccesDenied", "../" + user.getUrlMainPage());

        List<Offer> offers = offerDao.getOffersStudent(username);

        model.addAttribute("offers", offers);
        model.addAttribute("student", username);
        return "offer/list";
    }

    @RequestMapping("/list/skill/{name}")
    public String listOffersSkill(Model model, @PathVariable String name) {
        List<Offer> offers = offerDao.getOffersSkill(name);

        model.addAttribute("offers", offers);
        model.addAttribute("skill", name);
        return "offer/list";
    }

    @RequestMapping(value = "/add/{name}")
    public String addOfferSkill(HttpSession session, Model model, @PathVariable String name) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");
        
        Offer offer = new Offer();
        offer.setFromSkill(true);
        offer.setUsername(user.getUsername());
        offer.setName(name);
        model.addAttribute("offer", offer);
        return "offer/add";
    }

    @RequestMapping(value = "/add")
    public String addOffer(HttpSession session, Model model) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Offer offer = new Offer();
        offer.setFromSkill(false);
        offer.setUsername(user.getUsername());
        model.addAttribute("offer", offer);
        model.addAttribute("skills", skillDao.getAvailableSkills());
        return "offer/add";
    }

    @PostMapping(value = "/add")
    public String processAddOffer(@ModelAttribute("offer") Offer offer, Model model,
                                  BindingResult bindingResult) {
        validator.validate(offer, bindingResult);
        if (bindingResult.hasErrors()) {
            if (!offer.isFromSkill())
                model.addAttribute("skills", skillDao.getAvailableSkills());
            return "offer/add";
        }
        try {
            offerDao.addOffer(offer);
        }
        catch (DataAccessException e){
            throw new SkillSharingException("Error accessing the database\n" + e.getMessage(),
                    "ErrorAccessingDatabase", "/");
        }
        return "redirect:list/student/" + offer.getUsername();
    }

    @GetMapping(value = "/update/{id}")
    public String updateOffer(HttpSession session, Model model, @PathVariable int id) {
        if (session.getAttribute("user") == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }
        InternalUser user = (InternalUser) session.getAttribute("user");

        Offer offer = offerDao.getOffer(id);
        if (!offer.getUsername().equals(user.getUsername()))
            throw new SkillSharingException("You are not allowed to update this offer", "NotAllowed", "/");
        model.addAttribute("offer", offer);
        return "offer/update";
    }

    @PostMapping(value = "/update")
    public String processUpdateSubmit(@ModelAttribute("offer") Offer offer,
                                      BindingResult bindingResult) {
        validator.validate(offer, bindingResult);
        if (bindingResult.hasErrors()) return "offer/update";
        offerDao.updateOffer(offer);
        return "redirect:list/";
    }

}
