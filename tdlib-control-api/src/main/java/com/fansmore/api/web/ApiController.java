package com.fansmore.api.web;

import com.fansmore.api.common.Result;
import com.fansmore.api.common.ResultCode;
import com.fansmore.api.execute.ActuatorManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ApiController extends BaseController {

    @GetMapping("push/phone/{phone}")
    public Result pushPhone(@PathVariable String phone) {
        ActuatorManager.getInstance().pushPhone(phone);
        return ResultCode.SUCCESS.bindResult();
    }

    @GetMapping("push/code/{phone}/{code}")
    public Result pushCode(@PathVariable String phone, @PathVariable String code) {
        ActuatorManager.getInstance().pushCode(phone, code);
        return ResultCode.SUCCESS.bindResult();
    }

    @PostMapping("push/command/{phone}")
    public Result pushCommand(@PathVariable String phone, @RequestBody String command) {
        ActuatorManager.getInstance().pushCommand(phone, command);
        return ResultCode.SUCCESS.bindResult();
    }
}
