package com.fisi.proyectocursos.dto.controller;

import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fisi.proyectocursos.dto.RecoveryDTO;
import com.fisi.proyectocursos.model.User;
import com.fisi.proyectocursos.service.impl.UserServiceImpl;
import com.fisi.proyectocursos.utils.RecoveryURL;

import net.bytebuddy.utility.RandomString;

@Controller
public class ForgotPasswordController {

	@Autowired
	private UserServiceImpl userService;
	
	@Autowired
	private JavaMailSender mailSender;
	
	private String token;

	@GetMapping("/recuperar")
	public String recuperar(@ModelAttribute("recovery") RecoveryDTO recovery) {
		return "auth/recovery";
	}

	@PostMapping("/recuperar")
	public String recuperar(@ModelAttribute("recovery") RecoveryDTO recovery, Model model,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {

		if (userService.existsByUsername(recovery.getEmail())) {		
			String token = RandomString.make(45);

			userService.updateResetPasswordToken(token, recovery.getEmail());
			
			String resetPasswordLink = RecoveryURL.getSiteURL(request) + "/restablecer_contrasena?token=" + token;

			try {
				sendEmail(recovery.getEmail(), resetPasswordLink);
			} catch (UnsupportedEncodingException | MessagingException e) {
				redirectAttributes.addFlashAttribute("notification", "Error al enviar el correo.");
			}
			
			redirectAttributes.addFlashAttribute("notification",
					"Te acabamos de enviar un correo con las instrucciones para restablecer tu contraseña.");
			return "redirect:/login";
		} else {
			model.addAttribute("msg", "El correo ingresado no está registrado");
			return "auth/recovery";
		}

	}
	
	@GetMapping("/restablecer_contrasena")
	public String restablecer(@RequestParam String token, @ModelAttribute("recovery") RecoveryDTO recovery, Model model) {
		this.token = token;
		return "auth/change_password";
	}
	
	@PostMapping("/restablecer_contrasena")
	public String restablecer(@ModelAttribute("recovery") RecoveryDTO recovery, Model model, RedirectAttributes redirectAttributes) {
		User user = userService.findByResetPasswordToken(this.token);
		userService.updatePassword(user, recovery.getNewPassword());
		
		redirectAttributes.addFlashAttribute("notification", "Cambio de contraseña exitoso.");
		this.token = null;
		return "redirect:/login";
	}

	private void sendEmail(String email, String resetPasswordLink) throws UnsupportedEncodingException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		
		helper.setFrom("contacto@edufind.com", "Soporte EDUFIND");
		helper.setTo(email);
		
		String subject = "EDUFIND - Restablece tu contraseña";
		
		String content = "<p>Hola,</p>" 
				+ "<p>Recibimos tu solicitud de cambio de contraseña.</p>"
				+ "<p>Para restablecer tu contraseña usa el siguiente enlace:</p>"
				+ "<a href=\"" + resetPasswordLink + "\">Cambiar mi contraseña</a>"
				+ "<p>Si no has solicitado restablecer tu contraseña, comunícanoslo respondiendo directamente a este correo electrónico. Aún no se han hecho cambios en tu cuenta.</p>";
		
		helper.setSubject(subject);
		helper.setText(content, true);
		
		mailSender.send(message);
	
		
	}

}
