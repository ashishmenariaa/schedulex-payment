package expense_tracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // ✅ YEH ALAG CONTROLLER HAI (RestController se alag)
public class ViewController {
    
    @GetMapping("/")
    public String home() {
        return "index";  // ✅ index.html render karega
    }
}