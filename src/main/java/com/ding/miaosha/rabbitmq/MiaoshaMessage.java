package com.ding.miaosha.rabbitmq;


import com.ding.miaosha.domain.MiaoShaUser;

public class MiaoshaMessage {
	private MiaoShaUser user;
	private long goodsId;
	public MiaoShaUser getUser() {
		return user;
	}
	public void setUser(MiaoShaUser user) {
		this.user = user;
	}
	public long getGoodsId() {
		return goodsId;
	}
	public void setGoodsId(long goodsId) {
		this.goodsId = goodsId;
	}
}
