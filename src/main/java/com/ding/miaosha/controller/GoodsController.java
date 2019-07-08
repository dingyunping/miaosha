package com.ding.miaosha.controller;

import com.ding.miaosha.domain.MiaoShaUser;
import com.ding.miaosha.redis.GoodsKey;
import com.ding.miaosha.redis.RedisService;
import com.ding.miaosha.result.Result;
import com.ding.miaosha.service.GoodsService;
import com.ding.miaosha.service.MiaoshaUserService;
import com.ding.miaosha.vo.GoodsDetailVo;
import com.ding.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring4.context.SpringWebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/goods")
public class GoodsController {
    private static Logger log = LoggerFactory.getLogger(GoodsController.class);
    @Autowired
    MiaoshaUserService userService;
    @Autowired
    RedisService redisService;
    @Autowired
    GoodsService goodsService;
    //用于渲染
    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;
    @Autowired
    ApplicationContext applicationContext;
    //秒杀商品列表,返回html的源代码
    @RequestMapping(value = "/to_list",produces = "text/html")
    @ResponseBody
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, MiaoShaUser user) {
        model.addAttribute("user", user);
        //从缓存中取出页面缓存
        String html = redisService.get(GoodsKey.getGoodsList,"",String.class);
        if (!StringUtils.isEmpty(html)){
            return html;
        }
        //查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);
        //return "goods_list";

        SpringWebContext context = new SpringWebContext(request,response,
                request.getServletContext(),
                request.getLocale(),model.asMap(),applicationContext);
        //缓存为空，手动渲染,并存到缓存中
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list",context);
        if (!StringUtils.isEmpty(html)){
            redisService.set(GoodsKey.getGoodsList,"",html);
        }
        return html;
    }

    //秒杀商品详情页
    @RequestMapping(value = "/to_detail1/{goodsId}",produces = "text/html")
    @ResponseBody
    public String detail1(HttpServletRequest request,HttpServletResponse response,
                         Model model, MiaoShaUser user,
                         @PathVariable("goodsId")long goodsId) {
        model.addAttribute("user", user);
        //取缓存
        String html = redisService.get(GoodsKey.getGoodsDetail,""+goodsId,String.class);
        if (!StringUtils.isEmpty(html)){
            return html;
        }
        //查询商品列表,手动渲染
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods",goods);
        //
        long startTime = goods.getStartDate().getTime();
        long endTime = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if (now < startTime){//秒杀维开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)(startTime - now)/1000;
        }else if (now > endTime) {//秒杀结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("miaoshaStatus",miaoshaStatus);
        model.addAttribute("remainSeconds",remainSeconds);
       // return "goods_detail";

        SpringWebContext context = new SpringWebContext(request,response,
                request.getServletContext(),
                request.getLocale(),model.asMap(),applicationContext);
        //缓存为空，手动渲染,并存到缓存中
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail",context);
        if (!StringUtils.isEmpty(html)){
            redisService.set(GoodsKey.getGoodsDetail,""+goodsId,html);
        }
        return html;
    }

    //秒杀商品详情页
    @RequestMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail(HttpServletRequest request,HttpServletResponse response,
                         Model model, MiaoShaUser user,
                         @PathVariable("goodsId")long goodsId) {
        //查询商品列表
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        long startTime = goods.getStartDate().getTime();
        long endTime = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if (now < startTime){//秒杀维开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)(startTime - now)/1000;
        }else if (now > endTime) {//秒杀结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setMiaoshaStatus(miaoshaStatus);
        vo.setRemainSeconds(remainSeconds);
        return Result.success(vo);
    }
}
