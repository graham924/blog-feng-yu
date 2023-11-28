package com.minzheng.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.minzheng.blog.dao.*;
import com.minzheng.blog.dto.UserDetailDTO;
import com.minzheng.blog.entity.UserAuth;
import com.minzheng.blog.entity.UserInfo;
import com.minzheng.blog.exception.BizException;
import com.minzheng.blog.service.RedisService;
import com.minzheng.blog.util.IpUtils;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.minzheng.blog.constant.RedisPrefixConst.*;
import static com.minzheng.blog.enums.ZoneEnum.SHANGHAI;


/**
 * 用户详细信息服务
 *
 * @author yezhiqiu
 * @date 2021/08/10
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // 1.这里注入了5个对象，有不同的用处
    @Autowired
    private UserAuthDao userAuthDao;	// 1.1.查询账号认证信息（密码等）
    @Autowired
    private UserInfoDao userInfoDao;	// 1.2.查询账号基本信息
    @Autowired
    private RoleDao roleDao;			// 1.3.查询账号角色
    @Autowired
    private RedisService redisService;	// 1.4.查询账号的点赞信息
    @Resource
    private HttpServletRequest request;	// 1.5.获取到http请求

    // 2.完成账号认证，重写UserDetailsService方法，返回一个UserDetails对象
    @Override
    public UserDetails loadUserByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new BizException("用户名不能为空！");
        }

        // 2.1.查询账号是否存在，查到 userId、userInfoId、username、password、loginType
        UserAuth userAuth = userAuthDao.selectOne(
                new LambdaQueryWrapper<UserAuth>()
                        .select(UserAuth::getId, UserAuth::getUserInfoId, UserAuth::getUsername, UserAuth::getPassword, UserAuth::getLoginType)
                        .eq(UserAuth::getUsername, username)
        );

        // 2.2.如果没有查到，则抛异常
        if (Objects.isNull(userAuth)) {
            throw new BizException("用户名不存在!");
        }

        // 2.3.查到了，就将登录信息封装成一个UserDetails对象，并返回
        return convertUserDetail(userAuth, request);
    }

    /**
     *
     * 封装用户登录信息
     * 3.将登录信息封装成一个UserDetails对象
     * 返回一个 UserDetailDTO对象 也可以，因为 UserDetailDTO 是 UserDetails 的实现类
     */
    public UserDetailDTO convertUserDetail(UserAuth user, HttpServletRequest request) {
        // 3.1.查询账号信息
        UserInfo userInfo = userInfoDao.selectById(user.getUserInfoId());

        // 3.2.查询账号角色
        List<String> roleList = roleDao.listRolesByUserInfoId(userInfo.getId());

        // 3.3.从redis中查询账号点赞信息
        Set<Object> articleLikeSet = redisService.sMembers(ARTICLE_USER_LIKE + userInfo.getId());
        Set<Object> commentLikeSet = redisService.sMembers(COMMENT_USER_LIKE + userInfo.getId());
        Set<Object> talkLikeSet = redisService.sMembers(TALK_USER_LIKE + userInfo.getId());

        // 3.4.获取设备信息
        String ipAddress = IpUtils.getIpAddress(request);
        String ipSource = IpUtils.getIpSource(ipAddress);
        UserAgent userAgent = IpUtils.getUserAgent(request);

        // 3.5.将用户基本信息、认证信息、权限角色，封装成一个UserDetails对象
        return UserDetailDTO.builder()
                .id(user.getId())
                .loginType(user.getLoginType())
                .userInfoId(userInfo.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(userInfo.getEmail())
                .roleList(roleList)
                .nickname(userInfo.getNickname())
                .avatar(userInfo.getAvatar())
                .intro(userInfo.getIntro())
                .webSite(userInfo.getWebSite())
                .articleLikeSet(articleLikeSet)
                .commentLikeSet(commentLikeSet)
                .talkLikeSet(talkLikeSet)
                .ipAddress(ipAddress)
                .ipSource(ipSource)
                .isDisable(userInfo.getIsDisable())
                .browser(userAgent.getBrowser().getName())
                .os(userAgent.getOperatingSystem().getName())
                .lastLoginTime(LocalDateTime.now(ZoneId.of(SHANGHAI.getZone())))
                .build();
    }
}

//@Service
//public class UserDetailsServiceImpl implements UserDetailsService {
//    @Autowired
//    private UserAuthDao userAuthDao;
//    @Autowired
//    private UserInfoDao userInfoDao;
//    @Autowired
//    private RoleDao roleDao;
//    @Autowired
//    private RedisService redisService;
//    @Resource
//    private HttpServletRequest request;
//
//    @Override
//    public UserDetails loadUserByUsername(String username) {
//        if (StringUtils.isBlank(username)) {
//            throw new BizException("用户名不能为空！");
//        }
//        // 查询账号是否存在
//        UserAuth userAuth = userAuthDao.selectOne(new LambdaQueryWrapper<UserAuth>()
//                .select(UserAuth::getId, UserAuth::getUserInfoId, UserAuth::getUsername, UserAuth::getPassword, UserAuth::getLoginType)
//                .eq(UserAuth::getUsername, username));
//        if (Objects.isNull(userAuth)) {
//            throw new BizException("用户名不存在!");
//        }
//        // 封装登录信息
//        return convertUserDetail(userAuth, request);
//    }
//
//    /**
//     * 封装用户登录信息
//     *
//     * @param user    用户账号
//     * @param request 请求
//     * @return 用户登录信息
//     */
//    public UserDetailDTO convertUserDetail(UserAuth user, HttpServletRequest request) {
//        // 查询账号信息
//        UserInfo userInfo = userInfoDao.selectById(user.getUserInfoId());
//        // 查询账号角色
//        List<String> roleList = roleDao.listRolesByUserInfoId(userInfo.getId());
//        // 查询账号点赞信息
//        Set<Object> articleLikeSet = redisService.sMembers(ARTICLE_USER_LIKE + userInfo.getId());
//        Set<Object> commentLikeSet = redisService.sMembers(COMMENT_USER_LIKE + userInfo.getId());
//        Set<Object> talkLikeSet = redisService.sMembers(TALK_USER_LIKE + userInfo.getId());
//        // 获取设备信息
//        String ipAddress = IpUtils.getIpAddress(request);
//        String ipSource = IpUtils.getIpSource(ipAddress);
//        UserAgent userAgent = IpUtils.getUserAgent(request);
//        // 封装权限集合
//        return UserDetailDTO.builder()
//                .id(user.getId())
//                .loginType(user.getLoginType())
//                .userInfoId(userInfo.getId())
//                .username(user.getUsername())
//                .password(user.getPassword())
//                .email(userInfo.getEmail())
//                .roleList(roleList)
//                .nickname(userInfo.getNickname())
//                .avatar(userInfo.getAvatar())
//                .intro(userInfo.getIntro())
//                .webSite(userInfo.getWebSite())
//                .articleLikeSet(articleLikeSet)
//                .commentLikeSet(commentLikeSet)
//                .talkLikeSet(talkLikeSet)
//                .ipAddress(ipAddress)
//                .ipSource(ipSource)
//                .isDisable(userInfo.getIsDisable())
//                .browser(userAgent.getBrowser().getName())
//                .os(userAgent.getOperatingSystem().getName())
//                .lastLoginTime(LocalDateTime.now(ZoneId.of(SHANGHAI.getZone())))
//                .build();
//    }
//
//}
