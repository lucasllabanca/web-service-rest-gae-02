package br.com.lucasllabanca.gae_exemplo1.cron;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/cron")
public class CronService {

    private static final Logger log = Logger.getLogger(CronService.class.getName());

    @GetMapping(path = "/testcron")
    public ResponseEntity<?> testCron(@RequestHeader("x-appengine-cron") boolean isAppEngineCron) {

        if (isAppEngineCron) {
            log.info("Cron message --- " + Calendar.getInstance().getTime());
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

}
