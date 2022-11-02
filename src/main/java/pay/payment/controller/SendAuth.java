package pay.payment.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import pay.payment.AppConfig;

@Slf4j
@RestController
public class SendAuth {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);

    @GetMapping("/auth-function")
    public String authFunction(String tmp)    {
        WebClient webClient = (WebClient) ac.getBean("webClient");

        String block = webClient.post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("gun_result: " + block);

        return "test";
    }

    @GetMapping("/auth-return")
    public String getAuth(@ModelAttribute AuthClass authClass){
        log.info("gun_result: {} {} {}", authClass.getCode(), authClass.getScope(), authClass.getState());
        return "ok";
    }

    @Getter
    @Setter
    static class AuthClass {
        private String code;
        private String scope;
        private String state;
    }

}
