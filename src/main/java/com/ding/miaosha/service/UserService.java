package com.ding.miaosha.service;

import com.ding.miaosha.dao.UserDao;
import com.ding.miaosha.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService {
    @Autowired
    UserDao userDao;

    public User getById(int id){
        return userDao.getById(id);
    }

    /**
     * 加@Transactional保证事务，一个失败，会回滚
     * 若不加，则id为3的数据会被插入数据库
     * @return
     */
    @Transactional
    public Boolean tx(){
        User us1 = new User();
        us1.setId(3);
        us1.setName("3333");
        userDao.insert(us1);

        User us2 = new User();
        us2.setId(1);
        us2.setName("1111");
        userDao.insert(us2);
        return true;
    }

}
