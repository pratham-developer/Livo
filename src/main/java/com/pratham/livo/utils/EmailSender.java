package com.pratham.livo.utils;


import com.pratham.livo.dto.message.EmailMessage;
import com.pratham.livo.service.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSender {

    private final TemplateEngine templateEngine;
    private final MessagePublisher messagePublisher;

    private String generateHtml(String templateName, Map<String, Object> templateVariables) {
        Context context = new Context();
        context.setVariables(templateVariables);
        // process src/main/resources/templates/{templateName}.html
        return templateEngine.process(templateName, context);
    }

    public void sendEmail(String to, String subject, String templateName, Map<String, Object> templateVariables){
        try{
            String htmlContent = generateHtml(templateName,templateVariables);
            EmailMessage emailMessage = EmailMessage.builder()
                    .to(to).subject(subject).htmlContent(htmlContent).build();
            messagePublisher.publishEmail(emailMessage);
        }catch (Exception e) {
            log.error("Failure in sending email to {}", to, e);
        }
    }
}
