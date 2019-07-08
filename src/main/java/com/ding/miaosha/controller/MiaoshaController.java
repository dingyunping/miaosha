package com.ding.miaosha.controller;

import com.ding.miaosha.access.AccessLimit;
import com.ding.miaosha.domain.MiaoShaUser;
import com.ding.miaosha.domain.MiaoshaOrder;
import com.ding.miaosha.domain.OrderInfo;
import com.ding.miaosha.rabbitmq.MQSender;
import com.ding.miaosha.rabbitmq.MiaoshaMessage;
import com.ding.miaosha.redis.AccessKey;
import com.ding.miaosha.redis.GoodsKey;
import com.ding.miaosha.redis.MiaoshaKey;
import com.ding.miaosha.redis.RedisService;
import com.ding.miaosha.result.CodeMsg;
import com.ding.miaosha.result.Result;
import com.ding.miaosha.service.GoodsService;
import com.ding.miaosha.service.MiaoshaService;
import com.ding.miaosha.service.OrderService;
import com.ding.miaosha.util.MD5Util;
import com.ding.miaosha.util.UUIDUtil;
import com.ding.miaosha.vo.GoodsVo;
import com.sun.org.apache.regexp.internal.RE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean{
    private static Logger log = LoggerFactory.getLogger(LoginController.class);
    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    private Map<Long,Boolean> localOverMap = new HashMap<Long, Boolean>();

    @RequestMapping(value="/{path}/do_miaosha", method=RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoShaUser user,
                                   @RequestParam("goodsId")long goodsId,
                                   @PathVariable("path")String path){
        model.addAttribute("user", user);
        if (user == null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        //验证path
        boolean check = miaoshaService.checkPath(user,goodsId,path);
        if (!check){
            return Result.error(CodeMsg.REQUEST_ILLEAGL);
        }

        //内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock,""+goodsId);
        if (stock<0){
            localOverMap.put(goodsId,true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //判断商品是否已经秒杀到了，从缓存中查
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
        if (order != null){
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        //入队
        MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
        miaoshaMessage.setUser(user);
        miaoshaMessage.setGoodsId(goodsId);
        mqSender.sendMiaoshaMessage(miaoshaMessage);
       return Result.error(CodeMsg.MIAO_SHA_SUC);//排队中


//        //判断库存
//        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);//10个商品，两个请求
//        int stock = goods.getStockCount();
//        if (stock <= 0){
//           return Result.error(CodeMsg.MIAO_SHA_OVER);
//        }
//        //判断商品是否已经秒杀到了，从缓存中查
//        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
//        if (order != null){
//            return Result.error(CodeMsg.REPEATE_MIAOSHA);
//        }
        //进入秒杀，1、减库存，2、下订单，3、写入秒杀订单，在事务中进行，写在service，订单生成后保存到缓存中
//        OrderInfo orderInfo = miaoshaService.miaosha(user,goods);
//        return Result.success(orderInfo);
    }

    /**
     * 秒杀成功返回orderId
     * 秒杀失败返回-1
     * 0：排队中
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @AccessLimit(seconds=5,maxCount=10,needLogin=true)
    @RequestMapping(value="/result", method=RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model,MiaoShaUser user,
                                      @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return Result.success(result);
    }

    /**
     * 系统初始化,将秒杀商品数量预加载到缓存中
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if (goodsList == null){
            return;
        }
        for (GoodsVo goods : goodsList){
            redisService.set(GoodsKey.getMiaoshaGoodsStock,""+goods.getId(),goods.getStockCount());
            localOverMap.put(goods.getId(),false);
        }
    }

    @AccessLimit(seconds=5,maxCount=5,needLogin=true)
    @RequestMapping(value="/path", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request,
                                         MiaoShaUser user,
                                         @RequestParam("goodsId")long goodsId,
                                         @RequestParam("verifyCode")int verifyCode) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
//        //限流防刷
//        String uri = request.getRequestURI();
//        String key = uri + "_" + user.getId();
//        Integer count = redisService.get(AccessKey.access,key,Integer.class);
//        if (count == null){
//            redisService.set(AccessKey.access,key,1);
//        }else if (count < 5){
//            redisService.incr(AccessKey.access,key);
//        }else {
//            return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
//        }

        //验证码
        boolean check = miaoshaService.checkVerifyCode(user,goodsId,verifyCode);
        if (!check){
            return Result.error(CodeMsg.REQUEST_ILLEAGL);
        }

        //路径
        String path = miaoshaService.createMiaoshaPath(user,goodsId);
        return Result.success(path);
    }

    @RequestMapping(value="/verifyCode", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCod(HttpServletResponse response,MiaoShaUser user,
                                              @RequestParam("goodsId")long goodsId) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        try {
            BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        }catch(Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }
}
