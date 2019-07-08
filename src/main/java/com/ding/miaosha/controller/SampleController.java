package com.ding.miaosha.controller;

import com.ding.miaosha.domain.User;
import com.ding.miaosha.rabbitmq.MQSender;
import com.ding.miaosha.redis.RedisService;
import com.ding.miaosha.redis.UserKey;
import com.ding.miaosha.result.Result;
import com.ding.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/demo")
public class SampleController {
    @Autowired
    UserService userService;
    @Autowired
    RedisService redisService;
    @Autowired
    MQSender mqSender;

    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> home(){
        return Result.success("Hello,world");
    }
    @RequestMapping("/db/get")
    @ResponseBody
    public Result<User> dbget(){
        User user = userService.getById(1);
        return Result.success(user);
    }
    @RequestMapping("/thymeleaf")
    public String thymeleaf(Model model){
        model.addAttribute("name","dingyunping");
        return "hello";
    }
    @RequestMapping("db/tx")
    @ResponseBody
    public Result<Boolean> dbTx(){
        userService.tx();
        return Result.success(true);
    }
    @RequestMapping("redis/get")
    @ResponseBody
    public Result<User> redisGet() {
        User v1 = redisService.get(UserKey.getById,""+1,User.class);
        return Result.success(v1);
    }

    @RequestMapping("redis/set")
    @ResponseBody
    public Result<Boolean> redisSet() {
        User user = new User();
        user.setId(1);
        user.setName("11111");
        boolean ret = redisService.set(UserKey.getById,""+1,user);
        return Result.success(true);
    }
    @RequestMapping("/mq")
    @ResponseBody
    public Result<String> mq() {
        mqSender.send("hello world");
        return Result.success("hello world");
    }
    @RequestMapping("/mq/topic")
    @ResponseBody
    public Result<String> topic() {
        mqSender.sendTopic("hello world");
        return Result.success("hello world");
    }

    @RequestMapping("/mq/fanout")
    @ResponseBody
    public Result<String> fanout() {
        mqSender.sendFanout("hello world");
        return Result.success("hello world");
    }

    @RequestMapping("/mq/header")
    @ResponseBody
    public Result<String> header() {
        mqSender.sendHeader("hello world");
        return Result.success("hello world");
    }

}
