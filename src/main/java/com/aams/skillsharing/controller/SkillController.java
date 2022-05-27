package com.aams.skillsharing.controller;

import com.aams.skillsharing.dao.*;
import com.aams.skillsharing.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@Controller
@RequestMapping("/skill")
public class SkillController extends RoleController {
    private SkillDao skillDao;
    private RequestDao requestDao;
    private OfferDao offerDao;
    private EmailDao emailDao;
    private StudentDao studentDao;
    private static final SkillValidator validator = new SkillValidator();

    @Autowired
    public void setSkillDao(SkillDao skillDao) {
        this.skillDao = skillDao;
    }

    @Autowired
    public void setRequestDao(RequestDao requestDao) {
        this.requestDao = requestDao;
    }

    @Autowired
    public void setOfferDao(OfferDao offerDao) {
        this.offerDao = offerDao;
    }

    @Autowired
    public void setEmailDao(EmailDao emailDao) {
        this.emailDao = emailDao;
    }

    @Autowired
    public void setStudentDao(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    @RequestMapping("/list")
    public String listSkills(Model model) {
        model.addAttribute("skills", skillDao.getAvailableSkills());
        model.addAttribute("skills_disabled", skillDao.getDisabledSkills());
        return "skill/list";
    }

    @RequestMapping(value = "/add")
    public String addSkill(HttpSession session, Model model) {
        InternalUser user = checkSession(session, SKP_ROLE);
        if (user == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        Skill skill = new Skill();
        model.addAttribute("skill", skill);
        model.addAttribute("skillLevels", loadSkills());
        return "skill/add";
    }

    @PostMapping(value = "/add")
    public String processAddSkill(@ModelAttribute("skill") Skill skill, Model model,
                                  BindingResult bindingResult) {
        validator.validate(skill, bindingResult);

        if (bindingResult.hasErrors()) {
  /*          List<String> skillLevels = new LinkedList<>();
            for(SkillLevel skillLevel : SkillLevel.values())
                skillLevels.add(skillLevel.getId());*/
            model.addAttribute("skillLevels", loadSkills());
            return "skill/add";
        }
        try {
            skillDao.addSkill(skill);
        }
        catch (DataAccessException e){
            throw new SkillSharingException("Error accessing the database\n" + e.getMessage(),
                    "ErrorAccessingDatabase", "/");
        }
        return "redirect:list/";
    }

    @GetMapping(value = "/update/{name}")
    public String updateSkill(HttpSession session, Model model, @PathVariable String name) {
        InternalUser user = checkSession(session, SKP_ROLE);
        if (user == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        Skill skill = skillDao.getSkill(name);
        model.addAttribute("skill", skill);
        model.addAttribute("skillLevels", loadSkills());
        return "skill/update";
    }

    @PostMapping(value = "/update")
    public String processUpdateSubmit(@ModelAttribute("skill") Skill skill, Model model,
                                      BindingResult bindingResult) {
        validator.validate(skill, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("skills", loadSkills());
            return "skill/update";
        }

        if (skill.isCanceled()){
            List<Offer> offers = offerDao.getOffersSkillNotCollaborating(skill.getName());
            for(Offer offer : offers){
    //            offer.setFinishDate(LocalDate.now().minusDays(1L));
                offerDao.updateOffer(offer);

                Student student = studentDao.getStudent(offer.getUsername());
                Email email = new Email();
                email.setSender("skill.sharing@uji.es");
                email.setReceiver(student.getEmail());
                email.setSendDate(LocalDate.now());
                email.setSubject("Skill disabled");
                email.setBody("Due to the skill you were offering help has been disabled, you can no longer offer it.");
                emailDao.addEmail(email);
            }

            List<Request> requests = requestDao.getRequestsSkillNotCollaborating(skill.getName());
            for(Request request : requests){
    //            request.setFinishDate(LocalDate.now().minusDays(1L));
                requestDao.updateRequest(request);

                Student student = studentDao.getStudent(request.getUsername());
                Email email = new Email();
                email.setSender("skill.sharing@uji.es");
                email.setReceiver(student.getEmail());
                email.setSendDate(LocalDate.now());
                email.setSubject("Skill disabled");
                email.setBody("Due to the skill you were requesting help has been disabled, you can no longer request it.");
                emailDao.addEmail(email);
            }    
        }

        skillDao.updateSkill(skill);
        return "redirect:list/";
    }

    @RequestMapping(value = "/activate/{name}")
    public String activateSkill(HttpSession session, Model model, @PathVariable String name) {
        InternalUser user = checkSession(session, SKP_ROLE);
        if (user == null){
            model.addAttribute("user", new InternalUser());
            return "login";
        }

        Skill skill = skillDao.getSkill(name);
        skill.setCanceled(false);
        skillDao.updateSkill(skill);
        model.addAttribute("skills", skillDao.getAvailableSkills());
        model.addAttribute("skills_disabled", skillDao.getDisabledSkills());
        return "skill/list";
    }

    public List<String> loadSkills() {
        List<String> skillLevels = new LinkedList<>();
        for(SkillLevel skillLevel : SkillLevel.values())
            skillLevels.add(skillLevel.getId());
        return skillLevels;
    }

}
