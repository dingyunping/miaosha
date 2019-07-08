package com.ding.miaosha.service;

import com.ding.miaosha.dao.MiaoShaUserDao;
import com.ding.miaosha.domain.MiaoShaUser;
import com.ding.miaosha.exception.GlobalException;
import com.ding.miaosha.redis.MiaoshaUserKey;
import com.ding.miaosha.redis.RedisService;
import com.ding.miaosha.result.CodeMsg;
import com.ding.miaosha.util.MD5Util;
import com.ding.miaosha.util.UUIDUtil;
import com.ding.miaosha.vo.LoginVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class MiaoshaUserService {
	
	
	public static final String COOKI_NAME_TOKEN = "token";
	
	@Autowired
	MiaoShaUserDao miaoShaUserDao;
	
	@Autowired
	RedisService redisService;
	private static final String salt = "1a2b3c4d";
	
	public MiaoShaUser getById(long id) {
		//对象缓存，只要未发生变化，永久有效
		//取缓存,若不为空，取缓存，为空，取数据库
		MiaoShaUser user = redisService.get(MiaoshaUserKey.getById,""+id,MiaoShaUser.class);
		if (user != null){
			return user;
		}
		//取数据库
		user = miaoShaUserDao.getById(id);
		if (user != null){
			redisService.set(MiaoshaUserKey.getById,""+id,user);
		}
		return user;
	}

	public boolean updatePassword(String token,long id,String passwordNew){
		//取user
		MiaoShaUser user = getById(id);
		if (user == null){
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//更新数据库
		MiaoShaUser toBeUpdate = new MiaoShaUser();
		toBeUpdate.setId(id);
		toBeUpdate.setPassword(MD5Util.fromPassToDbPass(passwordNew,user.getSalt()));
		miaoShaUserDao.update(toBeUpdate);
		//处理缓存
		redisService.delete(MiaoshaUserKey.getById,""+id);
		user.setPassword(toBeUpdate.getPassword());
		redisService.set(MiaoshaUserKey.token,token,user);
		return true;
	}


	public boolean login(HttpServletResponse response, LoginVo loginVo) {
		if(loginVo == null) {
			throw new GlobalException(CodeMsg.SERVER_ERROR);
		}
		String mobile = loginVo.getMobile();
		String formPass = loginVo.getPassword();
		//判断手机号是否存在
		MiaoShaUser user = getById(Long.parseLong(mobile));
		if(user == null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//验证密码
		String dbPass = user.getPassword();
		String saltDB = user.getSalt();
		String calcPass = MD5Util.fromPassToDbPass(formPass, saltDB);
		if(!calcPass.equals(dbPass)) {
			throw new GlobalException(CodeMsg.PASSWORD_ERROR);
		}
		//生成cookie
		String token = UUIDUtil.uuid();
		addCookie(response, token, user);
		return true;
	}


	//从缓存中取出token
	public MiaoShaUser getByToken(HttpServletResponse response, String token) {
		if(StringUtils.isEmpty(token)) {
			return null;
		}
		MiaoShaUser user = redisService.get(MiaoshaUserKey.token, token, MiaoShaUser.class);
		//延长有效期，不要每次都生成一个新的token，只要更新就可以了，所以将token传进去
		if(user != null) {
			addCookie(response, token, user);
		}
		return user;
	}

	//将session信息保存到redis中,
	private void addCookie(HttpServletResponse response, String token, MiaoShaUser user) {
		redisService.set(MiaoshaUserKey.token, token, user);
		Cookie cookie = new Cookie(COOKI_NAME_TOKEN, token);
		cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
		cookie.setPath("/");
		response.addCookie(cookie);
	}

}
