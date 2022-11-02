package pay.payment.controller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

        String block = webClient.get()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("gun_result: " + block);

        return "test";
    }

    @GetMapping("/auth-return")
    public String getAuth(@RequestBody AuthClass authClass){
        log.info("gun_result: " + authClass.getClient_id());
        return "ok";
    }

    @Getter
    static class AuthClass {
        private String code;
        private String client_id;
        private String client_secret;
        private String redirect_uri;
        private String grant_type;
    }



}
